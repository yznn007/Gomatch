package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.ChatMessage;
import com.gomatch.database.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageRepositoryImpl.class);

    @Override
    public ChatMessage findById(Long messageId) {
        String sql = "SELECT * FROM t_chat_message WHERE message_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToChatMessage(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding chat message by id: {}", messageId, e);
        }
        return null;
    }

    @Override
    public List<ChatMessage> findByRoomId(Long roomId) {
        String sql = "SELECT * FROM t_chat_message WHERE room_id = ? ORDER BY create_time ASC";
        return executeQueryWithParam(sql, roomId);
    }

    @Override
    public List<ChatMessage> findByUserId(Long userId) {
        String sql = "SELECT * FROM t_chat_message WHERE user_id = ? ORDER BY create_time DESC";
        return executeQueryWithParam(sql, userId);
    }

    @Override
    public List<ChatMessage> findRecentByRoomId(Long roomId, int limit) {
        String sql = "SELECT * FROM t_chat_message WHERE room_id = ? ORDER BY create_time DESC LIMIT ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setInt(2, limit);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding recent messages for room: {}, limit: {}", roomId, limit, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<ChatMessage> findAll() {
        String sql = "SELECT * FROM t_chat_message ORDER BY create_time DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all chat messages", e);
        }
        return new ArrayList<>();
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        String sql = "INSERT INTO t_chat_message (room_id, user_id, content, message_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, message.getRoomId());
            stmt.setLong(2, message.getUserId());
            stmt.setString(3, message.getContent());
            stmt.setString(4, message.getMessageType());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                message.setMessageId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted chat message: {}", message.getMessageId());
        } catch (SQLException e) {
            logger.error("Error inserting chat message", e);
        }
        return message;
    }

    @Override
    public int deleteById(Long messageId) {
        String sql = "DELETE FROM t_chat_message WHERE message_id = ?";
        return executeUpdateWithParams(sql, messageId);
    }

    @Override
    public int deleteByRoomId(Long roomId) {
        String sql = "DELETE FROM t_chat_message WHERE room_id = ?";
        return executeUpdateWithParams(sql, roomId);
    }

    @Override
    public int countByRoomId(Long roomId) {
        String sql = "SELECT COUNT(*) FROM t_chat_message WHERE room_id = ?";
        return executeCountQuery(sql, roomId);
    }

    private List<ChatMessage> executeQueryWithParam(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return new ArrayList<>();
    }

    private List<ChatMessage> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<ChatMessage> messages = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            messages.add(mapResultSetToChatMessage(rs));
        }
        return messages;
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

    private ChatMessage mapResultSetToChatMessage(ResultSet rs) throws SQLException {
        ChatMessage message = new ChatMessage();
        message.setMessageId(rs.getLong("message_id"));
        message.setRoomId(rs.getLong("room_id"));
        message.setUserId(rs.getLong("user_id"));
        message.setContent(rs.getString("content"));
        message.setMessageType(rs.getString("message_type"));
        message.setCreateTime(rs.getTimestamp("create_time"));
        return message;
    }
}