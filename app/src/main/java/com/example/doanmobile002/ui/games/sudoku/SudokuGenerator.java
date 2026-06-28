package com.example.doanmobile002.ui.games.sudoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Sinh bàn Sudoku 9x9 hợp lệ bằng backtracking,
 * sau đó đục lỗ theo độ khó.
 */
public class SudokuGenerator {

    private final int[][] solution = new int[9][9];
    private final Random  random   = new Random();

    public int[][] getSolution() { return solution; }

    /** Sinh bàn đầy đủ hợp lệ (lời giải) */
    public void generateSolution() {
        fillBoard(0, 0);
    }

    private boolean fillBoard(int row, int col) {
        if (row == 9) return true;
        int nextRow = (col == 8) ? row + 1 : row;
        int nextCol = (col == 8) ? 0 : col + 1;

        List<Integer> numbers = shuffledNumbers();
        for (int num : numbers) {
            if (isValid(row, col, num)) {
                solution[row][col] = num;
                if (fillBoard(nextRow, nextCol)) return true;
                solution[row][col] = 0;
            }
        }
        return false;
    }

    private List<Integer> shuffledNumbers() {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= 9; i++) list.add(i);
        Collections.shuffle(list, random);
        return list;
    }

    private boolean isValid(int row, int col, int num) {
        for (int c = 0; c < 9; c++) if (solution[row][c] == num) return false;
        for (int r = 0; r < 9; r++) if (solution[r][col] == num) return false;
        int boxRow = (row / 3) * 3, boxCol = (col / 3) * 3;
        for (int r = boxRow; r < boxRow + 3; r++)
            for (int c = boxCol; c < boxCol + 3; c++)
                if (solution[r][c] == num) return false;
        return true;
    }

    /**
     * Đục lỗ tạo đề bài.
     * easy → giữ ~45 số (đục 36 ô)
     * hard → giữ ~28 số (đục 53 ô)
     */
    public int[][] generatePuzzle(String difficulty) {
        int[][] puzzle = new int[9][9];
        for (int r = 0; r < 9; r++)
            puzzle[r] = solution[r].clone();

        int cellsToRemove = "hard".equals(difficulty) ? 53 : 36;

        List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                positions.add(new int[]{r, c});
        Collections.shuffle(positions, random);

        for (int i = 0; i < cellsToRemove && i < positions.size(); i++) {
            int[] pos = positions.get(i);
            puzzle[pos[0]][pos[1]] = 0;
        }
        return puzzle;
    }
}