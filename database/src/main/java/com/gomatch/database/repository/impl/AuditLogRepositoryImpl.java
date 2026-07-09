package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.AuditLog;
import com.gomatch.database.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepositoryImpl implements AuditLogRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogRepositoryImpl.class);

    @Override
    public AuditLog findById(Long logId) {
        String sql = "SELECT * FROM t_audit_log WHERE log_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, logId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToAuditLog(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding audit log by id: {}", logId, e);
        }
        return null;
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        String sql = "SELECT * FROM t_audit_log WHERE user_id = ? ORDER BY create_time DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding audit logs by user id: {}", userId, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<AuditLog> findByActionType(String actionType) {
        String sql = "SELECT * FROM t_audit_log WHERE action_type = ? ORDER BY create_time DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, actionType);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding audit logs by action type: {}", actionType, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<AuditLog> findByUserIdAndActionType(Long userId, String actionType) {
        String sql = "SELECT * FROM t_audit_log WHERE user_id = ? AND action_type = ? ORDER BY create_time DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, actionType);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding audit logs by user id: {} and action type: {}", userId, actionType, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT * FROM t_audit_log ORDER BY create_time DESC LIMIT ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding recent audit logs", e);
        }
        return new ArrayList<>();
    }

    @Override
    public AuditLog save(AuditLog log) {
        String sql = "INSERT INTO t_audit_log (user_id, action_type, action_desc, target_type, target_id, " +
                "before_data, after_data, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            stmt.setLong(idx++, log.getUserId());
            stmt.setString(idx++, log.getActionType());
            stmt.setString(idx++, log.getActionDesc());
            stmt.setString(idx++, log.getTargetType());
            stmt.setObject(idx++, log.getTargetId());
            stmt.setString(idx++, log.getBeforeData());
            stmt.setString(idx++, log.getAfterData());
            stmt.setString(idx++, log.getIpAddress());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                log.setLogId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted audit log: {} for user: {}", log.getLogId(), log.getUserId());
        } catch (SQLException e) {
            logger.error("Error inserting audit log for user: {}", log.getUserId(), e);
        }
        return log;
    }

    @Override
    public int deleteByUserId(Long userId) {
        String sql = "DELETE FROM t_audit_log WHERE user_id = ?";
        return executeUpdateWithParams(sql, userId);
    }

    @Override
    public int deleteByDateRange(java.util.Date startDate, java.util.Date endDate) {
        String sql = "DELETE FROM t_audit_log WHERE create_time BETWEEN ? AND ?";
        return executeUpdateWithParams(sql, new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()));
    }

    @Override
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM t_audit_log WHERE user_id = ?";
        return executeCountQuery(sql, userId);
    }

    private List<AuditLog> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            logs.add(mapResultSetToAuditLog(rs));
        }
        return logs;
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

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLogId(rs.getLong("log_id"));
        log.setUserId(rs.getLong("user_id"));
        log.setActionType(rs.getString("action_type"));
        log.setActionDesc(rs.getString("action_desc"));
        log.setTargetType(rs.getString("target_type"));
        log.setTargetId(rs.getLong("target_id"));
        log.setBeforeData(rs.getString("before_data"));
        log.setAfterData(rs.getString("after_data"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreateTime(rs.getTimestamp("create_time"));
        return log;
    }
}
