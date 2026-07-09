package com.gomatch.database.entity;

import java.util.Date;

public class AuditLog {
    private Long logId;
    private Long userId;
    private String actionType;
    private String actionDesc;
    private String targetType;
    private Long targetId;
    private String beforeData;
    private String afterData;
    private String ipAddress;
    private Date createTime;

    public AuditLog() {}

    public AuditLog(Long userId, String actionType, String actionDesc) {
        this.userId = userId;
        this.actionType = actionType;
        this.actionDesc = actionDesc;
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActionDesc() { return actionDesc; }
    public void setActionDesc(String actionDesc) { this.actionDesc = actionDesc; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String beforeData) { this.beforeData = beforeData; }
    public String getAfterData() { return afterData; }
    public void setAfterData(String afterData) { this.afterData = afterData; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "AuditLog{" +
                "logId=" + logId +
                ", userId=" + userId +
                ", actionType='" + actionType + '\'' +
                ", actionDesc='" + actionDesc + '\'' +
                '}';
    }
}
