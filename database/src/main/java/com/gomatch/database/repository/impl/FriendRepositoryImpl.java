package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.Friend;
import com.gomatch.database.repository.FriendRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendRepositoryImpl implements FriendRepository {
    private static final Logger logger = LoggerFactory.getLogger(FriendRepositoryImpl.class);

    @Override
    public Friend findById(Long friendId) {
        String sql = "SELECT * FROM t_friend WHERE friend_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, friendId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFriend(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding friend by id: {}", friendId, e);
        }
        return null;
    }

    @Override
    public List<Friend> findByUserId(Long userId) {
        String sql = "SELECT * FROM t_friend WHERE user_id = ?";
        return executeQueryWithParam(sql, userId);
    }

    @Override
    public List<Friend> findByFriendUserId(Long friendUserId) {
        String sql = "SELECT * FROM t_friend WHERE friend_user_id = ?";
        return executeQueryWithParam(sql, friendUserId);
    }

    @Override
    public Friend findByUserIdAndFriendUserId(Long userId, Long friendUserId) {
        String sql = "SELECT * FROM t_friend WHERE user_id = ? AND friend_user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, friendUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFriend(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding friend by user_id: {} and friend_user_id: {}", userId, friendUserId, e);
        }
        return null;
    }

    @Override
    public List<Friend> findByRelationType(String relationType) {
        String sql = "SELECT * FROM t_friend WHERE relation_type = ?";
        return executeQueryWithParam(sql, relationType);
    }

    @Override
    public List<Friend> findAll() {
        String sql = "SELECT * FROM t_friend";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all friends", e);
        }
        return new ArrayList<>();
    }

    @Override
    public Friend save(Friend friend) {
        String sql = "INSERT INTO t_friend (user_id, friend_user_id, relation_type, remark) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, friend.getUserId());
            stmt.setLong(2, friend.getFriendUserId());
            stmt.setString(3, friend.getRelationType());
            stmt.setString(4, friend.getRemark());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                friend.setFriendId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted friend: {}", friend.getFriendId());
        } catch (SQLException e) {
            logger.error("Error inserting friend", e);
        }
        return friend;
    }

    @Override
    public int updateRemark(Long friendId, String remark) {
        String sql = "UPDATE t_friend SET remark = ? WHERE friend_id = ?";
        return executeUpdateWithParams(sql, remark, friendId);
    }

    @Override
    public int deleteById(Long friendId) {
        String sql = "DELETE FROM t_friend WHERE friend_id = ?";
        return executeUpdateWithParams(sql, friendId);
    }

    @Override
    public int deleteByUserIdAndFriendUserId(Long userId, Long friendUserId) {
        String sql = "DELETE FROM t_friend WHERE user_id = ? AND friend_user_id = ?";
        return executeUpdateWithParams(sql, userId, friendUserId);
    }

    @Override
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM t_friend WHERE user_id = ?";
        return executeCountQuery(sql, userId);
    }

    @Override
    public boolean existsByUserIdAndFriendUserId(Long userId, Long friendUserId) {
        String sql = "SELECT COUNT(*) FROM t_friend WHERE user_id = ? AND friend_user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, friendUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking friend existence", e);
        }
        return false;
    }

    private List<Friend> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<Friend> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<Friend> friends = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            friends.add(mapResultSetToFriend(rs));
        }
        return friends;
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

    private Friend mapResultSetToFriend(ResultSet rs) throws SQLException {
        Friend friend = new Friend();
        friend.setFriendId(rs.getLong("friend_id"));
        friend.setUserId(rs.getLong("user_id"));
        friend.setFriendUserId(rs.getLong("friend_user_id"));
        friend.setRelationType(rs.getString("relation_type"));
        friend.setRemark(rs.getString("remark"));
        friend.setCreateTime(rs.getTimestamp("create_time"));
        return friend;
    }
}