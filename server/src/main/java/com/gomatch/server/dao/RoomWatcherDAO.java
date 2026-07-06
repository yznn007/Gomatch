package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomWatcherDAO {
    private static final Logger log = LoggerFactory.getLogger(RoomWatcherDAO.class);

    public static class RoomWatcher {
        private final long watcherId;
        private final String roomId;
        private final long userId;
        private final String username;
        private final String joinTime;
        private final String leaveTime;

        public RoomWatcher(long watcherId, String roomId, long userId, String username,
                          String joinTime, String leaveTime) {
            this.watcherId = watcherId;
            this.roomId = roomId;
            this.userId = userId;
            this.username = username;
            this.joinTime = joinTime;
            this.leaveTime = leaveTime;
        }

        public long getWatcherId() { return watcherId; }
        public String getRoomId() { return roomId; }
        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getJoinTime() { return joinTime; }
        public String getLeaveTime() { return leaveTime; }
    }

    public long startWatching(String roomId, long userId) {
        String sql = "INSERT INTO t_room_watcher (room_id, user_id, join_time) VALUES (?, ?, NOW())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roomId);
            ps.setLong(2, userId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    log.debug("Watcher record created: watcherId={}, roomId={}, userId={}", id, roomId, userId);
                    return id;
                }
            }
        } catch (SQLException e) {
            log.error("Start watching error: roomId={}, userId={}", roomId, userId, e);
        }
        return -1;
    }

    public boolean stopWatching(String roomId, long userId) {
        String sql = "UPDATE t_room_watcher SET leave_time = NOW() WHERE room_id = ? AND user_id = ? AND leave_time IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setLong(2, userId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Watcher record updated: roomId={}, userId={}", roomId, userId);
                return true;
            }
        } catch (SQLException e) {
            log.error("Stop watching error: roomId={}, userId={}", roomId, userId, e);
        }
        return false;
    }

    public List<RoomWatcher> getWatcherHistory(long userId, int page, int size) {
        String sql = "SELECT w.watcher_id, w.room_id, w.user_id, u.username, w.join_time, w.leave_time " +
                     "FROM t_room_watcher w JOIN t_user u ON w.user_id = u.user_id " +
                     "WHERE w.user_id = ? " +
                     "ORDER BY w.join_time DESC LIMIT ? OFFSET ?";

        List<RoomWatcher> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, size);
            ps.setInt(3, page * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new RoomWatcher(
                            rs.getLong("watcher_id"),
                            rs.getString("room_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("join_time"),
                            rs.getString("leave_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get watcher history error: userId={}", userId, e);
        }
        return result;
    }

    public List<RoomWatcher> getCurrentWatchers(String roomId) {
        String sql = "SELECT w.watcher_id, w.room_id, w.user_id, u.username, w.join_time, w.leave_time " +
                     "FROM t_room_watcher w JOIN t_user u ON w.user_id = u.user_id " +
                     "WHERE w.room_id = ? AND w.leave_time IS NULL";

        List<RoomWatcher> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new RoomWatcher(
                            rs.getLong("watcher_id"),
                            rs.getString("room_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("join_time"),
                            rs.getString("leave_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get current watchers error: roomId={}", roomId, e);
        }
        return result;
    }

    public int getWatcherCount(String roomId) {
        String sql = "SELECT COUNT(*) FROM t_room_watcher WHERE room_id = ? AND leave_time IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Get watcher count error: roomId={}", roomId, e);
        }
        return 0;
    }

    public boolean cleanupStaleWatchers(int minutes) {
        String sql = "UPDATE t_room_watcher SET leave_time = NOW() WHERE leave_time IS NULL AND join_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minutes);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Cleaned up {} stale watchers", affected);
            }
            return affected > 0;
        } catch (SQLException e) {
            log.error("Cleanup stale watchers error", e);
        }
        return false;
    }
}