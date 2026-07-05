package com.gomatch.database.util;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,32}$"
    );

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    public static boolean isValidId(Long id) {
        return id != null && id > 0;
    }

    public static boolean isValidScore(Integer score) {
        return score != null && score >= 0;
    }

    public static boolean isValidStatus(String status) {
        return status != null && (status.equals("ACTIVE") || status.equals("INACTIVE") || 
                status.equals("BANNED") || status.equals("connected") || status.equals("disconnected"));
    }

    public static boolean isValidRole(String role) {
        return role != null && (role.equals("PLAYER") || role.equals("ADMIN"));
    }
}