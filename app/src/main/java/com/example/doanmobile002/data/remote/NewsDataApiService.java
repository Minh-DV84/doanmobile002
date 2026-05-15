package com.example.doanmobile002.data.remote;

import com.example.doanmobile002.models.NewsDataResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsDataApiService {

    // GET https://newsdata.io/api/1/news?apikey=KEY&q=QUERY&language=vi
    @GET("news")
    Call<NewsDataResponse> searchNews(
            @Query("apikey")   String apiKey,
            @Query("q")        String query,
            @Query("language") String language
    );

    // Latest Vietnamese news (no query, top headlines)
    @GET("news")
    Call<NewsDataResponse> getLatestNews(
            @Query("apikey")   String apiKey,
            @Query("country")  String country,
            @Query("language") String language
    );
}