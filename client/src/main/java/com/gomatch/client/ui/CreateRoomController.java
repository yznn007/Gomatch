package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class CreateRoomController implements NetworkListener {
    @FXML private TextField roomNameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final Gson gson = new Gson();
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void initialize() {
        ClientMain.getConnector().addListener(this);
    }

    @FXML
    private void handleConfirm() {
        String name = roomNameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("房间名不能为空");
            return;
        }
        String password = passwordField.getText().trim();
        ClientMain.getConnector().send(Protocol.CREATE_ROOM, name, password);
    }

    @FXML
    private void handleCancel() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.ROOM_CREATED.equals(command)) {
            com.gomatch.client.model.RoomInfo room = gson.fromJson(args[0], com.gomatch.client.model.RoomInfo.class);
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setRoomInfo(room, currentUser);
        } else if (Protocol.ERROR.equals(command)) {
            errorLabel.setText(args.length > 0 ? args[0] : "创建失败");
        }
    }
}
