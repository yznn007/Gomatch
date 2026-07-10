package com.gomatch.database.repository.impl;

import com.gomatch.database.config.DatabaseConnectionManager;
import com.gomatch.database.entity.Rank;
import com.gomatch.database.repository.RankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RankRepositoryImpl implements RankRepository {
    private static final Logger logger = LoggerFactory.getLogger(RankRepositoryImpl.class);

    @Override
    public Rank findById(Long rankId) {
        String sql = "SELECT * FROM t_rank WHERE rank_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, rankId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRank(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding rank by id: {}", rankId, e);
        }
        return null;
    }

    @Override
    public Rank findByUserId(Long userId) {
        String sql = "SELECT * FROM t_rank WHERE user_id = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRank(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding rank by user id: {}", userId, e);
        }
        return null;
    }

    @Override
    public List<Rank> findTopNRanks(int limit) {
        String sql = "SELECT * FROM t_rank ORDER BY score DESC LIMIT ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding top ranks, limit: {}", limit, e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Rank> findAll() {
        String sql = "SELECT * FROM t_rank ORDER BY score DESC";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return executeQueryAndMap(stmt);
        } catch (SQLException e) {
            logger.error("Error finding all ranks", e);
        }
        return new ArrayList<>();
    }

    @Override
    public Rank save(Rank rank) {
        if (rank.getRankId() == null) {
            return insert(rank);
        } else {
            update(rank);
            return rank;
        }
    }

    private Rank insert(Rank rank) {
        String sql = "INSERT INTO t_rank (user_id, rank, score, win_rate, ai_win_rate, version) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, rank.getUserId());
            stmt.setInt(2, rank.getRank());
            stmt.setInt(3, rank.getScore());
            stmt.setBigDecimal(4, rank.getWinRate());
            stmt.setBigDecimal(5, rank.getAiWinRate());
            stmt.setInt(6, rank.getVersion());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                rank.setRankId(rs.getLong(1));
            }
            conn.commit();
            logger.info("Inserted rank: {}", rank.getRankId());
        } catch (SQLException e) {
            logger.error("Error inserting rank", e);
        }
        return rank;
    }

    @Override
    public int update(Rank rank) {
        String sql = "UPDATE t_rank SET rank = ?, score = ?, win_rate = ?, ai_win_rate = ?, " +
                "version = version + 1 WHERE rank_id = ? AND version = ?";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rank.getRank());
            stmt.setInt(2, rank.getScore());
            stmt.setBigDecimal(3, rank.getWinRate());
            stmt.setBigDecimal(4, rank.getAiWinRate());
            stmt.setLong(5, rank.getRankId());
            stmt.setInt(6, rank.getVersion());
            int rows = stmt.executeUpdate();
            conn.commit();
            logger.info("Updated rank: {}, rows affected: {}", rank.getRankId(), rows);
            return rows;
        } catch (SQLException e) {
            logger.error("Error updating rank: {}", rank.getRankId(), e);
        }
        return 0;
    }

    @Override
    public int updateScore(Long userId, int score) {
        String sql = "UPDATE t_rank SET score = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, score, userId);
    }

    @Override
    public int updateRank(Long userId, int rank) {
        String sql = "UPDATE t_rank SET rank = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, rank, userId);
    }

    @Override
    public int updateWinRate(Long userId, double winRate) {
        String sql = "UPDATE t_rank SET win_rate = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, winRate, userId);
    }

    @Override
    public int updateAiWinRate(Long userId, double aiWinRate) {
        String sql = "UPDATE t_rank SET ai_win_rate = ?, version = version + 1 WHERE user_id = ?";
        return executeUpdateWithParams(sql, aiWinRate, userId);
    }

    @Override
    public int deleteById(Long rankId) {
        String sql = "DELETE FROM t_rank WHERE rank_id = ?";
        return executeUpdateWithParams(sql, rankId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM t_rank WHERE user_id = ?";
        return executeCountQuery(sql, userId) > 0;
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM t_rank";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting ranks", e);
        }
        return 0;
    }

    private List<Rank> executeQueryAndMap(PreparedStatement stmt) throws SQLException {
        List<Rank> ranks = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            ranks.add(mapResultSetToRank(rs));
        }
        return ranks;
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

    private Rank mapResultSetToRank(ResultSet rs) throws SQLException {
        Rank rank = new Rank();
        rank.setRankId(rs.getLong("rank_id"));
        rank.setUserId(rs.getLong("user_id"));
        rank.setRank(rs.getInt("rank"));
        rank.setScore(rs.getInt("score"));
        rank.setWinRate(rs.getBigDecimal("win_rate"));
        rank.setAiWinRate(rs.getBigDecimal("ai_win_rate"));
        rank.setVersion(rs.getInt("version"));
        rank.setUpdateTime(rs.getTimestamp("update_time"));
        return rank;
    }
}