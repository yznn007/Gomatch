package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.Room;
import com.gomatch.database.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomRepositoryImpl implements RoomRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepositoryImpl.class);

    @Override
    public Room findById(Long roomId) {
        String sql = "SELECT * FROM t_room WHERE room_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRoom(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding room by id: {}", roomId, e);
        }
        return null;
    }

    @Override
    public List<Room> findByStatus(String status) {
        String sql = "SELECT * FROM t_room WHERE status = ?";
        return executeQueryWithParam(sql, status);
    }

    @Override
    public List<Room> findByHostId(Long hostId) {
        String sql = "SELECT * FROM t_room WHERE host_id = ?";
        return executeQueryWithParam(sql, hostId);
    }

    @Override
    public List<Room> findByName(String roomName) {
        String sql = "SELECT * FROM t_room WHERE room_name LIKE ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + roomName + "%");
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding room by name: {}", roomName, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Room> findByAllowWatch(Boolean allowWatch) {
        String sql = "SELECT * FROM t_room WHERE allow_watch = ?";
        return executeQueryWithParam(sql, allowWatch);
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT * FROM t_room";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all rooms", e);
        }
        return new ArrayList<>();
    }

    @Override
    public Room save(Room room) {
        if (room.getRoomId() == null) {
            return insert(room);
        } else {
            update(room);
            return room;
        }
    }

    private Room insert(Room room) {
        if (room.getRoomId() == null) {
            room.setRoomId(System.currentTimeMillis() * 1000 + (long)(Math.random() * 1000));
        }
        String sql = "INSERT INTO t_room (room_id, room_name, password, max_players, current_players, " +
                "watcher_count, status, host_id, allow_watch, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, room.getRoomId());
            stmt.setString(2, room.getRoomName());
            stmt.setString(3, room.getPassword());
            stmt.setInt(4, room.getMaxPlayers());
            stmt.setInt(5, room.getCurrentPlayers());
            stmt.setInt(6, room.getWatcherCount());
            stmt.setString(7, room.getStatus());
            stmt.setLong(8, room.getHostId());
            stmt.setBoolean(9, room.getAllowWatch());
            stmt.setInt(10, room.getVersion());
            stmt.executeUpdate();
            conn.commit();
            logger.info("Inserted room: {}", room.getRoomId());
        } catch (SQLException e) {
            logger.error("Error inserting room", e);
        }
        return room;
    }

    @Override
    public int update(Room room) {
        String sql = "UPDATE t_room SET room_name = ?, password = ?, max_players = ?, current_players = ?, " +
                "watcher_count = ?, status = ?, host_id = ?, allow_watch = ?, version = version + 1 WHERE room_id = ? AND version = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomName());
            stmt.setString(2, room.getPassword());
            stmt.setInt(3, room.getMaxPlayers());
            stmt.setInt(4, room.getCurrentPlayers());
            stmt.setInt(5, room.getWatcherCount());
            stmt.setString(6, room.getStatus());
            stmt.setLong(7, room.getHostId());
            stmt.setBoolean(8, room.getAllowWatch());
            stmt.setLong(9, room.getRoomId());
            stmt.setInt(10, room.getVersion());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated room: {}, rows affected: {}", room.getRoomId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating room: {}", room.getRoomId(), e);
        }
        return 0;
    }

    @Override
    public int updateStatus(Long roomId, String status) {
        String sql = "UPDATE t_room SET status = ?, version = version + 1 WHERE room_id = ?";
        return executeUpdateWithParams(sql, status, roomId);
    }

    @Override
    public int updatePlayerCount(Long roomId, int count) {
        String sql = "UPDATE t_room SET current_players = ?, version = version + 1 WHERE room_id = ?";
        return executeUpdateWithParams(sql, count, roomId);
    }

    @Override
    public int updateWatcherCount(Long roomId, int count) {
        String sql = "UPDATE t_room SET watcher_count = ?, version = version + 1 WHERE room_id = ?";
        return executeUpdateWithParams(sql, count, roomId);
    }

    @Override
    public int deleteById(Long roomId) {
        String sql = "DELETE FROM t_room WHERE room_id = ?";
        return executeUpdateWithParams(sql, roomId);
    }

    @Override
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM t_room WHERE status = ?";
        return executeCountQuery(sql, status);
    }

    private List<Room> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<Room> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            rooms.add(mapResultSetToRoom(rs));
        }
        return rooms;
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

    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setRoomId(rs.getLong("room_id"));
        room.setRoomName(rs.getString("room_name"));
        room.setPassword(rs.getString("password"));
        room.setMaxPlayers(rs.getInt("max_players"));
        room.setCurrentPlayers(rs.getInt("current_players"));
        room.setWatcherCount(rs.getInt("watcher_count"));
        room.setStatus(rs.getString("status"));
        room.setHostId(rs.getLong("host_id"));
        room.setAllowWatch(rs.getBoolean("allow_watch"));
        room.setVersion(rs.getInt("version"));
        room.setCreateTime(rs.getTimestamp("create_time"));
        room.setUpdateTime(rs.getTimestamp("update_time"));
        return room;
    }
}