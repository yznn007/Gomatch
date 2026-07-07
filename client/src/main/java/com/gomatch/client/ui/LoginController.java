package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements NetworkListener {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        // 监听器在 connect 成功后由 handleLogin 添加，
        // 这里不提前注册，避免未连接时空转以及来回切换重复注册。
    }

    /** 打开服务器配置二级弹窗 */
    @FXML
    private void handleOpenServerConfig() {
        ScreenManager.showDialog("/fxml/ServerConfigDialogUI.fxml");
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
            if (!ClientMain.getConnector().isConnected()) {
                ClientMain.getConnector().connect(AppConfig.SERVER_HOST, AppConfig.SERVER_PORT);
            }
            ClientMain.getConnector().addListener(this);
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

        AppConfig.setCurrentUser(fakeUser);
        AppConfig.setPreviewMode(true);

        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(fakeUser);
        ctrl.setPreviewMode(true);
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.LOGIN_OK.equals(command)) {
            User user = new User();
            user.setUserId(Long.parseLong(args[0]));
            user.setUsername(args[1]);
            user.setScore(Integer.parseInt(args[2]));
            user.setWinCount(Integer.parseInt(args[3]));
            user.setLoseCount(Integer.parseInt(args[4]));
            user.setDrawCount(Integer.parseInt(args[5]));

            AppConfig.setCurrentUser(user);
            AppConfig.setPreviewMode(false);

            LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
            ctrl.setCurrentUser(user);
            ctrl.setPreviewMode(false);
        } else if (Protocol.LOGIN_FAIL.equals(command)) {
            errorLabel.setText(args[0]);
            ClientMain.getConnector().disconnect();
        }
    }
}
