package com.example.doanmobile002.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Provides Retrofit singletons for each API base URL.
 */
public class RetrofitClient {

    // ── Existing NewsAPI (kept for reference, no longer used for home) ──────
    private static final String NEWSAPI_BASE    = "https://newsapi.org/v2/";
    // ── NewsData.io ──────────────────────────────────────────────────────────
    private static final String NEWSDATA_BASE   = "https://newsdata.io/api/1/";
    // ── WeatherAPI.com ───────────────────────────────────────────────────────
    private static final String WEATHER_BASE    = "https://api.weatherapi.com/v1/";
    // ── VNAppMob (Gold SJC) ──────────────────────────────────────────────────
    private static final String GOLD_BASE       = "https://api.vnappmob.com/";

    // ── Singletons ───────────────────────────────────────────────────────────
    private static Retrofit newsDataRetrofit;
    private static Retrofit weatherRetrofit;
    private static Retrofit goldRetrofit;

    private RetrofitClient() {}

    // ── NewsData.io ──────────────────────────────────────────────────────────
    public static synchronized NewsDataApiService getNewsDataService() {
        if (newsDataRetrofit == null) {
            newsDataRetrofit = new Retrofit.Builder()
                    .baseUrl(NEWSDATA_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return newsDataRetrofit.create(NewsDataApiService.class);
    }

    // ── WeatherAPI.com ───────────────────────────────────────────────────────
    public static synchronized WeatherApiService getWeatherService() {
        if (weatherRetrofit == null) {
            weatherRetrofit = new Retrofit.Builder()
                    .baseUrl(WEATHER_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return weatherRetrofit.create(WeatherApiService.class);
    }

    // ── VNAppMob Gold SJC ────────────────────────────────────────────────────
    public static synchronized GoldApiService getGoldService() {
        if (goldRetrofit == null) {
            goldRetrofit = new Retrofit.Builder()
                    .baseUrl(GOLD_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return goldRetrofit.create(GoldApiService.class);
    }
}