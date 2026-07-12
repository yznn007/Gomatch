package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
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
    @FXML private Button backButton;

    public void setResult(String result, String reason) {
        setResult(result, reason, 0);
    }

    public void setResult(String result, String reason, int scoreChange) {
        if (backButton != null) {
            backButton.setText(I18N.get("gameover.back"));
        }
        if (replayButton != null) {
            replayButton.setText(I18N.get("gameover.replay"));
        }
        switch (result) {
            case "0" -> resultLabel.setText(I18N.get("gameover.blackWin"));
            case "1" -> resultLabel.setText(I18N.get("gameover.whiteWin"));
            case "2" -> resultLabel.setText(I18N.get("gameover.draw"));
            case "3" -> resultLabel.setText(I18N.get("gameover.disconnectWin"));
            default -> resultLabel.setText(result);
        }
        if (scoreChangeLabel != null) {
            String scoreLabel = I18N.get("gameover.score");
            if (scoreChange > 0) {
                scoreChangeLabel.setText("+" + scoreChange + " " + scoreLabel);
                scoreChangeLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#15803d", "#86efac") + "; -fx-font-weight: bold; -fx-font-size: 16px;");
            } else if (scoreChange < 0) {
                scoreChangeLabel.setText(scoreChange + " " + scoreLabel);
                scoreChangeLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + "; -fx-font-weight: bold; -fx-font-size: 16px;");
            } else {
                scoreChangeLabel.setText("+0 " + scoreLabel);
                scoreChangeLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-weight: bold; -fx-font-size: 16px;");
            }
        }
        if (!reason.isEmpty() && reasonLabel != null) {
            String localizedReason = I18N.get("gameover." + reason);
            if (localizedReason.startsWith("!") && localizedReason.endsWith("!")) {
                reasonLabel.setText(reason);
            } else {
                reasonLabel.setText(localizedReason);
            }
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
