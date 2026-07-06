package com.gomatch.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {
    private static final String CONFIG_FILE = "client-config.properties";
    private static final Properties props = new Properties();

    static {
        try (InputStream is = ClientConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {
        }
    }

    public static String getHost() {
        return props.getProperty("server.host", "127.0.0.1");
    }

    public static int getPort() {
        return Integer.parseInt(props.getProperty("server.port", "8888"));
    }
}
