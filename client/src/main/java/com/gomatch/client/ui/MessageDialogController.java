package com.gomatch.client.ui;

import com.gomatch.client.I18N;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MessageDialogController {
    public static String pendingTitle = "";
    public static String pendingMessage = "";

    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Button okBtn;

    @FXML
    private void initialize() {
        titleLabel.setText(pendingTitle.isEmpty() ? I18N.get("message.title") : pendingTitle);
        messageLabel.setText(pendingMessage);
    }

    @FXML
    private void handleOk() {
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }
}
