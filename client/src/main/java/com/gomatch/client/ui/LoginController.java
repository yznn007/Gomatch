package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements NetworkListener {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final Gson gson = new Gson();

    @FXML
    private void initialize() {
        ClientMain.getConnector().addListener(this);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("用户名和密码不能为空");
            return;
        }
        try {
            ClientMain.getConnector().connect("127.0.0.1", 8888);
            ClientMain.getConnector().send(Protocol.LOGIN, username, password);
        } catch (Exception e) {
            errorLabel.setText("连接服务器失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        ScreenManager.switchTo("/fxml/RegisterUI.fxml");
    }

    @FXML
    private void handlePreview() {
        User fakeUser = new User(0, "游客");
        fakeUser.setScore(1000);
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(fakeUser);
        ctrl.setPreviewMode(true);
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.LOGIN_OK.equals(command)) {
            User user = gson.fromJson(args[0], User.class);
            LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
            ctrl.setCurrentUser(user);
        } else if (Protocol.LOGIN_FAIL.equals(command)) {
            errorLabel.setText(args[0]);
            ClientMain.getConnector().disconnect();
        }
    }
}
