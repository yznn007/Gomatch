package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.util.Timer;
import java.util.TimerTask;

public class RankedLobbyController implements NetworkListener {
    @FXML private Label rankLabel;
    @FXML private Label matchStatus;
    @FXML private Button matchBtn;
    @FXML private Label countdownLabel;

    private User currentUser;
    private boolean previewMode;
    private boolean isMatching = false;
    private boolean isFound = false;
    private boolean isReady = false;
    private String opponentUsername = "";
    private Timer countdownTimer;
    private int countdownSeconds = 60;

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        int score = user.getScore();
        String tier;
        if (score <= 0)        tier = "未定级";
        else if (score < 1100) tier = "青铜 " + score;
        else if (score < 1300) tier = "白银 " + score;
        else if (score < 1500) tier = "黄金 " + score;
        else if (score < 1800) tier = "铂金 " + score;
        else if (score < 2100) tier = "钻石 " + score;
        else                   tier = "王者 " + score;
        rankLabel.setText("当前段位: " + tier);
    }

    @FXML
    private void handleMatch() {
        if (!isMatching && !isFound) {
            isMatching = true;
            matchStatus.setText("正在寻找对手...");
            matchStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px;");
            matchBtn.setText("取消排位匹配");
            matchBtn.getStyleClass().remove("btn-primary");
            matchBtn.getStyleClass().add("btn-danger");
            ClientMain.getConnector().addListener(this);
            ClientMain.getConnector().send(Protocol.RANKED_MATCH);
        } else if (isMatching && !isFound) {
            cancelMatch();
        } else if (isFound) {
            handleReady();
        }
    }

    private void handleReady() {
        isReady = !isReady;
        if (isReady) {
            matchBtn.setText("已准备");
            matchBtn.getStyleClass().remove("btn-primary");
            matchBtn.getStyleClass().add("btn-success");
            matchBtn.setDisable(false);
            matchStatus.setText("对手: " + opponentUsername + "\n我方已准备，等待对方准备...");
            matchStatus.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-text-alignment: center;");
        } else {
            matchBtn.setText("未准备");
            matchBtn.getStyleClass().remove("btn-success");
            matchBtn.getStyleClass().add("btn-primary");
            matchBtn.setDisable(false);
            matchStatus.setText("对手: " + opponentUsername + "\n点击按钮准备");
            matchStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 16px; -fx-text-alignment: center;");
        }
        ClientMain.getConnector().send(Message.of(Protocol.RANKED_READY, "ready", isReady ? "true" : "false"));
    }

    private void cancelMatch() {
        ClientMain.getConnector().send(Protocol.CANCEL_MATCH);
        stopMatching();
    }

    private void stopMatching() {
        isMatching = false;
        isFound = false;
        isReady = false;
        opponentUsername = "";
        stopCountdown();
        matchBtn.setText("开 始 排 位 匹 配");
        matchBtn.getStyleClass().removeAll("btn-danger", "btn-success", "btn-primary");
        matchBtn.getStyleClass().add("btn-primary");
        matchBtn.setDisable(false);
        matchStatus.setText("");
        matchStatus.setStyle("");
        countdownLabel.setText("");
        ClientMain.getConnector().removeListener(this);
    }

    private void startCountdown() {
        stopCountdown();
        countdownSeconds = 60;
        countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (countdownSeconds > 0) {
                        countdownLabel.setText("准备时间: " + countdownSeconds + "s");
                        countdownSeconds--;
                    } else {
                        stopCountdown();
                        countdownLabel.setText("");
                        matchStatus.setText("准备时间超时，已取消匹配");
                        matchStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-text-alignment: center;");
                        ClientMain.getConnector().send(Protocol.CANCEL_MATCH);
                        stopMatching();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    @FXML
    private void handleBack() {
        if (isFound) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("您当前在准备阶段，不能返回大厅");
            ScreenManager.showStyledAlert(alert);
            return;
        }
        if (isMatching) {
            ClientMain.getConnector().send(Protocol.CANCEL_MATCH);
            stopMatching();
        }
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        System.out.println("[RankedLobby] Received message: " + cmd + ", isMatching=" + isMatching + ", isFound=" + isFound + ", isReady=" + isReady);
        if ("RANKED_MATCH_WAIT".equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText(msg.getString("msg"));
                matchStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px;");
            });
        } else if (Protocol.RANKED_MATCH_FOUND.equals(cmd)) {
            isFound = true;
            isMatching = false;
            opponentUsername = msg.getString("opponentUsername");
            startCountdown();
            Platform.runLater(() -> {
                matchStatus.setText("找到对手: " + opponentUsername + "\n点击按钮准备");
                matchStatus.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-text-alignment: center;");
                matchBtn.setText("未准备");
                matchBtn.getStyleClass().remove("btn-danger");
                matchBtn.getStyleClass().add("btn-primary");
            });
        } else if (Protocol.RANKED_READY.equals(cmd)) {
            boolean blackReady = "true".equals(msg.getString("blackReady"));
            boolean whiteReady = "true".equals(msg.getString("whiteReady"));
            Platform.runLater(() -> {
                if (isReady) {
                    boolean opponentReady = blackReady && whiteReady;
                    if (opponentReady) {
                        matchStatus.setText("对手: " + opponentUsername + "\n双方已准备，即将开始...");
                        matchStatus.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-text-alignment: center;");
                    } else {
                        matchStatus.setText("对手: " + opponentUsername + "\n对战即将开始，等待对方玩家准备");
                        matchStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px; -fx-text-alignment: center;");
                    }
                } else {
                    boolean opponentReady = blackReady || whiteReady;
                    if (opponentReady) {
                        matchStatus.setText("对手: " + opponentUsername + "\n对方已准备，等待我方准备...");
                        matchStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px; -fx-text-alignment: center;");
                    } else {
                        matchStatus.setText("对手: " + opponentUsername + "\n点击按钮准备");
                        matchStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 16px; -fx-text-alignment: center;");
                    }
                }
            });
        } else if (Protocol.JOIN_OK.equals(cmd)) {
            if (!isFound) {
                matchStatus.setText("匹配成功，等待对手信息...");
                matchStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px;");
            }
        } else if (Protocol.GAME_START.equals(cmd)) {
            stopMatching();
            Platform.runLater(() -> {
                BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                String bId = String.valueOf(msg.getLong("blackUserId"));
                String wId = String.valueOf(msg.getLong("whiteUserId"));
                String bName = msg.has("blackUsername") ? msg.getString("blackUsername") : bId;
                String wName = msg.has("whiteUsername") ? msg.getString("whiteUsername") : wId;
                int bScore = msg.has("blackScore") ? msg.getInt("blackScore") : 0;
                int wScore = msg.has("whiteScore") ? msg.getInt("whiteScore") : 0;
                String username = currentUser != null ? currentUser.getUsername() : "";
                ctrl.setGameInfo(bId, bName, wId, wName, username, bScore, wScore);
                String bAvatar = msg.has("blackAvatarUrl") ? msg.getString("blackAvatarUrl") : "";
                String wAvatar = msg.has("whiteAvatarUrl") ? msg.getString("whiteAvatarUrl") : "";
                ctrl.setPlayerAvatars(bAvatar, wAvatar);
            });
        } else if (Protocol.CANCEL_MATCH.equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText("已取消匹配");
                matchStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px;");
            });
        } else if (Protocol.ERROR.equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText("匹配失败: " + msg.getString("msg"));
                matchStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px;");
            });
            stopMatching();
        }
    }
}