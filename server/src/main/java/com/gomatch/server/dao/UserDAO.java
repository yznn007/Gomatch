package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

public class UserDAO {
    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);

    public boolean register(String username, String password) {
        String sql = "INSERT INTO t_user (username, password) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Register failed", e);
            return false;
        }
    }

    public Long login(String username, String password) {
        String sql = "SELECT user_id FROM t_user WHERE username = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("user_id");
            }
        } catch (SQLException e) {
            log.error("Login failed", e);
        }
        return null;
    }
}
