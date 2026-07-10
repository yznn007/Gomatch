package com.gomatch.server.security;

import java.util.regex.Pattern;

/** Normalizes user-provided international phone numbers to E.164 format. */
public final class PhoneNumberNormalizer {
    private static final Pattern E164 = Pattern.compile("\\+?[1-9]\\d{7,14}");

    private PhoneNumberNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String compact = value.trim().replaceAll("[\\s()\\-]", "");
        if (!E164.matcher(compact).matches()) {
            return null;
        }
        return compact.startsWith("+") ? compact : "+" + compact;
    }
}
