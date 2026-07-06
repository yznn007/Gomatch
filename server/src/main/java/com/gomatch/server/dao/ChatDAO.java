package com.gomatch.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatDAO {
    private static final Logger log = LoggerFactory.getLogger(ChatDAO.class);

    public static class ChatMessage {
        private final long messageId;
        private final long roomId;
        private final long userId;
        private final String username;
        private final String content;
        private final String messageType;
        private final String createTime;

        public ChatMessage(long messageId, long roomId, long userId, String username,
                          String content, String messageType, String createTime) {
            this.messageId = messageId;
            this.roomId = roomId;
            this.userId = userId;
            this.username = username;
            this.content = content;
            this.messageType = messageType;
            this.createTime = createTime;
        }

        public long getMessageId() { return messageId; }
        public long getRoomId() { return roomId; }
        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getContent() { return content; }
        public String getMessageType() { return messageType; }
        public String getCreateTime() { return createTime; }
    }

    public long saveMessage(String roomId, long userId, String content, String messageType) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Cannot save empty message");
            return -1;
        }

        String sql = "INSERT INTO t_chat_message (room_id, user_id, content, message_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roomId);
            ps.setLong(2, userId);
            ps.setString(3, content);
            ps.setString(4, messageType);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long messageId = rs.getLong(1);
                    log.debug("Chat message saved: messageId={}, roomId={}, userId={}", messageId, roomId, userId);
                    return messageId;
                }
            }
        } catch (SQLException e) {
            log.error("Save chat message error: roomId={}, userId={}", roomId, userId, e);
        }
        return -1;
    }

    public List<ChatMessage> getMessagesByRoomId(String roomId, int page, int size) {
        String sql = "SELECT m.message_id, m.room_id, m.user_id, u.username, m.content, m.message_type, m.create_time " +
                     "FROM t_chat_message m JOIN t_user u ON m.user_id = u.user_id " +
                     "WHERE m.room_id = ? " +
                     "ORDER BY m.create_time DESC LIMIT ? OFFSET ?";

        List<ChatMessage> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setInt(2, size);
            ps.setInt(3, page * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ChatMessage(
                            rs.getLong("message_id"),
                            rs.getLong("room_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getString("message_type"),
                            rs.getString("create_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get chat messages error: roomId={}", roomId, e);
        }
        return result;
    }

    public List<ChatMessage> getRecentMessages(String roomId, int limit) {
        String sql = "SELECT m.message_id, m.room_id, m.user_id, u.username, m.content, m.message_type, m.create_time " +
                     "FROM t_chat_message m JOIN t_user u ON m.user_id = u.user_id " +
                     "WHERE m.room_id = ? " +
                     "ORDER BY m.create_time DESC LIMIT ?";

        List<ChatMessage> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ChatMessage(
                            rs.getLong("message_id"),
                            rs.getLong("room_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getString("message_type"),
                            rs.getString("create_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get recent chat messages error: roomId={}", roomId, e);
        }
        return result;
    }

    public int getMessageCount(String roomId) {
        String sql = "SELECT COUNT(*) FROM t_chat_message WHERE room_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Get message count error: roomId={}", roomId, e);
        }
        return 0;
    }

    public boolean deleteMessagesByRoomId(String roomId) {
        String sql = "DELETE FROM t_chat_message WHERE room_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Deleted {} messages for room {}", affected, roomId);
                return true;
            }
        } catch (SQLException e) {
            log.error("Delete messages error: roomId={}", roomId, e);
        }
        return false;
    }
}