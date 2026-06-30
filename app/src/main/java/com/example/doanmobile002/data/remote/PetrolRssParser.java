package com.example.doanmobile002.data.remote;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lấy giá xăng E10 RON95-III mới nhất bằng cách scrape bảng giá tại baomoi.com
 * (nguồn: giaxanghomnay.com, cập nhật theo mỗi kỳ điều hành của Petrolimex).
 *
 * Trang hiển thị 1 bảng HTML <table> rõ ràng dạng:
 *   <tr><td>Xăng E10 RON 95-III</td><td>19,910</td><td>20,300</td></tr>
 * → dễ parse hơn nhiều so với trang dạng card/div lồng nhau.
 *
 * Từ 1/6/2026: E10 RON95-III thay thế hoàn toàn RON95-III khoáng trên toàn quốc.
 */
public class PetrolRssParser {

    private static final String URL_BAOMOI   = "https://baomoi.com/tien-ich-gia-xang-dau.epi";
    private static final int    TIMEOUT_MS   = 8_000;

    public static class PetrolPrice {
        public final String e10ron95;   // e.g. "19,910 đ/L" — giá vùng 1
        public final String updateNote; // e.g. "Nguồn: Petrolimex"

        public PetrolPrice(String e10ron95, String updateNote) {
            this.e10ron95   = e10ron95;
            this.updateNote = updateNote;
        }
    }

    /**
     * Scrape giá E10 RON95-III (vùng 1) từ baomoi.com. Chạy trên background thread.
     * @return PetrolPrice hoặc null nếu không lấy được
     */
    public static PetrolPrice fetch() {
        try {
            String html = downloadText(URL_BAOMOI);
            if (html == null) return null;
            return extractFromHtml(html);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static PetrolPrice extractFromHtml(String html) {
        // Bảng giá có dòng dạng:
        // <tr>...Xăng E10 RON 95-III...</tr> chứa 2 số giá (vùng 1, vùng 2)
        // Lấy dòng <tr> chứa "RON 95-III" (ưu tiên) rồi tới "RON 95-V"

        String row = findRowContaining(html, "RON 95-III");
        if (row == null) {
            row = findRowContaining(html, "RON95-III");
        }
        if (row == null) {
            // fallback: thử bản V nếu không tìm thấy bản III
            row = findRowContaining(html, "RON 95-V");
        }
        if (row == null) return null;

        // Trong dòng <tr> đó, lấy số đầu tiên dạng XX,XXX hoặc XX.XXX (giá vùng 1)
        Matcher m = Pattern.compile("([1-9][0-9][.,][0-9]{3})").matcher(row);
        if (m.find()) {
            String raw = m.group(1).replace(".", ",");
            return new PetrolPrice(raw + " đ/L", "Nguồn: Petrolimex");
        }
        return null;
    }

    /**
     * Tìm đoạn <tr>...</tr> đầu tiên có chứa từ khoá cho trước (case-insensitive).
     */
    private static String findRowContaining(String html, String keyword) {
        String lowerHtml = html.toLowerCase();
        String lowerKw   = keyword.toLowerCase();

        int kwIndex = lowerHtml.indexOf(lowerKw);
        if (kwIndex < 0) return null;

        // Lùi về <tr> gần nhất trước keyword
        int trStart = lowerHtml.lastIndexOf("<tr", kwIndex);
        if (trStart < 0) trStart = kwIndex;

        // Tìm </tr> gần nhất sau keyword
        int trEnd = lowerHtml.indexOf("</tr>", kwIndex);
        if (trEnd < 0) return null;

        return html.substring(trStart, trEnd + 5);
    }

    private static String downloadText(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Android) AppleWebKit/537.36");
            conn.setRequestProperty("Accept-Language", "vi-VN,vi;q=0.9");
            conn.connect();

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return null;
            }

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