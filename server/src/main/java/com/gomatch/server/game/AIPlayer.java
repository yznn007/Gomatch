package com.gomatch.server.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class AIPlayer {
    private static final Logger log = LoggerFactory.getLogger(AIPlayer.class);
    private static final int SIZE = 15;
    private static final int EMPTY = 0;
    private static final int WIN_SCORE = 100000;
    private static final int BLOCKED_FOUR_SCORE = 10000;
    private static final int FOUR_SCORE = 50000;
    private static final int BLOCKED_THREE_SCORE = 1000;
    private static final int THREE_SCORE = 5000;
    private static final int BLOCKED_TWO_SCORE = 100;
    private static final int TWO_SCORE = 500;
    private static final int ONE_SCORE = 50;
    
    private final int aiColor;
    private final int playerColor;
    private final Random random = new Random();
    private int[][] board;
    
    public AIPlayer(int aiColor) {
        this.aiColor = aiColor;
        this.playerColor = aiColor == Game.BLACK ? Game.WHITE : Game.BLACK;
        this.board = new int[SIZE][SIZE];
    }
    
    public void updateBoard(int[][] newBoard) {
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(newBoard[i], 0, board[i], 0, SIZE);
        }
    }
    
    public int[] getMove(int[][] currentBoard) {
        updateBoard(currentBoard);
        
        int bestScore = Integer.MIN_VALUE;
        int bestX = -1;
        int bestY = -1;
        
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (board[x][y] == EMPTY) {
                    if (!hasNeighbor(x, y)) continue;
                    
                    board[x][y] = aiColor;
                    int attackScore = evaluatePosition(x, y, aiColor);
                    board[x][y] = EMPTY;
                    
                    board[x][y] = playerColor;
                    int defendScore = evaluatePosition(x, y, playerColor);
                    board[x][y] = EMPTY;
                    
                    int score = attackScore + defendScore;
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestX = x;
                        bestY = y;
                    } else if (score == bestScore && score > 0) {
                        if (random.nextBoolean()) {
                            bestX = x;
                            bestY = y;
                        }
                    }
                }
            }
        }
        
        if (bestX == -1) {
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    if (board[x][y] == EMPTY) {
                        bestX = x;
                        bestY = y;
                        break;
                    }
                }
                if (bestX != -1) break;
            }
        }
        
        if (bestX == -1) {
            bestX = SIZE / 2;
            bestY = SIZE / 2;
        }
        
        log.info("AI move: ({},{}), score={}", bestX, bestY, bestScore);
        return new int[]{bestX, bestY};
    }
    
    private boolean hasNeighbor(int x, int y) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE) {
                    if (board[nx][ny] != EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private int evaluatePosition(int x, int y, int color) {
        int score = 0;
        
        score += evaluateLine(x, y, 1, 0, color);
        score += evaluateLine(x, y, 0, 1, color);
        score += evaluateLine(x, y, 1, 1, color);
        score += evaluateLine(x, y, 1, -1, color);
        
        return score;
    }
    
    private int evaluateLine(int x, int y, int dx, int dy, int color) {
        int count = 1;
        int leftBlock = 0;
        int rightBlock = 0;
        
        int nx = x + dx;
        int ny = y + dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
            count++;
            nx += dx;
            ny += dy;
        }
        if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] != EMPTY) {
            rightBlock = 1;
        }
        
        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] != EMPTY) {
            leftBlock = 1;
        }
        
        int totalBlock = leftBlock + rightBlock;
        
        if (count >= 5) {
            return WIN_SCORE;
        }
        
        if (count == 4) {
            if (totalBlock == 0) {
                return FOUR_SCORE;
            } else if (totalBlock == 1) {
                return BLOCKED_FOUR_SCORE;
            }
        }
        
        if (count == 3) {
            if (totalBlock == 0) {
                return THREE_SCORE;
            } else if (totalBlock == 1) {
                return BLOCKED_THREE_SCORE;
            }
        }
        
        if (count == 2) {
            if (totalBlock == 0) {
                return TWO_SCORE;
            } else if (totalBlock == 1) {
                return BLOCKED_TWO_SCORE;
            }
        }
        
        if (count == 1 && totalBlock == 0) {
            return ONE_SCORE;
        }
        
        return 0;
    }
}