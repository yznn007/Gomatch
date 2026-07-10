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
import javafx.scene.layout.VBox;

public class RoomWaitingController implements NetworkListener {
    @FXML private Label roomTitleLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<String> playerList;
    @FXML private Button startBtn;
    @FXML private TextField chatInput;
    @FXML private TextArea chatArea;

    @FXML private Button menuPlayersBtn;
    @FXML private Button menuChatBtn;
    @FXML private VBox playersPane;
    @FXML private VBox chatPane;

    private final ObservableList<String> players = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;
    private String lastLocalChat;
    private long lastLocalChatTime;

    @FXML
    private void initialize() {
        showPlayersTab();
    }

    private void switchTab(VBox activePane, Button activeBtn) {
        playersPane.setVisible(false);
        playersPane.setManaged(false);
        chatPane.setVisible(false);
        chatPane.setManaged(false);

        activePane.setVisible(true);
        activePane.setManaged(true);

        menuPlayersBtn.getStyleClass().remove("active");
        menuChatBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    @FXML
    private void showPlayersTab() {
        switchTab(playersPane, menuPlayersBtn);
    }

    @FXML
    private void showChatTab() {
        switchTab(chatPane, menuChatBtn);
    }

    /**
     * 修复时序依赖：先调 setPreviewMode(true/false)，再调 setRoomInfo()。
     * 但为健壮性，setRoomInfo 内部检查 previewMode 字段已就绪。
     */
    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setRoomInfo(RoomInfo room, User user) {
        this.currentUser = user;
        roomTitleLabel.setText("房间: " + room.getRoomName() + " (#" + room.getRoomId() + ")");
        if (user != null) {
            players.add(user.getUsername());
        }
        playerList.setItems(players);
        if (user != null && (user.getUsername().equals(room.getOwnerName()) || previewMode)) {
            startBtn.setVisible(true);
        }
        // 非预览模式才注册网络监听（时序已由 ScreenManager.clearListeners + 先 setPreviewMode 保证）
        if (!previewMode) {
            ClientMain.getConnector().addListener(this);
        }
    }

    @FXML
    private void handleStart() {
        if (previewMode) {
            players.add("玩家C（bot）");
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            ctrl.setGameInfo(currentUser.getUsername(), "玩家C（bot）", currentUser.getUsername());
            ctrl.setPreviewMode(true);
            return;
        }
        ClientMain.getConnector().send(Protocol.READY);
    }

    @FXML
    private void handleLeave() {
        if (!previewMode) {
            ClientMain.getConnector().send(Protocol.LEAVE_ROOM);
        }
        // 返回大厅时传递用户信息
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        if (currentUser != null) {
            ctrl.setCurrentUser(currentUser);
        }
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;

        String display = currentUser.getUsername() + ": " + msg + "\n";
        chatArea.appendText(display);
        chatArea.setScrollTop(Double.MAX_VALUE);
        lastLocalChat = display;
        lastLocalChatTime = System.currentTimeMillis();

        if (!previewMode) {
            ClientMain.getConnector().send(Message.of(Protocol.CHAT, "message", msg));
        }
        chatInput.clear();
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.PLAYER_ENTER.equals(cmd)) {
            String username = msg.getString("username");
            players.add(username);
            statusLabel.setText("玩家 " + username + " 已加入");
        } else if (Protocol.PLAYER_LEAVE.equals(cmd)) {
            String username = msg.getString("username");
            players.remove(username);
        } else if (Protocol.CHAT_MSG.equals(cmd)) {
            String username = msg.getString("username");
            String message = msg.getString("message");
            String display = username + ": " + message + "\n";
            if (display.equals(lastLocalChat) &&
                System.currentTimeMillis() - lastLocalChatTime < 2000) {
                return;
            }
            chatArea.appendText(display);
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if (Protocol.GAME_START.equals(cmd)) {
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
        } else if ("UNDO_REQUESTED".equals(cmd)) {
            statusLabel.setText("玩家 " + msg.getString("requesterName") + " 请求悔棋");
        } else if ("UNDO_ACCEPTED".equals(cmd)) {
            statusLabel.setText("对手同意悔棋");
        } else if ("UNDO_REJECTED".equals(cmd)) {
            statusLabel.setText("对手拒绝悔棋");
        } else if ("DRAW_REQUESTED".equals(cmd)) {
            statusLabel.setText("玩家 " + msg.getString("requesterName") + " 请求求和");
        } else if ("DRAW_REJECTED".equals(cmd)) {
            statusLabel.setText("对手拒绝求和");
        }
    }
}
