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
            ClientMain.getConnector().send(Protocol.GAME_RECORDS);
        }
    }

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.GAME_RECORDS_LIST.equals(command) && args.length > 0) {
            try {
                Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> list = gson.fromJson(args[0], listType);
                records.clear();
                records.addAll(list);
            } catch (Exception ignored) {
            }
        }
    }
}
