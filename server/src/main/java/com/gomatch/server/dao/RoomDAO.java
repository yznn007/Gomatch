package com.gomatch.server.dao;

import com.gomatch.server.room.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;

/**
 * 房间数据访问层
 * 负责将内存中的房间状态持久化到数据库
 *
 * 注意：Room.roomId 使用 String (UUID格式，如 "abc12345")
 * 数据库 t_room.room_id 需为 VARCHAR(50) 以适配
 * 若使用原 BIGINT 设计，需先执行 ALTER TABLE 修改字段类型
 */
public class RoomDAO {
    private static final Logger log = LoggerFactory.getLogger(RoomDAO.class);

    /**
     * 保存房间到数据库（INSERT）
     * @param room 房间对象
     * @return 是否成功
     */
    public boolean saveRoom(Room room) {
        String sql = "INSERT INTO t_room " +
                     "(room_id, room_name, password, max_players, current_players, " +
                     "watcher_count, status, host_id, allow_watch, create_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "room_name = VALUES(room_name), " +
                     "password = VALUES(password), " +
                     "current_players = VALUES(current_players), " +
                     "watcher_count = VALUES(watcher_count), " +
                     "status = VALUES(status), " +
                     "allow_watch = VALUES(allow_watch), " +
                     "update_time = NOW()";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomId());
            ps.setString(2, room.getRoomName());
            // 密码不保存到数据库，仅在内存中验证（房间为临时性对象）
            ps.setString(3, null);
            ps.setInt(4, 2); // max_players 固定为 2
            ps.setInt(5, room.getPlayerCount());
            ps.setInt(6, room.getObserverCount());
            ps.setString(7, room.getStatus().toString().toLowerCase());
            ps.setLong(8, room.getOwner().getUserId());
            ps.setBoolean(9, true); // allow_watch 默认允许观战

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Room saved to database: roomId={}, status={}, players={}, watchers={}",
                        room.getRoomId(), room.getStatus(), room.getPlayerCount(), room.getObserverCount());
                return true;
            }
        } catch (SQLException e) {
            log.error("Save room failed: roomId={}", room.getRoomId(), e);
        }
        return false;
    }

    /**
     * 更新房间状态（UPDATE）
     * 仅更新变化频繁的字段：status, current_players, watcher_count
     * @param room 房间对象
     * @return 是否成功
     */
    public boolean updateRoom(Room room) {
        String sql = "UPDATE t_room SET " +
                     "current_players = ?, watcher_count = ?, status = ?, " +
                     "update_time = NOW() " +
                     "WHERE room_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, room.getPlayerCount());
            ps.setInt(2, room.getObserverCount());
            ps.setString(3, room.getStatus().toString().toLowerCase());
            ps.setString(4, room.getRoomId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Room updated: roomId={}, status={}, players={}/{}",
                        room.getRoomId(), room.getStatus(), room.getPlayerCount(), room.getObserverCount());
                return true;
            }
        } catch (SQLException e) {
            log.error("Update room failed: roomId={}", room.getRoomId(), e);
        }
        return false;
    }

    /**
     * 删除房间记录（DELETE）
     * @param roomId 房间ID
     * @return 是否成功
     */
    public boolean deleteRoom(String roomId) {
        String sql = "DELETE FROM t_room WHERE room_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Room deleted from database: roomId={}", roomId);
                return true;
            }
        } catch (SQLException e) {
            log.error("Delete room failed: roomId={}", roomId, e);
        }
        return false;
    }

    /**
     * 同步所有内存房间到数据库（定时任务调用）
     * 遍历 RoomManager 中的所有房间，逐个持久化
     * @param rooms 房间Map（roomId -> Room）
     * @return 成功持久化的房间数量
     */
    public int syncAllRooms(Map<String, Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            log.debug("No rooms to sync");
            return 0;
        }

        int successCount = 0;
        int failCount = 0;

        for (Room room : rooms.values()) {
            try {
                if (saveRoom(room)) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("Sync room failed: roomId={}", room.getRoomId(), e);
                failCount++;
            }
        }

        if (successCount > 0) {
            log.info("Room sync completed: {} rooms synced, {} failed",
                    successCount, failCount);
        }

        return successCount;
    }

    /**
     * 清理已结束的房间（定时任务调用）
     * 删除状态为 finished 且超过指定分钟数的房间记录
     * @param minutes 过期分钟数
     * @return 删除的房间数量
     */
    public int cleanupFinishedRooms(int minutes) {
        String sql = "DELETE FROM t_room WHERE status = 'finished' " +
                     "AND update_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minutes);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Cleaned up {} finished rooms older than {} minutes",
                        affected, minutes);
            }
            return affected;
        } catch (SQLException e) {
            log.error("Cleanup finished rooms error", e);
        }
        return 0;
    }
}