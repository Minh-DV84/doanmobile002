package com.example.doanmobile002.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.data.repository.WidgetRepository;
import com.example.doanmobile002.models.NewsArticle;
import com.example.doanmobile002.models.WidgetData;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final NewsRepository   newsRepo;
    private final WidgetRepository widgetRepo;

    private final MutableLiveData<List<NewsArticle>> articlesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean>           isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String>            errorLiveData     = new MutableLiveData<>();
    private final MutableLiveData<WidgetData>        widgetLiveData    = new MutableLiveData<>();

    // Track whether we're in search mode (so swipe-to-refresh restores headlines)
    private String lastQuery = null;

    public HomeViewModel() {
        newsRepo   = new NewsRepository();
        widgetRepo = new WidgetRepository();
    }

    // ── Exposed LiveData ──────────────────────────────────────────────────────
    public LiveData<List<NewsArticle>> getArticles()  { return articlesLiveData; }
    public LiveData<Boolean>           getIsLoading() { return isLoadingLiveData; }
    public LiveData<String>            getError()     { return errorLiveData; }
    public LiveData<WidgetData>        getWidgetData(){ return widgetLiveData; }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Load home RSS headlines */
    public void loadHomeNews() {
        lastQuery = null;
        isLoadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        newsRepo.getHomeNews(new NewsRepository.NewsCallback() {
            @Override public void onSuccess(List<NewsArticle> articles) {
                isLoadingLiveData.postValue(false);
                articlesLiveData.postValue(articles);
            }
            @Override public void onError(String message) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    /** Search via NewsData.io */
    public void searchNews(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadHomeNews();
            return;
        }
        lastQuery = query.trim();
        isLoadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        newsRepo.searchNews(lastQuery, new NewsRepository.NewsCallback() {
            @Override public void onSuccess(List<NewsArticle> articles) {
                isLoadingLiveData.postValue(false);
                articlesLiveData.postValue(articles);
            }
            @Override public void onError(String message) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    /** Called by swipe-to-refresh */
    public void refresh() {
        if (lastQuery == null) loadHomeNews();
        else searchNews(lastQuery);
    }

    /** Load 4-widget data (weather + static prices + day) */
    public void loadWidgetData() {
        widgetRepo.loadWidgetData(data -> widgetLiveData.postValue(data));
    }
}