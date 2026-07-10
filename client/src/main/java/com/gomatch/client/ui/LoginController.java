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
    @FXML private TextField accountField;
    @FXML private javafx.scene.control.Button btnCountryCode;
    @FXML private javafx.scene.layout.VBox inlineCodeMenu;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberCheckBox;
    @FXML private Label errorLabel;
    @FXML private AsciiTextCanvas brandCanvas;
    @FXML private javafx.scene.control.Button btnAccountTab;
    @FXML private javafx.scene.control.Button btnPhoneTab;
    @FXML private javafx.scene.layout.VBox countryCodeCard;

    private final CredentialStore credentialStore = new CredentialStore();
    private boolean isPhoneLogin = false;

    @FXML
    private void initialize() {
        rememberCheckBox.setSelected(UserSettings.get().rememberPassword);
        passwordField.clear();
        hideInlineMenu();
        if (!btnAccountTab.getStyleClass().contains("login-tab-btn-active")) {
            btnAccountTab.getStyleClass().add("login-tab-btn-active");
        }

        accountField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });
        passwordField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });

        credentialStore.loadUsername().ifPresent(saved -> {
            if (saved.startsWith("+")) {
                isPhoneLogin = true;
                countryCodeCard.setVisible(true);
                countryCodeCard.setManaged(true);
                if (!btnPhoneTab.getStyleClass().contains("login-tab-btn-active")) {
                    btnPhoneTab.getStyleClass().add("login-tab-btn-active");
                }
                btnAccountTab.getStyleClass().remove("login-tab-btn-active");

                String matchedCode = "+86";
                String[] codes = {"+86", "+1", "+81", "+44"};
                for (String code : codes) {
                    if (saved.startsWith(code)) {
                        matchedCode = code;
                    }
                }
                btnCountryCode.setText(matchedCode + " ▼");
                accountField.setText(saved.substring(matchedCode.length()));
                accountField.setPromptText("手机号");
            } else {
                accountField.setText(saved);
                accountField.setPromptText("用户名 / 邮箱 / ID");
            }
        });
    }

    @FXML
    private void toggleInlineMenu() {
        boolean show = !inlineCodeMenu.isVisible();
        inlineCodeMenu.setVisible(show);
        if (show) {
            inlineCodeMenu.applyCss();
        }
    }

    private void hideInlineMenu() {
        inlineCodeMenu.setVisible(false);
    }

    @FXML
    private void selectInlineCode(javafx.event.ActionEvent e) {
        javafx.scene.control.Button btn = (javafx.scene.control.Button) e.getSource();
        btnCountryCode.setText(btn.getText() + " ▼");
        hideInlineMenu();
    }

    private void saveOrClearCredentials() {
        try {
            if (rememberCheckBox.isSelected()) {
                String account = isPhoneLogin ? selectedCode() + accountField.getText().trim() : accountField.getText().trim();
                credentialStore.saveUsername(account);
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
    private void handleAccountTab() {
        if (!isPhoneLogin) return;
        isPhoneLogin = false;
        hideInlineMenu(); // 切换登录方式时，强制关闭可能开启的区号悬浮下拉菜单
        if (!btnAccountTab.getStyleClass().contains("login-tab-btn-active")) {
            btnAccountTab.getStyleClass().add("login-tab-btn-active");
        }
        btnPhoneTab.getStyleClass().remove("login-tab-btn-active");
        countryCodeCard.setVisible(false);
        countryCodeCard.setManaged(false);
        accountField.clear();
        accountField.setPromptText("用户名 / 邮箱 / ID");
    }

    @FXML
    private void handlePhoneTab() {
        if (isPhoneLogin) return;
        isPhoneLogin = true;
        hideInlineMenu(); // 切换登录方式时，强制关闭可能开启的区号悬浮下拉菜单
        if (!btnPhoneTab.getStyleClass().contains("login-tab-btn-active")) {
            btnPhoneTab.getStyleClass().add("login-tab-btn-active");
        }
        btnAccountTab.getStyleClass().remove("login-tab-btn-active");
        countryCodeCard.setVisible(true);
        countryCodeCard.setManaged(true);
        accountField.clear();
        accountField.setPromptText("手机号");
    }

    private String selectedCode() {
        if (btnCountryCode == null) return "+86";
        String value = btnCountryCode.getText();
        return value.replace("▼", "").trim();
    }

    @FXML
    private void handleLogin() {
        String rawAccount = accountField.getText().trim();
        String password = passwordField.getText().trim();
        if (rawAccount.isEmpty() || password.isEmpty()) {
            errorLabel.setText("账号和密码不能为空");
            return;
        }

        String account;
        if (isPhoneLogin) {
            account = PhoneInputValidator.toE164(selectedCode(), rawAccount);
            if (account == null) {
                errorLabel.setText("请输入有效的手机号");
                return;
            }
        } else {
            account = rawAccount;
        }

        try {
            if (!ClientMain.getConnector().isConnected()) {
                ClientMain.getConnector().connect(AppConfig.SERVER_HOST, AppConfig.SERVER_PORT);
            }
            ClientMain.getConnector().addListener(this);
            ClientMain.getConnector().send(Message.of(Protocol.LOGIN,
                    "account", account, "password", password));
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
