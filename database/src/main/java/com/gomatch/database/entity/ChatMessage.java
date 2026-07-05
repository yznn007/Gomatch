package com.gomatch.database.entity;

import java.util.Date;

public class ChatMessage {
    private Long messageId;
    private Long roomId;
    private Long userId;
    private String content;
    private String messageType;
    private Date createTime;

    public ChatMessage() {}

    public ChatMessage(Long roomId, Long userId, String content) {
        this.roomId = roomId;
        this.userId = userId;
        this.content = content;
        this.messageType = "text";
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId=" + messageId +
                ", roomId=" + roomId +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}