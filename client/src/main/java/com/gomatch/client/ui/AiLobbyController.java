package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AiLobbyController implements NetworkListener {
    @FXML private Label matchStatus;

    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        ClientMain.getConnector().addListener(this);
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleStartGame() {
    }

    @FXML
    private void handleEasy() {
        startGame("EASY");
    }

    @FXML
    private void handleMedium() {
        startGame("MEDIUM");
    }

    @FXML
    private void handleHard() {
        startGame("HARD");
    }

    private void startGame(String difficulty) {
        matchStatus.setText("正在创建 AI 对战...");
        
        if (previewMode) {
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            ctrl.setGameInfo(currentUser.getUsername(), "AI", currentUser.getUsername());
            ctrl.setPreviewMode(true);
            return;
        }
        
        ClientMain.getConnector().send("AI_MATCH", difficulty);
    }

    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.JOIN_OK.equals(command)) {
        } else if (Protocol.GAME_START.equals(command)) {
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            String uname = currentUser != null ? currentUser.getUsername() : "player1";
            if (args.length >= 4) {
                ctrl.setGameInfo(args[0], args[1], args[2], args[3], uname);
            } else {
                ctrl.setGameInfo(args[0], args[1], uname);
            }
        } else if (Protocol.ERROR.equals(command)) {
            matchStatus.setText(args[0]);
        }
    }
}