package com.example.doanmobile002.ui.games.minesweeper;

import android.content.Intent;
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

public class MinesweeperActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "extra_difficulty"; // "easy" | "hard"

    private MinesweeperGame game;
    private GameRepository  repo;
    private String          difficulty;

    private int rows, cols, mineCount;
    private TextView[][] cellViews;

    private GridLayout boardGrid;
    private TextView   tvMineCount, tvTimer, tvSmiley;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long    startTime;
    private boolean timerRunning = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (timerRunning) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                tvTimer.setText(String.format("⏱ %03d", Math.min(elapsed, 999)));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minesweeper);

        difficulty = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        if (difficulty == null) difficulty = "easy";

        repo = new GameRepository(this);
        setupDifficulty();
        setupToolbar();
        bindViews();
        startNewGame();
    }

    private void setupDifficulty() {
        if ("hard".equals(difficulty)) {
            rows = 16; cols = 16; mineCount = 40;
        } else {
            rows = 8; cols = 8; mineCount = 10;
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.msToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(
                    "hard".equals(difficulty) ? "Dò Mìn - Khó" : "Dò Mìn - Dễ");
        }
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void bindViews() {
        boardGrid   = findViewById(R.id.msBoardGrid);
        tvMineCount = findViewById(R.id.tvMineCount);
        tvTimer     = findViewById(R.id.tvTimer);
        tvSmiley    = findViewById(R.id.tvSmiley);

        tvSmiley.setOnClickListener(v -> startNewGame());
    }

    // ── Khởi tạo bàn mới ─────────────────────────────────────────────────────
    private void startNewGame() {
        game = new MinesweeperGame(rows, cols, mineCount);
        stopTimer();
        tvTimer.setText("⏱ 000");
        tvSmiley.setText("🙂");
        updateMineCounter();
        buildBoardUI();
    }

    private void buildBoardUI() {
        boardGrid.removeAllViews();
        boardGrid.setColumnCount(cols);
        boardGrid.setRowCount(rows);

        // Kích thước ô tuỳ độ khó để vừa màn hình
        int cellSizeDp = "hard".equals(difficulty) ? 20 : 38;
        int cellSizePx = dpToPx(cellSizeDp);

        cellViews = new TextView[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                TextView cellView = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(r), GridLayout.spec(c));
                params.width  = cellSizePx;
                params.height = cellSizePx;
                params.setMargins(1, 1, 1, 1);
                cellView.setLayoutParams(params);
                cellView.setGravity(Gravity.CENTER);
                cellView.setTextSize("hard".equals(difficulty) ? 10 : 16);
                cellView.setBackgroundResource(R.drawable.bg_ms_cell_hidden);

                final int row = r, col = c;
                cellView.setOnClickListener(v -> onCellClick(row, col));
                cellView.setOnLongClickListener(v -> {
                    onCellLongClick(row, col);
                    return true;
                });

                boardGrid.addView(cellView);
                cellViews[r][c] = cellView;
            }
        }
    }

    // ── Click thường: mở ô ───────────────────────────────────────────────────
    private void onCellClick(int r, int c) {
        if (game.isGameOver()) return;
        if (!timerRunning) startTimer();

        MinesweeperGame.ClickResult result = game.reveal(r, c);

        switch (result) {
            case MINE_HIT:
                renderBoard();
                stopTimer();
                tvSmiley.setText("😵");
                showGameOverDialog(false);
                break;
            case WIN:
                renderBoard();
                stopTimer();
                tvSmiley.setText("😎");
                long elapsedMs = System.currentTimeMillis() - startTime;
                repo.recordWin("minesweeper", difficulty, elapsedMs);
                showGameOverDialog(true);
                break;
            case SAFE:
                renderBoard();
                break;
            default:
                break;
        }
    }

    // ── Long press: đặt/bỏ cờ ───────────────────────────────────────────────
    private void onCellLongClick(int r, int c) {
        if (game.isGameOver()) return;
        if (game.toggleFlag(r, c)) {
            renderCell(r, c);
            updateMineCounter();
        }
    }

    // ── Render toàn bộ bàn cờ ────────────────────────────────────────────────
    private void renderBoard() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                renderCell(r, c);
    }

    private void renderCell(int r, int c) {
        MinesweeperCell cell = game.board[r][c];
        TextView view = cellViews[r][c];

        if (cell.isRevealed) {
            view.setBackgroundResource(R.drawable.bg_ms_cell_revealed);
            if (cell.isMine) {
                view.setText("💣");
            } else if (cell.adjacentMines > 0) {
                view.setText(String.valueOf(cell.adjacentMines));
                view.setTextColor(getNumberColor(cell.adjacentMines));
            } else {
                view.setText("");
            }
        } else if (cell.isFlagged) {
            view.setBackgroundResource(R.drawable.bg_ms_cell_hidden);
            view.setText("🚩");
        } else {
            view.setBackgroundResource(R.drawable.bg_ms_cell_hidden);
            view.setText("");
        }
    }

    private int getNumberColor(int n) {
        switch (n) {
            case 1: return Color.parseColor("#1976D2");
            case 2: return Color.parseColor("#388E3C");
            case 3: return Color.parseColor("#D32F2F");
            case 4: return Color.parseColor("#7B1FA2");
            case 5: return Color.parseColor("#FF8F00");
            case 6: return Color.parseColor("#00897B");
            case 7: return Color.parseColor("#000000");
            default: return Color.parseColor("#757575");
        }
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

    private void updateMineCounter() {
        tvMineCount.setText(String.format("🚩 %02d", game.getRemainingMines()));
    }

    // ── Dialog kết quả ───────────────────────────────────────────────────────
    private void showGameOverDialog(boolean won) {
        new AlertDialog.Builder(this)
                .setTitle(won ? "🎉 Chiến thắng!" : "💥 Thua rồi!")
                .setMessage(won ?
                        "Bạn đã dò hết mìn thành công!" :
                        "Bạn đã đạp trúng mìn. Thử lại nhé!")
                .setCancelable(false)
                .setPositiveButton("Chơi lại", (d, w) -> startNewGame())
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