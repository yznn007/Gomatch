package com.gomatch.client.ai;

/**
 * 简单启发式五子棋 AI
 * 对每个空位评估攻防得分，选最高分位置落子
 */
public class GomokuAI {
    private static final int SIZE = 15;
    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

    /**
     * 根据当前棋盘计算 AI 落子位置
     * @param board 15x15 棋盘，1=黑，2=白
     * @param aiColor AI 执子颜色
     * @return {x, y} 落子坐标，null 表示无可用位置
     */
    public static int[] findBestMove(int[][] board, int aiColor) {
        int opponentColor = (aiColor == 1) ? 2 : 1;
        int bestScore = -1;
        int[] bestMove = null;

        // 棋盘为空时下天元
        boolean empty = true;
        for (int i = 0; i < SIZE && empty; i++) {
            for (int j = 0; j < SIZE && empty; j++) {
                if (board[i][j] != 0) empty = false;
            }
        }
        if (empty) return new int[]{7, 7};

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (board[x][y] != 0) continue;
                // 只评估周围有棋子的空位（距离2以内）
                if (!hasNeighbor(board, x, y, 2)) continue;

                int attackScore = evaluatePosition(board, x, y, aiColor);
                int defendScore = evaluatePosition(board, x, y, opponentColor);

                // 必胜：自己能五连
                if (attackScore >= 100000) return new int[]{x, y};
                // 必防：对手能五连
                if (defendScore >= 100000) {
                    bestScore = 100000;
                    bestMove = new int[]{x, y};
                    continue;
                }

                // 综合评分：进攻权重略高于防守
                int score = Math.max(attackScore, defendScore) + (attackScore + defendScore);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = new int[]{x, y};
                }
            }
        }
        return bestMove;
    }

    /**
     * 评估在 (x,y) 落子某颜色后的得分
     */
    private static int evaluatePosition(int[][] board, int x, int y, int color) {
        int totalScore = 0;
        for (int[] d : DIRECTIONS) {
            int count = 1;
            int openEnds = 0;

            // 正方向
            int nx = x + d[0], ny = y + d[1];
            while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
                count++;
                nx += d[0];
                ny += d[1];
            }
            if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == 0) {
                openEnds++;
            }

            // 反方向
            nx = x - d[0];
            ny = y - d[1];
            while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
                count++;
                nx -= d[0];
                ny -= d[1];
            }
            if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == 0) {
                openEnds++;
            }

            totalScore += scorePattern(count, openEnds);
        }
        return totalScore;
    }

    /**
     * 根据连子数和开放端数评分
     */
    private static int scorePattern(int count, int openEnds) {
        if (openEnds == 0 && count < 5) return 0; // 两端都被堵，无价值
        if (count >= 5) return 100000;  // 五连
        if (count == 4) {
            if (openEnds == 2) return 50000;  // 活四
            if (openEnds == 1) return 5000;   // 冲四
        }
        if (count == 3) {
            if (openEnds == 2) return 3000;   // 活三
            if (openEnds == 1) return 500;    // 眠三
        }
        if (count == 2) {
            if (openEnds == 2) return 300;    // 活二
            if (openEnds == 1) return 50;     // 眠二
        }
        if (count == 1) {
            if (openEnds == 2) return 20;     // 活一
            if (openEnds == 1) return 5;      // 眠一
        }
        return 0;
    }

    /**
     * 检查 (x,y) 周围 distance 格内是否有棋子
     */
    private static boolean hasNeighbor(int[][] board, int x, int y, int distance) {
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] != 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
