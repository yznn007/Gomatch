package com.gomatch.database.repository;

import com.gomatch.database.entity.GameAction;

import java.util.Date;
import java.util.List;

public interface GameActionRepository {
    GameAction findById(Long actionId);
    List<GameAction> findByRecordId(Long recordId);
    List<GameAction> findByUserId(Long userId);
    List<GameAction> findByRecordIdAndStatus(Long recordId, String status);
    List<GameAction> findAll();
    GameAction save(GameAction action);
    int updateStatus(Long actionId, String status);
    int updateRejectReason(Long actionId, String reason);
    int updateOpponentResponse(Long actionId, Date responseTime);
    int deleteById(Long actionId);
    int deleteByRecordId(Long recordId);
}