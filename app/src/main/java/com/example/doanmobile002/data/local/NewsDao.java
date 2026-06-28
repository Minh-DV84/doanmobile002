package com.example.doanmobile002.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NewsDao {

    // ── Insert ───────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<NewsArticleEntity> articles);

    // ── Update saved status ──────────────────────────────────────────────────
    @Query("UPDATE news_articles SET isSaved = :isSaved WHERE url = :url")
    void updateSaved(String url, boolean isSaved);

    // ── LiveData queries (Room tự emit khi DB thay đổi) ─────────────────────
    @Query("SELECT * FROM news_articles WHERE type = 'home' ORDER BY savedAt DESC")
    LiveData<List<NewsArticleEntity>> getHomeArticles();

    @Query("SELECT * FROM news_articles WHERE isSaved = 1 ORDER BY savedAt DESC")
    LiveData<List<NewsArticleEntity>> getSavedArticles();

    // ── Tìm kiếm offline trong cache ────────────────────────────────────────
    @Query("SELECT * FROM news_articles WHERE title LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    List<NewsArticleEntity> searchLocal(String query);

    // ── Xóa cache trang chủ (giữ bài đã lưu) ───────────────────────────────
    @Query("DELETE FROM news_articles WHERE type = 'home' AND isSaved = 0")
    void clearHomeCache();

    // ── Kiểm tra 1 bài đã được lưu chưa (dùng cho nút bookmark) ─────────────
    @Query("SELECT EXISTS(SELECT 1 FROM news_articles WHERE url = :url AND isSaved = 1)")
    boolean isArticleSaved(String url);
}