package com.gomatch.database.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Resolves non-committed runtime configuration from .env and process variables. */
public final class RuntimeConfig {
    private static final Map<String, String> VARIABLES = Map.ofEntries(
            Map.entry("mysql.url", "GOMATCH_DB_URL"),
            Map.entry("mysql.username", "GOMATCH_DB_USERNAME"),
            Map.entry("mysql.password", "GOMATCH_DB_PASSWORD"),
            Map.entry("redis.host", "GOMATCH_REDIS_HOST"),
            Map.entry("redis.port", "GOMATCH_REDIS_PORT"),
            Map.entry("redis.password", "GOMATCH_REDIS_PASSWORD"),
            Map.entry("redis.database", "GOMATCH_REDIS_DATABASE"),
            Map.entry("oss.endpoint", "OSS_ENDPOINT"),
            Map.entry("oss.bucket", "OSS_BUCKET"),
            Map.entry("oss.access-key-id", "OSS_ACCESS_KEY_ID"),
            Map.entry("oss.access-key-secret", "OSS_ACCESS_KEY_SECRET")
    );

    private static final List<Map.Entry<String, String>> REQUIRED_VARIABLES = List.of(
            Map.entry("mysql.url", "GOMATCH_DB_URL"),
            Map.entry("mysql.username", "GOMATCH_DB_USERNAME"),
            Map.entry("mysql.password", "GOMATCH_DB_PASSWORD"),
            Map.entry("redis.host", "GOMATCH_REDIS_HOST"),
            Map.entry("redis.port", "GOMATCH_REDIS_PORT")
    );

    private RuntimeConfig() {
    }

    public static Properties loadRuntimeProperties(String resourceName) {
        Properties base = loadClasspathProperties(resourceName);
        Map<String, String> dotEnv = loadDotEnv(resolveDotEnvPath(System.getenv()));
        return resolve(base, dotEnv, System.getenv());
    }

    public static Properties resolve(Properties base, Map<String, String> dotEnv, Map<String, String> environment) {
        Properties resolved = new Properties();
        resolved.putAll(base);
        apply(resolved, dotEnv);
        apply(resolved, environment);
        return resolved;
    }

    public static Map<String, String> loadDotEnv(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return Map.of();
        }

        try {
            Map<String, String> values = new java.util.LinkedHashMap<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    throw new IllegalArgumentException("Malformed .env entry in " + path.getFileName());
                }
                values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
            return values;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read runtime configuration file: " + path, error);
        }
    }

    public static void requireRuntimeCredentials(Properties properties) {
        for (Map.Entry<String, String> required : REQUIRED_VARIABLES) {
            String value = properties.getProperty(required.getKey());
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing required runtime configuration: " + required.getValue());
            }
        }
    }

    private static Properties loadClasspathProperties(String resourceName) {
        Properties properties = new Properties();
        try (InputStream input = RuntimeConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input != null) {
                properties.load(input);
            }
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read classpath configuration: " + resourceName, error);
        }
    }

    private static Path resolveDotEnvPath(Map<String, String> environment) {
        String configuredPath = environment.get("GOMATCH_ENV_FILE");
        return configuredPath == null || configuredPath.isBlank()
                ? Path.of(System.getProperty("user.dir"), ".env")
                : Path.of(configuredPath);
    }

    private static void apply(Properties target, Map<String, String> source) {
        VARIABLES.forEach((property, variable) -> {
            String value = source.get(variable);
            if (value != null && !value.isBlank()) {
                target.setProperty(property, value);
            }
        });
    }
}
