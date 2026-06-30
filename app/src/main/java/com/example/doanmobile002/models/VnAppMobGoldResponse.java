package com.example.doanmobile002.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response từ https://api.vnappmob.com/api/v2/gold/sjc
 *
 * Theo tài liệu chính thức (https://vapi.vnappmob.com/gold.v2.html),
 * endpoint SJC trả về field "buy_1l" / "sell_1l" (giá vàng SJC 1 lượng),
 * KHÔNG PHẢI "buy_hcm"/"sell_hcm" (field đó chỉ dùng cho DOJI/PNJ).
 *
 * {
 *   "results": [
 *     {
 *       "buy_1l":  119500000.00,
 *       "sell_1l": 121800000.00
 *     }
 *   ]
 * }
 */
public class VnAppMobGoldResponse {

    @SerializedName("results")
    private List<GoldResult> results;

    public List<GoldResult> getResults() { return results; }

    public static class GoldResult {

        @SerializedName("buy_1l")
        private double buy1l;

        @SerializedName("sell_1l")
        private double sell1l;

        public double getBuy1l()  { return buy1l; }
        public double getSell1l() { return sell1l; }
    }
}