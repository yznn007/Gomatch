package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class SocketSessionDAO {
    private static final Logger log = LoggerFactory.getLogger(SocketSessionDAO.class);

    public long saveSession(long userId, String sessionKey, String clientIp, String connectionId, String roomId) {
        String sql = "INSERT INTO t_socket_session (user_id, session_key, client_ip, connection_id, room_id, status) VALUES (?, ?, ?, ?, ?, 'connected')";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, sessionKey);
            ps.setString(3, clientIp);
            ps.setString(4, connectionId);
            ps.setString(5, roomId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    log.debug("Socket session saved: sessionId={}, userId={}", id, userId);
                    return id;
                }
            }
        } catch (SQLException e) {
            log.error("Save socket session error: userId={}", userId, e);
        }
        return -1;
    }

    public boolean updateHeartbeat(String sessionKey) {
        String sql = "UPDATE t_socket_session SET last_heartbeat = NOW(), status = 'connected' WHERE session_key = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionKey);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Heartbeat updated: sessionKey={}", sessionKey.substring(0, 8));
                return true;
            }
        } catch (SQLException e) {
            log.error("Update heartbeat error: sessionKey={}", sessionKey, e);
        }
        return false;
    }

    public boolean updateStatus(String sessionKey, String status) {
        String sql = "UPDATE t_socket_session SET status = ?, update_time = NOW() WHERE session_key = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, sessionKey);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Status updated: sessionKey={}, status={}", sessionKey.substring(0, 8), status);
                return true;
            }
        } catch (SQLException e) {
            log.error("Update status error: sessionKey={}", sessionKey, e);
        }
        return false;
    }

    public boolean updateRoom(String sessionKey, String roomId) {
        String sql = "UPDATE t_socket_session SET room_id = ?, update_time = NOW() WHERE session_key = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setString(2, sessionKey);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Room updated: sessionKey={}, roomId={}", sessionKey.substring(0, 8), roomId);
                return true;
            }
        } catch (SQLException e) {
            log.error("Update room error: sessionKey={}", sessionKey, e);
        }
        return false;
    }

    public boolean disconnect(String sessionKey, String reason) {
        String sql = "UPDATE t_socket_session SET status = 'disconnected', disconnect_time = NOW(), " +
                     "disconnect_reason = ? WHERE session_key = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, sessionKey);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Session disconnected: sessionKey={}, reason={}", sessionKey.substring(0, 8), reason);
                return true;
            }
        } catch (SQLException e) {
            log.error("Disconnect error: sessionKey={}", sessionKey, e);
        }
        return false;
    }

    public int cleanupExpiredSessions(int minutes) {
        String sql = "DELETE FROM t_socket_session WHERE status = 'disconnected' AND disconnect_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minutes);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Cleaned up {} expired socket sessions", affected);
            }
            return affected;
        } catch (SQLException e) {
            log.error("Cleanup expired sessions error", e);
        }
        return 0;
    }
}