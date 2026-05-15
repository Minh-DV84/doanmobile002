package com.example.doanmobile002.models;

public class NewsArticle {
    private String title;
    private String description;
    private String urlToImage;
    private String url;
    private String publishedAt;
    private String sourceName;
    private String author;

    public NewsArticle() {}

    public NewsArticle(String title, String description, String urlToImage,
                       String url, String publishedAt, String sourceName, String author) {
        this.title = title;
        this.description = description;
        this.urlToImage = urlToImage;
        this.url = url;
        this.publishedAt = publishedAt;
        this.sourceName = sourceName;
        this.author = author;
    }

    public String getTitle()                        { return title; }
    public void setTitle(String title)              { this.title = title; }
    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }
    public String getUrlToImage()                   { return urlToImage; }
    public void setUrlToImage(String u)             { this.urlToImage = u; }
    public String getUrl()                          { return url; }
    public void setUrl(String url)                  { this.url = url; }
    public String getPublishedAt()                  { return publishedAt; }
    public void setPublishedAt(String p)            { this.publishedAt = p; }
    public String getSourceName()                   { return sourceName; }
    public void setSourceName(String s)             { this.sourceName = s; }
    public String getAuthor()                       { return author; }
    public void setAuthor(String author)            { this.author = author; }
}