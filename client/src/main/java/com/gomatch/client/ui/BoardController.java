package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;
import com.gomatch.common.Protocol;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class BoardController implements NetworkListener {
    private static final int SIZE   = 15;
    private static final int MARGIN = 20;

    @FXML private StackPane boardContainer;
    @FXML private Label blackLabel;
    @FXML private Label whiteLabel;
    @FXML private Label turnLabel;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private Button undoBtn;
    @FXML private Button drawBtn;
    @FXML private Button surrenderBtn;

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
    private int cellSize; // 动态计算

    @FXML
    private void initialize() {
        // 棋盘大小跟随容器动态调整
        boardContainer.widthProperty().addListener((obs, o, n) -> resizeBoard());
        boardContainer.heightProperty().addListener((obs, o, n) -> resizeBoard());

        canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();
        boardContainer.getChildren().add(canvas);

        canvas.setOnMouseClicked(this::handleMouseClick);
        ClientMain.getConnector().addListener(this);
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
        blackLabel.setText("黑方: " + blackId);
        whiteLabel.setText("白方: " + whiteId);
        myColor = username.equals(blackId) ? "black" : "white";
        myTurn  = myColor.equals("black");
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

    private void handleMouseClick(MouseEvent e) {
        if (!myTurn) return;
        if (cellSize == 0) return;
        int x = (int) Math.round((e.getX() - MARGIN) / cellSize);
        int y = (int) Math.round((e.getY() - MARGIN) / cellSize);
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        if (board[x][y] != 0) return;

        if (previewMode) {
            int color = currentTurn;
            drawStone(x, y, color);
            currentTurn = (color == 1) ? 2 : 1;
            updateTurnLabel();
            chatArea.appendText("[本地] " + (color == 1 ? "黑" : "白") + "方落子 (" + x + "," + y + ")\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
            return;
        }
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
            myTurn = "black".equals(myColor);
        } else {
            turnLabel.setText("当前回合: 白方 (" + whiteName + ")");
            myTurn = "white".equals(myColor);
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

    /** 棋谱回放：解析 "x,y,color;x,y,color;..." 格式依次落子 */
    public void replayMoves(String moves) {
        String[] steps = moves.split(";");
        for (String step : steps) {
            String[] parts = step.split(",");
            if (parts.length >= 3) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int color = Integer.parseInt(parts[2]);
                drawStone(x, y, color);
                currentTurn = (color == 1) ? 2 : 1;
            }
        }
        updateTurnLabel();
    }

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.MOVE_SYNC.equals(command)) {
            int x     = Integer.parseInt(args[0]);
            int y     = Integer.parseInt(args[1]);
            int color = Integer.parseInt(args[2]);
            drawStone(x, y, color);
            currentTurn = (color == 1) ? 2 : 1;
            updateTurnLabel();
        } else if (Protocol.GAME_OVER.equals(command)) {
            String result = args[0];
            String reason = args.length > 1 ? args[1] : "";
            GameOverController ctrl = ScreenManager.showDialog("/fxml/GameOverUI.fxml");
            ctrl.setResult(result, reason);
        } else if (Protocol.CHAT_MSG.equals(command)) {
            chatArea.appendText(args[0] + ": " + args[1] + "\n");
            // 自动滚动到底部
            chatArea.setScrollTop(Double.MAX_VALUE);
        }
    }
}
