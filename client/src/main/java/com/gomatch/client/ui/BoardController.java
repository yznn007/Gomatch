package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
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
    private int cellSize; // 动态计算
    private final List<int[]> moveHistory = new ArrayList<>();
    private final List<int[]> replayMoveList = new ArrayList<>();
    private int replayIndex = 0;
    private String lastLocalChat;
    private long lastLocalChatTime;
    private int lastMoveX = -1;
    private int lastMoveY = -1;

    private Label blackPlayerNameLabel;
    private Label whitePlayerNameLabel;
    private Label blackPlayerIdLabel;
    private Label whitePlayerIdLabel;
    private Label blackPlayerScoreLabel;
    private Label whitePlayerScoreLabel;
    private javafx.scene.layout.HBox blackCardRoot;
    private javafx.scene.layout.HBox whiteCardRoot;
    private Label blackOfflineTag;
    private Label whiteOfflineTag;

    // 本地倒计时
    private Timeline countdownTimeline;
    private int remainingSeconds;

    @FXML
    private void initialize() {
        ClientMain.getConnector().addListener(this);
        
        boardContainer.widthProperty().addListener((obs, o, n) -> resizeBoard());
        boardContainer.heightProperty().addListener((obs, o, n) -> resizeBoard());

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

        // 窗口拉伸时，侧边栏基于比例自适应调整
        rootPane.widthProperty().addListener((obs, o, n) -> adjustSidebarSize());
        boardContainer.heightProperty().addListener((obs, o, n) -> adjustSidebarSize());
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
        setGameInfo(blackId, blackId, whiteId, whiteId, username);
    }

    public void setGameInfo(String blackId, String blackDisplayName, String whiteId, String whiteDisplayName, String username) {
        this.blackName  = blackDisplayName;
        this.whiteName  = whiteDisplayName;
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

        if (isBlack) {
            setPlayerCardLabels(blackPlayerNameLabel, blackPlayerIdLabel, blackPlayerScoreLabel,
                    username != null ? username : blackDisplayName, myIdStr, myScore);
            setPlayerCardLabels(whitePlayerNameLabel, whitePlayerIdLabel, whitePlayerScoreLabel,
                    whiteDisplayName, whiteId, -1);
        } else {
            setPlayerCardLabels(blackPlayerNameLabel, blackPlayerIdLabel, blackPlayerScoreLabel,
                    blackDisplayName, blackId, -1);
            setPlayerCardLabels(whitePlayerNameLabel, whitePlayerIdLabel, whitePlayerScoreLabel,
                    username != null ? username : whiteDisplayName, myIdStr, myScore);
        }

        myColor = isBlack ? "black" : "white";
        myTurn  = isBlack;
        updateTurnLabel();

        // 观战模式：禁用落子和游戏操作
        if (spectatorMode) {
            myTurn = false;
            myColor = "spectator";
            applySpectatorMode();
        }
        updateActionButtons();

        if (!replayMode) {
            startCountdown();
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
                scoreLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            } else {
                scoreLbl.setText("?");
                scoreLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            }
        }
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
        this.replayMode = replayMode;
        if (replayMode) {
            gameOver = true;
            myTurn = false;
            blackName = "黑方";
            whiteName = "白方";
            myColor = "black";
            currentTurn = 1;
            if (blackPlayerNameLabel != null) blackPlayerNameLabel.setText(blackName);
            if (whitePlayerNameLabel != null) whitePlayerNameLabel.setText(whiteName);
            if (blackPlayerIdLabel != null) blackPlayerIdLabel.setText("#---");
            if (whitePlayerIdLabel != null) whitePlayerIdLabel.setText("#---");
            if (blackPlayerScoreLabel != null) blackPlayerScoreLabel.setText("?");
            if (whitePlayerScoreLabel != null) whitePlayerScoreLabel.setText("?");
            updateTurnLabel();

            if (gameControls != null) {
                gameControls.setVisible(false);
                gameControls.setManaged(false);
            }
            if (replayControls != null) {
                replayControls.setVisible(true);
                replayControls.setManaged(true);
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
        if (replayMode || gameOver || spectatorMode) return;
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
        ClientMain.getConnector().send(Message.of(Protocol.MOVE, "x", String.valueOf(x), "y", String.valueOf(y)));
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
        if (blackName == null) blackName = "黑方";
        if (whiteName == null) whiteName = "白方";
        if (currentTurn == 1) {
            turnLabel.setText("⚫ " + blackName + " 回合");
        } else {
            turnLabel.setText("⚪ " + whiteName + " 回合");
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
        blackCardRoot = buildOnePlayerCard(blackPlayerBox, "⚫", "黑方", "#1e293b");
        whiteCardRoot = buildOnePlayerCard(whitePlayerBox, "⚪", "白方", "#cbd5e1");
    }

    private javafx.scene.layout.HBox buildOnePlayerCard(VBox container, String stone, String colorLabel, String avatarBg) {
        if (container == null) return null;

        javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
        card.getStyleClass().add("player-info-card-inner");
        card.setSpacing(10);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        avatar.getStyleClass().add("avatar-badge");
        avatar.setStyle("-fx-background-color: " + avatarBg + "; -fx-min-width: 28px; -fx-max-width: 28px; -fx-min-height: 28px; -fx-max-height: 28px;");
        javafx.scene.control.Label avatarText = new javafx.scene.control.Label(colorLabel.substring(0, 1));
        avatarText.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        avatarText.getStyleClass().add("avatar-badge-text");
        avatarText.setStyle("-fx-font-size: 12px;");
        avatar.getChildren().add(avatarText);

        VBox infoBox = new VBox(2);
        infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label("---");
        nameLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        javafx.scene.layout.HBox detailRow = new javafx.scene.layout.HBox(6);
        detailRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.Label idLabel = new javafx.scene.control.Label("#---");
        idLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        javafx.scene.control.Label scoreLabel = new javafx.scene.control.Label("---");
        scoreLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        scoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        javafx.scene.control.Label roleLabel = new javafx.scene.control.Label(colorLabel);
        roleLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ("黑方".equals(colorLabel) ? "#1e293b" : "#94a3b8") + ";");

        detailRow.getChildren().addAll(idLabel, scoreLabel, roleLabel);
        infoBox.getChildren().addAll(nameLabel, detailRow);
        card.getChildren().addAll(avatar, infoBox);

        // 断线角标（默认隐藏），紧贴卡片右下角
        Label offlineTag = new Label("断线中");
        offlineTag.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        offlineTag.setStyle(
                "-fx-background-color: #fef2f2;"
                + "-fx-text-fill: #dc2626;"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 2 6 2 6;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: #fecaca;"
                + "-fx-border-radius: 8;"
                + "-fx-border-width: 1;"
        );
        offlineTag.setVisible(false);
        offlineTag.setManaged(false);

        StackPane cardWrapper = new StackPane(card, offlineTag);
        StackPane.setAlignment(offlineTag, javafx.geometry.Pos.BOTTOM_RIGHT);

        container.getChildren().clear();
        container.getChildren().add(cardWrapper);

        if ("黑方".equals(colorLabel)) {
            blackPlayerNameLabel = nameLabel;
            blackPlayerIdLabel = idLabel;
            blackPlayerScoreLabel = scoreLabel;
            blackCardRoot = card;
            blackOfflineTag = offlineTag;
        } else {
            whitePlayerNameLabel = nameLabel;
            whitePlayerIdLabel = idLabel;
            whitePlayerScoreLabel = scoreLabel;
            whiteCardRoot = card;
            whiteOfflineTag = offlineTag;
        }

        return card;
    }

    /**
     * 切换玩家在线/断线视觉状态。
     * @param isBlack true=黑方, false=白方
     * @param online  true=正常, false=断线中
     */
    private void setPlayerOnline(boolean isBlack, boolean online) {
        Label nameLabel = isBlack ? blackPlayerNameLabel : whitePlayerNameLabel;
        Label tag = isBlack ? blackOfflineTag : whiteOfflineTag;
        javafx.scene.layout.HBox card = isBlack ? blackCardRoot : whiteCardRoot;
        if (nameLabel == null || card == null) return;

        if (online) {
            if (tag != null) {
                tag.setVisible(false);
                tag.setManaged(false);
            }
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
            card.setOpacity(1.0);
        } else {
            if (tag != null) {
                tag.setVisible(true);
                tag.setManaged(true);
            }
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #dc2626;");
            card.setOpacity(0.55);
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

    private void updateTimerLabel() {
        if (timerLabel == null) return;
        timerLabel.setText("剩余: " + remainingSeconds + "s");
        if (remainingSeconds <= 10) {
            timerLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 18px; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 18px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleUndo() {
        ClientMain.getConnector().send(Protocol.REQUEST_UNDO);
    }

    @FXML
    private void handleDraw() {
        ClientMain.getConnector().send(Protocol.REQUEST_DRAW);
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

    /**
     * 根据 myTurn / gameOver / spectatorMode / replayMode 控制悔棋和求和按钮的可用状态。
     * 只有当前落子方才能提出悔棋或求和。
     */
    private void updateActionButtons() {
        boolean canAct = myTurn && !gameOver && !spectatorMode && !replayMode;
        if (undoBtn != null) undoBtn.setDisable(!canAct);
        if (drawBtn != null) drawBtn.setDisable(!canAct);
        if (saveBtn != null) saveBtn.setDisable(gameOver || spectatorMode || replayMode);
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
            moveHistory.clear();
            setGameInfo(msg.getString("blackUserId"), msg.getString("whiteUserId"), myUsername);
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
            updateActionButtons();
            stopCountdown();
            String winnerId = msg.getString("winnerId");
            String reason = msg.getString("msg");
            String result;
            if ("0".equals(winnerId)) {
                result = "平局";
            } else if (aiMode && "-1".equals(winnerId)) {
                result = "白方胜";
            } else if ("black".equals(myColor) && blackName.equals(winnerId)) {
                result = "黑方胜";
            } else if ("white".equals(myColor) && whiteName.equals(winnerId)) {
                result = "白方胜";
            } else if ("black".equals(myColor) && (whiteName.equals(winnerId) || ("-1".equals(winnerId) && aiMode))) {
                result = "对手胜";
            } else if ("white".equals(myColor) && blackName.equals(winnerId)) {
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
            ScreenManager.showDialog("/fxml/GameOverUI.fxml", (GameOverController ctrl) -> ctrl.setResult(result, reason, scoreChangeFinal));
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("悔棋请求");
            alert.setHeaderText("玩家 " + requesterName + " 请求悔棋");
            alert.setContentText("是否同意悔棋？");
            ScreenManager.showStyledAlert(alert).ifPresent(response -> {
                if (response == ButtonType.OK) {
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_UNDO, "accept", "1"));
                } else {
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_UNDO, "accept", "0"));
                }
            });
        } else if ("UNDO_ACCEPTED".equals(cmd)) {
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
            chatArea.appendText("[系统] 对手拒绝悔棋\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("DRAW_REQUESTED".equals(cmd)) {
            String requesterName = msg.getString("requesterName");
            chatArea.appendText("[系统] 玩家 " + requesterName + " 请求求和\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("求和请求");
            alert.setHeaderText("玩家 " + requesterName + " 请求求和");
            alert.setContentText("是否同意平局？");
            ScreenManager.showStyledAlert(alert).ifPresent(response -> {
                if (response == ButtonType.OK) {
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_DRAW, "accept", "1"));
                } else {
                    ClientMain.getConnector().send(Message.of(Protocol.REPLY_DRAW, "accept", "0"));
                }
            });
        } else if ("DRAW_REJECTED".equals(cmd)) {
            chatArea.appendText("[系统] 对手拒绝求和\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
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
            String userId = msg.getString("userId");
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
            String userId = msg.getString("userId");
            if (userId == null || userId.isEmpty()) return;
            boolean isBlack = userId.equals(blackId);
            boolean isWhite = userId.equals(whiteId);
            if (!isBlack && !isWhite) return;
            setPlayerOnline(isBlack, true);
            String name = isBlack ? blackName : whiteName;
            chatArea.appendText("[系统] 玩家 " + name + " 已重连\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("PLAYER_DISCONNECT_LIMIT".equals(cmd)) {
            String userId = msg.getString("userId");
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
}

