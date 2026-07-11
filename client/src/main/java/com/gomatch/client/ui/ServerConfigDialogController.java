package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.network.ClientConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ServerConfigDialogController {

    @FXML private ComboBox<String> ipComboBox;
    @FXML private TextField portField;
    @FXML private Label statusLabel;
    @FXML private Button applyBtn;

    @FXML
    private void initialize() {
        String host = AppConfig.SERVER_HOST;
        if (ClientConfig.getDefaultHost().equals(host)) {
            ipComboBox.getEditor().clear();
        } else {
            ipComboBox.setValue(host);
        }
        portField.setText(String.valueOf(AppConfig.SERVER_PORT));
        refreshStatus();
    }

    private void refreshStatus() {
        if (ClientMain.getConnector().isConnected()) {
            statusLabel.setText("已连接");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("未连接");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleApply() {
        String enteredHost = ipComboBox.getEditor().getText();
        String host = enteredHost == null || enteredHost.trim().isEmpty()
                ? ClientConfig.getDefaultHost()
                : enteredHost.trim();

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            statusLabel.setText("错误：端口须为 1-65535 的整数");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        // 保存配置
        AppConfig.SERVER_HOST = host;
        AppConfig.SERVER_PORT = port;
        ClientConfig.saveConfig(enteredHost, port);

        // 置为"连接中"，禁用按钮防止重复点击
        statusLabel.setText("连接中...");
        statusLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");
        applyBtn.setDisable(true);

        final String finalHost = host;
        final int finalPort = port;

        // 后台线程执行阻塞的网络操作，结果只更新状态标签，不嵌套打开新弹窗
        Thread t = new Thread(() -> {
            ClientMain.getConnector().disconnect();
            try {
                ClientMain.getConnector().connect(finalHost, finalPort);
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("✅ 已连接");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    applyBtn.setDisable(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("❌ 连接失败：" + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #dc2626;");
                    applyBtn.setDisable(false);
                    // 失败时保留弹窗，让用户看到错误信息
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

}
