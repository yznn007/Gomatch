package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 忘记密码流程控制器。
 * 
 * 流程：输入邮箱 → 获取验证码（60s有效）→ 输入验证码 → 设置新密码
 */
public class ForgotPasswordController implements NetworkListener {

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private Label codeDisplayLabel;
    @FXML private Label codeExpiryLabel;
    @FXML private Label messageLabel;
    @FXML private VBox stepEmailBox;
    @FXML private VBox stepCodeBox;
    @FXML private VBox stepPasswordBox;

    private String currentEmail = "";
    private Timeline codeVisibleTimer;   // 验证码可见计时器（30s后隐藏）
    private Timeline codeExpiryTimer;    // 验证码过期计时器（60s后失效）
    private int displaySecondsRemaining = 30;
    private int expirySecondsRemaining = 60;

    @FXML
    private void initialize() {
        // 初始化不做任何事
    }

    // ==================== Step 1: 发送验证码 ====================

    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            messageLabel.setText("请输入邮箱地址");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            messageLabel.setText("邮箱格式不正确");
            return;
        }

        currentEmail = email;
        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#b45309", "#fcd34d") + ";");

        try {
            if (!ClientMain.getConnector().isConnected()) {
                ClientMain.getConnector().connect(AppConfig.SERVER_HOST, AppConfig.SERVER_PORT);
            }
            ClientMain.getConnector().addListener(this);
            ClientMain.getConnector().send(Message.of(Protocol.FORGOT_PASSWORD, "email", email));
        } catch (Exception e) {
            messageLabel.setText("连接服务器失败: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
        }
    }

    // ==================== Step 2: 验证验证码 ====================

    @FXML
    private void handleVerifyCode() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            messageLabel.setText("请输入验证码");
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
            return;
        }
        if (code.length() != 6) {
            messageLabel.setText("验证码为6位数字");
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
            return;
        }

        messageLabel.setText("");
        try {
            ClientMain.getConnector().send(Message.of(Protocol.VERIFY_FORGOT_CODE,
                    "email", currentEmail,
                    "code", code));
        } catch (Exception e) {
            messageLabel.setText("发送失败: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
        }
    }

    // ==================== Step 3: 重置密码 ====================

    @FXML
    private void handleResetPassword() {
        String newPwd = newPasswordField.getText().trim();
        String confirm = confirmNewPasswordField.getText().trim();

        if (newPwd.isEmpty()) {
            messageLabel.setText("新密码不能为空");
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
            return;
        }
        if (newPwd.length() < 6) {
            messageLabel.setText("密码长度需 ≥6 位");
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
            return;
        }
        if (!newPwd.equals(confirm)) {
            messageLabel.setText("两次密码输入不一致");
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
            return;
        }
 
        messageLabel.setText("");
        try {
            ClientMain.getConnector().send(Message.of(Protocol.FORGOT_PWD_RESET,
                    "email", currentEmail,
                    "code", codeField.getText().trim(),
                    "newPassword", newPwd));
        } catch (Exception e) {
            messageLabel.setText("发送失败: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
        }
    }

    // ==================== 返回 ====================

    @FXML
    private void handleBack() {
        stopTimers();
        ClientMain.getConnector().removeListener(this);
        try {
            ClientMain.getConnector().disconnect();
        } catch (Exception ignored) {}
        ScreenManager.switchTo("/fxml/LoginUI.fxml");
    }

    // ==================== 网络消息处理 ====================

    @Override
    public void onMessage(Message msg) {
        Platform.runLater(() -> {
            String cmd = msg.getCmd();
            switch (cmd) {
                case Protocol.FORGOT_PWD_CODE -> handleCodeReceived(msg);
                case Protocol.FORGOT_PWD_VERIFY_OK -> handleVerifyOk();
                case Protocol.FORGOT_PWD_OK -> handleResetOk();
                case Protocol.FORGOT_PWD_FAIL -> handleFailed(msg);
            }
        });
    }

    private void handleCodeReceived(Message msg) {
        String code = msg.getString("code");
        codeDisplayLabel.setText(code);

        // 显示验证码区域
        stepEmailBox.setVisible(false);
        stepEmailBox.setManaged(false);
        stepCodeBox.setVisible(true);
        stepCodeBox.setManaged(true);
        stepPasswordBox.setVisible(false);
        stepPasswordBox.setManaged(false);

        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");

        // 启动定时器
        startCodeTimers();
    }

    private void handleVerifyOk() {
        // 验证成功，切换到密码重置界面
        stopTimers();

        stepEmailBox.setVisible(false);
        stepEmailBox.setManaged(false);
        stepCodeBox.setVisible(false);
        stepCodeBox.setManaged(false);
        stepPasswordBox.setVisible(true);
        stepPasswordBox.setManaged(true);

        messageLabel.setText("验证成功，请设置新密码");
        messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");
    }

    private void handleResetOk() {
        stopTimers();
        messageLabel.setText("密码重置成功，请返回登录");
        messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");

        // 禁用密码输入区域
        stepPasswordBox.setDisable(true);
    }

    private void handleFailed(Message msg) {
        String errMsg = msg.getString("msg");
        messageLabel.setText(errMsg.isEmpty() ? "操作失败" : errMsg);
        messageLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
    }

    // ==================== 验证码定时器 ====================

    private void startCodeTimers() {
        // 可见计时器：30秒后隐藏验证码显示
        displaySecondsRemaining = 30;
        codeVisibleTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            displaySecondsRemaining--;
            if (displaySecondsRemaining <= 0) {
                codeDisplayLabel.setText("******");
                codeExpiryLabel.setText("");
                codeVisibleTimer.stop();
            } else {
                codeExpiryLabel.setText("验证码将在 " + displaySecondsRemaining + " 秒后隐藏，60秒内有效");
            }
        }));
        codeVisibleTimer.setCycleCount(Timeline.INDEFINITE);
        codeVisibleTimer.play();

        // 过期计时器：60秒后提示过期
        expirySecondsRemaining = 60;
        codeExpiryTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            expirySecondsRemaining--;
            if (expirySecondsRemaining <= 0) {
                codeExpiryTimer.stop();
                codeDisplayLabel.setText("已过期");
                codeExpiryLabel.setText("验证码已过期，请重新获取");
                codeExpiryLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
            }
        }));
        codeExpiryTimer.setCycleCount(Timeline.INDEFINITE);
        codeExpiryTimer.play();
    }

    private void stopTimers() {
        if (codeVisibleTimer != null) {
            codeVisibleTimer.stop();
            codeVisibleTimer = null;
        }
        if (codeExpiryTimer != null) {
            codeExpiryTimer.stop();
            codeExpiryTimer = null;
        }
    }
}
