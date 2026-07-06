package com.gomatch.client.network;

import java.io.*;
import java.util.Properties;

public class ClientConfig {
    private static final String CONFIG_FILE = "client-config.properties";
    private static final Properties props = new Properties();

    static {
        // 1. 先尝试从当前工作目录加载本地配置文件
        File localFile = new File(CONFIG_FILE);
        if (localFile.exists()) {
            try (InputStream is = new FileInputStream(localFile)) {
                props.load(is);
            } catch (IOException ignored) {}
        } else {
            // 2. 否则从类路径加载默认配置
            try (InputStream is = ClientConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
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
        File localFile = new File(CONFIG_FILE);
        try (OutputStream os = new FileOutputStream(localFile)) {
            props.store(os, "Client configuration updated");
        } catch (IOException ignored) {}
    }
}
