package com.gomatch.database.entity;

import java.util.Date;

public class SocketSession {
    private Long sessionId;
    private Long userId;
    private String sessionKey;
    private String clientIp;
    private String connectionId;
    private Long roomId;
    private String status;
    private Date lastHeartbeat;
    private Date disconnectTime;
    private String disconnectReason;
    private Integer reconnectAttempts;
    private Date createTime;
    private Date updateTime;

    public SocketSession() {}

    public SocketSession(Long userId, String sessionKey) {
        this.userId = userId;
        this.sessionKey = sessionKey;
        this.status = "connected";
        this.reconnectAttempts = 0;
        this.lastHeartbeat = new Date();
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getConnectionId() { return connectionId; }
    public void setConnectionId(String connectionId) { this.connectionId = connectionId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Date lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public Date getDisconnectTime() { return disconnectTime; }
    public void setDisconnectTime(Date disconnectTime) { this.disconnectTime = disconnectTime; }
    public String getDisconnectReason() { return disconnectReason; }
    public void setDisconnectReason(String disconnectReason) { this.disconnectReason = disconnectReason; }
    public Integer getReconnectAttempts() { return reconnectAttempts; }
    public void setReconnectAttempts(Integer reconnectAttempts) { this.reconnectAttempts = reconnectAttempts; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "SocketSession{" +
                "sessionId=" + sessionId +
                ", userId=" + userId +
                ", sessionKey='" + sessionKey + '\'' +
                ", status='" + status + '\'' +
                ", roomId=" + roomId +
                '}';
    }
}