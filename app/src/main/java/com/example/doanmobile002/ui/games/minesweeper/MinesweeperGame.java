package com.example.doanmobile002.ui.games.minesweeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Logic thuần Minesweeper — không phụ thuộc Android UI.
 * Dễ test, dễ tái sử dụng.
 */
public class MinesweeperGame {

    public final int rows, cols, mineCount;
    public final MinesweeperCell[][] board;

    private boolean firstClickDone = false;
    private boolean gameOver        = false;
    private boolean won             = false;
    private int      revealedCount  = 0;
    private int      flagCount      = 0;

    public MinesweeperGame(int rows, int cols, int mineCount) {
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.board = new MinesweeperCell[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                board[r][c] = new MinesweeperCell();
        // Mìn được đặt SAU lần click đầu tiên (đảm bảo không thua ngay)
    }

    /** Kết quả click 1 ô */
    public enum ClickResult { SAFE, MINE_HIT, ALREADY_REVEALED, FLAGGED, WIN }

    public ClickResult reveal(int r, int c) {
        if (gameOver) return ClickResult.ALREADY_REVEALED;
        MinesweeperCell cell = board[r][c];
        if (cell.isFlagged) return ClickResult.FLAGGED;
        if (cell.isRevealed) return ClickResult.ALREADY_REVEALED;

        // Đặt mìn ở lần click đầu (tránh nổ ngay lần đầu)
        if (!firstClickDone) {
            placeMines(r, c);
            firstClickDone = true;
        }

        if (cell.isMine) {
            cell.isRevealed = true;
            gameOver = true;
            won = false;
            revealAllMines();
            return ClickResult.MINE_HIT;
        }

        floodReveal(r, c);

        if (revealedCount == rows * cols - mineCount) {
            gameOver = true;
            won = true;
            return ClickResult.WIN;
        }
        return ClickResult.SAFE;
    }

    /** Đặt/Bỏ cờ trên 1 ô — trả về true nếu thành công */
    public boolean toggleFlag(int r, int c) {
        MinesweeperCell cell = board[r][c];
        if (cell.isRevealed) return false;
        cell.isFlagged = !cell.isFlagged;
        flagCount += cell.isFlagged ? 1 : -1;
        return true;
    }

    // ── Flood fill mở các ô liền kề có 0 mìn xung quanh ─────────────────────
    private void floodReveal(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        MinesweeperCell cell = board[r][c];
        if (cell.isRevealed || cell.isFlagged) return;

        cell.isRevealed = true;
        revealedCount++;

        if (cell.adjacentMines == 0) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    floodReveal(r + dr, c + dc);
                }
            }
        }
    }

    // ── Đặt mìn ngẫu nhiên, tránh ô (safeR, safeC) và 8 ô lân cận ──────────
    private void placeMines(int safeR, int safeC) {
        Random rnd = new Random();
        List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (Math.abs(r - safeR) > 1 || Math.abs(c - safeC) > 1)
                    positions.add(new int[]{r, c});

        for (int i = 0; i < mineCount && !positions.isEmpty(); i++) {
            int idx = rnd.nextInt(positions.size());
            int[] pos = positions.remove(idx);
            board[pos[0]][pos[1]].isMine = true;
        }
        calculateAdjacency();
    }

    private void calculateAdjacency() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].isMine) continue;
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                                && board[nr][nc].isMine) count++;
                    }
                }
                board[r][c].adjacentMines = count;
            }
        }
    }

    private void revealAllMines() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].isMine) board[r][c].isRevealed = true;
    }

    public boolean isGameOver()   { return gameOver; }
    public boolean isWon()        { return won; }
    public int     getFlagCount() { return flagCount; }
    public int     getRemainingMines() { return mineCount - flagCount; }
}