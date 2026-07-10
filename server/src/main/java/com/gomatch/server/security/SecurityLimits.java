package com.gomatch.server.security;

/** Central protocol and business-input size policy. */
public final class SecurityLimits {
    public static final int MAX_MESSAGE_CHARS = 3_000_000;
    public static final int MAX_CHAT_CHARS = 500;
    public static final int MAX_FRIEND_MESSAGE_CHARS = 500;
    public static final int MAX_ROOM_NAME_CHARS = 100;
    public static final int MAX_ROOM_PASSWORD_CHARS = 100;
    public static final int MAX_PASSWORD_CHARS = 100;
    public static final int MAX_SAVE_NAME_CHARS = 100;
    public static final int MAX_SEARCH_KEYWORD_CHARS = 50;
    public static final int MAX_DIFFICULTY_CHARS = 16;
    public static final int MAX_AVATAR_BASE64_CHARS = 2_796_400;
    public static final int MAX_PAGE_SIZE = 100;

    private SecurityLimits() {}

    public static int page(int value) {
        return Math.max(0, value);
    }

    public static int pageSize(int value) {
        return Math.max(1, Math.min(MAX_PAGE_SIZE, value));
    }

    public static boolean validLength(String value, int min, int max) {
        return value != null && value.length() >= min && value.length() <= max;
    }
}
