package com.gomatch.client;

/**
 * 用户偏好设置模型。
 * 由 {@link ConfigManager} 持久化到 ~/.gomatch/config.json 并同步至云端。
 */
public final class UserSettings {

    private UserSettings() {}

    // ---- 基础设置 ----
    public boolean autoStart = false;
    public boolean rememberPassword = true;

    // ---- 图像设置 ----
    public String resolution = "900x640";
    public boolean fullscreen = false;
    public boolean vsync = true;
    public String fpsLimit = "60";

    // ---- 声音设置 ----
    public double volume = 80;
    public boolean bgMusic = true;
    public boolean soundEffect = true;
    public boolean lobbyMusic = true;
    public boolean muteOnBackground = false;
    public String selectedSongPath = "/audio/main_1.mp3";
    public String playOrder = "SINGLE_LOOP";

    // ---- UI 风格 ----
    public String theme = "清透白磨砂 (经典)";
    public boolean dynamicBackground = true;
    public String fontFamily = "等宽 Maple Mono (推荐)";

    // ---- 语言 ----
    public String language = "简体中文 (Chinese)";

    private static UserSettings instance = new UserSettings();

    public static UserSettings get() {
        return instance;
    }

    public static void load() {
        ConfigManager.getInstance().loadLocal();
    }

    /** 保存到磁盘并同步云端。 */
    public static void save() {
        ConfigManager.getInstance().saveAndSync();
    }

    static void replace(UserSettings settings) {
        instance = settings == null ? new UserSettings() : settings;
    }
}
