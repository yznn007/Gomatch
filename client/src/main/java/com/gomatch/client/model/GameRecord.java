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
}
