package com.example.doanmobile002.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.doanmobile002.data.local.AppDatabase;
import com.example.doanmobile002.data.local.GameScoreDao;
import com.example.doanmobile002.data.local.GameScoreEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameRepository {

    private final GameScoreDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GameRepository(Context context) {
        dao = AppDatabase.getInstance(context).gameScoreDao();
    }

    public LiveData<List<GameScoreEntity>> getAllScoresLive() {
        return dao.getAllLive();
    }

    /** Gọi khi người chơi THẮNG — tự so sánh và chỉ lưu nếu là kỷ lục mới */
    public void recordWin(String gameType, String difficulty, long timeMs) {
        String id = gameType + "_" + difficulty;
        executor.execute(() -> {
            GameScoreEntity entity = dao.getById(id);
            if (entity == null) {
                entity = new GameScoreEntity(id, gameType, difficulty);
            }
            entity.playCount++;
            entity.winCount++;
            entity.lastPlayedAt = System.currentTimeMillis();
            if (timeMs < entity.bestTimeMs) {
                entity.bestTimeMs = timeMs;
            }
            dao.insertOrUpdate(entity);
        });
    }

    /** Gọi khi người chơi THUA — chỉ tăng playCount */
    public void recordLoss(String gameType, String difficulty) {
        String id = gameType + "_" + difficulty;
        executor.execute(() -> {
            GameScoreEntity entity = dao.getById(id);
            if (entity == null) {
                entity = new GameScoreEntity(id, gameType, difficulty);
            }
            entity.playCount++;
            entity.lastPlayedAt = System.currentTimeMillis();
            dao.insertOrUpdate(entity);
        });
    }

    /** Lấy thời gian tốt nhất hiện tại (đồng bộ, dùng để hiện UI) */
    public interface BestTimeCallback {
        void onResult(long bestTimeMs); // Long.MAX_VALUE nếu chưa có
    }

    public void getBestTime(String gameType, String difficulty, BestTimeCallback callback) {
        String id = gameType + "_" + difficulty;
        executor.execute(() -> {
            GameScoreEntity entity = dao.getById(id);
            long best = entity != null ? entity.bestTimeMs : Long.MAX_VALUE;
            callback.onResult(best);
        });
    }
}