package com.example.doanmobile002.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response từ https://api.vnappmob.com/api/v2/gold/sjc
 * {
 *   "results": [
 *     {
 *       "buy_hcm": 119500000,
 *       "sell_hcm": 121800000,
 *       "buy_hn":  119500000,
 *       "sell_hn": 121800000,
 *       "updated": "2025-05-15T09:00:00"
 *     }
 *   ]
 * }
 */
public class VnAppMobGoldResponse {

    @SerializedName("results")
    private List<GoldResult> results;

    public List<GoldResult> getResults() { return results; }

    public static class GoldResult {

        @SerializedName("buy_hcm")
        private double buyHcm;

        @SerializedName("sell_hcm")
        private double sellHcm;

        @SerializedName("buy_hn")
        private double buyHn;

        @SerializedName("sell_hn")
        private double sellHn;

        @SerializedName("updated")
        private String updated;

        public double getBuyHcm()   { return buyHcm; }
        public double getSellHcm()  { return sellHcm; }
        public double getBuyHn()    { return buyHn; }
        public double getSellHn()   { return sellHn; }
        public String getUpdated()  { return updated; }
    }
}