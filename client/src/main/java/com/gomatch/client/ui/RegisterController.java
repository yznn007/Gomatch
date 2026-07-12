package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterController implements NetworkListener {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Button btnCountryCode;
    @FXML private VBox inlineCodeMenu;
    @FXML private VBox countryCodeCard;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        hideInlineMenu();
        phoneField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });
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
        Button btn = (Button) e.getSource();
        btnCountryCode.setText(btn.getText() + " ▼");
        hideInlineMenu();
    }

    private String selectedCode() {
        if (btnCountryCode == null) return "+86";
        return btnCountryCode.getText().replace("▼", "").trim();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            errorLabel.setText(I18N.get("register.allRequired"));
            return;
        }
        if (username.length() < 4 || username.length() > 16) {
            errorLabel.setText(I18N.get("register.usernameLength"));
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            errorLabel.setText(I18N.get("register.emailInvalid"));
            return;
        }
        if (password.length() < 6) {
            errorLabel.setText(I18N.get("register.pwdLength"));
            return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText(I18N.get("register.pwdMismatch"));
            return;
        }

        // 手机号为选填，若填写则校验格式
        String rawPhone = phoneField.getText().trim();
        String phone = null;
        if (!rawPhone.isEmpty()) {
            phone = PhoneInputValidator.toE164(selectedCode(), rawPhone);
            if (phone == null) {
                errorLabel.setText(I18N.get("register.phoneInvalid"));
                return;
            }
        }

        try {
            if (!ClientMain.getConnector().isConnected()) {
                ClientMain.getConnector().connect(AppConfig.SERVER_HOST, AppConfig.SERVER_PORT);
            }
            ClientMain.getConnector().addListener(this);
            Message msg = Message.of(Protocol.REGISTER,
                    "username", username,
                    "password", password,
                    "email", email);
            if (phone != null) {
                msg.getData().put("phone", phone);
            }
            ClientMain.getConnector().send(msg);
        } catch (Exception e) {
            errorLabel.setText(I18N.get("register.connectFailed", e.getMessage()));
        }
    }

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LoginUI.fxml");
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        // 服务端注册成功应回复 REGISTER_OK，兼容旧服务端也处理 LOGIN_OK
        if (Protocol.REGISTER_OK.equals(cmd) || Protocol.LOGIN_OK.equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText(I18N.get("register.success"));
            ClientMain.getConnector().disconnect();
            ScreenManager.switchTo("/fxml/LoginUI.fxml");
        } else if ("REGISTER_FAIL".equals(cmd)) {
            errorLabel.setText(msg.getString("msg"));
            ClientMain.getConnector().disconnect();
        }
    }
}
