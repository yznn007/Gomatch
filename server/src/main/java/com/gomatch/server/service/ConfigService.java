package com.gomatch.server.service;

import com.gomatch.server.dao.ConfigDAO;
import com.gomatch.server.redis.RedisManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Redis-first configuration cache with asynchronous MySQL write-behind on disconnect. */
public final class ConfigService {
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final ConfigService INSTANCE = new ConfigService();
    private static final String KEY_PREFIX = "gomatch:user:config:";
    private static final int MAX_CONFIG_BYTES = 16 * 1024;

    private final ConfigDAO configDAO = new ConfigDAO();
    private final ExecutorService flushExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "config-mysql-flush");
        thread.setDaemon(true);
        return thread;
    });

    private ConfigService() { }

    public static ConfigService getInstance() {
        return INSTANCE;
    }

    public String getConfigJson(long userId) {
        String key = key(userId);
        RedisManager redis = RedisManager.getInstance();
        String cached = redis.get(key);
        if (cached != null) {
            return cached;
        }
        String persisted = configDAO.findConfigJson(userId);
        if (persisted != null && redis.isConnected()) {
            redis.set(key, persisted);
        }
        return persisted;
    }

    public boolean updateConfig(long userId, String configJson) {
        if (configJson == null || configJson.isBlank()
                || configJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CONFIG_BYTES) {
            return false;
        }
        try {
            new JSONObject(configJson);
        } catch (Exception ignored) {
            return false;
        }
        RedisManager redis = RedisManager.getInstance();
        if (!redis.isConnected()) {
            return false;
        }
        redis.set(key(userId), configJson);
        return true;
    }

    public void flushAsync(long userId) {
        flushExecutor.submit(() -> {
            String configJson = RedisManager.getInstance().get(key(userId));
            if (configJson != null) {
                configDAO.upsertConfigJson(userId, configJson);
            }
        });
    }

    public void shutdown() {
        flushExecutor.shutdown();
        try {
            if (!flushExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                flushExecutor.shutdownNow();
            }
        } catch (InterruptedException error) {
            flushExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Configuration flush executor stopped");
    }

    private String key(long userId) {
        return KEY_PREFIX + userId;
    }
}