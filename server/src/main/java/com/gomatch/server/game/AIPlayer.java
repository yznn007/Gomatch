package com.gomatch.server.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIPlayer {
    private static final Logger log = LoggerFactory.getLogger(AIPlayer.class);
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
        this.board = new int[Game.SIZE][Game.SIZE];
    }

    public void updateBoard(int[][] newBoard) {
        for (int i = 0; i < Game.SIZE; i++) {
            System.arraycopy(newBoard[i], 0, board[i], 0, Game.SIZE);
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
     * 简单难度：20% 概率随机走子，80% 评分（攻防权重 4:6，防守略多但仍具威胁）
     */
    private int[] getEasyMove() {
        // 20% 概率随机走子
        if (random.nextInt(100) < 20) {
            List<int[]> candidates = new ArrayList<>();
            for (int x = 0; x < Game.SIZE; x++) {
                for (int y = 0; y < Game.SIZE; y++) {
                    if (board[x][y] == Game.EMPTY && hasNeighbor(x, y)) {
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

        for (int x = 0; x < Game.SIZE; x++) {
            for (int y = 0; y < Game.SIZE; y++) {
                if (board[x][y] == Game.EMPTY) {
                    if (!hasNeighbor(x, y)) continue;

                    board[x][y] = aiColor;
                    int attackScore = evaluatePosition(x, y, aiColor);
                    board[x][y] = Game.EMPTY;

                    board[x][y] = playerColor;
                    int defendScore = evaluatePosition(x, y, playerColor);
                    board[x][y] = Game.EMPTY;

                    // 偏防守但更积极：攻击权重 40%，防守权重 60%
                    int score = (int)(attackScore * 0.4) + (int)(defendScore * 0.6);

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
     * 中等难度：1 层 Minimax 前瞻搜索（全盘评估每步走法）
     */
    private int[] getMediumMove() {
        return hardMinimax(1);
    }

    /**
     * 困难难度：3 层 Minimax 前瞻搜索 + 强势进攻
     */
    private int[] getHardMove() {
        return hardMinimax(3);
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
                board[move[0]][move[1]] = Game.EMPTY;
                log.info("AI (HARD/win) move: ({},{})", move[0], move[1]);
                return move;
            }
            board[move[0]][move[1]] = Game.EMPTY;
        }

        // 检查是否必须防守（对手即将获胜）
        for (int[] move : candidates) {
            board[move[0]][move[1]] = playerColor;
            if (checkWin(move[0], move[1], playerColor)) {
                board[move[0]][move[1]] = Game.EMPTY;
                log.info("AI (HARD/block) move: ({},{})", move[0], move[1]);
                return move;
            }
            board[move[0]][move[1]] = Game.EMPTY;
        }

        // 检查AI能否形成活四（_XXXX_）- 优先进攻
        int[] liveFourMove = findBestPatternMove(candidates, aiColor, true, true);
        if (liveFourMove != null) {
            log.info("AI (HARD/liveFour) move: ({},{})", liveFourMove[0], liveFourMove[1]);
            return liveFourMove;
        }

        // 检查对手能否形成活四（_XXXX_）- 必须防守
        int[] blockLiveFourMove = findBestPatternMove(candidates, playerColor, false, true);
        if (blockLiveFourMove != null) {
            log.info("AI (HARD/blockLiveFour) move: ({},{})", blockLiveFourMove[0], blockLiveFourMove[1]);
            return blockLiveFourMove;
        }

        // 检查AI能否形成活三（_XXX_）- 优先进攻
        int[] liveThreeMove = findBestPatternMove(candidates, aiColor, true, false);
        if (liveThreeMove != null) {
            log.info("AI (HARD/liveThree) move: ({},{})", liveThreeMove[0], liveThreeMove[1]);
            return liveThreeMove;
        }

        // 检查对手能否形成活三（_XXX_）- 必须防守
        int[] blockLiveThreeMove = findBestPatternMove(candidates, playerColor, false, false);
        if (blockLiveThreeMove != null) {
            log.info("AI (HARD/blockLiveThree) move: ({},{})", blockLiveThreeMove[0], blockLiveThreeMove[1]);
            return blockLiveThreeMove;
        }

        // 启发式排序：优先评估攻防价值高的走法，提升 Alpha-Beta 剪枝效率
        candidates.sort((a, b) -> {
            board[a[0]][a[1]] = aiColor;
            int aAtk = evaluatePosition(a[0], a[1], aiColor);
            board[a[0]][a[1]] = Game.EMPTY;
            board[a[0]][a[1]] = playerColor;
            int aDef = evaluatePosition(a[0], a[1], playerColor);
            board[a[0]][a[1]] = Game.EMPTY;

            board[b[0]][b[1]] = aiColor;
            int bAtk = evaluatePosition(b[0], b[1], aiColor);
            board[b[0]][b[1]] = Game.EMPTY;
            board[b[0]][b[1]] = playerColor;
            int bDef = evaluatePosition(b[0], b[1], playerColor);
            board[b[0]][b[1]] = Game.EMPTY;

            return Integer.compare(bAtk + bDef, aAtk + aDef);
        });

        for (int[] move : candidates) {
            board[move[0]][move[1]] = aiColor;
            int score = minimax(depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            board[move[0]][move[1]] = Game.EMPTY;

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
                board[move[0]][move[1]] = Game.EMPTY;
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
                board[move[0]][move[1]] = Game.EMPTY;
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    /**
     * 评估整个棋盘：根据难度调整攻防权重
     */
    private int evaluateBoard() {
        int aiScore = 0;
        int playerScore = 0;
        for (int x = 0; x < Game.SIZE; x++) {
            for (int y = 0; y < Game.SIZE; y++) {
                if (board[x][y] == aiColor) {
                    aiScore += evaluatePosition(x, y, aiColor);
                } else if (board[x][y] == playerColor) {
                    playerScore += evaluatePosition(x, y, playerColor);
                }
            }
        }
        
        switch (difficulty) {
            case EASY:
                return (int)(aiScore * 1.2) - playerScore;
            case MEDIUM:
                return aiScore - (int)(playerScore * 1.3);
            case HARD:
                return aiScore - (int)(playerScore * 1.5);
            default:
                return aiScore - playerScore;
        }
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
        while (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE && board[nx][ny] == color) {
            count++;
            nx += dx;
            ny += dy;
        }
        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE && board[nx][ny] == color) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        return count;
    }

    private boolean checkLiveFour(int x, int y, int color) {
        return checkLinePattern(x, y, 1, 0, color, 4, 0)
                || checkLinePattern(x, y, 0, 1, color, 4, 0)
                || checkLinePattern(x, y, 1, 1, color, 4, 0)
                || checkLinePattern(x, y, 1, -1, color, 4, 0);
    }

    private boolean checkLiveThree(int x, int y, int color) {
        return checkLinePattern(x, y, 1, 0, color, 3, 0)
                || checkLinePattern(x, y, 0, 1, color, 3, 0)
                || checkLinePattern(x, y, 1, 1, color, 3, 0)
                || checkLinePattern(x, y, 1, -1, color, 3, 0);
    }

    private boolean checkLinePattern(int x, int y, int dx, int dy, int color, int targetCount, int allowedBlocks) {
        int count = 1;
        int leftBlock = 0;
        int rightBlock = 0;

        int nx = x + dx;
        int ny = y + dy;
        while (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE && board[nx][ny] == color) {
            count++;
            nx += dx;
            ny += dy;
        }
        if (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE) {
            if (board[nx][ny] != Game.EMPTY) {
                rightBlock = 1;
            }
        } else {
            rightBlock = 1;
        }

        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE && board[nx][ny] == color) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        if (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE) {
            if (board[nx][ny] != Game.EMPTY) {
                leftBlock = 1;
            }
        } else {
            leftBlock = 1;
        }

        return count == targetCount && (leftBlock + rightBlock) <= allowedBlocks;
    }

    private int[] findBestPatternMove(List<int[]> candidates, int color, boolean isAttack, boolean isFour) {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        for (int[] move : candidates) {
            board[move[0]][move[1]] = color;
            boolean match = isFour ? checkLiveFour(move[0], move[1], color) : checkLiveThree(move[0], move[1], color);
            if (match) {
                int score = evaluatePosition(move[0], move[1], color);
                if (!isAttack) {
                    score = -score;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
            board[move[0]][move[1]] = Game.EMPTY;
        }

        return bestMove;
    }

    /**
     * 获取候选落子点（有邻居的空位，优先靠近已有棋子中心）
     */
    private List<int[]> getCandidateMoves() {
        List<int[]> candidates = new ArrayList<>();
        for (int x = 0; x < Game.SIZE; x++) {
            for (int y = 0; y < Game.SIZE; y++) {
                if (board[x][y] == Game.EMPTY && hasNeighbor(x, y)) {
                    candidates.add(new int[]{x, y});
                }
            }
        }
        return candidates;
    }

    private int[] fallbackMove(int bestX, int bestY, int bestScore) {
        if (bestX == -1) {
            for (int x = 0; x < Game.SIZE; x++) {
                for (int y = 0; y < Game.SIZE; y++) {
                    if (board[x][y] == Game.EMPTY) {
                        bestX = x;
                        bestY = y;
                        break;
                    }
                }
                if (bestX != -1) break;
            }
        }
        if (bestX == -1) {
            bestX = Game.SIZE / 2;
            bestY = Game.SIZE / 2;
        }
        log.info("AI move: ({},{}), score={}", bestX, bestY, bestScore);
        return new int[]{bestX, bestY};
    }
    
    private boolean hasNeighbor(int x, int y) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE) {
                    if (board[nx][ny] != Game.EMPTY) {
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
        while (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE && board[nx][ny] == color) {
            count++;
            nx += dx;
            ny += dy;
        }
        if (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE) {
            if (board[nx][ny] != Game.EMPTY) {
                rightBlock = 1;
            }
        } else {
            rightBlock = 1;
        }
        
        nx = x - dx;
        ny = y - dy;
        while (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE && board[nx][ny] == color) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        if (nx >= 0 && nx < Game.SIZE && ny >= 0 && ny < Game.SIZE) {
            if (board[nx][ny] != Game.EMPTY) {
                leftBlock = 1;
            }
        } else {
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
            } else {
                return BLOCKED_FOUR_SCORE / 2;
            }
        }
        
        if (count == 3) {
            if (totalBlock == 0) {
                return THREE_SCORE;
            } else if (totalBlock == 1) {
                return BLOCKED_THREE_SCORE;
            } else {
                return BLOCKED_THREE_SCORE / 2;
            }
        }
        
        if (count == 2) {
            if (totalBlock == 0) {
                return TWO_SCORE;
            } else if (totalBlock == 1) {
                return BLOCKED_TWO_SCORE;
            } else {
                return BLOCKED_TWO_SCORE / 2;
            }
        }
        
        if (count == 1 && totalBlock < 2) {
            return ONE_SCORE;
        }
        
        return 0;
    }
}