package com.gomatch.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ScreenManager {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 640;
    private static final String STYLE_PATH = "/css/style.css";
    private static Stage stage;
    private static final Map<String, Object> controllers = new HashMap<>();
    private static boolean firstShow = true;

    public static void init(Stage s) {
        stage = s;
        stage.setTitle("Gomatch");
        stage.setMinWidth(720);
        stage.setMinHeight(540);
        stage.setResizable(true);
    }

    public static <T> T switchTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ScreenManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();
            controllers.put(fxmlPath, controller);

            Scene oldScene = stage.getScene();
            double w = oldScene == null ? WIDTH : oldScene.getWidth();
            double h = oldScene == null ? HEIGHT : oldScene.getHeight();
            Scene scene = new Scene(root, w, h);
            applyStylesheet(scene);
            stage.setScene(scene);

            if (firstShow) {
                stage.centerOnScreen();
                firstShow = false;
            }
            stage.show();
            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    public static <T> T showDialog(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(ScreenManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();
            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(root);
            applyStylesheet(scene);
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.setTitle("Gomatch");
            dialog.showAndWait();
            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dialog FXML: " + fxmlPath, e);
        }
    }

    private static void applyStylesheet(Scene scene) {
        String css = ScreenManager.class.getResource(STYLE_PATH).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getController(String fxmlPath) {
        return (T) controllers.get(fxmlPath);
    }

    public static Stage getStage() {
        return stage;
    }
}