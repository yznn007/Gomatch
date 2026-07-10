package com.gomatch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Maintains the local config mirror and performs authenticated cloud sync. */
public final class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();
    private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"), ".gomatch", "config.json");
    private static final Path LEGACY_FILE = Path.of(System.getProperty("user.home"), ".gomatch", "user-settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() { }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /** Called before the login screen is shown; missing files retain defaults. */
    public synchronized void loadLocal() {
        Path source = Files.exists(CONFIG_FILE) ? CONFIG_FILE : LEGACY_FILE;
        if (!Files.exists(source)) {
            return;
        }
        try {
            UserSettings.replace(GSON.fromJson(Files.readString(source, StandardCharsets.UTF_8), UserSettings.class));
        } catch (Exception ignored) {
            UserSettings.replace(null);
        }
    }

    /** Writes the current configuration locally, then sends it when authenticated. */
    public synchronized void saveAndSync() {
        saveLocal();
        if (ClientMain.getConnector() != null && ClientMain.getConnector().isConnected()
                && AppConfig.getCurrentUser() != null) {
            ClientMain.getConnector().send(Message.of(Protocol.REQ_UPDATE_CONFIG,
                    "configJson", GSON.toJson(UserSettings.get())));
        }
    }

    /** Handles the cloud response before it is delivered to individual controllers. */
    public synchronized void handleServerMessage(Message message) {
        if (!Protocol.RESP_GET_CONFIG.equals(message.getCmd())) {
            return;
        }
        String configJson = message.getString("configJson");
        if (configJson == null || configJson.isBlank()) {
            return;
        }
        try {
            JsonObject object = JsonParser.parseString(configJson).getAsJsonObject();
            UserSettings.replace(GSON.fromJson(object, UserSettings.class));
            saveLocal();
        } catch (Exception ignored) {
            // Invalid cloud data must never destroy a usable local configuration.
        }
    }

    private void saveLocal() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(UserSettings.get()), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}