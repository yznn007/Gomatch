package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ProfileController implements NetworkListener {
    @FXML private Label userIdLabel;
    @FXML private Label usernameLabel;
    @FXML private Label scoreLabel;
    @FXML private Label winLabel;
    @FXML private Label loseLabel;
    @FXML private Label drawLabel;
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private Label errorLabel;
    @FXML private TableView<String[]> recordTable;
    @FXML private TableColumn<String[], String> opponentCol;
    @FXML private TableColumn<String[], String> resultCol;
    @FXML private TableColumn<String[], String> timeCol;
    @FXML private TableColumn<String[], String> replayCol;

    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userIdLabel.setText(String.valueOf(user.getUserId()));
        usernameLabel.setText(user.getUsername());
        if (user.getScore() > 0) {
            scoreLabel.setText(String.valueOf(user.getScore()));
        } else {
            scoreLabel.setText("未定级");
        }
        winLabel.setText(String.valueOf(user.getWinCount()));
        loseLabel.setText(String.valueOf(user.getLoseCount()));
        drawLabel.setText(String.valueOf(user.getDrawCount()));
    }

    @FXML
    private void initialize() {
        if (!ClientMain.getConnector().isConnected()) {
            recordTable.getItems().addAll(
                new String[]{"玩家A", "胜", "2026-07-04 15:30", "回放"},
                new String[]{"玩家B", "负", "2026-07-04 16:10", "回放"}
            );
        }
    }

    @FXML
    private void handleChangePwd() {
        String oldPwd = oldPwdField.getText().trim();
        String newPwd = newPwdField.getText().trim();
        if (oldPwd.isEmpty() || newPwd.isEmpty()) {
            errorLabel.setText("密码不能为空");
            return;
        }
        if (newPwd.length() < 6) {
            errorLabel.setText("新密码长度需 ≥6 位");
            return;
        }
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send("CHANGE_PWD", oldPwd, newPwd);
        } else {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("预览模式：密码修改请求已模拟");
        }
    }

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(String command, String[] args) {
        if ("PWD_CHANGED".equals(command)) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("密码修改成功");
        } else if (Protocol.ERROR.equals(command)) {
            errorLabel.setText(args[0]);
        }
    }
}