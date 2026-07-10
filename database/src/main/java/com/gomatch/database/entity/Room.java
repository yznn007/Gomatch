package com.gomatch.database.entity;

import java.util.Date;

public class Room {
    private Long roomId;
    private String roomName;
    private String password;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Integer watcherCount;
    private String status;
    private Long hostId;
    private Boolean allowWatch;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    public Room() {}

    public Room(String roomName, Long hostId) {
        this.roomName = roomName;
        this.hostId = hostId;
        this.maxPlayers = 2;
        this.currentPlayers = 1;
        this.watcherCount = 0;
        this.status = "waiting";
        this.allowWatch = true;
        this.version = 0;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public Integer getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(Integer currentPlayers) { this.currentPlayers = currentPlayers; }
    public Integer getWatcherCount() { return watcherCount; }
    public void setWatcherCount(Integer watcherCount) { this.watcherCount = watcherCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public Boolean getAllowWatch() { return allowWatch; }
    public void setAllowWatch(Boolean allowWatch) { this.allowWatch = allowWatch; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", roomName='" + roomName + '\'' +
                ", currentPlayers=" + currentPlayers +
                ", status='" + status + '\'' +
                ", hostId=" + hostId +
                ", allowWatch=" + allowWatch +
                '}';
    }
}