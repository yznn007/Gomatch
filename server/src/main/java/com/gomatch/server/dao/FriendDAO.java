package com.gomatch.server.dao;

import com.gomatch.server.redis.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendDAO {
    private static final Logger log = LoggerFactory.getLogger(FriendDAO.class);

    public static class FriendInfo {
        private final long friendId;
        private final long userId;
        private final long friendUserId;
        private final String friendUsername;
        private final int friendScore;
        private final String relationType;
        private final String remark;
        private final boolean online;

        public FriendInfo(long friendId, long userId, long friendUserId,
                         String friendUsername, int friendScore,
                         String relationType, String remark) {
            this(friendId, userId, friendUserId, friendUsername, friendScore, relationType, remark, false);
        }

        public FriendInfo(long friendId, long userId, long friendUserId,
                         String friendUsername, int friendScore,
                         String relationType, String remark, boolean online) {
            this.friendId = friendId;
            this.userId = userId;
            this.friendUserId = friendUserId;
            this.friendUsername = friendUsername;
            this.friendScore = friendScore;
            this.relationType = relationType;
            this.remark = remark;
            this.online = online;
        }

        public long getFriendId() { return friendId; }
        public long getUserId() { return userId; }
        public long getFriendUserId() { return friendUserId; }
        public String getFriendUsername() { return friendUsername; }
        public int getFriendScore() { return friendScore; }
        public String getRelationType() { return relationType; }
        public String getRemark() { return remark; }
        public boolean isOnline() { return online; }
    }

    public static class FriendRequestInfo {
        private final long requestId;
        private final long fromUserId;
        private final String fromUsername;
        private final int fromScore;
        private final long toUserId;
        private final String status;
        private final String message;
        private final String createTime;

        public FriendRequestInfo(long requestId, long fromUserId, String fromUsername,
                                int fromScore, long toUserId, String status,
                                String message, String createTime) {
            this.requestId = requestId;
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.fromScore = fromScore;
            this.toUserId = toUserId;
            this.status = status;
            this.message = message;
            this.createTime = createTime;
        }

        public long getRequestId() { return requestId; }
        public long getFromUserId() { return fromUserId; }
        public String getFromUsername() { return fromUsername; }
        public int getFromScore() { return fromScore; }
        public long getToUserId() { return toUserId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getCreateTime() { return createTime; }
    }

    public boolean sendFriendRequest(long fromUserId, long toUserId, String message) {
        if (fromUserId == toUserId) {
            log.warn("Cannot send friend request to self: userId={}", fromUserId);
            return false;
        }

        if (isFriend(fromUserId, toUserId)) {
            log.warn("Already friends: fromUserId={}, toUserId={}", fromUserId, toUserId);
            return false;
        }

        if (hasPendingRequest(fromUserId, toUserId)) {
            log.warn("Friend request already pending: fromUserId={}, toUserId={}", fromUserId, toUserId);
            return false;
        }

        // 若对方已有反向pending请求，也不允许发送（避免双向请求互相覆盖）
        if (hasPendingRequest(toUserId, fromUserId)) {
            log.warn("Target user already sent a request to sender: fromUserId={}, toUserId={}", fromUserId, toUserId);
            return false;
        }

        // 只删除同方向的旧请求记录（不删除反向的，避免误删对方的pending请求）
        String deleteOldRequestSql = "DELETE FROM t_friend_request WHERE from_user_id = ? AND to_user_id = ?";
        String insertSql = "INSERT INTO t_friend_request (from_user_id, to_user_id, status, message) VALUES (?, ?, 'PENDING', ?)";
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement deletePs = conn.prepareStatement(deleteOldRequestSql)) {
                deletePs.setLong(1, fromUserId);
                deletePs.setLong(2, toUserId);
                deletePs.executeUpdate();
            }
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setLong(1, fromUserId);
                insertPs.setLong(2, toUserId);
                insertPs.setString(3, message);
                insertPs.executeUpdate();
            }
            conn.commit();
            log.info("Friend request sent: fromUserId={}, toUserId={}", fromUserId, toUserId);
            return true;
        } catch (SQLException e) {
            log.error("Send friend request error: fromUserId={}, toUserId={}", fromUserId, toUserId, e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { log.error("Close connection failed", e); }
            }
        }
    }

    public boolean acceptFriendRequest(long requestId, long toUserId) {
        String checkSql = "SELECT from_user_id FROM t_friend_request WHERE request_id = ? AND to_user_id = ? AND status = 'PENDING'";
        Long fromUserId = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, requestId);
            ps.setLong(2, toUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    fromUserId = rs.getLong("from_user_id");
                }
            }
        } catch (SQLException e) {
            log.error("Check friend request error: requestId={}", requestId, e);
            return false;
        }

        if (fromUserId == null) {
            log.warn("Friend request not found or not pending: requestId={}", requestId);
            return false;
        }

        String acceptSql = "UPDATE t_friend_request SET status = 'ACCEPTED', respond_time = NOW() WHERE request_id = ?";
        String insertFriend1 = "INSERT INTO t_friend (user_id, friend_user_id, relation_type) VALUES (?, ?, 'FRIEND')";
        String insertFriend2 = "INSERT INTO t_friend (user_id, friend_user_id, relation_type) VALUES (?, ?, 'FRIEND')";

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(acceptSql)) {
                ps1.setLong(1, requestId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = conn.prepareStatement(insertFriend1)) {
                ps2.setLong(1, fromUserId);
                ps2.setLong(2, toUserId);
                ps2.executeUpdate();
            }

            try (PreparedStatement ps3 = conn.prepareStatement(insertFriend2)) {
                ps3.setLong(1, toUserId);
                ps3.setLong(2, fromUserId);
                ps3.executeUpdate();
            }

            conn.commit();
            log.info("Friend request accepted: fromUserId={}, toUserId={}", fromUserId, toUserId);
            return true;
        } catch (SQLException e) {
            log.error("Accept friend request error: requestId={}", requestId, e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { log.error("Close connection failed", e); }
            }
        }
    }

    public boolean rejectFriendRequest(long requestId, long toUserId) {
        String sql = "UPDATE t_friend_request SET status = 'REJECTED', respond_time = NOW() WHERE request_id = ? AND to_user_id = ? AND status = 'PENDING'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ps.setLong(2, toUserId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.info("Friend request rejected: requestId={}", requestId);
                return true;
            } else {
                log.warn("Friend request not found or not pending: requestId={}", requestId);
                return false;
            }
        } catch (SQLException e) {
            log.error("Reject friend request error: requestId={}", requestId, e);
            return false;
        }
    }

    public boolean deleteFriend(long userId, long friendUserId) {
        String deleteFriendSql = "DELETE FROM t_friend WHERE (user_id = ? AND friend_user_id = ?) OR (user_id = ? AND friend_user_id = ?)";
        String deleteRequestSql = "DELETE FROM t_friend_request WHERE (from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?)";
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            int friendDeleted;
            try (PreparedStatement deleteFriendPs = conn.prepareStatement(deleteFriendSql)) {
                deleteFriendPs.setLong(1, userId);
                deleteFriendPs.setLong(2, friendUserId);
                deleteFriendPs.setLong(3, friendUserId);
                deleteFriendPs.setLong(4, userId);
                friendDeleted = deleteFriendPs.executeUpdate();
            }

            try (PreparedStatement deleteRequestPs = conn.prepareStatement(deleteRequestSql)) {
                deleteRequestPs.setLong(1, userId);
                deleteRequestPs.setLong(2, friendUserId);
                deleteRequestPs.setLong(3, friendUserId);
                deleteRequestPs.setLong(4, userId);
                deleteRequestPs.executeUpdate();
            }

            conn.commit();
            if (friendDeleted > 0) {
                log.info("Friend deleted: userId={}, friendUserId={}, rows={}", userId, friendUserId, friendDeleted);
                return true;
            } else {
                log.warn("Friend relationship not found: userId={}, friendUserId={}", userId, friendUserId);
                return false;
            }
        } catch (SQLException e) {
            log.error("Delete friend error: userId={}, friendUserId={}", userId, friendUserId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    log.error("Rollback failed", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.error("Close connection failed", e);
                }
            }
        }
    }

    public List<FriendInfo> getFriendList(long userId) {
        String sql = "SELECT f.friend_id, f.user_id, f.friend_user_id, u.username, u.score, f.relation_type, f.remark " +
                     "FROM t_friend f JOIN t_user u ON f.friend_user_id = u.user_id " +
                     "WHERE f.user_id = ? AND f.relation_type = 'FRIEND' " +
                     "ORDER BY u.score DESC";

        List<FriendInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long friendUserId = rs.getLong("friend_user_id");
                    boolean online = false;
                    if (RedisManager.getInstance().isConnected()) {
                        online = RedisManager.getInstance().isOnline(friendUserId);
                    } else {
                        online = com.gomatch.server.session.SessionManager.getInstance().getSessionByUserId(friendUserId) != null;
                    }
                    result.add(new FriendInfo(
                            rs.getLong("friend_id"),
                            rs.getLong("user_id"),
                            friendUserId,
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getString("relation_type"),
                            rs.getString("remark"),
                            online
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get friend list error: userId={}", userId, e);
        }
        return result;
    }

    public List<FriendRequestInfo> getPendingRequests(long toUserId) {
        String sql = "SELECT r.request_id, r.from_user_id, u.username, u.score, r.to_user_id, r.status, r.message, r.create_time " +
                     "FROM t_friend_request r JOIN t_user u ON r.from_user_id = u.user_id " +
                     "WHERE r.to_user_id = ? AND r.status = 'PENDING' " +
                     "ORDER BY r.create_time DESC";

        List<FriendRequestInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, toUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new FriendRequestInfo(
                            rs.getLong("request_id"),
                            rs.getLong("from_user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getLong("to_user_id"),
                            rs.getString("status"),
                            rs.getString("message"),
                            rs.getString("create_time")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get pending requests error: toUserId={}", toUserId, e);
        }
        return result;
    }

    public boolean isFriend(long userId, long friendUserId) {
        String sql = "SELECT 1 FROM t_friend WHERE user_id = ? AND friend_user_id = ? AND relation_type = 'FRIEND'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, friendUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Check friend error: userId={}, friendUserId={}", userId, friendUserId, e);
        }
        return false;
    }

    public boolean hasPendingRequest(long fromUserId, long toUserId) {
        String sql = "SELECT 1 FROM t_friend_request WHERE from_user_id = ? AND to_user_id = ? AND status = 'PENDING'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fromUserId);
            ps.setLong(2, toUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Check pending request error: fromUserId={}, toUserId={}", fromUserId, toUserId, e);
        }
        return false;
    }
}