package com.gomatch.client.ui;

import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class CreateRoomController implements NetworkListener {
    @FXML private TextField roomNameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleConfirm() {
        String name = roomNameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("房间名不能为空");
            return;
        }
        String password = passwordField.getText().trim();
        com.gomatch.client.ClientMain.getConnector()
                .send(Protocol.CREATE_ROOM, name, password);
    }

    @FXML
    private void handleCancel() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(String command, String[] args) {
        if ("ROOM_CREATED".equals(command)) {
            com.gomatch.client.model.RoomInfo room =
                    new com.google.gson.Gson().fromJson(args[0], com.gomatch.client.model.RoomInfo.class);
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setRoomInfo(room, currentUser);
        } else if (Protocol.ERROR.equals(command)) {
            errorLabel.setText(args[0]);
        }
    }
}
