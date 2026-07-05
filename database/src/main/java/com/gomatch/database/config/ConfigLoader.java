package com.gomatch.database.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    public static Properties loadProperties(String fileName) {
        Properties props = new Properties();
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                logger.error("Config file not found: {}", fileName);
                throw new RuntimeException("Config file not found: " + fileName);
            }
            props.load(is);
            logger.info("Loaded config file: {}", fileName);
        } catch (IOException e) {
            logger.error("Failed to load config file: {}", fileName, e);
            throw new RuntimeException("Failed to load config file: " + fileName, e);
        }
        return props;
    }
}