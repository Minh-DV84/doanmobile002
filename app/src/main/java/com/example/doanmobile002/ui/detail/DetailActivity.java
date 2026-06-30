package com.example.doanmobile002.ui.detail;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

    private WebView        webView;
    private ProgressBar    progressBar;
    private String         articleUrl;
    private String         articleTitle;
    private String         articleSource;
    private String         articleImage;
    private String         articlePublishedAt;

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

        newsRepository = new NewsRepository(this);
        firebaseAuth   = FirebaseAuth.getInstance();

        setupToolbar(articleTitle, articleSource);
        setupWebView();
        setupSaveButton();
        setupBackPressedHandler();

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
        // Áp lại cỡ chữ nếu user vừa kéo slider ở Tiện ích
        applyFontZoom();

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
                articleTitle, null, articleImage,
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
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadsImagesAutomatically(true);
        s.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Pixel 5) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Mobile Safari/537.36");

        // Áp % cỡ chữ ngay khi khởi tạo WebView
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
                // setTextZoom đã áp từ trước — không cần inject lại
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view,
                                                    WebResourceRequest req) {
                view.loadUrl(req.getUrl().toString());
                return true;
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

    /**
     * Áp cỡ chữ % vào WebView bằng WebSettings.setTextZoom().
     * Đây là API chính thức của Android — không cần JS, không bị chặn.
     * 100 = mặc định trang gốc, 150 = to hơn 50%, 80 = nhỏ hơn 20%.
     */
    private void applyFontZoom() {
        if (webView == null) return;
        int percent = FontSizeManager.getFontPercent(this);
        webView.getSettings().setTextZoom(percent);
    }

    // ── JS: ẩn header/footer/quảng cáo ──────────────────────────────────────

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

    // ── Share ─────────────────────────────────────────────────────────────────

    private void shareArticle() {
        if (articleUrl == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, articleUrl);
        startActivity(Intent.createChooser(share, "Chia sẻ bài viết"));
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