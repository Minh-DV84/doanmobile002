package com.example.doanmobile002.data.remote;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lấy giá xăng E10 RON95-III mới nhất bằng cách scrape trang giá chuyên biệt
 * tại baomoi.com (nguồn: giaxanghomnay.com, cập nhật theo mỗi kỳ điều hành
 * của Petrolimex).
 *
 * Trang chỉ hiển thị ĐÚNG 1 bảng HTML <table> cho riêng E10 RON95-III, dạng:
 *   <tr><td>Xăng E10 RON 95-III</td><td>19,910</td><td>20,300</td></tr>
 * → không lẫn các loại xăng/dầu khác như trang tổng hợp tien-ich-gia-xang-dau.epi.
 *
 * Từ 1/6/2026: E10 RON95-III thay thế hoàn toàn RON95-III khoáng trên toàn quốc.
 */
public class PetrolRssParser {

    // Trang chuyên biệt chỉ hiển thị 1 bảng giá E10 RON95-III duy nhất
    // (không lẫn các loại xăng/dầu khác như trang tổng hợp tien-ich-gia-xang-dau.epi)
    private static final String URL_BAOMOI   = "https://baomoi.com/tien-ich-gia-xang-dau-xang-e10-ron-95-iii.epi";
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

    // Giá xăng VN trong khoảng hợp lý — dùng để loại bỏ số rác bóc nhầm
    // (quảng cáo, script, id sản phẩm...) không phải giá xăng thật.
    private static final int MIN_VALID_PRICE = 15_000;
    private static final int MAX_VALID_PRICE = 35_000;

    private static PetrolPrice extractFromHtml(String html) {
        // Trang này chỉ có ĐÚNG 1 bảng giá E10 RON95-III, không lẫn loại
        // xăng/dầu khác — nhưng phần "Tin tức liên quan" phía dưới vẫn có
        // nhiều số khác (USD/thùng, tỷ đồng...) nên vẫn cần validate khoảng giá.
        String price = findValidPriceForKeyword(html, "RON 95-III");
        if (price == null) {
            price = findValidPriceForKeyword(html, "RON95-III");
        }
        if (price == null) return null;

        return new PetrolPrice(price + " đ/L", "Nguồn: Petrolimex");
    }

    /**
     * Quét QUA TẤT CẢ các vị trí xuất hiện của keyword trong trang (không chỉ
     * vị trí đầu tiên), tìm dòng <tr> hợp lệ tương ứng, và trả về giá đầu tiên
     * nằm trong khoảng hợp lý [MIN_VALID_PRICE, MAX_VALID_PRICE].
     *
     * Lý do quét nhiều vị trí: baomoi.com là trang tổng hợp tin tức, có thể
     * chứa từ khoá "RON 95-III" ở nhiều chỗ ngoài bảng giá (bài viết liên quan,
     * script, breadcrumb...). Nếu vị trí đầu tiên không phải bảng giá thật,
     * thử các vị trí tiếp theo thay vì lấy nhầm số rác.
     */
    private static String findValidPriceForKeyword(String html, String keyword) {
        String lowerHtml = html.toLowerCase();
        String lowerKw   = keyword.toLowerCase();

        int searchFrom = 0;
        while (true) {
            int kwIndex = lowerHtml.indexOf(lowerKw, searchFrom);
            if (kwIndex < 0) return null; // hết các vị trí khớp, không tìm thấy giá hợp lệ

            String row = extractRow(html, lowerHtml, kwIndex);
            if (row != null) {
                String price = extractValidPriceFromRow(row);
                if (price != null) return price;
            }

            // thử vị trí khớp tiếp theo
            searchFrom = kwIndex + lowerKw.length();
        }
    }

    /**
     * Tìm đoạn <tr>...</tr> bao quanh vị trí kwIndex. Trả về null nếu không
     * tìm thấy thẻ <tr> mở hợp lệ phía trước (tránh cắt nhầm từ giữa nội dung
     * không phải bảng, đây là nguyên nhân chính gây ra số giá sai trước đây).
     */
    private static String extractRow(String html, String lowerHtml, int kwIndex) {
        int trStart = lowerHtml.lastIndexOf("<tr", kwIndex);
        if (trStart < 0) return null; // không có <tr> mở phía trước → không phải bảng, bỏ qua

        int trEnd = lowerHtml.indexOf("</tr>", kwIndex);
        if (trEnd < 0) return null;

        return html.substring(trStart, trEnd + 5);
    }

    /**
     * Lấy số giá đầu tiên trong dòng <tr> nằm trong khoảng hợp lệ.
     * Quét qua TẤT CẢ số khớp định dạng XX,XXX/XX.XXX trong dòng, không chỉ
     * số đầu tiên, để bỏ qua các số rác (id, timestamp...) không phải giá xăng.
     */
    private static String extractValidPriceFromRow(String row) {
        Matcher m = Pattern.compile("([1-9][0-9][.,][0-9]{3})").matcher(row);
        while (m.find()) {
            String raw = m.group(1).replace(".", ",");
            int value;
            try {
                value = Integer.parseInt(raw.replace(",", ""));
            } catch (NumberFormatException e) {
                continue;
            }
            if (value >= MIN_VALID_PRICE && value <= MAX_VALID_PRICE) {
                return raw;
            }
        }
        return null;
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