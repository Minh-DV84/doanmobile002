package com.example.doanmobile002.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { NewsArticleEntity.class, GameScoreEntity.class },
        version  = 3,   // ← tăng từ 2 lên 3: thêm cột readAt cho lịch sử đọc
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract NewsDao newsDao();
    public abstract GameScoreDao gameScoreDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "kabar_database"
                            )
                            .fallbackToDestructiveMigration() // Đơn giản cho đồ án
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}