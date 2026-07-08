package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
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
        ClientMain.getConnector().send(Message.of(Protocol.CREATE_ROOM, "roomName", name, "password", password));
    }

    @FXML
    private void handleCancel() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(Message msg) {
        if (Protocol.JOIN_OK.equals(msg.getCmd())) {
            com.gomatch.client.model.RoomInfo room = new com.gomatch.client.model.RoomInfo();
            room.setRoomId(msg.getString("roomId"));
            room.setRoomName(msg.getString("roomName"));
            String[] players = msg.getString("players").split("/");
            room.setPlayerCount(Integer.parseInt(players[0]));
            room.setHasPassword(msg.optBoolean("hasPassword", false));
            RoomWaitingController ctrl = ScreenManager.switchTo("/fxml/RoomWaitingUI.fxml");
            ctrl.setRoomInfo(room, currentUser);
        } else if (Protocol.ERROR.equals(msg.getCmd())) {
            errorLabel.setText(msg.getString("msg"));
        }
    }
}
