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
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.doanmobile002.R;
import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.models.NewsArticle;
import com.google.firebase.auth.FirebaseAuth;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE        = "extra_title";
    public static final String EXTRA_URL           = "extra_url";
    public static final String EXTRA_SOURCE        = "extra_source";
    public static final String EXTRA_IMAGE         = "extra_image";
    public static final String EXTRA_PUBLISHED_AT  = "extra_published_at";

    private WebView     webView;
    private ProgressBar progressBar;
    private String      articleUrl;
    private String      articleTitle;
    private String      articleSource;
    private String      articleImage;
    private String      articlePublishedAt;

    private ImageView      btnSave;
    private NewsRepository newsRepository;
    private FirebaseAuth   firebaseAuth;
    private boolean        isSaved = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        String title  = getIntent().getStringExtra(EXTRA_TITLE);
        String source = getIntent().getStringExtra(EXTRA_SOURCE);
        articleUrl    = getIntent().getStringExtra(EXTRA_URL);
        articleTitle  = title;
        articleSource = source;
        articleImage       = getIntent().getStringExtra(EXTRA_IMAGE);
        articlePublishedAt = getIntent().getStringExtra(EXTRA_PUBLISHED_AT);

        newsRepository = new NewsRepository(this);
        firebaseAuth   = FirebaseAuth.getInstance();

        setupToolbar(title, source);
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
        // Đăng nhập/đăng xuất có thể xảy ra ở màn khác trong lúc Activity này
        // đang ở background — cập nhật lại icon cho đúng trạng thái hiện tại.
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

    private void setupToolbar(String title, String source) {
        Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(source != null ? source : "Bài viết");
        }
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(Color.WHITE);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareArticle());
    }

    /**
     * Khởi tạo nút Lưu: kiểm tra trạng thái đã lưu chưa, cập nhật icon,
     * và gắn listener để toggle lưu/bỏ lưu.
     */
    private void setupSaveButton() {
        btnSave = findViewById(R.id.btnSave);
        if (btnSave == null || articleUrl == null) return;

        // Chỉ kiểm tra trạng thái đã lưu nếu đã đăng nhập;
        // chưa đăng nhập thì không thể có bài đã lưu, giữ icon outline.
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

        // Chặn lưu bài nếu chưa đăng nhập
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu bài viết", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newState = !isSaved;

        NewsArticle article = new NewsArticle(
                articleTitle, null, articleImage,
                articleUrl, articlePublishedAt, articleSource, null
        );
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
                isSaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline
        );
    }

    /**
     * Thay cho onBackPressed() (đã deprecated, không còn được gọi với
     * back gesture trên Android 13+). Ưu tiên goBack() trong WebView,
     * nếu không còn lịch sử thì để hệ thống xử lý back bình thường.
     */
    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
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
        // Cho phép load hình ảnh
        s.setLoadsImagesAutomatically(true);
        // User-agent giả mobile để trang báo hiện bản mobile (gọn hơn)
        s.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Pixel 5) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                // Inject CSS để ẩn header/footer/nav/quảng cáo của trang báo
                injectHideUI(view);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                // Giữ mọi link trong app, không mở Chrome ngoài
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
     * Inject JavaScript để ẩn các thành phần không phải nội dung bài báo.
     * Phủ các báo VN lớn: VnExpress, Tuổi Trẻ, Thanh Niên, Dân Trí, Zing.
     */
    private void injectHideUI(WebView view) {
        String js = "javascript:(function() {" +
                // Danh sách selector cần ẨN (header, footer, nav, sidebar, ads)
                "var selectorsToHide = [" +
                // Chung
                "'header','footer','nav','.header','.footer','.navbar'," +
                "'.navigation','.sidebar','.side-bar','.ads','.ad'," +
                "'.advertisement','.banner','.popup','.modal'," +
                "'.social-share','.share-button','.comment-section'," +
                "'.comments','.related-news','.box-related','.read-more'," +
                "'.tag-list','.tags','.breadcrumb','.cookie-banner'," +
                // VnExpress
                "'.header-new','.footer-new','.box-category-top'," +
                "'.sidebar-1','.box-toppick','.bottom-bar'," +
                // Tuổi Trẻ
                "'.main-header','.main-footer','.box-menu'," +
                "'.box-tags','.box-comment','.box-newsletter'," +
                // Thanh Niên
                "'.zone-main-header','.zone-main-footer'," +
                "'.related-container','.sticky-bar'," +
                // Dân Trí
                "'.dt-header','.dt-footer','.dt-sidebar'," +
                "'.social-plugin','.tdi_122'," +
                // Zing
                "'.zn-header','.zn-footer','.zn-nav'" +
                "];" +
                // Ẩn tất cả
                "selectorsToHide.forEach(function(sel){" +
                "  var els = document.querySelectorAll(sel);" +
                "  els.forEach(function(el){ el.style.display='none'; });" +
                "});" +
                // Xóa margin/padding thừa của body
                "document.body.style.marginTop='0';" +
                "document.body.style.paddingTop='0';" +
                "})()";

        view.evaluateJavascript(js, null);
    }

    private void shareArticle() {
        if (articleUrl == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, articleUrl);
        startActivity(Intent.createChooser(share, "Chia sẻ bài viết"));
    }
}