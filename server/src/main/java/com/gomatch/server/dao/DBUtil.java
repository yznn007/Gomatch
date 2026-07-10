package com.gomatch.server.dao;

import com.gomatch.server.config.EnvironmentConfig;
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
    private static HikariDataSource dataSource;
    private static Properties configProperties;

    public static void init() {
        Properties loaded = new Properties();
        loadResource(loaded, "database.properties", "using database defaults");
        loadResource(loaded, "oss.properties", "OSS storage will be disabled");
        configProperties = EnvironmentConfig.resolve(loaded);

        if (EnvironmentConfig.usesDefaultDatabasePassword(configProperties)) {
            log.warn("SECURITY: using the development MySQL password; set GOMATCH_DB_PASSWORD for deployment");
        }
        if (EnvironmentConfig.usesEmptyRedisPassword(configProperties)) {
            log.warn("SECURITY: Redis password is empty; restrict Redis to localhost or configure authentication");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(configProperties.getProperty("mysql.url",
                "jdbc:mysql://localhost:3306/gomatch?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"));
        config.setUsername(configProperties.getProperty("mysql.username", "gomatch"));
        config.setPassword(configProperties.getProperty("mysql.password", "gomatch"));
        config.setMaximumPoolSize(Integer.parseInt(configProperties.getProperty("mysql.pool.maxSize", "20")));
        config.setMinimumIdle(Integer.parseInt(configProperties.getProperty("mysql.pool.minIdle", "5")));
        config.setAutoCommit(true);
        dataSource = new HikariDataSource(config);
        log.info("Database pool initialized");
        StorageManager.getInstance().initialize(configProperties);
    }

    private static void loadResource(Properties target, String resource, String missingMessage) {
        try (InputStream input = DBUtil.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                log.warn("{} not found, {}", resource, missingMessage);
            } else {
                target.load(input);
            }
        } catch (Exception error) {
            log.error("Failed to load {}", resource, error);
        }
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
