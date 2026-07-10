package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Minimal authoritative account state used during token reconnect. */
public final class UserAuthStateDAO {
    private static final Logger log = LoggerFactory.getLogger(UserAuthStateDAO.class);

    public AuthState find(long userId) {
        String sql = "SELECT role, status FROM t_user WHERE user_id = ?";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new AuthState(result.getString("role"), result.getString("status"));
                }
            }
        } catch (SQLException error) {
            log.error("Read auth state failed: userId={}", userId, error);
        }
        return null;
    }

    public record AuthState(String role, String status) {
        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }
}
