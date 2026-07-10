package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LobbyController implements NetworkListener {
    @FXML private Label userInfoLabel;
    @FXML private Label userScoreLabel;
    @FXML private StackPane lobbyAvatarPane;
    @FXML private Label avatarLabel;
    @FXML private ImageView avatarImageView;
    @FXML private VBox rankDrawer;
    @FXML private ListView<RankItem> rankDrawerList;
    @FXML private Button managementBtn;

    /** 排行榜抽屉数据模型 */
    public static class RankItem {
        public final int rank;
        public final String username;
        public final int score;
        public final String avatarUrl;
        public RankItem(int rank, String username, int score, String avatarUrl) {
            this.rank = rank;
            this.username = username;
            this.score = score;
            this.avatarUrl = avatarUrl;
        }
    }

    private final ObservableList<RankItem> drawerRanks = FXCollections.observableArrayList();
    private User currentUser;
    private boolean previewMode;

    @FXML
    private void initialize() {
        rankDrawerList.setItems(drawerRanks);
        // 自定义单元格：渐变小头像 + 排名/用户名 + 分数
        rankDrawerList.setCellFactory(lv -> new ListCell<RankItem>() {
            @Override
            protected void updateItem(RankItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                // 渐变小头像
                StackPane avatar = buildDrawerAvatar(item.username, item.avatarUrl, 28);
                row.getChildren().add(avatar);

                // 中间：排名 + 用户名
                VBox info = new VBox(2);
                info.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

                Label nameLabel = new Label(item.username);
                nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                Label rankLabel = new Label("第 " + item.rank + " 名");
                rankLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                info.getChildren().addAll(nameLabel, rankLabel);
                row.getChildren().add(info);

                // 右侧：分数
                Label scoreLabel = new Label(item.score + " 分");
                scoreLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0ea5e9;");
                row.getChildren().add(scoreLabel);

                setGraphic(row);
            }
        });
        ClientMain.getConnector().addListener(this);
        if (!ClientMain.getConnector().isConnected()) {
            drawerRanks.addAll(
                new RankItem(1, "棋圣", 1500, ""),
                new RankItem(2, "高手如云", 1350, ""),
                new RankItem(3, "五子连珠", 1200, "")
            );
        } else {
            ClientMain.getConnector().send(Protocol.RANKING);
        }
        // 将抽屉高度绑定到父容器，保证浮层铺满纵向
        rankDrawer.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent instanceof Region parent) {
                rankDrawer.prefHeightProperty().bind(parent.heightProperty());
            }
        });
    }

    /** 渐变 + 描边小头像（与原本 .avatar 风格一致），有 url 时显示图片 */
    private StackPane buildDrawerAvatar(String username, String avatarUrl, double size) {
        StackPane pane = new StackPane();
        pane.setPrefSize(size, size);
        pane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        pane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        pane.setAlignment(javafx.geometry.Pos.CENTER);
        pane.setStyle("-fx-background-color: " + gradientFor(username) + ";"
                + "-fx-background-radius: 50%;"
                + "-fx-border-color: rgba(255,255,255,0.85);"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 50%;");

        // 圆形 clip
        Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
        pane.setClip(clip);

        // 先放首字母：图片加载成功就盖住首字母，图片加载失败仍然显示首字母（不会变白板）
        Text fallback = new Text(initialOf(username));
        fallback.setFill(Color.WHITE);
        fallback.setStyle("-fx-font-weight: bold; -fx-font-size: " + (int) (size * 0.45) + "px;");
        pane.getChildren().add(fallback);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            final Image img = new Image(avatarUrl, true);
            final ImageView iv = new ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
            // 图片加载完成后再添加并盖住首字母；失败则保留首字母
            img.progressProperty().addListener((obs, o, n) -> {
                if (n != null && n.doubleValue() >= 1.0 && !img.isError()) {
                    if (!pane.getChildren().contains(iv)) {
                        pane.getChildren().add(iv);
                        iv.toFront();
                    }
                }
            });
            img.errorProperty().addListener((obs, o, n) -> {
                if (n) {
                    // 加载失败 → 不加 iv，保留首字母 fallback（pane 背景为渐变色）
                }
            });
        }
        return pane;
    }

    /** username -> 渐变色（hash 一致）。JavaFX CSS 不支持 CSS3 语法
     *  `linear-gradient(to bottom right, ...)`，必须用 `from x1 y1 to x2 y2` */
    private String gradientFor(String username) {
        if (username == null || username.isEmpty()) return "linear-gradient(from 0% 0% to 100% 100%, #60a5fa, #1e293b)";
        String[] palette = {
            "linear-gradient(from 0% 0% to 100% 100%, #60a5fa, #1e293b)",
            "linear-gradient(from 0% 0% to 100% 100%, #fbbf24, #b45309)",
            "linear-gradient(from 0% 0% to 100% 100%, #34d399, #065f46)",
            "linear-gradient(from 0% 0% to 100% 100%, #f472b6, #9d174d)",
            "linear-gradient(from 0% 0% to 100% 100%, #a78bfa, #5b21b6)",
            "linear-gradient(from 0% 0% to 100% 100%, #22d3ee, #155e75)",
            "linear-gradient(from 0% 0% to 100% 100%, #f87171, #991b1b)"
        };
        return palette[Math.abs(username.hashCode()) % palette.length];
    }

    private String initialOf(String username) {
        return (username == null || username.isEmpty()) ? "U" : username.substring(0, 1).toUpperCase();
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) {
            return;
        }
        String name = user.getUsername();
        userInfoLabel.setText(name);
        if (user.getScore() > 0) {
            userScoreLabel.setText(user.getScore() + " 分");
        } else {
            userScoreLabel.setText("未定级");
        }

        String avatarUrl = user.getAvatarUrl();
        // 顶栏默认渐变 + 描边，与全站小头像风格一致
        lobbyAvatarPane.setStyle("-fx-background-color: " + gradientFor(name) + ";"
                + "-fx-cursor: hand;"
                + "-fx-background-radius: 50%;"
                + "-fx-border-color: rgba(255,255,255,0.85);"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 50%;");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                Image image = new Image(avatarUrl, true);
                image.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        avatarImageView.setVisible(false);
                        avatarLabel.setVisible(true);
                        avatarLabel.setText(initialOf(name));
                    }
                });
                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                        avatarImageView.setImage(image);
                        avatarImageView.setVisible(true);
                        avatarLabel.setVisible(false);
                    }
                });
                if (image.isBackgroundLoading() && image.getProgress() >= 1.0 && !image.isError()) {
                    avatarImageView.setImage(image);
                    avatarImageView.setVisible(true);
                    avatarLabel.setVisible(false);
                } else if (!image.isBackgroundLoading()) {
                    if (!image.isError()) {
                        avatarImageView.setImage(image);
                        avatarImageView.setVisible(true);
                        avatarLabel.setVisible(false);
                    } else {
                        avatarImageView.setVisible(false);
                        avatarLabel.setVisible(true);
                        avatarLabel.setText(initialOf(name));
                    }
                }
            } catch (Exception e) {
                avatarImageView.setVisible(false);
                avatarLabel.setVisible(true);
                avatarLabel.setText(initialOf(name));
            }
        } else {
            avatarImageView.setVisible(false);
            avatarLabel.setVisible(true);
            avatarLabel.setText(initialOf(name));
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            managementBtn.setVisible(false);
            managementBtn.setManaged(false);
        }
    }

    @FXML
    private void toggleRankDrawer() {
        boolean open = !rankDrawer.isVisible();
        rankDrawer.setManaged(open);
        rankDrawer.setVisible(open);
    }

    @FXML
    private void handleAvatarClick(MouseEvent e) {
        e.consume(); // 阻止冒泡到 playerInfoBox 的 handleProfile
        if (currentUser == null) return;
        String url = currentUser.getAvatarUrl();
        String name = currentUser.getUsername();
        showAvatarPreview(url, name);
    }

    @FXML
    private void handleProfile() {
        ProfileController ctrl = ScreenManager.switchTo("/fxml/ProfileUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleCasual() {
        CasualLobbyController ctrl = ScreenManager.switchTo("/fxml/CasualLobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleRanked() {
        RankedLobbyController ctrl = ScreenManager.switchTo("/fxml/RankedLobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleAiMatch() {
        AiLobbyController ctrl = ScreenManager.switchTo("/fxml/AiLobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleFriends() {
        FriendsController ctrl = ScreenManager.switchTo("/fxml/FriendsUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleRankingPage() {
        RankingController ctrl = ScreenManager.switchTo("/fxml/RankingUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleManagement() {
        ManagementController ctrl = ScreenManager.switchTo("/fxml/ManagementUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }
    @FXML
    private void handleSettings() {
        SettingsController ctrl = ScreenManager.switchTo("/fxml/SettingsUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    @FXML
    private void handleAbout() {
        ScreenManager.switchTo("/fxml/AboutUI.fxml");
    }

    @Override
    public void onMessage(Message msg) {
        if (Protocol.RANKING_LIST.equals(msg.getCmd())) {
            JSONArray users = msg.getData().optJSONArray("users");
            if (users != null) {
                List<RankItem> items = new ArrayList<>();
                for (int i = 0; i < users.length(); i++) {
                    JSONObject u = users.optJSONObject(i);
                    if (u == null) continue;
                    int rank = u.optInt("rank", i + 1);
                    String username = u.optString("username", "");
                    int score = u.optInt("score", 0);
                    String avatarUrl = u.optString("avatarUrl", "");
                    items.add(new RankItem(rank, username, score, avatarUrl));
                }
                drawerRanks.setAll(items);
            }
        } else if (Protocol.USER_SCORE_UPDATE.equals(msg.getCmd())) {
            if (currentUser != null) {
                currentUser.setScore(msg.getInt("score"));
                currentUser.setWinCount(msg.getInt("winCount"));
                currentUser.setLoseCount(msg.getInt("loseCount"));
                currentUser.setDrawCount(msg.getInt("drawCount"));
                currentUser.setRank(msg.getInt("rank"));
                if (currentUser.getScore() > 0) {
                    userScoreLabel.setText(currentUser.getScore() + " 分");
                } else {
                    userScoreLabel.setText("未定级");
                }
            }
        }
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
