package com.example.doanmobile002.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GameScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(GameScoreEntity entity);

    @Query("SELECT * FROM game_scores WHERE id = :id")
    GameScoreEntity getById(String id);

    @Query("SELECT * FROM game_scores ORDER BY gameType, difficulty")
    LiveData<List<GameScoreEntity>> getAllLive();

    @Query("SELECT * FROM game_scores WHERE gameType = :gameType")
    LiveData<List<GameScoreEntity>> getByGameType(String gameType);
}