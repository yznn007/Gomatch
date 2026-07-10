package com.gomatch.client.ui;

import java.util.regex.Pattern;

final class PhoneInputValidator {
    private static final Pattern E164 = Pattern.compile("\\+?[1-9]\\d{7,14}");
    private static final Pattern LOCAL_NUMBER = Pattern.compile("\\d{4,14}");
    private PhoneInputValidator() { }

    static String toE164(String countryCode, String localNumber) {
        if (countryCode == null || localNumber == null) return null;
        String local = localNumber.trim().replaceAll("[\\s()\\-]", "");
        if (!LOCAL_NUMBER.matcher(local).matches()) return null;
        String value = countryCode.replaceAll("[^+0-9]", "") + local;
        return E164.matcher(value).matches() ? value : null;
    }

    static boolean isValid(String value) {
        return value != null && E164.matcher(value.trim().replaceAll("[\\s()\\-]", "")).matches();
    }
}