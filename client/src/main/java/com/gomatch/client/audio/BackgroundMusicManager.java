package com.gomatch.client.audio;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class BackgroundMusicManager {
    private static final Map<String, String> AVAILABLE_SONGS = new LinkedHashMap<>();

    static {
        AVAILABLE_SONGS.put("/audio/main_1.mp3", "music.223am");
        AVAILABLE_SONGS.put("/audio/main_2.mp3", "music.song2");
        AVAILABLE_SONGS.put("/audio/main_3.mp3", "music.allweknow");
        AVAILABLE_SONGS.put("/audio/main_4.mp3", "music.normalnomore");
        AVAILABLE_SONGS.put("/audio/main_5.mp3", "music.song5");
        AVAILABLE_SONGS.put("/audio/main_6.mp3", "music.song6");
        AVAILABLE_SONGS.put("/audio/main_7.mp3", "music.song7");
        AVAILABLE_SONGS.put("/audio/main_8.mp3", "music.song8");
        AVAILABLE_SONGS.put("/audio/main_9.mp3", "music.song9");
        AVAILABLE_SONGS.put("/audio/main_10.mp3", "music.song10");
    }

    private static String currentSongPath = "/audio/main_1.mp3";
    private static final ObjectProperty<PlayOrder> currentPlayOrder = new SimpleObjectProperty<>(PlayOrder.SINGLE_LOOP);
    private static final Random random = new Random();

    private static final StringProperty currentSongName = new SimpleStringProperty(AVAILABLE_SONGS.getOrDefault(currentSongPath, ""));

    private static final BooleanProperty globalMusicEnabled = new SimpleBooleanProperty(true);
    private static final BooleanProperty backgroundMusicEnabled = new SimpleBooleanProperty(true);
    private static final DoubleProperty globalVolume = new SimpleDoubleProperty(45);
    private static final DoubleProperty backgroundVolume = new SimpleDoubleProperty(35);

    private static MediaPlayer player;
    private static boolean unavailable;
    private static boolean listenersAttached;

    private BackgroundMusicManager() {
    }

    public static Map<String, String> getAvailableSongs() {
        return Collections.unmodifiableMap(AVAILABLE_SONGS);
    }

    public static String getCurrentSongName() {
        return currentSongName.get();
    }

    public static StringProperty currentSongNameProperty() {
        return currentSongName;
    }

    public static String getCurrentSongPath() {
        return currentSongPath;
    }

    public static PlayOrder getCurrentPlayOrder() {
        return currentPlayOrder.get();
    }

    public static ObjectProperty<PlayOrder> currentPlayOrderProperty() {
        return currentPlayOrder;
    }

    public static void setPlayOrder(PlayOrder order) {
        if (order == null || order == currentPlayOrder.get()) {
            return;
        }
        currentPlayOrder.set(order);
        if (player != null) {
            applyPlayOrderToPlayer(player);
        }
        com.gomatch.client.UserSettings.get().playOrder = order.name();
        com.gomatch.client.UserSettings.save();
    }

    public static void selectAndPlaySong(String resourcePath) {
        if (!AVAILABLE_SONGS.containsKey(resourcePath)) {
            return;
        }
        com.gomatch.client.UserSettings.get().selectedSongPath = resourcePath;
        com.gomatch.client.UserSettings.save();
        switchSong(resourcePath);
    }

    public static void applySettings() {
        if (javafx.application.Platform.isFxApplicationThread()) {
            doApplySettings();
        } else {
            javafx.application.Platform.runLater(BackgroundMusicManager::doApplySettings);
        }
    }

    private static void doApplySettings() {
        com.gomatch.client.UserSettings settings = com.gomatch.client.UserSettings.get();
        if (settings.playOrder != null) {
            try {
                PlayOrder order = PlayOrder.valueOf(settings.playOrder);
                if (order != currentPlayOrder.get()) {
                    currentPlayOrder.set(order);
                    if (player != null) {
                        applyPlayOrderToPlayer(player);
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        String songPath = settings.selectedSongPath;
        if (songPath != null && AVAILABLE_SONGS.containsKey(songPath)) {
            if (!songPath.equals(currentSongPath) || player == null) {
                currentSongPath = songPath;
                currentSongName.set(AVAILABLE_SONGS.getOrDefault(songPath, ""));
                unavailable = false;
                disposePlayer();
                playLobbyTheme();
            } else {
                if (player != null) {
                    applyPlayOrderToPlayer(player);
                }
            }
        }
    }

    public static void switchSong(String resourcePath) {
        if (!AVAILABLE_SONGS.containsKey(resourcePath)) {
            return;
        }
        if (resourcePath.equals(currentSongPath)) {
            return;
        }
        currentSongPath = resourcePath;
        currentSongName.set(AVAILABLE_SONGS.getOrDefault(resourcePath, ""));
        unavailable = false;
        disposePlayer();
        playLobbyTheme();
    }

    public static void playLobbyTheme() {
        if (unavailable) {
            return;
        }
        try {
            ensurePlayer();
            if (player != null && isPlayable()) {
                player.play();
            }
        } catch (MediaException | IllegalStateException ex) {
            unavailable = true;
            disposePlayer();
        }
    }

    public static void stop() {
        disposePlayer();
    }

    public static BooleanProperty globalMusicEnabledProperty() {
        return globalMusicEnabled;
    }

    public static BooleanProperty backgroundMusicEnabledProperty() {
        return backgroundMusicEnabled;
    }

    public static DoubleProperty globalVolumeProperty() {
        return globalVolume;
    }

    public static DoubleProperty backgroundVolumeProperty() {
        return backgroundVolume;
    }

    private static void ensurePlayer() {
        if (player != null) {
            return;
        }

        URL resource = BackgroundMusicManager.class.getResource(currentSongPath);
        if (resource == null) {
            unavailable = true;
            return;
        }

        Media media = new Media(resource.toExternalForm());
        player = new MediaPlayer(media);
        applyPlayOrderToPlayer(player);
        player.setOnError(() -> {
            unavailable = true;
            disposePlayer();
        });

        if (!listenersAttached) {
            listenersAttached = true;
            globalMusicEnabled.addListener((obs, oldVal, newVal) -> updatePlaybackState());
            backgroundMusicEnabled.addListener((obs, oldVal, newVal) -> updatePlaybackState());
            globalVolume.addListener((obs, oldVal, newVal) -> updateVolume());
            backgroundVolume.addListener((obs, oldVal, newVal) -> updateVolume());
        }
        updateVolume();
    }

    private static void applyPlayOrderToPlayer(MediaPlayer p) {
        switch (currentPlayOrder.get()) {
            case SINGLE_LOOP:
                p.setCycleCount(MediaPlayer.INDEFINITE);
                p.setOnEndOfMedia(() -> p.seek(javafx.util.Duration.ZERO));
                break;
            case SEQUENTIAL:
                p.setCycleCount(1);
                p.setOnEndOfMedia(BackgroundMusicManager::playNextSong);
                break;
            case RANDOM:
                p.setCycleCount(1);
                p.setOnEndOfMedia(BackgroundMusicManager::playRandomSong);
                break;
        }
    }

    private static void playNextSong() {
        List<String> paths = new ArrayList<>(AVAILABLE_SONGS.keySet());
        if (paths.size() <= 1) {
            return;
        }
        int idx = paths.indexOf(currentSongPath);
        int next = (idx + 1) % paths.size();
        switchSong(paths.get(next));
    }

    private static void playRandomSong() {
        List<String> paths = new ArrayList<>(AVAILABLE_SONGS.keySet());
        if (paths.size() <= 1) {
            return;
        }
        String next;
        do {
            next = paths.get(random.nextInt(paths.size()));
        } while (next.equals(currentSongPath));
        switchSong(next);
    }

    private static void updatePlaybackState() {
        if (player == null) {
            return;
        }

        updateVolume();
        if (isPlayable()) {
            player.play();
        } else {
            player.pause();
        }
    }

    private static void updateVolume() {
        if (player == null) {
            return;
        }

        double volume = (clamp(globalVolume.get()) / 100.0) * (clamp(backgroundVolume.get()) / 100.0);
        player.setVolume(isPlayable() ? volume : 0);
    }

    private static boolean isPlayable() {
        return globalMusicEnabled.get() && backgroundMusicEnabled.get();
    }

    private static double clamp(double value) {
        return Math.min(Math.max(value, 0), 100);
    }

    private static void disposePlayer() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }
}
