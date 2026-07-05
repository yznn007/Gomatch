package com.gomatch.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    private static DatabaseConnectionManager instance;
    private HikariDataSource dataSource;

    private DatabaseConnectionManager() {
        initDataSource();
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    private void initDataSource() {
        try {
            Properties props = ConfigLoader.loadProperties("database.properties");
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("mysql.url"));
            config.setUsername(props.getProperty("mysql.username"));
            config.setPassword(props.getProperty("mysql.password"));
            config.setDriverClassName(props.getProperty("mysql.driver"));
            
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("mysql.pool.maxSize")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("mysql.pool.minIdle")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("mysql.pool.timeout")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("mysql.pool.idleTimeout")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("mysql.pool.maxLifetime")));
            
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            
            dataSource = new HikariDataSource(config);
            
            try (Connection conn = dataSource.getConnection()) {
                logger.info("Database connection pool initialized successfully");
                logger.info("MySQL connection: {}", props.getProperty("mysql.url"));
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        return conn;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}