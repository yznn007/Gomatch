package com.gomatch.client;

import com.gomatch.client.model.User;
import com.google.gson.Gson;

/**
 * 全局应用配置 & 共享工具单例。
 * 服务器地址改这里，无需到处改代码。
 */
public final class AppConfig {

    private AppConfig() {}

    // ---------- 服务器连接配置 ----------
    public static String SERVER_HOST = com.gomatch.client.network.ClientConfig.getHost();
    public static int    SERVER_PORT = com.gomatch.client.network.ClientConfig.getPort();

    // ---------- 共享 Gson 单例（线程安全）----------
    public static final Gson GSON = new Gson();

    // ---------- 全局会话状态 (Session) ----------
    private static User currentUser;
    private static boolean previewMode;
    private static boolean aiMode;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isPreviewMode() {
        return previewMode;
    }

    public static void setPreviewMode(boolean preview) {
        previewMode = preview;
    }

    public static boolean isAiMode() {
        return aiMode;
    }

    public static void setAiMode(boolean ai) {
        aiMode = ai;
    }
}
