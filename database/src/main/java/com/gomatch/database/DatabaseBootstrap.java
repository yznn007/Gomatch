package com.gomatch.database;

import com.gomatch.database.config.ConfigLoader;
import com.gomatch.database.config.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

public class DatabaseBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseBootstrap.class);

    public static void main(String[] args) {
        logger.info("Starting Gomatch Database Module...");
        
        try {
            Properties props = ConfigLoader.loadProperties("database.properties");
            logger.info("Loaded database configuration");
            
            DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();
            logger.info("Database connection pool initialized");
            
            initializeDatabase(dbManager);
            
            logger.info("Gomatch Database Module started successfully");
        } catch (Exception e) {
            logger.error("Failed to start Gomatch Database Module", e);
            System.exit(1);
        }
    }

    public static void initializeDatabase(DatabaseConnectionManager dbManager) {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            InputStream is = DatabaseBootstrap.class.getClassLoader().getResourceAsStream("sql/init.sql");
            if (is == null) {
                logger.warn("init.sql not found, skipping database initialization");
                return;
            }
            
            Scanner scanner = new Scanner(is, "UTF-8");
            scanner.useDelimiter(";");
            
            while (scanner.hasNext()) {
                String sql = scanner.next().trim();
                if (!sql.isEmpty()) {
                    try {
                        stmt.execute(sql);
                        logger.debug("Executed SQL: {}", sql.length() > 50 ? sql.substring(0, 50) + "..." : sql);
                    } catch (Exception e) {
                        logger.warn("SQL execution warning: {}", e.getMessage());
                    }
                }
            }
            
            conn.commit();
            logger.info("Database initialization completed");
            
        } catch (Exception e) {
            logger.error("Error initializing database", e);
        }
    }

    public static void shutdown() {
        logger.info("Shutting down Gomatch Database Module...");
        DatabaseConnectionManager.getInstance().close();
        logger.info("Gomatch Database Module shutdown completed");
    }
}