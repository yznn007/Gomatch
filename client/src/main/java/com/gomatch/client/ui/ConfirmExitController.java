package com.gomatch.client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class ConfirmExitController {
    @FXML private Button yesBtn;
    @FXML private Button noBtn;

    private boolean confirmed = false;

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void handleYes() {
        confirmed = true;
        closeStage();
    }

    @FXML
    private void handleNo() {
        confirmed = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) yesBtn.getScene().getWindow();
        stage.close();
    }
}
