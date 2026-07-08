package com.gomatch.server.dao;

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

    public static void init() {
        Properties props = new Properties();
        try (InputStream is = DBUtil.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                log.warn("database.properties not found in classpath, using defaults");
            }
        } catch (Exception e) {
            log.error("Failed to load database.properties", e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("mysql.url", "jdbc:mysql://localhost:3306/gomatch?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"));
        config.setUsername(props.getProperty("mysql.username", "gomatch"));
        config.setPassword(props.getProperty("mysql.password", "gomatch"));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("mysql.pool.maxSize", "20")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("mysql.pool.minIdle", "5")));
        config.setAutoCommit(true);
        ds = new HikariDataSource(config);
        log.info("Database pool initialized");
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void close() {
        if (ds != null) ds.close();
    }
}
