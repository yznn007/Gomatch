package com.gomatch.server.session;

import com.gomatch.server.dao.SocketSessionDAO;
import com.gomatch.server.dao.UserAuthStateDAO;
import com.gomatch.server.handler.ClientHandler;
import com.gomatch.server.redis.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final SessionManager instance = new SessionManager();
    private static final SocketSessionDAO socketSessionDAO = new SocketSessionDAO();
    private static final UserAuthStateDAO userAuthStateDAO = new UserAuthStateDAO();
    private static final long SESSION_TIMEOUT = 300;

    public static class Session {
        public final String token;
        public final long userId;
        public final String username;
        public final int score;
        public final int winCount;
        public final int loseCount;
        public final int drawCount;
        public String role;
        public volatile ClientHandler handler;
        public volatile Instant lastActiveTime;
        public volatile String roomId;

        public Session(String token, long userId, String username, int score,
                       int winCount, int loseCount, int drawCount) {
            this(token, userId, username, score, winCount, loseCount, drawCount, "PLAYER");
        }

        public Session(String token, long userId, String username, int score,
                       int winCount, int loseCount, int drawCount, String role) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.score = score;
            this.winCount = winCount;
            this.loseCount = loseCount;
            this.drawCount = drawCount;
            this.role = role;
            this.lastActiveTime = Instant.now();
        }

        public boolean isExpired() {
            return Instant.now().isAfter(lastActiveTime.plusSeconds(SESSION_TIMEOUT));
        }

        public void updateLastActive() {
            lastActiveTime = Instant.now();
        }
    }

    private final Map<String, Session> sessionsByToken = new ConcurrentHashMap<>();
    private final Map<Long, Session> sessionsByUserId = new ConcurrentHashMap<>();

    private SessionManager() {
        startCleanupTask();
    }

    public static SessionManager getInstance() {
        return instance;
    }

    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60_000);
                    cleanupExpiredSessions();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "session-cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        log.info("Session cleanup task started");
    }

    private void cleanupExpiredSessions() {
        int removedCount = 0;
        for (Map.Entry<String, Session> entry : sessionsByToken.entrySet()) {
            Session candidate = entry.getValue();
            if (candidate.isExpired() && sessionsByToken.remove(entry.getKey(), candidate)) {
                sessionsByUserId.remove(candidate.userId, candidate);
                socketSessionDAO.disconnect(candidate.token, "timeout");
                RedisManager.getInstance().removeSession(candidate.token);
                removedCount++;
            }
        }
        if (removedCount > 0) {
            log.info("Cleaned up {} expired sessions", removedCount);
        }
        socketSessionDAO.cleanupExpiredSessions(60);
    }

    public synchronized Session createSession(long userId, String username, int score,
                                               int winCount, int loseCount, int drawCount, String role) {
        revokeAllSessionsByUserId(userId, "replaced");
        String token = UUID.randomUUID().toString();
        Session session = new Session(token, userId, username, score,
                winCount, loseCount, drawCount, role);
        sessionsByToken.put(token, session);
        sessionsByUserId.put(userId, session);
        socketSessionDAO.saveSession(userId, token, null, null, null);
        RedisManager.SessionData sessionData = new RedisManager.SessionData(
                userId, username, score, winCount, loseCount, drawCount,
                null, role, System.currentTimeMillis());
        RedisManager.getInstance().saveSession(token, sessionData);
        RedisManager.getInstance().setOnline(userId, token);
        log.info("Session created: userId={}, role={}, token={}", userId, role, token.substring(0, 8));
        return session;
    }

    public synchronized Session getSessionByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessionsByToken.get(token);
        if (session == null) {
            session = recoverFromRedis(token);
        }
        if (session == null) {
            return null;
        }

        UserAuthStateDAO.AuthState authState = userAuthStateDAO.find(session.userId);
        boolean active = authState != null && authState.isActive();
        SessionAuthorizationPolicy.Decision decision = SessionAuthorizationPolicy.decide(
                session.lastActiveTime, Instant.now(), session.handler != null, active);
        if (decision != SessionAuthorizationPolicy.Decision.ALLOW) {
            if (decision != SessionAuthorizationPolicy.Decision.ALREADY_CONNECTED) {
                removeSession(token, decision.name().toLowerCase());
            }
            log.warn("Reconnect rejected: userId={}, reason={}", session.userId, decision);
            return null;
        }

        session.role = authState.role();
        refreshActivity(session);
        return session;
    }

    private Session recoverFromRedis(String token) {
        RedisManager.SessionData redisSession = RedisManager.getInstance().getSession(token);
        if (redisSession == null) {
            return null;
        }
        Session existingByUserId = sessionsByUserId.get(redisSession.userId);
        if (existingByUserId != null) {
            log.warn("recoverFromRedis skipped: userId {} already has active session with token {}",
                    redisSession.userId, existingByUserId.token.substring(0, 8));
            return null;
        }
        Session session = new Session(token, redisSession.userId, redisSession.username,
                redisSession.score, redisSession.winCount, redisSession.loseCount,
                redisSession.drawCount, redisSession.role == null ? "PLAYER" : redisSession.role);
        session.roomId = redisSession.roomId;
        session.lastActiveTime = Instant.ofEpochMilli(redisSession.lastActiveTime);
        sessionsByToken.put(token, session);
        sessionsByUserId.put(redisSession.userId, session);
        socketSessionDAO.saveSession(redisSession.userId, token, null, null, null);
        return session;
    }

    private void refreshActivity(Session session) {
        session.updateLastActive();
        socketSessionDAO.updateHeartbeat(session.token);
        RedisManager.getInstance().updateSessionActivity(session.token);
    }

    public Session getSessionByUserId(long userId) {
        Session session = sessionsByUserId.get(userId);
        if (session != null) {
            refreshActivity(session);
        }
        return session;
    }

    public void removeSession(String token) {
        removeSession(token, "manual");
    }

    private void removeSession(String token, String reason) {
        Session session = sessionsByToken.remove(token);
        if (session == null) {
            RedisManager.getInstance().removeSession(token);
            return;
        }
        sessionsByUserId.remove(session.userId, session);
        socketSessionDAO.disconnect(token, reason);
        RedisManager.getInstance().removeSession(token);
        RedisManager.getInstance().setOffline(session.userId);
    }

    public synchronized void revokeAllSessionsByUserId(long userId, String reason) {
        for (Map.Entry<String, Session> entry : sessionsByToken.entrySet()) {
            Session session = entry.getValue();
            if (session.userId == userId && sessionsByToken.remove(entry.getKey(), session)) {
                sessionsByUserId.remove(userId, session);
                socketSessionDAO.disconnect(session.token, reason);
                RedisManager.getInstance().removeSession(session.token);
            }
        }
        sessionsByUserId.remove(userId);
        RedisManager.getInstance().setOffline(userId);
    }

    public void removeSessionByUserId(long userId) {
        revokeAllSessionsByUserId(userId, "manual");
    }

    public void updateSessionRoom(long userId, String roomId) {
        Session session = sessionsByUserId.get(userId);
        if (session != null) {
            session.roomId = roomId;
            refreshActivity(session);
            socketSessionDAO.updateRoom(session.token, roomId);
            RedisManager.getInstance().updateSessionRoom(session.token, roomId);
        }
    }

    public void updateSessionHandler(long userId, ClientHandler handler) {
        Session session = sessionsByUserId.get(userId);
        if (session == null) {
            return;
        }
        session.handler = handler;
        refreshActivity(session);
        if (handler != null) {
            socketSessionDAO.updateStatus(session.token, "connected");
        } else {
            socketSessionDAO.disconnect(session.token, "disconnected");
        }
    }

    public int getActiveSessionCount() {
        return sessionsByUserId.size();
    }
}
