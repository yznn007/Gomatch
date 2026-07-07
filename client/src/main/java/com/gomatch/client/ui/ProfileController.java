package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.GameRecord;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfileController implements NetworkListener {
    @FXML private Label userIdLabel;
    @FXML private Label usernameLabel;
    @FXML private Label profileAvatarLabel;
    @FXML private Label scoreLabel;
    @FXML private Label winLabel;
    @FXML private Label loseLabel;
    @FXML private Label drawLabel;
    @FXML private Label rankLabel;
    @FXML private Label totalGamesLabel;
    @FXML private Label winRateLabel;
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private Label errorLabel;

    @FXML private VBox recordsContainer;

    // 新增：左侧菜单按钮与右侧面板 FXML 绑定
    @FXML private Button menuInfoBtn;
    @FXML private Button menuRecordBtn;
    @FXML private Button menuPwdBtn;
    @FXML private VBox infoPane;
    @FXML private VBox recordPane;
    @FXML private VBox pwdPane;

    private final ObservableList<GameRecord> recordData = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        recordData.addListener((javafx.collections.ListChangeListener<GameRecord>) c -> renderRecordCards());
        ClientMain.getConnector().addListener(this);

        // 初始化激活默认标签页 (个人信息)
        showInfoTab();
    }

    private void renderRecordCards() {
        if (recordsContainer == null) return;
        javafx.application.Platform.runLater(() -> {
            recordsContainer.getChildren().clear();
            if (recordData.isEmpty()) {
                Label noRecordLabel = new Label("暂无对战记录，快去对战大厅来一局吧！");
                noRecordLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 0;");
                recordsContainer.getChildren().add(noRecordLabel);
                return;
            }

            for (GameRecord record : recordData) {
                // Card Root HBox
                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Result Badge & ELO change indicator
                javafx.scene.layout.StackPane badgeContainer = new javafx.scene.layout.StackPane();
                badgeContainer.getStyleClass().add("result-badge");
                String result = record.getResult() != null ? record.getResult() : "平";
                Label badgeText = new Label(result.substring(0, 1));
                badgeText.getStyleClass().add("result-badge-text");
                badgeContainer.getChildren().add(badgeText);

                Label scoreChangeLabel = new Label();
                scoreChangeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 32px;");
                if (result.contains("胜")) {
                    badgeContainer.getStyleClass().add("result-badge-win");
                    scoreChangeLabel.setText("+15");
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #22c55e;");
                } else if (result.contains("负")) {
                    badgeContainer.getStyleClass().add("result-badge-lose");
                    scoreChangeLabel.setText("-10");
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #ef4444;");
                } else {
                    badgeContainer.getStyleClass().add("result-badge-draw");
                    scoreChangeLabel.setText("+0");
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #94a3b8;");
                }

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                Label opponentLabel = new Label("与 " + record.getOpponent() + " 的对局");
                opponentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                
                // Parse moves and construct detailed info (duration, steps)
                String moves = record.getMoves();
                int stepsCount = 0;
                if (moves != null && !moves.isEmpty()) {
                    stepsCount = moves.split(";").length;
                }
                
                String durationStr = "3分45秒"; // Default/Mock duration
                Label detailsLabel = new Label("落子: " + (stepsCount > 0 ? stepsCount : "--") + " 步 | 用时: " + durationStr);
                detailsLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

                infoBox.getChildren().addAll(opponentLabel, detailsLabel);

                // Right VBox: Time & Action Button
                VBox rightBox = new VBox(4);
                rightBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Label timeLabel = new Label(record.getTime());
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

                Button replayBtn = new Button("回放");
                replayBtn.getStyleClass().add("btn-primary");
                replayBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                replayBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                replayBtn.setOnAction(e -> {
                    String movesStr = record.getMoves();
                    if (movesStr != null && !movesStr.isEmpty()) {
                        BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                        ctrl.setReplayMode(true);
                        ctrl.replayMoves(movesStr);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "暂无棋谱数据");
                        ScreenManager.showStyledAlert(alert);
                    }
                });

                rightBox.getChildren().addAll(timeLabel, replayBtn);

                card.getChildren().addAll(badgeContainer, scoreChangeLabel, infoBox, rightBox);
                recordsContainer.getChildren().add(card);
            }
        });
    }

    // 新增：标签页动态切换逻辑
    private void switchTab(VBox activePane, Button activeBtn) {
        infoPane.setVisible(false);
        infoPane.setManaged(false);
        recordPane.setVisible(false);
        recordPane.setManaged(false);
        pwdPane.setVisible(false);
        pwdPane.setManaged(false);

        activePane.setVisible(true);
        activePane.setManaged(true);

        menuInfoBtn.getStyleClass().remove("active");
        menuRecordBtn.getStyleClass().remove("active");
        menuPwdBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    @FXML
    private void showInfoTab() {
        switchTab(infoPane, menuInfoBtn);
    }

    @FXML
    private void showRecordTab() {
        switchTab(recordPane, menuRecordBtn);
    }

    @FXML
    private void showPwdTab() {
        switchTab(pwdPane, menuPwdBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userIdLabel.setText("#" + user.getUserId());
        usernameLabel.setText(user.getUsername());
        if (profileAvatarLabel != null) {
            String name = user.getUsername();
            profileAvatarLabel.setText(name == null || name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());
        }
        scoreLabel.setText(user.getScore() > 0 ? String.valueOf(user.getScore()) : "未定级");
        winLabel.setText(String.valueOf(user.getWinCount()));
        loseLabel.setText(String.valueOf(user.getLoseCount()));
        drawLabel.setText(String.valueOf(user.getDrawCount()));
 
        // 计算并设置总局数
        int total = user.getWinCount() + user.getLoseCount() + user.getDrawCount();
        totalGamesLabel.setText(total + " 场");
 
        // 计算并设置综合胜率
        String winRateStr = user.getWinRate();
        if (winRateStr == null || winRateStr.isEmpty()) {
            if (total > 0) {
                double rate = (double) user.getWinCount() / total * 100;
                winRateStr = String.format("%.1f%%", rate);
            } else {
                winRateStr = "0.0%";
            }
        }
        winRateLabel.setText(winRateStr);
 
        // 设置全服排名
        int rankVal = user.getRank();
        if (rankVal > 0 && rankVal <= 1000) {
            rankLabel.setText("第 " + rankVal + " 名");
        } else {
            rankLabel.setText("暂无排名");
        }

        // 用户信息设置完成后再查询战绩，避免 currentUser 为空时解析异常
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.QUERY_RECORD, "0", "20");
        } else {
            recordData.addAll(
                new GameRecord("玩家A", "胜", "2026-07-04 15:30", "查看"),
                new GameRecord("玩家B", "负", "2026-07-04 16:10", "查看")
            );
        }
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    @FXML
    private void handleChangePwd() {
        String oldPwd = oldPwdField.getText().trim();
        String newPwd = newPwdField.getText().trim();
        errorLabel.setStyle("-fx-text-fill: red;");
        if (oldPwd.isEmpty() || newPwd.isEmpty()) {
            errorLabel.setText("密码不能为空");
            return;
        }
        if (newPwd.length() < 6) {
            errorLabel.setText("新密码长度需 ≥6 位");
            return;
        }
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.CHANGE_PWD, oldPwd, newPwd);
        } else {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("预览模式：密码修改请求已模拟");
        }
    }


    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.PWD_CHANGED.equals(command)) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("密码修改成功");
            oldPwdField.clear();
            newPwdField.clear();
        } else if (Protocol.ERROR.equals(command)) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(args.length > 0 ? args[0] : "操作失败");
        } else if (Protocol.RECORD_LIST.equals(command)) {
            if (currentUser == null) {
                return;
            }
            recordData.clear();
            // 格式：服务端发 11 个字段：recordId|p1Id|p1Name|p2Id|p2Name|winnerId|result|moveCount|duration|createTime|boardData
            int i = 0;
            while (i + 10 <= args.length) {
                // String recordId = args[i];    // 0
                // String p1Id     = args[i + 1]; // 1
                String p1Name   = args[i + 2];   // 2
                // String p2Id     = args[i + 3]; // 3
                String p2Name   = args[i + 4];   // 4
                String winnerId = args[i + 5];   // 5
                // String moveCnt  = args[i + 7]; // 7
                // String duration = args[i + 8]; // 8
                String time     = args[i + 9];   // 9
                String boardData = i + 10 < args.length ? args[i + 10].replace("\\|", "|") : ""; // 10

                // 判断结果：比较 winnerId 与当前用户 ID
                String result;
                if ("0".equals(winnerId)) {
                    result = "平局";
                } else if (String.valueOf(currentUser.getUserId()).equals(winnerId)) {
                    result = "胜";
                } else {
                    result = "负";
                }

                // 对手名：当前用户是 p1 则对手是 p2，反之亦然
                String opponent = p1Name.equals(currentUser.getUsername()) ? p2Name : p1Name;

                recordData.add(new GameRecord(opponent, result, time, "查看", boardData));
                i += 11;  // 每条记录 11 个字段
            }
        }
    }


}