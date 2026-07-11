package com.gomatch.server.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class StorageManager {
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);
    private static StorageManager instance;

    private OssService ossService;
    private boolean enabled;

    private StorageManager() {}

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    public void initialize(Properties config) {
        String endpoint = config.getProperty("oss.endpoint");
        String bucket = config.getProperty("oss.bucket");
        String accessKeyId = config.getProperty("oss.access-key-id");
        String accessKeySecret = config.getProperty("oss.access-key-secret");
        long avatarMaxSize = Long.parseLong(config.getProperty("oss.avatar-max-size", "2097152"));
        long musicMaxSize = Long.parseLong(config.getProperty("oss.music-max-size", "10485760"));

        if (isConfigured(endpoint) && isConfigured(bucket) && isConfigured(accessKeyId) && isConfigured(accessKeySecret)) {
            ossService = new OssService(endpoint, bucket, accessKeyId, accessKeySecret, avatarMaxSize, musicMaxSize);
            enabled = true;
            logger.info("StorageManager initialized with OSS endpoint: {}", endpoint);
        } else {
            enabled = false;
            logger.warn("StorageManager not enabled - missing OSS configuration");
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank() && !value.startsWith("${");
    }
    public OssService getOssService() {
        return ossService;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
