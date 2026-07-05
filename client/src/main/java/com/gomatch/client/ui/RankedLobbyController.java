package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.RoomInfo;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class RankedLobbyController implements NetworkListener {
    @FXML private Label rankLabel;
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
        matchStatus.setText("正在匹配对手...");
        if (previewMode) {
            RoomInfo fakeRoom = new RoomInfo("7777", "排位对局", "对手", 1, "等待中");
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setPreviewMode(true);
            ctrl.setRoomInfo(fakeRoom, currentUser);
            return;
        }
        ClientMain.getConnector().send(Protocol.RANKED_MATCH);
    }

    @FXML
    private void handleBack() {
        // 修复：返回大厅时将 currentUser 传递给新的 LobbyController 实例
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.JOIN_OK.equals(command)) {
            RoomInfo room = AppConfig.GSON.fromJson(args[0], RoomInfo.class);
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setPreviewMode(false);
            ctrl.setRoomInfo(room, currentUser);
        } else if (Protocol.GAME_START.equals(command)) {
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            ctrl.setGameInfo(args[0], args[1], currentUser.getUsername());
        }
    }
}