package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
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

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.util.List;

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
    private void handleSettings() {
        SettingsController ctrl = ScreenManager.switchTo("/fxml/SettingsUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleAbout() {
        ScreenManager.switchTo("/fxml/AboutUI.fxml");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        ScreenManager.showStyledAlert(alert);
    }

    @Override
    public void onMessage(Message msg) {
        if (Protocol.RANKING_LIST.equals(msg.getCmd())) {
            // 服务端返回的 JSON 已被解析，这里是手动拼接的 JSON 数组
            // 暂时解析 data 中的原始 JSON 字符串
            org.json.JSONArray users = msg.getData().optJSONArray("users");
            if (users != null) {
                drawerRanks.clear();
                for (int i = 0; i < users.length(); i++) {
                    org.json.JSONObject u = users.getJSONObject(i);
                    drawerRanks.add(u.getString("rank") + ". " + u.getString("username") + "  " + u.getString("score"));
                }
            }
        } else if (Protocol.USER_SCORE_UPDATE.equals(msg.getCmd())) {
            if (currentUser != null) {
                currentUser.setScore(msg.getInt("score"));
                currentUser.setWinCount(msg.getInt("winCount"));
                currentUser.setLoseCount(msg.getInt("loseCount"));
                currentUser.setDrawCount(msg.getInt("drawCount"));
                if (currentUser.getScore() > 0) {
                    userScoreLabel.setText(currentUser.getScore() + " 分");
                } else {
                    userScoreLabel.setText("未定级");
                }
            }
        }
    }
}