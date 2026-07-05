package com.gomatch.database.entity;

import java.util.Date;

public class GameRecord {
    private Long recordId;
    private Long player1Id;
    private Long player2Id;
    private String gameType;
    private Long winnerId;
    private String result;
    private String boardData;
    private Integer duration;
    private Integer moveCount;
    private Date createTime;

    public GameRecord() {}

    public GameRecord(Long player1Id, Long player2Id, String gameType) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.gameType = gameType;
        this.result = "draw";
        this.moveCount = 0;
    }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(Long player1Id) { this.player1Id = player1Id; }
    public Long getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(Long player2Id) { this.player2Id = player2Id; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getBoardData() { return boardData; }
    public void setBoardData(String boardData) { this.boardData = boardData; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getMoveCount() { return moveCount; }
    public void setMoveCount(Integer moveCount) { this.moveCount = moveCount; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "GameRecord{" +
                "recordId=" + recordId +
                ", player1Id=" + player1Id +
                ", player2Id=" + player2Id +
                ", gameType='" + gameType + '\'' +
                ", winnerId=" + winnerId +
                ", result='" + result + '\'' +
                ", duration=" + duration +
                ", moveCount=" + moveCount +
                '}';
    }
}