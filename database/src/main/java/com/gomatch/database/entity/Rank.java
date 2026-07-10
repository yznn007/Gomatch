package com.gomatch.database.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Rank {
    private Long rankId;
    private Long userId;
    private Integer rank;
    private Integer score;
    private BigDecimal winRate;
    private BigDecimal aiWinRate;
    private Integer version;
    private Date updateTime;

    public Rank() {}

    public Rank(Long userId) {
        this.userId = userId;
        this.rank = 0;
        this.score = 1000;
        this.winRate = BigDecimal.ZERO;
        this.aiWinRate = BigDecimal.ZERO;
        this.version = 0;
    }

    public Long getRankId() { return rankId; }
    public void setRankId(Long rankId) { this.rankId = rankId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    public BigDecimal getAiWinRate() { return aiWinRate; }
    public void setAiWinRate(BigDecimal aiWinRate) { this.aiWinRate = aiWinRate; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "Rank{" +
                "rankId=" + rankId +
                ", userId=" + userId +
                ", rank=" + rank +
                ", score=" + score +
                ", winRate=" + winRate +
                ", aiWinRate=" + aiWinRate +
                '}';
    }
}