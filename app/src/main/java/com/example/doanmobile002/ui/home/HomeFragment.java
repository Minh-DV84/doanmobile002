package com.example.doanmobile002.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
import com.example.doanmobile002.R;
import com.example.doanmobile002.models.WidgetData;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private NewsAdapter   adapter;

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView      recyclerView;
    private ProgressBar       progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView          tvError;
    private EditText          etSearch;

    // Widget cards
    private TextView  tvDayOfWeek, tvDate;
    private TextView  tvWeatherCity, tvWeatherTemp, tvWeatherDesc;
    private ImageView imgWeatherIcon;
    private TextView  tvPetrolPrice, tvPetrolType;
    private TextView  tvGoldBuy, tvGoldSell, tvGoldUnit;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        bindViews(view);
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        observeViewModel();

        viewModel.loadHomeNews();
        viewModel.loadWidgetData();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void bindViews(View v) {
        recyclerView = v.findViewById(R.id.homeRecyclerView);
        progressBar  = v.findViewById(R.id.homeProgressBar);
        swipeRefresh = v.findViewById(R.id.homeSwipeRefresh);
        tvError      = v.findViewById(R.id.homeTvError);
        etSearch     = v.findViewById(R.id.homeEtSearch);

        // Widget: Day
        tvDayOfWeek = v.findViewById(R.id.tvDayOfWeek);
        tvDate      = v.findViewById(R.id.tvDate);

        // Widget: Weather
        tvWeatherCity = v.findViewById(R.id.tvWeatherCity);
        tvWeatherTemp = v.findViewById(R.id.tvWeatherTemp);
        tvWeatherDesc = v.findViewById(R.id.tvWeatherDesc);
        imgWeatherIcon = v.findViewById(R.id.imgWeatherIcon);

        // Widget: Petrol
        tvPetrolPrice = v.findViewById(R.id.tvPetrolPrice);
        tvPetrolType  = v.findViewById(R.id.tvPetrolType);

        // Widget: Gold
        tvGoldBuy  = v.findViewById(R.id.tvGoldBuy);
        tvGoldSell = v.findViewById(R.id.tvGoldSell);
        tvGoldUnit = v.findViewById(R.id.tvGoldUnit);
    }

    private void setupRecyclerView() {
        adapter = new NewsAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    viewModel.loadHomeNews();
                } else if (query.length() >= 3) {
                    viewModel.searchNews(query);
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(0xFF1877F2);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private void observeViewModel() {

        viewModel.getArticles().observe(getViewLifecycleOwner(), articles -> {
            if (articles != null && !articles.isEmpty()) {
                adapter.setArticles(articles);
                tvError.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                tvError.setText("Không có bài viết nào.");
                tvError.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (swipeRefresh.isRefreshing()) {
                if (!isLoading) swipeRefresh.setRefreshing(false);
            } else {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) {
                    tvError.setText(error);
                    tvError.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getWidgetData().observe(getViewLifecycleOwner(), this::bindWidgetData);
    }

    // ── Widget binding ────────────────────────────────────────────────────────

    private void bindWidgetData(WidgetData data) {
        if (data == null) return;

        // Day card
        if (tvDayOfWeek != null) tvDayOfWeek.setText(data.getDayOfWeek());
        if (tvDate != null)      tvDate.setText(data.getDateStr());

        // Weather card
        if (tvWeatherCity != null) tvWeatherCity.setText(data.getWeatherCity());
        if (tvWeatherTemp != null) tvWeatherTemp.setText(data.getWeatherTemp());
        if (tvWeatherDesc != null) tvWeatherDesc.setText(data.getWeatherDesc());
        if (imgWeatherIcon != null && data.getWeatherIcon() != null) {
            Glide.with(this)
                    .load(data.getWeatherIcon())
                    .into(imgWeatherIcon);
        }

        // Petrol card
        if (tvPetrolPrice != null) tvPetrolPrice.setText(data.getPetrolPrice());
        if (tvPetrolType  != null) tvPetrolType.setText(data.getPetrolDate());

        // Gold card
        if (tvGoldBuy  != null) tvGoldBuy.setText(data.getGoldBuy());
        if (tvGoldSell != null) tvGoldSell.setText(data.getGoldSell());
        if (tvGoldUnit != null) tvGoldUnit.setText(data.getGoldUnit());
    }
}