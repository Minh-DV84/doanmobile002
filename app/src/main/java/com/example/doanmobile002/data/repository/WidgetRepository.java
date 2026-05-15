package com.example.doanmobile002.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.doanmobile002.data.remote.GoldApiService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetches all 4 widget card data:
 *
 *  Card 1 — Thứ/Ngày      → system clock (synchronous)
 *  Card 2 — Thời tiết     → WeatherAPI.com
 *  Card 3 — Giá xăng      → RSS VnExpress (parse tự động, không cần key)
 *                           Fallback: giá static gần nhất
 *  Card 4 — Giá vàng SJC  → api.vnappmob.com/api/v2/gold/sjc (cần key)
 *                           Fallback: giá static gần nhất
 *
 *  Callback onResult() được gọi 1 lần sau khi TẤT CẢ 3 async task hoàn thành
 *  (hoặc timeout sau 10 giây).
 */
public class WidgetRepository {

    private static final String TAG = "WidgetRepository";

    // ── Keys ─────────────────────────────────────────────────────────────────
    // WeatherAPI.com — đăng ký free tại https://www.weatherapi.com/
    private static final String WEATHER_KEY  = "965faee10a044554b7881212261505";
    private static final String WEATHER_CITY = "Ha Noi";

    // VNAppMob Gold SJC — đăng ký free tại:
    // https://vapi.vnappmob.com/api/request_api_key?scope=gold
    // Dán key vào đây (format: "Bearer <key>")
    private static final String GOLD_BEARER  = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3ODAxMzAwMTMsImlhdCI6MTc3ODgzNDAxMywic2NvcGUiOiJnb2xkIiwicGVybWlzc2lvbiI6MH0.dC0zTW-VCGPL5vfnMXOgX261S44H8l-23iHgKrLVxys";

    // ── Static fallback (cập nhật theo kỳ điều hành thực tế) ─────────────────
    private static final String FALLBACK_RON95   = "20,827 đ/L";
    private static final String FALLBACK_E5RON92 = "19,979 đ/L";
    private static final String FALLBACK_GOLD_BUY  = "119.50";
    private static final String FALLBACK_GOLD_SELL = "121.80";

    private final WeatherApiService weatherApi;
    private final GoldApiService    goldApi;
    private final ExecutorService   executor = Executors.newFixedThreadPool(3);
    private final Handler           mainHandler = new Handler(Looper.getMainLooper());

    public interface WidgetCallback {
        /** Luôn được gọi — kể cả khi 1 hoặc nhiều nguồn lỗi (partial data) */
        void onResult(WidgetData data);
    }

    public WidgetRepository() {
        weatherApi = RetrofitClient.getWeatherService();
        goldApi    = RetrofitClient.getGoldService();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void loadWidgetData(WidgetCallback callback) {
        WidgetData data = new WidgetData();

        // Card 1: ngay lập tức, không cần network
        buildDayCard(data);

        // 3 tác vụ async chạy song song
        // Dùng AtomicInteger đếm ngược — khi về 0 thì callback
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
                            WeatherResponse wr  = response.body();
                            WeatherResponse.Current cur = wr.getCurrent();
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

    // ── Card 3: Giá xăng (RSS VnExpress, background thread) ──────────────────

    private void fetchPetrol(WidgetData data, Runnable onDone) {
        executor.execute(() -> {
            try {
                PetrolRssParser.PetrolPrice price = PetrolRssParser.fetch();
                if (price != null && price.ron95 != null) {
                    data.setPetrolPrice(price.ron95);
                    data.setPetrolDate("RON 95  •  " + price.updateNote);
                } else if (price != null && price.e5ron92 != null) {
                    data.setPetrolPrice(price.e5ron92);
                    data.setPetrolDate("E5 RON 92  •  " + price.updateNote);
                } else {
                    // Fallback static
                    data.setPetrolPrice(FALLBACK_RON95);
                    data.setPetrolDate("RON 95  •  Giá tham khảo");
                }
            } catch (Exception e) {
                Log.w(TAG, "Petrol fetch failed: " + e.getMessage());
                data.setPetrolPrice(FALLBACK_RON95);
                data.setPetrolDate("RON 95  •  Giá tham khảo");
            }
            onDone.run();
        });
    }

    // ── Card 4: Giá vàng SJC (VNAppMob API) ──────────────────────────────────

    private void fetchGold(WidgetData data, Runnable onDone) {
        goldApi.getSjcGoldPrice(GOLD_BEARER)
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

                            // Chuyển từ đồng → triệu đồng, format 1 chữ số thập phân
                            data.setGoldBuy(formatMillions(r.getBuyHcm()));
                            data.setGoldSell(formatMillions(r.getSellHcm()));
                            data.setGoldUnit("triệu đ/lượng  •  SJC HCM");

                        } else {
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
    }

    private void setGoldFallback(WidgetData data) {
        data.setGoldBuy(FALLBACK_GOLD_BUY);
        data.setGoldSell(FALLBACK_GOLD_SELL);
        data.setGoldUnit("triệu đ/lượng  •  Giá tham khảo");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Chuyển giá (đồng) sang triệu đồng với 2 chữ số thập phân.
     * Ví dụ: 119500000.0 → "119.50"
     */
    private String formatMillions(double value) {
        double millions = value / 1_000_000.0;
        return new DecimalFormat("###.##").format(millions);
    }
}