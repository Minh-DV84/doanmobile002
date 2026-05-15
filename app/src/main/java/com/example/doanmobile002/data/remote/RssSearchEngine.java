package com.example.doanmobile002.data.remote;

import com.example.doanmobile002.models.NewsArticle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Tìm kiếm tin tức tiếng Việt bằng cách:
 *  1. Pull nhiều RSS feed chủ đề từ VnExpress, Tuổi Trẻ, Dân Trí, Zing
 *  2. Filter bài nào có keyword trong title hoặc description
 *  3. Sort theo publishedAt mới nhất
 *
 * Ưu điểm:
 *  - 100% tiếng Việt
 *  - Không cần API key
 *  - Không giới hạn request
 *  - Không bị block (RSS là public)
 */
public class RssSearchEngine {

    // ── RSS feeds theo chủ đề — càng nhiều feed thì kết quả tìm kiếm càng rộng ──
    private static final String[][] ALL_FEEDS = {
            // VnExpress
            {"https://vnexpress.net/rss/tin-moi-nhat.rss",             "VnExpress"},
            {"https://vnexpress.net/rss/thoi-su.rss",                  "VnExpress"},
            {"https://vnexpress.net/rss/kinh-doanh.rss",               "VnExpress"},
            {"https://vnexpress.net/rss/the-gioi.rss",                 "VnExpress"},
            {"https://vnexpress.net/rss/giai-tri.rss",                 "VnExpress"},
            {"https://vnexpress.net/rss/the-thao.rss",                 "VnExpress"},
            {"https://vnexpress.net/rss/khoa-hoc.rss",                 "VnExpress"},
            {"https://vnexpress.net/rss/suc-khoe.rss",                 "VnExpress"},
            {"https://vnexpress.net/rss/phap-luat.rss",                "VnExpress"},
            // Tuổi Trẻ
            {"https://tuoitre.vn/rss/tin-moi-nhat.rss",                "Tuổi Trẻ"},
            {"https://tuoitre.vn/rss/thoi-su.rss",                     "Tuổi Trẻ"},
            {"https://tuoitre.vn/rss/kinh-te.rss",                     "Tuổi Trẻ"},
            {"https://tuoitre.vn/rss/the-gioi.rss",                    "Tuổi Trẻ"},
            {"https://tuoitre.vn/rss/giai-tri.rss",                    "Tuổi Trẻ"},
            {"https://tuoitre.vn/rss/the-thao.rss",                    "Tuổi Trẻ"},
            {"https://tuoitre.vn/rss/khoa-hoc.rss",                    "Tuổi Trẻ"},
            {"https://tuoitre.vn/rss/suc-khoe.rss",                    "Tuổi Trẻ"},
            // Dân Trí
            {"https://dantri.com.vn/rss/home.rss",                     "Dân Trí"},
            {"https://dantri.com.vn/rss/xa-hoi.rss",                   "Dân Trí"},
            {"https://dantri.com.vn/rss/kinh-doanh.rss",               "Dân Trí"},
            {"https://dantri.com.vn/rss/the-gioi.rss",                 "Dân Trí"},
            {"https://dantri.com.vn/rss/giai-tri.rss",                 "Dân Trí"},
            {"https://dantri.com.vn/rss/the-thao.rss",                 "Dân Trí"},
            {"https://dantri.com.vn/rss/suc-manh-so.rss",              "Dân Trí"},
            {"https://dantri.com.vn/rss/suc-khoe.rss",                 "Dân Trí"},
            // Zing News
            {"https://zingnews.vn/news.rss",                           "Zing News"},
            {"https://zingnews.vn/xa-hoi.rss",                         "Zing News"},
            {"https://zingnews.vn/kinh-doanh.rss",                     "Zing News"},
            {"https://zingnews.vn/the-gioi.rss",                       "Zing News"},
            {"https://zingnews.vn/giai-tri.rss",                       "Zing News"},
            {"https://zingnews.vn/the-thao.rss",                       "Zing News"},
            {"https://zingnews.vn/suc-khoe.rss",                       "Zing News"},
    };

