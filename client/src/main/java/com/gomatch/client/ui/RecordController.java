package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class RecordController implements NetworkListener {
    @FXML private TableView<Map<String, String>> recordTable;
    @FXML private TableColumn<Map<String, String>, String> opponentCol;
    @FXML private TableColumn<Map<String, String>, String> resultCol;
    @FXML private TableColumn<Map<String, String>, String> timeCol;
    @FXML private TableColumn<Map<String, String>, String> replayCol;

    private final ObservableList<Map<String, String>> records = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private String movesData; // 用于棋谱回放

    @FXML
    private void initialize() {
        opponentCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("opponent")));
        resultCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("result")));
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("time")));
        replayCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrDefault("replay", "回放")));
        // 回放按钮列
        replayCol.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("回放");
            {
                btn.setOnAction(e -> {
                    Map<String, String> row = getTableView().getItems().get(getIndex());
                    String moves = row.get("moves");
                    if (moves != null && !moves.isEmpty()) {
                        BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                        ctrl.setPreviewMode(true);
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
            ClientMain.getConnector().send("QUERY_RECORD", "0", "10");
        }
    }

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(String command, String[] args) {
        if ("RECORD_LIST".equals(command)) {
            records.clear();
            for (int i = 0; i < args.length; i += 10) {
                if (i + 9 < args.length) {
                    long currentUserId = com.gomatch.client.AppConfig.getCurrentUser().getUserId();
                    long player1Id = Long.parseLong(args[i + 1]);
                    long player2Id = Long.parseLong(args[i + 3]);
                    long winnerId = Long.parseLong(args[i + 5]);
                    
                    String opponentName = currentUserId == player1Id ? args[i + 4] : args[i + 2];
                    String result;
                    if (winnerId == 0) {
                        result = "平局";
                    } else if (winnerId == currentUserId) {
                        result = "胜";
                    } else {
                        result = "负";
                    }
                    
                    records.add(Map.of(
                        "opponent", opponentName,
                        "result", result,
                        "time", args[i + 9],
                        "replay", "回放",
                        "moves", ""
                    ));
                }
            }
        }
    }
}
