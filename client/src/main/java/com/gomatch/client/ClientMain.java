package com.gomatch.client;

import com.gomatch.client.network.ServerConnector;
import com.gomatch.client.ui.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientMain extends Application {
    private static ServerConnector connector;

    @Override
    public void start(Stage primaryStage) {
        // 动态加载并注册内置字体文件，确保客户端自带该字体，无需在系统级安装即可直接使用
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/MapleMono-NF-CN-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/MapleMono-NF-CN-Bold.ttf"), 14);

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
