package com.example.doanmobile002.ui.trending;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doanmobile002.R;

public class TrendingFragment extends Fragment {

    private TrendingViewModel viewModel;
    private TrendingAdapter   adapter;

    private RecyclerView       recyclerView;
    private ProgressBar        progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView           tvError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trending, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TrendingViewModel.class);

        bindViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();

        viewModel.loadTrending();
    }

    private void bindViews(View v) {
        recyclerView = v.findViewById(R.id.trendingRecyclerView);
        progressBar  = v.findViewById(R.id.trendingProgressBar);
        swipeRefresh = v.findViewById(R.id.trendingSwipeRefresh);
        tvError      = v.findViewById(R.id.trendingTvError);
    }

    private void setupRecyclerView() {
        adapter = new TrendingAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(0xFF1877F2);
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadTrending());
    }

    private void observeViewModel() {
        viewModel.getArticles().observe(getViewLifecycleOwner(), articles -> {
            swipeRefresh.setRefreshing(false);
            if (articles != null && !articles.isEmpty()) {
                adapter.setArticles(articles);
                tvError.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                tvError.setText("Chưa có tin xu hướng.\nKéo xuống để tải lại.");
                tvError.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (!swipeRefresh.isRefreshing())
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (!loading) swipeRefresh.setRefreshing(false);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }
}