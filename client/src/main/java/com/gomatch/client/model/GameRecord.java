package com.gomatch.client.model;

/**
 * 对战记录条目，用于 ProfileController 的 TableView。
 */
public class GameRecord {
    private String opponent;
    private String result;
    private String time;
    private String replay;

    public GameRecord() {}

    public GameRecord(String opponent, String result, String time, String replay) {
        this.opponent = opponent;
        this.result   = result;
        this.time     = time;
        this.replay   = replay;
    }

    public String getOpponent() { return opponent; }
    public void setOpponent(String opponent) { this.opponent = opponent; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getReplay() { return replay; }
    public void setReplay(String replay) { this.replay = replay; }
}
