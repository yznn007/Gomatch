package com.gomatch.client;

import com.gomatch.client.network.ServerConnector;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.client.ui.ScreenManager;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ClientMain extends Application {
    private static ServerConnector connector;
    private static boolean disconnectHandled = false;

    @Override
    public void start(Stage primaryStage) {
        // 动态加载并注册内置字体文件，确保客户端自带该字体，无需在系统级安装即可直接使用
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/MapleMono-NF-CN-Regular.ttf"), 14);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/MapleMono-NF-CN-Bold.ttf"), 14);

        connector = new ServerConnector();
        // 全局断线监听器
        connector.addListener(new NetworkListener() {
            @Override
            public void onMessage(Message msg) {
                if (Protocol.DISCONNECT.equals(msg.getCmd()) && !disconnectHandled) {
                    disconnectHandled = true;
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "与服务器断开连接，请重新登录");
                        alert.showAndWait();
                        ScreenManager.switchTo("/fxml/LoginUI.fxml");
                        disconnectHandled = false;
                    });
                }
            }
        });
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
