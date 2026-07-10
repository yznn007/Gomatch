package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Persists the durable per-user configuration JSON. */
public final class ConfigDAO {
    private static final Logger log = LoggerFactory.getLogger(ConfigDAO.class);

    public String findConfigJson(long userId) {
        String sql = "SELECT config_json FROM t_user_profile WHERE user_id = ?";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("config_json") : null;
            }
        } catch (SQLException error) {
            log.error("Failed to load cloud config: userId={}", userId, error);
            return null;
        }
    }

    public void upsertConfigJson(long userId, String configJson) {
        String sql = "INSERT INTO t_user_profile (user_id, config_json) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE config_json = VALUES(config_json), update_time = CURRENT_TIMESTAMP";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, configJson);
            statement.executeUpdate();
        } catch (SQLException error) {
            log.error("Failed to persist cloud config: userId={}", userId, error);
        }
    }
}