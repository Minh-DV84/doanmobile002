package com.example.doanmobile002.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.doanmobile002.data.remote.GoldApiService;
import com.example.doanmobile002.data.remote.GoldTokenManager;
import com.example.doanmobile002.data.remote.PetrolRssParser;
import com.example.doanmobile002.data.remote.RetrofitClient;
import com.example.doanmobile002.data.remote.WeatherApiService;
import com.example.doanmobile002.models.VnAppMobGoldResponse;
import com.example.doanmobile002.models.WeatherResponse;
import com.example.doanmobile002.models.WidgetData;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetches all 4 widget card data:
 *
 *  Card 1 — Thứ/Ngày      → system clock (synchronous)
 *  Card 2 — Thời tiết     → WeatherAPI.com
 *  Card 3 — Giá xăng      → scrape baomoi.com (Petrolimex, không cần key)
 *                           Fallback: giá static gần nhất
 *  Card 4 — Giá vàng SJC  → api.vnappmob.com/api/v2/gold/sjc
 *                           Fallback: giá static gần nhất
 *
 *  Callback onResult() được gọi 1 lần sau khi TẤT CẢ 3 async task hoàn thành.
 *
 *  Token VNAppMob Gold được TỰ ĐỘNG xin và làm mới bởi GoldTokenManager —
 *  không cần ai tay đổi token mỗi 15 ngày nữa. Token mới được lưu trong
 *  SharedPreferences, dùng lại cho tới khi gần hết hạn (xem GoldTokenManager).
 */
public class WidgetRepository {

    private static final String TAG = "WidgetRepository";

    // ── Keys ─────────────────────────────────────────────────────────────────
    private static final String WEATHER_KEY  = "965faee10a044554b7881212261505";
    private static final String WEATHER_CITY = "Ha Noi";

    // ── Static fallback (cập nhật theo kỳ điều hành thực tế) ─────────────────
    private static final String FALLBACK_E10RON95  = "19,910 đ/L";
    private static final String FALLBACK_GOLD_BUY  = "119.50";
    private static final String FALLBACK_GOLD_SELL = "121.80";

    private final WeatherApiService  weatherApi;
    private final GoldApiService     goldApi;
    private final GoldTokenManager   goldTokenManager;
    private final ExecutorService    executor    = Executors.newFixedThreadPool(3);
    private final Handler            mainHandler = new Handler(Looper.getMainLooper());

    public interface WidgetCallback {
        /** Luôn được gọi — kể cả khi 1 hoặc nhiều nguồn lỗi (partial data) */
        void onResult(WidgetData data);
    }

