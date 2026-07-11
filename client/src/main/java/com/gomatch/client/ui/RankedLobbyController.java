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
        if (score <= 0)        tier = I18N.get("ranked.unranked");
        else if (score < 1100) tier = I18N.get("ranked.bronze") + " " + score;
        else if (score < 1300) tier = I18N.get("ranked.silver") + " " + score;
        else if (score < 1500) tier = I18N.get("ranked.gold") + " " + score;
        else if (score < 1800) tier = I18N.get("ranked.platinum") + " " + score;
        else if (score < 2100) tier = I18N.get("ranked.diamond") + " " + score;
        else                   tier = I18N.get("ranked.master") + " " + score;
        rankLabel.setText(I18N.get("ranked.currentTier", tier));
    }

    @FXML
    private void handleMatch() {
        if (!isMatching && !isFound) {
            isMatching = true;
            matchStatus.setText(I18N.get("ranked.finding"));
            matchStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px;");
            matchBtn.setText(I18N.get("ranked.cancelMatch"));
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
            matchBtn.setText(I18N.get("ranked.ready"));
            matchBtn.getStyleClass().remove("btn-primary");
            matchBtn.getStyleClass().add("btn-success");
            matchBtn.setDisable(false);
            matchStatus.setText(I18N.get("ranked.opponentReady", opponentUsername));
            matchStatus.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-text-alignment: center;");
        } else {
            matchBtn.setText(I18N.get("ranked.notReady"));
            matchBtn.getStyleClass().remove("btn-success");
            matchBtn.getStyleClass().add("btn-primary");
            matchBtn.setDisable(false);
            matchStatus.setText(I18N.get("ranked.clickToReady", opponentUsername));
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
        matchBtn.setText(I18N.get("ranked.startMatch"));
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
                        countdownLabel.setText(I18N.get("ranked.countdown", countdownSeconds));
                        countdownSeconds--;
                    } else {
                        stopCountdown();
                        countdownLabel.setText("");
                        matchStatus.setText(I18N.get("ranked.timeout"));
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
            alert.setTitle(I18N.get("message.title"));
            alert.setHeaderText(null);
            alert.setContentText(I18N.get("ranked.cannotLeave"));
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
                matchStatus.setText(I18N.get("ranked.foundOpponent", opponentUsername));
                matchStatus.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-text-alignment: center;");
                matchBtn.setText(I18N.get("ranked.notReady"));
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
                        matchStatus.setText(I18N.get("ranked.bothReady", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-text-alignment: center;");
                    } else {
                        matchStatus.setText(I18N.get("ranked.waitOpponentReady", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px; -fx-text-alignment: center;");
                    }
                } else {
                    boolean opponentReady = blackReady || whiteReady;
                    if (opponentReady) {
                        matchStatus.setText(I18N.get("ranked.opponentPrepared", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px; -fx-text-alignment: center;");
                    } else {
                        matchStatus.setText(I18N.get("ranked.clickToReady", opponentUsername));
                        matchStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 16px; -fx-text-alignment: center;");
                    }
                }
            });
        } else if (Protocol.JOIN_OK.equals(cmd)) {
            if (!isFound) {
                matchStatus.setText(I18N.get("ranked.matchOk"));
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
                matchStatus.setText(I18N.get("ranked.canceled"));
                matchStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px;");
            });
        } else if (Protocol.ERROR.equals(cmd)) {
            Platform.runLater(() -> {
                matchStatus.setText(I18N.get("ranked.matchFail", msg.getString("msg")));
                matchStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px;");
            });
            stopMatching();
        }
    }
}