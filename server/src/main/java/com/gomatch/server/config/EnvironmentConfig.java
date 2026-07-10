package com.gomatch.server.config;

import java.util.Map;
import java.util.Properties;

/** Applies production environment overrides without removing local development defaults. */
public final class EnvironmentConfig {
    private static final Map<String, String> OVERRIDES = Map.ofEntries(
            Map.entry("mysql.url", "GOMATCH_DB_URL"),
            Map.entry("mysql.username", "GOMATCH_DB_USERNAME"),
            Map.entry("mysql.password", "GOMATCH_DB_PASSWORD"),
            Map.entry("redis.host", "GOMATCH_REDIS_HOST"),
            Map.entry("redis.port", "GOMATCH_REDIS_PORT"),
            Map.entry("redis.password", "GOMATCH_REDIS_PASSWORD"),
            Map.entry("oss.access-key-id", "OSS_ACCESS_KEY_ID"),
            Map.entry("oss.access-key-secret", "OSS_ACCESS_KEY_SECRET")
    );

    private EnvironmentConfig() {}

    public static Properties resolve(Properties source) {
        return resolve(source, System.getenv());
    }

    static Properties resolve(Properties source, Map<String, String> environment) {
        Properties resolved = new Properties();
        resolved.putAll(source);
        OVERRIDES.forEach((property, variable) -> {
            String value = environment.get(variable);
            if (value != null && !value.isBlank()) {
                resolved.setProperty(property, value);
            }
        });
        return resolved;
    }

    public static boolean usesDefaultDatabasePassword(Properties properties) {
        return "gomatch".equals(properties.getProperty("mysql.password", "gomatch"));
    }

    public static boolean usesEmptyRedisPassword(Properties properties) {
        return properties.getProperty("redis.password", "").isBlank();
    }
}
