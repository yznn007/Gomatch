package com.gomatch.database.entity;

import java.util.Date;

public class UserMusic {
    private Long musicId;
    private Long userId;
    private String musicName;
    private String musicPath;
    private String fileName;
    private Integer fileSize;
    private Integer duration;
    private String musicType;
    private Boolean isDefault;
    private Date createTime;
    private Date updateTime;

    public UserMusic() {}

    public UserMusic(Long userId, String musicName, String musicPath, String fileName) {
        this.userId = userId;
        this.musicName = musicName;
        this.musicPath = musicPath;
        this.fileName = fileName;
        this.musicType = "LOBBY";
        this.isDefault = false;
    }

    public Long getMusicId() { return musicId; }
    public void setMusicId(Long musicId) { this.musicId = musicId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMusicName() { return musicName; }
    public void setMusicName(String musicName) { this.musicName = musicName; }
    public String getMusicPath() { return musicPath; }
    public void setMusicPath(String musicPath) { this.musicPath = musicPath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getFileSize() { return fileSize; }
    public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getMusicType() { return musicType; }
    public void setMusicType(String musicType) { this.musicType = musicType; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "UserMusic{" +
                "musicId=" + musicId +
                ", userId=" + userId +
                ", musicName='" + musicName + '\'' +
                ", musicType='" + musicType + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}
