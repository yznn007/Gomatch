package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
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

    private static final File CRED_FILE = new File(
            System.getProperty("user.home"), ".gomatch/credentials.json");

    @FXML
    private void initialize() {
        // 加载已保存的凭据
        loadSavedCredentials();
    }

    /** 加载本地保存的用户名密码 */
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

    /** 保存或清除记住的凭据 */
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
            // 不记住密码时只保留用户名，密码清空
            try (Writer w = new FileWriter(CRED_FILE)) {
                SavedCredential c = new SavedCredential();
                c.username = usernameField.getText().trim();
                c.password = null;
                AppConfig.GSON.toJson(c, w);
            } catch (IOException ignored) { }
        }
    }

    /** 用于 JSON 序列化的简单凭据对象 */
    private static class SavedCredential {
        String username;
        String password;
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

            saveOrClearCredentials();

            LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
            ctrl.setCurrentUser(user);
            ctrl.setPreviewMode(false);
        } else if (Protocol.LOGIN_FAIL.equals(command)) {
            errorLabel.setText(args[0]);
            ClientMain.getConnector().disconnect();
        }
    }
}
