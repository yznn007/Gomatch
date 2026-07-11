package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.RoomInfo;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class CasualLobbyController implements NetworkListener {
    @FXML private VBox roomsContainer;
    @FXML private VBox spectateContainer;

    @FXML private Button menuRoomsBtn;
    @FXML private Button menuSpectateBtn;
    @FXML private VBox roomsPane;
    @FXML private VBox spectatePane;
    @FXML private Label statusLabel;
    @FXML private Button quickMatchBtn;

    private final ObservableList<RoomInfo> roomData    = FXCollections.observableArrayList();
    private final ObservableList<String>   spectateData = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;
    private boolean isMatching = false;
    private boolean joinedGame = false;
    private int matchCountdown = 0;
    private Timeline matchCountdownTimer;

    @FXML
    private void initialize() {
        roomData.addListener((javafx.collections.ListChangeListener<RoomInfo>) c -> renderRoomCards());
        spectateData.addListener((javafx.collections.ListChangeListener<String>) c -> renderSpectateCards());
        ClientMain.getConnector().addListener(this);
        if (!ClientMain.getConnector().isConnected()) {
            roomData.addAll(
                new RoomInfo("1001", "五子棋切磋", "玩家A", 2, "对局中"),
                new RoomInfo("1002", "休闲一局",  "玩家B", 1, "等待中")
            );
            spectateData.addAll("#1001 五子棋切磋  玩家A vs 玩家D");
        } else {
            ClientMain.getConnector().send(Protocol.ROOM_LIST);
        }

        showRoomsTab();
    }

    private void switchTab(VBox activePane, Button activeBtn) {
        roomsPane.setVisible(false);
        roomsPane.setManaged(false);
        spectatePane.setVisible(false);
        spectatePane.setManaged(false);

        activePane.setVisible(true);
        activePane.setManaged(true);

        menuRoomsBtn.getStyleClass().remove("active");
        menuSpectateBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    @FXML
    private void showRoomsTab() {
        switchTab(roomsPane, menuRoomsBtn);
        handleRefresh();
    }

    @FXML
    private void showSpectateTab() {
        switchTab(spectatePane, menuSpectateBtn);
        handleRefresh();
    }



    public void setPreviewMode(boolean previewMode) { this.previewMode = previewMode; }

    public void setCurrentUser(User user) { this.currentUser = user; }

    @FXML
    private void handleCreateRoom() {
        if (previewMode) {
            RoomInfo fakeRoom = new RoomInfo("9999", "预览房间", currentUser.getUsername(), 1, "等待中");
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setPreviewMode(true);
            ctrl.setRoomInfo(fakeRoom, currentUser);
            return;
        }
        CreateRoomController ctrl = ScreenManager.switchTo("/fxml/CreateRoomUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    // handleJoinRoom 已被内嵌卡片按钮取代

    private void joinRoom(RoomInfo room) {
        if (previewMode) {
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setPreviewMode(true);
            ctrl.setRoomInfo(room, currentUser);
            return;
        }
        if (room.isHasPassword()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("密码验证");
            dialog.setHeaderText("该房间需要密码");
            dialog.setContentText("请输入房间密码:");
            ScreenManager.showStyledDialog(dialog).ifPresent(password ->
                ClientMain.getConnector().send(Message.of(Protocol.JOIN_ROOM, "roomId", room.getRoomId(), "password", password.trim()))
            );
        } else {
            ClientMain.getConnector().send(Message.of(Protocol.JOIN_ROOM, "roomId", room.getRoomId(), "password", ""));
        }
    }

    @FXML
    private void handleQuickMatch() {
        if (previewMode) {
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            ctrl.setGameInfo(currentUser.getUsername(), "对手（bot）", currentUser.getUsername());
            ctrl.setPreviewMode(true);
            return;
        }
        if (isMatching) {
            cancelMatch();
        } else {
            startMatch();
        }
    }

    private void startMatch() {
        ClientMain.getConnector().send(Protocol.QUICK_MATCH);
        isMatching = true;
        matchCountdown = 0;
        updateMatchStatus();
        startCountdown();
        Platform.runLater(() -> {
            if (quickMatchBtn != null) {
                quickMatchBtn.setText("取消匹配");
                quickMatchBtn.getStyleClass().add("btn-danger");
            }
        });
    }

    private void cancelMatch() {
        ClientMain.getConnector().send(Protocol.CANCEL_MATCH);
        stopMatching();
    }

    private void stopMatching() {
        isMatching = false;
        if (matchCountdownTimer != null) {
            matchCountdownTimer.stop();
            matchCountdownTimer = null;
        }
        setStatus("", false);
        Platform.runLater(() -> {
            if (quickMatchBtn != null) {
                quickMatchBtn.setText("快速匹配");
                quickMatchBtn.getStyleClass().remove("btn-danger");
                quickMatchBtn.getStyleClass().remove("btn-success");
            }
        });
    }

    private void startCountdown() {
        if (matchCountdownTimer != null) {
            matchCountdownTimer.stop();
        }
        matchCountdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            matchCountdown++;
            updateMatchStatus();
        }));
        matchCountdownTimer.setCycleCount(Timeline.INDEFINITE);
        matchCountdownTimer.play();
    }

    private void updateMatchStatus() {
        setStatus("正在匹配中... 已等待: " + matchCountdown + "秒 (点击'取消匹配'取消)", true);
    }

    // handleSpectate 已被内嵌卡片按钮取代

    @FXML
    private void handleBack() {
        if (isMatching) {
            ClientMain.getConnector().send(Protocol.CANCEL_MATCH);
            stopMatching();
        }
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        if (currentUser != null) {
            ctrl.setCurrentUser(currentUser);
        }
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleRefresh() {
        setStatus("", false);
        if (previewMode) {
            roomData.clear();
            roomData.addAll(
                new RoomInfo("1001", "五子棋切磋", "玩家A", 2, "对局中"),
                new RoomInfo("1002", "休闲一局",  "玩家B", 1, "等待中")
            );
            spectateData.clear();
            spectateData.addAll("#1001 五子棋切磋  玩家A vs 玩家D");
        } else {
            ClientMain.getConnector().send(Protocol.ROOM_LIST);
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        ScreenManager.showStyledAlert(alert);
    }

    private void setStatus(String text, boolean visible) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setVisible(visible);
            statusLabel.setManaged(visible);
        }
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.ROOM_LIST.equals(cmd)) {
            // 服务端返回的 JSON 数组
            org.json.JSONArray rooms = msg.getData().optJSONArray("rooms");
            if (rooms == null) return;
            roomData.clear();
            for (int i = 0; i < rooms.length(); i++) {
                org.json.JSONObject r = rooms.getJSONObject(i);
                RoomInfo room = new RoomInfo();
                room.setRoomId(r.getString("roomId"));
                room.setRoomName(r.getString("roomName"));
                room.setStatus(r.getString("status"));
                room.setPlayerCount(Integer.parseInt(r.getString("players").split("/")[0]));
                room.setHasPassword(r.optBoolean("hasPassword", false));
                room.setOwnerName(r.optString("ownerName", "未知"));
                roomData.add(room);
            }
            spectateData.setAll(roomData.stream()
                .filter(r -> "PLAYING".equalsIgnoreCase(r.getStatus()))
                .map(r -> "#" + r.getRoomId() + " " + r.getRoomName() + "  玩家: " + r.getPlayerCount() + "/2")
                .collect(Collectors.toList()));
        } else if (Protocol.JOIN_OK.equals(cmd)) {
            // 如果已在 GAME_START 中进入了对局（快速匹配），忽略 JOIN_OK
            if (joinedGame) return;
            stopMatching();
            setStatus("", false);
            RoomInfo room = new RoomInfo();
            room.setRoomId(msg.getString("roomId"));
            room.setRoomName(msg.getString("roomName"));
            String ps = msg.getString("players");
            if (!ps.isEmpty()) {
                String[] pc = ps.split("/");
                try { room.setPlayerCount(Integer.parseInt(pc[0])); } catch (Exception ignored) {}
            }
            User u = currentUser != null ? currentUser : new com.gomatch.client.model.User();
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setPreviewMode(false);
            ctrl.setRoomInfo(room, u);
            // 加入者：补上已在房间中的对手名字
            String myName = u != null ? u.getUsername() : "";
            if (msg.has("blackUsername")) {
                String black = msg.getString("blackUsername");
                if (!black.isEmpty() && !black.equals(myName)) {
                    ctrl.addOpponentName(black);
                }
            }
            if (msg.has("whiteUsername")) {
                String white = msg.getString("whiteUsername");
                if (!white.isEmpty() && !white.equals(myName)) {
                    ctrl.addOpponentName(white);
                }
            }
        } else if (Protocol.JOIN_FAIL.equals(cmd)) {
            showAlert("加入失败: " + msg.getString("msg"));
        } else if (Protocol.GAME_START.equals(cmd)) {
            stopMatching();
            setStatus("", false);
            // 标记已进入游戏，防止后续 JOIN_OK 切回等待室
            joinedGame = true;
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            String bId = String.valueOf(msg.getLong("blackUserId"));
            String wId = String.valueOf(msg.getLong("whiteUserId"));
            String bName = msg.has("blackUsername") ? msg.getString("blackUsername") : bId;
            String wName = msg.has("whiteUsername") ? msg.getString("whiteUsername") : wId;
            int bScore = msg.has("blackScore") ? msg.getInt("blackScore") : 0;
            int wScore = msg.has("whiteScore") ? msg.getInt("whiteScore") : 0;
            String username = currentUser != null ? currentUser.getUsername() : "unknown";
            ctrl.setGameInfo(bId, bName, wId, wName, username, bScore, wScore);
            String bAvatar = msg.has("blackAvatarUrl") ? msg.getString("blackAvatarUrl") : "";
            String wAvatar = msg.has("whiteAvatarUrl") ? msg.getString("whiteAvatarUrl") : "";
            ctrl.setPlayerAvatars(bAvatar, wAvatar);
        } else if (Protocol.QUICK_MATCH_FOUND.equals(cmd)) {
            String opponent = msg.getString("opponentUsername");
            stopMatching();
            Platform.runLater(() -> {
                QuickMatchReadyController ctrl = ScreenManager.switchTo("/fxml/QuickMatchReadyUI.fxml");
                ctrl.setCurrentUser(currentUser);
                ctrl.setPreviewMode(previewMode);
                ctrl.setOpponent(opponent);
                ClientMain.getConnector().addListener(ctrl);
            });
        } else if (Protocol.MATCH_TIMEOUT.equals(cmd)) {
            stopMatching();
            setStatus("匹配超时，请重试", true);
        } else if (Protocol.CANCEL_MATCH.equals(cmd)) {
            stopMatching();
            setStatus("已取消匹配", true);
        } else if (Protocol.ERROR.equals(cmd)) {
            String errMsg = msg.getString("msg");
            if (errMsg.contains("等待对手")) {
                if (isMatching) {
                    // 已在本地点动倒计时，忽略服务端等待消息
                    return;
                }
                int idx = errMsg.lastIndexOf('(');
                String suffix = idx >= 0 ? " " + errMsg.substring(idx) : "";
                setStatus("等待对手中..." + suffix, true);
            } else {
                setStatus("", false);
                showAlert(errMsg);
            }
        }
    }

    private void renderRoomCards() {
        if (roomsContainer == null) return;
        javafx.application.Platform.runLater(() -> {
            roomsContainer.getChildren().clear();
            if (roomData.isEmpty()) {
                Label noRoomLabel = new Label("暂无活动房间，您可以创建一个！");
                noRoomLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-size: 13px; -fx-padding: 20 0;");
                roomsContainer.getChildren().add(noRoomLabel);
                return;
            }

            for (RoomInfo room : roomData) {
                // Card Root HBox
                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Double click handler
                card.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        joinRoom(room);
                    }
                });

                // Avatar Circle
                javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
                avatar.getStyleClass().add("avatar-badge");
                String hostName = room.getOwnerName() != null ? room.getOwnerName() : "未知";
                String charVal = hostName.isEmpty() ? "R" : hostName.substring(0, 1).toUpperCase();
                Label avatarText = new Label(charVal);
                avatarText.getStyleClass().add("avatar-badge-text");
                avatar.getChildren().add(avatarText);

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                HBox nameRow = new HBox(8);
                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label nameLabel = new Label(room.getRoomName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");
                Label idLabel = new Label("#" + room.getRoomId());
                idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");
                nameRow.getChildren().addAll(nameLabel, idLabel);

                if (room.isHasPassword()) {
                    Label lockIcon = new Label("[密]");
                    lockIcon.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    lockIcon.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    nameRow.getChildren().add(lockIcon);
                }

                Label ownerLabel = new Label("房主: " + hostName);
                ownerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ScreenManager.themeColor("#475569", "#cbd5e1") + ";");
                infoBox.getChildren().addAll(nameRow, ownerLabel);

                // Slots HBox (human figures)
                HBox slots = new HBox(4);
                slots.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                int pc = room.getPlayerCount();
                Label slotLabel = new Label("人数: " + pc + "/2");
                slotLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                slotLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + ScreenManager.themeColor("#475569", "#cbd5e1") + "; -fx-font-weight: bold;");
                slots.getChildren().add(slotLabel);

                // Status Badge
                Label statusTag = new Label();
                statusTag.getStyleClass().add("tag-badge");
                if ("对局中".equals(room.getStatus()) || "playing".equalsIgnoreCase(room.getStatus())) {
                    statusTag.setText("对局中");
                    statusTag.getStyleClass().add("tag-badge-grey");
                } else {
                    statusTag.setText("等待中");
                    statusTag.getStyleClass().add("tag-badge-green");
                }

                // Join Button
                Button actionBtn = new Button("加入");
                actionBtn.getStyleClass().add("btn-primary");
                actionBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                actionBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                actionBtn.setOnAction(e -> joinRoom(room));

                // Add to card
                card.getChildren().addAll(avatar, infoBox, slots, statusTag, actionBtn);
                roomsContainer.getChildren().add(card);
            }
        });
    }

    private void renderSpectateCards() {
        if (spectateContainer == null) return;
        javafx.application.Platform.runLater(() -> {
            spectateContainer.getChildren().clear();
            if (spectateData.isEmpty()) {
                Label noSpectateLabel = new Label("当前暂无进行中的精彩对局。");
                noSpectateLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-size: 13px; -fx-padding: 20 0;");
                spectateContainer.getChildren().add(noSpectateLabel);
                return;
            }

            for (String specItem : spectateData) {
                // Card Root HBox
                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Parse spectate string
                String roomId = "";
                String roomName = "对局";
                String matchDetails = "";
                try {
                    int firstSpace = specItem.indexOf(' ');
                    if (firstSpace > 0) {
                        roomId = specItem.substring(1, firstSpace);
                        int secondPart = specItem.indexOf("  ", firstSpace);
                        if (secondPart > 0) {
                            roomName = specItem.substring(firstSpace + 1, secondPart);
                            matchDetails = specItem.substring(secondPart + 2);
                        } else {
                            roomName = specItem.substring(firstSpace + 1);
                        }
                    }
                } catch (Exception ignored) {}

                // Avatar Icon (Eye 👁️) —— 固定 48x48 圆形，避免在 HBox 里被拉伸成椭圆
                javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
                avatar.getStyleClass().add("avatar-badge");
                avatar.setPrefSize(48, 48);
                avatar.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
                avatar.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
                avatar.setStyle("-fx-background-color: " + ScreenManager.themeColor("rgba(148,163,184,0.45)", "rgba(71,85,105,0.65)") + ";"); // 观战头像用板岩灰系
                Label avatarText = new Label("观");
                avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                avatarText.getStyleClass().add("avatar-badge-text");
                avatar.getChildren().add(avatarText);

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                Label titleLabel = new Label(roomName);
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");
                
                Label detailLabel = new Label(matchDetails.isEmpty() ? "对局编号: #" + roomId : matchDetails);
                detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");
                infoBox.getChildren().addAll(titleLabel, detailLabel);

                // Spectate Button
                Button actionBtn = new Button("观战");
                actionBtn.getStyleClass().add("btn-primary");
                actionBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                actionBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                final String finalRoomId = roomId;
                actionBtn.setOnAction(e -> {
                    if (previewMode) {
                        showAlert("预览模式：观战功能需要服务器");
                        return;
                    }
                    ClientMain.getConnector().send(Message.of(Protocol.SPECTATE, "roomId", finalRoomId));
                });

                card.getChildren().addAll(avatar, infoBox, actionBtn);
                spectateContainer.getChildren().add(card);
            }
        });
    }
}
