package com.gomatch.server.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    private final int aiColor;
    private final int playerColor;
    private final Random random = new Random();
    private final Difficulty difficulty;
    private int[][] board;

    public AIPlayer(int aiColor) {
        this(aiColor, Difficulty.MEDIUM);
    }

    public AIPlayer(int aiColor, Difficulty difficulty) {
        this.aiColor = aiColor;
        this.playerColor = aiColor == Game.BLACK ? Game.WHITE : Game.BLACK;
        this.difficulty = difficulty;
        this.board = new int[SIZE][SIZE];
    }
    
    public void updateBoard(int[][] newBoard) {
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(newBoard[i], 0, board[i], 0, SIZE);
        }
    }
    
    public int[] getMove(int[][] currentBoard) {
        updateBoard(currentBoard);

        switch (difficulty) {
            case EASY -> {
                return getEasyMove();
            }
            case MEDIUM -> {
                return getMediumMove();
            }
            case HARD -> {
                return getHardMove();
            }
        }
        return getMediumMove();
    }

    /**
     * 简单难度：40% 概率随机走子，60% 弱攻击评分（攻防权重 3:7）
     */
    private int[] getEasyMove() {
        // 40% 概率随机走子
        if (random.nextInt(100) < 40) {
            List<int[]> candidates = new ArrayList<>();
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    if (board[x][y] == EMPTY && hasNeighbor(x, y)) {
                        candidates.add(new int[]{x, y});
                    }
                }
            }
            if (!candidates.isEmpty()) {
                int[] move = candidates.get(random.nextInt(candidates.size()));
                log.info("AI (EASY/random) move: ({},{})", move[0], move[1]);
                return move;
            }
        }

        // 否则用弱攻击评分
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

                    // 攻弱防强：攻击权重 30%，防守权重 70%
                    int score = (int)(attackScore * 0.3) + (int)(defendScore * 0.7);

                    if (score > bestScore) {
                        bestScore = score;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }

        return fallbackMove(bestX, bestY, bestScore);
    }

    /**
     * 中等难度：攻防均衡评分
     */
    private int[] getMediumMove() {
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
                    } else if (score == bestScore && score > 0 && random.nextBoolean()) {
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }

        return fallbackMove(bestX, bestY, bestScore);
    }

    /**
     * 困难难度：二层 MinMax 前瞻搜索 + 强攻击评分
     */
    private int[] getHardMove() {
        return hardMinimax(2);
    }

    private int[] hardMinimax(int depth) {
        int bestScore = Integer.MIN_VALUE;
        int bestX = -1;
        int bestY = -1;

        List<int[]> candidates = getCandidateMoves();

        // 先检查直接获胜
        for (int[] move : candidates) {
            board[move[0]][move[1]] = aiColor;
            if (checkWin(move[0], move[1], aiColor)) {
                board[move[0]][move[1]] = EMPTY;
                log.info("AI (HARD/win) move: ({},{})", move[0], move[1]);
                return move;
            }
            board[move[0]][move[1]] = EMPTY;
        }

        // 检查是否必须防守（对手即将获胜）
        for (int[] move : candidates) {
            board[move[0]][move[1]] = playerColor;
            if (checkWin(move[0], move[1], playerColor)) {
                board[move[0]][move[1]] = EMPTY;
                log.info("AI (HARD/block) move: ({},{})", move[0], move[1]);
                return move;
            }
            board[move[0]][move[1]] = EMPTY;
        }

        for (int[] move : candidates) {
            board[move[0]][move[1]] = aiColor;
            int score = minimax(depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            board[move[0]][move[1]] = EMPTY;

            if (score > bestScore) {
                bestScore = score;
                bestX = move[0];
                bestY = move[1];
            }
        }

        int[] result = fallbackMove(bestX, bestY, bestScore);
        log.info("AI (HARD) move: ({},{}), score={}", result[0], result[1], bestScore);
        return result;
    }

    private int minimax(int depth, int alpha, int beta, boolean isMaximizing) {
        if (depth == 0) {
            return evaluateBoard();
        }

        List<int[]> candidates = getCandidateMoves();
        if (candidates.isEmpty()) return evaluateBoard();

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int[] move : candidates) {
                board[move[0]][move[1]] = aiColor;
                int eval = minimax(depth - 1, alpha, beta, false);
                board[move[0]][move[1]] = EMPTY;
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int[] move : candidates) {
                board[move[0]][move[1]] = playerColor;
                int eval = minimax(depth - 1, alpha, beta, true);
                board[move[0]][move[1]] = EMPTY;
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    /**
     * 评估整个棋盘：AI 分数 - 玩家分数（进攻倾向 1.2 倍）
     */
    private int evaluateBoard() {
        int aiScore = 0;
        int playerScore = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (board[x][y] == aiColor) {
                    aiScore += evaluatePosition(x, y, aiColor);
                } else if (board[x][y] == playerColor) {
                    playerScore += evaluatePosition(x, y, playerColor);
                }
            }
        }
        return (int)(aiScore * 1.1) - playerScore;
    }

    /**
     * 检查 x,y 是否已形成五连
     */
    private boolean checkWin(int x, int y, int color) {
        return (countLine(x, y, 1, 0, color) >= 5)
                || (countLine(x, y, 0, 1, color) >= 5)
                || (countLine(x, y, 1, 1, color) >= 5)
                || (countLine(x, y, 1, -1, color) >= 5);
    }

    private int countLine(int x, int y, int dx, int dy, int color) {
        int count = 1;
        int nx = x + dx;
        int ny = y + dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
            count++;
            nx += dx;
            ny += dy;
        }
        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        return count;
    }

    /**
     * 获取候选落子点（有邻居的空位，优先靠近已有棋子中心）
     */
    private List<int[]> getCandidateMoves() {
        List<int[]> candidates = new ArrayList<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (board[x][y] == EMPTY && hasNeighbor(x, y)) {
                    candidates.add(new int[]{x, y});
                }
            }
        }
        return candidates;
    }

    private int[] fallbackMove(int bestX, int bestY, int bestScore) {
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