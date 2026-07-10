package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.GameSave;
import com.gomatch.database.repository.GameSaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameSaveRepositoryImpl implements GameSaveRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameSaveRepositoryImpl.class);

    @Override
    public GameSave findById(Long saveId) {
        String sql = "SELECT * FROM t_game_save WHERE save_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, saveId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGameSave(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding game save by id: {}", saveId, e);
        }
        return null;
    }

    @Override
    public GameSave findByFileUri(String fileUri) {
        String sql = "SELECT * FROM t_game_save WHERE file_uri = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileUri);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGameSave(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding game save by file uri: {}", fileUri, e);
        }
        return null;
    }

    @Override
    public List<GameSave> findByUserId(Long userId) {
        String sql = "SELECT * FROM t_game_save WHERE user_id = ?";
        return executeQueryWithParam(sql, userId);
    }

    @Override
    public List<GameSave> findByRecordId(Long recordId) {
        String sql = "SELECT * FROM t_game_save WHERE record_id = ?";
        return executeQueryWithParam(sql, recordId);
    }

    @Override
    public List<GameSave> findAll() {
        String sql = "SELECT * FROM t_game_save";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all game saves", e);
        }
        return new ArrayList<>();
    }

    @Override
    public GameSave save(GameSave save) {
        if (save.getSaveId() == null) {
            return insert(save);
        } else {
            update(save);
            return save;
        }
    }

    private GameSave insert(GameSave save) {
        String sql = "INSERT INTO t_game_save (user_id, record_id, file_uri, save_name, " +
                "board_data, current_player, game_status, checksum, file_size) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, save.getUserId());
            stmt.setObject(2, save.getRecordId());
            stmt.setString(3, save.getFileUri());
            stmt.setString(4, save.getSaveName());
            stmt.setString(5, save.getBoardData());
            stmt.setInt(6, save.getCurrentPlayer());
            stmt.setString(7, save.getGameStatus());
            stmt.setString(8, save.getChecksum());
            stmt.setInt(9, save.getFileSize());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                save.setSaveId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted game save: {}", save.getSaveId());
        } catch (SQLException e) {
            logger.error("Error inserting game save", e);
        }
        return save;
    }

    @Override
    public int update(GameSave save) {
        String sql = "UPDATE t_game_save SET user_id = ?, record_id = ?, file_uri = ?, save_name = ?, " +
                "board_data = ?, current_player = ?, game_status = ?, checksum = ?, file_size = ? WHERE save_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, save.getUserId());
            stmt.setObject(2, save.getRecordId());
            stmt.setString(3, save.getFileUri());
            stmt.setString(4, save.getSaveName());
            stmt.setString(5, save.getBoardData());
            stmt.setInt(6, save.getCurrentPlayer());
            stmt.setString(7, save.getGameStatus());
            stmt.setString(8, save.getChecksum());
            stmt.setInt(9, save.getFileSize());
            stmt.setLong(10, save.getSaveId());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated game save: {}, rows affected: {}", save.getSaveId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating game save: {}", save.getSaveId(), e);
        }
        return 0;
    }

    @Override
    public int updateChecksum(Long saveId, String checksum) {
        String sql = "UPDATE t_game_save SET checksum = ? WHERE save_id = ?";
        return executeUpdateWithParams(sql, checksum, saveId);
    }

    @Override
    public int updateFileSize(Long saveId, Integer fileSize) {
        String sql = "UPDATE t_game_save SET file_size = ? WHERE save_id = ?";
        return executeUpdateWithParams(sql, fileSize, saveId);
    }

    @Override
    public int deleteById(Long saveId) {
        String sql = "DELETE FROM t_game_save WHERE save_id = ?";
        return executeUpdateWithParams(sql, saveId);
    }

    @Override
    public int deleteByFileUri(String fileUri) {
        String sql = "DELETE FROM t_game_save WHERE file_uri = ?";
        return executeUpdateWithParams(sql, fileUri);
    }

    @Override
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM t_game_save WHERE user_id = ?";
        return executeCountQuery(sql, userId);
    }

    private List<GameSave> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<GameSave> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<GameSave> saves = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            saves.add(mapResultSetToGameSave(rs));
        }
        return saves;
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

    private GameSave mapResultSetToGameSave(ResultSet rs) throws SQLException {
        GameSave save = new GameSave();
        save.setSaveId(rs.getLong("save_id"));
        save.setUserId(rs.getLong("user_id"));
        save.setRecordId(rs.getLong("record_id"));
        save.setFileUri(rs.getString("file_uri"));
        save.setSaveName(rs.getString("save_name"));
        save.setBoardData(rs.getString("board_data"));
        save.setCurrentPlayer(rs.getInt("current_player"));
        save.setGameStatus(rs.getString("game_status"));
        save.setChecksum(rs.getString("checksum"));
        save.setFileSize(rs.getInt("file_size"));
        save.setCreateTime(rs.getTimestamp("create_time"));
        save.setUpdateTime(rs.getTimestamp("update_time"));
        return save;
    }
}