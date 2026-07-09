package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.UserMusic;
import com.gomatch.database.repository.UserMusicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserMusicRepositoryImpl implements UserMusicRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserMusicRepositoryImpl.class);

    @Override
    public UserMusic findById(Long musicId) {
        String sql = "SELECT * FROM t_user_music WHERE music_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, musicId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUserMusic(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding music by id: {}", musicId, e);
        }
        return null;
    }

    @Override
    public List<UserMusic> findByUserId(Long userId) {
        String sql = "SELECT * FROM t_user_music WHERE user_id = ? ORDER BY create_time DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding music by user id: {}", userId, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<UserMusic> findByUserIdAndMusicType(Long userId, String musicType) {
        String sql = "SELECT * FROM t_user_music WHERE user_id = ? AND music_type = ? ORDER BY create_time DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, musicType);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding music by user id: {} and type: {}", userId, musicType, e);
        }
        return new ArrayList<>();
    }

    @Override
    public UserMusic findDefaultMusic(Long userId) {
        String sql = "SELECT * FROM t_user_music WHERE user_id = ? AND is_default = TRUE";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUserMusic(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding default music for user: {}", userId, e);
        }
        return null;
    }

    @Override
    public UserMusic findByUserIdAndFileName(Long userId, String fileName) {
        String sql = "SELECT * FROM t_user_music WHERE user_id = ? AND file_name = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, fileName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUserMusic(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding music by user id: {} and file name: {}", userId, fileName, e);
        }
        return null;
    }

    @Override
    public UserMusic save(UserMusic music) {
        if (music.getMusicId() == null) {
            return insert(music);
        } else {
            update(music);
            return music;
        }
    }

    private UserMusic insert(UserMusic music) {
        String sql = "INSERT INTO t_user_music (user_id, music_name, music_path, file_name, file_size, " +
                "duration, music_type, is_default) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            stmt.setLong(idx++, music.getUserId());
            stmt.setString(idx++, music.getMusicName());
            stmt.setString(idx++, music.getMusicPath());
            stmt.setString(idx++, music.getFileName());
            stmt.setInt(idx++, music.getFileSize());
            stmt.setObject(idx++, music.getDuration());
            stmt.setString(idx++, music.getMusicType());
            stmt.setBoolean(idx++, music.getIsDefault());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                music.setMusicId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted music: {} for user: {}", music.getMusicId(), music.getUserId());
        } catch (SQLException e) {
            logger.error("Error inserting music for user: {}", music.getUserId(), e);
        }
        return music;
    }

    @Override
    public int update(UserMusic music) {
        String sql = "UPDATE t_user_music SET music_name = ?, music_path = ?, file_name = ?, file_size = ?, " +
                "duration = ?, music_type = ?, is_default = ? WHERE music_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            stmt.setString(idx++, music.getMusicName());
            stmt.setString(idx++, music.getMusicPath());
            stmt.setString(idx++, music.getFileName());
            stmt.setInt(idx++, music.getFileSize());
            stmt.setObject(idx++, music.getDuration());
            stmt.setString(idx++, music.getMusicType());
            stmt.setBoolean(idx++, music.getIsDefault());
            stmt.setLong(idx++, music.getMusicId());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated music: {}, rows affected: {}", music.getMusicId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating music: {}", music.getMusicId(), e);
        }
        return 0;
    }

    @Override
    public int updateDefaultMusic(Long userId, Long musicId) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            String clearSql = "UPDATE t_user_music SET is_default = FALSE WHERE user_id = ?";
            try (PreparedStatement clearStmt = conn.prepareStatement(clearSql)) {
                clearStmt.setLong(1, userId);
                clearStmt.executeUpdate();
            }
            String setSql = "UPDATE t_user_music SET is_default = TRUE WHERE music_id = ? AND user_id = ?";
            try (PreparedStatement setStmt = conn.prepareStatement(setSql)) {
                setStmt.setLong(1, musicId);
                setStmt.setLong(2, userId);
                int rows = setStmt.executeUpdate();
                conn.commit();
                logger.info("Set default music: {} for user: {}, rows affected: {}", musicId, userId, rows);
                return rows;
            }
        } catch (SQLException e) {
            logger.error("Error updating default music for user: {}", userId, e);
        }
        return 0;
    }

    @Override
    public int clearDefaultMusic(Long userId) {
        String sql = "UPDATE t_user_music SET is_default = FALSE WHERE user_id = ?";
        return executeUpdateWithParams(sql, userId);
    }

    @Override
    public int deleteById(Long musicId) {
        String sql = "DELETE FROM t_user_music WHERE music_id = ?";
        return executeUpdateWithParams(sql, musicId);
    }

    @Override
    public int deleteByUserId(Long userId) {
        String sql = "DELETE FROM t_user_music WHERE user_id = ?";
        return executeUpdateWithParams(sql, userId);
    }

    @Override
    public boolean existsById(Long musicId) {
        String sql = "SELECT COUNT(*) FROM t_user_music WHERE music_id = ?";
        return executeCountQuery(sql, musicId) > 0;
    }

    @Override
    public boolean existsByUserIdAndFileName(Long userId, String fileName) {
        String sql = "SELECT COUNT(*) FROM t_user_music WHERE user_id = ? AND file_name = ?";
        return executeCountQueryWithTwoParams(sql, userId, fileName) > 0;
    }

    @Override
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM t_user_music WHERE user_id = ?";
        return executeCountQuery(sql, userId);
    }

    private List<UserMusic> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<UserMusic> musics = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            musics.add(mapResultSetToUserMusic(rs));
        }
        return musics;
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

    private int executeCountQueryWithTwoParams(String sql, Object param1, Object param2) {
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param1);
            stmt.setObject(2, param2);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error executing count query: {}", sql, e);
        }
        return 0;
    }

    private UserMusic mapResultSetToUserMusic(ResultSet rs) throws SQLException {
        UserMusic music = new UserMusic();
        music.setMusicId(rs.getLong("music_id"));
        music.setUserId(rs.getLong("user_id"));
        music.setMusicName(rs.getString("music_name"));
        music.setMusicPath(rs.getString("music_path"));
        music.setFileName(rs.getString("file_name"));
        music.setFileSize(rs.getInt("file_size"));
        music.setDuration(rs.getInt("duration"));
        music.setMusicType(rs.getString("music_type"));
        music.setIsDefault(rs.getBoolean("is_default"));
        music.setCreateTime(rs.getTimestamp("create_time"));
        music.setUpdateTime(rs.getTimestamp("update_time"));
        return music;
    }
}
