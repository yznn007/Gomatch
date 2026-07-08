package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.GameRecord;
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
import org.json.JSONArray;
import org.json.JSONObject;

public class ProfileController implements NetworkListener {
    @FXML private Label userIdLabel;
    @FXML private Label usernameLabel;
    @FXML private Label scoreLabel;
    @FXML private Label winLabel;
    @FXML private Label loseLabel;
    @FXML private Label drawLabel;
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private Label errorLabel;

    @FXML private TableView<GameRecord> recordTable;
    @FXML private TableColumn<GameRecord, String> opponentCol;
    @FXML private TableColumn<GameRecord, String> resultCol;
    @FXML private TableColumn<GameRecord, String> timeCol;
    @FXML private TableColumn<GameRecord, String> replayCol;

    // 新增：左侧菜单按钮与右侧面板 FXML 绑定
    @FXML private Button menuInfoBtn;
    @FXML private Button menuRecordBtn;
    @FXML private Button menuPwdBtn;
    @FXML private VBox infoPane;
    @FXML private VBox recordPane;
    @FXML private VBox pwdPane;

    private final ObservableList<GameRecord> recordData = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        // 绑定列
        opponentCol.setCellValueFactory(new PropertyValueFactory<>("opponent"));
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        replayCol.setCellValueFactory(new PropertyValueFactory<>("replay"));
        
        // 回放按钮列
        replayCol.setCellFactory(col -> new TableCell<GameRecord, String>() {
            final Button btn = new Button("查看");
            {
                btn.getStyleClass().add("control-button");
                btn.setOnAction(e -> {
                    GameRecord item = getTableView().getItems().get(getIndex());
                    String moves = item.getMoves();
                    if (moves != null && !moves.isEmpty()) {
                        BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                        ctrl.setReplayMode(true);
                        ctrl.replayMoves(moves);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "暂无棋谱数据");
                        alert.showAndWait();
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getTableView() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
        
        recordTable.setItems(recordData);

        ClientMain.getConnector().addListener(this);

        // 初始化激活默认标签页 (个人信息)
        showInfoTab();
    }

    // 新增：标签页动态切换逻辑
    private void switchTab(VBox activePane, Button activeBtn) {
        infoPane.setVisible(false);
        infoPane.setManaged(false);
        recordPane.setVisible(false);
        recordPane.setManaged(false);
        pwdPane.setVisible(false);
        pwdPane.setManaged(false);

        activePane.setVisible(true);
        activePane.setManaged(true);

        menuInfoBtn.getStyleClass().remove("active");
        menuRecordBtn.getStyleClass().remove("active");
        menuPwdBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    @FXML
    private void showInfoTab() {
        switchTab(infoPane, menuInfoBtn);
    }

    @FXML
    private void showRecordTab() {
        switchTab(recordPane, menuRecordBtn);
    }

    @FXML
    private void showPwdTab() {
        switchTab(pwdPane, menuPwdBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userIdLabel.setText(String.valueOf(user.getUserId()));
        usernameLabel.setText(user.getUsername());
        scoreLabel.setText(user.getScore() > 0 ? String.valueOf(user.getScore()) : "未定级");
        winLabel.setText(String.valueOf(user.getWinCount()));
        loseLabel.setText(String.valueOf(user.getLoseCount()));
        drawLabel.setText(String.valueOf(user.getDrawCount()));

        // 用户信息设置完成后再查询战绩，避免 currentUser 为空时解析异常
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.QUERY_RECORD, "page", 0, "size", 20));
        } else {
            recordData.addAll(
                new GameRecord("玩家A", "胜", "2026-07-04 15:30", "查看"),
                new GameRecord("玩家B", "负", "2026-07-04 16:10", "查看")
            );
        }
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    @FXML
    private void handleChangePwd() {
        String oldPwd = oldPwdField.getText().trim();
        String newPwd = newPwdField.getText().trim();
        errorLabel.setStyle("-fx-text-fill: red;");
        if (oldPwd.isEmpty() || newPwd.isEmpty()) {
            errorLabel.setText("密码不能为空");
            return;
        }
        if (newPwd.length() < 6) {
            errorLabel.setText("新密码长度需 ≥6 位");
            return;
        }
        if (ClientMain.getConnector().isConnected()) {
            ClientMain.getConnector().send(Message.of(Protocol.CHANGE_PWD, "oldPassword", oldPwd, "newPassword", newPwd));
        } else {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("预览模式：密码修改请求已模拟");
        }
    }


    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.PWD_CHANGED.equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("密码修改成功");
            oldPwdField.clear();
            newPwdField.clear();
        } else if (Protocol.ERROR.equals(cmd)) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(msg.has("msg") ? msg.getString("msg") : "操作失败");
        } else if (Protocol.RECORD_LIST.equals(cmd)) {
            if (currentUser == null) {
                return;
            }
            recordData.clear();
            JSONArray records = msg.getData().optJSONArray("records");
            if (records == null) return;
            for (int i = 0; i < records.length(); i++) {
                JSONObject r = records.getJSONObject(i);
                String p1Name = r.getString("player1Name");
                String p2Name = r.getString("player2Name");
                String winnerId = r.getString("winnerId");
                String time = r.getString("createTime");
                String boardData = r.optString("boardData", "").replace("\\|", "|");

                // 判断结果：比较 winnerId 与当前用户 ID
                String result;
                if ("0".equals(winnerId)) {
                    result = "平局";
                } else if (String.valueOf(currentUser.getUserId()).equals(winnerId)) {
                    result = "胜";
                } else {
                    result = "负";
                }

                // 对手名：当前用户是 p1 则对手是 p2，反之亦然
                String opponent = p1Name.equals(currentUser.getUsername()) ? p2Name : p1Name;

                recordData.add(new GameRecord(opponent, result, time, "查看", boardData));
            }
        }
    }


}