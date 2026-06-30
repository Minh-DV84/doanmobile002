package com.example.doanmobile002.data.remote;

import com.example.doanmobile002.models.NewsArticle;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses RSS 2.0 feeds from VnExpress, Tuổi Trẻ, Dân Trí, Zing.
 * Runs on a background thread — callers must NOT invoke from the main thread.
 */
public class RssParser {

    private static final int TIMEOUT_MS = 10_000;

    /**
     * Download and parse an RSS feed URL.
     *
     * @param feedUrl  full RSS URL
     * @param source   human-readable source name (e.g. "VnExpress")
     * @return list of articles; empty list on error
     */
    public static List<NewsArticle> parse(String feedUrl, String source) {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            URL url = new URL(feedUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            // Chặn cache để mỗi lần gọi (đặc biệt khi pull-to-refresh) đều
            // lấy bản mới nhất từ server, không trả lại response cũ đã cache
            conn.setUseCaches(false);
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0");
            conn.setRequestProperty("Pragma", "no-cache");
            conn.connect();

            InputStream is = conn.getInputStream();
            articles = parseStream(is, source);
            conn.disconnect();
        } catch (Exception e) {
            // Return empty — caller handles fallback
        }
        return articles;
    }

    private static List<NewsArticle> parseStream(InputStream is, String source) throws Exception {
        List<NewsArticle> articles = new ArrayList<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(is, "UTF-8");

        String title = null, link = null, description = null,
                pubDate = null, imageUrl = null;
        boolean inItem = false;
        String currentTag = "";

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                currentTag = parser.getName();

                if ("item".equalsIgnoreCase(currentTag)) {
                    inItem = true;
                    title = link = description = pubDate = imageUrl = null;
                }

                // <enclosure url="..." type="image/jpeg"/> or <media:content url="..."/>
                if (inItem) {
                    if ("enclosure".equalsIgnoreCase(currentTag)
                            || "content".equalsIgnoreCase(currentTag)
                            || "thumbnail".equalsIgnoreCase(currentTag)) {
                        String urlAttr = parser.getAttributeValue(null, "url");
                        if (urlAttr != null && !urlAttr.isEmpty()) imageUrl = urlAttr;
                    }
                }

            } else if (eventType == XmlPullParser.TEXT && inItem) {
                String text = parser.getText().trim();
                if (text.isEmpty()) {
                    eventType = parser.next();
                    continue;
                }
                switch (currentTag) {
                    case "title":       title       = text; break;
                    case "link":        link        = text; break;
                    case "description":
                        description = stripHtml(text);
                        // Try to extract image from description HTML
                        if (imageUrl == null) imageUrl = extractImgSrc(text);
                        break;
                    case "pubDate":     pubDate     = text; break;
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if ("item".equalsIgnoreCase(parser.getName()) && inItem) {
                    inItem = false;
                    if (title != null && !title.equals("[Removed]")) {
                        articles.add(new NewsArticle(
                                title, description, imageUrl,
                                link, pubDate, source, null));
                    }
                }
                currentTag = "";
            }

            eventType = parser.next();
        }
        return articles;
    }

    /** Very simple: strip all HTML tags */
    private static String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]*>", "").trim();
    }

    /** Extract first img src from HTML string */
    private static String extractImgSrc(String html) {
        if (html == null) return null;
        int idx = html.indexOf("src=\"");
        if (idx < 0) idx = html.indexOf("src='");
        if (idx < 0) return null;
        char quote = html.charAt(idx + 4);
        int start = idx + 5;
        int end   = html.indexOf(quote, start);
        if (end < 0) return null;
        String src = html.substring(start, end);
        return src.startsWith("http") ? src : null;
    }
}