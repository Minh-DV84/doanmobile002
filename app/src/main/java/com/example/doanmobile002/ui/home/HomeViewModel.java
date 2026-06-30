package com.example.doanmobile002.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.data.repository.WidgetRepository;
import com.example.doanmobile002.models.NewsArticle;
import com.example.doanmobile002.models.WidgetData;

import java.util.List;

/**
 * Dùng AndroidViewModel để có Context (cần cho Room + ConnectivityManager).
 * Single Source of Truth: UI observe articlesLiveData từ Room.
 */
public class HomeViewModel extends AndroidViewModel {

    private final NewsRepository   newsRepo;
    private final WidgetRepository widgetRepo;

    // ── LiveData expose cho UI ────────────────────────────────────────────────

    // Bài báo — lấy thẳng từ Room qua Transformations
    private LiveData<List<NewsArticle>> homeArticlesLive;

    // Kết quả search (không lưu DB, chỉ hiện tạm)
    private final MutableLiveData<List<NewsArticle>> searchResultsLive = new MutableLiveData<>();

    // Dùng MediatorLiveData để gộp home + search vào 1 stream cho Fragment
    private final MediatorLiveData<List<NewsArticle>> articlesLiveData = new MediatorLiveData<>();

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorLiveData     = new MutableLiveData<>();
    private final MutableLiveData<String>  toastLiveData     = new MutableLiveData<>(); // "offline" toast
    private final MutableLiveData<WidgetData> widgetLiveData = new MutableLiveData<>();

    private boolean isSearchMode = false;
    private String  lastQuery    = null;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        newsRepo   = new NewsRepository(application);
        widgetRepo = new WidgetRepository(application);

        // Lấy LiveData trang chủ từ Room
        homeArticlesLive = newsRepo.getHomeArticlesLive();

        // MediatorLiveData: khi không search → hiện homeArticlesLive
        articlesLiveData.addSource(homeArticlesLive, articles -> {
            if (!isSearchMode) {
                articlesLiveData.setValue(articles);
            }
        });

        articlesLiveData.addSource(searchResultsLive, results -> {
            if (isSearchMode) {
                articlesLiveData.setValue(results);
            }
        });
    }

    // ── Exposed LiveData ──────────────────────────────────────────────────────
    public LiveData<List<NewsArticle>> getArticles()  { return articlesLiveData; }
    public LiveData<Boolean>           getIsLoading() { return isLoadingLiveData; }
    public LiveData<String>            getError()     { return errorLiveData; }
    public LiveData<String>            getToast()     { return toastLiveData; }
    public LiveData<WidgetData>        getWidgetData(){ return widgetLiveData; }
    public LiveData<List<NewsArticle>> getSavedArticles() {
        return newsRepo.getSavedArticlesLive();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Sync tin tức mới từ RSS → Room (trang chủ) */
    public void loadHomeNews() {
        isSearchMode = false;
        lastQuery    = null;
        isLoadingLiveData.setValue(true);

        newsRepo.syncHomeNews(new NewsRepository.StatusCallback() {
            @Override public void onOnline() {
                // Đang fetch dữ liệu ẩn dưới nền, ta có thể giữ spinner chạy
            }
            @Override public void onOffline() {
                isLoadingLiveData.postValue(false);
                // Hiện toast offline — Room LiveData vẫn tự hiện cache
                toastLiveData.postValue("offline");
            }
            @Override public void onError(String msg) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(msg);
            }
            @Override public void onSuccessComplete() {
                // Tắt loading ngay lập tức khi việc parse và ghi DB hoàn tất
                isLoadingLiveData.postValue(false);
            }
        });
    }

    /** Tìm kiếm */
    public void searchNews(String query) {
        if (query == null || query.trim().isEmpty()) {
            exitSearch();
            return;
        }
        isSearchMode = true;
        lastQuery    = query.trim();
        isLoadingLiveData.setValue(true);

        newsRepo.searchNews(lastQuery, new NewsRepository.NewsCallback() {
            @Override public void onSuccess(List<NewsArticle> articles) {
                isLoadingLiveData.postValue(false);
                searchResultsLive.postValue(articles);
            }
            @Override public void onError(String message) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    /** Thoát search → về trang chủ */
    public void exitSearch() {
        isSearchMode = false;
        lastQuery    = null;
        // Lấy lại giá trị hiện tại từ Room
        if (homeArticlesLive.getValue() != null) {
            articlesLiveData.setValue(homeArticlesLive.getValue());
        }
    }

    /** Lưu / Bỏ lưu bài viết */
    public void toggleSave(NewsArticle article, boolean save) {
        newsRepo.toggleSave(article, save);
    }

    /** Swipe refresh */
    public void refresh() {
        if (!isSearchMode) loadHomeNews();
        else searchNews(lastQuery);
    }

    public void loadWidgetData() {
        widgetRepo.loadWidgetData(data -> widgetLiveData.postValue(data));
    }
}