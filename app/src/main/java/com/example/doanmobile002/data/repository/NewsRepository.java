package com.example.doanmobile002.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.example.doanmobile002.data.remote.RssParser;
import com.example.doanmobile002.data.remote.RssSearchEngine;
import com.example.doanmobile002.models.NewsArticle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NewsRepository {

    // ── RSS Sources (trang chủ) ──────────────────────────────────────────────
    private static final String[][] RSS_FEEDS = {
            {"https://vnexpress.net/rss/tin-moi-nhat.rss", "VnExpress"},
            {"https://tuoitre.vn/rss/tin-moi-nhat.rss",    "Tuổi Trẻ"},
            {"https://dantri.com.vn/rss/home.rss",          "Dân Trí"},
            {"https://zingnews.vn/news.rss",                "Zing News"},
    };

    private final ExecutorService executor    = Executors.newFixedThreadPool(4);
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    public interface NewsCallback {
        void onSuccess(List<NewsArticle> articles);
        void onError(String message);
    }

    // ── 1. Trang chủ: parallel RSS fetch ────────────────────────────────────
    public void getHomeNews(NewsCallback callback) {
        List<NewsArticle> combined = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining    = new AtomicInteger(RSS_FEEDS.length);

        for (String[] feed : RSS_FEEDS) {
            final String url    = feed[0];
            final String source = feed[1];

            executor.execute(() -> {
                List<NewsArticle> result = RssParser.parse(url, source);
                combined.addAll(result);

                if (remaining.decrementAndGet() == 0) {
                    mainHandler.post(() -> {
                        if (!combined.isEmpty()) {
                            List<NewsArticle> sorted = new ArrayList<>(combined);
                            sorted.sort((a, b) -> {
                                String da = a.getPublishedAt() != null ? a.getPublishedAt() : "";
                                String db = b.getPublishedAt() != null ? b.getPublishedAt() : "";
                                return db.compareTo(da);
                            });
                            callback.onSuccess(sorted);
                        } else {
                            callback.onError("Không thể tải tin tức. Kiểm tra kết nối mạng.");
                        }
                    });
                }
            });
        }
    }

    // ── 2. Tìm kiếm: RSS toàn bộ chủ đề + filter keyword ───────────────────
    /**
     * Tìm kiếm 100% tiếng Việt qua RSS.
     * Hỗ trợ cả tìm có dấu lẫn không dấu: "bong da" tìm được "bóng đá".
     */
    public void searchNews(String query, NewsCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            getHomeNews(callback);
            return;
        }

        final String trimmed = query.trim();
        executor.execute(() -> {
            List<NewsArticle> results = RssSearchEngine.search(trimmed);
            mainHandler.post(() -> {
                if (results.isEmpty()) {
                    callback.onError("Không tìm thấy kết quả cho \"" + trimmed + "\"");
                } else {
                    callback.onSuccess(results);
                }
            });
        });
    }
}