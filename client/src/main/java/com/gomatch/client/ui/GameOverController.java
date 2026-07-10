package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class GameOverController {
    @FXML private Label resultLabel;
    @FXML private Label reasonLabel;
    @FXML private Label scoreChangeLabel;
    @FXML private Button replayButton;

    public void setResult(String result, String reason) {
        setResult(result, reason, 0);
    }

    public void setResult(String result, String reason, int scoreChange) {
        switch (result) {
            case "0" -> resultLabel.setText("黑方胜!");
            case "1" -> resultLabel.setText("白方胜!");
            case "2" -> resultLabel.setText("平局!");
            case "3" -> resultLabel.setText("对方断线，你赢了!");
            default -> resultLabel.setText(result);
        }
        if (scoreChangeLabel != null) {
            if (scoreChange > 0) {
                scoreChangeLabel.setText("+" + scoreChange + " 积分");
                scoreChangeLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#15803d", "#86efac") + "; -fx-font-weight: bold; -fx-font-size: 16px;");
            } else if (scoreChange < 0) {
                scoreChangeLabel.setText(scoreChange + " 积分");
                scoreChangeLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + "; -fx-font-weight: bold; -fx-font-size: 16px;");
            } else {
                scoreChangeLabel.setText("+0 积分");
                scoreChangeLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-weight: bold; -fx-font-size: 16px;");
            }
        }
        if (!reason.isEmpty() && reasonLabel != null) {
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
        if (AppConfig.isAiMode()) {
            // AI模式：发送AI_MATCH重新开始，BoardController仍在背后等待响应
            ClientMain.getConnector().send(Protocol.AI_MATCH);
        } else {
            // PVP模式：发送再来一局请求给对手
            ClientMain.getConnector().send(Protocol.REQUEST_REMATCH);
        }
    }

    /**
     * 断线/强制结束等场景隐藏"再来一局"按钮，仅保留"返回大厅"。
     */
    public void hideReplayButton() {
        if (replayButton != null) {
            replayButton.setVisible(false);
            replayButton.setManaged(false);
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) resultLabel.getScene().getWindow();
        stage.close();
    }
}
