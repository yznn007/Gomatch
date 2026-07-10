package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.UserSettings;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class LoginController implements NetworkListener {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberCheckBox;
    @FXML private Label errorLabel;
    @FXML private AsciiTextCanvas brandCanvas;

    private static final File CRED_FILE = new File(
            System.getProperty("user.home"), ".gomatch/credentials.json");

    @FXML
    private void initialize() {
        // 读取用户偏好：记住密码默认勾选状态
        rememberCheckBox.setSelected(UserSettings.get().rememberPassword);
        loadSavedCredentials();
    }

    private void loadSavedCredentials() {
        if (!CRED_FILE.exists()) return;
        try (Reader r = new FileReader(CRED_FILE)) {
            SavedCredential c = AppConfig.GSON.fromJson(r, SavedCredential.class);
            if (c != null && c.username != null) {
                usernameField.setText(c.username);
                passwordField.setText(c.password != null ? c.password : "");
                rememberCheckBox.setSelected(c.password != null);
            }
        } catch (IOException ignored) { }
    }

    private void saveOrClearCredentials() {
        File dir = CRED_FILE.getParentFile();
        if (!dir.exists()) dir.mkdirs();

        if (rememberCheckBox.isSelected()) {
            try (Writer w = new FileWriter(CRED_FILE)) {
                SavedCredential c = new SavedCredential();
                c.username = usernameField.getText().trim();
                c.password = passwordField.getText().trim();
                AppConfig.GSON.toJson(c, w);
            } catch (IOException ignored) { }
        } else {
            try (Writer w = new FileWriter(CRED_FILE)) {
                SavedCredential c = new SavedCredential();
                c.username = usernameField.getText().trim();
                c.password = null;
                AppConfig.GSON.toJson(c, w);
            } catch (IOException ignored) { }
        }
    }

    private static class SavedCredential {
        String username;
        String password;
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
            ClientMain.getConnector().send(Message.of(Protocol.LOGIN, "username", username, "password", password));
        } catch (Exception e) {
            errorLabel.setText("连接服务器失败: " + e.getMessage());
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

        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(fakeUser);
        ctrl.setPreviewMode(true);
    }

    @Override
    public void onMessage(Message msg) {
        if (Protocol.LOGIN_OK.equals(msg.getCmd())) {
            User user = new User();
            user.setUserId(msg.getLong("userId"));
            user.setUsername(msg.getString("username"));
            user.setScore(msg.getInt("score"));
            user.setWinCount(msg.getInt("winCount"));
            user.setLoseCount(msg.getInt("loseCount"));
            user.setDrawCount(msg.getInt("drawCount"));
            user.setRank(msg.getInt("rank"));
            user.setRole(msg.getString("role").isEmpty() ? "PLAYER" : msg.getString("role"));
            user.setAvatarUrl(msg.getString("avatar"));

            AppConfig.setCurrentUser(user);
            AppConfig.setPreviewMode(false);

            String token = msg.getString("token");
            if (token != null && !token.isEmpty()) {
                AppConfig.setAuthToken(token);
            }

            saveOrClearCredentials();

            if (brandCanvas != null) brandCanvas.stop();

            String roomId = msg.getString("roomId");
            if (roomId != null && !roomId.isEmpty()) {
                String blackUserId   = String.valueOf(msg.getLong("blackUserId"));
                String whiteUserId   = String.valueOf(msg.getLong("whiteUserId"));
                int    currentColor  = msg.getInt("currentColor");
                String disconnectedUserId = String.valueOf(msg.optLong("disconnectedUserId", 0));
                // 双方头像 URL（断线重连时不会收到 GAME_START，必须从 LOGIN_OK 携带）
                String blackAvatarUrl = msg.has("blackAvatarUrl") ? msg.getString("blackAvatarUrl") : "";
                String whiteAvatarUrl = msg.has("whiteAvatarUrl") ? msg.getString("whiteAvatarUrl") : "";
                String username      = user.getUsername();

                BoardController boardCtrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                boardCtrl.resumeFromLogin(blackUserId, whiteUserId, username,
                        currentColor, disconnectedUserId, blackAvatarUrl, whiteAvatarUrl);
            } else {
                LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
                ctrl.setCurrentUser(user);
                ctrl.setPreviewMode(false);
            }
        } else if (Protocol.LOGIN_FAIL.equals(msg.getCmd())) {
            errorLabel.setText(msg.getString("msg"));
            ClientMain.getConnector().disconnect();
        }
    }
}

