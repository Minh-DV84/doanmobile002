package com.example.doanmobile002.ui.trending;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.models.NewsArticle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrendingViewModel extends AndroidViewModel {

    private final NewsRepository newsRepo;
    private final MutableLiveData<List<NewsArticle>> articlesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean>           isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String>            errorLiveData     = new MutableLiveData<>();

    private static final String[][] TRENDING_FEEDS = {
            {"https://vnexpress.net/rss/tin-moi-nhat.rss",       "VnExpress"},
            {"https://tuoitre.vn/rss/tin-moi-nhat.rss",          "Tuổi Trẻ"},
            {"https://dantri.com.vn/rss/home.rss",               "Dân Trí"},
            {"https://zingnews.vn/news.rss",                     "Zing News"},
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

                if (articles != null && !articles.isEmpty()) {
                    // BƯỚC 1: Đếm tần suất xuất hiện của từ khóa trong tiêu đề
                    Map<String, Integer> keywordCount = new HashMap<>();
                    for (NewsArticle article : articles) {
                        if (article.getTitle() == null) continue;

                        // Chuyển tiêu đề về chữ thường, tách thành các từ đơn
                        String[] words = article.getTitle().toLowerCase().split("\\s+");
                        for (String word : words) {
                            // Lọc bỏ các từ nối quá ngắn hoặc không có nghĩa (stopwords)
                            if (word.length() > 2) {
                                keywordCount.put(word, keywordCount.getOrDefault(word, 0) + 1);
                            }
                        }
                    }

                    // BƯỚC 2 & 3: Tính điểm xu hướng và sắp xếp danh sách bài viết
                    articles.sort((a1, a2) -> {
                        int score1 = calculateTrendingScore(a1, keywordCount);
                        int score2 = calculateTrendingScore(a2, keywordCount);

                        // Sắp xếp giảm dần (Bài điểm cao hơn lên đầu)
                        return Integer.compare(score2, score1);
                    });
                }

                articlesLiveData.postValue(articles);
            }

            @Override
            public void onError(String message) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }
        });
    }

    // Hàm phụ trợ tính điểm dựa trên tổng tần suất các từ khóa có trong tiêu đề
    private int calculateTrendingScore(NewsArticle article, Map<String, Integer> keywordCount) {
        if (article.getTitle() == null) return 0;

        int totalScore = 0;
        String[] words = article.getTitle().toLowerCase().split("\\s+");
        for (String word : words) {
            if (keywordCount.containsKey(word)) {
                totalScore += keywordCount.get(word);
            }
        }
        return totalScore;
    }
}