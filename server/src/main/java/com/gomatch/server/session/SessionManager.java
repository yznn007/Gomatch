package com.gomatch.server.session;

import com.gomatch.server.dao.SocketSessionDAO;
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
        public ClientHandler handler;
        public Instant lastActiveTime;
        public String roomId;

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
            this.handler = null;
            this.lastActiveTime = Instant.now();
            this.roomId = null;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(lastActiveTime.plusSeconds(SESSION_TIMEOUT));
        }

        public void updateLastActive() {
            this.lastActiveTime = Instant.now();
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
            while (true) {
                try {
                    Thread.sleep(60000);
                    cleanupExpiredSessions();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
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
            if (entry.getValue().isExpired()) {
                Session session = sessionsByToken.remove(entry.getKey());
                sessionsByUserId.remove(session.userId);
                socketSessionDAO.disconnect(session.token, "timeout");
                removedCount++;
                log.debug("Cleaned up expired session: userId={}", session.userId);
            }
        }
        if (removedCount > 0) {
            log.info("Cleaned up {} expired sessions", removedCount);
        }

        socketSessionDAO.cleanupExpiredSessions(60);
    }

    public Session createSession(long userId, String username, int score,
                                  int winCount, int loseCount, int drawCount, String role) {
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

    public Session getSessionByToken(String token) {
        Session session = sessionsByToken.get(token);
        if (session != null) {
            session.updateLastActive();
            socketSessionDAO.updateHeartbeat(token);
            RedisManager.getInstance().updateSessionActivity(token);
        } else {
            RedisManager.SessionData redisSession = RedisManager.getInstance().getSession(token);
            if (redisSession != null) {
                session = new Session(token, redisSession.userId, redisSession.username,
                        redisSession.score, redisSession.winCount, redisSession.loseCount,
                        redisSession.drawCount, redisSession.role != null ? redisSession.role : "PLAYER");
                session.roomId = redisSession.roomId;
                session.lastActiveTime = Instant.ofEpochMilli(redisSession.lastActiveTime);
                sessionsByToken.put(token, session);
                sessionsByUserId.put(redisSession.userId, session);
                socketSessionDAO.saveSession(redisSession.userId, token, null, null, null);
                log.info("Session recovered from Redis: userId={}", redisSession.userId);
            }
        }
        return session;
    }

    public Session getSessionByUserId(long userId) {
        Session session = sessionsByUserId.get(userId);
        if (session != null) {
            session.updateLastActive();
            socketSessionDAO.updateHeartbeat(session.token);
        }
        return session;
    }

    public void removeSession(String token) {
        Session session = sessionsByToken.remove(token);
        if (session != null) {
            sessionsByUserId.remove(session.userId);
            socketSessionDAO.disconnect(token, "manual");
            RedisManager.getInstance().removeSession(token);
            RedisManager.getInstance().setOffline(session.userId);
            log.info("Session removed: userId={}", session.userId);
        }
    }

    public void removeSessionByUserId(long userId) {
        Session session = sessionsByUserId.remove(userId);
        if (session != null) {
            sessionsByToken.remove(session.token);
            socketSessionDAO.disconnect(session.token, "manual");
            RedisManager.getInstance().removeSession(session.token);
            RedisManager.getInstance().setOffline(userId);
            log.info("Session removed by userId: {}", userId);
        }
    }

    public void updateSessionRoom(long userId, String roomId) {
        Session session = sessionsByUserId.get(userId);
        if (session != null) {
            session.roomId = roomId;
            session.updateLastActive();
            socketSessionDAO.updateRoom(session.token, roomId);
            RedisManager.getInstance().updateSessionRoom(session.token, roomId);
            log.debug("Session room updated: userId={}, roomId={}", userId, roomId);
        }
    }

    public void updateSessionHandler(long userId, ClientHandler handler) {
        Session session = sessionsByUserId.get(userId);
        if (session != null) {
            session.handler = handler;
            session.updateLastActive();
            if (handler != null) {
                socketSessionDAO.updateStatus(session.token, "connected");
            } else {
                socketSessionDAO.disconnect(session.token, "disconnected");
            }
        }
    }

    public int getActiveSessionCount() {
        return sessionsByUserId.size();
    }
}



