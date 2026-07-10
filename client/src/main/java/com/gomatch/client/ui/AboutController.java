package com.gomatch.client.ui;

import javafx.fxml.FXML;

public class AboutController {
    @FXML
    private void handleClose() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }
}
