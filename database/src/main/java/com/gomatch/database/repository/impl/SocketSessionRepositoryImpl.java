package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.SocketSession;
import com.gomatch.database.repository.SocketSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SocketSessionRepositoryImpl implements SocketSessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(SocketSessionRepositoryImpl.class);

    @Override
    public SocketSession findById(Long sessionId) {
        String sql = "SELECT * FROM t_socket_session WHERE session_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSocketSession(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding socket session by id: {}", sessionId, e);
        }
        return null;
    }

    @Override
    public SocketSession findBySessionKey(String sessionKey) {
        String sql = "SELECT * FROM t_socket_session WHERE session_key = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionKey);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSocketSession(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding socket session by key: {}", sessionKey, e);
        }
        return null;
    }

    @Override
    public SocketSession findByUserId(Long userId) {
        String sql = "SELECT * FROM t_socket_session WHERE user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSocketSession(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding socket session by user id: {}", userId, e);
        }
        return null;
    }

    @Override
    public List<SocketSession> findByStatus(String status) {
        String sql = "SELECT * FROM t_socket_session WHERE status = ?";
        return executeQueryWithParam(sql, status);
    }

    @Override
    public List<SocketSession> findByRoomId(Long roomId) {
        String sql = "SELECT * FROM t_socket_session WHERE room_id = ?";
        return executeQueryWithParam(sql, roomId);
    }

    @Override
    public List<SocketSession> findExpiredSessions(Date timeout) {
        String sql = "SELECT * FROM t_socket_session WHERE status = 'connected' AND last_heartbeat < ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(timeout.getTime()));
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding expired sessions", e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<SocketSession> findAll() {
        String sql = "SELECT * FROM t_socket_session";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all socket sessions", e);
        }
        return new ArrayList<>();
    }

    @Override
    public SocketSession save(SocketSession session) {
        if (session.getSessionId() == null) {
            return insert(session);
        } else {
            update(session);
            return session;
        }
    }

    private SocketSession insert(SocketSession session) {
        String sql = "INSERT INTO t_socket_session (user_id, session_key, client_ip, connection_id, " +
                "room_id, status, last_heartbeat, reconnect_attempts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, session.getUserId());
            stmt.setString(2, session.getSessionKey());
            stmt.setString(3, session.getClientIp());
            stmt.setString(4, session.getConnectionId());
            stmt.setObject(5, session.getRoomId());
            stmt.setString(6, session.getStatus());
            stmt.setTimestamp(7, new Timestamp(session.getLastHeartbeat().getTime()));
            stmt.setInt(8, session.getReconnectAttempts());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                session.setSessionId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted socket session: {}", session.getSessionId());
        } catch (SQLException e) {
            logger.error("Error inserting socket session", e);
        }
        return session;
    }

    @Override
    public int update(SocketSession session) {
        String sql = "UPDATE t_socket_session SET user_id = ?, session_key = ?, client_ip = ?, " +
                "connection_id = ?, room_id = ?, status = ?, last_heartbeat = ?, disconnect_time = ?, " +
                "disconnect_reason = ?, reconnect_attempts = ? WHERE session_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, session.getUserId());
            stmt.setString(2, session.getSessionKey());
            stmt.setString(3, session.getClientIp());
            stmt.setString(4, session.getConnectionId());
            stmt.setObject(5, session.getRoomId());
            stmt.setString(6, session.getStatus());
            stmt.setTimestamp(7, new Timestamp(session.getLastHeartbeat().getTime()));
            stmt.setTimestamp(8, session.getDisconnectTime() != null ? new Timestamp(session.getDisconnectTime().getTime()) : null);
            stmt.setString(9, session.getDisconnectReason());
            stmt.setInt(10, session.getReconnectAttempts());
            stmt.setLong(11, session.getSessionId());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated socket session: {}, rows affected: {}", session.getSessionId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating socket session: {}", session.getSessionId(), e);
        }
        return 0;
    }

    @Override
    public int updateStatus(Long sessionId, String status) {
        String sql = "UPDATE t_socket_session SET status = ? WHERE session_id = ?";
        return executeUpdateWithParams(sql, status, sessionId);
    }

    @Override
    public int updateHeartbeat(Long sessionId) {
        String sql = "UPDATE t_socket_session SET last_heartbeat = NOW() WHERE session_id = ?";
        return executeUpdateWithParams(sql, sessionId);
    }

    @Override
    public int updateConnectionId(Long sessionId, String connectionId) {
        String sql = "UPDATE t_socket_session SET connection_id = ? WHERE session_id = ?";
        return executeUpdateWithParams(sql, connectionId, sessionId);
    }

    @Override
    public int updateRoomId(Long sessionId, Long roomId) {
        String sql = "UPDATE t_socket_session SET room_id = ? WHERE session_id = ?";
        return executeUpdateWithParams(sql, roomId, sessionId);
    }

    @Override
    public int updateDisconnectTime(Long sessionId, Date disconnectTime) {
        String sql = "UPDATE t_socket_session SET disconnect_time = ? WHERE session_id = ?";
        return executeUpdateWithParams(sql, new Timestamp(disconnectTime.getTime()), sessionId);
    }

    @Override
    public int updateDisconnectReason(Long sessionId, String disconnectReason) {
        String sql = "UPDATE t_socket_session SET disconnect_reason = ? WHERE session_id = ?";
        return executeUpdateWithParams(sql, disconnectReason, sessionId);
    }

    @Override
    public int updateReconnectAttempts(Long sessionId, Integer reconnectAttempts) {
        String sql = "UPDATE t_socket_session SET reconnect_attempts = ? WHERE session_id = ?";
        return executeUpdateWithParams(sql, reconnectAttempts, sessionId);
    }

    @Override
    public int deleteById(Long sessionId) {
        String sql = "DELETE FROM t_socket_session WHERE session_id = ?";
        return executeUpdateWithParams(sql, sessionId);
    }

    @Override
    public int deleteByUserId(Long userId) {
        String sql = "DELETE FROM t_socket_session WHERE user_id = ?";
        return executeUpdateWithParams(sql, userId);
    }

    @Override
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM t_socket_session WHERE status = ?";
        return executeCountQuery(sql, status);
    }

    private List<SocketSession> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<SocketSession> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<SocketSession> sessions = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            sessions.add(mapResultSetToSocketSession(rs));
        }
        return sessions;
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

    private SocketSession mapResultSetToSocketSession(ResultSet rs) throws SQLException {
        SocketSession session = new SocketSession();
        session.setSessionId(rs.getLong("session_id"));
        session.setUserId(rs.getLong("user_id"));
        session.setSessionKey(rs.getString("session_key"));
        session.setClientIp(rs.getString("client_ip"));
        session.setConnectionId(rs.getString("connection_id"));
        session.setRoomId(rs.getLong("room_id"));
        session.setStatus(rs.getString("status"));
        session.setLastHeartbeat(rs.getTimestamp("last_heartbeat"));
        session.setDisconnectTime(rs.getTimestamp("disconnect_time"));
        session.setDisconnectReason(rs.getString("disconnect_reason"));
        session.setReconnectAttempts(rs.getInt("reconnect_attempts"));
        session.setCreateTime(rs.getTimestamp("create_time"));
        session.setUpdateTime(rs.getTimestamp("update_time"));
        return session;
    }
}