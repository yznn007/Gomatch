package com.gomatch.database.repository;

import com.gomatch.database.entity.SocketSession;

import java.util.Date;
import java.util.List;

public interface SocketSessionRepository {
    SocketSession findById(Long sessionId);
    SocketSession findBySessionKey(String sessionKey);
    SocketSession findByUserId(Long userId);
    List<SocketSession> findByStatus(String status);
    List<SocketSession> findByRoomId(Long roomId);
    List<SocketSession> findExpiredSessions(Date timeout);
    List<SocketSession> findAll();
    SocketSession save(SocketSession session);
    int update(SocketSession session);
    int updateStatus(Long sessionId, String status);
    int updateHeartbeat(Long sessionId);
    int updateConnectionId(Long sessionId, String connectionId);
    int updateRoomId(Long sessionId, Long roomId);
    int updateDisconnectTime(Long sessionId, Date disconnectTime);
    int updateDisconnectReason(Long sessionId, String disconnectReason);
    int updateReconnectAttempts(Long sessionId, Integer reconnectAttempts);
    int deleteById(Long sessionId);
    int deleteByUserId(Long userId);
    int countByStatus(String status);
}