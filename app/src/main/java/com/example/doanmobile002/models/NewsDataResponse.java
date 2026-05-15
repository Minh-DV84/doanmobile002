package com.example.doanmobile002.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NewsDataResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("totalResults")
    private int totalResults;

    @SerializedName("results")
    private List<Article> results;

    public String getStatus()          { return status; }
    public int getTotalResults()       { return totalResults; }
    public List<Article> getResults()  { return results; }

    public static class Article {
        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        @SerializedName("image_url")
        private String imageUrl;

        @SerializedName("link")
        private String link;

        @SerializedName("pubDate")
        private String pubDate;

        @SerializedName("source_name")
        private String sourceName;

        @SerializedName("creator")
        private List<String> creator;

        public String getTitle()      { return title; }
        public String getDescription(){ return description; }
        public String getImageUrl()   { return imageUrl; }
        public String getLink()       { return link; }
        public String getPubDate()    { return pubDate; }
        public String getSourceName() { return sourceName; }
        public String getAuthor() {
            if (creator != null && !creator.isEmpty()) return creator.get(0);
            return null;
        }
    }
}