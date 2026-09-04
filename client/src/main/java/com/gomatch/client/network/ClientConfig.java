package com.gomatch.client.network;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ClientConfig {
    private static final String DEFAULT_CONFIG = "client-config.json";
    private static final String LOCAL_CONFIG = "config.json";
    private static final String CLIENT_ENV_FILE_VARIABLE = "GOMATCH_CLIENT_ENV_FILE";
    private static final String ENV_FILE_VARIABLE = "GOMATCH_ENV_FILE";
    private static final String SERVER_HOST_VARIABLE = "GOMATCH_SERVER_HOST";
    private static final String SERVER_PORT_VARIABLE = "GOMATCH_SERVER_PORT";
    private static final Properties defaultProps = new Properties();
    private static final Properties dotEnvProps = new Properties();
    private static final Properties localProps = new Properties();

    static {
        // 1. 加载随客户端发布的默认配置。
        try (InputStream is = ClientConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG)) {
            if (is != null) {
                defaultProps.load(is);
            }
        } catch (IOException ignored) {}

        // 2. 加载客户端 .env；进程环境变量在读取时拥有更高优先级。
        loadDotEnv();

        // 3. 如存在本地配置，则读取用户显式保存的字段。
        File userHome = new File(System.getProperty("user.home"), ".gomatch");
        File localFile = new File(userHome, LOCAL_CONFIG);
        if (localFile.exists()) {
            try (InputStream is = new FileInputStream(localFile)) {
                localProps.load(is);
            } catch (IOException e) {
                System.err.println("读取客户端配置文件失败: " + e.getMessage());
            }
        }
    }

    public static String getHost() {
        return firstNonBlank(
                System.getenv(SERVER_HOST_VARIABLE),
                dotEnvProps.getProperty(SERVER_HOST_VARIABLE),
                localProps.getProperty("server.host"),
                getDefaultHost());
    }

    public static int getPort() {
        String configuredPort = firstNonBlank(
                System.getenv(SERVER_PORT_VARIABLE),
                dotEnvProps.getProperty(SERVER_PORT_VARIABLE));
        if (configuredPort != null) {
            return parsePort(configuredPort, getDefaultPort());
        }
        return parsePort(localProps.getProperty("server.port"), getDefaultPort());
    }

    public static String getDefaultHost() {
        return firstNonBlank(
                System.getenv(SERVER_HOST_VARIABLE),
                dotEnvProps.getProperty(SERVER_HOST_VARIABLE),
                defaultProps.getProperty("server.host"),
                "127.0.0.1");
    }

    public static int getDefaultPort() {
        String configuredPort = firstNonBlank(
                System.getenv(SERVER_PORT_VARIABLE),
                dotEnvProps.getProperty(SERVER_PORT_VARIABLE));
        return parsePort(configuredPort, parsePort(defaultProps.getProperty("server.port"), 8888));
    }

    private static int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * 保存用户手动覆盖。host 为空时移除本地 IP 覆盖，恢复 .env 或包内默认服务器；
     * 若已配置 GOMATCH_SERVER_HOST/GOMATCH_SERVER_PORT，运行时配置优先于此本地覆盖。
     */
    public static void saveConfig(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            localProps.remove("server.host");
        } else {
            localProps.setProperty("server.host", host.trim());
        }
        localProps.setProperty("server.port", String.valueOf(port));

        File userHome = new File(System.getProperty("user.home"), ".gomatch");
        if (!userHome.exists()) {
            userHome.mkdirs();
        }
        File localFile = new File(userHome, LOCAL_CONFIG);
        try (OutputStream os = new FileOutputStream(localFile)) {
            localProps.store(os, "Client configuration updated");
        } catch (IOException ignored) {}
    }

    private static void loadDotEnv() {
        Path envFile = resolveDotEnvPath();
        if (!Files.isRegularFile(envFile)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    System.err.println("忽略格式错误的 .env 配置: " + envFile.getFileName());
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                if (!SERVER_HOST_VARIABLE.equals(key) && !SERVER_PORT_VARIABLE.equals(key)) {
                    continue;
                }
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                dotEnvProps.setProperty(key, value);
            }
        } catch (IOException e) {
            System.err.println("读取 .env 失败: " + e.getMessage());
        }
    }

    private static Path resolveDotEnvPath() {
        String configuredPath = firstNonBlank(
                System.getenv(CLIENT_ENV_FILE_VARIABLE),
                System.getenv(ENV_FILE_VARIABLE));
        return configuredPath == null || configuredPath.isBlank()
                ? Path.of(System.getProperty("user.dir"), ".env")
                : Path.of(configuredPath);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
