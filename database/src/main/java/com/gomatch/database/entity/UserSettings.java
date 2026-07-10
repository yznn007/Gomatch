package com.gomatch.database.entity;

import java.util.Date;

public class UserSettings {
    private Long settingsId;
    private Long userId;
    private String avatarPath;
    private String language;
    private String fontFamily;
    private Integer fontSize;
    private Integer frameRate;
    private Integer windowWidth;
    private Integer windowHeight;
    private Boolean backgroundMusicEnabled;
    private Double backgroundMusicVolume;
    private Boolean soundEffectsEnabled;
    private Double soundEffectsVolume;
    private Long lobbyMusicId;
    private String theme;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    public UserSettings() {}

    public UserSettings(Long userId) {
        this.userId = userId;
        this.language = "zh_CN";
        this.fontFamily = "SimSun";
        this.fontSize = 12;
        this.frameRate = 60;
        this.windowWidth = 900;
        this.windowHeight = 640;
        this.backgroundMusicEnabled = true;
        this.backgroundMusicVolume = 0.5;
        this.soundEffectsEnabled = true;
        this.soundEffectsVolume = 0.7;
        this.theme = "light";
        this.version = 0;
    }

    public Long getSettingsId() { return settingsId; }
    public void setSettingsId(Long settingsId) { this.settingsId = settingsId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }
    public Integer getFrameRate() { return frameRate; }
    public void setFrameRate(Integer frameRate) { this.frameRate = frameRate; }
    public Integer getWindowWidth() { return windowWidth; }
    public void setWindowWidth(Integer windowWidth) { this.windowWidth = windowWidth; }
    public Integer getWindowHeight() { return windowHeight; }
    public void setWindowHeight(Integer windowHeight) { this.windowHeight = windowHeight; }
    public Boolean getBackgroundMusicEnabled() { return backgroundMusicEnabled; }
    public void setBackgroundMusicEnabled(Boolean backgroundMusicEnabled) { this.backgroundMusicEnabled = backgroundMusicEnabled; }
    public Double getBackgroundMusicVolume() { return backgroundMusicVolume; }
    public void setBackgroundMusicVolume(Double backgroundMusicVolume) { this.backgroundMusicVolume = backgroundMusicVolume; }
    public Boolean getSoundEffectsEnabled() { return soundEffectsEnabled; }
    public void setSoundEffectsEnabled(Boolean soundEffectsEnabled) { this.soundEffectsEnabled = soundEffectsEnabled; }
    public Double getSoundEffectsVolume() { return soundEffectsVolume; }
    public void setSoundEffectsVolume(Double soundEffectsVolume) { this.soundEffectsVolume = soundEffectsVolume; }
    public Long getLobbyMusicId() { return lobbyMusicId; }
    public void setLobbyMusicId(Long lobbyMusicId) { this.lobbyMusicId = lobbyMusicId; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "UserSettings{" +
                "settingsId=" + settingsId +
                ", userId=" + userId +
                ", language='" + language + '\'' +
                ", theme='" + theme + '\'' +
                '}';
    }
}
