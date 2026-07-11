package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
                new FriendInfoModel("2", I18N.get("friends.preview.friend1"), "1200", true),
                new FriendInfoModel("3", I18N.get("friends.preview.friend2"), "1450", true),
                new FriendInfoModel("4", I18N.get("friends.preview.friend3"), "1000", false)
            );
        }
    }

    private void fetchRequestsList() {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.FRIEND_REQUESTS);
        } else {
            // 预览模式伪数据
            requestsData.setAll(
                new FriendRequestModel("1", "5", I18N.get("friends.preview.req1"), "1100", I18N.get("friends.preview.reqMsg1")),
                new FriendRequestModel("2", "6", I18N.get("friends.preview.req2"), "1380", I18N.get("friends.preview.reqMsg2"))
            );
        }
    }

    @FXML
    private void handleAddFriend() {
        String targetIdStr = friendIdField.getText().trim();
        String msg = addMsgField.getText().trim();
        errorLabel.setText("");
        if (targetIdStr.isEmpty()) {
            errorLabel.setText(I18N.get("friends.idEmpty"));
            return;
        }

        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.ADD_FRIEND, "friendUserId", Long.parseLong(targetIdStr), "message", msg));
        } else {
            errorLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");
            errorLabel.setText(I18N.get("friends.preview.sentRequest", targetIdStr));
        }
    }

    private void handleAcceptRequest(String requestId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.ACCEPT_FRIEND, "requestId", Long.parseLong(requestId)));
        } else {
            showAlert(I18N.get("friends.simulatedAccept", requestId));
            requestsData.removeIf(r -> r.getRequestId().equals(requestId));
        }
    }

    private void handleRejectRequest(String requestId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.REJECT_FRIEND, "requestId", Long.parseLong(requestId)));
        } else {
            showAlert(I18N.get("friends.simulatedReject", requestId));
            requestsData.removeIf(r -> r.getRequestId().equals(requestId));
        }
    }

    private void handleDeleteFriend(String friendUserId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.DELETE_FRIEND, "friendUserId", Long.parseLong(friendUserId)));
        } else {
            showAlert(I18N.get("friends.simulatedDelete", friendUserId));
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
                    friendsData.add(new FriendInfoModel(
                            String.valueOf(f.optLong("friendUserId")),
                            f.optString("friendUsername"),
                            String.valueOf(f.optInt("friendScore")),
                            f.optBoolean("online", false),
                            f.optString("avatarUrl", f.optString("avatar", ""))
                    ));
                }
            }
        } else if ("FRIEND_REQUEST_LIST".equals(cmd)) {
            requestsData.clear();
            org.json.JSONArray requests = msg.getData().optJSONArray("requests");
            if (requests != null) {
                for (int i = 0; i < requests.length(); i++) {
                    org.json.JSONObject r = requests.getJSONObject(i);
                    requestsData.add(new FriendRequestModel(
                            String.valueOf(r.opt("requestId")),
                            String.valueOf(r.optLong("fromUserId")),
                            r.optString("fromUsername"),
                            String.valueOf(r.optInt("fromScore")),
                            r.optString("message")
                    ));
                }
            }
        } else if ("ADD_FRIEND_OK".equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#16a34a", "#86efac") + ";");
            errorLabel.setText(I18N.get("friends.requestSent"));
            friendIdField.clear();
            addMsgField.clear();
        } else if ("ADD_FRIEND_FAIL".equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(msg.getString("msg"));
        } else if ("ACCEPT_FRIEND_OK".equals(cmd)) {
            showAlert(I18N.get("friends.addedSuccess"));
            fetchRequestsList();
            fetchFriendsList();
        } else if ("ACCEPT_FRIEND_FAIL".equals(cmd)) {
            showAlert(I18N.get("friends.acceptFail"));
        } else if ("REJECT_FRIEND_OK".equals(cmd)) {
            showAlert(I18N.get("friends.rejected"));
            fetchRequestsList();
        } else if ("REJECT_FRIEND_FAIL".equals(cmd)) {
            showAlert(I18N.get("friends.rejectFail"));
        } else if ("DELETE_FRIEND_OK".equals(cmd)) {
            showAlert(I18N.get("friends.deleteSuccess"));
            fetchFriendsList();
        } else if ("DELETE_FRIEND_FAIL".equals(cmd)) {
            showAlert(I18N.get("friends.deleteFail"));
        } else if (Protocol.ERROR.equals(cmd)) {
            showAlert(I18N.get("friends.error", msg.getString("msg")));
        }
    }

    // 辅助模型类
    public static class FriendInfoModel {
        private final String friendUserId;
        private final String friendUsername;
        private final String friendScore;
        private final boolean online;
        private final String avatar;

        public FriendInfoModel(String friendUserId, String friendUsername, String friendScore, boolean online) {
            this(friendUserId, friendUsername, friendScore, online, "");
        }

        public FriendInfoModel(String friendUserId, String friendUsername, String friendScore, boolean online, String avatar) {
            this.friendUserId = friendUserId;
            this.friendUsername = friendUsername;
            this.friendScore = friendScore;
            this.online = online;
            this.avatar = avatar;
        }

        public String getFriendUserId() { return friendUserId; }
        public String getFriendUsername() { return friendUsername; }
        public String getFriendScore() { return friendScore; }
        public boolean isOnline() { return online; }
        public String getAvatar() { return avatar; }
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
                Label noFriendLabel = new Label(I18N.get("friends.noFriends"));
                noFriendLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-size: 13px; -fx-padding: 20 0;");
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
                
                String avatarUrl = friend.getAvatar();
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    try {
                        Image image = new Image(avatarUrl, true);
                        ImageView avatarImg = new ImageView();
                        avatarImg.setFitWidth(36);
                        avatarImg.setFitHeight(36);
                        image.errorProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal) {
                                avatarImg.setVisible(false);
                                avatarText.setVisible(true);
                            }
                        });
                        image.progressProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                                avatarImg.setImage(image);
                                avatarImg.setVisible(true);
                                avatarText.setVisible(false);
                            }
                        });
                        if (!image.isBackgroundLoading() && !image.isError()) {
                            avatarImg.setImage(image);
                            avatarImg.setVisible(true);
                            avatarText.setVisible(false);
                        }
                        avatar.getChildren().addAll(avatarText, avatarImg);
                    } catch (Exception e) {
                        avatar.getChildren().add(avatarText);
                    }
                } else {
                    avatar.getChildren().add(avatarText);
                }

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
                Label scoreLabel = new Label(friend.getFriendScore() + I18N.get("board.score"));
                scoreLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                scoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                
                Label activeLabel = new Label(isOnline ? I18N.get("friends.online") : I18N.get("friends.offline"));
                activeLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                activeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isOnline ? "#16a34a;" : "#64748b;"));
                
                detailRow.getChildren().addAll(scoreLabel, activeLabel);
                infoBox.getChildren().addAll(nameRow, detailRow);

                // Action Buttons
                HBox actionBox = new HBox(8);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Button deleteBtn = new Button(I18N.get("friends.delete"));
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                deleteBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px; -fx-text-fill: #ef4444;");
 
                // Create ContextMenu for double confirmation (secondary menu)
                ContextMenu confirmMenu = new ContextMenu();
                MenuItem confirmItem = new MenuItem(I18N.get("friends.confirmDelete"));
                confirmItem.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + "; -fx-font-weight: bold;");
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
                Label noReqLabel = new Label(I18N.get("friends.noRequests"));
                noReqLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-size: 13px; -fx-padding: 20 0;");
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
                             "\u201C" + req.getMessage() + "\u201D" : I18N.get("friends.defaultReqMsg");
                Label msgLabel = new Label(msg);
                msgLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
                infoBox.getChildren().addAll(nameRow, msgLabel);

                // Action Buttons
                HBox actionBox = new HBox(8);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Button acceptBtn = new Button(I18N.get("friends.accept"));
                acceptBtn.getStyleClass().add("btn-primary");
                acceptBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                acceptBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                acceptBtn.setOnAction(e -> handleAcceptRequest(req.getRequestId()));

                Button rejectBtn = new Button(I18N.get("friends.reject"));
                rejectBtn.getStyleClass().add("btn-danger");
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
