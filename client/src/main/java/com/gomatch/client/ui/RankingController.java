package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

public class RankingController implements NetworkListener {
    @FXML private VBox rankingContainer;

    @FXML private Button menuRankingBtn;
    @FXML private VBox rankingPane;

    private final ObservableList<User> rankingData = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        rankingData.addListener((javafx.collections.ListChangeListener<User>) c -> renderRankingCards());
        ClientMain.getConnector().addListener(this);

        loadRanking();
        showRankingTab();
        renderRankingCards();
    }

    /** 加载排行榜：预览/未连接时显示本地假数据，联网时向服务器请求。 */
    private void loadRanking() {
        if (isPreview() || !ClientMain.getConnector().isConnected()) {
            loadMockRanking();
            return;
        }
        ClientMain.getConnector().send(Protocol.RANKING);
    }

    private void loadMockRanking() {
        String[][] fakeData = {
            {"1", "棋圣",   "1500", "83.3%"},
            {"2", "高手如云","1350", "72.7%"},
            {"3", "五子连珠","1200", "63.6%"},
            {"4", "新手求带","1050", "44.4%"},
            {"5", "游客",   "1000",  "0%"  }
        };
        rankingData.clear();
        for (String[] row : fakeData) {
            User u = new User();
            u.setRank(Integer.parseInt(row[0]));
            u.setUsername(row[1]);
            u.setScore(Integer.parseInt(row[2]));
            u.setWinRate(row[3]);
            rankingData.add(u);
        }
    }

    private String getRankTier(int score) {
        if (score < 1050) return "[青铜]";
        if (score < 1200) return "[白银]";
        if (score < 1350) return "[黄金]";
        if (score < 1500) return "[铂金]";
        return "[钻石]";
    }

    private void renderRankingCards() {
        if (rankingContainer == null) return;
        Platform.runLater(() -> {
            rankingContainer.getChildren().clear();
            if (rankingData.isEmpty()) {
                Label noRankLabel = new Label("暂无排行数据。");
                noRankLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 0;");
                rankingContainer.getChildren().add(noRankLabel);
                return;
            }

            for (User user : rankingData) {
                // Card Root HBox
                HBox card = new HBox();
                card.getStyleClass().add("list-item-card");
                card.setSpacing(14);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                // Highlight current user
                boolean isMe = currentUser != null && currentUser.getUsername() != null 
                             && currentUser.getUsername().equals(user.getUsername());
                if (isMe) {
                    card.getStyleClass().add("list-item-card-highlight");
                }

                // Rank Badge (Trophies for 1st, 2nd, 3rd)
                javafx.scene.layout.StackPane rankBadge = new javafx.scene.layout.StackPane();
                rankBadge.getStyleClass().add("rank-number-badge");
                int rankVal = user.getRank();
                Label rankNum = new Label(String.valueOf(rankVal));
                rankNum.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                rankNum.getStyleClass().add("rank-number-text");
                if (rankVal == 1) {
                    rankNum.setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else if (rankVal == 2) {
                    rankNum.setStyle("-fx-text-fill: #475569; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else if (rankVal == 3) {
                    rankNum.setStyle("-fx-text-fill: #78350f; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
                rankBadge.getChildren().add(rankNum);

                // Avatar Circle
                javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
                avatar.getStyleClass().add("avatar-badge");
                if (rankVal == 1) {
                    avatar.setStyle("-fx-background-color: #fbbf24;"); // Gold for 1st
                } else if (rankVal == 2) {
                    avatar.setStyle("-fx-background-color: #cbd5e1;"); // Silver for 2nd
                } else if (rankVal == 3) {
                    avatar.setStyle("-fx-background-color: #d97706;"); // Bronze for 3rd
                }
                String name = user.getUsername() != null ? user.getUsername() : "U";
                String charVal = name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase();
                Label avatarText = new Label(charVal);
                avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                avatarText.getStyleClass().add("avatar-badge-text");
                avatar.getChildren().add(avatarText);

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                HBox nameRow = new HBox(8);
                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label nameLabel = new Label(user.getUsername());
                nameLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
                if (isMe) {
                    nameLabel.setText(nameLabel.getText() + " (我)");
                    nameLabel.setStyle(nameLabel.getStyle() + " -fx-text-fill: #0ea5e9;");
                }
                nameRow.getChildren().addAll(nameLabel);

                String winRateStr = user.getWinRate() != null ? user.getWinRate() : "";
                if (!winRateStr.isEmpty() && !winRateStr.contains("%")) {
                    winRateStr = winRateStr + "%";
                }
                Label detailsLabel = new Label("积分: " + user.getScore() + " | 胜率: " + winRateStr);
                detailsLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

                infoBox.getChildren().addAll(nameRow, detailsLabel);

                card.getChildren().addAll(rankBadge, avatar, infoBox);
                rankingContainer.getChildren().add(card);
            }
        });
    }

    @FXML
    public void showRankingTab() {
        rankingPane.setVisible(true);
        rankingPane.setManaged(true);
        menuRankingBtn.getStyleClass().add("active");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
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
        if (Protocol.RANKING_LIST.equals(cmd)) {
            parseRankingList(msg);
        } else if (Protocol.ERROR.equals(cmd)) {
            // 服务端无数据或异常时，已联网但无数据则清空并提示，非联网预览模式已走假数据
            rankingData.clear();
            renderRankingCards();
        }
    }

    private void parseRankingList(Message msg) {
        JSONArray users = msg.getData() != null ? msg.getData().optJSONArray("users") : null;
        if (users == null) {
            rankingData.clear();
            renderRankingCards();
            return;
        }
        ObservableList<User> parsed = FXCollections.observableArrayList();
        for (int i = 0; i < users.length(); i++) {
            JSONObject u = users.optJSONObject(i);
            if (u == null) continue;
            try {
                User user = new User();
                user.setRank(u.optInt("rank", i + 1));
                user.setUsername(u.optString("username", ""));
                user.setScore(u.optInt("score", 0));
                // 服务端 winRate 可能是 double 或字符串，统一按字符串读取
                Object winRateObj = u.opt("winRate");
                String winRateStr = winRateObj != null ? String.valueOf(winRateObj) : "";
                user.setWinRate(winRateStr);
                parsed.add(user);
            } catch (Exception e) {
                // 单条记录解析失败不影响其他记录
                System.err.println("解析排行榜用户失败: " + u);
            }
        }
        rankingData.setAll(parsed);
    }

    private boolean isPreview() {
        return previewMode || AppConfig.isPreviewMode();
    }
}
