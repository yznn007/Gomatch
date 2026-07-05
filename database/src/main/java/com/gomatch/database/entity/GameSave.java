package com.gomatch.database.entity;

import java.util.Date;

public class GameSave {
    private Long saveId;
    private Long userId;
    private Long recordId;
    private String fileUri;
    private String saveName;
    private String boardData;
    private Integer currentPlayer;
    private String gameStatus;
    private String checksum;
    private Integer fileSize;
    private Date createTime;
    private Date updateTime;

    public GameSave() {}

    public GameSave(Long userId, String fileUri, String boardData, Integer currentPlayer) {
        this.userId = userId;
        this.fileUri = fileUri;
        this.boardData = boardData;
        this.currentPlayer = currentPlayer;
        this.gameStatus = "playing";
        this.fileSize = 0;
    }

    public Long getSaveId() { return saveId; }
    public void setSaveId(Long saveId) { this.saveId = saveId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getFileUri() { return fileUri; }
    public void setFileUri(String fileUri) { this.fileUri = fileUri; }
    public String getSaveName() { return saveName; }
    public void setSaveName(String saveName) { this.saveName = saveName; }
    public String getBoardData() { return boardData; }
    public void setBoardData(String boardData) { this.boardData = boardData; }
    public Integer getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(Integer currentPlayer) { this.currentPlayer = currentPlayer; }
    public String getGameStatus() { return gameStatus; }
    public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Integer getFileSize() { return fileSize; }
    public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "GameSave{" +
                "saveId=" + saveId +
                ", userId=" + userId +
                ", fileUri='" + fileUri + '\'' +
                ", saveName='" + saveName + '\'' +
                ", gameStatus='" + gameStatus + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}