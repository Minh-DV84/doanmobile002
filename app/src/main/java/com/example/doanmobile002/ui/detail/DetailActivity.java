package com.example.doanmobile002.ui.detail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.doanmobile002.R;
import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.models.NewsArticle;
import com.example.doanmobile002.utils.FontSizeManager;
import com.google.firebase.auth.FirebaseAuth;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE        = "extra_title";
    public static final String EXTRA_URL          = "extra_url";
    public static final String EXTRA_SOURCE       = "extra_source";
    public static final String EXTRA_IMAGE        = "extra_image";
    public static final String EXTRA_PUBLISHED_AT = "extra_published_at";
    public static final String EXTRA_DESCRIPTION  = "extra_description";

    private WebView        webView;
    private ProgressBar    progressBar;
    private View           offlineLayout;
    private String         articleUrl;
    private String         articleTitle;
    private String         articleSource;
    private String         articleImage;
    private String         articlePublishedAt;
    private String         articleDescription;

    private ImageView      btnSave;
    private NewsRepository newsRepository;
    private FirebaseAuth   firebaseAuth;
    private boolean        isSaved = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        articleTitle       = getIntent().getStringExtra(EXTRA_TITLE);
        articleSource      = getIntent().getStringExtra(EXTRA_SOURCE);
        articleUrl         = getIntent().getStringExtra(EXTRA_URL);
        articleImage       = getIntent().getStringExtra(EXTRA_IMAGE);
        articlePublishedAt = getIntent().getStringExtra(EXTRA_PUBLISHED_AT);
        articleDescription = getIntent().getStringExtra(EXTRA_DESCRIPTION);

        newsRepository = new NewsRepository(this);
        firebaseAuth   = FirebaseAuth.getInstance();

        setupToolbar(articleTitle, articleSource);
        setupWebView();
        setupSaveButton();
        setupBackPressedHandler();

        // ── Ghi vào lịch sử đọc ngay khi mở bài ──────────────────────────
        saveToHistory();

        // LUÔN LUÔN cho WebView load url (nếu có)
        // Nếu có mạng: Load mới. Nếu mất mạng: Load từ Cache.
        // Nếu không có cả mạng lẫn Cache -> hàm onReceivedError sẽ tự bắt lỗi và gọi showOfflineSummary()
        if (articleUrl != null && !articleUrl.isEmpty()) {
            webView.loadUrl(articleUrl);
        } else {
            Toast.makeText(this, "Không có link bài viết", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (btnSave != null && articleUrl != null) {
            if (firebaseAuth.getCurrentUser() != null) {
                newsRepository.checkSaved(articleUrl, saved -> {
                    isSaved = saved;
                    updateSaveIcon();
                });
            } else {
                isSaved = false;
                updateSaveIcon();
            }
        }
    }

    // ── Lịch sử đọc ──────────────────────────────────────────────────────────

    private void saveToHistory() {
        if (articleUrl == null || articleUrl.isEmpty()) return;
        NewsArticle article = new NewsArticle(
                articleTitle, articleDescription, articleImage,
                articleUrl, articlePublishedAt, articleSource, null
        );
        newsRepository.markArticleAsRead(article);
    }

    private void showOfflineSummary() {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);

        offlineLayout = findViewById(R.id.detailOfflineLayout);
        if (offlineLayout == null) {
            Toast.makeText(this,
                    "Không có mạng. Đang hiện tóm tắt đã lưu.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        offlineLayout.setVisibility(View.VISIBLE);

        ImageView imgOffline   = findViewById(R.id.detailOfflineImage);
        TextView  tvOfflineTitle   = findViewById(R.id.detailOfflineTitle);
        TextView  tvOfflineSource  = findViewById(R.id.detailOfflineSource);
        TextView  tvOfflineSummary = findViewById(R.id.detailOfflineSummary);

        if (imgOffline != null) {
            Glide.with(this)
                    .load(articleImage)
                    .placeholder(R.drawable.placeholder_news)
                    .error(R.drawable.placeholder_news)
                    .into(imgOffline);
        }
        if (tvOfflineTitle != null)  tvOfflineTitle.setText(articleTitle);
        if (tvOfflineSource != null) tvOfflineSource.setText(
                (articleSource != null ? articleSource : "") + "  •  Chế độ ngoại tuyến");
        if (tvOfflineSummary != null) {
            tvOfflineSummary.setText(
                    articleDescription != null && !articleDescription.isEmpty()
                            ? articleDescription
                            : "Không có tóm tắt cho bài viết này. Kết nối mạng để đọc đầy đủ.");
        }
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void setupToolbar(String title, String source) {
        Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(source != null ? source : "Bài viết");
        }
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        toolbar.setNavigationOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareArticle());
    }

    // ── Nút Lưu ──────────────────────────────────────────────────────────────

    private void setupSaveButton() {
        btnSave = findViewById(R.id.btnSave);
        if (btnSave == null || articleUrl == null) return;
        if (firebaseAuth.getCurrentUser() != null) {
            newsRepository.checkSaved(articleUrl, saved -> {
                isSaved = saved;
                updateSaveIcon();
            });
        }
        btnSave.setOnClickListener(v -> toggleSaveArticle());
    }

    private void toggleSaveArticle() {
        if (articleUrl == null || articleUrl.isEmpty()) return;
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu bài viết",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        boolean newState = !isSaved;
        NewsArticle article = new NewsArticle(
                articleTitle, articleDescription, articleImage,
                articleUrl, articlePublishedAt, articleSource, null);
        newsRepository.toggleSave(article, newState);
        isSaved = newState;
        updateSaveIcon();
        Toast.makeText(this,
                newState ? "Đã lưu bài viết" : "Đã bỏ lưu bài viết",
                Toast.LENGTH_SHORT).show();
    }

    private void updateSaveIcon() {
        if (btnSave == null) return;
        btnSave.setImageResource(
                isSaved ? R.drawable.ic_bookmark_filled
                        : R.drawable.ic_bookmark_outline);
    }

    // ── Back handler ──────────────────────────────────────────────────────────

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (webView != null && webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    // ── WebView ───────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView     = findViewById(R.id.detailWebView);
        progressBar = findViewById(R.id.detailProgressBar);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true); // Cần thiết cho Cache
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadsImagesAutomatically(true);
        s.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Pixel 5) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Mobile Safari/537.36");

        // BẬT CƠ CHẾ CACHE (OFFLINE ĐỌC BÁO)
        if (isNetworkAvailable()) {
            s.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            s.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }

        applyFontZoom();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                injectHideUI(view);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String clickedUrl = req.getUrl().toString();

                // CHẶN CHUYỂN HƯỚNG BÀI VIẾT KHÁC TRONG WEBVIEW
                if (clickedUrl.equals(articleUrl)) {
                    return false; // Cho phép load (là bài gốc)
                } else {
                    return true;  // Chặn load
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    showOfflineSummary();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100)
                    progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void applyFontZoom() {
        if (webView == null) return;
        int percent = FontSizeManager.getFontPercent(this);
        webView.getSettings().setTextZoom(percent);
    }

    private void injectHideUI(WebView view) {
        String js = "javascript:(function() {" +
                "var sel = [" +
                "'header','footer','nav','.header','.footer','.navbar'," +
                "'.navigation','.sidebar','.side-bar','.ads','.ad'," +
                "'.advertisement','.banner','.popup','.modal'," +
                "'.social-share','.share-button','.comment-section'," +
                "'.comments','.related-news','.box-related','.read-more'," +
                "'.tag-list','.tags','.breadcrumb','.cookie-banner'," +
                "'.header-new','.footer-new','.box-category-top'," +
                "'.sidebar-1','.box-toppick','.bottom-bar'," +
                "'.main-header','.main-footer','.box-menu'," +
                "'.box-tags','.box-comment','.box-newsletter'," +
                "'.zone-main-header','.zone-main-footer'," +
                "'.related-container','.sticky-bar'," +
                "'.dt-header','.dt-footer','.dt-sidebar'," +
                "'.social-plugin'," +
                "'.zn-header','.zn-footer','.zn-nav'" +
                "];" +
                "sel.forEach(function(s){" +
                "  document.querySelectorAll(s).forEach(function(e){" +
                "    e.style.display='none';});" +
                "});" +
                "document.body.style.marginTop='0';" +
                "document.body.style.paddingTop='0';" +
                "})()";
        view.evaluateJavascript(js, null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void shareArticle() {
        if (articleUrl == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, articleUrl);
        startActivity(Intent.createChooser(share, "Chia sẻ bài viết"));
    }

    // ── Kiểm tra mạng ────────────────────────────────────────────────────────
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}