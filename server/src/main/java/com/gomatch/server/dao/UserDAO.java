package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);
    private final RankDAO rankDAO = new RankDAO();

    public static class UserInfo {
        private final long userId;
        private final String username;
        private final int score;
        private final int winCount;
        private final int loseCount;
        private final int drawCount;

        public UserInfo(long userId, String username, int score,
                        int winCount, int loseCount, int drawCount) {
            this.userId = userId;
            this.username = username;
            this.score = score;
            this.winCount = winCount;
            this.loseCount = loseCount;
            this.drawCount = drawCount;
        }

        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public int getScore() { return score; }
        public int getWinCount() { return winCount; }
        public int getLoseCount() { return loseCount; }
        public int getDrawCount() { return drawCount; }
    }

    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("Register failed: empty username");
            return false;
        }
        if (password == null || password.length() < 6) {
            log.warn("Register failed: password too short");
            return false;
        }
        if (username.length() > 16) {
            log.warn("Register failed: username too long");
            return false;
        }
        if (existsByUsername(username)) {
            log.warn("Register failed: username already exists: {}", username);
            return false;
        }

        String sql = "INSERT INTO t_user (username, password) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim());
            ps.setString(2, password);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long userId = rs.getLong(1);
                    rankDAO.upsertRank(userId, 1000, 0, 0, 0);
                }
            }
            log.info("User registered: {}", username);
            return true;
        } catch (SQLException e) {
            log.error("Register SQL error: {}", username, e);
            return false;
        }
    }

    public UserInfo login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        String sql = "SELECT user_id, username, score, win_count, lose_count, draw_count " +
                     "FROM t_user WHERE username = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserInfo(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Login SQL error: {}", username, e);
        }
        return null;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM t_user WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Check username error: {}", username, e);
        }
        return false;
    }

    public UserInfo getUserById(long userId) {
        String sql = "SELECT user_id, username, score, win_count, lose_count, draw_count " +
                     "FROM t_user WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserInfo(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Get user by id error: {}", userId, e);
        }
        return null;
    }

    public boolean existsActiveUser(long userId) {
        String sql = "SELECT 1 FROM t_user WHERE user_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Check active user error: {}", userId, e);
        }
        return false;
    }

    public boolean updateScore(long userId, int scoreDelta) {
        String sql = "UPDATE t_user SET score = score + ? WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scoreDelta);
            ps.setLong(2, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                rankDAO.updateScore(userId, scoreDelta);
            }
            return success;
        } catch (SQLException e) {
            log.error("Update score error: userId={}", userId, e);
            return false;
        }
    }

    public void updateWinCount(long userId) {
        String sql = "UPDATE t_user SET win_count = win_count + 1, update_time = NOW() WHERE user_id = ?";
        executeUpdate(sql, userId);
    }

    public void updateLoseCount(long userId) {
        String sql = "UPDATE t_user SET lose_count = lose_count + 1, update_time = NOW() WHERE user_id = ?";
        executeUpdate(sql, userId);
    }

    public void updateDrawCount(long userId) {
        String sql = "UPDATE t_user SET draw_count = draw_count + 1, update_time = NOW() WHERE user_id = ?";
        executeUpdate(sql, userId);
    }

    public boolean changePassword(long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            log.warn("Change password failed: new password too short");
            return false;
        }

        String checkSql = "SELECT password FROM t_user WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.warn("Change password failed: user not found, userId={}", userId);
                    return false;
                }
                String currentPassword = rs.getString("password");
                if (!newPassword.equals(oldPassword) && !oldPassword.equals(currentPassword)) {
                    log.warn("Change password failed: old password mismatch, userId={}", userId);
                    return false;
                }
            }
        } catch (SQLException e) {
            log.error("Check password error: userId={}", userId, e);
            return false;
        }

        String updateSql = "UPDATE t_user SET password = ?, update_time = NOW() WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newPassword);
            ps.setLong(2, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                log.info("Password changed successfully: userId={}", userId);
            }
            return success;
        } catch (SQLException e) {
            log.error("Change password error: userId={}", userId, e);
            return false;
        }
    }

    private void executeUpdate(String sql, long userId) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Execute update error: userId={}", userId, e);
        }
    }

    public List<UserInfo> getRanking(int limit) {
        String sql = "SELECT user_id, username, score, win_count, lose_count, draw_count " +
                     "FROM t_user WHERE status = 'ACTIVE' " +
                     "ORDER BY score DESC LIMIT ?";
        List<UserInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new UserInfo(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get ranking error", e);
        }
        return result;
    }
}