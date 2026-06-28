package com.example.doanmobile002.ui.games;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.doanmobile002.R;
import com.example.doanmobile002.data.local.GameScoreEntity;
import com.example.doanmobile002.data.repository.GameRepository;
import com.example.doanmobile002.ui.games.minesweeper.MinesweeperActivity;
import com.example.doanmobile002.ui.games.sudoku.SudokuActivity;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class GamesFragment extends Fragment {

    private GameRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_games, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new GameRepository(requireContext());

        // ── Minesweeper cards ──────────────────────────────────────────────
        view.findViewById(R.id.cardMinesweeperEasy).setOnClickListener(v ->
                openMinesweeper("easy"));
        view.findViewById(R.id.cardMinesweeperHard).setOnClickListener(v ->
                openMinesweeper("hard"));

        // ── Sudoku cards ───────────────────────────────────────────────────
        view.findViewById(R.id.cardSudokuEasy).setOnClickListener(v ->
                openSudoku("easy"));
        view.findViewById(R.id.cardSudokuHard).setOnClickListener(v ->
                openSudoku("hard"));

        loadBestTimes(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) loadBestTimes(getView());
    }

    private void openMinesweeper(String difficulty) {
        Intent intent = new Intent(requireContext(), MinesweeperActivity.class);
        intent.putExtra(MinesweeperActivity.EXTRA_DIFFICULTY, difficulty);
        startActivity(intent);
    }

    private void openSudoku(String difficulty) {
        Intent intent = new Intent(requireContext(), SudokuActivity.class);
        intent.putExtra(SudokuActivity.EXTRA_DIFFICULTY, difficulty);
        startActivity(intent);
    }

    private void loadBestTimes(View root) {
        TextView tvMsEasy = root.findViewById(R.id.tvMinesweeperEasyBest);
        TextView tvMsHard = root.findViewById(R.id.tvMinesweeperHardBest);
        TextView tvSdEasy = root.findViewById(R.id.tvSudokuEasyBest);
        TextView tvSdHard = root.findViewById(R.id.tvSudokuHardBest);

        repo.getBestTime("minesweeper", "easy", t ->
                postBest(tvMsEasy, t));
        repo.getBestTime("minesweeper", "hard", t ->
                postBest(tvMsHard, t));
        repo.getBestTime("sudoku", "easy", t ->
                postBest(tvSdEasy, t));
        repo.getBestTime("sudoku", "hard", t ->
                postBest(tvSdHard, t));
    }

    private void postBest(TextView tv, long timeMs) {
        if (tv == null || !isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (timeMs == Long.MAX_VALUE) {
                tv.setText("Chưa có kỷ lục");
            } else {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs);
                tv.setText(String.format("Kỷ lục: %02d:%02d", seconds / 60, seconds % 60));
            }
        });
    }
}