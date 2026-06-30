package com.example.doanmobile002.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Quản lý cỡ chữ WebView theo phần trăm (%).
 *
 * Mặc định: 100% (bằng trang gốc)
 * Phạm vi : 80% → 150%  (bước 10%)
 *
 * WebView.getSettings().setTextZoom(int percent) — API chính thức của Android,
 * không cần inject JS, áp dụng ngay lập tức cho toàn bộ trang.
 */
public class FontSizeManager {

    private static final String PREF_NAME    = "app_settings";
    private static final String KEY_FONT_PCT = "reader_font_percent";

    public static final int MIN_PERCENT     = 80;
    public static final int MAX_PERCENT     = 150;
    public static final int DEFAULT_PERCENT = 100;
    public static final int STEP            = 10;

    private FontSizeManager() {}

    /** Lấy % hiện tại (80–150), mặc định 100 */
    public static int getFontPercent(Context ctx) {
        return getPrefs(ctx).getInt(KEY_FONT_PCT, DEFAULT_PERCENT);
    }

    /** Lưu % mới */
    public static void setFontPercent(Context ctx, int percent) {
        int clamped = Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
        getPrefs(ctx).edit().putInt(KEY_FONT_PCT, clamped).apply();
    }

    /** Nhãn hiển thị: "80%", "100%", ... */
    public static String getLabel(int percent) {
        return percent + "%";
    }

    private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}