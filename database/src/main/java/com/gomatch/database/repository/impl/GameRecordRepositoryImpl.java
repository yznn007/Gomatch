package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.GameRecord;
import com.gomatch.database.repository.GameRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameRecordRepositoryImpl implements GameRecordRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameRecordRepositoryImpl.class);

    @Override
    public GameRecord findById(Long recordId) {
        String sql = "SELECT * FROM t_game_record WHERE record_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGameRecord(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding game record by id: {}", recordId, e);
        }
        return null;
    }

    @Override
    public List<GameRecord> findByPlayer1Id(Long player1Id) {
        String sql = "SELECT * FROM t_game_record WHERE player1_id = ?";
        return executeQueryWithParam(sql, player1Id);
    }

    @Override
    public List<GameRecord> findByPlayer2Id(Long player2Id) {
        String sql = "SELECT * FROM t_game_record WHERE player2_id = ?";
        return executeQueryWithParam(sql, player2Id);
    }

    @Override
    public List<GameRecord> findByPlayerId(Long playerId) {
        String sql = "SELECT * FROM t_game_record WHERE player1_id = ? OR player2_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, playerId);
            stmt.setLong(2, playerId);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding game records by player id: {}", playerId, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<GameRecord> findByGameType(String gameType) {
        String sql = "SELECT * FROM t_game_record WHERE game_type = ?";
        return executeQueryWithParam(sql, gameType);
    }

    @Override
    public List<GameRecord> findByWinnerId(Long winnerId) {
        String sql = "SELECT * FROM t_game_record WHERE winner_id = ?";
        return executeQueryWithParam(sql, winnerId);
    }

    @Override
    public List<GameRecord> findAll() {
        String sql = "SELECT * FROM t_game_record";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all game records", e);
        }
        return new ArrayList<>();
    }

    @Override
    public GameRecord save(GameRecord record) {
        if (record.getRecordId() == null) {
            return insert(record);
        } else {
            update(record);
            return record;
        }
    }

    private GameRecord insert(GameRecord record) {
        String sql = "INSERT INTO t_game_record (player1_id, player2_id, game_type, winner_id, " +
                "result, board_data, duration, move_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, record.getPlayer1Id());
            stmt.setLong(2, record.getPlayer2Id());
            stmt.setString(3, record.getGameType());
            stmt.setObject(4, record.getWinnerId());
            stmt.setString(5, record.getResult());
            stmt.setString(6, record.getBoardData());
            stmt.setObject(7, record.getDuration());
            stmt.setObject(8, record.getMoveCount());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                record.setRecordId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted game record: {}", record.getRecordId());
        } catch (SQLException e) {
            logger.error("Error inserting game record", e);
        }
        return record;
    }

    @Override
    public int update(GameRecord record) {
        String sql = "UPDATE t_game_record SET player1_id = ?, player2_id = ?, game_type = ?, " +
                "winner_id = ?, result = ?, board_data = ?, duration = ?, move_count = ? WHERE record_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, record.getPlayer1Id());
            stmt.setLong(2, record.getPlayer2Id());
            stmt.setString(3, record.getGameType());
            stmt.setObject(4, record.getWinnerId());
            stmt.setString(5, record.getResult());
            stmt.setString(6, record.getBoardData());
            stmt.setObject(7, record.getDuration());
            stmt.setObject(8, record.getMoveCount());
            stmt.setLong(9, record.getRecordId());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated game record: {}, rows affected: {}", record.getRecordId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating game record: {}", record.getRecordId(), e);
        }
        return 0;
    }

    @Override
    public int updateResult(Long recordId, Long winnerId, String result) {
        String sql = "UPDATE t_game_record SET winner_id = ?, result = ? WHERE record_id = ?";
        return executeUpdateWithParams(sql, winnerId, result, recordId);
    }

    @Override
    public int updateBoardData(Long recordId, String boardData) {
        String sql = "UPDATE t_game_record SET board_data = ? WHERE record_id = ?";
        return executeUpdateWithParams(sql, boardData, recordId);
    }

    @Override
    public int updateDuration(Long recordId, Integer duration) {
        String sql = "UPDATE t_game_record SET duration = ? WHERE record_id = ?";
        return executeUpdateWithParams(sql, duration, recordId);
    }

    @Override
    public int updateMoveCount(Long recordId, Integer moveCount) {
        String sql = "UPDATE t_game_record SET move_count = ? WHERE record_id = ?";
        return executeUpdateWithParams(sql, moveCount, recordId);
    }

    @Override
    public int deleteById(Long recordId) {
        String sql = "DELETE FROM t_game_record WHERE record_id = ?";
        return executeUpdateWithParams(sql, recordId);
    }

    @Override
    public int countByPlayerId(Long playerId) {
        String sql = "SELECT COUNT(*) FROM t_game_record WHERE player1_id = ? OR player2_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, playerId);
            stmt.setLong(2, playerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting game records by player id: {}", playerId, e);
        }
        return 0;
    }

    @Override
    public int countByGameType(String gameType) {
        String sql = "SELECT COUNT(*) FROM t_game_record WHERE game_type = ?";
        return executeCountQuery(sql, gameType);
    }

    private List<GameRecord> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<GameRecord> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<GameRecord> records = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            records.add(mapResultSetToGameRecord(rs));
        }
        return records;
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

    private GameRecord mapResultSetToGameRecord(ResultSet rs) throws SQLException {
        GameRecord record = new GameRecord();
        record.setRecordId(rs.getLong("record_id"));
        record.setPlayer1Id(rs.getLong("player1_id"));
        record.setPlayer2Id(rs.getLong("player2_id"));
        record.setGameType(rs.getString("game_type"));
        record.setWinnerId(rs.getLong("winner_id"));
        record.setResult(rs.getString("result"));
        record.setBoardData(rs.getString("board_data"));
        record.setDuration(rs.getInt("duration"));
        record.setMoveCount(rs.getInt("move_count"));
        record.setCreateTime(rs.getTimestamp("create_time"));
        return record;
    }
}