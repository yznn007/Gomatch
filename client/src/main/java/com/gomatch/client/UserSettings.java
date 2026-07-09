package com.gomatch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

/**
 * 用户偏好设置持久化。
 * 存储路径：~/.gomatch/user-settings.json
 */
public final class UserSettings {

    private UserSettings() {}

    private static final File FILE = new File(
            new File(System.getProperty("user.home"), ".gomatch"), "user-settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

    // ---- UI 风格 ----
    public String theme = "清透白磨砂 (经典)";
    public boolean dynamicBackground = true;
    public String fontFamily = "等宽 Maple Mono (推荐)";

    // ---- 语言 ----
    public String language = "简体中文 (Chinese)";

    // ---- 单例 ----
    private static UserSettings instance = new UserSettings();

    public static UserSettings get() {
        return instance;
    }

    /** 从磁盘加载，文件不存在则使用默认值 */
    public static void load() {
        if (FILE.exists()) {
            try (Reader r = new FileReader(FILE)) {
                instance = GSON.fromJson(r, UserSettings.class);
                if (instance == null) instance = new UserSettings();
            } catch (IOException ignored) {}
        }
    }

    /** 保存到磁盘 */
    public static void save() {
        FILE.getParentFile().mkdirs();
        try (Writer w = new FileWriter(FILE)) {
            GSON.toJson(instance, w);
        } catch (IOException ignored) {}
    }
}
