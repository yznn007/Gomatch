package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.RoomInfo;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.stream.Collectors;

public class CasualLobbyController implements NetworkListener {
    @FXML private VBox roomsContainer;
    @FXML private VBox spectateContainer;

    @FXML private Button menuRoomsBtn;
    @FXML private Button menuSpectateBtn;
    @FXML private VBox roomsPane;
    @FXML private VBox spectatePane;
    @FXML private Label statusLabel;

    private final ObservableList<RoomInfo> roomData    = FXCollections.observableArrayList();
    private final ObservableList<String>   spectateData = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;

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
            dialog.showAndWait().ifPresent(password ->
                ClientMain.getConnector().send(Protocol.JOIN_ROOM, room.getRoomId(), password.trim())
            );
        } else {
            ClientMain.getConnector().send(Protocol.JOIN_ROOM, room.getRoomId(), "");
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
        ClientMain.getConnector().send(Protocol.QUICK_MATCH);
        setStatus("正在匹配中，请稍候...", true);
    }

    // handleSpectate 已被内嵌卡片按钮取代

    @FXML
    private void handleBack() {
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
    public void onMessage(String command, String[] args) {
        if (Protocol.RANKING_LIST.equals(command)) {
            roomData.clear();
            for (int i = 0; i < args.length; i += 6) {
                if (i + 5 < args.length) {
                    RoomInfo room = new RoomInfo();
                    room.setRoomId(args[i]);
                    room.setRoomName(args[i + 1]);
                    room.setStatus(args[i + 2]);
                    String[] players = args[i + 3].split("/");
                    room.setPlayerCount(Integer.parseInt(players[0]));
                    // args[i+4] is max (always "2"), skip
                    room.setHasPassword("1".equals(args[i + 5]));
                    roomData.add(room);
                }
            }
            spectateData.setAll(roomData.stream()
                .filter(r -> "PLAYING".equalsIgnoreCase(r.getStatus()))
                .map(r -> "#" + r.getRoomId() + " " + r.getRoomName() + "  玩家: " + r.getPlayerCount() + "/2")
                .collect(Collectors.toList()));
        } else if (Protocol.JOIN_OK.equals(command)) {
            setStatus("", false);
            RoomInfo room = new RoomInfo();
            if (args.length > 0) room.setRoomId(args[0]);
            if (args.length > 1) room.setRoomName(args[1]);
            if (args.length > 2) {
                String[] pc = args[2].split("/");
                try {
                    room.setPlayerCount(Integer.parseInt(pc[0]));
                } catch (Exception ignored) {}
            }
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setPreviewMode(false);
            ctrl.setRoomInfo(room, currentUser != null ? currentUser : new com.gomatch.client.model.User());
        } else if (Protocol.JOIN_FAIL.equals(command)) {
            showAlert("加入失败: " + args[0]);
        } else if (Protocol.GAME_START.equals(command)) {
            setStatus("", false);
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            String username = currentUser != null ? currentUser.getUsername() : "unknown";
            if (args.length >= 4) {
                ctrl.setGameInfo(args[0], args[1], args[2], args[3], username);
            } else {
                ctrl.setGameInfo(args[0], args[1], username);
            }
        } else if (Protocol.ERROR.equals(command)) {
            String msg = args.length > 0 ? args[0] : "未知错误";
            if (msg.contains("等待对手")) {
                int idx = msg.lastIndexOf('(');
                String suffix = idx >= 0 ? " " + msg.substring(idx) : "";
                setStatus("等待对手中..." + suffix, true);
            } else {
                setStatus("", false);
                showAlert(msg);
            }
        }
    }

    private void renderRoomCards() {
        if (roomsContainer == null) return;
        javafx.application.Platform.runLater(() -> {
            roomsContainer.getChildren().clear();
            if (roomData.isEmpty()) {
                Label noRoomLabel = new Label("暂无活动房间，您可以创建一个！");
                noRoomLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 0;");
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
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                Label idLabel = new Label("#" + room.getRoomId());
                idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                nameRow.getChildren().addAll(nameLabel, idLabel);

                if (room.isHasPassword()) {
                    Label lockIcon = new Label("[密]");
                    lockIcon.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    lockIcon.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    nameRow.getChildren().add(lockIcon);
                }

                Label ownerLabel = new Label("房主: " + hostName);
                ownerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                infoBox.getChildren().addAll(nameRow, ownerLabel);

                // Slots HBox (human figures)
                HBox slots = new HBox(4);
                slots.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                int pc = room.getPlayerCount();
                Label slotLabel = new Label("人数: " + pc + "/2");
                slotLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                slotLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-weight: bold;");
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
                noSpectateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 0;");
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

                // Avatar Icon (Eye 👁️)
                javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
                avatar.getStyleClass().add("avatar-badge");
                avatar.setStyle("-fx-background-color: #a78bfa;"); // Purple for spectating
                Label avatarText = new Label("观");
                avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                avatarText.getStyleClass().add("avatar-badge-text");
                avatar.getChildren().add(avatarText);

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                Label titleLabel = new Label(roomName);
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                
                Label detailLabel = new Label(matchDetails.isEmpty() ? "对局编号: #" + roomId : matchDetails);
                detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
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
                    ClientMain.getConnector().send(Protocol.SPECTATE, finalRoomId);
                });

                card.getChildren().addAll(avatar, infoBox, actionBtn);
                spectateContainer.getChildren().add(card);
            }
        });
    }
}