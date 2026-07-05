package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.User;
import com.gomatch.database.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);

    @Override
    public User findById(Long userId) {
        String sql = "SELECT * FROM t_user WHERE user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", userId, e);
        }
        return null;
    }

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM t_user WHERE username = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT * FROM t_user WHERE email = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email: {}", email, e);
        }
        return null;
    }

    @Override
    public List<User> findByRole(String role) {
        String sql = "SELECT * FROM t_user WHERE role = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding users by role: {}", role, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<User> findByStatus(String status) {
        String sql = "SELECT * FROM t_user WHERE status = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding users by status: {}", status, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<User> findByRoleAndStatus(String role, String status) {
        String sql = "SELECT * FROM t_user WHERE role = ? AND status = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setString(2, status);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding users by role: {} and status: {}", role, status, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM t_user";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all users", e);
        }
        return new ArrayList<>();
    }

    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            return insert(user);
        } else {
            update(user);
            return user;
        }
    }

    private User insert(User user) {
        String sql = "INSERT INTO t_user (username, password, email, avatar, role, status, score, " +
                "win_count, lose_count, draw_count, ai_win_count, ai_lose_count, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getAvatar());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getStatus());
            stmt.setInt(7, user.getScore());
            stmt.setInt(8, user.getWinCount());
            stmt.setInt(9, user.getLoseCount());
            stmt.setInt(10, user.getDrawCount());
            stmt.setInt(11, user.getAiWinCount());
            stmt.setInt(12, user.getAiLoseCount());
            stmt.setInt(13, user.getVersion());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                user.setUserId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted user: {}", user.getUserId());
        } catch (SQLException e) {
            logger.error("Error inserting user: {}", user.getUsername(), e);
        }
        return user;
    }

    @Override
    public int update(User user) {
        String sql = "UPDATE t_user SET username = ?, password = ?, email = ?, avatar = ?, " +
                "role = ?, status = ?, score = ?, win_count = ?, lose_count = ?, draw_count = ?, " +
                "ai_win_count = ?, ai_lose_count = ?, version = version + 1 WHERE user_id = ? AND version = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getAvatar());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getStatus());
            stmt.setInt(7, user.getScore());
            stmt.setInt(8, user.getWinCount());
            stmt.setInt(9, user.getLoseCount());
            stmt.setInt(10, user.getDrawCount());
            stmt.setInt(11, user.getAiWinCount());
            stmt.setInt(12, user.getAiLoseCount());
            stmt.setLong(13, user.getUserId());
            stmt.setInt(14, user.getVersion());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated user: {}, rows affected: {}", user.getUserId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getUserId(), e);
        }
        return 0;
    }

    @Override
    public int updateRole(Long userId, String role) {
        String sql = "UPDATE t_user SET role = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, role, userId);
    }

    @Override
    public int updateStatus(Long userId, String status) {
        String sql = "UPDATE t_user SET status = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, status, userId);
    }

    @Override
    public int updateScore(Long userId, int delta) {
        String sql = "UPDATE t_user SET score = score + ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, delta, userId);
    }

    @Override
    public int updateWinCount(Long userId, int delta) {
        String sql = "UPDATE t_user SET win_count = win_count + ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, delta, userId);
    }

    @Override
    public int updateLoseCount(Long userId, int delta) {
        String sql = "UPDATE t_user SET lose_count = lose_count + ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, delta, userId);
    }

    @Override
    public int updateDrawCount(Long userId, int delta) {
        String sql = "UPDATE t_user SET draw_count = draw_count + ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, delta, userId);
    }

    @Override
    public int updateAiWinCount(Long userId, int delta) {
        String sql = "UPDATE t_user SET ai_win_count = ai_win_count + ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, delta, userId);
    }

    @Override
    public int updateAiLoseCount(Long userId, int delta) {
        String sql = "UPDATE t_user SET ai_lose_count = ai_lose_count + ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, delta, userId);
    }

    @Override
    public int deleteById(Long userId) {
        String sql = "DELETE FROM t_user WHERE user_id = ?";
        return executeUpdateWithParams(sql, userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM t_user WHERE username = ?";
        return executeCountQuery(sql, username) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM t_user WHERE email = ?";
        return executeCountQuery(sql, email) > 0;
    }

    @Override
    public int countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM t_user WHERE role = ?";
        return executeCountQuery(sql, role);
    }

    @Override
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM t_user WHERE status = ?";
        return executeCountQuery(sql, status);
    }

    private List<User> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<User> users = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        return users;
    }

    private int executeUpdateWithParams(String sql, Object... params) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            int rows = stmt.executeUpdate();
            conn.commit();
            return rows;
        } catch (SQLException e) {
            logger.error("Error executing update: {}", sql, e);
        }
        return 0;
    }

    private int executeCountQuery(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error executing count query: {}", sql, e);
        }
        return 0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setAvatar(rs.getString("avatar"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        user.setScore(rs.getInt("score"));
        user.setWinCount(rs.getInt("win_count"));
        user.setLoseCount(rs.getInt("lose_count"));
        user.setDrawCount(rs.getInt("draw_count"));
        user.setAiWinCount(rs.getInt("ai_win_count"));
        user.setAiLoseCount(rs.getInt("ai_lose_count"));
        user.setVersion(rs.getInt("version"));
        user.setCreateTime(rs.getTimestamp("create_time"));
        user.setUpdateTime(rs.getTimestamp("update_time"));
        return user;
    }
}