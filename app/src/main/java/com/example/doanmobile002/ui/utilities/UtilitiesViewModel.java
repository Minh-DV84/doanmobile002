package com.example.doanmobile002.ui.utilities;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.models.NewsArticle;

import java.util.List;

public class UtilitiesViewModel extends AndroidViewModel {

    private final NewsRepository newsRepo;

    public UtilitiesViewModel(@NonNull Application application) {
        super(application);
        newsRepo = new NewsRepository(application);
    }

    /** LiveData danh sách bài đã lưu — Room tự notify khi thay đổi */
    public LiveData<List<NewsArticle>> getSavedArticles() {
        return newsRepo.getSavedArticlesLive();
    }

    /** Bỏ lưu bài viết */
    public void unsaveArticle(NewsArticle article) {
        newsRepo.toggleSave(article, false);
    }
}