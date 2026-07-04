package com.gomatch.client;

import com.gomatch.client.network.ServerConnector;
import com.gomatch.client.ui.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientMain extends Application {
    private static ServerConnector connector;

    @Override
    public void start(Stage primaryStage) {
        connector = new ServerConnector();
        ScreenManager.init(primaryStage);
        ScreenManager.switchTo("/fxml/LoginUI.fxml");
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.disconnect();
        }
    }

    public static ServerConnector getConnector() {
        return connector;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
