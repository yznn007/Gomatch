package com.gomatch.server.dao;

import com.google.gson.Gson;
import com.gomatch.server.game.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameSaveDAO {
    private static final Logger log = LoggerFactory.getLogger(GameSaveDAO.class);
    private static final Gson gson = new Gson();

    public static class GameSaveInfo {
        private final long saveId;
        private final long userId;
        private final Long recordId;
        private final String fileUri;
        private final String saveName;
        private final String boardData;
        private final int currentPlayer;
        private final String gameStatus;
        private final String createTime;

        public GameSaveInfo(long saveId, long userId, Long recordId, String fileUri,
                           String saveName, String boardData, int currentPlayer,
                           String gameStatus, String createTime) {
            this.saveId = saveId;
            this.userId = userId;
            this.recordId = recordId;
            this.fileUri = fileUri;
            this.saveName = saveName;
            this.boardData = boardData;
            this.currentPlayer = currentPlayer;
            this.gameStatus = gameStatus;
            this.createTime = createTime;
        }

        public long getSaveId() { return saveId; }
        public long getUserId() { return userId; }
        public Long getRecordId() { return recordId; }
        public String getFileUri() { return fileUri; }
        public String getSaveName() { return saveName; }
        public String getBoardData() { return boardData; }
        public int getCurrentPlayer() { return currentPlayer; }
        public String getGameStatus() { return gameStatus; }
        public String getCreateTime() { return createTime; }
    }

    public long saveGame(long userId, Long recordId, String saveName, Game game) {
        String fileUri = generateFileUri(userId);
        String boardData = gson.toJson(game.getMoveHistory());
        String gameStatus = game.isFinished() ? "finished" : "playing";
        int currentPlayer = game.getCurrentPlayer();

        String sql = "INSERT INTO t_game_save (user_id, record_id, file_uri, save_name, " +
                     "board_data, current_player, game_status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            if (recordId != null) {
                ps.setLong(2, recordId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, fileUri);
            ps.setString(4, saveName);
            ps.setString(5, boardData);
            ps.setInt(6, currentPlayer);
            ps.setString(7, gameStatus);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long saveId = rs.getLong(1);
                    log.info("Game saved: saveId={}, userId={}, fileUri={}, moves={}",
                            saveId, userId, fileUri, game.getMoveCount());
                    return saveId;
                }
            }
        } catch (SQLException e) {
            log.error("Save game error: userId={}", userId, e);
        }
        return -1;
    }

    public GameSaveInfo getGameSave(long saveId) {
        String sql = "SELECT save_id, user_id, record_id, file_uri, save_name, " +
                     "board_data, current_player, game_status, create_time " +
                     "FROM t_game_save WHERE save_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, saveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new GameSaveInfo(
                            rs.getLong("save_id"),
                            rs.getLong("user_id"),
                            rs.getLong("record_id") == 0 ? null : rs.getLong("record_id"),
                            rs.getString("file_uri"),
                            rs.getString("save_name"),
                            rs.getString("board_data"),
                            rs.getInt("current_player"),
                            rs.getString("game_status"),
                            rs.getString("create_time")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Get game save error: saveId={}", saveId, e);
        }
        return null;
    }

    public List<GameSaveInfo> getGameSavesByUserId(long userId, int page, int size) {
        String sql = "SELECT save_id, user_id, record_id, file_uri, save_name, " +
                     "board_data, current_player, game_status, create_time " +
                     "FROM t_game_save WHERE user_id = ? " +
                     "ORDER BY create_time DESC LIMIT ? OFFSET ?";

        List<GameSaveInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, size);
            ps.setInt(3, page * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new GameSaveInfo(
                            rs.getLong("save_id"),
                            rs.getLong("user_id"),
                            rs.getLong("record_id") == 0 ? null : rs.getLong("record_id"),
                            rs.getString("file_uri"),
                            rs.getString("save_name"),
                            rs.getString("board_data"),
                            rs.getInt("current_player"),
                            rs.getString("game_status"),
                            rs.getString("create_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get game saves error: userId={}", userId, e);
        }
        return result;
    }

    public boolean deleteGameSave(long saveId, long userId) {
        String sql = "DELETE FROM t_game_save WHERE save_id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, saveId);
            ps.setLong(2, userId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Game save deleted: saveId={}, userId={}", saveId, userId);
                return true;
            } else {
                log.warn("Game save not found or not owned: saveId={}, userId={}", saveId, userId);
                return false;
            }
        } catch (SQLException e) {
            log.error("Delete game save error: saveId={}", saveId, e);
            return false;
        }
    }

    private String generateFileUri(long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String hash = UUID.randomUUID().toString().substring(0, 8);
        return "game://localhost:8888/v1/" + userId + "/" + timestamp + "/" + hash;
    }

    public Game loadGameFromSave(GameSaveInfo saveInfo) {
        Game game = new Game();
        game.reset();

        if (saveInfo.getBoardData() != null && !saveInfo.getBoardData().isEmpty()) {
            try {
                Game.MoveRecord[] moves = gson.fromJson(saveInfo.getBoardData(), Game.MoveRecord[].class);
                for (Game.MoveRecord move : moves) {
                    game.move(move.x, move.y, move.userId);
                }
                log.info("Game loaded from save: saveId={}, moves={}", saveInfo.getSaveId(), moves.length);
            } catch (Exception e) {
                log.error("Load game from save error: saveId={}", saveInfo.getSaveId(), e);
            }
        }

        return game;
    }
}