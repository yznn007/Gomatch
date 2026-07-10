package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.FriendRequest;
import com.gomatch.database.repository.FriendRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FriendRequestRepositoryImpl implements FriendRequestRepository {
    private static final Logger logger = LoggerFactory.getLogger(FriendRequestRepositoryImpl.class);

    @Override
    public FriendRequest findById(Long requestId) {
        String sql = "SELECT * FROM t_friend_request WHERE request_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, requestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFriendRequest(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding friend request by id: {}", requestId, e);
        }
        return null;
    }

    @Override
    public List<FriendRequest> findByFromUserId(Long fromUserId) {
        String sql = "SELECT * FROM t_friend_request WHERE from_user_id = ?";
        return executeQueryWithParam(sql, fromUserId);
    }

    @Override
    public List<FriendRequest> findByToUserId(Long toUserId) {
        String sql = "SELECT * FROM t_friend_request WHERE to_user_id = ?";
        return executeQueryWithParam(sql, toUserId);
    }

    @Override
    public FriendRequest findByFromUserIdAndToUserId(Long fromUserId, Long toUserId) {
        String sql = "SELECT * FROM t_friend_request WHERE from_user_id = ? AND to_user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, fromUserId);
            stmt.setLong(2, toUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToFriendRequest(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding friend request by from_user_id: {} and to_user_id: {}", fromUserId, toUserId, e);
        }
        return null;
    }

    @Override
    public List<FriendRequest> findByStatus(String status) {
        String sql = "SELECT * FROM t_friend_request WHERE status = ?";
        return executeQueryWithParam(sql, status);
    }

    @Override
    public List<FriendRequest> findAll() {
        String sql = "SELECT * FROM t_friend_request";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all friend requests", e);
        }
        return new ArrayList<>();
    }

    @Override
    public FriendRequest save(FriendRequest request) {
        String sql = "INSERT INTO t_friend_request (from_user_id, to_user_id, status, message) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, request.getFromUserId());
            stmt.setLong(2, request.getToUserId());
            stmt.setString(3, request.getStatus());
            stmt.setString(4, request.getMessage());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                request.setRequestId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted friend request: {}", request.getRequestId());
        } catch (SQLException e) {
            logger.error("Error inserting friend request", e);
        }
        return request;
    }

    @Override
    public int updateStatus(Long requestId, String status) {
        String sql = "UPDATE t_friend_request SET status = ?, respond_time = NOW() WHERE request_id = ?";
        return executeUpdateWithParams(sql, status, requestId);
    }

    @Override
    public int updateRespondTime(Long requestId, Date respondTime) {
        String sql = "UPDATE t_friend_request SET respond_time = ? WHERE request_id = ?";
        return executeUpdateWithParams(sql, new Timestamp(respondTime.getTime()), requestId);
    }

    @Override
    public int deleteById(Long requestId) {
        String sql = "DELETE FROM t_friend_request WHERE request_id = ?";
        return executeUpdateWithParams(sql, requestId);
    }

    @Override
    public int countByToUserId(Long toUserId) {
        String sql = "SELECT COUNT(*) FROM t_friend_request WHERE to_user_id = ? AND status = 'PENDING'";
        return executeCountQuery(sql, toUserId);
    }

    @Override
    public boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId) {
        String sql = "SELECT COUNT(*) FROM t_friend_request WHERE from_user_id = ? AND to_user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, fromUserId);
            stmt.setLong(2, toUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking friend request existence", e);
        }
        return false;
    }

    private List<FriendRequest> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<FriendRequest> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<FriendRequest> requests = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            requests.add(mapResultSetToFriendRequest(rs));
        }
        return requests;
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

    private FriendRequest mapResultSetToFriendRequest(ResultSet rs) throws SQLException {
        FriendRequest request = new FriendRequest();
        request.setRequestId(rs.getLong("request_id"));
        request.setFromUserId(rs.getLong("from_user_id"));
        request.setToUserId(rs.getLong("to_user_id"));
        request.setStatus(rs.getString("status"));
        request.setMessage(rs.getString("message"));
        request.setCreateTime(rs.getTimestamp("create_time"));
        request.setRespondTime(rs.getTimestamp("respond_time"));
        return request;
    }
}