package com.gomatch.server.dao;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RecordDAO {
    private static final Logger log = LoggerFactory.getLogger(RecordDAO.class);
    private static final Gson gson = new Gson();

    public long saveRecord(String roomId, long player1Id, long player2Id,
                           Long winnerId, String result,
                           List<MoveRecord> moves, Instant startTime) {
        int duration = (int) Duration.between(startTime, Instant.now()).getSeconds();
        int moveCount = moves.size();

        String boardData = gson.toJson(moves);

        String sql = "INSERT INTO t_game_record " +
                     "(player1_id, player2_id, game_type, winner_id, result, " +
                     "board_data, duration, move_count, create_time) " +
                     "VALUES (?, ?, 'PVP', ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, player1Id);
            ps.setLong(2, player2Id);
            if (winnerId != null) {
                ps.setLong(3, winnerId);
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setString(4, result);
            ps.setString(5, boardData);
            ps.setInt(6, duration);
            ps.setInt(7, moveCount);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long recordId = rs.getLong(1);
                    log.info("Game record saved: room={}, recordId={}, p1={}, p2={}, winner={}, duration={}s, moves={}",
                            roomId, recordId, player1Id, player2Id, winnerId, duration, moveCount);
                    return recordId;
                }
            }
        } catch (SQLException e) {
            log.error("Save record failed: roomId={}", roomId, e);
        }
        return -1;
    }

    public static class MoveRecord {
        public int x;
        public int y;
        public int color;
        public long userId;

        public MoveRecord(int x, int y, int color, long userId) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.userId = userId;
        }
    }

    public List<String[]> getHistory(long userId, int page, int size) {
        String sql = "SELECT record_id, player1_id, player2_id, winner_id, result, " +
                     "board_data, duration, move_count, create_time " +
                     "FROM t_game_record " +
                     "WHERE player1_id = ? OR player2_id = ? " +
                     "ORDER BY create_time DESC LIMIT ? OFFSET ?";

        List<String[]> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setInt(3, size);
            ps.setInt(4, page * size);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new String[]{
                        String.valueOf(rs.getLong("record_id")),
                        String.valueOf(rs.getLong("player1_id")),
                        String.valueOf(rs.getLong("player2_id")),
                        String.valueOf(rs.getLong("winner_id")),
                        rs.getString("result"),
                        String.valueOf(rs.getInt("move_count")),
                        String.valueOf(rs.getInt("duration")),
                        rs.getString("create_time")
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Get history error: userId={}", userId, e);
        }
        return result;
    }
}