package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController implements NetworkListener {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private javafx.scene.control.Button btnCountryCode;
    @FXML private javafx.scene.layout.VBox inlineCodeMenu;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        hideInlineMenu();
        usernameField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });
        emailField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });
        phoneField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });
        passwordField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });
        confirmField.focusedProperty().addListener((obs, old, nv) -> { if (nv) hideInlineMenu(); });
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

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = PhoneInputValidator.toE164(selectedCode(), phoneField.getText());
        String password = passwordField.getText().trim();
        String confirm  = confirmField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            errorLabel.setText("用户名、邮箱、手机号和密码不能为空");
            return;
        }
        if (username.length() < 4 || username.length() > 16) {
            errorLabel.setText("用户名长度需 4-16 位");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            errorLabel.setText("邮箱格式不正确");
            return;
        }
        if (phone == null) {
            errorLabel.setText("请选择国家区号后填写有效手机号");
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
            if (!ClientMain.getConnector().isConnected()) {
                ClientMain.getConnector().connect(AppConfig.SERVER_HOST, AppConfig.SERVER_PORT);
            }
            ClientMain.getConnector().addListener(this);
            ClientMain.getConnector().send(Message.of(Protocol.REGISTER,
                    "username", username,
                    "password", password,
                    "email", email,
                    "phone", phone));
        } catch (Exception e) {
            errorLabel.setText("连接服务器失败: " + e.getMessage());
        }
    }

    private String selectedCode() {
        if (btnCountryCode == null) return "+86";
        String value = btnCountryCode.getText();
        return value.replace("▼", "").trim();
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
            errorLabel.setText("注册成功，请登录");
            ClientMain.getConnector().disconnect();
            ScreenManager.switchTo("/fxml/LoginUI.fxml");
        } else if ("REGISTER_FAIL".equals(cmd)) {
            errorLabel.setText(msg.getString("msg"));
            ClientMain.getConnector().disconnect();
        }
    }
}
