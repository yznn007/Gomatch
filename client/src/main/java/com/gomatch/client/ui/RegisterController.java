package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController implements NetworkListener {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        ClientMain.getConnector().addListener(this);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm = confirmField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("用户名和密码不能为空");
            return;
        }
        if (username.length() < 4 || username.length() > 16) {
            errorLabel.setText("用户名长度需 4-16 位");
            return;
        }
        if (password.length() < 6) {
            errorLabel.setText("密码长度需 ≥6 位");
            return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText("两次密码输入不一致");
            return;
        }
        try {
            ClientMain.getConnector().connect("127.0.0.1", 8888);
            ClientMain.getConnector().send(Protocol.REGISTER, username, password);
        } catch (Exception e) {
            errorLabel.setText("连接服务器失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LoginUI.fxml");
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.LOGIN_OK.equals(command)) {
            ScreenManager.switchTo("/fxml/LoginUI.fxml");
        } else if (Protocol.LOGIN_FAIL.equals(command)) {
            errorLabel.setText(args[0]);
            ClientMain.getConnector().disconnect();
        }
    }
}
