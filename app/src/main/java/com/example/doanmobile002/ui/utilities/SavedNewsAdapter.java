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

public class SavedNewsAdapter extends RecyclerView.Adapter<SavedNewsAdapter.SavedViewHolder> {

    public interface OnUnsaveListener {
        void onUnsave(NewsArticle article);
    }

    private List<NewsArticle>  articles = new ArrayList<>();
    private final Context          context;
    private final OnUnsaveListener unsaveListener;

    public SavedNewsAdapter(Context context, OnUnsaveListener listener) {
        this.context        = context;
        this.unsaveListener = listener;
    }

    public void setArticles(List<NewsArticle> list) {
        this.articles = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_news, parent, false);
        return new SavedViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedViewHolder holder, int position) {
        holder.bind(articles.get(position));
    }

    @Override
    public int getItemCount() { return articles.size(); }

    class SavedViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb, btnUnsave;
        TextView  tvTitle, tvSource, tvTime;

        SavedViewHolder(View v) {
            super(v);
            imgThumb  = v.findViewById(R.id.imgSavedThumb);
            tvTitle   = v.findViewById(R.id.tvSavedTitle);
            tvSource  = v.findViewById(R.id.tvSavedSource);
            tvTime    = v.findViewById(R.id.tvSavedTime);
            btnUnsave = v.findViewById(R.id.btnUnsave);
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

            // Click → mở chi tiết
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_TITLE,  a.getTitle());
                intent.putExtra(DetailActivity.EXTRA_URL,    a.getUrl());
                intent.putExtra(DetailActivity.EXTRA_SOURCE, a.getSourceName());
                context.startActivity(intent);
            });

            // Nút bỏ lưu (icon trái tim gạch)
            btnUnsave.setOnClickListener(v -> {
                if (unsaveListener != null) unsaveListener.onUnsave(a);
            });

            // Long press cũng gọi unsave
            itemView.setOnLongClickListener(v -> {
                if (unsaveListener != null) unsaveListener.onUnsave(a);
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