package com.example.doanmobile002.ui.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.models.NewsArticle;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final NewsRepository newsRepo;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        newsRepo = new NewsRepository(application);
    }

    /** LiveData danh sách bài đã đọc — Room tự notify khi có bài mới */
    public LiveData<List<NewsArticle>> getHistoryArticles() {
        return newsRepo.getHistoryArticlesLive();
    }

    /** Xóa 1 mục khỏi lịch sử (không ảnh hưởng nếu đã lưu) */
    public void removeFromHistory(NewsArticle article) {
        if (article != null) newsRepo.removeFromHistory(article.getUrl());
    }

    /** Xóa toàn bộ lịch sử (giữ nguyên bài đã lưu) */
    public void clearAllHistory() {
        newsRepo.clearHistory();
    }
}