package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.RoomInfo;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

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
        ClientMain.getConnector().send(Message.of(Protocol.SPECTATE, "roomId", roomId));
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
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.RANKING_LIST.equals(cmd)) {
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
                room.setHasPassword("1".equals(r.getString("hasPassword")));
                roomData.add(room);
            }
            spectateData.setAll(roomData.stream()
                .filter(r -> "PLAYING".equalsIgnoreCase(r.getStatus()))
                .map(r -> "#" + r.getRoomId() + " " + r.getRoomName() + "  玩家: " + r.getPlayerCount() + "/2")
                .collect(Collectors.toList()));
        } else if (Protocol.JOIN_OK.equals(cmd)) {
            setStatus("", false);
            RoomInfo room = new RoomInfo();
            room.setRoomId(msg.getString("roomId"));
            room.setRoomName(msg.getString("roomName"));
            String ps = msg.getString("players");
            if (!ps.isEmpty()) {
                String[] pc = ps.split("/");
                try { room.setPlayerCount(Integer.parseInt(pc[0])); } catch (Exception ignored) {}
            }
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setPreviewMode(false);
            ctrl.setRoomInfo(room, currentUser != null ? currentUser : new com.gomatch.client.model.User());
        } else if (Protocol.JOIN_FAIL.equals(cmd)) {
            showAlert("加入失败: " + msg.getString("msg"));
        } else if (Protocol.GAME_START.equals(cmd)) {
            setStatus("", false);
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            ctrl.setGameInfo(msg.getString("blackUserId"), msg.getString("whiteUserId"), currentUser != null ? currentUser.getUsername() : "unknown");
        } else if (Protocol.ERROR.equals(cmd)) {
            String errMsg = msg.getString("msg");
            if (errMsg.contains("等待对手")) {
                int idx = errMsg.lastIndexOf('(');
                String suffix = idx >= 0 ? " " + errMsg.substring(idx) : "";
                setStatus("等待对手中..." + suffix, true);
            } else {
                setStatus("", false);
                showAlert(errMsg);
            }
        }
    }
}