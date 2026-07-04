package com.gomatch.server.game;

public class Game {
    public static final int SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private final int[][] board = new int[SIZE][SIZE];
    private int currentPlayer = BLACK;
    private boolean finished = false;

    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isFinished() { return finished; }
    public int[][] getBoard() { return board; }

    public boolean move(int x, int y) {
        if (finished || x < 0 || x >= SIZE || y < 0 || y >= SIZE || board[x][y] != EMPTY) {
            return false;
        }
        board[x][y] = currentPlayer;
        if (checkWin(x, y)) {
            finished = true;
        } else {
            currentPlayer = (currentPlayer == BLACK) ? WHITE : BLACK;
        }
        return true;
    }

    public int getWinner() {
        return finished ? (currentPlayer == BLACK ? BLACK : WHITE) : EMPTY;
    }

    private boolean checkWin(int x, int y) {
        int color = board[x][y];
        int[][] dirs = {{1,0}, {0,1}, {1,1}, {1,-1}};
        for (int[] d : dirs) {
            int count = 1;
            for (int sign = -1; sign <= 1; sign += 2) {
                int nx = x + sign * d[0];
                int ny = y + sign * d[1];
                while (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && board[nx][ny] == color) {
                    count++;
                    nx += sign * d[0];
                    ny += sign * d[1];
                }
            }
            if (count >= 5) return true;
        }
        return false;
    }
}
