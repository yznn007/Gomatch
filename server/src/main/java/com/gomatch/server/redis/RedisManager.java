package com.gomatch.server.redis;

import com.google.gson.Gson;
import com.gomatch.database.config.RuntimeConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RedisManager {
    private static final Logger log = LoggerFactory.getLogger(RedisManager.class);
    private static final RedisManager instance = new RedisManager();

    private RedisClient redisClient;
    private RedisCommands<String, String> syncCommands;
    private volatile boolean connected = false;
    private final Gson gson = new Gson();

    private RedisManager() {}

    public static RedisManager getInstance() { return instance; }

    public synchronized void init() {
        try {
            Properties props = RuntimeConfig.loadRuntimeProperties("database.properties");
            RuntimeConfig.requireRuntimeCredentials(props);
            String host = props.getProperty("redis.host");
            int port = Integer.parseInt(props.getProperty("redis.port"));
            String password = props.getProperty("redis.password");
            int database = Integer.parseInt(props.getProperty("redis.database", "0"));
            int timeout = Integer.parseInt(props.getProperty("redis.timeout", "3000"));

            RedisURI uri = RedisURI.builder()
                    .withHost(host)
                    .withPort(port)
                    .withTimeout(java.time.Duration.ofMillis(timeout))
                    .withDatabase(database)
                    .build();

            if (!password.isEmpty()) {
                uri = RedisURI.builder()
                        .withHost(host)
                        .withPort(port)
                        .withTimeout(java.time.Duration.ofMillis(timeout))
                        .withDatabase(database)
                        .withPassword(password.toCharArray())
                        .build();
            }

            redisClient = RedisClient.create(uri);
            syncCommands = redisClient.connect().sync();
            connected = true;

            String ping = syncCommands.ping();
            if ("PONG".equals(ping)) {
                log.info("Redis connected: {}:{}", host, port);
                try {
                    syncCommands.configSet("stop-writes-on-bgsave-error", "no");
                    log.info("Set Redis stop-writes-on-bgsave-error to no successfully");
                } catch (Exception e) {
                    log.warn("Failed to set Redis stop-writes-on-bgsave-error to no: {}", e.getMessage());
                }
            } else {
                log.warn("Redis ping returned unexpected: {}", ping);
            }

        } catch (Exception e) {
            connected = false;
            throw new IllegalStateException("Redis is required but could not be initialized", e);
        }
    }

    public synchronized void close() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        connected = false;
        log.info("Redis connection closed");
    }

    public boolean isConnected() {
        return connected && syncCommands != null;
    }

    public void saveGameState(String roomId, String boardJson) {
        if (!isConnected()) return;
        try {
            String key = "game:" + roomId;
            syncCommands.set(key, boardJson);
            syncCommands.expire(key, 3600);
            log.debug("Saved game state for room {}: {} bytes", roomId, boardJson.length());
        } catch (Exception e) {
            log.warn("Failed to save game state to Redis: {}", e.getMessage());
        }
    }

    public String getGameState(String roomId) {
        if (!isConnected()) return null;
        try {
            return syncCommands.get("game:" + roomId);
        } catch (Exception e) {
            log.warn("Failed to get game state from Redis: {}", e.getMessage());
            return null;
        }
    }

    public void deleteGameState(String roomId) {
        if (!isConnected()) return;
        try {
            syncCommands.del("game:" + roomId);
        } catch (Exception e) {
            log.warn("Failed to delete game state from Redis: {}", e.getMessage());
        }
    }

    public boolean acquireLock(String roomId) {
        if (!isConnected()) return false;
        try {
            String luaScript =
                "if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', 5) then " +
                "return 1 " +
                "else " +
                "return 0 " +
                "end";
            Long result = syncCommands.eval(luaScript, io.lettuce.core.ScriptOutputType.INTEGER,
                new String[]{"lock:room:" + roomId},
                String.valueOf(System.currentTimeMillis()));
            return result != null && result == 1;
        } catch (Exception e) {
            log.warn("Failed to acquire lock: {}", e.getMessage());
            return false;
        }
    }

    public void releaseLock(String roomId) {
        if (!isConnected()) return;
        try {
            syncCommands.del("lock:room:" + roomId);
        } catch (Exception e) {
            log.warn("Failed to release lock: {}", e.getMessage());
        }
    }

    public void publish(String roomId, String message) {
        if (!isConnected()) return;
        try {
            String channel = "gomatch:game:" + roomId;
            syncCommands.publish(channel, message);
        } catch (Exception e) {
            log.warn("Failed to publish message: {}", e.getMessage());
        }
    }

    public void subscribe(String roomId, RedisPubSubListener<String, String> listener) {
        if (!isConnected()) return;
        try {
            StatefulRedisPubSubConnection<String, String> pubSubConn = redisClient.connectPubSub();
            pubSubConn.addListener(listener);
            pubSubConn.sync().subscribe("gomatch:game:" + roomId);
            log.info("Subscribed to channel: gomatch:game:{}", roomId);
        } catch (Exception e) {
            log.warn("Failed to subscribe: {}", e.getMessage());
        }
    }

    // ==================== Session Management ====================

    public static class SessionData {
        public long userId;
        public String username;
        public int score;
        public int winCount;
        public int loseCount;
        public int drawCount;
        public String roomId;
        public String role;
        public long lastActiveTime;

        public SessionData() {}

        public SessionData(long userId, String username, int score, int winCount,
                          int loseCount, int drawCount, String roomId, String role, long lastActiveTime) {
            this.userId = userId;
            this.username = username;
            this.score = score;
            this.winCount = winCount;
            this.loseCount = loseCount;
            this.drawCount = drawCount;
            this.roomId = roomId;
            this.role = role;
            this.lastActiveTime = lastActiveTime;
        }
    }

    public void saveSession(String token, SessionData data) {
        if (!isConnected()) return;
        try {
            String key = "session:" + token;
            String json = gson.toJson(data);
            syncCommands.set(key, json);
            syncCommands.expire(key, 300);
            log.debug("Saved session: token={}, userId={}", token.substring(0, 8), data.userId);
        } catch (Exception e) {
            log.warn("Failed to save session: {}", e.getMessage());
        }
    }

    public SessionData getSession(String token) {
        if (!isConnected()) return null;
        try {
            String key = "session:" + token;
            String json = syncCommands.get(key);
            if (json != null) {
                syncCommands.expire(key, 300);
                return gson.fromJson(json, SessionData.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get session: {}", e.getMessage());
        }
        return null;
    }

    public void removeSession(String token) {
        if (!isConnected()) return;
        try {
            syncCommands.del("session:" + token);
        } catch (Exception e) {
            log.warn("Failed to remove session: {}", e.getMessage());
        }
    }

    public void updateSessionRoom(String token, String roomId) {
        if (!isConnected()) return;
        try {
            String key = "session:" + token;
            String json = syncCommands.get(key);
            if (json != null) {
                SessionData data = gson.fromJson(json, SessionData.class);
                data.roomId = roomId;
                data.lastActiveTime = System.currentTimeMillis();
                syncCommands.set(key, gson.toJson(data));
                syncCommands.expire(key, 300);
            }
        } catch (Exception e) {
            log.warn("Failed to update session room: {}", e.getMessage());
        }
    }

    public void updateSessionActivity(String token) {
        if (!isConnected()) return;
        try {
            String key = "session:" + token;
            String json = syncCommands.get(key);
            if (json != null) {
                SessionData data = gson.fromJson(json, SessionData.class);
                data.lastActiveTime = System.currentTimeMillis();
                syncCommands.set(key, gson.toJson(data));
                syncCommands.expire(key, 300);
            }
        } catch (Exception e) {
            log.warn("Failed to update session activity: {}", e.getMessage());
        }
    }

    // ==================== Online Status ====================

    public void setOnline(long userId, String token) {
        if (!isConnected()) return;
        try {
            String key = "online:" + userId;
            syncCommands.set(key, token);
            syncCommands.expire(key, 300);
            log.debug("User online: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to set online status: {}", e.getMessage());
        }
    }

    public void setOffline(long userId) {
        if (!isConnected()) return;
        try {
            syncCommands.del("online:" + userId);
            log.debug("User offline: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to set offline status: {}", e.getMessage());
        }
    }

    public boolean isOnline(long userId) {
        if (!isConnected()) return false;
        try {
            return syncCommands.exists("online:" + userId) == 1;
        } catch (Exception e) {
            log.warn("Failed to check online status: {}", e.getMessage());
            return false;
        }
    }

    // ==================== Ranking (Sorted Set) ====================

    public void updateRank(long userId, int score) {
        if (!isConnected()) return;
        try {
            syncCommands.zadd("rank:leaderboard", score, String.valueOf(userId));
            log.debug("Updated rank: userId={}, score={}", userId, score);
        } catch (Exception e) {
            log.warn("Failed to update rank: {}", e.getMessage());
        }
    }

    public List<Long> getTopRank(int limit) {
        if (!isConnected()) return Collections.emptyList();
        try {
            List<String> result = syncCommands.zrevrange("rank:leaderboard", 0, limit - 1);
            List<Long> userIds = new ArrayList<>();
            for (String id : result) {
                userIds.add(Long.parseLong(id));
            }
            return userIds;
        } catch (Exception e) {
            log.warn("Failed to get top rank: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Long getRank(long userId) {
        if (!isConnected()) return null;
        try {
            Long rank = syncCommands.zrevrank("rank:leaderboard", String.valueOf(userId));
            return rank != null ? rank + 1 : null;
        } catch (Exception e) {
            log.warn("Failed to get rank: {}", e.getMessage());
            return null;
        }
    }

    public Integer getScore(long userId) {
        if (!isConnected()) return null;
        try {
            Double score = syncCommands.zscore("rank:leaderboard", String.valueOf(userId));
            return score != null ? score.intValue() : null;
        } catch (Exception e) {
            log.warn("Failed to get score: {}", e.getMessage());
            return null;
        }
    }

    public void clearRank() {
        if (!isConnected()) return;
        try {
            syncCommands.del("rank:leaderboard");
        } catch (Exception e) {
            log.warn("Failed to clear rank: {}", e.getMessage());
        }
    }

    // ==================== Match Queue ====================

    public void addMatchQueue(long userId, int score) {
        if (!isConnected()) return;
        try {
            String value = userId + ":" + score;
            syncCommands.lpush("match:queue", value);
            log.debug("Added to match queue: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to add to match queue: {}", e.getMessage());
        }
    }

    public String pollMatchQueue() {
        if (!isConnected()) return null;
        try {
            return syncCommands.rpop("match:queue");
        } catch (Exception e) {
            log.warn("Failed to poll match queue: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从匹配队列中移除指定用户
     */
    public void removeFromMatchQueue(long userId) {
        if (!isConnected()) return;
        try {
            // 使用 LREM 从列表中移除匹配的值
            // 由于值格式是 userId:score，需要前缀匹配
            List<String> all = syncCommands.lrange("match:queue", 0, -1);
            for (String entry : all) {
                String[] parts = entry.split(":");
                if (parts.length >= 1 && String.valueOf(userId).equals(parts[0])) {
                    syncCommands.lrem("match:queue", 1, entry);
                    log.debug("Removed from match queue: userId={}", userId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to remove from match queue: {}", e.getMessage());
        }
    }

    public int getMatchQueueSize() {
        if (!isConnected()) return 0;
        try {
            return syncCommands.llen("match:queue").intValue();
        } catch (Exception e) {
            log.warn("Failed to get match queue size: {}", e.getMessage());
            return 0;
        }
    }

    public void addRankedMatchQueue(long userId, int score) {
        if (!isConnected()) return;
        try {
            syncCommands.zadd("match:ranked", score, String.valueOf(userId));
            log.debug("Added to ranked match queue: userId={}, score={}", userId, score);
        } catch (Exception e) {
            log.warn("Failed to add to ranked match queue: {}", e.getMessage());
        }
    }

    public void removeRankedMatchQueue(long userId) {
        if (!isConnected()) return;
        try {
            syncCommands.zrem("match:ranked", String.valueOf(userId));
            log.debug("Removed from ranked match queue: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to remove from ranked match queue: {}", e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    public String pollRankedMatchQueue(int playerScore, long excludeUserId) {
        if (!isConnected()) return null;
        try {
            int maxDiff = 500;
            List<String> results = syncCommands.zrangebyscore("match:ranked",
                    String.valueOf(playerScore - maxDiff),
                    String.valueOf(playerScore + maxDiff));
            if (results != null && !results.isEmpty()) {
                String excludeIdStr = String.valueOf(excludeUserId);
                for (String opponentId : results) {
                    if (!opponentId.equals(excludeIdStr)) {
                        Double opponentScore = syncCommands.zscore("match:ranked", opponentId);
                        syncCommands.zrem("match:ranked", opponentId);
                        return opponentId + ":" + (opponentScore != null ? opponentScore.intValue() : 0);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to poll ranked match queue: {}", e.getMessage());
        }
        return null;
    }

    public boolean isInRankedMatchQueue(long userId) {
        if (!isConnected()) return false;
        try {
            Double score = syncCommands.zscore("match:ranked", String.valueOf(userId));
            return score != null;
        } catch (Exception e) {
            log.warn("Failed to check ranked match queue: {}", e.getMessage());
            return false;
        }
    }

    public int getRankedMatchQueueSize() {
        if (!isConnected()) return 0;
        try {
            return syncCommands.zcard("match:ranked").intValue();
        } catch (Exception e) {
            log.warn("Failed to get ranked match queue size: {}", e.getMessage());
            return 0;
        }
    }

    // ==================== 通用 Key-Value 操作 ====================

    /**
     * 设置 key 的值并指定过期时间（秒）。
     */
    public void setex(String key, long seconds, String value) {
        if (!isConnected()) return;
        try {
            syncCommands.setex(key, seconds, value);
        } catch (Exception e) {
            log.warn("Failed to setex key {}: {}", key, e.getMessage());
        }
    }
    /** 设置 key 的值，不设置过期时间。 */
    public void set(String key, String value) {
        if (!isConnected()) return;
        try {
            syncCommands.set(key, value);
        } catch (Exception e) {
            log.warn("Failed to set key {}: {}", key, e.getMessage());
        }
    }

    /** 获取 key 的值。 */
    public String get(String key) {
        if (!isConnected()) return null;
        try {
            return syncCommands.get(key);
        } catch (Exception e) {
            log.warn("Failed to get key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 删除 key。
     */
    public void del(String key) {
        if (!isConnected()) return;
        try {
            syncCommands.del(key);
        } catch (Exception e) {
            log.warn("Failed to del key {}: {}", key, e.getMessage());
        }
    }

    /**
     * 检查 key 是否存在。
     */
    public boolean exists(String key) {
        if (!isConnected()) return false;
        try {
            return syncCommands.exists(key) == 1;
        } catch (Exception e) {
            log.warn("Failed to check exists key {}: {}", key, e.getMessage());
            return false;
        }
    }

    // ==================== 对战记录缓存 ====================

    private static final long RECORD_TTL_SECONDS = 3 * 24 * 3600; // 3天

    /**
     * 将对战记录缓存到 Redis，为双方玩家各存一份
     */
    public void saveGameRecord(long userId, String recordJson, long timestampMs) {
        if (!isConnected()) return;
        try {
            String key = "record:" + userId;
            syncCommands.zadd(key, (double) timestampMs, recordJson);
            syncCommands.expire(key, RECORD_TTL_SECONDS);
            log.debug("Game record cached in Redis: userId={}, key={}", userId, key);
        } catch (Exception e) {
            log.warn("Failed to cache game record in Redis: userId={}", userId, e.getMessage());
        }
    }

    /**
     * 从 Redis 获取用户对战记录（按时间倒序，支持分页）
     */
    public List<String> getGameRecords(long userId, int page, int size) {
        if (!isConnected()) return Collections.emptyList();
        try {
            String key = "record:" + userId;
            // zrevrange 按 score 倒序，最新的在前
            int start = page * size;
            int stop = start + size - 1;
            List<String> results = syncCommands.zrevrange(key, start, stop);
            // 续期 TTL
            if (results != null && !results.isEmpty()) {
                syncCommands.expire(key, RECORD_TTL_SECONDS);
            }
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get game records from Redis: userId={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}



