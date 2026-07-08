package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LobbyController implements NetworkListener {
    @FXML private Label userInfoLabel;
    @FXML private Label userScoreLabel;
    @FXML private Label avatarLabel;
    @FXML private VBox rankDrawer;
    @FXML private ListView<String> rankDrawerList;

    private final ObservableList<String> drawerRanks = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        rankDrawerList.setItems(drawerRanks);
        ClientMain.getConnector().addListener(this);
        if (!ClientMain.getConnector().isConnected()) {
            drawerRanks.addAll("1. 棋圣  1500", "2. 高手如云  1350", "3. 五子连珠  1200");
        } else {
            ClientMain.getConnector().send(Protocol.RANKING);
        }
        // 将抽屉高度绑定到父容器，保证浮层铺满纵向
        rankDrawer.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent instanceof javafx.scene.layout.Region parent) {
                rankDrawer.prefHeightProperty().bind(parent.heightProperty());
            }
        });
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) {
            return;
        }
        String name = user.getUsername();
        userInfoLabel.setText(name);
        if (user.getScore() > 0) {
            userScoreLabel.setText(user.getScore() + " 分");
        } else {
            userScoreLabel.setText("未定级");
        }
        avatarLabel.setText(name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());
    }

    @FXML
    private void toggleRankDrawer() {
        boolean open = !rankDrawer.isVisible();
        rankDrawer.setManaged(open);
        rankDrawer.setVisible(open);
    }

    @FXML
    private void handleProfile() {
        ProfileController ctrl = ScreenManager.switchTo("/fxml/ProfileUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleCasual() {
        CasualLobbyController ctrl = ScreenManager.switchTo("/fxml/CasualLobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleRanked() {
        RankedLobbyController ctrl = ScreenManager.switchTo("/fxml/RankedLobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleAiMatch() {
        AiLobbyController ctrl = ScreenManager.switchTo("/fxml/AiLobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleFriends() {
        FriendsController ctrl = ScreenManager.switchTo("/fxml/FriendsUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleRankingPage() {
        RankingController ctrl = ScreenManager.switchTo("/fxml/RankingUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleManagement() {
        ManagementController ctrl = ScreenManager.switchTo("/fxml/ManagementUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }
    @FXML
    private void handleSettings() {
        SettingsController ctrl = ScreenManager.switchTo("/fxml/SettingsUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleAbout() {
        ScreenManager.switchTo("/fxml/AboutUI.fxml");
    }

    @Override
    public void onMessage(Message msg) {
        if (Protocol.RANKING_LIST.equals(msg.getCmd())) {
            // 服务端返回的 JSON 数组
            org.json.JSONArray users = msg.getData().optJSONArray("users");
            if (users != null) {
                drawerRanks.clear();
                for (int i = 0; i < users.length(); i++) {
                    org.json.JSONObject u = users.optJSONObject(i);
                    if (u == null) continue;
                    int rank = u.optInt("rank", i + 1);
                    String username = u.optString("username", "");
                    int score = u.optInt("score", 0);
                    drawerRanks.add(rank + ". " + username + "  " + score);
                }
            }
        } else if (Protocol.USER_SCORE_UPDATE.equals(msg.getCmd())) {
            if (currentUser != null) {
                currentUser.setScore(msg.getInt("score"));
                currentUser.setWinCount(msg.getInt("winCount"));
                currentUser.setLoseCount(msg.getInt("loseCount"));
                currentUser.setDrawCount(msg.getInt("drawCount"));
                currentUser.setRank(msg.getInt("rank"));
                if (currentUser.getScore() > 0) {
                    userScoreLabel.setText(currentUser.getScore() + " 分");
                } else {
                    userScoreLabel.setText("未定级");
                }
            }
        }
    }
}
