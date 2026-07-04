package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

public class RecordDAO {
    private static final Logger log = LoggerFactory.getLogger(RecordDAO.class);

    public void saveRecord(String roomId, long blackId, long whiteId,
                           Long winnerId, int result, String steps) {
        String sql = "INSERT INTO t_game_record (room_id, black_user_id, white_user_id, winner_id, result, steps, start_time) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setLong(2, blackId);
            ps.setLong(3, whiteId);
            if (winnerId != null) ps.setLong(4, winnerId);
            else ps.setNull(4, Types.BIGINT);
            ps.setInt(5, result);
            ps.setString(6, steps);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Save record failed", e);
        }
    }
}
