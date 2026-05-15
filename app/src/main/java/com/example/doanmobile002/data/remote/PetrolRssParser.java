package com.example.doanmobile002.data.remote;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lấy giá xăng mới nhất bằng cách parse RSS VnExpress chủ đề giá xăng.
 * Không cần API key, cập nhật ngay sau mỗi kỳ điều chỉnh (~1 tuần/lần).
 *
 * Chiến lược:
 *  1. Lấy RSS https://vnexpress.net/rss/kinh-doanh/hang-hoa.rss
 *  2. Tìm bài đầu tiên có title chứa "giá xăng"
 *  3. Regex extract giá RON 95 từ description
 *  4. Fallback: trả về null → caller dùng giá static
 */
public class PetrolRssParser {

    // RSS kinh doanh/hàng hóa của VnExpress — luôn có tin giá xăng sau mỗi kỳ điều chỉnh
    private static final String RSS_URL    = "https://vnexpress.net/rss/kinh-doanh/hang-hoa.rss";
    private static final int    TIMEOUT_MS = 8_000;

    public static class PetrolPrice {
        public final String ron95;   // e.g. "20,827"
        public final String e5ron92; // e.g. "19,979"
        public final String updateNote; // e.g. "Cập nhật từ VnExpress"

        public PetrolPrice(String ron95, String e5ron92, String updateNote) {
            this.ron95       = ron95;
            this.e5ron92     = e5ron92;
            this.updateNote  = updateNote;
        }
    }

    /**
     * Parse RSS và extract giá xăng. Chạy trên background thread.
     * @return PetrolPrice hoặc null nếu không lấy được
     */
    public static PetrolPrice fetch() {
        try {
            String rssContent = downloadText(RSS_URL);
            if (rssContent == null) return null;
            return extractFromRss(rssContent);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static PetrolPrice extractFromRss(String rss) {
        // Tìm item đầu tiên có "giá xăng" trong title (case-insensitive)
        // Split theo <item> rồi tìm bài phù hợp
        String[] items = rss.split("<item>");
        for (int i = 1; i < items.length; i++) {
            String item = items[i].toLowerCase();
            if (item.contains("giá xăng") || item.contains("gia xang")
                    || item.contains("xăng dầu") || item.contains("xang dau")) {
                return extractPriceFromItem(items[i]);
            }
        }
        return null;
    }

    private static PetrolPrice extractPriceFromItem(String item) {
        // Lấy description và title
        String description = extractTag(item, "description");
        String title       = extractTag(item, "title");
        String combined    = (title != null ? title : "") + " " + (description != null ? description : "");

        // Loại bỏ HTML tags
        combined = combined.replaceAll("<[^>]*>", " ");

        // Pattern tìm giá RON 95: "95" followed by price in VND
        // e.g. "RON 95-III: 20.827 đồng" hay "xăng RON 95 giảm còn 20,827"
        String ron95   = findPrice(combined, new String[]{
                "95[\\s\\-:]+(?:iii|v|ii)?[\\s:]*([0-9]{2}[.,][0-9]{3})",
                "ron\\s*95[^0-9]*([0-9]{2}[.,][0-9]{3})",
                "xăng\\s*95[^0-9]*([0-9]{2}[.,][0-9]{3})"
        });

        String e5ron92 = findPrice(combined, new String[]{
                "e5[\\s\\-]*ron\\s*92[^0-9]*([0-9]{2}[.,][0-9]{3})",
                "e5[^0-9]*([0-9]{2}[.,][0-9]{3})",
                "ron\\s*92[^0-9]*([0-9]{2}[.,][0-9]{3})"
        });

        if (ron95 == null && e5ron92 == null) return null;

        // Format: thêm "đ/L"
        return new PetrolPrice(
                ron95   != null ? formatPrice(ron95)   : null,
                e5ron92 != null ? formatPrice(e5ron92) : null,
                "Nguồn: VnExpress"
        );
    }

    /**
     * Thử từng regex pattern, trả về group(1) của match đầu tiên.
     */
    private static String findPrice(String text, String[] patterns) {
        String lower = text.toLowerCase();
        for (String pat : patterns) {
            try {
                Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(lower);
                if (m.find()) {
                    return m.group(1);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String formatPrice(String raw) {
        // Chuẩn hoá: thay "." bằng "," nếu cần
        return raw.replace(".", ",") + " đ/L";
    }

    private static String extractTag(String xml, String tag) {
        try {
            int start = xml.indexOf("<" + tag);
            if (start < 0) return null;
            start = xml.indexOf(">", start) + 1;
            // Handle CDATA
            if (xml.startsWith("<![CDATA[", start)) start += 9;
            int end = xml.indexOf("</" + tag + ">", start);
            if (end < 0) return null;
            String val = xml.substring(start, end);
            if (val.endsWith("]]>")) val = val.substring(0, val.length() - 3);
            return val.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static String downloadText(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();

            InputStream is = conn.getInputStream();
            byte[] buf = new byte[65536];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, "UTF-8"));
            }
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}