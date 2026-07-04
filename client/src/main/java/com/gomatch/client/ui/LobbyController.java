package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.util.List;

public class LobbyController implements NetworkListener {
    @FXML private Label userInfoLabel;
    @FXML private Label userScoreLabel;
    @FXML private Label avatarLabel;
    @FXML private VBox rankDrawer;
    @FXML private ListView<String> rankDrawerList;

    private final ObservableList<String> drawerRanks = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
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
    private void handleFriends() {
        showAlert(previewMode ? "预览模式：好友功能" : "好友功能开发中");
    }

    @FXML
    private void handleSettings() {
        showAlert(previewMode ? "预览模式：设置功能" : "设置功能开发中");
    }

    @FXML
    private void handleAbout() {
        ScreenManager.switchTo("/fxml/AboutUI.fxml");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.showAndWait();
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.RANKING_LIST.equals(command)) {
            drawerRanks.clear();
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !args[i].isEmpty()) {
                    drawerRanks.add((i + 1) + ". " + args[i]);
                }
            }
        }
    }
}