    /**
     * @param context dùng để khởi tạo SharedPreferences cho GoldTokenManager —
     *                 truyền context của Application hoặc Activity đều được,
     *                 GoldTokenManager tự lấy applicationContext bên trong.
     */
    public WidgetRepository(Context context) {
        weatherApi       = RetrofitClient.getWeatherService();
        goldApi          = RetrofitClient.getGoldService();
        goldTokenManager = new GoldTokenManager(context);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void loadWidgetData(WidgetCallback callback) {
        WidgetData data = new WidgetData();

        // Card 1: ngay lập tức, không cần network
        buildDayCard(data);

        // 3 tác vụ async chạy song song
        AtomicInteger pending = new AtomicInteger(3);
        Runnable checkDone = () -> {
            if (pending.decrementAndGet() == 0) {
                mainHandler.post(() -> callback.onResult(data));
            }
        };

        fetchWeather(data, checkDone);
        fetchPetrol(data, checkDone);
        fetchGold(data, checkDone);
    }

    // ── Card 1: Thứ/Ngày ─────────────────────────────────────────────────────

    private void buildDayCard(WidgetData data) {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        String[] days = {"Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư",
                "Thứ năm", "Thứ sáu", "Thứ bảy"};
        data.setDayOfWeek(days[dow - 1]);
        data.setDateStr(new SimpleDateFormat("dd/MM/yyyy", new Locale("vi")).format(new Date()));
    }

    // ── Card 2: Thời tiết ─────────────────────────────────────────────────────

    private void fetchWeather(WidgetData data, Runnable onDone) {
        weatherApi.getCurrentWeather(WEATHER_KEY, WEATHER_CITY, "vi")
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call,
                                           Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse wr   = response.body();
                            WeatherResponse.Current  cur = wr.getCurrent();
                            WeatherResponse.Location loc = wr.getLocation();
                            data.setWeatherCity(loc != null ? loc.getName() : WEATHER_CITY);
                            data.setWeatherTemp((int) cur.getTempC() + "°C");
                            if (cur.getCondition() != null) {
                                data.setWeatherDesc(cur.getCondition().getText());
                                String icon = cur.getCondition().getIcon();
                                if (icon != null && icon.startsWith("//")) icon = "https:" + icon;
                                data.setWeatherIcon(icon);
                            }
                        } else {
                            setWeatherFallback(data);
                        }
                        onDone.run();
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        Log.w(TAG, "Weather fetch failed: " + t.getMessage());
                        setWeatherFallback(data);
                        onDone.run();
                    }
                });
    }

    private void setWeatherFallback(WidgetData data) {
        data.setWeatherCity(WEATHER_CITY);
        data.setWeatherTemp("--°C");
        data.setWeatherDesc("Không lấy được");
    }

    // ── Card 3: Giá xăng E10 RON95-III (scrape baomoi.com) ───────────────────

    private void fetchPetrol(WidgetData data, Runnable onDone) {
        executor.execute(() -> {
            try {
                PetrolRssParser.PetrolPrice price = PetrolRssParser.fetch();
                if (price != null && price.e10ron95 != null) {
                    data.setPetrolPrice(price.e10ron95);
                    data.setPetrolDate("E10 RON95  •  " + price.updateNote);
                } else {
                    data.setPetrolPrice(FALLBACK_E10RON95);
                    data.setPetrolDate("E10 RON95  •  Giá tham khảo");
                }
            } catch (Exception e) {
                Log.w(TAG, "Petrol fetch failed: " + e.getMessage());
                data.setPetrolPrice(FALLBACK_E10RON95);
                data.setPetrolDate("E10 RON95  •  Giá tham khảo");
            }
            onDone.run();
        });
    }

    // ── Card 4: Giá vàng SJC (VNAppMob API) ──────────────────────────────────
    // Endpoint /api/v2/gold/sjc trả field "buy_1l" / "sell_1l" (giá theo lượng)
    // Token được GoldTokenManager tự xin/làm mới — xem class đó để biết chi tiết.

    private void fetchGold(WidgetData data, Runnable onDone) {
        // Lấy token có thể cần gọi network (khi token cũ hết hạn) → chạy trên
        // background thread, không gọi trực tiếp từ main thread.
        executor.execute(() -> {
            String bearer = goldTokenManager.getBearerTokenSync();

            if (bearer == null) {
                // Không xin được token (vd. mất mạng ngay từ bước xin token) →
                // dùng fallback ngay, không cần gọi API giá vàng nữa.
                Log.w(TAG, "Không lấy được Gold token, dùng fallback");
                setGoldFallback(data);
                onDone.run();
                return;
            }

            goldApi.getSjcGoldPrice(bearer)
                    .enqueue(new Callback<VnAppMobGoldResponse>() {
                        @Override
                        public void onResponse(Call<VnAppMobGoldResponse> call,
                                               Response<VnAppMobGoldResponse> response) {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().getResults() != null
                                    && !response.body().getResults().isEmpty()) {

                                VnAppMobGoldResponse.GoldResult r =
                                        response.body().getResults().get(0);
                                data.setGoldBuy(formatMillions(r.getBuy1l()));
                                data.setGoldSell(formatMillions(r.getSell1l()));
                                data.setGoldUnit("triệu đ/lượng  •  SJC");

                            } else {
                                Log.w(TAG, "Gold response not successful: code="
                                        + response.code()
                                        + " body=" + (response.body() != null));
                                setGoldFallback(data);
                            }
                            onDone.run();
                        }

                        @Override
                        public void onFailure(Call<VnAppMobGoldResponse> call, Throwable t) {
                            Log.w(TAG, "Gold fetch failed: " + t.getMessage());
                            setGoldFallback(data);
                            onDone.run();
                        }
                    });
        });
    }

    private void setGoldFallback(WidgetData data) {
        data.setGoldBuy(FALLBACK_GOLD_BUY);
        data.setGoldSell(FALLBACK_GOLD_SELL);
        data.setGoldUnit("triệu đ/lượng  •  Giá tham khảo");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Chuyển giá (đồng) sang triệu đồng, LUÔN hiển thị đủ 2 chữ số thập phân
     * và dùng dấu phẩy kiểu Việt Nam thay vì dấu chấm.
     * Ví dụ: 145000000.0 → "145,00"   (không bị rút gọn thành "145")
     *        119500000.0 → "119,50"
     */
    private String formatMillions(double value) {
        double millions = value / 1_000_000.0;
        DecimalFormat df = new DecimalFormat("0.00",
                new java.text.DecimalFormatSymbols(new Locale("vi", "VN")));
        return df.format(millions);   // pattern "0.00" luôn giữ đủ 2 số thập phân
    }
}