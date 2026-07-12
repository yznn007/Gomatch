package com.gomatch.server.dao;

import com.gomatch.server.security.PhoneNumberNormalizer;
import org.mindrot.jbcrypt.BCrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDAO {
    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);
    private final RankDAO rankDAO = new RankDAO();

    static final String LOGIN_BY_ACCOUNT_SQL = "SELECT user_id, username, password, score, win_count, lose_count, draw_count, " +
            "role, status, phone FROM t_user WHERE (user_id = ? OR username = ? OR email = ? OR phone = ?)";

    public static class UserInfo {
        private final long userId;
        private final String username;
        private final int score;
        private final int winCount;
        private final int loseCount;
        private final int drawCount;
        private final String role;
        private final String status;
        private final String phone;

        public UserInfo(long userId, String username, int score,
                        int winCount, int loseCount, int drawCount) {
            this(userId, username, score, winCount, loseCount, drawCount, "PLAYER", "ACTIVE", null);
        }

        public UserInfo(long userId, String username, int score,
                        int winCount, int loseCount, int drawCount,
                        String role, String status) {
            this(userId, username, score, winCount, loseCount, drawCount, role, status, null);
        }

        public UserInfo(long userId, String username, int score,
                        int winCount, int loseCount, int drawCount,
                        String role, String status, String phone) {
            this.userId = userId;
            this.username = username;
            this.score = score;
            this.winCount = winCount;
            this.loseCount = loseCount;
            this.drawCount = drawCount;
            this.role = role;
            this.status = status;
            this.phone = phone;
        }

        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public int getScore() { return score; }
        public int getWinCount() { return winCount; }
        public int getLoseCount() { return loseCount; }
        public int getDrawCount() { return drawCount; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public String getPhone() { return phone; }
    }

    /**
     * @deprecated 使用 {@link #register(String, String, String)} 代替，邮箱为必填项
     */
    @Deprecated
    public boolean register(String username, String password) {
        return register(username, password, "", "");
    }

    public boolean register(String username, String password, String email) {
        return register(username, password, email, "");
    }

    public boolean register(String username, String password, String email, String phone) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("Register failed: empty username");
            return false;
        }
        if (password == null || password.length() < 6) {
            log.warn("Register failed: password too short");
            return false;
        }
        if (username.length() > 16) {
            log.warn("Register failed: username too long");
            return false;
        }
        if (existsByUsername(username.trim())) {
            log.warn("Register failed: username already exists: {}", username);
            return false;
        }
        // 邮箱不能为空
        String emailTrimmed = (email != null) ? email.trim() : "";
        if (emailTrimmed.isEmpty()) {
            log.warn("Register failed: empty email");
            return false;
        }
        // 检查邮箱是否已注册
        if (emailExists(emailTrimmed)) {
            log.warn("Register failed: email already registered: {}", emailTrimmed);
            return false;
        }
        String normalizedPhone = null;
        if (phone != null && !phone.trim().isEmpty()) {
            normalizedPhone = PhoneNumberNormalizer.normalize(phone);
            if (normalizedPhone == null) {
                log.warn("Register failed: invalid phone number");
                return false;
            }
            if (phoneExists(normalizedPhone)) {
                log.warn("Register failed: phone already registered: {}", normalizedPhone);
                return false;
            }
        }

        String sql = "INSERT INTO t_user (username, password, email, phone) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim());
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            ps.setString(3, emailTrimmed);
            ps.setString(4, normalizedPhone);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long userId = rs.getLong(1);
                    rankDAO.upsertRank(userId, 1000, 0, 0, 0);
                }
            }
            log.info("User registered: {} (email={}, phone={})", username, emailTrimmed, normalizedPhone);
            return true;
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            log.warn("Register failed: duplicate username, email, or phone (concurrent): {}", username);
            return false;
        } catch (SQLException e) {
            log.error("Register SQL error: {}", username, e);
            return false;
        }
    }

    public UserInfo login(String account, String password) {
        return login(account, password, null);
    }

    public UserInfo login(String account, String password, String phone) {
        if (account == null || password == null) {
            return null;
        }
        String accountValue = account.trim();
        long accountId = parseAccountId(accountValue);
        String normalizedEmail = accountValue.toLowerCase(Locale.ROOT);
        String normalizedPhone = PhoneNumberNormalizer.normalize(phone);
        if (normalizedPhone == null) {
            normalizedPhone = PhoneNumberNormalizer.normalize(accountValue);
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(LOGIN_BY_ACCOUNT_SQL)) {
            if (accountId == -1) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, accountId);
            }
            ps.setString(2, accountValue);
            ps.setString(3, normalizedEmail);
            ps.setString(4, normalizedPhone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long userId = rs.getLong("user_id");
                    String storedPassword = rs.getString("password");
                    boolean passwordValid;

                    passwordValid = matchesPassword(password, storedPassword);
                    if (!isBcryptHash(storedPassword)) {
                        if (passwordValid) {
                            upgradePasswordToHash(userId, password);
                            log.info("Password upgraded to bcrypt for userId={}", userId);
                        }
                    }

                    if (!passwordValid) {
                        log.warn("Login failed: password mismatch for account: {}", account);
                        return null;
                    }
                    String status = rs.getString("status");
                    if ("BANNED".equals(status)) {
                        log.warn("Banned user attempt to login: {}", account);
                        return new UserInfo(userId, rs.getString("username"), rs.getInt("score"), rs.getInt("win_count"), rs.getInt("lose_count"), rs.getInt("draw_count"), rs.getString("role"), status, rs.getString("phone"));
                    }
                    return new UserInfo(
                            userId,
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count"),
                            rs.getString("role"),
                            rs.getString("status"),
                            rs.getString("phone")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Login SQL error: {}", account, e);
        }
        return null;
    }

    private static boolean isBcryptHash(String password) {
        return password != null && password.startsWith("$2a$");
    }

    static boolean matchesPassword(String candidate, String storedPassword) {
        if (candidate == null || storedPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return BCrypt.checkpw(candidate, storedPassword);
        }
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }

    private void upgradePasswordToHash(long userId, String plainPassword) {
        String sql = "UPDATE t_user SET password = ?, update_time = NOW() WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to upgrade password for userId={}", userId, e);
        }
    }

    private long parseAccountId(String account) {
        if (!account.matches("[1-9]\\d{0,18}")) {
            return -1;
        }
        try {
            return Long.parseLong(account);
        } catch (NumberFormatException tooLarge) {
            return -1;
        }
    }

    public boolean phoneExists(String phone) {
        String normalizedPhone = PhoneNumberNormalizer.normalize(phone);
        if (normalizedPhone == null) {
            return false;
        }
        String sql = "SELECT 1 FROM t_user WHERE phone = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedPhone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Check phone exists error: {}", normalizedPhone, e);
        }
        return false;
    }
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM t_user WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // 数据库查询失败时保守处理：视作用户名已存在，拒绝注册
            log.error("Check username error, treating as duplicate: {}", username, e);
        }
        return true;
    }

    public UserInfo getUserById(long userId) {
        String sql = "SELECT user_id, username, score, win_count, lose_count, draw_count " +
                     "FROM t_user WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserInfo(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Get user by id error: {}", userId, e);
        }
        return null;
    }

    public boolean existsActiveUser(long userId) {
        String sql = "SELECT 1 FROM t_user WHERE user_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Check active user error: {}", userId, e);
        }
        return false;
    }

    public boolean updateScore(long userId, int scoreDelta) {
        String sql = "UPDATE t_user SET score = score + ? WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scoreDelta);
            ps.setLong(2, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                rankDAO.updateScore(userId, scoreDelta);
            }
            return success;
        } catch (SQLException e) {
            log.error("Update score error: userId={}", userId, e);
            return false;
        }
    }

    public void updateWinCount(long userId) {
        String sql = "UPDATE t_user SET win_count = win_count + 1, update_time = NOW() WHERE user_id = ?";
        executeUpdate(sql, userId);
    }

    public void updateLoseCount(long userId) {
        String sql = "UPDATE t_user SET lose_count = lose_count + 1, update_time = NOW() WHERE user_id = ?";
        executeUpdate(sql, userId);
    }

    public void updateDrawCount(long userId) {
        String sql = "UPDATE t_user SET draw_count = draw_count + 1, update_time = NOW() WHERE user_id = ?";
        executeUpdate(sql, userId);
    }

    /**
     * 查询邮箱是否存在
     */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM t_user WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Check email exists error: {}", email, e);
        }
        return false;
    }

    /**
     * 根据邮箱查询用户信息（用于忘记密码验证）
     * @return userId，如果不存在返回 0
     */
    public long findUserIdByEmail(String email) {
        String sql = "SELECT user_id FROM t_user WHERE email = ? AND status = 'ACTIVE'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("user_id");
                }
            }
        } catch (SQLException e) {
            log.error("Find user by email error: {}", email, e);
        }
        return 0;
    }

    /**
     * 忘记密码：直接重置密码（清空旧密码，设置新密码）
     */
    public boolean resetPassword(long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            log.warn("Reset password failed: new password too short");
            return false;
        }

        String sql = "UPDATE t_user SET password = ?, update_time = NOW() WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            ps.setLong(2, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                log.info("Password reset successfully: userId={}", userId);
            }
            return success;
        } catch (SQLException e) {
            log.error("Reset password error: userId={}", userId, e);
            return false;
        }
    }

    public boolean changePassword(long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            log.warn("Change password failed: new password too short");
            return false;
        }

        String checkSql = "SELECT password FROM t_user WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.warn("Change password failed: user not found, userId={}", userId);
                    return false;
                }
                String storedPassword = rs.getString("password");
                if (!matchesPassword(oldPassword, storedPassword)) {
                    log.warn("Change password failed: old password mismatch, userId={}", userId);
                    return false;
                }
                if (matchesPassword(newPassword, storedPassword)) {
                    log.warn("Change password failed: new password same as old, userId={}", userId);
                    return false;
                }
            }
        } catch (SQLException e) {
            log.error("Check password error: userId={}", userId, e);
            return false;
        }

        String updateSql = "UPDATE t_user SET password = ?, update_time = NOW() WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            ps.setLong(2, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                log.info("Password changed successfully: userId={}", userId);
            }
            return success;
        } catch (SQLException e) {
            log.error("Change password error: userId={}", userId, e);
            return false;
        }
    }

    private void executeUpdate(String sql, long userId) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Execute update error: userId={}", userId, e);
        }
    }

    public List<UserInfo> getRanking(int limit) {
        String sql = "SELECT user_id, username, score, win_count, lose_count, draw_count " +
                     "FROM t_user WHERE status = 'ACTIVE' " +
                     "ORDER BY score DESC LIMIT ?";
        List<UserInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new UserInfo(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get ranking error", e);
        }
        return result;
    }

    // ==================== 管理员方法 ====================

    /**
     * 封禁用户
     */
    public boolean banUser(long userId) {
        String sql = "UPDATE t_user SET status = 'BANNED', update_time = NOW() WHERE user_id = ? AND role != 'ADMIN'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) log.info("User banned: userId={}", userId);
            return success;
        } catch (SQLException e) {
            log.error("Ban user error: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 解封用户
     */
    public boolean unbanUser(long userId) {
        String sql = "UPDATE t_user SET status = 'ACTIVE', update_time = NOW() WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) log.info("User unbanned: userId={}", userId);
            return success;
        } catch (SQLException e) {
            log.error("Unban user error: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 修改用户角色
     */
    public boolean updateUserRole(long userId, String newRole) {
        if (!"PLAYER".equals(newRole) && !"ADMIN".equals(newRole)) {
            log.warn("Invalid role: {}", newRole);
            return false;
        }
        String sql = "UPDATE t_user SET role = ?, update_time = NOW() WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setLong(2, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) log.info("User role updated: userId={}, role={}", userId, newRole);
            return success;
        } catch (SQLException e) {
            log.error("Update user role error: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 获取用户头像URL
     */
    public String getAvatar(long userId) {
        String sql = "SELECT avatar FROM t_user WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String url = rs.getString("avatar");
                    return url != null ? url : "";
                }
            }
        } catch (SQLException e) {
            log.error("Get avatar error: userId={}", userId, e);
        }
        return "";
    }

    /**
     * 更新用户头像URL
     */
    public boolean updateAvatar(long userId, String avatar) {
        String sql = "UPDATE t_user SET avatar = ?, update_time = NOW() WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, avatar);
            ps.setLong(2, userId);
            boolean success = ps.executeUpdate() > 0;
            if (success) log.info("Avatar updated: userId={}", userId);
            return success;
        } catch (SQLException e) {
            log.error("Update avatar error: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 获取所有用户列表（分页，含 role 和 status）
     */
    public List<UserInfo> getAllUsers(int page, int size) {
        String sql = "SELECT user_id, username, score, win_count, lose_count, draw_count, role, status " +
                     "FROM t_user ORDER BY user_id ASC LIMIT ? OFFSET ?";
        List<UserInfo> result = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, page * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new UserInfo(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count"),
                            rs.getString("role"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Get all users error", e);
        }
        return result;
    }

    /**
     * 搜索用户
     */
    public List<UserInfo> searchUsers(String keyword, int page, int size) {
        List<UserInfo> result = new ArrayList<>();
        if (keyword == null) {
            return result;
        }
        String sql = "SELECT user_id, username, score, win_count, lose_count, draw_count, role, status " +
                     "FROM t_user WHERE username LIKE ? ORDER BY user_id ASC LIMIT ? OFFSET ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setInt(2, size);
            ps.setInt(3, page * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new UserInfo(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getInt("win_count"),
                            rs.getInt("lose_count"),
                            rs.getInt("draw_count"),
                            rs.getString("role"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Search users error: keyword={}", keyword, e);
        }
        return result;
    }

    /**
     * 获取用户总数
     */
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM t_user";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Get user count error", e);
        }
        return 0;
    }
}