package com.gomatch.database.entity;

import java.util.Date;

public class User {
    private Long userId;
    private String username;
    private String password;
    private String email;
    private String avatar;
    private String role;
    private String status;
    private Integer score;
    private Integer winCount;
    private Integer loseCount;
    private Integer drawCount;
    private Integer aiWinCount;
    private Integer aiLoseCount;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    public User() {}

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = "PLAYER";
        this.status = "ACTIVE";
        this.score = 1000;
        this.winCount = 0;
        this.loseCount = 0;
        this.drawCount = 0;
        this.aiWinCount = 0;
        this.aiLoseCount = 0;
        this.version = 0;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getWinCount() { return winCount; }
    public void setWinCount(Integer winCount) { this.winCount = winCount; }
    public Integer getLoseCount() { return loseCount; }
    public void setLoseCount(Integer loseCount) { this.loseCount = loseCount; }
    public Integer getDrawCount() { return drawCount; }
    public void setDrawCount(Integer drawCount) { this.drawCount = drawCount; }
    public Integer getAiWinCount() { return aiWinCount; }
    public void setAiWinCount(Integer aiWinCount) { this.aiWinCount = aiWinCount; }
    public Integer getAiLoseCount() { return aiLoseCount; }
    public void setAiLoseCount(Integer aiLoseCount) { this.aiLoseCount = aiLoseCount; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", score=" + score +
                ", winCount=" + winCount +
                ", loseCount=" + loseCount +
                ", drawCount=" + drawCount +
                '}';
    }
}