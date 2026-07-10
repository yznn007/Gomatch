package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameActionDAO {
    private static final Logger log = LoggerFactory.getLogger(GameActionDAO.class);

    public static class GameAction {
        private final long actionId;
        private final long recordId;
        private final long userId;
        private final String actionType;
        private final String actionStatus;
        private final int moveNumber;
        private final String rejectReason;
        private final String opponentResponse;
        private final String createTime;

        public GameAction(long actionId, long recordId, long userId, String actionType,
                         String actionStatus, int moveNumber, String rejectReason,
                         String opponentResponse, String createTime) {
            this.actionId = actionId;
            this.recordId = recordId;
            this.userId = userId;
            this.actionType = actionType;
            this.actionStatus = actionStatus;
            this.moveNumber = moveNumber;
            this.rejectReason = rejectReason;
            this.opponentResponse = opponentResponse;
            this.createTime = createTime;
        }

        public long getActionId() { return actionId; }
        public long getRecordId() { return recordId; }
        public long getUserId() { return userId; }
        public String getActionType() { return actionType; }
        public String getActionStatus() { return actionStatus; }
        public int getMoveNumber() { return moveNumber; }
        public String getRejectReason() { return rejectReason; }
        public String getOpponentResponse() { return opponentResponse; }
        public String getCreateTime() { return createTime; }
    }

    public long recordAction(Long recordId, long userId, String actionType, int moveNumber) {
        String sql = "INSERT INTO t_game_action (record_id, user_id, action_type, action_status, move_number) VALUES (?, ?, ?, 'pending', ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (recordId != null && recordId > 0) {
                ps.setLong(1, recordId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setLong(2, userId);
            ps.setString(3, actionType);
            ps.setInt(4, moveNumber);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    log.debug("Game action recorded: actionId={}, recordId={}, userId={}, type={}", id, recordId, userId, actionType);
                    return id;
                }
            }
        } catch (SQLException e) {
            log.error("Record game action error: recordId={}, userId={}, type={}", recordId, userId, actionType, e);
        }
        return -1;
    }

    public boolean updateActionStatus(long actionId, String status) {
        String sql = "UPDATE t_game_action SET action_status = ?, opponent_response = NOW() WHERE action_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, actionId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Game action status updated: actionId={}, status={}", actionId, status);
                return true;
            }
        } catch (SQLException e) {
            log.error("Update game action status error: actionId={}", actionId, e);
        }
        return false;
    }

    public boolean updateActionStatusWithReason(long actionId, String status, String reason) {
        String sql = "UPDATE t_game_action SET action_status = ?, reject_reason = ?, opponent_response = NOW() WHERE action_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setLong(3, actionId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Game action status updated: actionId={}, status={}, reason={}", actionId, status, reason);
                return true;
            }
        } catch (SQLException e) {
            log.error("Update game action status error: actionId={}", actionId, e);
        }
        return false;
    }

    public List<GameAction> getActionsByRecordId(long recordId) {
        String sql = "SELECT action_id, record_id, user_id, action_type, action_status, move_number, reject_reason, opponent_response, create_time " +
                     "FROM t_game_action WHERE record_id = ? ORDER BY create_time DESC";

        List<GameAction> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new GameAction(
                            rs.getLong("action_id"),
                            rs.getLong("record_id"),
                            rs.getLong("user_id"),
                            rs.getString("action_type"),
                            rs.getString("action_status"),
                            rs.getInt("move_number"),
                            rs.getString("reject_reason"),
                            rs.getString("opponent_response"),
                            rs.getString("create_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get game actions error: recordId={}", recordId, e);
        }
        return result;
    }

    public List<GameAction> getActionsByUserId(long userId, int page, int size) {
        String sql = "SELECT action_id, record_id, user_id, action_type, action_status, move_number, reject_reason, opponent_response, create_time " +
                     "FROM t_game_action WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?";

        List<GameAction> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, size);
            ps.setInt(3, page * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new GameAction(
                            rs.getLong("action_id"),
                            rs.getLong("record_id"),
                            rs.getLong("user_id"),
                            rs.getString("action_type"),
                            rs.getString("action_status"),
                            rs.getInt("move_number"),
                            rs.getString("reject_reason"),
                            rs.getString("opponent_response"),
                            rs.getString("create_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get game actions by userId error: userId={}", userId, e);
        }
        return result;
    }

    public int getActionCount(long recordId, String actionType) {
        String sql = "SELECT COUNT(*) FROM t_game_action WHERE record_id = ? AND action_type = ? AND action_status = 'pending'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            ps.setString(2, actionType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Get action count error: recordId={}", recordId, e);
        }
        return 0;
    }
}