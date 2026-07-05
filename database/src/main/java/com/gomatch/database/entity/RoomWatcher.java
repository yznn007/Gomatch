package com.gomatch.database.entity;

import java.util.Date;

public class RoomWatcher {
    private Long watcherId;
    private Long roomId;
    private Long userId;
    private Date joinTime;
    private Date leaveTime;

    public RoomWatcher() {}

    public RoomWatcher(Long roomId, Long userId) {
        this.roomId = roomId;
        this.userId = userId;
        this.joinTime = new Date();
    }

    public Long getWatcherId() { return watcherId; }
    public void setWatcherId(Long watcherId) { this.watcherId = watcherId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Date getJoinTime() { return joinTime; }
    public void setJoinTime(Date joinTime) { this.joinTime = joinTime; }
    public Date getLeaveTime() { return leaveTime; }
    public void setLeaveTime(Date leaveTime) { this.leaveTime = leaveTime; }

    @Override
    public String toString() {
        return "RoomWatcher{" +
                "watcherId=" + watcherId +
                ", roomId=" + roomId +
                ", userId=" + userId +
                ", joinTime=" + joinTime +
                ", leaveTime=" + leaveTime +
                '}';
    }
}