    private static final int MAX_RESULTS  = 30; // tối đa bao nhiêu bài trả về
    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    /**
     * Tìm kiếm theo keyword. Chạy trên background thread — KHÔNG gọi từ main thread.
     *
     * @param query  từ khóa tìm kiếm (tiếng Việt hoặc không dấu đều OK)
     * @return danh sách bài viết match, sort mới nhất trước
     */
    public static List<NewsArticle> search(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();

        String[] keywords = normalizeKeywords(query.trim());
        List<NewsArticle> combined = Collections.synchronizedList(new ArrayList<>());

        // Fetch tất cả feed song song
        List<Future<?>> futures = new ArrayList<>();
        for (String[] feed : ALL_FEEDS) {
            final String url    = feed[0];
            final String source = feed[1];
            futures.add(executor.submit(() -> {
                List<NewsArticle> articles = RssParser.parse(url, source);
                for (NewsArticle a : articles) {
                    if (matches(a, keywords)) {
                        combined.add(a);
                    }
                }
            }));
        }

        // Chờ tất cả hoàn thành (hoặc timeout)
        for (Future<?> f : futures) {
            try { f.get(10, java.util.concurrent.TimeUnit.SECONDS); }
            catch (Exception ignored) {}
        }

        // Sort mới nhất trước, giới hạn kết quả
        List<NewsArticle> result = new ArrayList<>(combined);
        result.sort((a, b) -> {
            String da = a.getPublishedAt() != null ? a.getPublishedAt() : "";
            String db = b.getPublishedAt() != null ? b.getPublishedAt() : "";
            return db.compareTo(da);
        });

        // Loại bỏ duplicate theo URL
        result = deduplicateByUrl(result);

        return result.size() > MAX_RESULTS ? result.subList(0, MAX_RESULTS) : result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Kiểm tra bài có chứa ít nhất 1 keyword trong title hoặc description.
     * So sánh không phân biệt hoa thường, hỗ trợ cả có dấu lẫn không dấu.
     */
    private static boolean matches(NewsArticle article, String[] keywords) {
        String title = article.getTitle() != null
                ? removeDiacritics(article.getTitle().toLowerCase()) : "";
        String desc  = article.getDescription() != null
                ? removeDiacritics(article.getDescription().toLowerCase()) : "";

        for (String kw : keywords) {
            if (title.contains(kw) || desc.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Tách query thành các keyword riêng.
     * Tạo cả phiên bản có dấu lẫn không dấu để match rộng hơn.
     */
    private static String[] normalizeKeywords(String query) {
        String lower    = query.toLowerCase();
        String noDiacritic = removeDiacritics(lower);
        // Nếu query có nhiều từ, thêm cả từng từ riêng lẻ (>= 3 ký tự)
        List<String> kws = new ArrayList<>();
        kws.add(lower);
        kws.add(noDiacritic);
        for (String word : noDiacritic.split("\\s+")) {
            if (word.length() >= 3) kws.add(word);
        }
        return kws.toArray(new String[0]);
    }

    private static List<NewsArticle> deduplicateByUrl(List<NewsArticle> list) {
        List<NewsArticle> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (NewsArticle a : list) {
            String url = a.getUrl() != null ? a.getUrl() : a.getTitle();
            if (url != null && seen.add(url)) result.add(a);
        }
        return result;
    }

    /**
     * Loại bỏ dấu tiếng Việt để so sánh không dấu.
     * "bóng đá" → "bong da", "sức khỏe" → "suc khoe"
     */
    private static String removeDiacritics(String input) {
        if (input == null) return "";
        input = input
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]",         "e")
                .replaceAll("[ìíịỉĩ]",                 "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]",    "o")
                .replaceAll("[ùúụủũưừứựửữ]",           "u")
                .replaceAll("[ỳýỵỷỹ]",                 "y")
                .replaceAll("[đ]",                      "d")
                .replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]",    "a")
                .replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]",           "e")
                .replaceAll("[ÌÍỊỈĨ]",                  "i")
                .replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]",    "o")
                .replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]",           "u")
                .replaceAll("[ỲÝỴỶỸ]",                 "y")
                .replaceAll("[Đ]",                      "d");
        return input;
    }
}