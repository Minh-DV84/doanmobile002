package com.example.doanmobile002.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Lưu thời gian/kết quả tốt nhất cho mỗi game + độ khó.
 * id = "minesweeper_easy", "minesweeper_hard", "sudoku_easy", "sudoku_hard"
 */
@Entity(tableName = "game_scores")
public class GameScoreEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    public String gameType;     // "minesweeper" | "sudoku"
    public String difficulty;   // "easy" | "hard"
    public long   bestTimeMs;   // thời gian tốt nhất (ms)
    public int    winCount;     // số lần thắng
    public int    playCount;    // tổng số lần chơi
    public long   lastPlayedAt; // timestamp lần chơi gần nhất

    public GameScoreEntity() {}

    public GameScoreEntity(@NonNull String id, String gameType, String difficulty) {
        this.id           = id;
        this.gameType      = gameType;
        this.difficulty    = difficulty;
        this.bestTimeMs    = Long.MAX_VALUE;
        this.winCount      = 0;
        this.playCount     = 0;
        this.lastPlayedAt  = System.currentTimeMillis();
    }
}