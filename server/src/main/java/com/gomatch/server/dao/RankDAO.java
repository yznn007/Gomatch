package com.gomatch.server.dao;

import com.gomatch.server.redis.RedisManager;
import com.gomatch.server.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RankDAO {
    private static final Logger log = LoggerFactory.getLogger(RankDAO.class);

    public static class RankInfo {
        private final long rankId;
        private final long userId;
        private final String username;
        private final String avatar;
        private final int rank;
        private final int score;
        private final double winRate;
        private final double aiWinRate;

        public RankInfo(long rankId, long userId, String username, String avatar,
                        int rank, int score, double winRate, double aiWinRate) {
            this.rankId = rankId;
            this.userId = userId;
            this.username = username;
            this.avatar = avatar;
            this.rank = rank;
            this.score = score;
            this.winRate = winRate;
            this.aiWinRate = aiWinRate;
        }

        public long getRankId() { return rankId; }
        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getAvatar() { return avatar; }
        public int getRank() { return rank; }
        public int getScore() { return score; }
        public double getWinRate() { return winRate; }
        public double getAiWinRate() { return aiWinRate; }
    }

    public boolean upsertRank(long userId, int score, int winCount, int loseCount, int drawCount) {
        double winRate = calculateWinRate(winCount, loseCount, drawCount);

        String sql = "INSERT INTO t_rank (user_id, score, win_rate, version) VALUES (?, ?, ?, 1) " +
                     "ON DUPLICATE KEY UPDATE score = ?, win_rate = ?, version = version + 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, score);
            ps.setDouble(3, winRate);
            ps.setInt(4, score);
            ps.setDouble(5, winRate);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                RedisManager.getInstance().updateRank(userId, score);
                log.debug("Rank updated: userId={}, score={}, winRate={}", userId, score, winRate);
                return true;
            }
        } catch (SQLException e) {
            log.error("Upsert rank error: userId={}", userId, e);
        }
        return false;
    }

    public boolean updateScore(long userId, int scoreDelta) {
        String sql = "UPDATE t_rank SET score = score + ?, version = version + 1 WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scoreDelta);
            ps.setLong(2, userId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                Integer currentScore = RedisManager.getInstance().getScore(userId);
                if (currentScore != null) {
                    RedisManager.getInstance().updateRank(userId, currentScore + scoreDelta);
                }
                log.debug("Rank score updated: userId={}, delta={}", userId, scoreDelta);
                return true;
            }
        } catch (SQLException e) {
            log.error("Update rank score error: userId={}", userId, e);
        }
        return false;
    }

    public List<RankInfo> getRanking(int limit) {
        List<Long> redisTopUsers = RedisManager.getInstance().getTopRank(limit);
        if (!redisTopUsers.isEmpty()) {
            List<RankInfo> fromRedis = getRankingFromRedis(redisTopUsers, limit);
            if (!fromRedis.isEmpty()) {
                return fromRedis;
            }
        }

        return getRankingFromDB(limit);
    }

    private List<RankInfo> getRankingFromDB(int limit) {
        String sql = "SELECT r.rank_id, r.user_id, u.username, u.avatar, r.rank, r.score, r.win_rate, r.ai_win_rate " +
                     "FROM t_rank r JOIN t_user u ON r.user_id = u.user_id " +
                     "WHERE u.status = 'ACTIVE' " +
                     "ORDER BY r.score DESC LIMIT ?";

        List<RankInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    result.add(new RankInfo(
                            rs.getLong("rank_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            resolveAvatarUrl(rs.getString("avatar")),
                            rank++,
                            rs.getInt("score"),
                            rs.getDouble("win_rate"),
                            rs.getDouble("ai_win_rate")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get ranking error", e);
        }
        return result;
    }


    public List<RankInfo> getFriendsRanking(long userId) {
        String sql = "SELECT r.rank_id, r.user_id, u.username, u.avatar, r.rank, r.score, r.win_rate, r.ai_win_rate " +
                     "FROM t_rank r JOIN t_user u ON r.user_id = u.user_id " +
                     "WHERE (u.user_id = ? OR u.user_id IN (SELECT friend_user_id FROM t_friend WHERE user_id = ? AND relation_type = 'FRIEND')) " +
                     "AND u.status = 'ACTIVE' " +
                     "ORDER BY r.score DESC";

        List<RankInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    result.add(new RankInfo(
                            rs.getLong("rank_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            resolveAvatarUrl(rs.getString("avatar")),
                            rank++,
                            rs.getInt("score"),
                            rs.getDouble("win_rate"),
                            rs.getDouble("ai_win_rate")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get friends ranking error: userId={}", userId, e);
        }
        return result;
    }

    private List<RankInfo> getRankingFromRedis(List<Long> userIds, int limit) {
        String sql = "SELECT r.rank_id, r.user_id, u.username, u.avatar, r.rank, r.score, r.win_rate, r.ai_win_rate " +
                     "FROM t_rank r JOIN t_user u ON r.user_id = u.user_id " +
                     "WHERE r.user_id IN (" + buildPlaceholders(userIds.size()) + ") " +
                     "AND u.status = 'ACTIVE'";

        List<RankInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < userIds.size(); i++) {
                ps.setLong(i + 1, userIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Map<Long, RankInfo> map = new java.util.HashMap<>();
                while (rs.next()) {
                    map.put(rs.getLong("user_id"), new RankInfo(
                            rs.getLong("rank_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            resolveAvatarUrl(rs.getString("avatar")),
                            0,
                            rs.getInt("score"),
                            rs.getDouble("win_rate"),
                            rs.getDouble("ai_win_rate")
                    ));
                }
                int rank = 1;
                for (Long userId : userIds) {
                    RankInfo info = map.get(userId);
                    if (info != null) {
                        result.add(new RankInfo(
                                info.getRankId(),
                                info.getUserId(),
                                info.getUsername(),
                                info.getAvatar(),
                                rank++,
                                info.getScore(),
                                info.getWinRate(),
                                info.getAiWinRate()
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Get ranking from Redis error", e);
        }
        // Redis 查到的用户已全部失效时，返回空列表以便回退到数据库
        if (result.isEmpty()) {
            return result;
        }
        // 若 Redis 返回的数量与预期严重不符（大量脏数据），也返回空以回退 DB
        if (result.size() < Math.min(userIds.size(), limit) / 2 && result.size() < 5) {
            log.warn("Redis ranking data is incomplete, fallback to DB");
            return new ArrayList<>();
        }
        return result;
    }

    private String buildPlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    public RankInfo getRankByUserId(long userId) {
        Long redisRank = RedisManager.getInstance().getRank(userId);
        Integer redisScore = RedisManager.getInstance().getScore(userId);

        String sql = "SELECT r.rank_id, r.user_id, u.username, u.avatar, r.rank, r.score, r.win_rate, r.ai_win_rate " +
                     "FROM t_rank r JOIN t_user u ON r.user_id = u.user_id " +
                     "WHERE r.user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int rank = redisRank != null ? redisRank.intValue() : rs.getInt("rank");
                    int score = redisScore != null ? redisScore.intValue() : rs.getInt("score");
                    return new RankInfo(
                            rs.getLong("rank_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            resolveAvatarUrl(rs.getString("avatar")),
                            rank,
                            score,
                            rs.getDouble("win_rate"),
                            rs.getDouble("ai_win_rate")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Get rank by userId error: userId={}", userId, e);
        }
        return null;
    }

    public int refreshAllRanks() {
        String sql = "INSERT INTO t_rank (user_id, score, win_rate) " +
                     "SELECT user_id, score, " +
                     "CASE WHEN (win_count + lose_count + draw_count) > 0 THEN " +
                     "ROUND(win_count * 100.0 / (win_count + lose_count + draw_count), 2) ELSE 0.00 END " +
                     "FROM t_user WHERE status = 'ACTIVE' " +
                     "ON DUPLICATE KEY UPDATE score = VALUES(score), win_rate = VALUES(win_rate), t_rank.version = t_rank.version + 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int affected = ps.executeUpdate();
            log.info("Refreshed {} ranks", affected);

            String selectSql = "SELECT user_id, score FROM t_user WHERE status = 'ACTIVE'";
            try (PreparedStatement selectPs = conn.prepareStatement(selectSql);
                 ResultSet rs = selectPs.executeQuery()) {
                RedisManager.getInstance().clearRank();
                while (rs.next()) {
                    RedisManager.getInstance().updateRank(rs.getLong("user_id"), rs.getInt("score"));
                }
                log.info("Refreshed Redis leaderboard with {} users", affected);
            }

            return affected;
        } catch (SQLException e) {
            log.error("Refresh all ranks error", e);
        }
        return 0;
    }

    public int recalculateRankOrder() {
        String sql = "UPDATE t_rank r JOIN (" +
                     "SELECT user_id, @rank := @rank + 1 as new_rank FROM t_rank ORDER BY score DESC" +
                     ") ranked ON r.user_id = ranked.user_id SET r.rank = ranked.new_rank";

        try (Connection conn = DBUtil.getConnection()) {
            try (Statement initStmt = conn.createStatement()) {
                initStmt.execute("SET @rank = 0");
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int affected = ps.executeUpdate();
                log.info("Recalculated rank order for {} users", affected);
                return affected;
            }
        } catch (SQLException e) {
            log.error("Recalculate rank order error", e);
        }
        return 0;
    }

    public int getActiveRankCount() {
        String sql = "SELECT COUNT(*) FROM t_rank";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Get active rank count error", e);
        }
        return 0;
    }

    private double calculateWinRate(int winCount, int loseCount, int drawCount) {
        int total = winCount + loseCount + drawCount;
        if (total == 0) {
            return 0.0;
        }
        return Math.round(winCount * 100.0 / total * 100) / 100.0;
    }

    /**
     * 将数据库存储的 avatar（可能是裸 OSS object key、完整 URL、已签名 URL）转成客户端可直接访问的 URL。
     * - 已经是 http(s):// 或 file:// 的，直接返回
     * - 含 ?（已带签名参数）的，直接返回
     * - 否则视为裸 object key，走 OssService 生成 30 天预签名 URL
     * - OSS 未启用或失败时，回退到空字符串（前端会显示首字母头像）
     */
    private String resolveAvatarUrl(String stored) {
        if (stored == null || stored.isEmpty()) {
            return "";
        }
        String trimmed = stored.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://")) {
            return trimmed;
        }
        if (trimmed.contains("?")) {
            return trimmed;
        }
        try {
            return StorageManager.getInstance().getOssService().generatePresignedUrl(trimmed, 86400 * 30);
        } catch (Exception e) {
            log.debug("Generate presigned url failed for {}, fallback empty: {}", trimmed, e.getMessage());
            return "";
        }
    }
}
