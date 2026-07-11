package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
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

public class QuickMatchReadyController implements NetworkListener {
    @FXML private Label rankLabel;
    @FXML private Label matchStatus;
    @FXML private Button matchBtn;
    @FXML private Label countdownLabel;

    private User currentUser;
    private boolean previewMode;
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
    }

    @FXML
    private void handleReady() {
        isReady = !isReady;
        if (isReady) {
            matchBtn.setText(I18N.get("ranked.ready"));
            matchBtn.getStyleClass().remove("btn-primary");
            matchBtn.getStyleClass().add("btn-success");
            matchBtn.setDisable(false);
            matchStatus.setText(I18N.get("ranked.opponentReady", opponentUsername));
            matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#15803d", "#86efac") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
        } else {
            matchBtn.setText(I18N.get("ranked.notReady"));
            matchBtn.getStyleClass().remove("btn-success");
            matchBtn.getStyleClass().add("btn-primary");
            matchBtn.setDisable(false);
            matchStatus.setText(I18N.get("ranked.clickToReady", opponentUsername));
            matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
        }
        ClientMain.getConnector().send(Message.of(Protocol.QUICK_READY, "ready", isReady ? "true" : "false"));
    }

    private void stopMatching() {
        isFound = false;
        isReady = false;
        opponentUsername = "";
        stopCountdown();
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
                        countdownLabel.setText(I18N.get("ranked.countdown", countdownSeconds));
                        countdownSeconds--;
                    } else {
                        stopCountdown();
                        countdownLabel.setText("");
                        matchStatus.setText(I18N.get("ranked.timeout"));
                        matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
                        ClientMain.getConnector().send(Protocol.CANCEL_MATCH);
                        stopMatching();
                        backToLobby();
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
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(I18N.get("message.title"));
        alert.setHeaderText(null);
        alert.setContentText(I18N.get("ranked.cannotLeave"));
        ScreenManager.showStyledAlert(alert);
    }

    private void backToLobby() {
        Platform.runLater(() -> {
            LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
            if (currentUser != null) {
                ctrl.setCurrentUser(currentUser);
            }
            ctrl.setPreviewMode(previewMode);
        });
    }

    public void setOpponent(String opponent) {
        this.opponentUsername = opponent;
        isFound = true;
        startCountdown();
        matchStatus.setText(I18N.get("ranked.clickToReady", opponentUsername));
        matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
        matchBtn.setText(I18N.get("ranked.notReady"));
        matchBtn.getStyleClass().add("btn-primary");
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.QUICK_MATCH_FOUND.equals(cmd)) {
            opponentUsername = msg.getString("opponentUsername");
            isFound = true;
            startCountdown();
            Platform.runLater(() -> {
                matchStatus.setText(I18N.get("ranked.foundOpponent", opponentUsername));
                matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#15803d", "#86efac") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
                matchBtn.setText(I18N.get("ranked.notReady"));
                matchBtn.getStyleClass().add("btn-primary");
            });
        } else if (Protocol.QUICK_READY.equals(cmd)) {
            boolean blackReady = "true".equals(msg.getString("blackReady"));
            boolean whiteReady = "true".equals(msg.getString("whiteReady"));
            Platform.runLater(() -> {
                if (isReady) {
                    boolean opponentReady = blackReady && whiteReady;
                    if (opponentReady) {
                        matchStatus.setText(I18N.get("ranked.bothReady", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#15803d", "#86efac") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
                    } else {
                        matchStatus.setText(I18N.get("ranked.waitOpponentReady", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#b45309", "#fcd34d") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
                    }
                } else {
                    boolean opponentReady = blackReady || whiteReady;
                    if (opponentReady) {
                        matchStatus.setText(I18N.get("ranked.opponentPrepared", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#b45309", "#fcd34d") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
                    } else {
                        matchStatus.setText(I18N.get("ranked.clickToReady", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-size: 16px; -fx-text-alignment: center;");
                    }
                }
            });
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
                matchStatus.setText(I18N.get("quick.canceled"));
                matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-size: 16px;");
            });
            stopMatching();
            backToLobby();
        } else if (Protocol.MATCH_TIMEOUT.equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText(I18N.get("quick.timeout"));
                matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + "; -fx-font-size: 16px;");
            });
            stopMatching();
            backToLobby();
        } else if (Protocol.ERROR.equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText(I18N.get("quick.matchFail", msg.getString("msg")));
                matchStatus.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + "; -fx-font-size: 16px;");
            });
            stopMatching();
            backToLobby();
        }
    }
}