package com.example.doanmobile002.ui.trending;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.models.NewsArticle;

import java.util.List;

public class TrendingViewModel extends AndroidViewModel {

    private final NewsRepository newsRepo;

    private final MutableLiveData<List<NewsArticle>> articlesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean>           isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String>            errorLiveData     = new MutableLiveData<>();

    // RSS feeds mảng trending (hot nhất từ các nguồn lớn)
    private static final String[][] TRENDING_FEEDS = {
            {"https://vnexpress.net/rss/tin-moi-nhat.rss",       "VnExpress"},
            {"https://tuoitre.vn/rss/tin-moi-nhat.rss",          "Tuổi Trẻ"},
            {"https://dantri.com.vn/rss/home.rss",               "Dân Trí"},
            {"https://zingnews.vn/news.rss",                     "Zing News"},
            {"https://vnexpress.net/rss/the-gioi.rss",           "VnExpress Thế Giới"},
            {"https://vnexpress.net/rss/kinh-doanh.rss",         "VnExpress Kinh Doanh"},
    };

    public TrendingViewModel(@NonNull Application application) {
        super(application);
        newsRepo = new NewsRepository(application);
    }

    public LiveData<List<NewsArticle>> getArticles()  { return articlesLiveData; }
    public LiveData<Boolean>           getIsLoading() { return isLoadingLiveData; }
    public LiveData<String>            getError()     { return errorLiveData; }

    public void loadTrending() {
        isLoadingLiveData.setValue(true);

        newsRepo.fetchTrendingNews(TRENDING_FEEDS, new NewsRepository.NewsCallback() {
            @Override
            public void onSuccess(List<NewsArticle> articles) {
                isLoadingLiveData.postValue(false);
                articlesLiveData.postValue(articles);
            }
            @Override
            public void onError(String message) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }
}