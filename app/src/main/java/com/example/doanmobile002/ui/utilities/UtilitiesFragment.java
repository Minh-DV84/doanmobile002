package com.example.doanmobile002.ui.utilities;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmobile002.R;
import com.example.doanmobile002.models.NewsArticle;
import com.example.doanmobile002.ui.login.LoginActivity;
import com.example.doanmobile002.utils.FontSizeManager;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class UtilitiesFragment extends Fragment {

    private static final int TAB_SAVED   = 0;
    private static final int TAB_HISTORY = 1;

    private UtilitiesViewModel viewModel;
    private SavedNewsAdapter   savedAdapter;
    private HistoryNewsAdapter historyAdapter;

    // Vùng nội dung dùng chung cho cả 2 tab
    private RecyclerView recyclerView;
    private TextView     tvEmpty;
    private View         layoutLoginPrompt;
    private Button       btnGoLogin;
    private TabLayout    tabSavedHistory;
    private TextView     btnClearHistory;

    // Cỡ chữ
    private TextView tvFontPreview;
    private TextView tvFontPercent;
    private Slider   sliderFontSize;

    private FirebaseAuth firebaseAuth;
    private int          currentTab = TAB_SAVED;

    // Giữ tham chiếu observer hiện tại để gỡ đúng cái khi đổi tab,
    // tránh observe trùng 2 LiveData cùng lúc gây lag/giật UI.
    private Observer<List<NewsArticle>> activeObserver;
    private LiveData<List<NewsArticle>> activeLiveData;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_utilities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        viewModel    = new ViewModelProvider(this).get(UtilitiesViewModel.class);

        bindViews(view);
        setupFontSizeCard();
        setupAdapters();
        setupTabs();
        switchToTab(TAB_SAVED);   // mặc định mở tab Đã lưu
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại UI cỡ chữ
        int currentPercent = FontSizeManager.getFontPercent(requireContext());
        if (sliderFontSize != null) sliderFontSize.setValue(currentPercent);
        refreshFontUI(currentPercent);

        // Re-check login khi quay lại tab Đã lưu (sau khi đăng nhập ở màn khác)
        if (currentTab == TAB_SAVED) updateSavedTabVisibility();
    }

    // ── Bind views ────────────────────────────────────────────────────────────

    private void bindViews(View v) {
        recyclerView      = v.findViewById(R.id.savedRecyclerView);
        tvEmpty           = v.findViewById(R.id.savedTvEmpty);
        layoutLoginPrompt = v.findViewById(R.id.layoutLoginPrompt);
        btnGoLogin        = v.findViewById(R.id.btnGoLogin);
        tabSavedHistory   = v.findViewById(R.id.tabSavedHistory);
        btnClearHistory   = v.findViewById(R.id.btnClearHistory);

        tvFontPreview  = v.findViewById(R.id.tvFontPreview);
        tvFontPercent  = v.findViewById(R.id.tvFontPercent);
        sliderFontSize = v.findViewById(R.id.sliderFontSize);
    }

    // ── Font Size Card (giữ nguyên không đổi) ───────────────────────────────

    private void setupFontSizeCard() {
        int currentPercent = FontSizeManager.getFontPercent(requireContext());
        sliderFontSize.setValue(currentPercent);
        refreshFontUI(currentPercent);

        sliderFontSize.addOnChangeListener((slider, value, fromUser) -> {
            int percent = (int) value;
            FontSizeManager.setFontPercent(requireContext(), percent);
            refreshFontUI(percent);
        });
    }

    private void refreshFontUI(int percent) {
        if (tvFontPreview == null || tvFontPercent == null) return;
        tvFontPercent.setText(FontSizeManager.getLabel(percent));
        float baseSizeSp = 16f;
        float newSizeSp  = baseSizeSp * (percent / 100f);
        tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSizeSp);
    }

    // ── Adapters ──────────────────────────────────────────────────────────────

    private void setupAdapters() {
        savedAdapter = new SavedNewsAdapter(requireContext(), article ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Bỏ lưu bài viết?")
                        .setMessage(article.getTitle())
                        .setPositiveButton("Bỏ lưu", (d, w) -> {
                            viewModel.unsaveArticle(article);
                            Toast.makeText(requireContext(), "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show()
        );

        historyAdapter = new HistoryNewsAdapter(requireContext(), article -> {
            viewModel.removeFromHistory(article);
            Toast.makeText(requireContext(), "Đã xóa khỏi lịch sử", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private void setupTabs() {
        tabSavedHistory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                switchToTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnClearHistory.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xóa toàn bộ lịch sử?")
                        .setMessage("Các bài đã lưu (❤) sẽ không bị ảnh hưởng.")
                        .setPositiveButton("Xóa", (d, w) -> {
                            viewModel.clearAllHistory();
                            Toast.makeText(requireContext(), "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show());
    }

    /**
     * Chuyển nội dung RecyclerView giữa 2 tab: gỡ observer cũ, gắn observer mới,
     * đổi adapter, và cập nhật trạng thái UI (login prompt / nút xóa lịch sử).
     */
    private void switchToTab(int tab) {
        currentTab = tab;

        // Gỡ observer LiveData cũ trước khi gắn cái mới, tránh leak / double-update
        if (activeLiveData != null && activeObserver != null) {
            activeLiveData.removeObserver(activeObserver);
        }

        if (tab == TAB_SAVED) {
            btnClearHistory.setVisibility(View.GONE);
            recyclerView.setAdapter(savedAdapter);
            updateSavedTabVisibility();
        } else {
            layoutLoginPrompt.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(historyAdapter);
            observeHistory();
        }
    }

    // ── Tab "Đã lưu" ─────────────────────────────────────────────────────────

    private void updateSavedTabVisibility() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            layoutLoginPrompt.setVisibility(View.VISIBLE);
            btnGoLogin.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), LoginActivity.class)));
        } else {
            layoutLoginPrompt.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            observeSaved();
        }
    }

    private void observeSaved() {
        activeLiveData = viewModel.getSavedArticles();
        activeObserver = articles -> {
            if (articles != null && !articles.isEmpty()) {
                savedAdapter.setArticles(articles);
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                savedAdapter.setArticles(null);
                tvEmpty.setText("Bạn chưa lưu bài viết nào.\nBấm ❤ trên bài viết để lưu.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        };
        activeLiveData.observe(getViewLifecycleOwner(), activeObserver);
    }

    // ── Tab "Lịch sử" ────────────────────────────────────────────────────────

    private void observeHistory() {
        activeLiveData = viewModel.getHistoryArticles();
        activeObserver = articles -> {
            if (articles != null && !articles.isEmpty()) {
                historyAdapter.setArticles(articles);
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                btnClearHistory.setVisibility(View.VISIBLE);
            } else {
                historyAdapter.setArticles(null);
                tvEmpty.setText("Bạn chưa đọc bài viết nào.\nCác bài bạn mở sẽ hiện ở đây, kể cả khi offline.");
                tvEmpty.setVisibility(View.VISIBLE);
                btnClearHistory.setVisibility(View.GONE);
            }
        };
        activeLiveData.observe(getViewLifecycleOwner(), activeObserver);
    }
}