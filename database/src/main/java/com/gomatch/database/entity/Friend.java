package com.gomatch.database.entity;

import java.util.Date;

public class Friend {
    private Long friendId;
    private Long userId;
    private Long friendUserId;
    private String relationType;
    private String remark;
    private Date createTime;

    public Friend() {}

    public Friend(Long userId, Long friendUserId) {
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.relationType = "FRIEND";
    }

    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getFriendUserId() { return friendUserId; }
    public void setFriendUserId(Long friendUserId) { this.friendUserId = friendUserId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "Friend{" +
                "friendId=" + friendId +
                ", userId=" + userId +
                ", friendUserId=" + friendUserId +
                ", relationType='" + relationType + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}