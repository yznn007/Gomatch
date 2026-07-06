package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SettingsController {
    @FXML private Label serverLabel;
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
        serverLabel.setText(AppConfig.SERVER_HOST + ":" + AppConfig.SERVER_PORT);
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
}
