package com.gomatch.client.network;

import java.io.*;
import java.util.Properties;

public class ClientConfig {
    private static final String DEFAULT_CONFIG = "client-config.json";
    private static final String LOCAL_CONFIG = "config.json";
    private static final Properties props = new Properties();

    static {
        // 1. 先尝试从用户主目录加载本地配置文件 (config.json)
        File userHome = new File(System.getProperty("user.home"), ".gomatch");
        File localFile = new File(userHome, LOCAL_CONFIG);
        if (localFile.exists()) {
            try (InputStream is = new FileInputStream(localFile)) {
                props.load(is);
            } catch (IOException ignored) {}
        } else {
            // 2. 否则从类路径加载默认配置 (client-config.json)
            try (InputStream is = ClientConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG)) {
                if (is != null) {
                    props.load(is);
                }
            } catch (IOException ignored) {}
        }
    }

    public static String getHost() {
        return props.getProperty("server.host", "127.0.0.1");
    }

    public static int getPort() {
        return Integer.parseInt(props.getProperty("server.port", "8888"));
    }

    public static void saveConfig(String host, int port) {
        props.setProperty("server.host", host);
        props.setProperty("server.port", String.valueOf(port));
        File userHome = new File(System.getProperty("user.home"), ".gomatch");
        if (!userHome.exists()) {
            userHome.mkdirs();
        }
        File localFile = new File(userHome, LOCAL_CONFIG);
        try (OutputStream os = new FileOutputStream(localFile)) {
            props.store(os, "Client configuration updated");
        } catch (IOException ignored) {}
    }
}
