package com.gomatch.server.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {
    private static final Logger log = LoggerFactory.getLogger(Game.class);

    public static final int SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private final int[][] board = new int[SIZE][SIZE];
    private int currentPlayer = BLACK;
    private boolean finished = false;
    private int winner = EMPTY;
    private int moveCount = 0;

    private final List<MoveRecord> moveHistory = new ArrayList<>();

    public static class MoveRecord {
        public int x;
        public int y;
        public int color;
        public long userId;

        public MoveRecord(int x, int y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public MoveRecord(int x, int y, int color, long userId) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.userId = userId;
        }

        @Override
        public String toString() {
            return String.format("MoveRecord{x=%d, y=%d, color=%d, userId=%d}", x, y, color, userId);
        }
    }

    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isFinished() { return finished; }
    public int getMoveCount() { return moveCount; }
    public int[][] getBoard() { return board; }
    public List<MoveRecord> getMoveHistory() { return Collections.unmodifiableList(moveHistory); }

    public void reset() {
        log.debug("Resetting game board: SIZE={}", SIZE);
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        currentPlayer = BLACK;
        finished = false;
        winner = EMPTY;
        moveCount = 0;
        moveHistory.clear();
        log.info("Game reset complete: currentPlayer=BLACK, finished=false, moveCount=0");
    }

    public boolean move(int x, int y) {
        return move(x, y, 0);
    }

    public boolean move(int x, int y, long userId) {
        log.debug("Attempting move: x={}, y={}, userId={}, currentPlayer={}, finished={}",
                x, y, userId, currentPlayer == BLACK ? "BLACK" : "WHITE", finished);

        if (finished) {
            log.warn("Move rejected: game already finished");
            return false;
        }

        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
            log.warn("Move rejected: invalid coordinates ({},{}), SIZE={}", x, y, SIZE);
            return false;
        }

        if (board[x][y] != EMPTY) {
            log.warn("Move rejected: position ({},{}) already occupied by player {}",
                    x, y, board[x][y] == BLACK ? "BLACK" : "WHITE");
            return false;
        }

        board[x][y] = currentPlayer;
        moveCount++;
        moveHistory.add(new MoveRecord(x, y, currentPlayer, userId));
        log.info("Move successful: ({},{}) by player {} (userId={}), moveCount={}",
                x, y, currentPlayer == BLACK ? "BLACK" : "WHITE", userId, moveCount);

        if (checkWin(x, y)) {
            finished = true;
            winner = currentPlayer;
            log.info("Game ended: player {} (userId={}) won at position ({},{})",
                    currentPlayer == BLACK ? "BLACK" : "WHITE", userId, x, y);
        } else if (moveCount == SIZE * SIZE) {
            finished = true;
            winner = EMPTY;
            log.info("Game ended: board full at moveCount={}, draw", moveCount);
        } else {
            currentPlayer = (currentPlayer == BLACK) ? WHITE : BLACK;
            log.debug("Turn switched to: {}", currentPlayer == BLACK ? "BLACK" : "WHITE");
        }
        return true;
    }

    public int getWinner() {
        return winner;
    }

    private boolean checkWin(int x, int y) {
        log.debug("Checking win at position ({},{})", x, y);
        int color = board[x][y];
        int[][] dirs = {{1,0}, {0,1}, {1,1}, {1,-1}};
        String[] dirNames = {"horizontal", "vertical", "diagonal_down", "diagonal_up"};

        for (int d = 0; d < dirs.length; d++) {
            int count = 1;
            for (int sign = -1; sign <= 1; sign += 2) {
                int nx = x + sign * dirs[d][0];
                int ny = y + sign * dirs[d][1];
                while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
                    count++;
                    nx += sign * dirs[d][0];
                    ny += sign * dirs[d][1];
                }
            }
            log.debug("Direction {}: count={}", dirNames[d], count);
            if (count >= 5) {
                log.info("Win detected at ({},{}) in {} direction with {} consecutive stones",
                        x, y, dirNames[d], count);
                return true;
            }
        }
        log.debug("No win detected at ({},{})", x, y);
        return false;
    }

    public synchronized boolean undo() {
        log.debug("Attempting undo: moveCount={}, finished={}", moveCount, finished);

        if (moveHistory.isEmpty()) {
            log.warn("Undo rejected: no moves to undo");
            return false;
        }

        MoveRecord lastMove = moveHistory.remove(moveHistory.size() - 1);
        board[lastMove.x][lastMove.y] = EMPTY;
        moveCount--;
        currentPlayer = lastMove.color;
        
        if (finished) {
            finished = false;
            winner = EMPTY;
            log.debug("Undo reset game state: finished=false, winner=EMPTY");
        }

        log.info("Undo successful: removed move ({},{}) by player {}, moveCount={}",
                lastMove.x, lastMove.y, lastMove.color == BLACK ? "BLACK" : "WHITE", moveCount);
        return true;
    }
}