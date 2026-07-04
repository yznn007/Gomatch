package com.gomatch.client.ui;

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
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @FXML
    private void handleReplay() {
        closeDialog();
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    private void closeDialog() {
        Stage stage = (Stage) resultLabel.getScene().getWindow();
        stage.close();
    }
}
