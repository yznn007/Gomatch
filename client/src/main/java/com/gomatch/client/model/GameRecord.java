package com.gomatch.client.model;

/**
 * 对战记录条目，用于 ProfileController 的 TableView。
 */
public class GameRecord {
    private String opponent;
    private String result;
    private String time;
    private String replay;
    private String moves;   // 棋盘数据（JSON），用于回放
    private int moveCount;
    private int duration;   // 对局时长（秒）
    private int scoreChange;
    private String player1Id;
    private String player1Name;
    private String player2Id;
    private String player2Name;

    public GameRecord() {}

    public GameRecord(String opponent, String result, String time, String replay) {
        this.opponent = opponent;
        this.result   = result;
        this.time     = time;
        this.replay   = replay;
    }

    public GameRecord(String opponent, String result, String time, String replay, String moves) {
        this.opponent = opponent;
        this.result   = result;
        this.time     = time;
        this.replay   = replay;
        this.moves    = moves;
    }

    public String getOpponent() { return opponent; }
    public void setOpponent(String opponent) { this.opponent = opponent; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getReplay() { return replay; }
    public void setReplay(String replay) { this.replay = replay; }

    public String getMoves() { return moves; }
    public void setMoves(String moves) { this.moves = moves; }

    public int getMoveCount() { return moveCount; }
    public void setMoveCount(int moveCount) { this.moveCount = moveCount; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getScoreChange() { return scoreChange; }
    public void setScoreChange(int scoreChange) { this.scoreChange = scoreChange; }

    public String getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; }
    public String getPlayer1Name() { return player1Name; }
    public void setPlayer1Name(String player1Name) { this.player1Name = player1Name; }
    public String getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; }
    public String getPlayer2Name() { return player2Name; }
    public void setPlayer2Name(String player2Name) { this.player2Name = player2Name; }
}
