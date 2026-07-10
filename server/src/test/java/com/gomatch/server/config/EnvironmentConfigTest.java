package com.gomatch.server.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentConfigTest {
    @Test
    void environmentOverridesDevelopmentDefaults() {
        Properties source = new Properties();
        source.setProperty("mysql.password", "gomatch");
        source.setProperty("redis.password", "");

        Properties resolved = EnvironmentConfig.resolve(source, Map.of(
                "GOMATCH_DB_PASSWORD", "production-secret",
                "GOMATCH_REDIS_PASSWORD", "redis-secret"));

        assertEquals("production-secret", resolved.getProperty("mysql.password"));
        assertEquals("redis-secret", resolved.getProperty("redis.password"));
        assertFalse(EnvironmentConfig.usesDefaultDatabasePassword(resolved));
        assertFalse(EnvironmentConfig.usesEmptyRedisPassword(resolved));
    }

    @Test
    void reportsWeakFallbacks() {
        Properties source = new Properties();
        source.setProperty("mysql.password", "gomatch");
        source.setProperty("redis.password", "");

        assertTrue(EnvironmentConfig.usesDefaultDatabasePassword(source));
        assertTrue(EnvironmentConfig.usesEmptyRedisPassword(source));
    }
}
