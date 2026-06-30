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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOne(NewsArticleEntity article);

    // ── Update saved status ──────────────────────────────────────────────────
    @Query("UPDATE news_articles SET isSaved = :isSaved WHERE url = :url")
    void updateSaved(String url, boolean isSaved);

    // ── LiveData queries (Room tự emit khi DB thay đổi) ─────────────────────
    @Query("SELECT * FROM news_articles WHERE type = 'home' ORDER BY savedAt DESC")
    LiveData<List<NewsArticleEntity>> getHomeArticles();

    @Query("SELECT * FROM news_articles WHERE isSaved = 1 ORDER BY savedAt DESC")
    LiveData<List<NewsArticleEntity>> getSavedArticles();

    /**
     * Lịch sử đọc — bài có readAt > 0 (đã từng mở DetailActivity).
     * Sắp xếp theo thời điểm đọc gần nhất, không phải thời điểm crawl.
     */
    @Query("SELECT * FROM news_articles WHERE readAt > 0 ORDER BY readAt DESC")
    LiveData<List<NewsArticleEntity>> getHistoryArticles();

    // ── Tìm kiếm offline trong cache ────────────────────────────────────────
    @Query("SELECT * FROM news_articles WHERE title LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    List<NewsArticleEntity> searchLocal(String query);

    // ── Xóa cache trang chủ (giữ bài đã lưu / đã đọc) ──────────────────────
    @Query("DELETE FROM news_articles WHERE type = 'home' AND isSaved = 0 AND readAt = 0")
    void clearHomeCache();

    // ── Kiểm tra 1 bài đã được lưu chưa (dùng cho nút bookmark) ─────────────
    @Query("SELECT EXISTS(SELECT 1 FROM news_articles WHERE url = :url AND isSaved = 1)")
    boolean isArticleSaved(String url);

    /**
     * Đánh dấu 1 bài đã đọc — set readAt = thời điểm hiện tại.
     * Nếu bài chưa tồn tại trong bảng thì không làm gì (phải insert trước).
     */
    @Query("UPDATE news_articles SET readAt = :readAt WHERE url = :url")
    void markAsRead(String url, long readAt);

    /** Xóa toàn bộ lịch sử đọc, KHÔNG xóa bài đã lưu (isSaved giữ nguyên) */
    @Query("UPDATE news_articles SET readAt = 0 WHERE readAt > 0 AND isSaved = 0")
    void clearHistory();

    /** Xóa 1 mục lịch sử cụ thể (vẫn giữ nếu đã lưu) */
    @Query("UPDATE news_articles SET readAt = 0 WHERE url = :url AND isSaved = 0")
    void removeFromHistory(String url);
}