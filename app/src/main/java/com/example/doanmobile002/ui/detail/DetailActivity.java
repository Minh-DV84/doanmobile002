package com.example.doanmobile002.ui.detail;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.doanmobile002.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE      = "extra_title";
    public static final String EXTRA_URL        = "extra_url";
    public static final String EXTRA_URL_IMAGE  = "extra_url_image";
    public static final String EXTRA_DESCRIPTION= "extra_description";
    public static final String EXTRA_SOURCE     = "extra_source";
    public static final String EXTRA_PUBLISHED  = "extra_published";

    private String articleUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent   = getIntent();
        String title    = intent.getStringExtra(EXTRA_TITLE);
        String urlImage = intent.getStringExtra(EXTRA_URL_IMAGE);
        String desc     = intent.getStringExtra(EXTRA_DESCRIPTION);
        String source   = intent.getStringExtra(EXTRA_SOURCE);
        String pubAt    = intent.getStringExtra(EXTRA_PUBLISHED);
        articleUrl      = intent.getStringExtra(EXTRA_URL);

        setupToolbar(title, source);
        bindHeader(title, urlImage, source, pubAt, desc);
        fetchFullContent(); // Tự động fetch nội dung đầy đủ
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────
    private void setupToolbar(String title, String source) {
        Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(source != null ? source : "");
        }
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(Color.WHITE);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareArticle());
    }

    // ── Hiển thị header ngay (ảnh + tiêu đề + tóm tắt) ─────────────────────
    private void bindHeader(String title, String urlImage,
                            String source, String pubAt, String desc) {
        ImageView imgHero  = findViewById(R.id.detailImgHero);
        TextView  tvTitle  = findViewById(R.id.detailTvTitle);
        TextView  tvSource = findViewById(R.id.detailTvSource);
        TextView  tvTime   = findViewById(R.id.detailTvTime);
        TextView  tvDesc   = findViewById(R.id.detailTvDescription);

        // Ảnh
        Glide.with(this)
                .load(urlImage)
                .placeholder(R.drawable.placeholder_news)
                .error(R.drawable.placeholder_news)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(imgHero);

        tvTitle.setText(title != null ? title : "");
        tvSource.setText(source != null ? source : "");
        tvTime.setText(formatDate(pubAt));

        // Hiện mô tả trước, sau khi fetch xong sẽ thay bằng full content
        if (desc != null && !desc.isEmpty()) {
            tvDesc.setText(desc);
        }
    }

    // ── Fetch nội dung đầy đủ bằng Jsoup (chạy background thread) ───────────
    private void fetchFullContent() {
        ProgressBar progressBar = findViewById(R.id.detailProgressBar);
        TextView    tvContent   = findViewById(R.id.detailTvDescription);
        TextView    tvLoading   = findViewById(R.id.detailTvLoading);

        progressBar.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                // Fetch HTML trang báo
                Document doc = Jsoup.connect(articleUrl)
                        .userAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();

                // Trích xuất nội dung chính — thử các selector phổ biến của báo VN
                String content = extractContent(doc);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvLoading.setVisibility(View.GONE);
                    if (content != null && !content.isEmpty()) {
                        tvContent.setText(content);
                    } else {
                        tvContent.setText("Không thể tải nội dung đầy đủ.\nVui lòng nhấn nút bên dưới để đọc trên trình duyệt.");
                        findViewById(R.id.detailBtnOpenBrowser).setVisibility(View.VISIBLE);
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvLoading.setVisibility(View.GONE);
                    tvContent.setText("Không thể tải nội dung. Kiểm tra kết nối mạng.");
                    findViewById(R.id.detailBtnOpenBrowser).setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // ── Trích xuất nội dung chính từ HTML ────────────────────────────────────
    private String extractContent(Document doc) {
        StringBuilder sb = new StringBuilder();

        // Danh sách selector theo thứ tự ưu tiên — phủ các báo VN lớn
        String[] selectors = {
                // VnExpress
                "article.fck_detail",
                // Tuổi Trẻ
                "div.detail-content",
                // Thanh Niên
                "div#abody",
                // Dân Trí
                "div.singular-content",
                // Zing News
                "div.the-article-body",
                // CafeF
                "div#mainContent",
                // Báo chung
                "article",
                "div.article-content",
                "div.post-content",
                "div.entry-content",
                "div.content-detail",
                "div[itemprop=articleBody]",
                "div.article-body",
                "div.body-content"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                // Xóa các phần tử thừa: quảng cáo, script, liên quan
                el.select("script, style, .ads, .advertisement, "
                        + ".related, .box-related, .tag, figure.photo, "
                        + ".author-info, .social-share").remove();

                // Lấy từng đoạn văn
                Elements paragraphs = el.select("p");
                if (!paragraphs.isEmpty()) {
                    for (Element p : paragraphs) {
                        String text = p.text().trim();
                        if (text.length() > 30) { // Bỏ đoạn quá ngắn (caption, label)
                            sb.append(text).append("\n\n");
                        }
                    }
                }

                if (sb.length() > 100) break; // Lấy được rồi thì dừng
            }
        }

        return sb.toString().trim();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private void shareArticle() {
        if (articleUrl == null || articleUrl.isEmpty()) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, articleUrl);
        startActivity(Intent.createChooser(share, "Chia sẻ bài viết"));
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd HH:mm:ss",
                "EEE, dd MMM yyyy HH:mm:ss Z"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p,
                        p.startsWith("EEE") ? Locale.ENGLISH : Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(raw);
                if (d != null) {
                    return new SimpleDateFormat("HH:mm - dd/MM/yyyy",
                            new Locale("vi")).format(d);
                }
            } catch (ParseException ignored) {}
        }
        return raw.substring(0, Math.min(10, raw.length()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}