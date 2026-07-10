package com.gomatch.database.repository;

import com.gomatch.database.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {
    ChatMessage findById(Long messageId);
    List<ChatMessage> findByRoomId(Long roomId);
    List<ChatMessage> findByUserId(Long userId);
    List<ChatMessage> findRecentByRoomId(Long roomId, int limit);
    List<ChatMessage> findAll();
    ChatMessage save(ChatMessage message);
    int deleteById(Long messageId);
    int deleteByRoomId(Long roomId);
    int countByRoomId(Long roomId);
}