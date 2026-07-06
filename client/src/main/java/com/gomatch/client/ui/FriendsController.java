package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.beans.property.SimpleStringProperty;
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

    // 我的好友表格
    @FXML private TableView<FriendInfoModel> friendsTable;
    @FXML private TableColumn<FriendInfoModel, String> friendIdCol;
    @FXML private TableColumn<FriendInfoModel, String> friendNameCol;
    @FXML private TableColumn<FriendInfoModel, String> friendScoreCol;
    @FXML private TableColumn<FriendInfoModel, String> friendActionCol;

    // 好友请求表格
    @FXML private TableView<FriendRequestModel> requestsTable;
    @FXML private TableColumn<FriendRequestModel, String> reqUserIdCol;
    @FXML private TableColumn<FriendRequestModel, String> reqNameCol;
    @FXML private TableColumn<FriendRequestModel, String> reqScoreCol;
    @FXML private TableColumn<FriendRequestModel, String> reqMsgCol;
    @FXML private TableColumn<FriendRequestModel, String> reqActionCol;

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
        // 绑定我的好友列
        friendIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFriendUserId()));
        friendNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFriendUsername()));
        friendScoreCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFriendScore()));
        
        // 删除好友按钮列
        friendActionCol.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("删除");
            {
                btn.getStyleClass().add("btn-secondary");
                btn.setStyle("-fx-text-fill: red; -fx-padding: 2 8;");
                btn.setOnAction(e -> {
                    FriendInfoModel item = getTableView().getItems().get(getIndex());
                    handleDeleteFriend(item.getFriendUserId());
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
        friendsTable.setItems(friendsData);

        // 绑定好友请求列
        reqUserIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFromUserId()));
        reqNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFromUsername()));
        reqScoreCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFromScore()));
        reqMsgCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMessage()));
        
        // 接受/拒绝按钮列
        reqActionCol.setCellFactory(col -> new TableCell<>() {
            final Button acceptBtn = new Button("接受");
            final Button rejectBtn = new Button("拒绝");
            final HBox box = new HBox(8, acceptBtn, rejectBtn);
            {
                acceptBtn.getStyleClass().add("btn-primary");
                acceptBtn.setStyle("-fx-padding: 2 8;");
                acceptBtn.setOnAction(e -> {
                    FriendRequestModel item = getTableView().getItems().get(getIndex());
                    handleAcceptRequest(item.getRequestId());
                });

                rejectBtn.getStyleClass().add("btn-secondary");
                rejectBtn.setStyle("-fx-padding: 2 8; -fx-text-fill: red;");
                rejectBtn.setOnAction(e -> {
                    FriendRequestModel item = getTableView().getItems().get(getIndex());
                    handleRejectRequest(item.getRequestId());
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        requestsTable.setItems(requestsData);

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
                new FriendInfoModel("2", "高手小白", "1200"),
                new FriendInfoModel("3", "五子棋王者", "1450"),
                new FriendInfoModel("4", "大厅萌新", "1000")
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
        if (targetIdStr.isEmpty()) {
            errorLabel.setText("用户ID不能为空");
            return;
        }

        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.ADD_FRIEND, targetIdStr, msg);
        } else {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("预览模式：已模拟向用户 " + targetIdStr + " 发送好友申请");
        }
    }

    private void handleAcceptRequest(String requestId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.ACCEPT_FRIEND, requestId);
        } else {
            showAlert("已模拟接受请求 " + requestId);
            requestsData.removeIf(r -> r.getRequestId().equals(requestId));
        }
    }

    private void handleRejectRequest(String requestId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.REJECT_FRIEND, requestId);
        } else {
            showAlert("已模拟拒绝请求 " + requestId);
            requestsData.removeIf(r -> r.getRequestId().equals(requestId));
        }
    }

    private void handleDeleteFriend(String friendUserId) {
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Protocol.DELETE_FRIEND, friendUserId);
        } else {
            showAlert("已模拟删除好友 " + friendUserId);
            friendsData.removeIf(f -> f.getFriendUserId().equals(friendUserId));
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
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
    public void onMessage(String command, String[] args) {
        if ("FRIEND_LIST".equals(command)) {
            friendsData.clear();
            for (int i = 0; i < args.length; i += 3) {
                if (i + 2 < args.length) {
                    friendsData.add(new FriendInfoModel(args[i], args[i + 1], args[i + 2]));
                }
            }
        } else if ("FRIEND_REQUEST_LIST".equals(command)) {
            requestsData.clear();
            for (int i = 0; i < args.length; i += 5) {
                if (i + 4 < args.length) {
                    requestsData.add(new FriendRequestModel(args[i], args[i + 1], args[i + 2], args[i + 3], args[i + 4]));
                }
            }
        } else if ("ADD_FRIEND_OK".equals(command)) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("好友申请发送成功！");
            friendIdField.clear();
            addMsgField.clear();
        } else if ("ADD_FRIEND_FAIL".equals(command)) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(args.length > 0 ? args[0] : "发送失败（对方不存在或已申请）");
        } else if ("ACCEPT_FRIEND_OK".equals(command)) {
            showAlert("已成功添加好友！");
            fetchRequestsList();
            fetchFriendsList();
        } else if ("ACCEPT_FRIEND_FAIL".equals(command)) {
            showAlert("接受好友申请失败！");
        } else if ("REJECT_FRIEND_OK".equals(command)) {
            showAlert("已拒绝该好友申请。");
            fetchRequestsList();
        } else if ("REJECT_FRIEND_FAIL".equals(command)) {
            showAlert("拒绝操作失败！");
        } else if ("DELETE_FRIEND_OK".equals(command)) {
            showAlert("删除好友成功。");
            fetchFriendsList();
        } else if ("DELETE_FRIEND_FAIL".equals(command)) {
            showAlert("删除好友失败！");
        } else if (Protocol.ERROR.equals(command)) {
            showAlert("错误: " + (args.length > 0 ? args[0] : "未知错误"));
        }
    }

    // 辅助模型类
    public static class FriendInfoModel {
        private final String friendUserId;
        private final String friendUsername;
        private final String friendScore;

        public FriendInfoModel(String friendUserId, String friendUsername, String friendScore) {
            this.friendUserId = friendUserId;
            this.friendUsername = friendUsername;
            this.friendScore = friendScore;
        }

        public String getFriendUserId() { return friendUserId; }
        public String getFriendUsername() { return friendUsername; }
        public String getFriendScore() { return friendScore; }
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
}
