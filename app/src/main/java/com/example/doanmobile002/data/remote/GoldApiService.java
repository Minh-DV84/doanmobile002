package com.example.doanmobile002.data.remote;

import com.example.doanmobile002.models.VnAppMobGoldResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface GoldApiService {

    /**
     * GET https://api.vnappmob.com/api/v2/gold/sjc
     * Header: Authorization: Bearer <api_key>
     *
     * Đăng ký API key miễn phí tại:
     * https://vapi.vnappmob.com/api/request_api_key?scope=gold
     * Key tự gia hạn sau 15 ngày khi dùng lại.
     */
    @GET("api/v2/gold/sjc")
    Call<VnAppMobGoldResponse> getSjcGoldPrice(
            @Header("Authorization") String bearerToken
    );
}