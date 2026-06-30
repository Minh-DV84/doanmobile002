package com.example.doanmobile002.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tự động xin và làm mới token cho VNAppMob Gold API.
 *
 * Token VNAppMob hết hạn sau 15 NGÀY kể từ lúc xin (theo doc chính thức:
 * https://vapi.vnappmob.com/gold.v2.html). Class này tự gọi endpoint xin
 * token mới khi token hiện tại đã hết hạn hoặc sắp hết hạn, lưu lại trong
 * SharedPreferences kèm thời điểm hết hạn — KHÔNG cần ai phải tay copy token
 * mới dán vào code mỗi 15 ngày nữa.
 *
 * Cách dùng (trong WidgetRepository, trước khi gọi goldApi.getSjcGoldPrice):
 *   GoldTokenManager tokenManager = new GoldTokenManager(context);
 *   tokenManager.getBearerToken(bearer -> {
 *       goldApi.getSjcGoldPrice(bearer).enqueue(...);
 *   });
 */
public class GoldTokenManager {

    private static final String TAG = "GoldTokenManager";

    private static final String REQUEST_TOKEN_URL =
            "https://vapi.vnappmob.com/api/request_api_key?scope=gold";

    private static final String PREFS_NAME       = "gold_token_prefs";
    private static final String KEY_TOKEN        = "gold_token";
    private static final String KEY_EXPIRES_AT   = "gold_token_expires_at"; // epoch millis

    // Xin token mới sớm hơn hạn thật 1 ngày, để tránh trường hợp app không mở
    // đúng lúc token hết hạn (request sẽ fail 403 nếu chờ tới sát hạn mới đổi).
    private static final long SAFETY_MARGIN_MS = 24L * 60 * 60 * 1000;

    private static final int TIMEOUT_MS = 8_000;

    public interface TokenCallback {
        /** Luôn được gọi — token có thể là token mới, token cũ còn hạn, hoặc null nếu xin thất bại */
        void onToken(String bearerTokenOrNull);
    }

    private final SharedPreferences prefs;

    public GoldTokenManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Lấy chuỗi "Bearer <token>" sẵn sàng để gọi API. Gọi network ĐỒNG BỘ nếu
     * cần xin token mới — PHẢI gọi hàm này từ background thread, không gọi từ
     * main thread.
     *
     * @return "Bearer xxx" hoặc null nếu không xin được token (gọi nơi dùng tự
     *         xử lý fallback, tương tự cách PetrolRssParser.fetch() trả null).
     */
    public String getBearerTokenSync() {
        String cached = prefs.getString(KEY_TOKEN, null);
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
        long now = System.currentTimeMillis();

        if (cached != null && now < expiresAt - SAFETY_MARGIN_MS) {
            return "Bearer " + cached;
        }

        // Token chưa có, hoặc đã/sắp hết hạn → xin token mới
        String newToken = requestNewToken();
        if (newToken == null) {
            // Xin mới thất bại (vd. mất mạng) — vẫn thử dùng token cũ nếu còn,
            // tốt hơn là trả về null hoàn toàn (token có thể vẫn còn hạn thật
            // dù đã qua mốc an toàn 1 ngày).
            return cached != null ? "Bearer " + cached : null;
        }

        long newExpiresAt = now + (15L * 24 * 60 * 60 * 1000); // 15 ngày từ bây giờ
        prefs.edit()
                .putString(KEY_TOKEN, newToken)
                .putLong(KEY_EXPIRES_AT, newExpiresAt)
                .apply();

        return "Bearer " + newToken;
    }

    /** Gọi GET request_api_key?scope=gold, trả về token (chuỗi JWT) hoặc null nếu lỗi. */
    private String requestNewToken() {
        try {
            URL url = new URL(REQUEST_TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            if (conn.getResponseCode() != 200) {
                Log.w(TAG, "request_api_key trả về mã lỗi: " + conn.getResponseCode());
                conn.disconnect();
                return null;
            }

            InputStream is = conn.getInputStream();
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, "UTF-8"));
            }
            conn.disconnect();

            // Response dạng: {"results":"eyJhbGci...."}
            String body = sb.toString();
            Matcher m = Pattern.compile("\"results\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            if (m.find()) {
                return m.group(1);
            }
            Log.w(TAG, "Không parse được token từ response: " + body);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Xin token mới thất bại: " + e.getMessage());
            return null;
        }
    }
}