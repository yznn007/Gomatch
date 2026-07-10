package com.gomatch.server.security;

import com.gomatch.common.Protocol;
import com.gomatch.server.room.Room;
import com.gomatch.server.room.RoomManager;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;

/** Validates and rate-limits raw protocol requests before ClientHandler dispatch. */
public final class ProtocolRequestGuard {
    private static final int MAX_STANDARD_MESSAGE_CHARS = 65_536;

    private final FixedWindowRateLimiter<String> commandLimiter;
    private final FixedWindowRateLimiter<String> loginLimiter;
    private final FixedWindowRateLimiter<String> registerLimiter;

    public ProtocolRequestGuard() {
        this(new FixedWindowRateLimiter<>(120, 60_000),
                new FixedWindowRateLimiter<>(10, 60_000),
                new FixedWindowRateLimiter<>(5, 3_600_000));
    }

    private ProtocolRequestGuard(FixedWindowRateLimiter<String> commandLimiter,
                                 FixedWindowRateLimiter<String> loginLimiter,
                                 FixedWindowRateLimiter<String> registerLimiter) {
        this.commandLimiter = commandLimiter;
        this.loginLimiter = loginLimiter;
        this.registerLimiter = registerLimiter;
    }

    static ProtocolRequestGuard forTesting(int commands, int logins, int registrations) {
        return new ProtocolRequestGuard(
                new FixedWindowRateLimiter<>(commands, 60_000),
                new FixedWindowRateLimiter<>(logins, 60_000),
                new FixedWindowRateLimiter<>(registrations, 60_000));
    }

    public String validateAndNormalize(String line, InetAddress address) throws ProtocolRejectedException {
        Objects.requireNonNull(line, "line");
        String ip = address == null ? "unknown" : address.getHostAddress();
        if (!commandLimiter.tryAcquire(ip)) {
            throw rejected("request rate exceeded");
        }

        JSONObject root;
        try {
            root = new JSONObject(line);
        } catch (RuntimeException invalidJson) {
            if (line.length() > MAX_STANDARD_MESSAGE_CHARS) {
                throw rejected("invalid request too large");
            }
            return line;
        }

        String cmd = root.optString("cmd", "");
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            data = new JSONObject();
            root.put("data", data);
        }

        if (!Protocol.UPLOAD_AVATAR.equals(cmd) && line.length() > MAX_STANDARD_MESSAGE_CHARS) {
            throw rejected("request too large");
        }

        if (Protocol.LOGIN.equals(cmd)) {
            String account = data.optString("account", "").trim();
            requireLength(account, 1, 100, "account");
            requireLength(data.optString("password", ""), 1, SecurityLimits.MAX_PASSWORD_CHARS, "password");
            if (!loginLimiter.tryAcquire(ip + '|' + account)) {
                throw rejected("login rate exceeded");
            }
        } else if (Protocol.REGISTER.equals(cmd)) {
            requireLength(data.optString("username", "").trim(), 1, 16, "username");
            requireLength(data.optString("password", ""), 1, SecurityLimits.MAX_PASSWORD_CHARS, "password");
            requireLength(data.optString("phone", "").trim(), 8, 32, "phone");
            if (!registerLimiter.tryAcquire(ip)) {
                throw rejected("registration rate exceeded");
            }
        }

        switch (cmd) {
            case Protocol.CHANGE_PWD -> {
                requireLength(data.optString("oldPassword", ""), 1, SecurityLimits.MAX_PASSWORD_CHARS, "oldPassword");
                requireLength(data.optString("newPassword", ""), 1, SecurityLimits.MAX_PASSWORD_CHARS, "newPassword");
            }
            case Protocol.CREATE_ROOM -> {
                requireLength(data.optString("roomName", "").trim(), 1, SecurityLimits.MAX_ROOM_NAME_CHARS, "roomName");
                requireOptionalLength(data.optString("password", ""), SecurityLimits.MAX_ROOM_PASSWORD_CHARS, "password");
            }
            case Protocol.JOIN_ROOM ->
                    requireOptionalLength(data.optString("password", ""), SecurityLimits.MAX_ROOM_PASSWORD_CHARS, "password");
            case Protocol.SPECTATE -> validateSpectatePassword(data);
            case Protocol.CHAT -> requireLength(data.optString("message", ""), 1, SecurityLimits.MAX_CHAT_CHARS, "message");
            case Protocol.ADD_FRIEND ->
                    requireOptionalLength(data.optString("message", ""), SecurityLimits.MAX_FRIEND_MESSAGE_CHARS, "message");
            case Protocol.SAVE_GAME ->
                    requireOptionalLength(data.optString("saveName", ""), SecurityLimits.MAX_SAVE_NAME_CHARS, "saveName");
            case Protocol.USER_LIST ->
                    requireOptionalLength(data.optString("keyword", ""), SecurityLimits.MAX_SEARCH_KEYWORD_CHARS, "keyword");
            case Protocol.AI_MATCH, Protocol.CREATE_AI_ROOM ->
                    requireOptionalLength(data.optString("difficulty", ""), SecurityLimits.MAX_DIFFICULTY_CHARS, "difficulty");
            case Protocol.UPLOAD_AVATAR ->
                    requireLength(data.optString("data", ""), 1, SecurityLimits.MAX_AVATAR_BASE64_CHARS, "avatar");
            default -> { }
        }

        if (data.has("page")) {
            data.put("page", SecurityLimits.page(data.optInt("page", 0)));
        }
        if (data.has("size")) {
            data.put("size", SecurityLimits.pageSize(data.optInt("size", 10)));
        }
        return root.toString();
    }

    private void validateSpectatePassword(JSONObject data) throws ProtocolRejectedException {
        String password = data.optString("password", "");
        requireOptionalLength(password, SecurityLimits.MAX_ROOM_PASSWORD_CHARS, "password");
        Room room = RoomManager.getInstance().getRoom(data.optString("roomId", ""));
        if (room != null && room.hasPassword() && !room.checkPassword(password)) {
            throw rejected("protected room credentials required");
        }
    }

    private void requireLength(String value, int min, int max, String field) throws ProtocolRejectedException {
        if (!SecurityLimits.validLength(value, min, max)) {
            throw rejected(field + " length invalid");
        }
    }

    private void requireOptionalLength(String value, int max, String field) throws ProtocolRejectedException {
        if (value != null && value.length() > max) {
            throw rejected(field + " length invalid");
        }
    }

    private ProtocolRejectedException rejected(String reason) {
        return new ProtocolRejectedException(reason);
    }

    public static final class ProtocolRejectedException extends IOException {
        public ProtocolRejectedException(String reason) {
            super(reason);
        }
    }
}
