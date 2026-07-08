package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

@SuppressWarnings("unchecked")
public class RecordController implements NetworkListener {
    @FXML private TableView<Map<String, String>> recordTable;
    @FXML private TableColumn<Map<String, String>, String> opponentCol;
    @FXML private TableColumn<Map<String, String>, String> resultCol;
    @FXML private TableColumn<Map<String, String>, String> timeCol;
    @FXML private TableColumn<Map<String, String>, String> replayCol;

    private final ObservableList<Map<String, String>> records = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        opponentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("opponent")));
        resultCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("result")));
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("time")));
        replayCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrDefault("replay", "回放")));
        // 回放按钮列
        replayCol.setCellFactory(col -> new TableCell<Map<String, String>, String>() {
            final Button btn = new Button("回放");
            {
                btn.setOnAction(e -> {
                    Map<String, String> row = getTableView().getItems().get(getIndex());
                    String moves = row.get("moves");
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
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });

        recordTable.setItems(records);

        if (!ClientMain.getConnector().isConnected()) {
            records.addAll(
                Map.of("opponent", "玩家A", "result", "胜", "time", "2026-07-04 15:30", "replay", "回放", "moves", "3,3,1;3,4,2;4,4,1;4,5,2;5,5,1"),
                Map.of("opponent", "玩家B", "result", "负", "time", "2026-07-04 16:10", "replay", "回放", "moves", "7,7,1;7,8,2;8,8,1;8,9,2;9,9,1;9,10,2")
            );
        } else {
            ClientMain.getConnector().addListener(this);
            ClientMain.getConnector().send(Message.of(Protocol.QUERY_RECORD, "page", 0, "size", 10));
        }
    }

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(Message msg) {
        if ("RECORD_LIST".equals(msg.getCmd())) {
            records.clear();
            com.gomatch.client.model.User currentUser = com.gomatch.client.AppConfig.getCurrentUser();
            if (currentUser == null) {
                return;
            }
            long currentUserId = currentUser.getUserId();
            JSONArray recs = msg.getData().optJSONArray("records");
            if (recs == null) return;
            for (int i = 0; i < recs.length(); i++) {
                JSONObject r = recs.getJSONObject(i);
                long player1Id = Long.parseLong(r.getString("player1Id"));
                long winnerId = Long.parseLong(r.getString("winnerId"));

                String opponentName = currentUserId == player1Id ? r.getString("player2Name") : r.getString("player1Name");
                String result;
                if (winnerId == 0) {
                    result = "平局";
                } else if (winnerId == currentUserId) {
                    result = "胜";
                } else {
                    result = "负";
                }

                String boardData = r.optString("boardData", "").replace("\\|", "|");

                records.add(Map.of(
                    "opponent", opponentName,
                    "result", result,
                    "time", r.getString("createTime"),
                    "replay", "回放",
                    "moves", boardData
                ));
            }
        }
    }
}
