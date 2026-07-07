package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
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
    @FXML private Label blackLabel;
    @FXML private Label whiteLabel;
    @FXML private Label turnLabel;
    @FXML private Label timerLabel;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private Button undoBtn;
    @FXML private Button drawBtn;
    @FXML private Button surrenderBtn;
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
    private boolean previewMode;
    private boolean aiMode;
    private boolean replayMode;
    private boolean gameOver;
    private int cellSize; // 动态计算
    private final List<int[]> moveHistory = new ArrayList<>();
    private final List<int[]> replayMoveList = new ArrayList<>();
    private int replayIndex = 0;
    private String lastLocalChat;
    private long lastLocalChatTime;

    // 本地倒计时
    private Timeline countdownTimeline;
    private int remainingSeconds;

    @FXML
    private void initialize() {
        ClientMain.getConnector().addListener(this);
        
        boardContainer.widthProperty().addListener((obs, o, n) -> resizeBoard());
        boardContainer.heightProperty().addListener((obs, o, n) -> resizeBoard());

        canvas = new Canvas() {
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
        int boardPx = (SIZE - 1) * cellSize + MARGIN * 2;
        canvas.setWidth(boardPx);
        canvas.setHeight(boardPx);
        redrawAll();
    }

    /** 重绘棋盘格线 + 已落的所有棋子 */
    private void redrawAll() {
        drawBoard();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (board[x][y] != 0) {
                    drawStoneOnCanvas(x, y, board[x][y]);
                }
            }
        }
    }

    public void setGameInfo(String blackId, String whiteId, String username) {
        this.blackName  = blackId;
        this.whiteName  = whiteId;
        this.myUsername = username;
        this.aiMode = "AI".equals(whiteId) || "-1".equals(whiteId);
        this.gameOver = false;
        AppConfig.setAiMode(this.aiMode);
        blackLabel.setText("黑方: " + blackId);
        whiteLabel.setText("白方: " + (aiMode ? "AI" : whiteId));
        
        // 判断我方颜色：同时支持数字 userId（PvP 模式）和用户名（AI 模式）
        User cu = AppConfig.getCurrentUser();
        boolean isBlack = false;
        if (cu != null) {
            String myIdStr = String.valueOf(cu.getUserId());
            isBlack = myIdStr.equals(blackId) || (username != null && username.equals(blackId));
        } else if (username != null) {
            isBlack = username.equals(blackId);
        }
        myColor = isBlack ? "black" : "white";
        myTurn  = isBlack;
        updateTurnLabel();
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
            blackLabel.setText("黑方: " + blackName);
            whiteLabel.setText("白方: " + whiteName);
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
        }
    }

    private void handleMouseClick(MouseEvent e) {
        if (replayMode || gameOver) return;
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
        moveHistory.add(new int[]{x, y, color});
        myTurn = false;
        updateTurnLabel();
        ClientMain.getConnector().send(Protocol.MOVE, String.valueOf(x), String.valueOf(y));
    }

    private void drawStone(int x, int y, int color) {
        drawStoneOnCanvas(x, y, color);
        board[x][y] = color;
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

    private void drawBoard() {
        if (cellSize == 0) return;
        // 清除画布上的旧内容，防止尺寸变化时留下残影
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

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
        if (currentTurn == 1) {
            turnLabel.setText("当前回合: 黑方 (" + blackName + ")");
        } else {
            turnLabel.setText("当前回合: 白方 (" + whiteName + ")");
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
    private void handleSurrender() {
        if (previewMode) {
            GameOverController ctrl = ScreenManager.showDialog("/fxml/GameOverUI.fxml");
            ctrl.setResult("2", "预览模式 - 平局");
            return;
        }
        ClientMain.getConnector().send(Protocol.SURRENDER);
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
            ClientMain.getConnector().send(Protocol.CHAT, msg);
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

        // 默认显示完整棋局
        replayIndex = replayMoveList.size();
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
            currentTurn = (last[2] == 1) ? 2 : 1;
        } else {
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
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }


    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.GAME_START.equals(command)) {
            moveHistory.clear();
            if (args.length >= 2) {
                setGameInfo(args[0], args[1], myUsername);
            }
            startCountdown();
        } else if (Protocol.MOVE_SYNC.equals(command)) {
            int x     = Integer.parseInt(args[0]);
            int y     = Integer.parseInt(args[1]);
            int color = Integer.parseInt(args[2]);
            drawStone(x, y, color);
            moveHistory.add(new int[]{x, y, color});
            currentTurn = (color == 1) ? 2 : 1;
            myTurn = (myColor.equals("black") && currentTurn == 1) ||
                     (myColor.equals("white") && currentTurn == 2);
            updateTurnLabel();
            startCountdown();
        } else if (Protocol.GAME_OVER.equals(command)) {
            gameOver = true;
            myTurn = false;
            stopCountdown();
            String winnerId = args[0];
            String reason = args.length > 1 ? args[1] : "";
            String result;
            if ("0".equals(winnerId)) {
                result = "平局";
            } else if (aiMode && "-1".equals(winnerId)) {
                result = "对手胜";
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
            GameOverController ctrl = ScreenManager.showDialog("/fxml/GameOverUI.fxml");
            ctrl.setResult(result, reason);
        } else if (Protocol.CHAT_MSG.equals(command)) {
            String username = args.length > 1 ? args[1] : args[0];
            String message = args.length > 2 ? args[2] : "";
            String display = username + ": " + message + "\n";
            // 避免服务端回显导致自己发送的消息出现两次
            if (display.equals(lastLocalChat) &&
                System.currentTimeMillis() - lastLocalChatTime < 2000) {
                return;
            }
            chatArea.appendText(display);
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("UNDO_REQUESTED".equals(command)) {
            chatArea.appendText("[系统] 玩家 " + args[1] + " 请求悔棋\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            // 弹出确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("悔棋请求");
            alert.setHeaderText("玩家 " + args[1] + " 请求悔棋");
            alert.setContentText("是否同意悔棋？");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    ClientMain.getConnector().send(Protocol.REPLY_UNDO, "1");
                } else {
                    ClientMain.getConnector().send(Protocol.REPLY_UNDO, "0");
                }
            });
        } else if ("UNDO_ACCEPTED".equals(command)) {
            if (moveHistory.size() >= 2) {
                int[] opponentMove = moveHistory.remove(moveHistory.size() - 1);
                board[opponentMove[0]][opponentMove[1]] = 0;
                int[] myMove = moveHistory.remove(moveHistory.size() - 1);
                board[myMove[0]][myMove[1]] = 0;
                currentTurn = myMove[2]; // 回到请求悔棋者的回合
                myTurn = (myColor.equals("black") && currentTurn == 1) ||
                         (myColor.equals("white") && currentTurn == 2);
                updateTurnLabel();
                redrawAll();
            }
            startCountdown();
            chatArea.appendText("[系统] 悔棋成功\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("UNDO_REJECTED".equals(command)) {
            chatArea.appendText("[系统] 对手拒绝悔棋\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if ("DRAW_REQUESTED".equals(command)) {
            chatArea.appendText("[系统] 玩家 " + args[1] + " 请求求和\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            // 弹出确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("求和请求");
            alert.setHeaderText("玩家 " + args[1] + " 请求求和");
            alert.setContentText("是否同意平局？");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    ClientMain.getConnector().send(Protocol.REPLY_DRAW, "1");
                } else {
                    ClientMain.getConnector().send(Protocol.REPLY_DRAW, "0");
                }
            });
        } else if ("DRAW_REJECTED".equals(command)) {
            chatArea.appendText("[系统] 对手拒绝求和\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        } else if (Protocol.TURN_TIMEOUT.equals(command)) {
            gameOver = true;
            stopCountdown();
            // 超时的是当前回合方 → myTurn=true 表示我超时
            String result = myTurn ? "超时负" : "对手超时";
            myTurn = false;
            updateTurnLabel();
            GameOverController ctrl = ScreenManager.showDialog("/fxml/GameOverUI.fxml");
            ctrl.setResult(result, "timeout");
            chatArea.appendText("[系统] " + result + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
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
