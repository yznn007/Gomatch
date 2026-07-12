package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.model.GameRecord;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.Base64;

public class ProfileController implements NetworkListener {
    @FXML private Label userIdLabel;
    @FXML private Label usernameLabel;
    @FXML private Label profileAvatarLabel;
    @FXML private ImageView profileAvatarImg;
    @FXML private StackPane profileAvatarPane;
    @FXML private Button uploadAvatarBtn;
    @FXML private Label uploadStatusLabel;
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

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "--";
        int m = seconds / 60;
        int s = seconds % 60;
        if (m > 0) {
            return m + I18N.get("profile.minute") + (s > 0 ? s + I18N.get("profile.second") : "");
        }
        return s + I18N.get("profile.second");
    }

    /**
     * 判定棋谱数据是否可用于回放：
     *  - null / 空串视为无效
     *  - JSON 数组 "[]" 视为无效（无落子）
     *  - 其他任意非空内容视为有效（JSON 数组或老格式字符串）
     * 注意：即使 boardData 为 "[]"（双方未落子就认输），也允许进入回放界面查看空棋盘。
     * 客户端 replayMoves 在 moves 为空时安全地初始化空棋谱列表，不会报错。
     */
    private boolean isReplayDataValid(String movesStr) {
        if (movesStr == null) return false;
        String trimmed = movesStr.trim();
        if (trimmed.isEmpty()) return false;
        return true;
    }

    /**
     * 从记录 JSON 中提取 boardData 棋谱数据，兼容多种存储格式：
     *  - 字符串类型的 JSON 数组，例如 "[{"x":0,...}]"（Redis 缓存路径）
     *  - 字符串类型的 JSON 字符串（双重编码）
     *  - JSONArray 类型（org.json 反序列化结果）
     *  - 老格式 "x,y,color;x,y,color" 或 "x,y,color|x,y,color"
     * 提取失败或为空时返回空字符串，调用方据此判定"暂无棋谱数据"。
     */
    private String extractBoardData(JSONObject r) {
        // 1. 优先尝试作为 JSONArray 直接获取（如果服务端传来的是数组类型）
        JSONArray arr = r.optJSONArray("boardData");
        if (arr != null && arr.length() > 0) {
            return arr.toString();
        }

        // 2. 作为字符串取出（绝大多数情况）
        String raw = r.optString("boardData", "");
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        // 3. 标准化：去掉外层字符串包裹（双重 JSON 编码场景）
        String trimmed = raw.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }

        // 4. 已是 JSON 数组形式：原样返回
        if (trimmed.startsWith("[")) {
            return trimmed;
        }

        // 5. 老格式兼容：管道分隔或分号分隔的 "x,y,color"
        if (trimmed.contains("|") || trimmed.contains(";")) {
            return trimmed.replace("\\|", "|");
        }

        return trimmed;
    }

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
                Label noRecordLabel = new Label(I18N.get("profile.noRecords"));
                noRecordLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-size: 13px; -fx-padding: 20 0;");
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
                String result = record.getResult() != null && !record.getResult().isEmpty() ? record.getResult() : I18N.get("profile.draw");
                Label badgeText = new Label(result.substring(0, 1));
                badgeText.getStyleClass().add("result-badge-text");
                badgeContainer.getChildren().add(badgeText);

                int scoreChange = record.getScoreChange();
                Label scoreChangeLabel = new Label();
                scoreChangeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 32px;");
                if (I18N.get("record.win").equals(result)) {
                    badgeContainer.getStyleClass().add("result-badge-win");
                    scoreChangeLabel.setText("+" + scoreChange);
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");
                } else if (I18N.get("record.lose").equals(result)) {
                    badgeContainer.getStyleClass().add("result-badge-lose");
                    scoreChangeLabel.setText(String.valueOf(scoreChange));
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #ef4444;");
                } else {
                    badgeContainer.getStyleClass().add("result-badge-draw");
                    scoreChangeLabel.setText("+" + scoreChange);
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #94a3b8;");
                }

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                Label opponentLabel = new Label(I18N.get("profile.gameWith", record.getOpponent()));
                opponentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");

                int moveCount = record.getMoveCount();
                int durationSec = record.getDuration();
                String durationStr = formatDuration(durationSec);
                Label detailsLabel = new Label(I18N.get("profile.moves", moveCount > 0 ? String.valueOf(moveCount) : I18N.get("profile.noMoves"), durationStr));
                detailsLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");

                infoBox.getChildren().addAll(opponentLabel, detailsLabel);

                // Right VBox: Time & Action Button
                VBox rightBox = new VBox(4);
                rightBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Label timeLabel = new Label(record.getTime());
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");

                Button replayBtn = new Button(I18N.get("profile.replay"));
                replayBtn.getStyleClass().add("btn-primary");
                replayBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                replayBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                replayBtn.setOnAction(e -> {
                    String movesStr = record.getMoves();
                    if (isReplayDataValid(movesStr)) {
                        BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                        // 传入 player1/2 信息 + 头像 URL，让回放时黑白方卡片显示真实玩家名、积分与头像
                        // 必须用 8 参数版本（带 blackAvatarUrl/whiteAvatarUrl），否则头像会缺失
                        ctrl.setReplayMode(true,
                            record.getPlayer1Id(), record.getPlayer1Name(),
                            record.getPlayer2Id(), record.getPlayer2Name(),
                            record.getPlayer1Score(), record.getPlayer2Score(),
                            record.getPlayer1Avatar(), record.getPlayer2Avatar());
                        ctrl.replayMoves(movesStr);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, I18N.get("profile.noReplayData"));
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
        if (recordData.isEmpty()) {
            loadRecords();
        }
        renderRecordCards();
    }

    /**
     * 统一加载对战记录：
     * - 预览模式 / 未连接服务器：加载本地模拟数据
     * - 已连接服务器：发送 QUERY_RECORD 请求
     */
    private void loadRecords() {
        if (isPreview() || !ClientMain.getConnector().isConnected()) {
            loadMockRecords();
        } else {
            ClientMain.getConnector().send(Message.of(Protocol.QUERY_RECORD, "page", 0, "size", 20));
        }
    }

    private void loadMockRecords() {
        recordData.setAll(
            new GameRecord(I18N.get("casual.preview.playerA"), I18N.get("record.win"), "2026-07-04 15:30", I18N.get("profile.viewReplay"), "0,0,1;1,1,2;2,2,1;3,3,2;4,4,1"),
            new GameRecord(I18N.get("casual.preview.playerB"), I18N.get("record.lose"), "2026-07-04 16:10", I18N.get("profile.viewReplay"), "5,5,1;6,6,2;7,7,1;8,8,2;9,9,1")
        );
    }

    private boolean isPreview() {
        return previewMode || AppConfig.isPreviewMode();
    }

    @FXML
    private void showPwdTab() {
        switchTab(pwdPane, menuPwdBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) {
            return;
        }
        userIdLabel.setText("#" + user.getUserId());
        usernameLabel.setText(user.getUsername());
        
        if (profileAvatarLabel != null && profileAvatarImg != null) {
            String name = user.getUsername();
            String avatarUrl = user.getAvatar();

            // 设置渐变背景，与 Lobby / Ranking 风格统一
            profileAvatarPane.setStyle("-fx-background-color: " + gradientFor(name) + ";"
                    + "-fx-background-radius: 50%;"
                    + "-fx-cursor: hand;");
            // 默认显示首字母（与全站统一），等图片加载完成后再切到真实头像
            profileAvatarImg.setVisible(false);
            profileAvatarLabel.setVisible(true);
            profileAvatarLabel.setText(name == null || name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    Image image = new Image(avatarUrl, true);
                    image.errorProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) {
                            Platform.runLater(() -> {
                                profileAvatarImg.setVisible(false);
                                profileAvatarLabel.setVisible(true);
                                profileAvatarLabel.setText(name == null || name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());
                            });
                        }
                    });
                    image.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                            Platform.runLater(() -> {
                                profileAvatarImg.setImage(image);
                                profileAvatarImg.setVisible(true);
                                profileAvatarLabel.setVisible(false);
                            });
                        }
                    });
                    if (image.isBackgroundLoading() && image.getProgress() >= 1.0 && !image.isError()) {
                        Platform.runLater(() -> {
                            profileAvatarImg.setImage(image);
                            profileAvatarImg.setVisible(true);
                            profileAvatarLabel.setVisible(false);
                        });
                    } else if (!image.isBackgroundLoading()) {
                        if (!image.isError()) {
                            Platform.runLater(() -> {
                                profileAvatarImg.setImage(image);
                                profileAvatarImg.setVisible(true);
                                profileAvatarLabel.setVisible(false);
                            });
                        } else {
                            Platform.runLater(() -> {
                                profileAvatarImg.setVisible(false);
                                profileAvatarLabel.setVisible(true);
                                profileAvatarLabel.setText(name == null || name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());
                            });
                        }
                    }
                } catch (Exception e) {
                    profileAvatarImg.setVisible(false);
                    profileAvatarLabel.setVisible(true);
                    profileAvatarLabel.setText(name == null || name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());
                }
            }
        }
        
        if (uploadAvatarBtn != null) {
            uploadAvatarBtn.setVisible(!isPreview());
            uploadAvatarBtn.setText(user.getAvatar() != null && !user.getAvatar().isEmpty() ? "修改头像" : "上传头像");
        }
        
        scoreLabel.setText(user.getScore() > 0 ? String.valueOf(user.getScore()) : I18N.get("profile.unranked"));
        winLabel.setText(String.valueOf(user.getWinCount()));
        loseLabel.setText(String.valueOf(user.getLoseCount()));
        drawLabel.setText(String.valueOf(user.getDrawCount()));
 
        // 计算并设置总局数
        int total = user.getWinCount() + user.getLoseCount() + user.getDrawCount();
        totalGamesLabel.setText(I18N.get("profile.totalGamesValue", total));
 
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
            rankLabel.setText(I18N.get("profile.rankNo", rankVal));
        } else {
            rankLabel.setText(I18N.get("profile.noRank"));
        }

        // 用户信息设置完成后再查询战绩，避免 currentUser 为空时解析异常
        loadRecords();
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
            errorLabel.setText(I18N.get("profile.pwdEmpty"));
            return;
        }
        if (newPwd.length() < 6) {
            errorLabel.setText(I18N.get("profile.pwdLength"));
            return;
        }
        if (newPwd.equals(oldPwd)) {
            errorLabel.setText(I18N.get("profile.pwdSame"));
            return;
        }
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.CHANGE_PWD, "oldPassword", oldPwd, "newPassword", newPwd));
        } else {
            errorLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");
            errorLabel.setText(I18N.get("profile.preview.changePwd"));
        }
    }


    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleAvatarClick(MouseEvent e) {
        if (currentUser == null) return;
        String url = currentUser.getAvatar();
        String name = currentUser.getUsername();
        showAvatarPreview(url, name);
    }

    @FXML
    private void handleUploadAvatar() {
        if (isPreview() || !ClientMain.getConnector().isConnected()) {
            showUploadStatus(I18N.get("profile.preview.avatar"), "orange");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18N.get("profile.selectAvatar"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(I18N.get("profile.imageFiles"), "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(uploadAvatarBtn.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        // 弹出裁剪对话框，让用户自由裁剪 640×640 头像
        ScreenManager.showDialog("/fxml/CropAvatarUI.fxml",
            (CropAvatarController ctrl) -> {
                ctrl.setSourceFile(selectedFile);
                ctrl.setOnCropComplete(croppedBytes -> {
                    String base64Data = Base64.getEncoder().encodeToString(croppedBytes);
                    ClientMain.getConnector().send(Message.of("UPLOAD_AVATAR", "data", base64Data));
                    showUploadStatus(I18N.get("profile.uploadingAvatar"), "blue");
                });
            }
        );
    }
    
    private void showUploadStatus(String message, String color) {
        if (uploadStatusLabel != null) {
            uploadStatusLabel.setText(message);
            uploadStatusLabel.setStyle("-fx-text-fill: " + color + ";");
        }
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.PWD_CHANGED.equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");
            errorLabel.setText(I18N.get("profile.pwdChanged"));
            oldPwdField.clear();
            newPwdField.clear();
        } else if (Protocol.ERROR.equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + ";");
            errorLabel.setText(msg.has("msg") ? msg.getString("msg") : I18N.get("profile.operationFailed"));
        } else if ("AVATAR_UPLOADED".equals(cmd)) {
            String url = msg.getString("url");
            String message = msg.getString("msg");
            if (url != null && !url.isEmpty()) {
                showUploadStatus(I18N.get("profile.avatarUploaded"), "green");
                if (currentUser != null) {
                    currentUser.setAvatar(url);
                    setCurrentUser(currentUser);
                }
            } else {
                showUploadStatus(message != null ? message : I18N.get("profile.avatarUploadFailed"), "red");
            }
        } else if ("AVATAR_DATA".equals(cmd)) {
            String url = msg.getString("url");
            if (currentUser != null && url != null) {
                currentUser.setAvatar(url);
                setCurrentUser(currentUser);
            }
        } else if (Protocol.RECORD_LIST.equals(cmd)) {
            if (currentUser == null) {
                return;
            }
            try {
                recordData.clear();
                if (msg.getData() == null) return;
                JSONArray records = msg.getData().optJSONArray("records");
                if (records == null) return;
                for (int i = 0; i < records.length(); i++) {
                    JSONObject r = records.getJSONObject(i);
                    String p1Name = r.optString("player1Name", I18N.get("profile.unknown"));
                    String p2Name = r.optString("player2Name", I18N.get("profile.unknown"));
                    String winnerId = String.valueOf(r.optLong("winnerId", 0));
                    String time = r.optString("createTime", "");
                    // 兼容多种格式：JSON 数组字符串（Redis 缓存）、老格式的分号/管道分隔、JSON 字符串
                    String boardData = extractBoardData(r);
                    int moveCount = r.optInt("moveCount", 0);
                    int duration = r.optInt("duration", 0);
                    int scoreChange = r.optInt("scoreChange", 0);
                    int player1Score = r.optInt("player1Score", -1);
                    int player2Score = r.optInt("player2Score", -1);

                    // 判断结果：比较 winnerId 与当前用户 ID
                    String result;
                    if ("0".equals(winnerId)) {
                        result = I18N.get("record.draw");
                    } else if (String.valueOf(currentUser.getUserId()).equals(winnerId)) {
                        result = I18N.get("record.win");
                    } else {
                        result = I18N.get("record.lose");
                    }

                    // 对手名：当前用户是 p1 则对手是 p2，反之亦然
                    String currentName = currentUser.getUsername();
                    String opponent = currentName != null && currentName.equals(p1Name) ? p2Name : p1Name;

                    GameRecord gr = new GameRecord(opponent, result, time, I18N.get("profile.viewReplay"), boardData);
                    gr.setMoveCount(moveCount);
                    gr.setDuration(duration);
                    gr.setScoreChange(scoreChange);
                    // 保存 player1/2 信息，用于回放时正确显示黑白方名字和积分
                    gr.setPlayer1Id(String.valueOf(r.optLong("player1Id", 0)));
                    gr.setPlayer1Name(p1Name);
                    gr.setPlayer1Score(player1Score);
                    // 保存头像 URL，用于回放页面显示双方头像（从服务端 record 中读取）
                    gr.setPlayer1Avatar(r.optString("player1Avatar", ""));
                    gr.setPlayer2Id(String.valueOf(r.optLong("player2Id", 0)));
                    gr.setPlayer2Name(p2Name);
                    gr.setPlayer2Score(player2Score);
                    gr.setPlayer2Avatar(r.optString("player2Avatar", ""));
                    recordData.add(gr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAvatarPreview(String avatarUrl, String username) {
        ScreenManager.showDialog("/fxml/AvatarPreviewUI.fxml",
            (AvatarPreviewController ctrl) -> {
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Image image = new Image(avatarUrl, true);
                    ctrl.setAvatar(image, username);
                } else {
                    ctrl.setAvatar(null, username);
                }
            }
        );
    }

    /** username -> 渐变色（hash 一致）。与 Lobby / Ranking 风格统一。 */
    private String gradientFor(String username) {
        if (username == null || username.isEmpty()) return "linear-gradient(from 0% 0% to 100% 100%, #60a5fa, #1e293b)";
        String[] palette = {
            "linear-gradient(from 0% 0% to 100% 100%, #60a5fa, #1e293b)",
            "linear-gradient(from 0% 0% to 100% 100%, #fbbf24, #b45309)",
            "linear-gradient(from 0% 0% to 100% 100%, #34d399, #065f46)",
            "linear-gradient(from 0% 0% to 100% 100%, #f472b6, #9d174d)",
            "linear-gradient(from 0% 0% to 100% 100%, #a78bfa, #5b21b6)",
            "linear-gradient(from 0% 0% to 100% 100%, #22d3ee, #155e75)",
            "linear-gradient(from 0% 0% to 100% 100%, #f87171, #991b1b)"
        };
        return palette[Math.abs(username.hashCode()) % palette.length];
    }

}
