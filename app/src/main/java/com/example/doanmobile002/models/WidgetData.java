package com.example.doanmobile002.models;

/**
 * Holds data for the 4 quick-info widget cards shown below the search bar:
 *  1. Day/Date
 *  2. Weather
 *  3. Petrol price
 *  4. Gold price
 */
public class WidgetData {
    // --- Day card ---
    private String dayOfWeek;   // e.g. "Thứ Năm"
    private String dateStr;     // e.g. "15/05/2025"

    // --- Weather card ---
    private String weatherCity;
    private String weatherTemp;  // e.g. "32°C"
    private String weatherDesc;  // e.g. "Có mây"
    private String weatherIcon;  // icon URL from WeatherAPI

    // --- Petrol card ---
    private String petrolPrice;  // e.g. "21,310 đ/L"
    private String petrolDate;   // e.g. "Cập nhật 01/05"

    // --- Gold card ---
    private String goldBuy;      // e.g. "119.50"
    private String goldSell;     // e.g. "121.80"
    private String goldUnit;     // "triệu đ/lượng"

    public WidgetData() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getDayOfWeek()             { return dayOfWeek; }
    public void setDayOfWeek(String d)       { this.dayOfWeek = d; }

    public String getDateStr()               { return dateStr; }
    public void setDateStr(String d)         { this.dateStr = d; }

    public String getWeatherCity()           { return weatherCity; }
    public void setWeatherCity(String c)     { this.weatherCity = c; }

    public String getWeatherTemp()           { return weatherTemp; }
    public void setWeatherTemp(String t)     { this.weatherTemp = t; }

    public String getWeatherDesc()           { return weatherDesc; }
    public void setWeatherDesc(String d)     { this.weatherDesc = d; }

    public String getWeatherIcon()           { return weatherIcon; }
    public void setWeatherIcon(String i)     { this.weatherIcon = i; }

    public String getPetrolPrice()           { return petrolPrice; }
    public void setPetrolPrice(String p)     { this.petrolPrice = p; }

    public String getPetrolDate()            { return petrolDate; }
    public void setPetrolDate(String d)      { this.petrolDate = d; }

    public String getGoldBuy()               { return goldBuy; }
    public void setGoldBuy(String g)         { this.goldBuy = g; }

    public String getGoldSell()              { return goldSell; }
    public void setGoldSell(String g)        { this.goldSell = g; }

    public String getGoldUnit()              { return goldUnit; }
    public void setGoldUnit(String u)        { this.goldUnit = u; }
}