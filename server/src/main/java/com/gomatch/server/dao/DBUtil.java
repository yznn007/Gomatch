package com.gomatch.server.dao;

import com.gomatch.server.storage.StorageManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static final Logger log = LoggerFactory.getLogger(DBUtil.class);
    private static HikariDataSource ds;
    private static Properties configProps;

    public static void init() {
        configProps = new Properties();
        try (InputStream is = DBUtil.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (is != null) {
                configProps.load(is);
            } else {
                log.warn("database.properties not found in classpath, using defaults");
            }
        } catch (Exception e) {
            log.error("Failed to load database.properties", e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(configProps.getProperty("mysql.url", "jdbc:mysql://localhost:3306/gomatch?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"));
        config.setUsername(configProps.getProperty("mysql.username", "gomatch"));
        config.setPassword(configProps.getProperty("mysql.password", "gomatch"));
        config.setMaximumPoolSize(Integer.parseInt(configProps.getProperty("mysql.pool.maxSize", "20")));
        config.setMinimumIdle(Integer.parseInt(configProps.getProperty("mysql.pool.minIdle", "5")));
        config.setAutoCommit(true);
        ds = new HikariDataSource(config);
        log.info("Database pool initialized");

        StorageManager.getInstance().initialize(configProps);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void close() {
        if (ds != null) ds.close();
    }

    public static Properties getConfigProps() {
        return configProps;
    }
}
