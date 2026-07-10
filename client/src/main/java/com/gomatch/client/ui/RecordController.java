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

    /** 字符串兜底：null 时返回空串（非 null），保证 setReplayMode 接收的参数不会 NPE */
    private static String safeStr(String v) {
        return v == null ? "" : v;
    }

    /** 字符串兜底：null/空时返回 fallback，避免回放卡片显示空白 */
    private static String safeStr(String v, String fallback) {
        return (v == null || v.isEmpty()) ? (fallback == null ? "" : fallback) : v;
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

                Object boardDataObj = r.opt("boardData");
                String boardData;
                if (boardDataObj instanceof org.json.JSONArray) {
                    boardData = boardDataObj.toString();
                } else {
                    boardData = r.optString("boardData", "");
                }
                boardData = boardData.replace("\\|", "|");
                int moveCount = r.optInt("moveCount", 0);
                int duration = r.optInt("duration", 0);
                int scoreChange = r.optInt("scoreChange", 0);

                // 使用 HashMap 替代 Map.ofEntries —— 后者最多只支持 10 个 entry，
                // 之前 12 个 entry 会抛 IllegalArgumentException，导致 player1/2 的 ID 和名字
                // 全部丢失，回放时只能看到"黑方/白方/#---"占位符。
                java.util.HashMap<String, String> recordMap = new java.util.HashMap<>();
                recordMap.put("opponent", opponentName);
                recordMap.put("result", result);
                recordMap.put("time", r.getString("createTime"));
                recordMap.put("replay", "回放");
                recordMap.put("moves", boardData);
                recordMap.put("moveCount", String.valueOf(moveCount));
                recordMap.put("duration", String.valueOf(duration));
                recordMap.put("scoreChange", String.valueOf(scoreChange));
                recordMap.put("player1Id", String.valueOf(player1Id));
                recordMap.put("player1Name", r.getString("player1Name"));
                recordMap.put("player1Score", String.valueOf(r.optInt("player1Score", -1)));
                recordMap.put("player1Avatar", r.optString("player1Avatar", ""));
                recordMap.put("player2Id", String.valueOf(r.getLong("player2Id")));
                recordMap.put("player2Name", r.getString("player2Name"));
                recordMap.put("player2Score", String.valueOf(r.optInt("player2Score", -1)));
                recordMap.put("player2Avatar", r.optString("player2Avatar", ""));
                records.add(recordMap);
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
                // 关键：Java lambda 闭包按引用捕获循环变量，所有按钮的 onAction 会共享同一条 record 引用
                // （最终指向最后一条 record）。这里用 final 局部变量复制当前迭代的 record 引用，
                // 让每个按钮的闭包只看到自己这条 record。
                final Map<String, String> finalRecord = record;

                // Card Root HBox
                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Result Badge & ELO change indicator
                javafx.scene.layout.StackPane badgeContainer = new javafx.scene.layout.StackPane();
                badgeContainer.getStyleClass().add("result-badge");
                String result = finalRecord.getOrDefault("result", "平");
                Label badgeText = new Label(result.substring(0, 1));
                badgeText.getStyleClass().add("result-badge-text");
                badgeContainer.getChildren().add(badgeText);

                int scoreChange = parseIntOrDefault(finalRecord.get("scoreChange"), 0);
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

                Label opponentLabel = new Label("与 " + finalRecord.get("opponent") + " 的对局");
                opponentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");

                int moveCount = parseIntOrDefault(finalRecord.get("moveCount"), 0);
                int durationSec = parseIntOrDefault(finalRecord.get("duration"), 0);
                String durationStr = formatDuration(durationSec);
                Label detailsLabel = new Label("落子: " + (moveCount > 0 ? moveCount : "--") + " 步 | 用时: " + durationStr);
                detailsLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");

                infoBox.getChildren().addAll(opponentLabel, detailsLabel);

                // Right VBox: Time & Action Button
                VBox rightBox = new VBox(4);
                rightBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Label timeLabel = new Label(finalRecord.get("time"));
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");

                Button replayBtn = new Button("回放");
                replayBtn.getStyleClass().add("btn-primary");
                replayBtn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                replayBtn.setStyle("-fx-padding: 4 14; -fx-font-size: 12px;");
                replayBtn.setOnAction(e -> {
                    String movesStr = finalRecord.get("moves");
                    if (movesStr != null && !movesStr.isEmpty()) {
                        // 第一保险：从 recordMap 拿 p1/p2 的 ID 和名字
                        String p1Id   = safeStr(finalRecord.get("player1Id"));
                        String p1Name = safeStr(finalRecord.get("player1Name"), p1Id);
                        String p2Id   = safeStr(finalRecord.get("player2Id"));
                        String p2Name = safeStr(finalRecord.get("player2Name"), p2Id);
                        // 第二保险：如果 recordMap 里名字还是空，从 boardData 反推黑白方 userId
                        // （boardData[0] 是黑方第一步、boardData[1] 是白方第一步）
                        try {
                            org.json.JSONArray bd = new org.json.JSONArray(movesStr);
                            if ((p1Name.isEmpty() || p2Name.isEmpty()) && bd.length() >= 2) {
                                long firstBlackUid  = bd.getJSONObject(0).optLong("userId", -2);
                                long firstWhiteUid  = bd.getJSONObject(1).optLong("userId", -2);
                                if (p1Id.isEmpty() && firstBlackUid != -2) p1Id = String.valueOf(firstBlackUid);
                                if (p2Id.isEmpty() && firstWhiteUid != -2) p2Id = String.valueOf(firstWhiteUid);
                                if (p1Name.isEmpty() && firstBlackUid != -2) p1Name = String.valueOf(firstBlackUid);
                                if (p2Name.isEmpty() && firstWhiteUid != -2) p2Name = String.valueOf(firstWhiteUid);
                            }
                        } catch (Exception ex) {
                            // boardData 解析失败，使用兜底信息
                        }
                        int p1Score = parseIntOrDefault(finalRecord.get("player1Score"), -1);
                        int p2Score = parseIntOrDefault(finalRecord.get("player2Score"), -1);
                        String p1Avatar = finalRecord.getOrDefault("player1Avatar", "");
                        String p2Avatar = finalRecord.getOrDefault("player2Avatar", "");
                        BoardController ctrl = ScreenManager.switchTo("/fxml/BoardUI.fxml");
                        ctrl.setReplayMode(true, p1Id, p1Name, p2Id, p2Name, p1Score, p2Score, p1Avatar, p2Avatar);
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
