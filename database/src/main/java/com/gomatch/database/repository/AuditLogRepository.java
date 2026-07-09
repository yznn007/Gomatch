package com.gomatch.database.repository;

import com.gomatch.database.entity.AuditLog;

import java.util.List;

public interface AuditLogRepository {
    AuditLog findById(Long logId);
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByActionType(String actionType);
    List<AuditLog> findByUserIdAndActionType(Long userId, String actionType);
    List<AuditLog> findRecent(int limit);
    AuditLog save(AuditLog log);
    int deleteByUserId(Long userId);
    int deleteByDateRange(java.util.Date startDate, java.util.Date endDate);
    int countByUserId(Long userId);
}
