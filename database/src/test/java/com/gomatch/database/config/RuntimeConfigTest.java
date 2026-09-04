package com.gomatch.database.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeConfigTest {
    @Test
    void processEnvironmentOverridesDotEnvWhichOverridesClasspathProperties() {
        Properties base = new Properties();
        base.setProperty("mysql.username", "classpath-user");

        Properties resolved = RuntimeConfig.resolve(base,
                Map.of("GOMATCH_DB_USERNAME", "dotenv-user"),
                Map.of("GOMATCH_DB_USERNAME", "process-user"));

        assertEquals("process-user", resolved.getProperty("mysql.username"));
    }

    @Test
    void mapsRedisFieldsFromDotEnv() {
        Properties resolved = RuntimeConfig.resolve(new Properties(), Map.of(
                "GOMATCH_REDIS_HOST", "127.0.0.1",
                "GOMATCH_REDIS_PORT", "6379",
                "GOMATCH_REDIS_PASSWORD", "test-secret",
                "GOMATCH_REDIS_DATABASE", "2"), Map.of());

        assertEquals("127.0.0.1", resolved.getProperty("redis.host"));
        assertEquals("6379", resolved.getProperty("redis.port"));
        assertEquals("test-secret", resolved.getProperty("redis.password"));
        assertEquals("2", resolved.getProperty("redis.database"));
    }

    @Test
    void mapsMailFieldsFromDotEnv() {
        Properties resolved = RuntimeConfig.resolve(new Properties(), Map.of(
                "MAIL_HOST", "smtp.example.com",
                "MAIL_PORT", "465",
                "MAIL_USER", "mail-user",
                "MAIL_PASSWORD", "mail-secret",
                "MAIL_FROM", "mail-from"), Map.of());

        assertEquals("smtp.example.com", resolved.getProperty("mail.host"));
        assertEquals("465", resolved.getProperty("mail.port"));
        assertEquals("mail-user", resolved.getProperty("mail.user"));
        assertEquals("mail-secret", resolved.getProperty("mail.password"));
        assertEquals("mail-from", resolved.getProperty("mail.from"));
    }

    @Test
    void parsesCommentsAndValuesContainingEquals() throws Exception {
        Path envFile = Files.createTempFile("gomatch-runtime-config", ".env");
        Files.writeString(envFile, "# local settings\nGOMATCH_DB_PASSWORD=abc=123\n\n", StandardCharsets.UTF_8);

        assertEquals("abc=123", RuntimeConfig.loadDotEnv(envFile).get("GOMATCH_DB_PASSWORD"));
    }

    @Test
    void allowsRedisWithoutAuthentication() {
        Properties properties = new Properties();
        properties.setProperty("mysql.url", "jdbc:mysql://localhost/gomatch");
        properties.setProperty("mysql.username", "gomatch");
        properties.setProperty("mysql.password", "test-password");
        properties.setProperty("redis.host", "127.0.0.1");
        properties.setProperty("redis.port", "6379");

        RuntimeConfig.requireRuntimeCredentials(properties);
    }
    @Test
    void rejectsMissingRequiredCredential() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> RuntimeConfig.requireRuntimeCredentials(new Properties()));

        assertEquals("Missing required runtime configuration: GOMATCH_DB_URL", error.getMessage());
    }
}
