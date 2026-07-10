package com.gomatch.client.model;

public class User {
    private long userId;
    private String username;
    private int score;
    private int winCount;
    private int loseCount;
    private int drawCount;
    private int rank;
    private String winRate;
    private String role = "PLAYER";
    private String phone;
    private String avatarUrl;

    public User() {}

    public User(long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getWinCount() { return winCount; }
    public void setWinCount(int winCount) { this.winCount = winCount; }
    public int getLoseCount() { return loseCount; }
    public void setLoseCount(int loseCount) { this.loseCount = loseCount; }
    public int getDrawCount() { return drawCount; }
    public void setDrawCount(int drawCount) { this.drawCount = drawCount; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getWinRate() { return winRate; }
    public void setWinRate(String winRate) { this.winRate = winRate; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

