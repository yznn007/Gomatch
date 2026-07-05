package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class GameOverController {
    @FXML private Label resultLabel;
    @FXML private Label reasonLabel;

    public void setResult(String result, String reason) {
        switch (result) {
            case "0" -> resultLabel.setText("黑方胜!");
            case "1" -> resultLabel.setText("白方胜!");
            case "2" -> resultLabel.setText("平局!");
            case "3" -> resultLabel.setText("对方断线，你赢了!");
            default -> resultLabel.setText("对局结束");
        }
        if (!reason.isEmpty()) {
            reasonLabel.setText(reason);
        }
    }

    @FXML
    private void handleBack() {
        closeDialog();
        // 修复：从全局 AppConfig 中获取当前用户与预览状态回传大厅，防止状态丢失
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(AppConfig.getCurrentUser());
        ctrl.setPreviewMode(AppConfig.isPreviewMode());
    }

    @FXML
    private void handleReplay() {
        closeDialog();
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(AppConfig.getCurrentUser());
        ctrl.setPreviewMode(AppConfig.isPreviewMode());
    }

    private void closeDialog() {
        Stage stage = (Stage) resultLabel.getScene().getWindow();
        stage.close();
    }
}
