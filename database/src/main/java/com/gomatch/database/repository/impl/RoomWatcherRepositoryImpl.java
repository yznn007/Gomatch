package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.RoomWatcher;
import com.gomatch.database.repository.RoomWatcherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RoomWatcherRepositoryImpl implements RoomWatcherRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomWatcherRepositoryImpl.class);

    @Override
    public RoomWatcher findById(Long watcherId) {
        String sql = "SELECT * FROM t_room_watcher WHERE watcher_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, watcherId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRoomWatcher(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding room watcher by id: {}", watcherId, e);
        }
        return null;
    }

    @Override
    public List<RoomWatcher> findByRoomId(Long roomId) {
        String sql = "SELECT * FROM t_room_watcher WHERE room_id = ?";
        return executeQueryWithParam(sql, roomId);
    }

    @Override
    public List<RoomWatcher> findByUserId(Long userId) {
        String sql = "SELECT * FROM t_room_watcher WHERE user_id = ?";
        return executeQueryWithParam(sql, userId);
    }

    @Override
    public RoomWatcher findByRoomIdAndUserId(Long roomId, Long userId) {
        String sql = "SELECT * FROM t_room_watcher WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRoomWatcher(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding room watcher by room_id: {} and user_id: {}", roomId, userId, e);
        }
        return null;
    }

    @Override
    public List<RoomWatcher> findAll() {
        String sql = "SELECT * FROM t_room_watcher";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all room watchers", e);
        }
        return new ArrayList<>();
    }

    @Override
    public RoomWatcher save(RoomWatcher watcher) {
        String sql = "INSERT INTO t_room_watcher (room_id, user_id, join_time) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, watcher.getRoomId());
            stmt.setLong(2, watcher.getUserId());
            stmt.setTimestamp(3, new Timestamp(watcher.getJoinTime().getTime()));
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                watcher.setWatcherId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted room watcher: {}", watcher.getWatcherId());
        } catch (SQLException e) {
            logger.error("Error inserting room watcher", e);
        }
        return watcher;
    }

    @Override
    public int updateLeaveTime(Long watcherId, Date leaveTime) {
        String sql = "UPDATE t_room_watcher SET leave_time = ? WHERE watcher_id = ?";
        return executeUpdateWithParams(sql, new Timestamp(leaveTime.getTime()), watcherId);
    }

    @Override
    public int deleteById(Long watcherId) {
        String sql = "DELETE FROM t_room_watcher WHERE watcher_id = ?";
        return executeUpdateWithParams(sql, watcherId);
    }

    @Override
    public int deleteByRoomId(Long roomId) {
        String sql = "DELETE FROM t_room_watcher WHERE room_id = ?";
        return executeUpdateWithParams(sql, roomId);
    }

    @Override
    public int countByRoomId(Long roomId) {
        String sql = "SELECT COUNT(*) FROM t_room_watcher WHERE room_id = ?";
        return executeCountQuery(sql, roomId);
    }

    private List<RoomWatcher> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<RoomWatcher> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<RoomWatcher> watchers = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            watchers.add(mapResultSetToRoomWatcher(rs));
        }
        return watchers;
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

    private RoomWatcher mapResultSetToRoomWatcher(ResultSet rs) throws SQLException {
        RoomWatcher watcher = new RoomWatcher();
        watcher.setWatcherId(rs.getLong("watcher_id"));
        watcher.setRoomId(rs.getLong("room_id"));
        watcher.setUserId(rs.getLong("user_id"));
        watcher.setJoinTime(rs.getTimestamp("join_time"));
        watcher.setLeaveTime(rs.getTimestamp("leave_time"));
        return watcher;
    }
}