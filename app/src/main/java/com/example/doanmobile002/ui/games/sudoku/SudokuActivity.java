package com.example.doanmobile002.ui.games.sudoku;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.doanmobile002.R;
import com.example.doanmobile002.data.repository.GameRepository;

public class SudokuActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "extra_difficulty";
    private static final int MAX_MISTAKES = 3;

    private String difficulty;
    private int[][] puzzle;     // 0 = ô trống
    private int[][] solution;
    private int[][] userInput;  // số người dùng đã điền (0 nếu chưa)
    private boolean[][] isFixed; // true = số đề bài, không sửa được

    private TextView[][] cellViews;
    private GridLayout boardGrid;
    private TextView tvMistakes, tvTimer, tvSelectedNumber;

    private int selectedRow = -1, selectedCol = -1;
    private int mistakeCount = 0;

    private GameRepository repo;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime;
    private boolean timerRunning = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (timerRunning) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                tvTimer.setText(String.format("⏱ %02d:%02d", elapsed / 60, elapsed % 60));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);

        difficulty = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        if (difficulty == null) difficulty = "easy";

        repo = new GameRepository(this);
        setupToolbar();
        bindViews();
        setupNumberPad();
        startNewGame();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.sdToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(
                    "hard".equals(difficulty) ? "Sudoku - Khó" : "Sudoku - Dễ");
        }
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void bindViews() {
        boardGrid  = findViewById(R.id.sdBoardGrid);
        tvMistakes = findViewById(R.id.tvMistakes);
        tvTimer    = findViewById(R.id.tvSdTimer);
    }

    // ── Khởi tạo game mới ────────────────────────────────────────────────────
    private void startNewGame() {
        SudokuGenerator generator = new SudokuGenerator();
        generator.generateSolution();
        solution = generator.getSolution();
        puzzle   = generator.generatePuzzle(difficulty);

        userInput = new int[9][9];
        isFixed   = new boolean[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                userInput[r][c] = puzzle[r][c];
                isFixed[r][c]   = puzzle[r][c] != 0;
            }
        }

        mistakeCount = 0;
        updateMistakeDisplay();
        stopTimer();
        tvTimer.setText("⏱ 00:00");

        buildBoardUI();
        startTimer();
    }

    private void buildBoardUI() {
        boardGrid.removeAllViews();
        boardGrid.setColumnCount(9);
        boardGrid.setRowCount(9);

        int cellSizePx = dpToPx(36);
        cellViews = new TextView[9][9];

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(r), GridLayout.spec(c));
                params.width  = cellSizePx;
                params.height = cellSizePx;

                // Margin đặc biệt để tạo viền đậm phân chia khối 3x3
                int marginRight  = (c == 2 || c == 5) ? 3 : 1;
                int marginBottom = (r == 2 || r == 5) ? 3 : 1;
                params.setMargins(1, 1, marginRight, marginBottom);

                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(16);

                final int row = r, col = c;
                cell.setOnClickListener(v -> onCellSelected(row, col));

                boardGrid.addView(cell);
                cellViews[r][c] = cell;
            }
        }
        renderBoard();
    }

    // ── Bàn phím số (1-9) ────────────────────────────────────────────────────
    private void setupNumberPad() {
        int[] btnIds = {
                R.id.btnNum1, R.id.btnNum2, R.id.btnNum3,
                R.id.btnNum4, R.id.btnNum5, R.id.btnNum6,
                R.id.btnNum7, R.id.btnNum8, R.id.btnNum9
        };
        for (int i = 0; i < 9; i++) {
            final int number = i + 1;
            findViewById(btnIds[i]).setOnClickListener(v -> onNumberInput(number));
        }
        findViewById(R.id.btnErase).setOnClickListener(v -> onNumberInput(0));
    }

    // ── Chọn ô ───────────────────────────────────────────────────────────────
    private void onCellSelected(int r, int c) {
        selectedRow = r;
        selectedCol = c;
        renderBoard();
    }

    // ── Nhập số ──────────────────────────────────────────────────────────────
    private void onNumberInput(int number) {
        if (selectedRow == -1 || isFixed[selectedRow][selectedCol]) return;

        if (number == 0) {
            // Xóa số
            userInput[selectedRow][selectedCol] = 0;
            renderBoard();
            return;
        }

        userInput[selectedRow][selectedCol] = number;

        if (number != solution[selectedRow][selectedCol]) {
            // SAI
            mistakeCount++;
            updateMistakeDisplay();
            renderBoard();

            if (mistakeCount >= MAX_MISTAKES) {
                stopTimer();
                showLoseDialog();
            }
        } else {
            // ĐÚNG
            renderBoard();
            if (checkWin()) {
                stopTimer();
                long elapsedMs = System.currentTimeMillis() - startTime;
                repo.recordWin("sudoku", difficulty, elapsedMs);
                showWinDialog();
            }
        }
    }

    private boolean checkWin() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (userInput[r][c] != solution[r][c]) return false;
        return true;
    }

    // ── Render bàn cờ ────────────────────────────────────────────────────────
    private void renderBoard() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                TextView cell = cellViews[r][c];
                int value = userInput[r][c];

                cell.setText(value == 0 ? "" : String.valueOf(value));

                boolean selected   = (r == selectedRow && c == selectedCol);
                boolean sameNumber = selectedRow != -1 &&
                        value != 0 && value == userInput[selectedRow][selectedCol];
                boolean isWrong = !isFixed[r][c] && value != 0 &&
                        value != solution[r][c];

                if (selected) {
                    cell.setBackgroundColor(Color.parseColor("#BBDEFB"));
                } else if (sameNumber) {
                    cell.setBackgroundColor(Color.parseColor("#E3F2FD"));
                } else {
                    cell.setBackgroundColor(Color.WHITE);
                }

                if (isWrong) {
                    cell.setTextColor(Color.parseColor("#E53935"));
                } else if (isFixed[r][c]) {
                    cell.setTextColor(Color.parseColor("#1A1A2E"));
                    cell.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    cell.setTextColor(Color.parseColor("#1877F2"));
                    cell.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        }
    }

    private void updateMistakeDisplay() {
        tvMistakes.setText(String.format("❌ %d/%d", mistakeCount, MAX_MISTAKES));
    }

    // ── Timer ────────────────────────────────────────────────────────────────
    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunning = true;
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    private void showLoseDialog() {
        repo.recordLoss("sudoku", difficulty);
        new AlertDialog.Builder(this)
                .setTitle("😢 Thua cuộc!")
                .setMessage("Bạn đã sai 3 lần. Phải bắt đầu lại từ đầu.")
                .setCancelable(false)
                .setPositiveButton("Chơi bàn mới", (d, w) -> startNewGame())
                .setNegativeButton("Thoát", (d, w) -> finish())
                .show();
    }

    private void showWinDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 Chúc mừng!")
                .setMessage("Bạn đã hoàn thành bàn Sudoku!")
                .setCancelable(false)
                .setPositiveButton("Chơi bàn mới", (d, w) -> startNewGame())
                .setNegativeButton("Thoát", (d, w) -> finish())
                .show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}