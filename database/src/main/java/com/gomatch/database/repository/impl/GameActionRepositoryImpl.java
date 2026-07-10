package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.GameAction;
import com.gomatch.database.repository.GameActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GameActionRepositoryImpl implements GameActionRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameActionRepositoryImpl.class);

    @Override
    public GameAction findById(Long actionId) {
        String sql = "SELECT * FROM t_game_action WHERE action_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, actionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToGameAction(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding game action by id: {}", actionId, e);
        }
        return null;
    }

    @Override
    public List<GameAction> findByRecordId(Long recordId) {
        String sql = "SELECT * FROM t_game_action WHERE record_id = ?";
        return executeQueryWithParam(sql, recordId);
    }

    @Override
    public List<GameAction> findByUserId(Long userId) {
        String sql = "SELECT * FROM t_game_action WHERE user_id = ?";
        return executeQueryWithParam(sql, userId);
    }

    @Override
    public List<GameAction> findByRecordIdAndStatus(Long recordId, String status) {
        String sql = "SELECT * FROM t_game_action WHERE record_id = ? AND action_status = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recordId);
            stmt.setString(2, status);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding game actions by record_id: {} and status: {}", recordId, status, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<GameAction> findAll() {
        String sql = "SELECT * FROM t_game_action";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all game actions", e);
        }
        return new ArrayList<>();
    }

    @Override
    public GameAction save(GameAction action) {
        String sql = "INSERT INTO t_game_action (record_id, user_id, action_type, action_status, move_number, reject_reason) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, action.getRecordId());
            stmt.setLong(2, action.getUserId());
            stmt.setString(3, action.getActionType());
            stmt.setString(4, action.getActionStatus());
            stmt.setObject(5, action.getMoveNumber());
            stmt.setString(6, action.getRejectReason());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                action.setActionId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted game action: {}", action.getActionId());
        } catch (SQLException e) {
            logger.error("Error inserting game action", e);
        }
        return action;
    }

    @Override
    public int updateStatus(Long actionId, String status) {
        String sql = "UPDATE t_game_action SET action_status = ? WHERE action_id = ?";
        return executeUpdateWithParams(sql, status, actionId);
    }

    @Override
    public int updateRejectReason(Long actionId, String reason) {
        String sql = "UPDATE t_game_action SET reject_reason = ? WHERE action_id = ?";
        return executeUpdateWithParams(sql, reason, actionId);
    }

    @Override
    public int updateOpponentResponse(Long actionId, Date responseTime) {
        String sql = "UPDATE t_game_action SET opponent_response = ? WHERE action_id = ?";
        return executeUpdateWithParams(sql, new Timestamp(responseTime.getTime()), actionId);
    }

    @Override
    public int deleteById(Long actionId) {
        String sql = "DELETE FROM t_game_action WHERE action_id = ?";
        return executeUpdateWithParams(sql, actionId);
    }

    @Override
    public int deleteByRecordId(Long recordId) {
        String sql = "DELETE FROM t_game_action WHERE record_id = ?";
        return executeUpdateWithParams(sql, recordId);
    }

    private List<GameAction> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<GameAction> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<GameAction> actions = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            actions.add(mapResultSetToGameAction(rs));
        }
        return actions;
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

    private GameAction mapResultSetToGameAction(ResultSet rs) throws SQLException {
        GameAction action = new GameAction();
        action.setActionId(rs.getLong("action_id"));
        action.setRecordId(rs.getLong("record_id"));
        action.setUserId(rs.getLong("user_id"));
        action.setActionType(rs.getString("action_type"));
        action.setActionStatus(rs.getString("action_status"));
        action.setMoveNumber(rs.getInt("move_number"));
        action.setRejectReason(rs.getString("reject_reason"));
        action.setOpponentResponse(rs.getTimestamp("opponent_response"));
        action.setCreateTime(rs.getTimestamp("create_time"));
        return action;
    }
}