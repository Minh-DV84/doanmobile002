package com.example.doanmobile002.ui.trending;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.doanmobile002.R;
import com.example.doanmobile002.data.repository.NewsRepository;
import com.example.doanmobile002.models.NewsArticle;
import com.example.doanmobile002.ui.detail.DetailActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.TrendingViewHolder> {

    private List<NewsArticle> articles = new ArrayList<>();
    private final Context context;
    private final NewsRepository newsRepository;
    private final FirebaseAuth firebaseAuth;

    public TrendingAdapter(Context context) {
        this.context        = context;
        this.newsRepository = new NewsRepository(context);
        this.firebaseAuth   = FirebaseAuth.getInstance();
    }

    public void setArticles(List<NewsArticle> list) {
        this.articles = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trending, parent, false);
        return new TrendingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
        holder.bind(articles.get(position), position + 1);
    }

    @Override
    public int getItemCount() { return articles.size(); }

    class TrendingViewHolder extends RecyclerView.ViewHolder {
        TextView  tvRank, tvTitle, tvSource, tvTime, tvHotBadge;
        ImageView imgThumb, btnSave;

        TrendingViewHolder(View v) {
            super(v);
            tvRank     = v.findViewById(R.id.tvTrendingRank);
            tvTitle    = v.findViewById(R.id.tvTrendingTitle);
            tvSource   = v.findViewById(R.id.tvTrendingSource);
            tvTime     = v.findViewById(R.id.tvTrendingTime);
            tvHotBadge = v.findViewById(R.id.tvHotBadge);
            imgThumb   = v.findViewById(R.id.imgTrendingThumb);
            btnSave    = v.findViewById(R.id.btnTrendingSave);
        }

        void bind(NewsArticle a, int rank) {
            tvRank.setText(String.valueOf(rank));
            tvTitle.setText(a.getTitle());
            tvSource.setText(a.getSourceName() != null ? a.getSourceName() : "News");
            tvTime.setText(formatDate(a.getPublishedAt()));

            // Hiện badge HOT cho top 3
            if (tvHotBadge != null)
                tvHotBadge.setVisibility(rank <= 3 ? View.VISIBLE : View.GONE);

            // Màu rank đặc biệt cho top 3
            if (rank == 1) tvRank.setTextColor(0xFFE53935);
            else if (rank == 2) tvRank.setTextColor(0xFFFF6F00);
            else if (rank == 3) tvRank.setTextColor(0xFF1E88E5);
            else tvRank.setTextColor(0xFF9E9E9E);

            Glide.with(context)
                    .load(a.getUrlToImage())
                    .placeholder(R.drawable.placeholder_news)
                    .error(R.drawable.placeholder_news)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imgThumb);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_TITLE,  a.getTitle());
                intent.putExtra(DetailActivity.EXTRA_URL,    a.getUrl());
                intent.putExtra(DetailActivity.EXTRA_SOURCE, a.getSourceName());
                intent.putExtra(DetailActivity.EXTRA_IMAGE,  a.getUrlToImage());
                intent.putExtra(DetailActivity.EXTRA_PUBLISHED_AT, a.getPublishedAt());
                context.startActivity(intent);
            });

            // Nút lưu nhanh — kiểm tra trạng thái rồi cập nhật icon
            if (btnSave != null) {
                final String thisUrl = a.getUrl();
                btnSave.setImageResource(R.drawable.ic_bookmark_outline);
                btnSave.setTag(thisUrl);

                boolean isLoggedIn = firebaseAuth.getCurrentUser() != null;

                // Chưa đăng nhập thì chắc chắn không có bài đã lưu, bỏ qua check
                if (isLoggedIn && thisUrl != null) {
                    newsRepository.checkSaved(thisUrl, saved -> {
                        // Chỉ áp dụng nếu view chưa bị recycle sang item khác
                        if (thisUrl.equals(btnSave.getTag())) {
                            btnSave.setImageResource(
                                    saved ? R.drawable.ic_bookmark_filled
                                            : R.drawable.ic_bookmark_outline);
                        }
                    });
                }

                btnSave.setOnClickListener(v -> {
                    if (thisUrl == null) return;

                    // Chặn lưu bài nếu chưa đăng nhập
                    if (firebaseAuth.getCurrentUser() == null) {
                        Toast.makeText(context, "Vui lòng đăng nhập để lưu bài viết",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    newsRepository.checkSaved(thisUrl, saved -> {
                        boolean newState = !saved;
                        newsRepository.toggleSave(a, newState);
                        if (thisUrl.equals(btnSave.getTag())) {
                            btnSave.setImageResource(
                                    newState ? R.drawable.ic_bookmark_filled
                                            : R.drawable.ic_bookmark_outline);
                        }
                        Toast.makeText(context,
                                newState ? "Đã lưu bài viết" : "Đã bỏ lưu bài viết",
                                Toast.LENGTH_SHORT).show();
                    });
                });
            }
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