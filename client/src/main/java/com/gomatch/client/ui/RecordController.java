package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class RecordController implements NetworkListener {
    @FXML private VBox recordsContainer;

    private final ObservableList<Map<String, String>> records = FXCollections.observableArrayList();

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "--";
        int m = seconds / 60;
        int s = seconds % 60;
        if (m > 0) {
            return m + "分" + (s > 0 ? s + "秒" : "");
        }
        return s + "秒";
    }

    @SuppressWarnings("unchecked")
    @FXML
    private void initialize() {
        records.addListener((javafx.collections.ListChangeListener<Map<String, String>>) c -> renderRecordCards());
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
                long player1Id = r.getLong("player1Id");
                long winnerId = r.getLong("winnerId");

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
                int moveCount = r.optInt("moveCount", 0);
                int duration = r.optInt("duration", 0);
                int scoreChange = r.optInt("scoreChange", 0);

                records.add(Map.of(
                    "opponent", opponentName,
                    "result", result,
                    "time", r.getString("createTime"),
                    "replay", "回放",
                    "moves", boardData,
                    "moveCount", String.valueOf(moveCount),
                    "duration", String.valueOf(duration),
                    "scoreChange", String.valueOf(scoreChange)
                ));
            }
        }
    }

    private void renderRecordCards() {
        if (recordsContainer == null) return;
        javafx.application.Platform.runLater(() -> {
            recordsContainer.getChildren().clear();
            if (records.isEmpty()) {
                Label noRecordLabel = new Label("暂无对战记录，快去对战大厅来一局吧！");
                noRecordLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 0;");
                recordsContainer.getChildren().add(noRecordLabel);
                return;
            }

            for (Map<String, String> record : records) {
                // Card Root HBox
                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Result Badge & ELO change indicator
                javafx.scene.layout.StackPane badgeContainer = new javafx.scene.layout.StackPane();
                badgeContainer.getStyleClass().add("result-badge");
                String result = record.getOrDefault("result", "平");
                Label badgeText = new Label(result.substring(0, 1));
                badgeText.getStyleClass().add("result-badge-text");
                badgeContainer.getChildren().add(badgeText);

                int scoreChange = parseIntOrDefault(record.get("scoreChange"), 0);
                Label scoreChangeLabel = new Label();
                scoreChangeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 32px;");
                if (result.contains("胜")) {
                    badgeContainer.getStyleClass().add("result-badge-win");
                    scoreChangeLabel.setText("+" + scoreChange);
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #22c55e;");
                } else if (result.contains("负")) {
                    badgeContainer.getStyleClass().add("result-badge-lose");
                    scoreChangeLabel.setText(String.valueOf(scoreChange));
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #ef4444;");
                } else {
                    badgeContainer.getStyleClass().add("result-badge-draw");
                    scoreChangeLabel.setText("+" + scoreChange);
                    scoreChangeLabel.setStyle(scoreChangeLabel.getStyle() + " -fx-text-fill: #94a3b8;");
                }

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                Label opponentLabel = new Label("与 " + record.get("opponent") + " 的对局");
                opponentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

                int moveCount = parseIntOrDefault(record.get("moveCount"), 0);
                int durationSec = parseIntOrDefault(record.get("duration"), 0);
                String durationStr = formatDuration(durationSec);
                Label detailsLabel = new Label("落子: " + (moveCount > 0 ? moveCount : "--") + " 步 | 用时: " + durationStr);
                detailsLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

                infoBox.getChildren().addAll(opponentLabel, detailsLabel);

                // Right VBox: Time & Action Button
                VBox rightBox = new VBox(4);
                rightBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Label timeLabel = new Label(record.get("time"));
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

                Button replayBtn = new Button("回放");
                replayBtn.getStyleClass().add("btn-primary");
                replayBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                replayBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                replayBtn.setOnAction(e -> {
                    String movesStr = record.get("moves");
                    if (movesStr != null && !movesStr.isEmpty()) {
                        BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                        ctrl.setReplayMode(true);
                        ctrl.replayMoves(movesStr);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "暂无棋谱数据");
                        ScreenManager.showStyledAlert(alert);
                    }
                });

                rightBox.getChildren().addAll(timeLabel, replayBtn);

                card.getChildren().addAll(badgeContainer, scoreChangeLabel, infoBox, rightBox);
                recordsContainer.getChildren().add(card);
            }
        });
    }
}
