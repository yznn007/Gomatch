package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
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
        matchStatus.setText(I18N.get("ai.creatingGame"));
        
        if (previewMode) {
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            ctrl.setGameInfo(currentUser.getUsername(), "AI", currentUser.getUsername());
            ctrl.setPreviewMode(true);
            return;
        }
        
        ClientMain.getConnector().send(Message.of(Protocol.AI_MATCH, "difficulty", difficulty));
    }

    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.JOIN_OK.equals(cmd)) {
        } else if (Protocol.GAME_START.equals(cmd)) {
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            String bId = String.valueOf(msg.getLong("blackUserId"));
            String wId = String.valueOf(msg.getLong("whiteUserId"));
            String bName = msg.has("blackUsername") ? msg.getString("blackUsername") : bId;
            String wName = msg.has("whiteUsername") ? msg.getString("whiteUsername") : wId;
            int bScore = msg.has("blackScore") ? msg.getInt("blackScore") : 0;
            int wScore = msg.has("whiteScore") ? msg.getInt("whiteScore") : 0;
            String username = currentUser != null ? currentUser.getUsername() : "";
            ctrl.setGameInfo(bId, bName, wId, wName, username, bScore, wScore);
            String bAvatar = msg.has("blackAvatarUrl") ? msg.getString("blackAvatarUrl") : "";
            String wAvatar = msg.has("whiteAvatarUrl") ? msg.getString("whiteAvatarUrl") : "";
            ctrl.setPlayerAvatars(bAvatar, wAvatar);
        } else if (Protocol.ERROR.equals(cmd)) {
            matchStatus.setText(msg.getString("msg"));
        }
    }
}
