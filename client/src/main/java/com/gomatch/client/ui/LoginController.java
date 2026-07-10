package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.UserSettings;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.client.security.CredentialStore;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController implements NetworkListener {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberCheckBox;
    @FXML private Label errorLabel;
    @FXML private AsciiTextCanvas brandCanvas;

    private final CredentialStore credentialStore = new CredentialStore();

    @FXML
    private void initialize() {
        rememberCheckBox.setSelected(UserSettings.get().rememberPassword);
        credentialStore.loadUsername().ifPresent(usernameField::setText);
        passwordField.clear();
    }

    private void saveOrClearCredentials() {
        try {
            if (rememberCheckBox.isSelected()) {
                credentialStore.saveUsername(usernameField.getText());
            } else {
                credentialStore.clear();
            }
        } catch (IOException ignored) {
            // Credential persistence is optional and must never block login.
        }
    }

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
            ClientMain.getConnector().send(Message.of(Protocol.LOGIN,
                    "username", username, "password", password));
        } catch (Exception error) {
            errorLabel.setText("连接服务器失败: " + error.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        if (brandCanvas != null) brandCanvas.stop();
        ScreenManager.switchTo("/fxml/RegisterUI.fxml");
    }

    @FXML
    private void handleForgotPassword() {
        if (brandCanvas != null) brandCanvas.stop();
        ScreenManager.switchTo("/fxml/ForgotPasswordUI.fxml");
    }

    @FXML
    private void handlePreview() {
        if (brandCanvas != null) brandCanvas.stop();
        User fakeUser = new User(0, "游客");
        fakeUser.setScore(1000);
        AppConfig.setCurrentUser(fakeUser);
        AppConfig.setPreviewMode(true);
        LobbyController controller = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        controller.setCurrentUser(fakeUser);
        controller.setPreviewMode(true);
    }

    @Override
    public void onMessage(Message message) {
        if (Protocol.LOGIN_OK.equals(message.getCmd())) {
            handleLoginSuccess(message);
        } else if (Protocol.LOGIN_FAIL.equals(message.getCmd())) {
            errorLabel.setText(message.getString("msg"));
            ClientMain.getConnector().disconnect();
        }
    }

    private void handleLoginSuccess(Message message) {
        User user = new User();
        user.setUserId(message.getLong("userId"));
        user.setUsername(message.getString("username"));
        user.setScore(message.getInt("score"));
        user.setWinCount(message.getInt("winCount"));
        user.setLoseCount(message.getInt("loseCount"));
        user.setDrawCount(message.getInt("drawCount"));
        user.setRank(message.getInt("rank"));
        user.setRole(message.getString("role").isEmpty() ? "PLAYER" : message.getString("role"));
        user.setAvatarUrl(message.getString("avatar"));
        AppConfig.setCurrentUser(user);
        AppConfig.setPreviewMode(false);

        String token = message.getString("token");
        if (!token.isEmpty()) {
            AppConfig.setAuthToken(token);
        }
        saveOrClearCredentials();
        if (brandCanvas != null) brandCanvas.stop();

        String roomId = message.getString("roomId");
        if (!roomId.isEmpty()) {
            BoardController boardController = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            boardController.resumeFromLogin(
                    String.valueOf(message.getLong("blackUserId")),
                    String.valueOf(message.getLong("whiteUserId")),
                    user.getUsername(),
                    message.getInt("currentColor"),
                    String.valueOf(message.optLong("disconnectedUserId", 0)),
                    message.has("blackAvatarUrl") ? message.getString("blackAvatarUrl") : "",
                    message.has("whiteAvatarUrl") ? message.getString("whiteAvatarUrl") : "");
        } else {
            LobbyController controller = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
            controller.setCurrentUser(user);
            controller.setPreviewMode(false);
        }
    }
}
