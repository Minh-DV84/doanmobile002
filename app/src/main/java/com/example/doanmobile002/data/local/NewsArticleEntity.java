package com.example.doanmobile002.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Room Entity — bảng lưu bài báo offline.
 * type = "home"  → bài trang chủ (cache tự động)
 * type = "saved" → bài người dùng bấm Lưu
 */
@Entity(tableName = "news_articles")
public class NewsArticleEntity {

    @PrimaryKey
    @NonNull
    public String url = "";

    public String title;
    public String description;
    public String urlToImage;
    public String publishedAt;
    public String sourceName;
    public String author;
    public String type;         // "home" | "saved"
    public long   savedAt;      // timestamp lúc lưu (ms)
    public boolean isSaved;     // người dùng bấm Lưu

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
    }
}