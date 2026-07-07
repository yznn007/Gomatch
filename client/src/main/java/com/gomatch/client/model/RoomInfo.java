package com.gomatch.client.model;

public class RoomInfo {
    private String roomId;
    private String roomName;
    private String ownerName;
    private int playerCount;
    private String status;
    private boolean hasPassword;

    public RoomInfo() {}

    public RoomInfo(String roomId, String roomName, String ownerName, int playerCount, String status) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.ownerName = ownerName;
        this.playerCount = playerCount;
        this.status = status;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }
}
