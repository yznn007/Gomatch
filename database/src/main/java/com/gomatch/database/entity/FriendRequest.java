package com.gomatch.database.entity;

import java.util.Date;

public class FriendRequest {
    private Long requestId;
    private Long fromUserId;
    private Long toUserId;
    private String status;
    private String message;
    private Date createTime;
    private Date respondTime;

    public FriendRequest() {}

    public FriendRequest(Long fromUserId, Long toUserId) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = "PENDING";
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getRespondTime() { return respondTime; }
    public void setRespondTime(Date respondTime) { this.respondTime = respondTime; }

    @Override
    public String toString() {
        return "FriendRequest{" +
                "requestId=" + requestId +
                ", fromUserId=" + fromUserId +
                ", toUserId=" + toUserId +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}