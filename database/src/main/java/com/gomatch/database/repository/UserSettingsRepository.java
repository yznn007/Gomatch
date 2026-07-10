package com.gomatch.database.repository;

import com.gomatch.database.entity.UserSettings;

public interface UserSettingsRepository {
    UserSettings findByUserId(Long userId);
    UserSettings save(UserSettings settings);
    int update(UserSettings settings);
    int updateAvatarPath(Long userId, String avatarPath);
    int updateLanguage(Long userId, String language);
    int updateFontFamily(Long userId, String fontFamily);
    int updateFontSize(Long userId, Integer fontSize);
    int updateFrameRate(Long userId, Integer frameRate);
    int updateWindowSize(Long userId, Integer windowWidth, Integer windowHeight);
    int updateBackgroundMusicEnabled(Long userId, Boolean enabled);
    int updateBackgroundMusicVolume(Long userId, Double volume);
    int updateSoundEffectsEnabled(Long userId, Boolean enabled);
    int updateSoundEffectsVolume(Long userId, Double volume);
    int updateLobbyMusicId(Long userId, Long musicId);
    int updateTheme(Long userId, String theme);
    int deleteByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
