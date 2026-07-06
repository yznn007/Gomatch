package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.ClientConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class SettingsController {
    @FXML private ComboBox<String> ipComboBox;
    @FXML private TextField portField;
    @FXML private Label statusLabel;
    @FXML private CheckBox soundCheckBox;
    @FXML private Button exitBtn;
    @FXML private Button logoutBtn;

    // 左侧导航菜单按钮与右侧面板绑定的 FXML 变量
    @FXML private Button menuBaseBtn;
    @FXML private Button menuGraphicsBtn;
    @FXML private Button menuAudioBtn;
    @FXML private Button menuStyleBtn;
    @FXML private Button menuServerBtn;
    @FXML private Button menuExitBtn;

    @FXML private VBox basePane;
    @FXML private VBox graphicsPane;
    @FXML private VBox audioPane;
    @FXML private VBox stylePane;
    @FXML private VBox serverPane;
    @FXML private VBox exitPane;

    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        // 绑定IP和端口默认值
        String currentHost = AppConfig.SERVER_HOST;
        if (currentHost == null || currentHost.trim().isEmpty()) {
            currentHost = "127.0.0.1";
        }
        ipComboBox.setValue(currentHost);
        portField.setText(String.valueOf(AppConfig.SERVER_PORT));

        if (ClientMain.getConnector().isConnected()) {
            statusLabel.setText("已连接");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("未连接 (网络离线)");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
        // 初始激活基础设置标签页
        showBaseTab();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            statusLabel.setText("已连接 (浏览模式)");
            statusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    // 标签页动态切换逻辑
    private void switchTab(VBox activePane, Button activeBtn) {
        basePane.setVisible(false);
        basePane.setManaged(false);
        graphicsPane.setVisible(false);
        graphicsPane.setManaged(false);
        audioPane.setVisible(false);
        audioPane.setManaged(false);
        stylePane.setVisible(false);
        stylePane.setManaged(false);
        serverPane.setVisible(false);
        serverPane.setManaged(false);
        exitPane.setVisible(false);
        exitPane.setManaged(false);

        activePane.setVisible(true);
        activePane.setManaged(true);

        menuBaseBtn.getStyleClass().remove("active");
        menuGraphicsBtn.getStyleClass().remove("active");
        menuAudioBtn.getStyleClass().remove("active");
        menuStyleBtn.getStyleClass().remove("active");
        menuServerBtn.getStyleClass().remove("active");
        menuExitBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    @FXML
    private void showBaseTab() {
        switchTab(basePane, menuBaseBtn);
    }

    @FXML
    private void showGraphicsTab() {
        switchTab(graphicsPane, menuGraphicsBtn);
    }

    @FXML
    private void showAudioTab() {
        switchTab(audioPane, menuAudioBtn);
    }

    @FXML
    private void showStyleTab() {
        switchTab(stylePane, menuStyleBtn);
    }

    @FXML
    private void showServerTab() {
        switchTab(serverPane, menuServerBtn);
    }

    @FXML
    private void showExitTab() {
        switchTab(exitPane, menuExitBtn);
    }

    @FXML
    private void handleLogout() {
        ClientMain.getConnector().disconnect();
        AppConfig.setCurrentUser(null);
        ScreenManager.switchTo("/fxml/LoginUI.fxml");
    }

    @FXML
    private void handleExitGame() {
        // 使用项目自定义UI风格的二级确认对话框
        ConfirmExitController ctrl = ScreenManager.showDialog("/fxml/ConfirmExitUI.fxml");
        if (ctrl != null && ctrl.isConfirmed()) {
            ClientMain.getConnector().disconnect();
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleSaveConfig() {
        String host = ipComboBox.getValue();
        if (host == null || host.trim().isEmpty()) {
            showAlert("错误", "服务器 IP 地址不能为空！");
            return;
        }
        host = host.trim();
        
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showAlert("错误", "端口号必须是 1 到 65535 之间的有效整数！");
            return;
        }
        
        // 1. 保存到内存 AppConfig 中
        AppConfig.SERVER_HOST = host;
        AppConfig.SERVER_PORT = port;
        
        // 2. 保存到本地 properties 配置文件中以持久化
        ClientConfig.saveConfig(host, port);
        
        // 3. 断开当前的老连接
        ClientMain.getConnector().disconnect();
        
        // 4. 立即尝试连接到新 IP
        try {
            ClientMain.getConnector().connect(host, port);
            
            // 5. 连接成功，刷新界面状态
            statusLabel.setText("已连接 (" + host + ":" + port + ")");
            statusLabel.setStyle("-fx-text-fill: green;");
            showAlert("配置已应用", "已成功保存服务器配置！\n\n网络检测结果：\n连接【" + host + ":" + port + "】成功！✅\n客户端已切换至新服务器环境。");
        } catch (Exception e) {
            // 6. 连接失败，更新状态
            statusLabel.setText("未连接 (连接 " + host + ":" + port + " 失败)");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            showAlert("配置已保存", "已成功保存服务器配置！\n\n网络检测结果：\n连接【" + host + ":" + port + "】失败！❌\n\n请确保目标服务端已启动。\n错误详情：" + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        MessageDialogController.pendingTitle = title;
        MessageDialogController.pendingMessage = msg;
        ScreenManager.showDialog("/fxml/MessageDialogUI.fxml");
    }
}
