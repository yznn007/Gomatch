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
    private static final int SIZE = 15;
    private static final int CELL = 36;
    private static final int MARGIN = 20;
    private static final int BOARD_PX = (SIZE - 1) * CELL + MARGIN * 2;

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
    private String myColor;
    private String myUsername;
    private String blackName;
    private String whiteName;
    private boolean previewMode;

    @FXML
    private void initialize() {
        canvas = new Canvas(BOARD_PX, BOARD_PX);
        gc = canvas.getGraphicsContext2D();
        boardContainer.getChildren().add(canvas);
        drawBoard();

        canvas.setOnMouseClicked(this::handleMouseClick);
        if (!previewMode) {
            ClientMain.getConnector().addListener(this);
        }
    }

    public void setGameInfo(String blackId, String whiteId, String username) {
        this.blackName = blackId;
        this.whiteName = whiteId;
        this.myUsername = username;
        blackLabel.setText("黑方: " + blackId);
        whiteLabel.setText("白方: " + whiteId);
        myColor = username.equals(blackId) ? "black" : "white";
        myTurn = myColor.equals("black");
        updateTurnLabel();
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            drawStone(3, 3, 1);
            drawStone(3, 4, 2);
            drawStone(4, 4, 1);
            drawStone(4, 5, 2);
            drawStone(5, 5, 1);
            currentTurn = 2;
            updateTurnLabel();
            chatArea.appendText("[系统] 预览模式 - 点击棋盘可本地落子\n");
            chatArea.appendText("[系统] 点击认输可查看结算页面\n");
        }
    }

    private void handleMouseClick(MouseEvent e) {
        if (!myTurn) return;
        int x = (int) Math.round((e.getX() - MARGIN) / CELL);
        int y = (int) Math.round((e.getY() - MARGIN) / CELL);
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        if (board[x][y] != 0) return;

        if (previewMode) {
            int color = currentTurn;
            drawStone(x, y, color);
            currentTurn = (color == 1) ? 2 : 1;
            updateTurnLabel();
            chatArea.appendText("[本地] " + (color == 1 ? "黑" : "白") + "方落子 (" + x + "," + y + ")\n");
            return;
        }
        ClientMain.getConnector().send(Protocol.MOVE, String.valueOf(x), String.valueOf(y));
    }

    private void drawStone(int x, int y, int color) {
        double cx = MARGIN + x * CELL;
        double cy = MARGIN + y * CELL;
        double radius = CELL / 2.0 - 2;

        gc.setStroke(Color.BLACK);
        if (color == 1) {
            gc.setFill(Color.BLACK);
        } else {
            gc.setFill(Color.WHITE);
        }
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        board[x][y] = color;
    }

    private void drawBoard() {
        gc.setFill(Color.rgb(222, 184, 135));
        gc.fillRect(0, 0, BOARD_PX, BOARD_PX);

        gc.setStroke(Color.BLACK);
        for (int i = 0; i < SIZE; i++) {
            gc.strokeLine(MARGIN, MARGIN + i * CELL, MARGIN + (SIZE - 1) * CELL, MARGIN + i * CELL);
            gc.strokeLine(MARGIN + i * CELL, MARGIN, MARGIN + i * CELL, MARGIN + (SIZE - 1) * CELL);
        }

        int[][] starPoints = {{3,3}, {3,11}, {7,7}, {11,3}, {11,11}};
        gc.setFill(Color.BLACK);
        for (int[] p : starPoints) {
            gc.fillOval(MARGIN + p[0] * CELL - 3, MARGIN + p[1] * CELL - 3, 6, 6);
        }
    }

    private void updateTurnLabel() {
        if (currentTurn == 1) {
            turnLabel.setText("当前回合: 黑方 (" + blackName + ")");
            myTurn = myColor.equals("black");
        } else {
            turnLabel.setText("当前回合: 白方 (" + whiteName + ")");
            myTurn = myColor.equals("white");
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

    @Override
    public void onMessage(String command, String[] args) {
        if (Protocol.MOVE_SYNC.equals(command)) {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
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
        }
    }
}
