package com.example.doanmobile002.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Room Entity — bảng lưu bài báo offline.
 *
 * type = "home"  → bài trang chủ (cache tự động, có thể bị clearHomeCache() xoá)
 * type = "saved" → bài người dùng bấm Lưu (Firebase-style bookmark)
 *
 * readAt > 0     → bài đã được mở xem (lịch sử đọc).
 *                  Độc lập với isSaved — 1 bài có thể vừa đã đọc vừa đã lưu,
 *                  hoặc chỉ đã đọc mà chưa lưu.
 *
 * description    → dùng làm "tóm tắt offline" hiển thị trong lịch sử/đã lưu
 *                  khi không có mạng để mở WebView (RSS đã cung cấp sẵn).
 */
@Entity(tableName = "news_articles")
public class NewsArticleEntity {

    @PrimaryKey
    @NonNull
    public String url = "";

    public String  title;
    public String  description;
    public String  urlToImage;
    public String  publishedAt;
    public String  sourceName;
    public String  author;
    public String  type;         // "home" | "saved"
    public long    savedAt;      // timestamp lúc crawl/lưu (ms)
    public boolean isSaved;      // người dùng bấm Lưu
    public long    readAt;       // timestamp lúc mở đọc; 0 = chưa từng đọc

    public NewsArticleEntity() {}

    public NewsArticleEntity(@NonNull String url, String title, String description,
                             String urlToImage, String publishedAt,
                             String sourceName, String author, String type) {
        this.url         = url;
        this.title       = title;
        this.description = description;
        this.urlToImage  = urlToImage;
        this.publishedAt = publishedAt;
        this.sourceName  = sourceName;
        this.author      = author;
        this.type        = type;
        this.savedAt     = System.currentTimeMillis();
        this.isSaved     = false;
        this.readAt      = 0L;
    }
}