package com.gomatch.client;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 国际化工具类。通过 I18N.get("key") 获取当前语言的文本。
 * 语言由 UserSettings.language 控制。
 */
public final class I18N {

    private static final String BASE_NAME = "messages";
    private static Locale currentLocale = Locale.SIMPLIFIED_CHINESE;
    private static ResourceBundle bundle;

    private I18N() {}

    public static void init(String langCode) {
        if (langCode != null && langCode.contains("English")) {
            currentLocale = Locale.ENGLISH;
        } else {
            currentLocale = Locale.SIMPLIFIED_CHINESE;
        }
        reloadBundle();
    }

    /** 重新加载资源包（切换语言后调用） */
    public static void reloadBundle() {
        try {
            bundle = ResourceBundle.getBundle(BASE_NAME, currentLocale);
        } catch (Exception e) {
            // fallback to Chinese
            bundle = ResourceBundle.getBundle(BASE_NAME, Locale.SIMPLIFIED_CHINESE);
        }
    }

    /** 获取当前 Resource Bundle（供 FXMLLoader 使用） */
    public static ResourceBundle getBundle() {
        return bundle;
    }

    /** 获取翻译文本 */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return '!' + key + '!';
        }
    }

    /** 获取翻译文本并格式化（支持 %s / %d 等占位符） */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return String.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    public static boolean isEnglish() {
        return currentLocale.equals(Locale.ENGLISH);
    }
}
