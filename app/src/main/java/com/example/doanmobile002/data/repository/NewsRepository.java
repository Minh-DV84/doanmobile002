package com.example.doanmobile002.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.doanmobile002.data.local.AppDatabase;
import com.example.doanmobile002.data.local.NewsArticleEntity;
import com.example.doanmobile002.data.local.NewsDao;
import com.example.doanmobile002.data.remote.RssParser;
import com.example.doanmobile002.data.remote.RssSearchEngine;
import com.example.doanmobile002.models.NewsArticle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single Source of Truth:
 *  - UI luôn đọc từ Room (LiveData)
 *  - Khi có mạng: fetch RSS → lưu vào Room → Room tự notify UI
 *  - Khi mất mạng: báo offline → UI vẫn hiện dữ liệu cũ từ Room
 */
public class NewsRepository {

    private static final String[][] RSS_FEEDS = {
            {"https://vnexpress.net/rss/tin-moi-nhat.rss", "VnExpress"},
            {"https://tuoitre.vn/rss/tin-moi-nhat.rss",    "Tuổi Trẻ"},
            {"https://dantri.com.vn/rss/home.rss",          "Dân Trí"},
            {"https://zingnews.vn/news.rss",                "Zing News"},
    };

    private final NewsDao         dao;
    private final Context         context;
    private final ExecutorService executor    = Executors.newFixedThreadPool(4);
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    public interface NewsCallback {
        void onSuccess(List<NewsArticle> articles);
        void onError(String message);
    }

    public interface StatusCallback {
        void onOffline();
        void onOnline();
        void onError(String msg);
    }

    public interface SavedStateCallback {
        void onResult(boolean isSaved);
    }

    public NewsRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dao     = AppDatabase.getInstance(this.context).newsDao();
    }

    // ── LiveData trang chủ ───────────────────────────────────────────────────
    public LiveData<List<NewsArticle>> getHomeArticlesLive() {
        return Transformations.map(dao.getHomeArticles(), this::mapEntityList);
    }

    // ── LiveData bài đã lưu ──────────────────────────────────────────────────
    public LiveData<List<NewsArticle>> getSavedArticlesLive() {
        return Transformations.map(dao.getSavedArticles(), this::mapEntityList);
    }

    // ── Fetch trang chủ từ RSS → lưu Room ───────────────────────────────────
    public void syncHomeNews(StatusCallback status) {
        if (!isOnline()) {
            mainHandler.post(status::onOffline);
            return;
        }

        mainHandler.post(status::onOnline);

        List<NewsArticle> combined = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining    = new AtomicInteger(RSS_FEEDS.length);

        for (String[] feed : RSS_FEEDS) {
            final String url    = feed[0];
            final String source = feed[1];

            executor.execute(() -> {
                List<NewsArticle> result = RssParser.parse(url, source);
                combined.addAll(result);

                if (remaining.decrementAndGet() == 0) {
                    if (!combined.isEmpty()) {
                        combined.sort((a, b) -> {
                            String da = a.getPublishedAt() != null ? a.getPublishedAt() : "";
                            String db = b.getPublishedAt() != null ? b.getPublishedAt() : "";
                            return db.compareTo(da);
                        });
                        executor.execute(() -> {
                            dao.clearHomeCache();
                            dao.insertAll(mapArticleList(combined, "home"));
                        });
                    } else {
                        mainHandler.post(() -> status.onError("Không tải được tin tức mới"));
                    }
                }
            });
        }
    }

    // ── Fetch tin xu hướng (không lưu Room, chỉ trả về UI) ──────────────────
    public void fetchTrendingNews(String[][] feeds, NewsCallback callback) {
        if (!isOnline()) {
            mainHandler.post(() -> callback.onError("Không có kết nối mạng"));
            return;
        }

        List<NewsArticle> combined = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining    = new AtomicInteger(feeds.length);

        for (String[] feed : feeds) {
            final String url    = feed[0];
            final String source = feed[1];

            executor.execute(() -> {
                List<NewsArticle> result = RssParser.parse(url, source);
                combined.addAll(result);

                if (remaining.decrementAndGet() == 0) {
                    if (combined.isEmpty()) {
                        mainHandler.post(() ->
                                callback.onError("Không tải được tin xu hướng"));
                        return;
                    }
                    combined.sort((a, b) -> {
                        String da = a.getPublishedAt() != null ? a.getPublishedAt() : "";
                        String db = b.getPublishedAt() != null ? b.getPublishedAt() : "";
                        return db.compareTo(da);
                    });
                    List<NewsArticle> top = combined.size() > 50
                            ? new ArrayList<>(combined.subList(0, 50))
                            : new ArrayList<>(combined);

                    mainHandler.post(() -> callback.onSuccess(top));
                }
            });
        }
    }

    // ── Tìm kiếm ─────────────────────────────────────────────────────────────
    public void searchNews(String query, NewsCallback callback) {
        if (query == null || query.trim().isEmpty()) return;
        final String trimmed = query.trim();

        if (isOnline()) {
            executor.execute(() -> {
                List<NewsArticle> results = RssSearchEngine.search(trimmed);
                mainHandler.post(() -> {
                    if (results.isEmpty())
                        callback.onError("Không tìm thấy kết quả cho \"" + trimmed + "\"");
                    else
                        callback.onSuccess(results);
                });
            });
        } else {
            executor.execute(() -> {
                List<NewsArticleEntity> local = dao.searchLocal(trimmed);
                mainHandler.post(() -> {
                    if (local.isEmpty())
                        callback.onError("Không có kết quả offline cho \"" + trimmed + "\"");
                    else
                        callback.onSuccess(mapEntityList(local));
                });
            });
        }
    }

    // ── Lưu / Bỏ lưu bài ────────────────────────────────────────────────────
    public void toggleSave(NewsArticle article, boolean save) {
        executor.execute(() -> {
            if (save) {
                NewsArticleEntity entity = mapArticle(article, "saved");
                entity.isSaved = true;
                dao.insertAll(Collections.singletonList(entity));
            }
            dao.updateSaved(article.getUrl(), save);
        });
    }

    // ── Kiểm tra 1 bài đã lưu chưa (trả qua callback) ───────────────────────
    public void checkSaved(String url, SavedStateCallback callback) {
        if (url == null || callback == null) return;
        executor.execute(() -> {
            boolean saved = dao.isArticleSaved(url);
            mainHandler.post(() -> callback.onResult(saved));
        });
    }

    // ── Kiểm tra mạng ────────────────────────────────────────────────────────
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps =
                cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private List<NewsArticleEntity> mapArticleList(List<NewsArticle> list, String type) {
        List<NewsArticleEntity> out = new ArrayList<>();
        for (NewsArticle a : list) {
            if (a.getUrl() != null) out.add(mapArticle(a, type));
        }
        return out;
    }

    private NewsArticleEntity mapArticle(NewsArticle a, String type) {
        return new NewsArticleEntity(
                a.getUrl(), a.getTitle(), a.getDescription(),
                a.getUrlToImage(), a.getPublishedAt(),
                a.getSourceName(), a.getAuthor(), type
        );
    }

    private List<NewsArticle> mapEntityList(List<NewsArticleEntity> list) {
        List<NewsArticle> out = new ArrayList<>();
        if (list == null) return out;
        for (NewsArticleEntity e : list) {
            out.add(new NewsArticle(e.title, e.description, e.urlToImage,
                    e.url, e.publishedAt, e.sourceName, e.author));
        }
        return out;
    }
}