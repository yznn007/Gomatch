package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoardController implements NetworkListener {
    private static final int SIZE   = 15;
    private static final int MARGIN = 20;

    @FXML private BorderPane rootPane;
    @FXML private StackPane boardContainer;
    @FXML private VBox blackPlayerBox;
    @FXML private VBox whitePlayerBox;
    @FXML private Label turnLabel;
    @FXML private Label timerLabel;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private Button undoBtn;
    @FXML private Button drawBtn;
    @FXML private Button surrenderBtn;
    @FXML private Button saveBtn;
    @FXML private VBox gameControls;
    @FXML private VBox replayControls;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Button exitBtn;
    @FXML private Button toggleSidebarBtn;
    @FXML private VBox sidebarContent;
    @FXML private Region dragHandle;
    @FXML private VBox spectatorControls;
    @FXML private Button leaveSpectateBtn;

    private double dragStartX;
    private double dragStartWidth;
    private boolean sidebarCollapsed = false;
    private double sidebarRatio = -1.0;

    private Canvas canvas;
    private GraphicsContext gc;
    private final int[][] board = new int[SIZE][SIZE];
    private int currentTurn = 1; // 1=black, 2=white
    private boolean myTurn;
    private String myColor  = "black"; // 防止 NPE，给默认值
    private String myUsername;
    private String blackName = "";
    private String whiteName = "";
    private String blackId   = "";
    private String whiteId   = "";
    private boolean previewMode;
    private boolean aiMode;
    private boolean spectatorMode;
    private boolean replayMode;
    private boolean gameOver;
    private Stage gameOverDialogStage;   // 游戏结束弹窗引用，用于再来一局时关闭
    private boolean pendingRequest = false; // 是否有待处理的悔棋/求和请求
    private Stage pendingRequestStage; // 等待对方回复的弹窗引用
    private int cellSize; // 动态计算
    private final List<int[]> moveHistory = new ArrayList<>();
    private final List<int[]> replayMoveList = new ArrayList<>();
    private int replayIndex = 0;
    private String lastLocalChat;
    private long lastLocalChatTime;
    private int lastMoveX = -1;
    private int lastMoveY = -1;

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }



    private Label blackPlayerNameLabel;
    private Label whitePlayerNameLabel;
    private Label blackPlayerIdLabel;
    private Label whitePlayerIdLabel;
    private Label blackPlayerScoreLabel;
    private Label whitePlayerScoreLabel;
    private javafx.scene.layout.Pane blackCardRoot;
    private javafx.scene.layout.Pane whiteCardRoot;
    private Label blackOfflineTag;
    private Label whiteOfflineTag;
    private Label blackSelfTag;
    private Label whiteSelfTag;

    // 双方头像组件引用
    private ImageView blackAvatarImageView;
    private ImageView whiteAvatarImageView;
    private Label blackAvatarText;
    private Label whiteAvatarText;
    private StackPane blackAvatarPane;
    private StackPane whiteAvatarPane;
    private javafx.scene.image.Image blackAvatarImage;  // 防止 GC
    private javafx.scene.image.Image whiteAvatarImage;  // 防止 GC

    // 本地倒计时
    private Timeline countdownTimeline;
    private int remainingSeconds;

    @FXML
    private void initialize() {
        ClientMain.getConnector().addListener(this);
        
        // 延迟到下一帧 runLater：避免在 JavaFX 布局脉冲中同步 setWidth/setHeight
        // → 触发新一轮 boardContainer 尺寸变化 → listener 再次进入 resizeBoard 的连锁循环。
        boardContainer.widthProperty().addListener((obs, o, n) -> Platform.runLater(this::resizeBoard));
        boardContainer.heightProperty().addListener((obs, o, n) -> Platform.runLater(this::resizeBoard));

        canvas = new Canvas(1, 1) {
            @Override
            public boolean isResizable() {
                return true;
            }
            @Override
            public double minWidth(double height) {
                return 10;
            }
            @Override
            public double minHeight(double width) {
                return 10;
            }
            @Override
            public double prefWidth(double height) {
                return 100;
            }
            @Override
            public double prefHeight(double width) {
                return 100;
            }
            @Override
            public double maxWidth(double height) {
                return 4096;
            }
            @Override
            public double maxHeight(double width) {
                return 4096;
            }
        };
        gc = canvas.getGraphicsContext2D();
        boardContainer.getChildren().add(canvas);

        canvas.setOnMouseClicked(this::handleMouseClick);
        ClientMain.getConnector().addListener(this);

        buildPlayerCards();

        // 双向拖拽拉伸侧边栏
        dragHandle.setOnMousePressed(e -> {
            dragStartX = e.getScreenX();
            dragStartWidth = sidebarContent.getWidth();
        });
        dragHandle.setOnMouseDragged(e -> {
            double totalW = rootPane.getWidth() - 60;
            if (totalW <= 0) return;
            double deltaX = e.getScreenX() - dragStartX;
            double newWidth = dragStartWidth - deltaX;
            newWidth = clampSidebarWidth(newWidth, totalW);
            sidebarContent.setPrefWidth(newWidth);
            sidebarContent.setMinWidth(newWidth);
            sidebarContent.setMaxWidth(newWidth);
            sidebarRatio = newWidth / totalW;
        });

        // 窗口拉伸时，侧边栏基于比例自适应调整。
        // **不要监听 boardContainer.heightProperty**：adjustSidebarSize 设置 sidebar 宽度
        // → BorderPane 重布局 → boardContainer 宽高变化 → heightProperty 回调
        // adjustSidebarSize 再次设置 sidebar 宽度，形成跨帧反馈循环，导致"棋盘放大+不停抖动"
        rootPane.widthProperty().addListener((obs, o, n) -> adjustSidebarSize());
    }

    /** 根据容器尺寸重新计算 cellSize 并重绘全盘 */
    private void resizeBoard() {
        double w = boardContainer.getWidth();
        double h = boardContainer.getHeight();
        double available = Math.min(w, h) - MARGIN * 2;
        if (available <= 0) return;
        cellSize = (int) (available / (SIZE - 1));
        if (cellSize <= 0) return;
        int boardPx = (SIZE - 1) * cellSize + MARGIN * 2;
        if (boardPx <= 0) return;
        double curW = canvas.getWidth();
        double curH = canvas.getHeight();
        if (Math.abs(curW - boardPx) < 1 && Math.abs(curH - boardPx) < 1) return;
        canvas.setWidth(boardPx);
        canvas.setHeight(boardPx);
        redrawAll();
    }

    /** 重绘棋盘格线 + 已落的所有棋子 */
    private void redrawAll() {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;
        drawBoard();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (board[x][y] != 0) {
                    drawStoneOnCanvas(x, y, board[x][y]);
                }
            }
        }
        drawLastMoveMarker();
    }

    public void setGameInfo(String blackId, String whiteId, String username) {
        // MATCH_FOUND 阶段还不知道 displayName/分数，用 ID 当占位（不渲染到 UI 上）
        setGameInfo(blackId, null, whiteId, null, username, -1, -1);
    }

    public void setGameInfo(String blackId, String blackDisplayName, String whiteId, String whiteDisplayName,
                            String username, int blackScore, int whiteScore) {
        this.setGameInfo(blackId, blackDisplayName, whiteId, whiteDisplayName, username);
        // 补充分数（4 参数版已设 -1 占位为"?"，这里用实参覆盖；GAME_START 不会传 -1）
        if (blackPlayerScoreLabel != null) {
            if (blackScore >= 0) {
                blackPlayerScoreLabel.setText(blackScore + "分");
                blackPlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-weight: bold;");
            } else if (blackDisplayName == null) {
                blackPlayerScoreLabel.setText("?");
                blackPlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#94a3b8") + ";");
            }
        }
        if (whitePlayerScoreLabel != null) {
            if (whiteScore >= 0) {
                whitePlayerScoreLabel.setText(whiteScore + "分");
                whitePlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-weight: bold;");
            } else if (whiteDisplayName == null) {
                whitePlayerScoreLabel.setText("?");
                whitePlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#94a3b8") + ";");
            }
        }
    }

    private void setGameInfo(String blackId, String blackDisplayName, String whiteId, String whiteDisplayName, String username) {
        // 形参 displayName 为 null 时用 ID 作占位（MATCH_FOUND 阶段无完整信息）
        this.blackName  = (blackDisplayName != null && !blackDisplayName.isEmpty()) ? blackDisplayName : blackId;
        this.whiteName  = (whiteDisplayName != null && !whiteDisplayName.isEmpty()) ? whiteDisplayName : whiteId;
        this.blackId    = blackId != null ? blackId : "";
        this.whiteId    = whiteId != null ? whiteId : "";
        this.myUsername = username;
        this.aiMode = "AI".equals(whiteDisplayName) || "-1".equals(whiteId);
        this.gameOver = false;
        AppConfig.setAiMode(this.aiMode);

        User cu = AppConfig.getCurrentUser();
        String myIdStr = cu != null ? String.valueOf(cu.getUserId()) : "";
        int myScore = cu != null ? cu.getScore() : 0;

        boolean isBlack = false;
        this.spectatorMode = false;
        if (cu != null) {
            isBlack = myIdStr.equals(blackId) || (username != null && username.equals(blackId));
            // 观战检测：当前用户既不是黑方也不是白方
            if (!isBlack && !myIdStr.equals(whiteId)
                    && !(username != null && username.equals(whiteId))) {
                this.spectatorMode = true;
            }
        } else if (username != null) {
            isBlack = username.equals(blackId);
            if (!isBlack && !username.equals(whiteId)) {
                this.spectatorMode = true;
            }
        }

        // 双方玩家卡片都按实参的 displayName / id / score 设置。
        // 对于"自己"那一方，如果 displayName 还没拿到（GAME_START 未到），用 myUsername（自己登录名）作为可读占位。
        String blackDisplay = this.blackName;
        String whiteDisplay = this.whiteName;
        if (username != null && !username.isEmpty()) {
            if (myIdStr.equals(this.blackId) || (cu == null && username.equals(this.blackId))) {
                blackDisplay = username;
            }
            if (myIdStr.equals(this.whiteId) || (cu == null && username.equals(this.whiteId))) {
                whiteDisplay = username;
            }
        }
        boolean isBlackForScore = myIdStr.equals(this.blackId) || (username != null && username.equals(this.blackId));
        boolean isWhiteForScore = myIdStr.equals(this.whiteId) || (username != null && username.equals(this.whiteId));
        setPlayerCardLabels(blackPlayerNameLabel, blackPlayerIdLabel, blackPlayerScoreLabel,
                blackDisplay, this.blackId, isBlackForScore ? myScore : -1);
        setPlayerCardLabels(whitePlayerNameLabel, whitePlayerIdLabel, whitePlayerScoreLabel,
                whiteDisplay, this.whiteId, isWhiteForScore ? myScore : -1);

        myColor = isBlack ? "black" : "white";
        myTurn  = isBlack;
        updateTurnLabel();

        // 观战模式：禁用落子和游戏操作
        if (spectatorMode) {
            myTurn = false;
            myColor = "spectator";
            applySpectatorMode();
        } else {
            if (spectatorControls != null) {
                spectatorControls.setVisible(false);
                spectatorControls.setManaged(false);
            }
        }
        updateActionButtons();

        if (!replayMode) {
            startCountdown();
        }
        updatePlayerCardSelfMark();
    }

    /**
     * 在 setGameInfo 之后调用，根据 myColor 给"自己"那方的卡片增加 self 标记（蓝色徽章 + 边框高亮）。
     * 观战/未登录时清空双方 self 标记。
     */
    private void updatePlayerCardSelfMark() {
        boolean isMyBlack = "black".equals(myColor);
        boolean isMyWhite = "white".equals(myColor);
        applySelfMark(blackCardRoot, blackSelfTag, isMyBlack);
        applySelfMark(whiteCardRoot, whiteSelfTag, isMyWhite);
    }

    private void applySelfMark(javafx.scene.layout.Pane cardRoot, Label selfTag, boolean isSelf) {
        if (cardRoot != null) {
            cardRoot.getStyleClass().remove("player-info-card-self");
            if (isSelf) cardRoot.getStyleClass().add("player-info-card-self");
        }
        if (selfTag != null) {
            selfTag.setVisible(isSelf);
            selfTag.setManaged(isSelf);
        }
    }

    private void applySpectatorMode() {
        if (gameControls != null) {
            gameControls.setVisible(false);
            gameControls.setManaged(false);
        }
        if (chatInput != null) {
            chatInput.setVisible(false);
            chatInput.setManaged(false);
        }
        if (spectatorControls != null) {
            spectatorControls.setVisible(true);
            spectatorControls.setManaged(true);
        }
        if (chatArea != null) {
            chatArea.appendText("[系统] 观战模式 - 无法落子或操作\n");
        }
    }

    /**
     * 断线后重开客户端登录时恢复对局。
     * 由 LoginController 在收到携带 roomId 的 LOGIN_OK 后调用。
     */
    public void resumeFromLogin(String blackUserId, String whiteUserId, String username,
                                 int currentColor, String disconnectedUserId) {
        resumeFromLogin(blackUserId, whiteUserId, username, currentColor, disconnectedUserId, "", "");
    }

    /**
     * 断线后重开客户端登录时恢复对局（带头像 URL）。由 LoginController 在收到携带 roomId 的 LOGIN_OK 后调用。
     */
    public void resumeFromLogin(String blackUserId, String whiteUserId, String username,
                                 int currentColor, String disconnectedUserId,
                                 String blackAvatarUrl, String whiteAvatarUrl) {
        this.blackId = blackUserId != null ? blackUserId : "";
        this.whiteId = whiteUserId != null ? whiteUserId : "";
        this.blackName = blackUserId != null ? blackUserId : "黑方";
        this.whiteName = whiteUserId != null ? whiteUserId : "白方";
        this.myUsername = username;
        this.gameOver = false;
        this.spectatorMode = false;
        this.previewMode = false;
        this.aiMode = false;
        this.moveHistory.clear();

        User cu = AppConfig.getCurrentUser();
        String myIdStr = cu != null ? String.valueOf(cu.getUserId()) : "";
        int myScore = cu != null ? cu.getScore() : 0;

        boolean isBlack = myIdStr.equals(blackUserId)
                || (username != null && username.equals(blackUserId));

        if (isBlack) {
            setPlayerCardLabels(blackPlayerNameLabel, blackPlayerIdLabel, blackPlayerScoreLabel,
                    username != null ? username : blackUserId, myIdStr, myScore);
            setPlayerCardLabels(whitePlayerNameLabel, whitePlayerIdLabel, whitePlayerScoreLabel,
                    whiteUserId, whiteUserId, -1);
        } else {
            setPlayerCardLabels(blackPlayerNameLabel, blackPlayerIdLabel, blackPlayerScoreLabel,
                    blackUserId, blackUserId, -1);
            setPlayerCardLabels(whitePlayerNameLabel, whitePlayerIdLabel, whitePlayerScoreLabel,
                    username != null ? username : whiteUserId, myIdStr, myScore);
        }

        myColor = isBlack ? "black" : "white";
        this.currentTurn = currentColor;
        myTurn = (currentColor == 1 && isBlack) || (currentColor == 2 && !isBlack);

        updateTurnLabel();
        updateActionButtons();
        // 断线重连后也必须刷新"你"标记（蓝色 self 徽章 + 左边框高亮），
        // 否则走 resumeFromLogin 路径的玩家卡片没有 self 标识，无法一眼看出自己是黑方/白方。
        updatePlayerCardSelfMark();

        if (spectatorControls != null) {
            spectatorControls.setVisible(false);
            spectatorControls.setManaged(false);
        }

        // 处理断线角标：如果对手仍在断线中，显示断线标记
        if (disconnectedUserId != null && !disconnectedUserId.isEmpty()
                && !"0".equals(disconnectedUserId)) {
            boolean opponentIsBlack = disconnectedUserId.equals(blackUserId);
            setPlayerOnline(opponentIsBlack, false);
            String opponentName = opponentIsBlack ? blackName : whiteName;
            chatArea.appendText("[系统] 对手 " + opponentName + " 已断线，等待重连...\n");
        }

        chatArea.appendText("[系统] 已恢复对局\n");
        chatArea.setScrollTop(Double.MAX_VALUE);

        // 恢复双方头像（断线重连后不会收到 GAME_START 推送，必须主动注入）
        if (blackAvatarPane != null && whiteAvatarPane != null) {
            setPlayerAvatars(blackAvatarUrl, whiteAvatarUrl);
        }

        if (!replayMode) {
            startCountdown();
        }
    }

    private void setPlayerCardLabels(Label nameLbl, Label idLbl, Label scoreLbl,
                                      String name, String id, int score) {
        if (nameLbl != null) nameLbl.setText(name);
        if (idLbl != null) idLbl.setText(id.isEmpty() ? "#---" : "#" + id);
        if (scoreLbl != null) {
            if (score >= 0) {
                scoreLbl.setText(score + "分");
                scoreLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-weight: bold;");
            } else {
                scoreLbl.setText("?");
                scoreLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#94a3b8") + ";");
            }
        }
    }

    /**
     * 更新单个玩家的头像：有头像 URL 时加载图片，加载完成显示图片；否则显示首字母 fallback。
     * @param pane         头像 StackPane
     * @param textLabel    首字母 Label（无图片时显示）
     * @param imageView    头像 ImageView
     * @param avatarUrl    头像 URL（空字符串表示无头像）
     * @param isBlack      true 为黑方（存储引用防止 GC）
     */
    private void updatePlayerAvatar(StackPane pane, Label textLabel, ImageView imageView, String avatarUrl, boolean isBlack) {
        if (pane == null || textLabel == null || imageView == null) {
            return;
        }
        // 默认显示首字母，等图片加载完成后再隐藏
        textLabel.setVisible(true);
        imageView.setVisible(false);
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }
        try {
            javafx.scene.image.Image image = new javafx.scene.image.Image(avatarUrl, true);
            // 存储引用防止 GC
            if (isBlack) {
                this.blackAvatarImage = image;
            } else {
                this.whiteAvatarImage = image;
            }
            image.errorProperty().addListener((obs, o, n) -> {
                if (n) {
                    Platform.runLater(() -> {
                        imageView.setVisible(false);
                        textLabel.setVisible(true);
                    });
                }
            });
            image.progressProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() >= 1.0 && !image.isError()) {
                    Platform.runLater(() -> {
                        imageView.setImage(image);
                        imageView.setVisible(true);
                        textLabel.setVisible(false);
                    });
                }
            });
            // 缓存命中：后台加载完成但 progress 已是 1.0，监听器不会触发
            if (image.isBackgroundLoading() && image.getProgress() >= 1.0 && !image.isError()) {
                imageView.setImage(image);
                imageView.setVisible(true);
                textLabel.setVisible(false);
            } else if (!image.isBackgroundLoading() && !image.isError()) {
                // 同步加载立即完成
                imageView.setImage(image);
                imageView.setVisible(true);
                textLabel.setVisible(false);
            }
        } catch (Exception e) {
            // 仅首字母可见
        }
    }

    /**
     * 供外部 Controller（AiLobby / CasualLobby / RankedLobby / RoomWaiting）在切换场景时
     * 主动注入双方头像 URL。因为 CopyOnWriteArrayList 快照迭代导致 BoardController 在
     * switchTo 期间注册为监听器后，无法收到同一条 GAME_START 消息，必须通过此方法显式传递。
     */
    public void setPlayerAvatars(String blackAvatarUrl, String whiteAvatarUrl) {
        updatePlayerAvatar(blackAvatarPane, blackAvatarText, blackAvatarImageView, blackAvatarUrl, true);
        updatePlayerAvatar(whiteAvatarPane, whiteAvatarText, whiteAvatarImageView, whiteAvatarUrl, false);
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            // 预览时默认黑方，设置好 myColor 再操作
            myColor    = "black";
            blackName  = "黑方";
            whiteName  = "白方";
            myTurn     = true;
            // 等 canvas 完成布局后才能画棋子
            boardContainer.widthProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() > 0 && board[3][3] == 0) {
                    drawStone(3, 3, 1);
                    drawStone(3, 4, 2);
                    drawStone(4, 4, 1);
                    drawStone(4, 5, 2);
                    drawStone(5, 5, 1);
                    lastMoveX = 5;
                    lastMoveY = 5;
                    currentTurn = 2;
                    updateTurnLabel();
                }
            });
            chatArea.appendText("[系统] 预览模式 - 点击棋盘可本地落子\n");
            chatArea.appendText("[系统] 点击认输可查看结算页面\n");
        }
    }

    public void setReplayMode(boolean replayMode) {
        setReplayMode(replayMode, null, null, null, null);
    }

    /**
     * 设置回放模式（带玩家信息）。
     * @param blackId      黑方用户ID（可为 null，用占位符）
     * @param blackName    黑方显示名（可为 null）
     * @param whiteId      白方用户ID
     * @param whiteName    白方显示名
     */
    public void setReplayMode(boolean replayMode,
                              String blackId, String blackName,
                              String whiteId, String whiteName) {
        setReplayMode(replayMode, blackId, blackName, whiteId, whiteName, -1, -1, null, null);
    }

    /**
     * 设置回放模式（带玩家信息、积分与头像 URL）。
     * @param blackId       黑方用户ID（可为 null，用占位符）
     * @param blackName     黑方显示名（可为 null）
     * @param whiteId       白方用户ID
     * @param whiteName     白方显示名
     * @param blackScore    黑方积分（-1表示未知）
     * @param whiteScore    白方积分（-1表示未知）
     * @param blackAvatarUrl 黑方头像 URL（null/空表示用首字母占位）
     * @param whiteAvatarUrl 白方头像 URL（null/空表示用首字母占位）
     */
    public void setReplayMode(boolean replayMode,
                              String blackId, String blackName,
                              String whiteId, String whiteName,
                              int blackScore, int whiteScore,
                              String blackAvatarUrl, String whiteAvatarUrl) {
        setReplayMode(replayMode, blackId, blackName, whiteId, whiteName, blackScore, whiteScore);
        // 同步设置双方头像（来自 record 中的 player1Avatar/player2Avatar）
        setPlayerAvatars(blackAvatarUrl, whiteAvatarUrl);
    }

    /**
     * 设置回放模式（带玩家信息和积分）。
     * @param blackId      黑方用户ID（可为 null，用占位符）
     * @param blackName    黑方显示名（可为 null）
     * @param whiteId      白方用户ID
     * @param whiteName    白方显示名
     * @param blackScore   黑方积分（-1表示未知）
     * @param whiteScore   白方积分（-1表示未知）
     */
    public void setReplayMode(boolean replayMode,
                              String blackId, String blackName,
                              String whiteId, String whiteName,
                              int blackScore, int whiteScore) {
        this.replayMode = replayMode;
        if (replayMode) {
            gameOver = true;
            myTurn = false;
            // 兜底：名字为空时回退到 ID，再不行用"黑方/白方"
            this.blackName = (blackName != null && !blackName.isEmpty()) ? blackName : (isEmpty(blackId) ? "黑方" : blackId);
            this.whiteName = (whiteName != null && !whiteName.isEmpty()) ? whiteName : (isEmpty(whiteId) ? "白方" : whiteId);
            this.blackId = blackId != null ? blackId : "";
            this.whiteId = whiteId != null ? whiteId : "";
            myColor = "black";
            currentTurn = 1;
            if (blackPlayerNameLabel != null) blackPlayerNameLabel.setText(this.blackName);
            if (whitePlayerNameLabel != null) whitePlayerNameLabel.setText(this.whiteName);
            if (blackPlayerIdLabel != null) blackPlayerIdLabel.setText(this.blackId.isEmpty() ? "#---" : "#" + this.blackId);
            if (whitePlayerIdLabel != null) whitePlayerIdLabel.setText(this.whiteId.isEmpty() ? "#---" : "#" + this.whiteId);
            if (blackPlayerScoreLabel != null) {
                if (blackScore >= 0) {
                    blackPlayerScoreLabel.setText(blackScore + "分");
                    blackPlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-weight: bold;");
                } else {
                    blackPlayerScoreLabel.setText("?");
                    blackPlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + ";");
                }
            }
            if (whitePlayerScoreLabel != null) {
                if (whiteScore >= 0) {
                    whitePlayerScoreLabel.setText(whiteScore + "分");
                    whitePlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-weight: bold;");
                } else {
                    whitePlayerScoreLabel.setText("?");
                    whitePlayerScoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#94a3b8", "#64748b") + ";");
                }
            }
            // 回放模式无倒计时：清空 timerLabel 并停止任何 timeline
            if (timerLabel != null) {
                timerLabel.setText("");
                timerLabel.setVisible(false);
                timerLabel.setManaged(false);
            }
            updateTurnLabel();

            if (gameControls != null) {
                gameControls.setVisible(false);
                gameControls.setManaged(false);
            }
            if (replayControls != null) {
                replayControls.setVisible(true);
                replayControls.setManaged(true);
            }
            if (spectatorControls != null) {
                spectatorControls.setVisible(false);
                spectatorControls.setManaged(false);
            }
            if (chatInput != null) {
                chatInput.setVisible(false);
                chatInput.setManaged(false);
            }
            chatArea.appendText("[系统] 棋谱回放模式\n");
            stopCountdown();
        }
    }

    public void loadSavedGame(String saveName, String boardData, int savedCurrentPlayer) {
        setReplayMode(true);
        currentTurn = savedCurrentPlayer == 2 ? 2 : 1;
        replayMoves(boardData);
        currentTurn = savedCurrentPlayer == 2 ? 2 : 1;
        updateTurnLabel();
        if (chatArea != null) {
            chatArea.appendText("[系统] 已打开存档：" + (saveName == null || saveName.isBlank() ? "未命名棋局" : saveName) + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        }
    }
    private void handleMouseClick(MouseEvent e) {
        if (replayMode || gameOver || spectatorMode || pendingRequest) return;
        if (!myTurn) return;
        if (cellSize == 0) return;
        int x = (int) Math.round((e.getX() - MARGIN) / cellSize);
        int y = (int) Math.round((e.getY() - MARGIN) / cellSize);
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        if (board[x][y] != 0) return;

        if (previewMode) {
            int color = currentTurn;
            drawStone(x, y, color);
            moveHistory.add(new int[]{x, y, color});
            currentTurn = (color == 1) ? 2 : 1;
            updateTurnLabel();
            chatArea.appendText("[本地] " + (color == 1 ? "黑" : "白") + "方落子 (" + x + "," + y + ")\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            return;
        }
        int color = myColor.equals("black") ? 1 : 2;
        drawStone(x, y, color);
        myTurn = false;
        updateTurnLabel();
        updateActionButtons();
        ClientMain.getConnector().send(Message.of(Protocol.MOVE, "x", x, "y", y));
    }

    private void drawStone(int x, int y, int color) {
        board[x][y] = color;
        lastMoveX = x;
        lastMoveY = y;
        redrawAll();
    }

    private void drawStoneOnCanvas(int x, int y, int color) {
        if (cellSize == 0) return;
        double cx     = MARGIN + x * cellSize;
        double cy     = MARGIN + y * cellSize;
        double radius = cellSize / 2.0 - 2;

        // 高光点偷移到左上方，模拟球体光泽
        double hlX = cx - radius * 0.32;
        double hlY = cy - radius * 0.32;

        javafx.scene.paint.RadialGradient gradient;
        if (color == 1) {
            // 黑子：深灰 → 纯黑
            gradient = new javafx.scene.paint.RadialGradient(
                0, 0, hlX, hlY, radius * 1.1,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.rgb(90, 90, 90)),
                new javafx.scene.paint.Stop(0.5, Color.rgb(30, 30, 30)),
                new javafx.scene.paint.Stop(1, Color.rgb(5, 5, 5))
            );
        } else {
            // 白子：纯白 → 浅蓝灰
            gradient = new javafx.scene.paint.RadialGradient(
                0, 0, hlX, hlY, radius * 1.1,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.WHITE),
                new javafx.scene.paint.Stop(0.6, Color.rgb(230, 235, 245)),
                new javafx.scene.paint.Stop(1, Color.rgb(195, 208, 222))
            );
        }

        // 外轮阴影（先画稍大的半透明圆）
        gc.setFill(Color.rgb(0, 0, 0, 0.18));
        gc.fillOval(cx - radius + 1.5, cy - radius + 2.5, radius * 2, radius * 2);

        // 主体
        gc.setFill(gradient);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // 轮廓线
        gc.setStroke(color == 1 ? Color.rgb(0, 0, 0, 0.85) : Color.rgb(160, 175, 190, 0.90));
        gc.setLineWidth(0.8);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        gc.setLineWidth(1.0); // 恢复默认
    }

    private void drawLastMoveMarker() {
        if (lastMoveX < 0 || lastMoveY < 0 || lastMoveX >= SIZE || lastMoveY >= SIZE || cellSize == 0) return;
        if (board[lastMoveX][lastMoveY] == 0) return;

        double cx = MARGIN + lastMoveX * cellSize;
        double cy = MARGIN + lastMoveY * cellSize;
        double radius = cellSize / 2.0 - 2;
        double markerRadius = radius + 3.5;

        gc.setStroke(Color.rgb(220, 38, 38, 0.95));
        gc.setLineWidth(2.8);
        gc.strokeOval(cx - markerRadius, cy - markerRadius, markerRadius * 2, markerRadius * 2);
        gc.setLineWidth(1.0);
    }
    private void drawBoard() {
        if (cellSize == 0) return;
        double cw = canvas.getWidth();
        double ch = canvas.getHeight();
        if (cw <= 0 || ch <= 0) return;
        gc.clearRect(0, 0, cw, ch);

        int boardPx = (SIZE - 1) * cellSize + MARGIN * 2;
        gc.setFill(Color.rgb(222, 184, 135));
        gc.fillRect(0, 0, boardPx, boardPx);

        gc.setStroke(Color.BLACK);
        for (int i = 0; i < SIZE; i++) {
            gc.strokeLine(MARGIN, MARGIN + i * cellSize, MARGIN + (SIZE - 1) * cellSize, MARGIN + i * cellSize);
            gc.strokeLine(MARGIN + i * cellSize, MARGIN, MARGIN + i * cellSize, MARGIN + (SIZE - 1) * cellSize);
        }

        int[][] starPoints = {{3,3},{3,11},{7,7},{11,3},{11,11}};
        gc.setFill(Color.BLACK);
        for (int[] p : starPoints) {
            gc.fillOval(MARGIN + p[0] * cellSize - 3, MARGIN + p[1] * cellSize - 3, 6, 6);
        }
    }

    private void updateTurnLabel() {
        if (blackName == null) blackName = I18N.isEnglish() ? "Black" : "黑方";
        if (whiteName == null) whiteName = I18N.isEnglish() ? "White" : "白方";
        if (I18N.isEnglish()) {
            String turnSuffix = "'s Turn";
            turnLabel.setText(currentTurn == 1
                    ? "[Black] " + blackName + turnSuffix
                    : "[White] " + whiteName + turnSuffix);
        } else {
            turnLabel.setText(currentTurn == 1
                    ? "【黑方】" + blackName + " 回合"
                    : "【白方】" + whiteName + " 回合");
        }
        updatePlayerCardHighlight();
    }

    private void updatePlayerCardHighlight() {
        boolean blackActive = currentTurn == 1;
        if (blackCardRoot != null) {
            blackCardRoot.getStyleClass().remove("player-info-card-active");
            if (blackActive) blackCardRoot.getStyleClass().add("player-info-card-active");
        }
        if (whiteCardRoot != null) {
            whiteCardRoot.getStyleClass().remove("player-info-card-active");
            if (!blackActive) whiteCardRoot.getStyleClass().add("player-info-card-active");
        }
    }

    private void buildPlayerCards() {
        blackCardRoot = buildOnePlayerCard(blackPlayerBox, "⚫", "黑方", ScreenManager.themeColor("#1e293b", "#f8fafc"));
        whiteCardRoot = buildOnePlayerCard(whitePlayerBox, "⚪", "白方", ScreenManager.themeColor("#cbd5e1", "#cbd5e1"));
    }

    private javafx.scene.layout.Pane buildOnePlayerCard(VBox container, String stone, String colorLabel, String avatarBg) {
        if (container == null) return null;

        boolean isBlack = "黑方".equals(colorLabel);

        final double AVATAR_SIZE = 28.0;
        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        avatar.getStyleClass().add("avatar-badge");
        avatar.setStyle("-fx-background-color: " + avatarBg + "; -fx-min-width: " + AVATAR_SIZE + "px; -fx-max-width: "
                + AVATAR_SIZE + "px; -fx-min-height: " + AVATAR_SIZE + "px; -fx-max-height: " + AVATAR_SIZE + "px;");
        javafx.scene.control.Label avatarText = new javafx.scene.control.Label(colorLabel.substring(0, 1));
        avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        avatarText.getStyleClass().add("avatar-badge-text");
        avatarText.setStyle("-fx-font-size: 12px;");

        // 真实头像 ImageView（默认隐藏，加载成功后才显示）
        ImageView avatarImageView = new ImageView();
        avatarImageView.setFitWidth(AVATAR_SIZE);
        avatarImageView.setFitHeight(AVATAR_SIZE);
        avatarImageView.setPreserveRatio(true);
        avatarImageView.setVisible(false);
        // 用圆角 Rectangle 替代 Circle 作为 clip，避免 JavaFX 21 BoxBounds ClassCastException
        javafx.scene.shape.Rectangle imgClip = new javafx.scene.shape.Rectangle(AVATAR_SIZE, AVATAR_SIZE);
        imgClip.setArcWidth(AVATAR_SIZE);
        imgClip.setArcHeight(AVATAR_SIZE);
        avatarImageView.setClip(imgClip);

        avatar.getChildren().addAll(avatarText, avatarImageView);

        // 保存引用
        if (isBlack) {
            blackAvatarPane = avatar;
            blackAvatarText = avatarText;
            blackAvatarImageView = avatarImageView;
        } else {
            whiteAvatarPane = avatar;
            whiteAvatarText = avatarText;
            whiteAvatarImageView = avatarImageView;
        }

        VBox infoBox = new VBox(2);
        infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        // infoBox 现在放在 Pane cardPane 中（绝对定位），不再用 HBox.setHgrow
        // （HBox.setHgrow 是 HBox 的静态方法，要求 child 是 HBox 的子节点；这里 infoBox 在 Pane 里）

        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label("---");
        nameLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");

        javafx.scene.layout.HBox detailRow = new javafx.scene.layout.HBox(6);
        detailRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.Label idLabel = new javafx.scene.control.Label("#---");
        idLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#94a3b8") + ";");

        javafx.scene.control.Label scoreLabel = new javafx.scene.control.Label("---");
        scoreLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        scoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + "; -fx-font-weight: bold;");

        javafx.scene.control.Label roleLabel = new javafx.scene.control.Label(colorLabel);
        roleLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ("黑方".equals(colorLabel) ? ScreenManager.themeColor("#1e293b", "#f8fafc") : ScreenManager.themeColor("#94a3b8", "#64748b")) + ";");

        detailRow.getChildren().addAll(idLabel, scoreLabel, roleLabel);
        infoBox.getChildren().addAll(nameLabel, detailRow);

        // 断线角标（默认隐藏）。
        // 之前用 #fef2f2 浅红 + #dc2626 文字在浅色背景下几乎不可见，
        // 改用饱和度高的 #dc2626 红色背景 + 白色文字，确保任何主题下都清晰可见。
        Label offlineTag = new Label("断线中");
        offlineTag.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        offlineTag.setStyle(
                "-fx-background-color: #dc2626;"
                + "-fx-text-fill: #ffffff;"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 2 6 2 6;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: #b91c1c;"
                + "-fx-border-radius: 8;"
                + "-fx-border-width: 1;"
        );
        offlineTag.setVisible(false);
        offlineTag.setManaged(false);

        // "你"徽章（默认隐藏）
        Label selfTag = new Label("你");
        selfTag.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        selfTag.setStyle(
                "-fx-background-color: " + ScreenManager.themeColor("rgba(100,116,139,0.25)", "rgba(148,163,184,0.35)") + ";"
                + "-fx-text-fill: " + ScreenManager.themeColor("#475569", "#f8fafc") + ";"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 2 7 2 7;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + ScreenManager.themeColor("rgba(100,116,139,0.40)", "rgba(203,213,225,0.50)") + ";"
                + "-fx-border-radius: 8;"
                + "-fx-border-width: 1;"
        );
        selfTag.setVisible(false);
        selfTag.setManaged(false);

        // **根治方案**：把 HBox card 改成 Pane，绝对定位 avatar + infoBox + selfTag + offlineTag。
        // Pane 直接装进 VBox container，**不再用任何中转层**。
        // 关键修复：之前所有方案都失败的根本原因是 `.player-info-card { -fx-min-height: 0 }`
        // 把 VBox 高度锁死到 0，导致两个 cardWrapper 叠在一起。本轮 CSS 已删掉 min-height:0。
        // 但中转层（StackPane/Pane wrapper）也有 prefHeight 抖动问题，所以这一轮连中转层也不要了。
        //
        // **关键：CSS 中 .player-info-card-inner 已去掉 padding (8 12 8 40) 和 alignment**。
        // 原因：Pane 的 prefHeight = max(child prefHeight) + top+bottom padding，
        // padding 会让 Pane 高度从 36px 撑到 54px，压扁 chatArea。
        // 去掉 CSS padding 后，Pane 高度回归 36px，padding 视觉完全由 Java 代码中的
        // avatar.setLayoutX(40) + selfTag.setLayoutX(4) + offlineTag 绝对定位实现。
        // 同时保留 CSS 类方式（非 setStyle 行内样式），确保 .player-info-card-offline
        // / -self / -active 等动态状态类可以正常叠加覆盖，不受行内样式优先级压制。
        javafx.scene.layout.Pane cardPane = new javafx.scene.layout.Pane();
        cardPane.getStyleClass().add("player-info-card-inner");
        cardPane.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        cardPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cardPane.prefWidthProperty().bind(container.widthProperty());

        // avatar 绝对定位：水平 padding-left 40px 起，垂直居中
        avatar.setLayoutX(40);
        avatar.layoutYProperty().bind(cardPane.heightProperty().subtract(28).divide(2));

        // infoBox 绝对定位：avatar 右侧 (spacing=10)，垂直居中
        infoBox.setLayoutX(40 + 28 + 10);
        infoBox.layoutYProperty().bind(cardPane.heightProperty().subtract(infoBox.heightProperty()).divide(2));

        // "你"徽章：左上角 padding 区 (4,4)，不参与 Pane 高度计算（setManaged(false)）
        selfTag.setLayoutX(4);
        selfTag.setLayoutY(4);

        // "断线中"徽章：右下角 padding 区 (4,4)，跟随 card 宽高变化
        offlineTag.layoutXProperty().bind(cardPane.widthProperty().subtract(offlineTag.widthProperty()).subtract(4));
        offlineTag.layoutYProperty().bind(cardPane.heightProperty().subtract(offlineTag.heightProperty()).subtract(4));

        // 一次性把所有子节点加到 Pane
        cardPane.getChildren().addAll(avatar, infoBox, selfTag, offlineTag);

        container.getChildren().clear();
        container.getChildren().add(cardPane);

        if (isBlack) {
            blackPlayerNameLabel = nameLabel;
            blackPlayerIdLabel = idLabel;
            blackPlayerScoreLabel = scoreLabel;
            blackCardRoot = cardPane;
            blackOfflineTag = offlineTag;
            blackSelfTag = selfTag;
        } else {
            whitePlayerNameLabel = nameLabel;
            whitePlayerIdLabel = idLabel;
            whitePlayerScoreLabel = scoreLabel;
            whiteCardRoot = cardPane;
            whiteOfflineTag = offlineTag;
            whiteSelfTag = selfTag;
        }

        return cardPane;
    }

    /**
     * 切换玩家在线/断线视觉状态。
     *
     * <p><b>关键修复：offlineTag 永不做 setManaged(true)</b>。offlineTag 的 layoutY
     * 绑定到 cardPane.heightProperty()（公式：cardPane.height - tag.height - 4），
     * 若将它设为 managed，其 layoutY 变化会改变 Pane 的 computePrefHeight，
     * 导致 cardPane height 再次变化 → layoutY 再次变化 → 形成跨帧"无限布局反馈循环"，
     * 链条为 cardPane ↔ VBox ↔ sidebar ↔ BorderPane ↔ boardContainer ↔ resizeBoard，
     * 最终表现为棋盘不停抖动。</p>
     *
     * <p>解法：offlineTag 永远 setManaged(false)，仅通过 setVisible 切换显示。
     * 因为它在 Pane 中绝对定位（layoutX/Y），不参与布局不会影响父容器尺寸，
     * 但 layoutY 绑定仍能保证窗口拉伸时徽章跟随移动到卡片右下角。</p>
     *
     * @param isBlack true=黑方, false=白方
     * @param online  true=正常, false=断线中
     */
    private void setPlayerOnline(boolean isBlack, boolean online) {
        Label nameLabel = isBlack ? blackPlayerNameLabel : whitePlayerNameLabel;
        Label tag = isBlack ? blackOfflineTag : whiteOfflineTag;
        javafx.scene.layout.Pane card = isBlack ? blackCardRoot : whiteCardRoot;
        if (nameLabel == null || card == null) return;

        if (online) {
            if (tag != null) {
                tag.setVisible(false);
            }
            // 恢复正常视觉：移除 offline class + 名字恢复主题色
            card.getStyleClass().remove("player-info-card-offline");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");
        } else {
            if (tag != null) {
                tag.setVisible(true);
            }
            // 断线视觉：offlineTag 红底白字 + 卡片浅红背景 + 红色 innershadow（不影响 layoutBounds）
            card.getStyleClass().add("player-info-card-offline");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");
        }
    }

    private void startCountdown() {
        stopCountdown();
        remainingSeconds = AppConfig.TURN_TIME_LIMIT;
        updateTimerLabel();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerLabel();
            if (remainingSeconds <= 0) {
                stopCountdown();
            }
        }));
        countdownTimeline.setCycleCount(AppConfig.TURN_TIME_LIMIT);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void resumeCountdown() {
        stopCountdown();
        if (remainingSeconds <= 0) {
            remainingSeconds = AppConfig.TURN_TIME_LIMIT;
        }
        updateTimerLabel();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerLabel();
            if (remainingSeconds <= 0) {
                stopCountdown();
            }
        }));
        countdownTimeline.setCycleCount(remainingSeconds);
        countdownTimeline.play();
    }

    private void updateTimerLabel() {
        if (timerLabel == null) return;
        timerLabel.setText("剩余: " + remainingSeconds + "s");
        if (remainingSeconds <= 10) {
            timerLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#dc2626", "#fca5a5") + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#334155", "#e2e8f0") + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleUndo() {
        if (pendingRequest) return;
        stopCountdown();
        ClientMain.getConnector().send(Protocol.REQUEST_UNDO);
        pendingRequest = true;
        showPendingRequestDialog("悔棋请求", "等待对方同意或拒绝悔棋...");
    }

    @FXML
    private void handleDraw() {
        if (pendingRequest) return;
        stopCountdown();
        ClientMain.getConnector().send(Protocol.REQUEST_DRAW);
        pendingRequest = true;
        showPendingRequestDialog("求和请求", "等待对方同意或拒绝求和...");
    }

    @FXML
    private void handleSaveGame() {
        if (previewMode) {
            chatArea.appendText("[系统] 浏览模式不保存棋局\n");
            return;
        }
        String saveName = "棋局存档 " + java.time.LocalDateTime.now().withNano(0);
        ClientMain.getConnector().send(Message.of(Protocol.SAVE_GAME, "saveName", saveName));
    }
    @FXML
    private void handleSurrender() {
        if (previewMode) {
            ScreenManager.showDialog("/fxml/GameOverUI.fxml", (GameOverController ctrl) -> ctrl.setResult("2", "预览模式 - 平局"));
            return;
        }
        ClientMain.getConnector().send(Protocol.SURRENDER);
    }

    @FXML
    private void handleLeaveSpectate() {
        if (previewMode) {
            LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
            ctrl.setCurrentUser(AppConfig.getCurrentUser());
            ctrl.setPreviewMode(true);
            return;
        }
        ClientMain.getConnector().send(Protocol.LEAVE_SPECTATE);
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(AppConfig.getCurrentUser());
        ctrl.setPreviewMode(false);
    }

    /**
     * 根据 myTurn / gameOver / spectatorMode / replayMode 控制悔棋和求和按钮的可用状态。
     * 只有当前落子方才能提出悔棋或求和。
     */
    private void updateActionButtons() {
        boolean canAct = myTurn && !gameOver && !spectatorMode && !replayMode && !pendingRequest;
        if (undoBtn != null) undoBtn.setDisable(!canAct);
        if (drawBtn != null) drawBtn.setDisable(!canAct);
        if (saveBtn != null) saveBtn.setDisable(gameOver || spectatorMode || replayMode);
    }

    /**
     * 显示游戏结束弹窗（非阻塞模式），返回 Stage 引用以便外部关闭。
     * 与 ScreenManager.showDialog 不同，此方法使用 show() 而非 showAndWait()，
     * 保证 BoardController 可以继续接收网络消息（如再来一局请求）。
     */
    private Stage showGameOverDialog(String fxmlPath, java.util.function.Consumer<GameOverController> setupCallback) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            GameOverController ctrl = loader.getController();
            if (setupCallback != null) {
                setupCallback.accept(ctrl);
            }
            Stage dialog = new Stage();
            dialog.initOwner(ScreenManager.getStage());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            // 给弹窗添加基本包装
            javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox();
            layout.setStyle("-fx-background-color: transparent;");
            javafx.scene.layout.VBox.setVgrow(root, javafx.scene.layout.Priority.ALWAYS);
            layout.getChildren().add(root);

            javafx.scene.Scene scene = new javafx.scene.Scene(layout);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            ScreenManager.applyStylesheetPublic(scene);
            ScreenManager.applyDialogTheme(scene);
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();
            return dialog;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load dialog: " + fxmlPath, e);
        }
    }

    private void showPendingRequestDialog(String title, String message) {
        if (pendingRequestStage != null) {
            pendingRequestStage.close();
        }
        javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(15);
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.setPadding(new javafx.geometry.Insets(30));
        layout.setStyle("-fx-background-color: " + ScreenManager.themeColor("rgba(255,255,255,0.96)", "rgba(10,15,28,0.96)") + "; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + ScreenManager.themeColor("#1e293b", "#f8fafc") + ";");

        javafx.scene.control.Label messageLabel = new javafx.scene.control.Label(message);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setStyle("-fx-progress-color: " + ScreenManager.themeColor("#64748b", "#cbd5e1") + ";");
        spinner.setPrefSize(30, 30);

        layout.getChildren().addAll(titleLabel, spinner, messageLabel);

        pendingRequestStage = new javafx.stage.Stage();
        pendingRequestStage.setTitle(title);
        pendingRequestStage.initOwner(ScreenManager.getStage());
        pendingRequestStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        pendingRequestStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        javafx.scene.Scene scene = new javafx.scene.Scene(layout);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        ScreenManager.applyStylesheetPublic(scene);
        ScreenManager.applyDialogTheme(scene);
        pendingRequestStage.setScene(scene);
        pendingRequestStage.setResizable(false);
        pendingRequestStage.show();
    }

    private void closePendingRequestDialog() {
        if (pendingRequestStage != null) {
            pendingRequestStage.close();
            pendingRequestStage = null;
        }
        pendingRequest = false;
        updateActionButtons();
    }

    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;

        String display = myUsername + ": " + msg + "\n";
        chatArea.appendText(display);
        chatArea.setScrollTop(Double.MAX_VALUE);
        lastLocalChat = display;
        lastLocalChatTime = System.currentTimeMillis();

        if (!previewMode) {
            ClientMain.getConnector().send(Message.of(Protocol.CHAT, "message", msg));
        }
        chatInput.clear();
    }

    /** 棋谱回放：解析落子序列并支持上一步/下一步浏览 */
    public void replayMoves(String moves) {
        replayMoveList.clear();
        if (moves == null || moves.isEmpty()) return;

        try {
            if (moves.startsWith("[")) {
                Gson gson = new Gson();
                Type moveListType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                List<Map<String, Object>> moveList = gson.fromJson(moves, moveListType);
                for (Map<String, Object> move : moveList) {
                    int x = ((Number) move.get("x")).intValue();
                    int y = ((Number) move.get("y")).intValue();
                    int color = ((Number) move.get("color")).intValue();
                    replayMoveList.add(new int[]{x, y, color});
                }
            } else {
                String[] steps = moves.split(";");
                for (String step : steps) {
                    String[] parts = step.split(",");
                    if (parts.length >= 3) {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int color = Integer.parseInt(parts[2]);
                        replayMoveList.add(new int[]{x, y, color});
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 默认从头开始
        replayIndex = 0;
        showReplayStep(replayIndex);
    }

    private void showReplayStep(int step) {
        // 清空棋盘
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                board[x][y] = 0;
            }
        }
        // 重绘到第 step 步
        int limit = Math.max(0, Math.min(step, replayMoveList.size()));
        for (int i = 0; i < limit; i++) {
            int[] m = replayMoveList.get(i);
            board[m[0]][m[1]] = m[2];
        }
        if (limit > 0) {
            int[] last = replayMoveList.get(limit - 1);
            lastMoveX = last[0];
            lastMoveY = last[1];
            currentTurn = (last[2] == 1) ? 2 : 1;
        } else {
            lastMoveX = -1;
            lastMoveY = -1;
            currentTurn = 1;
        }
        redrawAll();
        updateTurnLabel();
        chatArea.appendText("[系统] 第 " + limit + " / " + replayMoveList.size() + " 步\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    @FXML
    private void handleReplayPrev() {
        if (!replayMode || replayIndex <= 0) return;
        replayIndex--;
        showReplayStep(replayIndex);
    }

    @FXML
    private void handleReplayNext() {
        if (!replayMode || replayIndex >= replayMoveList.size()) return;
        replayIndex++;
        showReplayStep(replayIndex);
    }

    @FXML
    private void handleReplayExit() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(AppConfig.getCurrentUser());
    }


    @Override
    public void onMessage(Message msg) {
        String cmd = msg.getCmd();
        if (Protocol.GAME_START.equals(cmd)) {
            // 重置棋盘状态准备新游戏
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    board[x][y] = 0;
                }
            }
            lastMoveX = -1;
            lastMoveY = -1;
            currentTurn = 1;
            moveHistory.clear();
            String bId = String.valueOf(msg.getLong("blackUserId"));
            String wId = String.valueOf(msg.getLong("whiteUserId"));
            String bName = msg.has("blackUsername") ? msg.getString("blackUsername") : bId;
            String wName = msg.has("whiteUsername") ? msg.getString("whiteUsername") : wId;
            int bScore = msg.has("blackScore") ? msg.getInt("blackScore") : 0;
            int wScore = msg.has("whiteScore") ? msg.getInt("whiteScore") : 0;
            String bAvatar = msg.has("blackAvatarUrl") ? msg.getString("blackAvatarUrl") : "";
            String wAvatar = msg.has("whiteAvatarUrl") ? msg.getString("whiteAvatarUrl") : "";
            // 优先使用实例字段；为空时从 AppConfig 取，避免新建 Board 后 myUsername 未初始化导致白方卡片显示 "unknown"
            if (myUsername == null) {
                User cu0 = AppConfig.getCurrentUser();
                myUsername = cu0 != null ? cu0.getUsername() : null;
            }
            setGameInfo(bId, bName, wId, wName, myUsername, bScore, wScore);
            // 应用双方头像（存储 Image 引用防止 GC）
            updatePlayerAvatar(blackAvatarPane, blackAvatarText, blackAvatarImageView, bAvatar, true);
            updatePlayerAvatar(whiteAvatarPane, whiteAvatarText, whiteAvatarImageView, wAvatar, false);
            redrawAll();
            startCountdown();
        } else if (Protocol.MOVE_SYNC.equals(cmd)) {
            int x     = msg.getInt("x");
            int y     = msg.getInt("y");
            int color = msg.getInt("color");
            drawStone(x, y, color);
            moveHistory.add(new int[]{x, y, color});
            currentTurn = (color == 1) ? 2 : 1;
            myTurn = (myColor.equals("black") && currentTurn == 1) ||
                     (myColor.equals("white") && currentTurn == 2);
            updateTurnLabel();
            updateActionButtons();
            startCountdown();
        } else if (Protocol.GAME_OVER.equals(cmd)) {
            gameOver = true;
            myTurn = false;
            closePendingRequestDialog();
            updateActionButtons();
            stopCountdown();
            String winnerId = String.valueOf(msg.getLong("winnerId"));
            String reason = msg.getString("msg");
            String result;
            if ("0".equals(winnerId)) {
                result = "平局";
            } else if (aiMode && "-1".equals(winnerId)) {
                result = "白方胜";
            } else if ("black".equals(myColor) && blackId.equals(winnerId)) {
                result = "黑方胜";
            } else if ("white".equals(myColor) && whiteId.equals(winnerId)) {
                result = "白方胜";
            } else if ("black".equals(myColor) && (whiteId.equals(winnerId) || ("-1".equals(winnerId) && aiMode))) {
                result = "对手胜";
            } else if ("white".equals(myColor) && blackId.equals(winnerId)) {
                result = "对手胜";
            } else {
                result = "游戏结束";
            }
            updateTurnLabel();

            // 解析本局积分变化
            int myScoreChange = 0;
            User currentUser = AppConfig.getCurrentUser();
            if (currentUser != null) {
                org.json.JSONObject scoreChanges = msg.getData() != null
                        ? msg.getData().optJSONObject("scoreChanges") : null;
                if (scoreChanges != null) {
                    myScoreChange = scoreChanges.optInt(String.valueOf(currentUser.getUserId()), 0);
                }
            }

            final int scoreChangeFinal = myScoreChange;
            // 关闭旧弹窗（如存在），然后存储新弹窗引用
            if (gameOverDialogStage != null) {
                gameOverDialogStage.close();
                gameOverDialogStage = null;
            }
            if (spectatorMode) {
                String spectatorResult;
                if ("0".equals(winnerId)) {
                    spectatorResult = "平局";
                } else if (blackId.equals(winnerId)) {
                    spectatorResult = "黑方胜 (" + blackName + ")";
                } else if (whiteId.equals(winnerId)) {
                    spectatorResult = "白方胜 (" + whiteName + ")";
                } else {
                    spectatorResult = "游戏结束";
                }
                chatArea.appendText("[系统] 对局结束: " + spectatorResult + "\n");
                chatArea.setScrollTop(Double.MAX_VALUE);
                gameOverDialogStage = showGameOverDialog("/fxml/GameOverUI.fxml", (GameOverController ctrl) -> {
                    ctrl.setResult(spectatorResult, reason, 0);
                    ctrl.hideReplayButton();
                });
            } else {
                // 断线结束不显示"再来一局"
                final boolean disconnectEnd = reason != null && reason.contains("断线");
                // 使用 showDialogWithoutWait 避免阻塞，以便后续接收 rematch 消息
                gameOverDialogStage = showGameOverDialog("/fxml/GameOverUI.fxml", (GameOverController ctrl) -> {
                    ctrl.setResult(result, reason, scoreChangeFinal);
                    if (disconnectEnd) {
                        ctrl.hideReplayButton();
                    }
                });
            }
        } else if ("SAVE_GAME_OK".equals(cmd)) {
            chatArea.appendText("[系统] 棋局已保存：" + msg.getString("saveName") + "\n");
        } else if ("SAVE_GAME_FAIL".equals(cmd)) {
            chatArea.appendText("[系统] 保存失败：" + msg.getString("msg") + "\n");
        } else if (Protocol.CHAT_MSG.equals(cmd)) {
            String username = msg.getString("username");
            String message = msg.getString("message");
            String display = username + ": " + message + "\n";
            if (display.equals(lastLocalChat) &&
                System.currentTimeMillis() - lastLocalChatTime < 2000) {
                return;
            }
            chatArea.appendText(display);
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("UNDO_REQUESTED".equals(cmd)) {
            String requesterName = msg.getString("requesterName");
            chatArea.appendText("[系统] 玩家 " + requesterName + " 请求悔棋\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            // 观战者只显示系统消息，不弹确认框（悔棋是玩家私事，观战者无权决定）
            if (spectatorMode || previewMode) {
                return;
            }
            stopCountdown();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("悔棋请求");
            alert.setHeaderText("玩家 " + requesterName + " 请求悔棋");
            alert.setContentText("是否同意悔棋？");
            ScreenManager.showStyledAlert(alert).ifPresent(response -> {
                if (response == ButtonType.OK) {
                    startCountdown();
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_UNDO, "accept", true));
                } else {
                    resumeCountdown();
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_UNDO, "accept", false));
                }
            });
        } else if ("UNDO_ACCEPTED".equals(cmd)) {
            closePendingRequestDialog();
            if (moveHistory.size() >= 2) {
                int[] opponentMove = moveHistory.remove(moveHistory.size() - 1);
                board[opponentMove[0]][opponentMove[1]] = 0;
                int[] myMove = moveHistory.remove(moveHistory.size() - 1);
                board[myMove[0]][myMove[1]] = 0;
                currentTurn = myMove[2];
                if (moveHistory.isEmpty()) {
                    lastMoveX = -1;
                    lastMoveY = -1;
                } else {
                    int[] last = moveHistory.get(moveHistory.size() - 1);
                    lastMoveX = last[0];
                    lastMoveY = last[1];
                }
                myTurn = (myColor.equals("black") && currentTurn == 1) ||
                         (myColor.equals("white") && currentTurn == 2);
                updateTurnLabel();
                updateActionButtons();
                redrawAll();
            }
            startCountdown();
            chatArea.appendText("[系统] 悔棋成功\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("UNDO_REJECTED".equals(cmd)) {
            closePendingRequestDialog();
            resumeCountdown();
            chatArea.appendText("[系统] 对手拒绝悔棋\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("DRAW_REQUESTED".equals(cmd)) {
            String requesterName = msg.getString("requesterName");
            chatArea.appendText("[系统] 玩家 " + requesterName + " 请求求和\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            // 观战者只显示系统消息，不弹确认框
            if (spectatorMode || previewMode) {
                return;
            }
            stopCountdown();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("求和请求");
            alert.setHeaderText("玩家 " + requesterName + " 请求求和");
            alert.setContentText("是否同意平局？");
            ScreenManager.showStyledAlert(alert).ifPresent(response -> {
                if (response == ButtonType.OK) {
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_DRAW, "accept", true));
                } else {
                    resumeCountdown();
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_DRAW, "accept", false));
                }
            });
        } else if ("DRAW_REJECTED".equals(cmd)) {
            closePendingRequestDialog();
            resumeCountdown();
            chatArea.appendText("[系统] 对手拒绝求和\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if (Protocol.REMATCH_REQUESTED.equals(cmd)) {
            // 收到对手的再来一局请求
            String requesterName = msg.getString("requesterName");
            chatArea.appendText("[系统] 玩家 " + requesterName + " 请求再来一局\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("再来一局请求");
            alert.setHeaderText("玩家 " + requesterName + " 请求再来一局");
            alert.setContentText("是否同意再来一局？");
            java.util.Optional<ButtonType> response = ScreenManager.showStyledAlert(alert);
            if (response.isPresent() && response.get() == ButtonType.OK) {
                // 同意：关闭结算弹窗，发送同意消息
                closeGameOverDialog();
                ClientMain.getConnector().send(Message.of(Protocol.REPLY_REMATCH, "accept", true));
            } else {
                // 拒绝 / 关闭弹窗：发送拒绝消息，关闭结算弹窗，返回大厅
                ClientMain.getConnector().send(Message.of(Protocol.REPLY_REMATCH, "accept", false));
                closeGameOverDialog();
                LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
                ctrl.setCurrentUser(AppConfig.getCurrentUser());
                ctrl.setPreviewMode(AppConfig.isPreviewMode());
            }
        } else if (Protocol.REMATCH_ACCEPTED.equals(cmd)) {
            // 对手同意了再来一局，或双方都已同意
            closeGameOverDialog();
            chatArea.appendText("[系统] 双方同意再来一局，准备开始新对局...\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            // 服务端随后会发送 GAME_START，棋盘将在 onMessage(GAME_START) 中重置
        } else if (Protocol.REMATCH_REJECTED.equals(cmd)) {
            // 对手拒绝了再来一局，关闭结算弹窗并弹提示，再返回大厅
            closeGameOverDialog();
            String replierName = msg.getString("replierName");
            String displayName = (replierName != null && !replierName.isEmpty()) ? replierName : "对方";
            chatArea.appendText("[系统] " + displayName + " 拒绝了再来一局\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("再来一局");
            info.setHeaderText(displayName + " 拒绝了再来一局");
            info.setContentText("将返回大厅");
            ScreenManager.showStyledAlert(info);
            LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
            ctrl.setCurrentUser(AppConfig.getCurrentUser());
            ctrl.setPreviewMode(AppConfig.isPreviewMode());
        } else if (Protocol.TURN_TIMEOUT.equals(cmd)) {
            gameOver = true;
            stopCountdown();
            String result = myTurn ? "超时负" : "对手超时";
            myTurn = false;
            updateActionButtons();
            updateTurnLabel();
            ScreenManager.showDialog("/fxml/GameOverUI.fxml", (GameOverController ctrl) -> ctrl.setResult(result, "timeout"));
            chatArea.appendText("[系统] " + result + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if (Protocol.USER_SCORE_UPDATE.equals(cmd)) {
            User cu = AppConfig.getCurrentUser();
            if (cu != null) {
                cu.setScore(msg.getInt("score"));
                cu.setWinCount(msg.getInt("winCount"));
                cu.setLoseCount(msg.getInt("loseCount"));
                cu.setDrawCount(msg.getInt("drawCount"));
            }
        } else if ("PLAYER_DISCONNECTED".equals(cmd)) {
            String userId = String.valueOf(msg.getLong("userId"));
            if (userId == null || userId.isEmpty()) return;
            boolean isBlack = userId.equals(blackId);
            boolean isWhite = userId.equals(whiteId);
            if (!isBlack && !isWhite) return;
            setPlayerOnline(isBlack, false);
            String name = isBlack ? blackName : whiteName;
            int dcCount = msg.getInt("disconnectCount");
            chatArea.appendText("[系统] 玩家 " + name + " 断线 (第" + dcCount + "/" + 3 + "次)，等待重连...\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("PLAYER_RECONNECTED".equals(cmd)) {
            String userId = String.valueOf(msg.getLong("userId"));
            if (userId == null || userId.isEmpty()) return;
            boolean isBlack = userId.equals(blackId);
            boolean isWhite = userId.equals(whiteId);
            if (!isBlack && !isWhite) return;
            setPlayerOnline(isBlack, true);
            String name = isBlack ? blackName : whiteName;
            chatArea.appendText("[系统] 玩家 " + name + " 已重连\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("PLAYER_DISCONNECT_LIMIT".equals(cmd)) {
            String userId = String.valueOf(msg.getLong("userId"));
            if (userId != null && (userId.equals(blackId) || userId.equals(whiteId))) {
                setPlayerOnline(userId.equals(blackId), true);
            }
            // GAME_OVER 随后会触发，此处仅清理断线角标
        }
    }

    @FXML
    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        if (sidebarCollapsed) {
            sidebarContent.setVisible(false);
            sidebarContent.setManaged(false);
            dragHandle.setVisible(false);
            dragHandle.setManaged(false);
            toggleSidebarBtn.setText("«");
        } else {
            sidebarContent.setVisible(true);
            sidebarContent.setManaged(true);
            dragHandle.setVisible(true);
            dragHandle.setManaged(true);
            toggleSidebarBtn.setText("»");
            // 展开时立即计算应用最新比例尺寸
            adjustSidebarSize();
        }
    }

    private void adjustSidebarSize() {
        if (sidebarCollapsed) return;
        double totalW = rootPane.getWidth() - 60;
        if (totalW <= 0) return;
        if (sidebarRatio <= 0) {
            sidebarRatio = 220.0 / totalW;
        }
        double newWidth = totalW * sidebarRatio;
        newWidth = clampSidebarWidth(newWidth, totalW);
        sidebarContent.setPrefWidth(newWidth);
        sidebarContent.setMinWidth(newWidth);
        sidebarContent.setMaxWidth(newWidth);
    }

    private double clampSidebarWidth(double width, double totalW) {
        double minW = 150.0;
        double maxHeight = boardContainer.getHeight();
        if (maxHeight <= 0) {
            maxHeight = rootPane.getHeight() - 40;
            if (maxHeight <= 0) maxHeight = 400.0;
        }
        // 侧栏最大宽度 = 总可用宽度 - 按钮及拉手总宽(30px) - 棋盘自适应物理高度(棋盘是正方形)
        double maxW = totalW - 30 - maxHeight;
        if (maxW < minW) {
            maxW = minW;
        }
        if (width < minW) return minW;
        if (width > maxW) return maxW;
        return width;
    }

    /** 关闭游戏结束弹窗并清理引用 */
    private void closeGameOverDialog() {
        if (gameOverDialogStage != null) {
            gameOverDialogStage.close();
            gameOverDialogStage = null;
        }
    }
}
