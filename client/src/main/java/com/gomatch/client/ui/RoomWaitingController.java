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
    @FXML private Button readyBtn;
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
    private boolean isReady;
    private boolean blackReady;
    private boolean whiteReady;

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
        // 预览模式下房主可见开始按钮；正式模式下始终显示准备按钮（等待对手加入后才能开始）
        if (previewMode) {
            readyBtn.setVisible(true);
            readyBtn.setText("%room.startGame");
        } else {
            readyBtn.setVisible(true); // 始终显示准备按钮
            readyBtn.setDisable(players.size() < 2); // 人数不足时禁用
        }
        updateReadyStatusLabel();
        // 非预览模式才注册网络监听
        if (!previewMode) {
            ClientMain.getConnector().addListener(this);
        }
    }

    /** 添加对手用户名到列表中（用于加入者收到 JOIN_OK 后） */
    public void addOpponentName(String opponentName) {
        if (opponentName != null && !opponentName.isEmpty()) {
            String displayName = getDisplayName(opponentName, false);
            if (!players.contains(displayName)) {
                players.add(0, displayName); // 对手在前（黑方在先）
                readyBtn.setVisible(true);
                readyBtn.setDisable(false); // 对手加入后启用准备按钮
            }
        }
    }

    /** 获取带准备状态的显示名称 */
    private String getDisplayName(String username, boolean isReady) {
        return username + (isReady ? " ✓" : "");
    }

    /** 更新玩家列表的准备状态显示 */
    private void updatePlayerListReadyStatus() {
        if (players.size() >= 2) {
            String blackName = players.get(0).replace(" ✓", "");
            String whiteName = players.get(1).replace(" ✓", "");
            players.set(0, getDisplayName(blackName, blackReady));
            players.set(1, getDisplayName(whiteName, whiteReady));
        } else if (players.size() == 1) {
            String name = players.get(0).replace(" ✓", "");
            players.set(0, getDisplayName(name, blackReady));
        }
    }

    @FXML
    private void handleReady() {
        if (previewMode) {
            players.add("玩家C（bot）");
            BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
            ctrl.setGameInfo(currentUser.getUsername(), "玩家C（bot）", currentUser.getUsername());
            ctrl.setPreviewMode(true);
            return;
        }
        isReady = !isReady;
        ClientMain.getConnector().send(Message.of(Protocol.READY, "ready", String.valueOf(isReady)));
        updateReadyButtonText();
        // 立即更新本地状态显示
        if (currentUser != null && players.size() >= 2) {
            String myName = currentUser.getUsername();
            if (myName.equals(players.get(0).replace(" ✓", ""))) {
                blackReady = isReady;
            } else if (myName.equals(players.get(1).replace(" ✓", ""))) {
                whiteReady = isReady;
            }
            updateReadyStatusLabel();
            updatePlayerListReadyStatus();
        }
    }

    private void updateReadyButtonText() {
        if (isReady) {
            readyBtn.setText("取消准备");
            readyBtn.getStyleClass().remove("btn-primary");
            readyBtn.getStyleClass().add("btn-secondary");
        } else {
            readyBtn.setText("准备");
            readyBtn.getStyleClass().remove("btn-secondary");
            readyBtn.getStyleClass().add("btn-primary");
        }
    }

    private void updateReadyStatusLabel() {
        if (players.size() < 2) {
            statusLabel.setText("等待其他玩家加入...");
            return;
        }
        String blackName = players.size() > 0 ? players.get(0) : "黑方";
        String whiteName = players.size() > 1 ? players.get(1) : "白方";
        String blackStatus = blackReady ? "✓" : "⏳";
        String whiteStatus = whiteReady ? "✓" : "⏳";
        statusLabel.setText(String.format("黑方 %s %s  |  白方 %s %s", blackName, blackStatus, whiteName, whiteStatus));
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
            String displayName = getDisplayName(username, false);
            if (!players.contains(displayName)) {
                players.add(displayName);
            }
            // 第二人加入后启用准备按钮
            if (players.size() >= 2) {
                readyBtn.setVisible(true);
                readyBtn.setDisable(false);
            }
            updateReadyStatusLabel();
        } else if (Protocol.PLAYER_LEAVE.equals(cmd)) {
            String username = msg.getString("username");
            // 移除可能带准备状态标记的玩家名称
            players.remove(username);
            players.remove(getDisplayName(username, true));
            isReady = false;
            blackReady = false;
            whiteReady = false;
            updateReadyButtonText();
            updateReadyStatusLabel();
            updatePlayerListReadyStatus();
            // 人数不足时禁用准备按钮
            if (players.size() < 2) {
                readyBtn.setDisable(true);
            }
        } else if (Protocol.READY.equals(cmd)) {
            blackReady = "true".equals(msg.getString("blackReady"));
            whiteReady = "true".equals(msg.getString("whiteReady"));
            // 同步自己的准备状态（防止服务端状态与本地不一致）
            if (currentUser != null) {
                String myName = currentUser.getUsername();
                if (players.size() >= 1 && myName.equals(players.get(0).replace(" ✓", ""))) {
                    isReady = blackReady;
                } else if (players.size() >= 2 && myName.equals(players.get(1).replace(" ✓", ""))) {
                    isReady = whiteReady;
                }
            }
            updateReadyButtonText();
            updateReadyStatusLabel();
            updatePlayerListReadyStatus();
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
