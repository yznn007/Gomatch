package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RankedLobbyController implements NetworkListener {
    @FXML private Label rankLabel;
    @FXML private Label matchStatus;

    private User currentUser;
    private boolean previewMode;
    private boolean isMatching = false;

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        int score = user.getScore();
        String tier;
        if (score <= 0)        tier = "未定级";
        else if (score < 1100) tier = "青铜 " + score;
        else if (score < 1300) tier = "白银 " + score;
        else if (score < 1500) tier = "黄金 " + score;
        else if (score < 1800) tier = "铂金 " + score;
        else if (score < 2100) tier = "钻石 " + score;
        else                   tier = "王者 " + score;
        rankLabel.setText("当前段位: " + tier);
    }

    @FXML
    private void handleMatch() {
        if (isMatching) return;
        isMatching = true;
        matchStatus.setText("正在寻找对手...");
        matchStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px;");
        ClientMain.getConnector().addListener(this);
        ClientMain.getConnector().send(Protocol.RANKED_MATCH);
    }

    @FXML
    private void handleBack() {
        if (isMatching) {
            ClientMain.getConnector().removeListener(this);
            isMatching = false;
        }
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if ("RANKED_MATCH_WAIT".equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText(msg.getString("msg"));
                matchStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px;");
            });
        } else if (Protocol.JOIN_OK.equals(cmd)) {
            isMatching = false;
            ClientMain.getConnector().removeListener(this);
        } else if (Protocol.GAME_START.equals(cmd)) {
            isMatching = false;
            ClientMain.getConnector().removeListener(this);
            Platform.runLater(() -> {
                BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                ctrl.setGameInfo(msg.getString("blackUserId"), msg.getString("whiteUserId"),
                        currentUser != null ? currentUser.getUsername() : "");
            });
        } else if (Protocol.ERROR.equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText("匹配失败: " + msg.getString("msg"));
                matchStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px;");
                isMatching = false;
            });
            ClientMain.getConnector().removeListener(this);
        }
    }
}