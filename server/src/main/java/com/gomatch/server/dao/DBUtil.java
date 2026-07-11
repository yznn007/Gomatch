package com.gomatch.server.dao;

import com.gomatch.database.config.RuntimeConfig;
import com.gomatch.server.storage.StorageManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static final Logger log = LoggerFactory.getLogger(DBUtil.class);
    private static HikariDataSource dataSource;
    private static Properties configProperties;

    public static void init() {
        configProperties = RuntimeConfig.loadRuntimeProperties("database.properties");
        RuntimeConfig.requireRuntimeCredentials(configProperties);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(configProperties.getProperty("mysql.url"));
        config.setUsername(configProperties.getProperty("mysql.username"));
        config.setPassword(configProperties.getProperty("mysql.password"));
        config.setMaximumPoolSize(Integer.parseInt(configProperties.getProperty("mysql.pool.maxSize", "20")));
        config.setMinimumIdle(Integer.parseInt(configProperties.getProperty("mysql.pool.minIdle", "5")));
        config.setAutoCommit(true);
        dataSource = new HikariDataSource(config);
        log.info("Database pool initialized");
        StorageManager.getInstance().initialize(configProperties);
    }


    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public static Properties getConfigProps() {
        return configProperties;
    }

    public static Properties getConfig() {
        return configProperties;
    }
}
