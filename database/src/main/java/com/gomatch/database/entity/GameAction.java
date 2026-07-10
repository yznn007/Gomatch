package com.gomatch.database.entity;

import java.util.Date;

public class GameAction {
    private Long actionId;
    private Long recordId;
    private Long userId;
    private String actionType;
    private String actionStatus;
    private Integer moveNumber;
    private String rejectReason;
    private Date opponentResponse;
    private Date createTime;

    public GameAction() {}

    public GameAction(Long recordId, Long userId, String actionType, Integer moveNumber) {
        this.recordId = recordId;
        this.userId = userId;
        this.actionType = actionType;
        this.actionStatus = "PENDING";
        this.moveNumber = moveNumber;
    }

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }
    public Integer getMoveNumber() { return moveNumber; }
    public void setMoveNumber(Integer moveNumber) { this.moveNumber = moveNumber; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Date getOpponentResponse() { return opponentResponse; }
    public void setOpponentResponse(Date opponentResponse) { this.opponentResponse = opponentResponse; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "GameAction{" +
                "actionId=" + actionId +
                ", recordId=" + recordId +
                ", userId=" + userId +
                ", actionType='" + actionType + '\'' +
                ", actionStatus='" + actionStatus + '\'' +
                ", moveNumber=" + moveNumber +
                '}';
    }
}