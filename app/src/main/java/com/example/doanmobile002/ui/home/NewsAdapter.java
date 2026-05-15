package com.example.doanmobile002.ui.home;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.doanmobile002.R;
import com.example.doanmobile002.models.NewsArticle;
import com.example.doanmobile002.ui.detail.DetailActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_BANNER = 0;
    private static final int TYPE_NORMAL = 1;

    private List<NewsArticle> articles = new ArrayList<>();
    private final Context context;

    public NewsAdapter(Context context) { this.context = context; }

    public void setArticles(List<NewsArticle> newArticles) {
        this.articles = newArticles != null ? newArticles : new ArrayList<>();
        notifyDataSetChanged();
    }

    public int getItemCount()           { return articles.size(); }
    public int getItemViewType(int pos) { return pos == 0 ? TYPE_BANNER : TYPE_NORMAL; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_BANNER)
            return new BannerViewHolder(inf.inflate(R.layout.item_news_banner, parent, false));
        return new CardViewHolder(inf.inflate(R.layout.item_news_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NewsArticle a = articles.get(position);
        if (holder instanceof BannerViewHolder) ((BannerViewHolder) holder).bind(a);
        else                                    ((CardViewHolder)   holder).bind(a);
    }

    // ── Banner ────────────────────────────────────────────────────────────────
    class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;
        TextView  tvCategory, tvTitle, tvMeta;

        BannerViewHolder(View v) {
            super(v);
            imgBanner  = v.findViewById(R.id.imgBanner);
            tvCategory = v.findViewById(R.id.tvBannerCategory);
            tvTitle    = v.findViewById(R.id.tvBannerTitle);
            tvMeta     = v.findViewById(R.id.tvBannerMeta);
        }

        void bind(NewsArticle a) {
            tvTitle.setText(a.getTitle());
            tvCategory.setText(a.getSourceName() != null ? a.getSourceName() : "News");
            tvMeta.setText(formatDate(a.getPublishedAt()));

            Glide.with(context)
                    .load(a.getUrlToImage())
                    .placeholder(R.drawable.placeholder_news)
                    .error(R.drawable.placeholder_news)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imgBanner);

            // Click → mở DetailActivity
            itemView.setOnClickListener(v -> openDetail(a));
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView  tvTitle, tvSource, tvTime;

        CardViewHolder(View v) {
            super(v);
            imgThumb = v.findViewById(R.id.imgThumb);
            tvTitle  = v.findViewById(R.id.tvCardTitle);
            tvSource = v.findViewById(R.id.tvCardSource);
            tvTime   = v.findViewById(R.id.tvCardTime);
        }

        void bind(NewsArticle a) {
            tvTitle.setText(a.getTitle());
            tvSource.setText(a.getSourceName() != null ? a.getSourceName() : "Unknown");
            tvTime.setText(formatDate(a.getPublishedAt()));

            Glide.with(context)
                    .load(a.getUrlToImage())
                    .placeholder(R.drawable.placeholder_news)
                    .error(R.drawable.placeholder_news)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imgThumb);

            // Click → mở DetailActivity
            itemView.setOnClickListener(v -> openDetail(a));
        }
    }

    // ── Mở màn hình chi tiết ─────────────────────────────────────────────────
    private void openDetail(NewsArticle a) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_TITLE,  a.getTitle());
        intent.putExtra(DetailActivity.EXTRA_URL,    a.getUrl());
        intent.putExtra(DetailActivity.EXTRA_SOURCE, a.getSourceName());
        context.startActivity(intent);
    }

    // ── Format thời gian ─────────────────────────────────────────────────────
    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] isoPatterns = {
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : isoPatterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(raw);
                if (d != null) return relativeTime(d);
            } catch (ParseException ignored) {}
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            Date d = sdf.parse(raw);
            if (d != null) return relativeTime(d);
        } catch (ParseException ignored) {}
        return raw.substring(0, Math.min(10, raw.length()));
    }

    private String relativeTime(Date date) {
        return DateUtils.getRelativeTimeSpanString(
                date.getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        ).toString();
    }
}