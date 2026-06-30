package com.example.doanmobile002.ui.utilities;

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

/**
 * Hiển thị danh sách bài đã đọc (Room: readAt > 0) trong tab "Lịch sử"
 * của UtilitiesFragment. Dùng chung layout item_saved_news.xml với
 * SavedNewsAdapter — chỉ khác hành vi nút phải (xóa khỏi lịch sử thay
 * vì bỏ lưu).
 */
public class HistoryNewsAdapter extends RecyclerView.Adapter<HistoryNewsAdapter.HistoryViewHolder> {

    public interface OnRemoveListener {
        void onRemove(NewsArticle article);
    }

    private List<NewsArticle>      articles = new ArrayList<>();
    private final Context          context;
    private final OnRemoveListener removeListener;

    public HistoryNewsAdapter(Context context, OnRemoveListener listener) {
        this.context        = context;
        this.removeListener = listener;
    }

    public void setArticles(List<NewsArticle> list) {
        this.articles = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_news, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(articles.get(position));
    }

    @Override
    public int getItemCount() { return articles.size(); }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb, btnRemove;
        TextView  tvTitle, tvSource, tvTime;

        HistoryViewHolder(View v) {
            super(v);
            imgThumb  = v.findViewById(R.id.imgSavedThumb);
            tvTitle   = v.findViewById(R.id.tvSavedTitle);
            tvSource  = v.findViewById(R.id.tvSavedSource);
            tvTime    = v.findViewById(R.id.tvSavedTime);
            btnRemove = v.findViewById(R.id.btnUnsave);   // tái dùng id sẵn có
        }

        void bind(NewsArticle a) {
            tvTitle.setText(a.getTitle());
            tvSource.setText(a.getSourceName() != null ? a.getSourceName() : "News");
            tvTime.setText(formatDate(a.getPublishedAt()));

            Glide.with(context)
                    .load(a.getUrlToImage())
                    .placeholder(R.drawable.placeholder_news)
                    .error(R.drawable.placeholder_news)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imgThumb);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_TITLE,        a.getTitle());
                intent.putExtra(DetailActivity.EXTRA_URL,          a.getUrl());
                intent.putExtra(DetailActivity.EXTRA_SOURCE,       a.getSourceName());
                intent.putExtra(DetailActivity.EXTRA_IMAGE,        a.getUrlToImage());
                intent.putExtra(DetailActivity.EXTRA_PUBLISHED_AT, a.getPublishedAt());
                intent.putExtra(DetailActivity.EXTRA_DESCRIPTION,  a.getDescription());
                context.startActivity(intent);
            });

            if (btnRemove != null) {
                btnRemove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                btnRemove.setOnClickListener(v -> {
                    if (removeListener != null) removeListener.onRemove(a);
                });
            }

            itemView.setOnLongClickListener(v -> {
                if (removeListener != null) removeListener.onRemove(a);
                return true;
            });
        }
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(raw);
                if (d != null) return DateUtils.getRelativeTimeSpanString(
                        d.getTime(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS).toString();
            } catch (ParseException ignored) {}
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            Date d = sdf.parse(raw);
            if (d != null) return DateUtils.getRelativeTimeSpanString(
                    d.getTime(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS).toString();
        } catch (ParseException ignored) {}
        return raw.substring(0, Math.min(10, raw.length()));
    }
}