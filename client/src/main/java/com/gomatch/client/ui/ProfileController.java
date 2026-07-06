package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.GameRecord;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

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
        recordTable.setItems(recordData);

        ClientMain.getConnector().addListener(this);
        if (!ClientMain.getConnector().isConnected()) {
            recordData.addAll(
                new GameRecord("玩家A", "胜", "2026-07-04 15:30", "查看"),
                new GameRecord("玩家B", "负", "2026-07-04 16:10", "查看")
            );
        } else {
            ClientMain.getConnector().send(Protocol.QUERY_RECORD, "0", "20");
        }

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
            ClientMain.getConnector().send(Protocol.CHANGE_PWD, oldPwd, newPwd);
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
    public void onMessage(String command, String[] args) {
        if (Protocol.PWD_CHANGED.equals(command)) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("密码修改成功");
            oldPwdField.clear();
            newPwdField.clear();
        } else if (Protocol.ERROR.equals(command)) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(args.length > 0 ? args[0] : "操作失败");
        } else if (Protocol.RECORD_LIST.equals(command)) {
            recordData.clear();
            // 格式: RECORD_LIST|recordId|p1Id|p1Name|p2Id|p2Name|winnerId|result|moveCount|duration|time|...
            int i = 0;
            while (i + 10 <= args.length) {
                // String recordId = args[i];    // 暂未用到
                // String p1Id     = args[i + 1];
                String p1Name   = args[i + 2];
                // String p2Id     = args[i + 3];
                String p2Name   = args[i + 4];
                String winnerId = args[i + 5];
                // String result   = args[i + 6];
                // String moveCnt  = args[i + 7];
                // String duration = args[i + 8];
                String time     = args[i + 9];

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

                recordData.add(new GameRecord(opponent, result, time, "查看"));
                i += 10;
            }
        }
    }


}