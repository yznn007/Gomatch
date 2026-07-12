package com.gomatch.client.network;

import java.io.*;
import java.util.Properties;

public class ClientConfig {
    private static final String DEFAULT_CONFIG = "client-config.json";
    private static final String LOCAL_CONFIG = "config.json";
    private static final Properties defaultProps = new Properties();
    private static final Properties localProps = new Properties();

    static {
        // 1. 加载随客户端发布的默认配置。
        try (InputStream is = ClientConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG)) {
            if (is != null) {
                defaultProps.load(is);
            }
        } catch (IOException ignored) {}

        // 2. 如存在本地配置，则仅覆盖其中显式设置的字段。
        File userHome = new File(System.getProperty("user.home"), ".gomatch");
        File localFile = new File(userHome, LOCAL_CONFIG);
        if (localFile.exists()) {
            try (InputStream is = new FileInputStream(localFile)) {
                localProps.load(is);
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
        }
    }

    public static String getHost() {
        return localProps.getProperty("server.host", getDefaultHost());
    }

    public static int getPort() {
        return parsePort(localProps.getProperty("server.port"), getDefaultPort());
    }

    public static String getDefaultHost() {
        return defaultProps.getProperty("server.host", "127.0.0.1");
    }

    public static int getDefaultPort() {
        return parsePort(defaultProps.getProperty("server.port"), 8888);
    }

    private static int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * 保存用户手动覆盖。host 为空时移除本地 IP 覆盖，恢复包内默认服务器。
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
}
