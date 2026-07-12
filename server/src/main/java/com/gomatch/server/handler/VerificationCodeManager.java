package com.gomatch.server.handler;

import com.gomatch.server.redis.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 忘记密码验证码管理器。
 * 使用 Redis 存储验证码，60 秒自动过期。
 * Key 格式: {@code vcode:forgot:<email>}，TTL 60s。
 */
public class VerificationCodeManager {
    private static final Logger log = LoggerFactory.getLogger(VerificationCodeManager.class);
    private static final VerificationCodeManager INSTANCE = new VerificationCodeManager();

    /** Redis Key 前缀 */
    private static final String KEY_PREFIX = "vcode:forgot:";

    /** 验证码有效期（秒） */
    private static final long CODE_TTL_SECONDS = 120;

    /** 记录已通过验证码校验的邮箱，用于重置密码步骤的安全校验 */
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    private VerificationCodeManager() {}

    public static VerificationCodeManager getInstance() {
        return INSTANCE;
    }

    /**
     * 为指定邮箱生成 6 位数字验证码，存入 Redis 并返回。
     * 如果已存在未过期的验证码则覆盖。
     */
    public String generateCode(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        String key = KEY_PREFIX + email.trim().toLowerCase();

        RedisManager redis = RedisManager.getInstance();
        if (!redis.isConnected()) {
            log.error("Redis not connected, cannot generate verification code");
            return null;
        }

        try {
            redis.setex(key, CODE_TTL_SECONDS, code);
            log.info("Verification code generated for {} (Redis TTL={}s)", email, CODE_TTL_SECONDS);
        } catch (Exception e) {
            log.error("Failed to store verification code in Redis: {}", e.getMessage());
            return null;
        }
        return code;
    }

    /**
     * 验证邮箱对应的验证码是否正确且未过期。
     * 验证成功后立即删除 Redis 中的验证码（"验证即焚"）。
     *
     * @return true 表示验证成功
     */
    public boolean verifyCode(String email, String code) {
        String key = KEY_PREFIX + email.trim().toLowerCase();

        RedisManager redis = RedisManager.getInstance();
        if (!redis.isConnected()) {
            log.warn("Redis not connected, cannot verify code");
            return false;
        }

        try {
            String storedCode = redis.get(key);
            if (storedCode == null) {
                log.warn("Verification failed for {}: no code exists or expired", email);
                return false;
            }
            if (!storedCode.equals(code.trim())) {
                log.warn("Verification failed for {}: wrong code", email);
                return false;
            }
            // 验证成功后删除 Redis 中的验证码，并标记该邮箱已验证
            redis.del(key);
            verifiedEmails.put(email, true);
            log.info("Verification succeeded for {}", email);
            return true;
        } catch (Exception e) {
            log.error("Failed to verify code in Redis: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查验证码是否仍然有效（不删除）
     */
    public boolean isCodeValid(String email) {
        String key = KEY_PREFIX + email.trim().toLowerCase();

        RedisManager redis = RedisManager.getInstance();
        if (!redis.isConnected()) return false;

        try {
            return redis.exists(key);
        } catch (Exception e) {
            log.error("Failed to check code validity in Redis: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查邮箱是否已通过验证码校验。
     */
    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(verifiedEmails.getOrDefault(email, false));
    }

    /**
     * 密码重置成功后清除验证标记，防止同一验证多次重置。
     */
    public void clearVerified(String email) {
        verifiedEmails.remove(email);
    }
}
