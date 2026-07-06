package com.gomatch.client.ui;

import com.gomatch.client.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RankedLobbyController {
    @FXML private Label rankLabel;
    @FXML private Label matchStatus;

    private User currentUser;
    private boolean previewMode;

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
        // 从段位界面进入匹配大厅（房间大厅）
        CasualLobbyController ctrl = ScreenManager.switchTo("/fxml/CasualLobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleBack() {
        // 返回大厅时将 currentUser 传递给新的 LobbyController 实例
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }
}