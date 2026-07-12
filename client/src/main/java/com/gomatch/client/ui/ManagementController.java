package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

public class ManagementController implements NetworkListener {
    @FXML private Button menuSavesBtn;
    @FXML private Button menuChatBtn;
    @FXML private Button menuWatchBtn;
    @FXML private Button menuAdminBtn;
    @FXML private VBox savesPane;
    @FXML private VBox chatPane;
    @FXML private VBox watchPane;
    @FXML private VBox adminPane;
    @FXML private ListView<String> savesList;
    @FXML private ListView<String> chatHistoryList;
    @FXML private ListView<String> watchHistoryList;
    @FXML private ListView<String> adminUsersList;
    @FXML private TextField saveNameField;
    @FXML private TextField chatRoomIdField;
    @FXML private TextField adminKeywordField;
    @FXML private TextField adminUserIdField;
    @FXML private TextField adminRoleField;
    @FXML private TextField adminRoomIdField;
    @FXML private Label statusLabel;
    @FXML private Label adminStatsLabel;

    private final ObservableList<String> saves = FXCollections.observableArrayList();
    private final ObservableList<String> chats = FXCollections.observableArrayList();
    private final ObservableList<String> watchers = FXCollections.observableArrayList();
    private final ObservableList<String> adminUsers = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        savesList.setItems(saves);
        chatHistoryList.setItems(chats);
        watchHistoryList.setItems(watchers);
        adminUsersList.setItems(adminUsers);
        ClientMain.getConnector().addListener(this);
        showSavesTab();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateAdminState();
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            statusLabel.setText(I18N.get("management.previewMode"));
        }
    }

    @FXML private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser != null ? currentUser : AppConfig.getCurrentUser());
        ctrl.setPreviewMode(previewMode);
    }

    @FXML private void showSavesTab() {
        switchTab(savesPane, menuSavesBtn);
        requestSaves();
    }

    @FXML private void showChatTab() {
        switchTab(chatPane, menuChatBtn);
    }

    @FXML private void showWatchTab() {
        switchTab(watchPane, menuWatchBtn);
        requestWatcherHistory();
    }

    @FXML private void showAdminTab() {
        switchTab(adminPane, menuAdminBtn);
        updateAdminState();
        if (isAdmin()) {
            requestAdminStats();
            requestUserList();
        }
    }

    private void switchTab(VBox activePane, Button activeBtn) {
        for (VBox pane : new VBox[]{savesPane, chatPane, watchPane, adminPane}) {
            pane.setVisible(false);
            pane.setManaged(false);
        }
        for (Button btn : new Button[]{menuSavesBtn, menuChatBtn, menuWatchBtn, menuAdminBtn}) {
            btn.getStyleClass().remove("active");
        }
        activePane.setVisible(true);
        activePane.setManaged(true);
        activeBtn.getStyleClass().add("active");
    }

    @FXML private void requestSaves() {
        if (!ensureOnline()) return;
        ClientMain.getConnector().send(Message.of(Protocol.SAVE_LIST, "page", 0, "size", 50));
    }

    @FXML private void handleSaveCurrentGame() {
        if (!ensureOnline()) return;
        String name = saveNameField.getText() == null ? "" : saveNameField.getText().trim();
        if (name.isEmpty()) {
            name = I18N.get("management.saveArchive", java.time.LocalDateTime.now().withNano(0));
        }
        ClientMain.getConnector().send(Message.of(Protocol.SAVE_GAME, "saveName", name));
    }

    @FXML private void handleLoadSave() {
        Long saveId = selectedId(savesList);
        if (saveId == null || !ensureOnline()) return;
        ClientMain.getConnector().send(Message.of(Protocol.LOAD_GAME, "saveId", saveId));
    }

    @FXML private void handleDeleteSave() {
        Long saveId = selectedId(savesList);
        if (saveId == null || !ensureOnline()) return;
        ClientMain.getConnector().send(Message.of(Protocol.DELETE_SAVE, "saveId", saveId));
    }

    @FXML private void requestChatHistory() {
        if (!ensureOnline()) return;
        String roomId = chatRoomIdField.getText() == null ? "" : chatRoomIdField.getText().trim();
        if (roomId.isEmpty()) {
            setStatus(I18N.get("management.roomIdEmpty"));
            return;
        }
        ClientMain.getConnector().send(Message.of(Protocol.CHAT_HISTORY, "roomId", roomId, "page", 0, "size", 50));
    }

    @FXML private void requestWatcherHistory() {
        if (!ensureOnline()) return;
        ClientMain.getConnector().send(Message.of(Protocol.WATCHER_HISTORY, "page", 0, "size", 50));
    }

    @FXML private void requestUserList() {
        if (!isAdmin() || !ensureOnline()) return;
        String keyword = adminKeywordField.getText() == null ? "" : adminKeywordField.getText().trim();
        ClientMain.getConnector().send(Message.of(Protocol.USER_LIST, "page", 0, "size", 50, "keyword", keyword));
    }

    @FXML private void requestAdminStats() {
        if (!isAdmin() || !ensureOnline()) return;
        ClientMain.getConnector().send(Message.cmdOnly(Protocol.ADMIN_STATS));
    }

    @FXML private void handleBanUser() {
        Long userId = inputUserId();
        if (userId != null && isAdmin() && ensureOnline()) {
            ClientMain.getConnector().send(Message.of(Protocol.BAN_USER, "userId", userId));
        }
    }

    @FXML private void handleUnbanUser() {
        Long userId = inputUserId();
        if (userId != null && isAdmin() && ensureOnline()) {
            ClientMain.getConnector().send(Message.of(Protocol.UNBAN_USER, "userId", userId));
        }
    }

    @FXML private void handleUpdateRole() {
        Long userId = inputUserId();
        String role = adminRoleField.getText() == null ? "" : adminRoleField.getText().trim().toUpperCase();
        if (userId == null || role.isEmpty() || !isAdmin() || !ensureOnline()) return;
        ClientMain.getConnector().send(Message.of(Protocol.UPDATE_USER_ROLE, "userId", userId, "role", role));
    }

    @FXML private void handleForceCloseRoom() {
        if (!isAdmin() || !ensureOnline()) return;
        String roomId = adminRoomIdField.getText() == null ? "" : adminRoomIdField.getText().trim();
        if (roomId.isEmpty()) {
            setStatus(I18N.get("management.roomIdEmpty"));
            return;
        }
        ClientMain.getConnector().send(Message.of(Protocol.FORCE_CLOSE_ROOM, "roomId", roomId));
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.SAVE_LIST.equals(cmd)) {
            renderSaves(msg.getData().optJSONArray("saves"));
        } else if ("SAVE_GAME_OK".equals(cmd)) {
            setStatus(I18N.get("management.saveOk", msg.getString("saveName")));
            requestSaves();
        } else if ("SAVE_GAME_FAIL".equals(cmd) || "LOAD_GAME_FAIL".equals(cmd) || "DELETE_SAVE_FAIL".equals(cmd)) {
            setStatus(msg.getString("msg"));
        } else if ("LOAD_GAME_OK".equals(cmd)) {
            openLoadedSave(msg);
        } else if ("DELETE_SAVE_OK".equals(cmd)) {
            setStatus(I18N.get("management.deleted"));
            requestSaves();
        } else if (Protocol.CHAT_HISTORY.equals(cmd)) {
            renderChats(msg.getData().optJSONArray("messages"));
        } else if (Protocol.WATCHER_HISTORY.equals(cmd)) {
            renderWatchers(msg.getData().optJSONArray("watchers"));
        } else if (Protocol.USER_LIST.equals(cmd)) {
            renderUsers(msg.getData().optJSONArray("users"));
        } else if (Protocol.ADMIN_STATS.equals(cmd)) {
            adminStatsLabel.setText(I18N.get("management.adminStats", msg.getInt("totalUsers"), msg.getInt("onlineUsers"), msg.getInt("activeRooms")));
        } else if (cmd.endsWith("_OK") || "ROLE_UPDATED".equals(cmd)) {
            setStatus(I18N.get("management.operationOk", cmd));
            if (isAdmin()) requestUserList();
        } else if (cmd.endsWith("_FAIL") || Protocol.ERROR.equals(cmd)) {
            setStatus(msg.getString("msg"));
        }
    }

    private void openLoadedSave(Message msg) {
        BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
        ctrl.loadSavedGame(msg.getString("saveName"), msg.getString("boardData"), msg.getInt("currentPlayer"));
    }
    private void renderSaves(JSONArray arr) {
        saves.clear();
        if (arr == null || arr.length() == 0) {
            saves.add(I18N.get("management.noSaves"));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject s = arr.optJSONObject(i);
            if (s == null) continue;
            saves.add(s.optString("saveId") + " | " + s.optString("saveName") + " | " + s.optString("gameStatus") + " | " + s.optString("createTime"));
        }
    }

    private void renderChats(JSONArray arr) {
        chats.clear();
        if (arr == null || arr.length() == 0) {
            chats.add(I18N.get("management.noChats"));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject c = arr.optJSONObject(i);
            if (c == null) continue;
            chats.add(c.optString("createTime") + " | " + c.optString("username") + ": " + c.optString("content"));
        }
    }

    private void renderWatchers(JSONArray arr) {
        watchers.clear();
        if (arr == null || arr.length() == 0) {
            watchers.add(I18N.get("management.noWatchers"));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject w = arr.optJSONObject(i);
            if (w == null) continue;
            watchers.add(w.optString("roomId") + " | " + w.optString("joinTime") + " -> " + w.optString("leaveTime"));
        }
    }

    private void renderUsers(JSONArray arr) {
        adminUsers.clear();
        if (arr == null || arr.length() == 0) {
            adminUsers.add(I18N.get("management.noUsers"));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject u = arr.optJSONObject(i);
            if (u == null) continue;
            adminUsers.add(u.optString("userId") + " | " + u.optString("username") + " | " + u.optString("role") + " | " + u.optString("status") + " | " + u.optString("score"));
        }
    }

    private boolean ensureOnline() {
        if (previewMode || !ClientMain.getConnector().isConnected()) {
            setStatus(I18N.get("management.needServer"));
            return false;
        }
        return true;
    }

    private boolean isAdmin() {
        User user = currentUser != null ? currentUser : AppConfig.getCurrentUser();
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private void updateAdminState() {
        if (adminPane == null) return;
        if (!isAdmin()) {
            adminStatsLabel.setText(I18N.get("management.notAdmin"));
        }
    }

    private Long selectedId(ListView<String> list) {
        String selected = list.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBlank() || !Character.isDigit(selected.charAt(0))) {
            setStatus(I18N.get("management.selectRecord"));
            return null;
        }
        try {
            return Long.parseLong(selected.split("\\|")[0].trim());
        } catch (Exception e) {
            setStatus(I18N.get("management.invalidRecordId"));
            return null;
        }
    }

    private Long inputUserId() {
        try {
            return Long.parseLong(adminUserIdField.getText().trim());
        } catch (Exception e) {
            setStatus(I18N.get("management.invalidUserId"));
            return null;
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }
}

