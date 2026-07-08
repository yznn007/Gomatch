package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class FriendsController implements NetworkListener {

    @FXML private Button menuMyFriendsBtn;
    @FXML private Button menuRequestsBtn;
    @FXML private Button menuAddBtn;

    @FXML private VBox myFriendsPane;
    @FXML private VBox requestsPane;
    @FXML private VBox addPane;

    @FXML private VBox friendsContainer;
    @FXML private VBox requestsContainer;

    // 添加好友表单
    @FXML private TextField friendIdField;
    @FXML private TextField addMsgField;
    @FXML private Label errorLabel;

    private final ObservableList<FriendInfoModel> friendsData = FXCollections.observableArrayList();
    private final ObservableList<FriendRequestModel> requestsData = FXCollections.observableArrayList();

    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        friendsData.addListener((javafx.collections.ListChangeListener<FriendInfoModel>) c -> renderFriendCards());
        requestsData.addListener((javafx.collections.ListChangeListener<FriendRequestModel>) c -> renderRequestCards());
        ClientMain.getConnector().addListener(this);

        // 显示默认选项卡
        showMyFriendsTab();
    }

    private void switchTab(VBox activePane, Button activeBtn) {
        myFriendsPane.setVisible(false);
        myFriendsPane.setManaged(false);
        requestsPane.setVisible(false);
        requestsPane.setManaged(false);
        addPane.setVisible(false);
        addPane.setManaged(false);

        activePane.setVisible(true);
        activePane.setManaged(true);

        menuMyFriendsBtn.getStyleClass().remove("active");
        menuRequestsBtn.getStyleClass().remove("active");
        menuAddBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    @FXML
    private void showMyFriendsTab() {
        switchTab(myFriendsPane, menuMyFriendsBtn);
        fetchFriendsList();
    }

    @FXML
    private void showRequestsTab() {
        switchTab(requestsPane, menuRequestsBtn);
        fetchRequestsList();
    }

    @FXML
    private void showAddTab() {
        switchTab(addPane, menuAddBtn);
        errorLabel.setText("");
    }

    private void fetchFriendsList() {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.FRIEND_LIST);
        } else {
            // 预览模式伪数据
            friendsData.setAll(
                new FriendInfoModel("2", "高手小白", "1200", true),
                new FriendInfoModel("3", "五子棋王者", "1450", true),
                new FriendInfoModel("4", "大厅萌新", "1000", false)
            );
        }
    }

    private void fetchRequestsList() {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.FRIEND_REQUESTS);
        } else {
            // 预览模式伪数据
            requestsData.setAll(
                new FriendRequestModel("1", "5", "求战棋魔", "1100", "加个好友一起下棋啊！"),
                new FriendRequestModel("2", "6", "棋圣传人", "1380", "打得不错，加个好友。")
            );
        }
    }

    @FXML
    private void handleAddFriend() {
        String targetIdStr = friendIdField.getText().trim();
        String msg = addMsgField.getText().trim();
        errorLabel.setText("");
        if (targetIdStr.isEmpty()) {
            errorLabel.setText("用户ID不能为空");
            return;
        }

        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.ADD_FRIEND, "friendUserId", targetIdStr, "message", msg));
        } else {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("预览模式：已模拟向用户 " + targetIdStr + " 发送好友申请");
        }
    }

    private void handleAcceptRequest(String requestId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.ACCEPT_FRIEND, "requestId", requestId));
        } else {
            showAlert("已模拟接受请求 " + requestId);
            requestsData.removeIf(r -> r.getRequestId().equals(requestId));
        }
    }

    private void handleRejectRequest(String requestId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.REJECT_FRIEND, "requestId", requestId));
        } else {
            showAlert("已模拟拒绝请求 " + requestId);
            requestsData.removeIf(r -> r.getRequestId().equals(requestId));
        }
    }

    private void handleDeleteFriend(String friendUserId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.DELETE_FRIEND, "friendUserId", friendUserId));
        } else {
            showAlert("已模拟删除好友 " + friendUserId);
            friendsData.removeIf(f -> f.getFriendUserId().equals(friendUserId));
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        ScreenManager.showStyledAlert(alert);
    }

    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            fetchFriendsList();
            fetchRequestsList();
        }
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if ("FRIEND_LIST".equals(cmd)) {
            friendsData.clear();
            org.json.JSONArray friends = msg.getData().optJSONArray("friends");
            if (friends != null) {
                for (int i = 0; i < friends.length(); i++) {
                    org.json.JSONObject f = friends.getJSONObject(i);
                    friendsData.add(new FriendInfoModel(f.getString("friendUserId"), f.getString("friendUsername"), f.getString("friendScore"), f.optBoolean("online", false)));
                }
            }
        } else if ("FRIEND_REQUEST_LIST".equals(cmd)) {
            requestsData.clear();
            org.json.JSONArray requests = msg.getData().optJSONArray("requests");
            if (requests != null) {
                for (int i = 0; i < requests.length(); i++) {
                    org.json.JSONObject r = requests.getJSONObject(i);
                    requestsData.add(new FriendRequestModel(r.getString("requestId"), r.getString("fromUserId"), r.getString("fromUsername"), r.getString("fromScore"), r.getString("message")));
                }
            }
        } else if ("ADD_FRIEND_OK".equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("好友申请发送成功！");
            friendIdField.clear();
            addMsgField.clear();
        } else if ("ADD_FRIEND_FAIL".equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(msg.getString("msg"));
        } else if ("ACCEPT_FRIEND_OK".equals(cmd)) {
            showAlert("已成功添加好友！");
            fetchRequestsList();
            fetchFriendsList();
        } else if ("ACCEPT_FRIEND_FAIL".equals(cmd)) {
            showAlert("接受好友申请失败！");
        } else if ("REJECT_FRIEND_OK".equals(cmd)) {
            showAlert("已拒绝该好友申请。");
            fetchRequestsList();
        } else if ("REJECT_FRIEND_FAIL".equals(cmd)) {
            showAlert("拒绝操作失败！");
        } else if ("DELETE_FRIEND_OK".equals(cmd)) {
            showAlert("删除好友成功。");
            fetchFriendsList();
        } else if ("DELETE_FRIEND_FAIL".equals(cmd)) {
            showAlert("删除好友失败！");
        } else if (Protocol.ERROR.equals(cmd)) {
            showAlert("错误: " + msg.getString("msg"));
        }
    }

    // 辅助模型类
    public static class FriendInfoModel {
        private final String friendUserId;
        private final String friendUsername;
        private final String friendScore;
        private final boolean online;

        public FriendInfoModel(String friendUserId, String friendUsername, String friendScore, boolean online) {
            this.friendUserId = friendUserId;
            this.friendUsername = friendUsername;
            this.friendScore = friendScore;
            this.online = online;
        }

        public String getFriendUserId() { return friendUserId; }
        public String getFriendUsername() { return friendUsername; }
        public String getFriendScore() { return friendScore; }
        public boolean isOnline() { return online; }
    }

    public static class FriendRequestModel {
        private final String requestId;
        private final String fromUserId;
        private final String fromUsername;
        private final String fromScore;
        private final String message;

        public FriendRequestModel(String requestId, String fromUserId, String fromUsername, String fromScore, String message) {
            this.requestId = requestId;
            this.fromUserId = fromUserId;
            this.fromUsername = fromUsername;
            this.message = message;
            this.fromScore = fromScore;
        }

        public String getRequestId() { return requestId; }
        public String getFromUserId() { return fromUserId; }
        public String getFromUsername() { return fromUsername; }
        public String getFromScore() { return fromScore; }
        public String getMessage() { return message; }
    }

    private void renderFriendCards() {
        if (friendsContainer == null) return;
        javafx.application.Platform.runLater(() -> {
            friendsContainer.getChildren().clear();
            if (friendsData.isEmpty()) {
                Label noFriendLabel = new Label("暂无好友，快去添加吧！");
                noFriendLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 0;");
                friendsContainer.getChildren().add(noFriendLabel);
                return;
            }

            for (FriendInfoModel friend : friendsData) {
                // Card Root HBox
                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Avatar with Status Dot
                javafx.scene.layout.StackPane avatarContainer = new javafx.scene.layout.StackPane();
                avatarContainer.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);

                javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
                avatar.getStyleClass().add("avatar-badge");
                String name = friend.getFriendUsername() != null ? friend.getFriendUsername() : "F";
                String charVal = name.isEmpty() ? "F" : name.substring(0, 1).toUpperCase();
                Label avatarText = new Label(charVal);
                avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                avatarText.getStyleClass().add("avatar-badge-text");
                avatar.getChildren().add(avatarText);

                // Status dot
                javafx.scene.shape.Circle statusDot = new javafx.scene.shape.Circle(5);
                boolean isOnline = friend.isOnline();
                statusDot.setFill(javafx.scene.paint.Color.web(isOnline ? "#22c55e" : "#94a3b8"));
                statusDot.setStroke(javafx.scene.paint.Color.WHITE);
                statusDot.setStrokeWidth(1.5);
                statusDot.setTranslateX(2);
                statusDot.setTranslateY(2);

                avatarContainer.getChildren().addAll(avatar, statusDot);

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                HBox nameRow = new HBox(8);
                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label nameLabel = new Label(friend.getFriendUsername());
                nameLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                Label idLabel = new Label("#" + friend.getFriendUserId());
                idLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                nameRow.getChildren().addAll(nameLabel, idLabel);

                HBox detailRow = new HBox(8);
                detailRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label scoreLabel = new Label(friend.getFriendScore() + " 分");
                scoreLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                scoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                
                Label activeLabel = new Label(isOnline ? "在线" : "离线");
                activeLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                activeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isOnline ? "#16a34a;" : "#64748b;"));
                
                detailRow.getChildren().addAll(scoreLabel, activeLabel);
                infoBox.getChildren().addAll(nameRow, detailRow);

                // Action Buttons
                HBox actionBox = new HBox(8);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Button deleteBtn = new Button("删除");
                deleteBtn.getStyleClass().add("btn-secondary");
                deleteBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                deleteBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px; -fx-text-fill: #ef4444;");
 
                // Create ContextMenu for double confirmation (secondary menu)
                ContextMenu confirmMenu = new ContextMenu();
                MenuItem confirmItem = new MenuItem("确认删除");
                confirmItem.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                confirmItem.setOnAction(evt -> handleDeleteFriend(friend.getFriendUserId()));
                confirmMenu.getItems().add(confirmItem);
 
                deleteBtn.setOnAction(e -> {
                    // Show context menu right below the delete button
                    confirmMenu.show(deleteBtn, javafx.geometry.Side.BOTTOM, 0, 0);
                });
                actionBox.getChildren().add(deleteBtn);

                card.getChildren().addAll(avatarContainer, infoBox, actionBox);
                friendsContainer.getChildren().add(card);
            }
        });
    }

    private void renderRequestCards() {
        if (requestsContainer == null) return;
        javafx.application.Platform.runLater(() -> {
            requestsContainer.getChildren().clear();
            if (requestsData.isEmpty()) {
                Label noReqLabel = new Label("暂无待处理的好友申请。");
                noReqLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 0;");
                requestsContainer.getChildren().add(noReqLabel);
                return;
            }

            for (FriendRequestModel req : requestsData) {
                // Card Root HBox
                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Avatar Circle
                javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
                avatar.getStyleClass().add("avatar-badge");
                avatar.setStyle("-fx-background-color: #f59e0b;"); // Orange for requests
                String name = req.getFromUsername() != null ? req.getFromUsername() : "R";
                String charVal = name.isEmpty() ? "R" : name.substring(0, 1).toUpperCase();
                Label avatarText = new Label(charVal);
                avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                avatarText.getStyleClass().add("avatar-badge-text");
                avatar.getChildren().add(avatarText);

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                HBox nameRow = new HBox(8);
                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label nameLabel = new Label(req.getFromUsername());
                nameLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                Label idLabel = new Label("#" + req.getFromUserId());
                idLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                nameRow.getChildren().addAll(nameLabel, idLabel);

                String msg = req.getMessage() != null && !req.getMessage().isEmpty() ? 
                             "“" + req.getMessage() + "”" : "加个好友吧！";
                Label msgLabel = new Label(msg);
                msgLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
                infoBox.getChildren().addAll(nameRow, msgLabel);

                // Action Buttons
                HBox actionBox = new HBox(8);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Button acceptBtn = new Button("接受");
                acceptBtn.getStyleClass().add("btn-primary");
                acceptBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                acceptBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                acceptBtn.setOnAction(e -> handleAcceptRequest(req.getRequestId()));

                Button rejectBtn = new Button("拒绝");
                rejectBtn.getStyleClass().add("btn-secondary");
                rejectBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                rejectBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px; -fx-text-fill: #ef4444;");
                rejectBtn.setOnAction(e -> handleRejectRequest(req.getRequestId()));

                actionBox.getChildren().addAll(acceptBtn, rejectBtn);

                card.getChildren().addAll(avatar, infoBox, actionBox);
                requestsContainer.getChildren().add(card);
            }
        });
    }
}
