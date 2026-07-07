package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.RoomInfo;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class CasualLobbyController implements NetworkListener {
    @FXML private TableView<RoomInfo> roomTable;
    @FXML private TableColumn<RoomInfo, String>  roomIdCol;
    @FXML private TableColumn<RoomInfo, String>  roomNameCol;
    @FXML private TableColumn<RoomInfo, String>  ownerCol;
    @FXML private TableColumn<RoomInfo, Integer> playerCountCol;
    @FXML private TableColumn<RoomInfo, String>  statusCol;
    @FXML private ListView<String> spectateList;

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
        roomIdCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        roomNameCol.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        ownerCol.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        playerCountCol.setCellValueFactory(new PropertyValueFactory<>("playerCount"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        roomTable.setItems(roomData);
        roomTable.setRowFactory(tv -> {
            TableRow<RoomInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    joinRoom(row.getItem());
                }
            });
            return row;
        });
        spectateList.setItems(spectateData);
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

    @FXML
    private void handleJoinRoom() {
        RoomInfo selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("请先选择一个房间");
            return;
        }
        joinRoom(selected);
    }

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

    @FXML
    private void handleSpectate() {
        String selected = spectateList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(previewMode ? "预览模式：观战" : "请先选择一个对局");
            return;
        }
        if (previewMode) {
            showAlert("预览模式：观战功能需要服务器");
            return;
        }
        String roomId = selected.split(" ")[0].replace("#", "");
        ClientMain.getConnector().send(Protocol.SPECTATE, roomId);
    }

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
        alert.showAndWait();
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
                .filter(r -> "playing".equals(r.getStatus()))
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
            ctrl.setGameInfo(args[0], args[1], currentUser != null ? currentUser.getUsername() : "unknown");
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
}