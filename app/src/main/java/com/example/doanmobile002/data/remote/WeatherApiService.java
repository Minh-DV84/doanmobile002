package com.example.doanmobile002.data.remote;

import com.example.doanmobile002.models.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    // WeatherAPI.com current weather
    // GET https://api.weatherapi.com/v1/current.json?key=KEY&q=Hanoi&lang=vi
    @GET("current.json")
    Call<WeatherResponse> getCurrentWeather(
            @Query("key")  String apiKey,
            @Query("q")    String city,
            @Query("lang") String lang
    );
}