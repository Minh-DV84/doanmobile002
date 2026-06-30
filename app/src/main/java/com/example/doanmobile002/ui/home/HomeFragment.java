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

import java.util.LinkedHashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private HomeViewModel  viewModel;
    private NewsAdapter    adapter;

    private RecyclerView       recyclerView;
    private ProgressBar        progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView           tvError;
    private EditText           etSearch;

    // Banner offline
    private View     offlineBanner;
    private TextView tvOfflineMsg;

    // Widget cards
    private TextView  tvDayOfWeek, tvDate;
    private TextView  tvWeatherCity, tvWeatherTemp, tvWeatherDesc;
    private ImageView imgWeatherIcon;
    private TextView  tvPetrolPrice, tvPetrolType;
    private TextView  tvGoldBuy, tvGoldSell, tvGoldUnit;

    // Hashtag chips
    private TextView chipThoiSu, chipTheThao, chipKinhTe,
            chipSucKhoe, chipKhoaHoc, chipTheGioi, chipGiaiTri;
    private TextView activeChip = null;

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
        setupHashtags();
        setupSwipeRefresh();
        observeViewModel();

        viewModel.loadHomeNews();
        viewModel.loadWidgetData();
    }

    private void bindViews(View v) {
        recyclerView  = v.findViewById(R.id.homeRecyclerView);
        progressBar   = v.findViewById(R.id.homeProgressBar);
        swipeRefresh  = v.findViewById(R.id.homeSwipeRefresh);
        tvError       = v.findViewById(R.id.homeTvError);
        etSearch      = v.findViewById(R.id.homeEtSearch);
        offlineBanner = v.findViewById(R.id.offlineBanner);
        tvOfflineMsg  = v.findViewById(R.id.tvOfflineMsg);

        tvDayOfWeek    = v.findViewById(R.id.tvDayOfWeek);
        tvDate         = v.findViewById(R.id.tvDate);
        tvWeatherCity  = v.findViewById(R.id.tvWeatherCity);
        tvWeatherTemp  = v.findViewById(R.id.tvWeatherTemp);
        tvWeatherDesc  = v.findViewById(R.id.tvWeatherDesc);
        imgWeatherIcon = v.findViewById(R.id.imgWeatherIcon);
        tvPetrolPrice  = v.findViewById(R.id.tvPetrolPrice);
        tvPetrolType   = v.findViewById(R.id.tvPetrolType);
        tvGoldBuy      = v.findViewById(R.id.tvGoldBuy);
        tvGoldSell     = v.findViewById(R.id.tvGoldSell);
        tvGoldUnit     = v.findViewById(R.id.tvGoldUnit);

        // Hashtag chips
        chipThoiSu  = v.findViewById(R.id.chipThoiSu);
        chipTheThao = v.findViewById(R.id.chipTheThao);
        chipKinhTe  = v.findViewById(R.id.chipKinhTe);
        chipSucKhoe = v.findViewById(R.id.chipSucKhoe);
        chipKhoaHoc = v.findViewById(R.id.chipKhoaHoc);
        chipTheGioi = v.findViewById(R.id.chipTheGioi);
        chipGiaiTri = v.findViewById(R.id.chipGiaiTri);
    }

    private void setupRecyclerView() {
        adapter = new NewsAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                if (q.isEmpty()) {
                    // Nếu người dùng xoá hết chữ → reset chip + thoát search
                    setChipActive(null);
                    viewModel.exitSearch();
                } else if (q.length() >= 3) {
                    viewModel.searchNews(q);
                }
            }
        });
    }

    private void setupHashtags() {
        // Map chip → từ khoá tìm kiếm
        Map<TextView, String> chips = new LinkedHashMap<>();
        chips.put(chipThoiSu,  "thời sự");
        chips.put(chipTheThao, "thể thao");
        chips.put(chipKinhTe,  "kinh tế");
        chips.put(chipSucKhoe, "sức khỏe");
        chips.put(chipKhoaHoc, "khoa học");
        chips.put(chipTheGioi, "thế giới");
        chips.put(chipGiaiTri, "giải trí");

        for (Map.Entry<TextView, String> entry : chips.entrySet()) {
            TextView chip    = entry.getKey();
            String   keyword = entry.getValue();

            chip.setOnClickListener(v -> {
                if (chip == activeChip) {
                    // Bấm lại chip đang chọn → thoát search
                    setChipActive(null);
                    etSearch.setText("");
                    viewModel.exitSearch();
                } else {
                    // Chọn chip mới → search theo từ khoá
                    setChipActive(chip);
                    etSearch.setText(keyword);
                    etSearch.setSelection(keyword.length()); // đặt con trỏ cuối
                    viewModel.searchNews(keyword);
                }
            });
        }
    }

    /**
     * Đặt chip được chọn (active) và reset chip cũ về trạng thái bình thường.
     * Truyền null để bỏ chọn tất cả.
     */
    private void setChipActive(TextView chip) {
        if (activeChip != null) {
            activeChip.setBackgroundResource(R.drawable.bg_chip);
            activeChip.setTextColor(0xFF1877F2);
        }
        activeChip = chip;
        if (chip != null) {
            chip.setBackgroundResource(R.drawable.bg_chip_active);
            chip.setTextColor(0xFFFFFFFF);
        }
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(0xFF1877F2);
        swipeRefresh.setOnRefreshListener(() -> {
            // Khi swipe refresh → reset chip + load lại trang chủ
            setChipActive(null);
            etSearch.setText("");
            viewModel.refresh();
        });
    }

    private void observeViewModel() {

        // Bài báo — từ Room (luôn có dữ liệu kể cả offline)
        viewModel.getArticles().observe(getViewLifecycleOwner(), articles -> {
            swipeRefresh.setRefreshing(false);
            if (articles != null && !articles.isEmpty()) {
                adapter.setArticles(articles);
                tvError.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                tvError.setText("Chưa có bài viết nào.\nKéo xuống để tải.");
                tvError.setVisibility(View.VISIBLE);
            }
        });

        // Loading
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (!swipeRefresh.isRefreshing())
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (!loading) swipeRefresh.setRefreshing(false);
        });

        // Lỗi thông thường
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Toast offline — hiện banner thay vì Toast
        viewModel.getToast().observe(getViewLifecycleOwner(), type -> {
            if ("offline".equals(type) && offlineBanner != null) {
                offlineBanner.setVisibility(View.VISIBLE);
                offlineBanner.postDelayed(
                        () -> offlineBanner.setVisibility(View.GONE), 4000);
            }
        });

        viewModel.getWidgetData().observe(getViewLifecycleOwner(), this::bindWidgetData);
    }

    private void bindWidgetData(WidgetData data) {
        if (data == null) return;
        if (tvDayOfWeek != null)   tvDayOfWeek.setText(data.getDayOfWeek());
        if (tvDate != null)        tvDate.setText(data.getDateStr());
        if (tvWeatherCity != null) tvWeatherCity.setText(data.getWeatherCity());
        if (tvWeatherTemp != null) tvWeatherTemp.setText(data.getWeatherTemp());
        if (tvWeatherDesc != null) tvWeatherDesc.setText(data.getWeatherDesc());
        if (imgWeatherIcon != null && data.getWeatherIcon() != null)
            Glide.with(this).load(data.getWeatherIcon()).into(imgWeatherIcon);
        if (tvPetrolPrice != null) tvPetrolPrice.setText(data.getPetrolPrice());
        if (tvPetrolType != null)  tvPetrolType.setText(data.getPetrolDate());
        if (tvGoldBuy != null)     tvGoldBuy.setText(data.getGoldBuy());
        if (tvGoldSell != null)    tvGoldSell.setText(data.getGoldSell());
        if (tvGoldUnit != null)    tvGoldUnit.setText(data.getGoldUnit());
    }
}