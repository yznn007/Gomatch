package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.UserSettings;
import com.gomatch.database.repository.UserSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class UserSettingsRepositoryImpl implements UserSettingsRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserSettingsRepositoryImpl.class);

    @Override
    public UserSettings findByUserId(Long userId) {
        String sql = "SELECT * FROM t_user_settings WHERE user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUserSettings(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding settings by user id: {}", userId, e);
        }
        return null;
    }

    @Override
    public UserSettings save(UserSettings settings) {
        if (settings.getSettingsId() == null) {
            return insert(settings);
        } else {
            update(settings);
            return settings;
        }
    }

    private UserSettings insert(UserSettings settings) {
        String sql = "INSERT INTO t_user_settings (user_id, avatar_path, language, font_family, font_size, " +
                "frame_rate, window_width, window_height, background_music_enabled, background_music_volume, " +
                "sound_effects_enabled, sound_effects_volume, lobby_music_id, theme, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            stmt.setLong(idx++, settings.getUserId());
            stmt.setString(idx++, settings.getAvatarPath());
            stmt.setString(idx++, settings.getLanguage());
            stmt.setString(idx++, settings.getFontFamily());
            stmt.setInt(idx++, settings.getFontSize());
            stmt.setInt(idx++, settings.getFrameRate());
            stmt.setInt(idx++, settings.getWindowWidth());
            stmt.setInt(idx++, settings.getWindowHeight());
            stmt.setBoolean(idx++, settings.getBackgroundMusicEnabled());
            stmt.setDouble(idx++, settings.getBackgroundMusicVolume());
            stmt.setBoolean(idx++, settings.getSoundEffectsEnabled());
            stmt.setDouble(idx++, settings.getSoundEffectsVolume());
            stmt.setObject(idx++, settings.getLobbyMusicId());
            stmt.setString(idx++, settings.getTheme());
            stmt.setInt(idx++, settings.getVersion());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                settings.setSettingsId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted settings for user: {}", settings.getUserId());
        } catch (SQLException e) {
            logger.error("Error inserting settings for user: {}", settings.getUserId(), e);
        }
        return settings;
    }

    @Override
    public int update(UserSettings settings) {
        String sql = "UPDATE t_user_settings SET avatar_path = ?, language = ?, font_family = ?, font_size = ?, " +
                "frame_rate = ?, window_width = ?, window_height = ?, background_music_enabled = ?, " +
                "background_music_volume = ?, sound_effects_enabled = ?, sound_effects_volume = ?, " +
                "lobby_music_id = ?, theme = ?, version = version + 1 WHERE user_id = ? AND version = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            stmt.setString(idx++, settings.getAvatarPath());
            stmt.setString(idx++, settings.getLanguage());
            stmt.setString(idx++, settings.getFontFamily());
            stmt.setInt(idx++, settings.getFontSize());
            stmt.setInt(idx++, settings.getFrameRate());
            stmt.setInt(idx++, settings.getWindowWidth());
            stmt.setInt(idx++, settings.getWindowHeight());
            stmt.setBoolean(idx++, settings.getBackgroundMusicEnabled());
            stmt.setDouble(idx++, settings.getBackgroundMusicVolume());
            stmt.setBoolean(idx++, settings.getSoundEffectsEnabled());
            stmt.setDouble(idx++, settings.getSoundEffectsVolume());
            stmt.setObject(idx++, settings.getLobbyMusicId());
            stmt.setString(idx++, settings.getTheme());
            stmt.setLong(idx++, settings.getUserId());
            stmt.setInt(idx++, settings.getVersion());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated settings for user: {}, rows affected: {}", settings.getUserId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating settings for user: {}", settings.getUserId(), e);
        }
        return 0;
    }

    @Override
    public int updateAvatarPath(Long userId, String avatarPath) {
        String sql = "UPDATE t_user_settings SET avatar_path = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, avatarPath, userId);
    }

    @Override
    public int updateLanguage(Long userId, String language) {
        String sql = "UPDATE t_user_settings SET language = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, language, userId);
    }

    @Override
    public int updateFontFamily(Long userId, String fontFamily) {
        String sql = "UPDATE t_user_settings SET font_family = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, fontFamily, userId);
    }

    @Override
    public int updateFontSize(Long userId, Integer fontSize) {
        String sql = "UPDATE t_user_settings SET font_size = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, fontSize, userId);
    }

    @Override
    public int updateFrameRate(Long userId, Integer frameRate) {
        String sql = "UPDATE t_user_settings SET frame_rate = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, frameRate, userId);
    }

    @Override
    public int updateWindowSize(Long userId, Integer windowWidth, Integer windowHeight) {
        String sql = "UPDATE t_user_settings SET window_width = ?, window_height = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, windowWidth, windowHeight, userId);
    }

    @Override
    public int updateBackgroundMusicEnabled(Long userId, Boolean enabled) {
        String sql = "UPDATE t_user_settings SET background_music_enabled = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, enabled, userId);
    }

    @Override
    public int updateBackgroundMusicVolume(Long userId, Double volume) {
        String sql = "UPDATE t_user_settings SET background_music_volume = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, volume, userId);
    }

    @Override
    public int updateSoundEffectsEnabled(Long userId, Boolean enabled) {
        String sql = "UPDATE t_user_settings SET sound_effects_enabled = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, enabled, userId);
    }

    @Override
    public int updateSoundEffectsVolume(Long userId, Double volume) {
        String sql = "UPDATE t_user_settings SET sound_effects_volume = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, volume, userId);
    }

    @Override
    public int updateLobbyMusicId(Long userId, Long musicId) {
        String sql = "UPDATE t_user_settings SET lobby_music_id = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, musicId, userId);
    }

    @Override
    public int updateTheme(Long userId, String theme) {
        String sql = "UPDATE t_user_settings SET theme = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, theme, userId);
    }

    @Override
    public int deleteByUserId(Long userId) {
        String sql = "DELETE FROM t_user_settings WHERE user_id = ?";
        return executeUpdateWithParams(sql, userId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM t_user_settings WHERE user_id = ?";
        return executeCountQuery(sql, userId) > 0;
    }

    private int executeUpdateWithParams(String sql, Object... params) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            int rows = stmt.executeUpdate();
            conn.commit();
            return rows;
        } catch (SQLException e) {
            logger.error("Error executing update: {}", sql, e);
        }
        return 0;
    }

    private int executeCountQuery(String sql, Object param) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error executing count query: {}", sql, e);
        }
        return 0;
    }

    private UserSettings mapResultSetToUserSettings(ResultSet rs) throws SQLException {
        UserSettings settings = new UserSettings();
        settings.setSettingsId(rs.getLong("settings_id"));
        settings.setUserId(rs.getLong("user_id"));
        settings.setAvatarPath(rs.getString("avatar_path"));
        settings.setLanguage(rs.getString("language"));
        settings.setFontFamily(rs.getString("font_family"));
        settings.setFontSize(rs.getInt("font_size"));
        settings.setFrameRate(rs.getInt("frame_rate"));
        settings.setWindowWidth(rs.getInt("window_width"));
        settings.setWindowHeight(rs.getInt("window_height"));
        settings.setBackgroundMusicEnabled(rs.getBoolean("background_music_enabled"));
        settings.setBackgroundMusicVolume(rs.getDouble("background_music_volume"));
        settings.setSoundEffectsEnabled(rs.getBoolean("sound_effects_enabled"));
        settings.setSoundEffectsVolume(rs.getDouble("sound_effects_volume"));
        settings.setLobbyMusicId(rs.getLong("lobby_music_id"));
        settings.setTheme(rs.getString("theme"));
        settings.setVersion(rs.getInt("version"));
        settings.setCreateTime(rs.getTimestamp("create_time"));
        settings.setUpdateTime(rs.getTimestamp("update_time"));
        return settings;
    }
}
