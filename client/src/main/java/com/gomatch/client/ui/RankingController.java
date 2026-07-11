package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

public class RankingController implements NetworkListener {
    @FXML private VBox rankingContainer;

    @FXML private Button menuGlobalBtn;
    @FXML private Button menuFriendsBtn;
    @FXML private Label rankingTitleLabel;
    @FXML private VBox rankingPane;

    private final ObservableList<User> rankingData = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;
    private String currentTab = "GLOBAL";

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
            {"1", I18N.get("lobby.preview.rank1"),   "1500", "83.3%"},
            {"2", I18N.get("lobby.preview.rank2"), "1350", "72.7%"},
            {"3", I18N.get("lobby.preview.rank3"), "1200", "63.6%"},
            {"4", I18N.get("ranking.preview.player4"), "1050", "44.4%"},
            {"5", I18N.get("common.guest"),   "1000",  "0%"  }
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

    private void showRankingTab() {
        currentTab = "GLOBAL";
        rankingTitleLabel.setText(I18N.get("ranking.globalTitle"));
        menuGlobalBtn.getStyleClass().remove("active");
        menuFriendsBtn.getStyleClass().remove("active");
        menuGlobalBtn.getStyleClass().add("active");
    }

    private void renderRankingCards() {
        if (rankingContainer == null) return;
        Platform.runLater(() -> {
            rankingContainer.getChildren().clear();
            if (rankingData.isEmpty()) {
                Label noRankLabel = new Label(I18N.get("ranking.noRankData"));
                noRankLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-size: 13px; -fx-padding: 20 0;");
                rankingContainer.getChildren().add(noRankLabel);
                return;
            }

            // 好友排行：只有自己（无好友）时给出友好提示
            if ("FRIENDS".equals(currentTab) && rankingData.size() == 1
                    && currentUser != null && currentUser.getUsername() != null
                    && currentUser.getUsername().equals(rankingData.get(0).getUsername())) {
                Label noFriendLabel = new Label(I18N.get("ranking.noFriends"));
                noFriendLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + "; -fx-font-size: 13px; -fx-padding: 20 0;");
                rankingContainer.getChildren().add(noFriendLabel);
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

                // Rank Badge
                javafx.scene.layout.StackPane rankBadge = new javafx.scene.layout.StackPane();
                rankBadge.getStyleClass().add("rank-number-badge");
                int rankVal = user.getRank();
                Label rankNum = new Label(String.valueOf(rankVal));
                rankNum.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                rankNum.getStyleClass().add("rank-number-text");
                // 统一走 CSS 默认文字色，主题切换由 .theme-dark 自动适配；不再按金/银/铜写死
                rankNum.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                rankBadge.getChildren().add(rankNum);

                // Avatar Circle
                final double AVATAR_SIZE = 44;
                String name = user.getUsername() != null ? user.getUsername() : "U";
                javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
                avatar.getStyleClass().add("avatar-badge");
                avatar.setPrefSize(AVATAR_SIZE, AVATAR_SIZE);
                avatar.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
                avatar.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
                // 所有排名统一用渐变底色，与 LobbyController 抽屉头像风格一致
                avatar.setStyle("-fx-background-color: " + gradientFor(name) + ";");
                String charVal = name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase();
                Label avatarText = new Label(charVal);
                avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                avatarText.getStyleClass().add("avatar-badge-text");

                // 加载真实头像（如果有）
                String avatarUrl = user.getAvatar();
                // 默认仅显示首字母；图片加载成功后再切到真实头像
                avatarText.setVisible(true);
                ImageView avatarImg = new ImageView();
                avatarImg.setFitWidth(AVATAR_SIZE);
                avatarImg.setFitHeight(AVATAR_SIZE);
                avatarImg.setVisible(false);
                javafx.scene.shape.Circle imgClip = new javafx.scene.shape.Circle(
                        AVATAR_SIZE / 2.0, AVATAR_SIZE / 2.0, AVATAR_SIZE / 2.0);
                avatarImg.setClip(imgClip);
                avatar.getChildren().addAll(avatarText, avatarImg);
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    try {
                        final Image image = new Image(avatarUrl, true);
                        image.errorProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal) {
                                avatarImg.setVisible(false);
                                avatarText.setVisible(true);
                            }
                        });
                        image.progressProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                                avatarImg.setImage(image);
                                avatarImg.setVisible(true);
                                avatarText.setVisible(false);
                            }
                        });
                        // 缓存命中同步路径
                        if (image.getProgress() >= 1.0 && !image.isError()) {
                            avatarImg.setImage(image);
                            avatarImg.setVisible(true);
                            avatarText.setVisible(false);
                        }
                        if (!image.isBackgroundLoading() && !image.isError()) {
                            avatarImg.setImage(image);
                            avatarImg.setVisible(true);
                            avatarText.setVisible(false);
                        }
                    } catch (Exception e) {
                        // 加载失败：保留首字母
                    }
                }

                // 点击头像放大预览
                final String previewUrl = avatarUrl;
                final String previewName = name;
                avatar.setOnMouseClicked((MouseEvent e) -> {
                    e.consume();
                    showAvatarPreview(previewUrl, previewName);
                });
                avatar.setStyle(avatar.getStyle() + " -fx-cursor: hand;");

                // Info VBox
                VBox infoBox = new VBox(4);
                infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                HBox nameRow = new HBox(8);
                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label nameLabel = new Label(user.getUsername());
                nameLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");
                if (isMe) {
                    nameLabel.setText(nameLabel.getText() + " (" + I18N.get("ranking.me") + ")");
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#0f766e", "#2dd4bf") + ";");
                } else {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");
                }
                nameRow.getChildren().addAll(nameLabel);

                String winRateStr = user.getWinRate() != null ? user.getWinRate() : "";
                if (!winRateStr.isEmpty() && !winRateStr.contains("%")) {
                    winRateStr = winRateStr + "%";
                }
                Label detailsLabel = new Label(I18N.get("ranking.scoreWinRate", user.getScore(), winRateStr));
                detailsLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");

                infoBox.getChildren().addAll(nameRow, detailsLabel);

                card.getChildren().addAll(rankBadge, avatar, infoBox);
                rankingContainer.getChildren().add(card);
            }
        });
    }

    @FXML
    public void showGlobalRanking() {
        currentTab = "GLOBAL";
        rankingTitleLabel.setText(I18N.get("ranking.globalTitle"));

        menuGlobalBtn.getStyleClass().remove("active");
        menuFriendsBtn.getStyleClass().remove("active");
        menuGlobalBtn.getStyleClass().add("active");

        loadRankingData();
    }

    @FXML
    public void showFriendsRanking() {
        currentTab = "FRIENDS";
        rankingTitleLabel.setText(I18N.get("ranking.friendTitle"));
        
        menuGlobalBtn.getStyleClass().remove("active");
        menuFriendsBtn.getStyleClass().remove("active");
        menuFriendsBtn.getStyleClass().add("active");

        loadRankingData();
    }

    private void loadRankingData() {
        rankingData.clear();
        if (com.gomatch.client.ClientMain.getConnector().isConnected() && !previewMode) {
            com.gomatch.client.ClientMain.getConnector().send(
                com.gomatch.common.Message.of(com.gomatch.common.Protocol.RANKING, "type", currentTab)
            );
        } else {
            if ("GLOBAL".equals(currentTab)) {
                String[][] fakeData = {
                    {"1", I18N.get("lobby.preview.rank1"),   "1500", "83.3%"},
                    {"2", I18N.get("lobby.preview.rank2"), "1350", "72.7%"},
                    {"3", I18N.get("lobby.preview.rank3"), "1200", "63.6%"},
                    {"4", I18N.get("ranking.preview.player4"), "1050", "44.4%"},
                    {"5", I18N.get("common.guest"),   "1000",  "0%"  }
                };
                for (String[] row : fakeData) {
                    User u = new User();
                    u.setRank(Integer.parseInt(row[0]));
                    u.setUsername(row[1]);
                    u.setScore(Integer.parseInt(row[2]));
                    u.setWinRate(row[3]);
                    rankingData.add(u);
                }
            } else {
                String[][] fakeData = {
                    {"1", I18N.get("ranking.me"),     "1250", "66.7%"},
                    {"2", I18N.get("ranking.preview.friend1"),  "1150", "55.6%"},
                    {"3", I18N.get("ranking.preview.friend2"), "1050", "48.0%"}
                };
                for (String[] row : fakeData) {
                    User u = new User();
                    u.setRank(Integer.parseInt(row[0]));
                    if (I18N.get("ranking.me").equals(row[1]) && currentUser != null && currentUser.getUsername() != null) {
                        u.setUsername(currentUser.getUsername());
                    } else {
                        u.setUsername(row[1]);
                    }
                    u.setScore(Integer.parseInt(row[2]));
                    u.setWinRate(row[3]);
                    rankingData.add(u);
                }
            }
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (previewMode) {
            loadRankingData();
        } else {
            renderRankingCards();
        }
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        loadRankingData();
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
            // 服务端返回 type 字段区分全服排行/好友排行
            String type = msg.getData() != null ? msg.getData().optString("type", "GLOBAL") : "GLOBAL";
            if (!type.equalsIgnoreCase(currentTab)) {
                return;
            }
            parseRankingList(msg);
        } else if (Protocol.ERROR.equals(cmd)) {
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
                // 头像 URL
                user.setAvatar(u.optString("avatar", ""));
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

    /**
     * username -> 渐变底色。JavaFX CSS 不支持 CSS3 语法 `linear-gradient(to bottom right, ...)`，
     * 必须用 `from x1 y1 to x2 y2`。所有色板都使用 明亮 → 中等亮度 的双端饱和色，
     * 避免深色端在小尺寸头像上整体显灰暗。
     */
    private String gradientFor(String username) {
        if (username == null || username.isEmpty()) {
            return "linear-gradient(from 0% 0% to 100% 100%, #60a5fa, #38bdf8)";
        }
        String[] palette = {
            "linear-gradient(from 0% 0% to 100% 100%, #60a5fa, #38bdf8)",  // 天蓝
            "linear-gradient(from 0% 0% to 100% 100%, #fbbf24, #f59e0b)",  // 琥珀
            "linear-gradient(from 0% 0% to 100% 100%, #34d399, #10b981)",  // 翠绿
            "linear-gradient(from 0% 0% to 100% 100%, #f472b6, #ec4899)",  // 粉红
            "linear-gradient(from 0% 0% to 100% 100%, #a78bfa, #8b5cf6)",  // 紫罗兰
            "linear-gradient(from 0% 0% to 100% 100%, #22d3ee, #06b6d4)",  // 青蓝
            "linear-gradient(from 0% 0% to 100% 100%, #f87171, #ef4444)"   // 珊瑚红
        };
        return palette[Math.abs(username.hashCode()) % palette.length];
    }

    private void showAvatarPreview(String avatarUrl, String username) {
        ScreenManager.showDialog("/fxml/AvatarPreviewUI.fxml",
            (AvatarPreviewController ctrl) -> {
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Image image = new Image(avatarUrl, true);
                    ctrl.setAvatar(image, username);
                } else {
                    ctrl.setAvatar(null, username);
                }
            }
        );
    }
}
