package com.example.doanmobile002.models;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {

    @SerializedName("location")
    private Location location;

    @SerializedName("current")
    private Current current;

    public Location getLocation() { return location; }
    public Current getCurrent()   { return current; }

    public static class Location {
        @SerializedName("name")
        private String name;
        public String getName() { return name; }
    }

    public static class Current {
        @SerializedName("temp_c")
        private float tempC;

        @SerializedName("condition")
        private Condition condition;

        public float getTempC()         { return tempC; }
        public Condition getCondition() { return condition; }
    }

    public static class Condition {
        @SerializedName("text")
        private String text;

        @SerializedName("icon")
        private String icon;

        public String getText() { return text; }
        public String getIcon() { return icon; }
    }
}