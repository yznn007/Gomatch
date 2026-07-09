package com.gomatch.client.audio;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

public final class BackgroundMusicManager {
    private static final String LOBBY_THEME_PATH = "/audio/lobby-theme.mp3";

    private static final BooleanProperty globalMusicEnabled = new SimpleBooleanProperty(true);
    private static final BooleanProperty backgroundMusicEnabled = new SimpleBooleanProperty(true);
    private static final DoubleProperty globalVolume = new SimpleDoubleProperty(80);
    private static final DoubleProperty backgroundVolume = new SimpleDoubleProperty(70);

    private static MediaPlayer player;
    private static boolean unavailable;

    private BackgroundMusicManager() {
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

        URL resource = BackgroundMusicManager.class.getResource(LOBBY_THEME_PATH);
        if (resource == null) {
            unavailable = true;
            return;
        }

        Media media = new Media(resource.toExternalForm());
        player = new MediaPlayer(media);
        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.setOnEndOfMedia(() -> player.seek(javafx.util.Duration.ZERO));
        player.setOnError(() -> {
            unavailable = true;
            disposePlayer();
        });

        globalMusicEnabled.addListener((obs, oldVal, newVal) -> updatePlaybackState());
        backgroundMusicEnabled.addListener((obs, oldVal, newVal) -> updatePlaybackState());
        globalVolume.addListener((obs, oldVal, newVal) -> updateVolume());
        backgroundVolume.addListener((obs, oldVal, newVal) -> updateVolume());
        updateVolume();
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
