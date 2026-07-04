package com.gomatch.client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class RecordController {
    @FXML private TableView<String[]> recordTable;
    @FXML private TableColumn<String[], String> opponentCol;
    @FXML private TableColumn<String[], String> resultCol;
    @FXML private TableColumn<String[], String> timeCol;
    @FXML private TableColumn<String[], String> replayCol;

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }
}
