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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmobile002.R;
import com.example.doanmobile002.ui.login.LoginActivity;
import com.example.doanmobile002.utils.FontSizeManager;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UtilitiesFragment extends Fragment {

    private UtilitiesViewModel viewModel;
    private SavedNewsAdapter   adapter;

    // Các View cho Bài viết đã lưu
    private RecyclerView recyclerView;
    private TextView     tvEmpty;
    private View         layoutLoginPrompt;
    private Button       btnGoLogin;

    // Các View cho Cỡ chữ (dùng thanh trượt % thay vì nút bấm)
    private TextView tvFontPreview;
    private TextView tvFontPercent;
    private Slider   sliderFontSize;

    private FirebaseAuth firebaseAuth;

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
        setupRecyclerView();
        checkLoginState();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLoginState();
        // Cập nhật lại UI cỡ chữ (nhỡ user đổi ở màn hình khác hoặc xóa data)
        int currentPercent = FontSizeManager.getFontPercent(requireContext());
        if (sliderFontSize != null) {
            sliderFontSize.setValue(currentPercent);
        }
        refreshFontUI(currentPercent);
    }

    // ── Bind views ────────────────────────────────────────────────────────────

    private void bindViews(View v) {
        // Ánh xạ bài đã lưu
        recyclerView      = v.findViewById(R.id.savedRecyclerView);
        tvEmpty           = v.findViewById(R.id.savedTvEmpty);
        layoutLoginPrompt = v.findViewById(R.id.layoutLoginPrompt);
        btnGoLogin        = v.findViewById(R.id.btnGoLogin);

        // Ánh xạ cỡ chữ (Slider)
        tvFontPreview  = v.findViewById(R.id.tvFontPreview);
        tvFontPercent  = v.findViewById(R.id.tvFontPercent);
        sliderFontSize = v.findViewById(R.id.sliderFontSize);
    }

    // ── Font Size Card (Slider) ───────────────────────────────────────────────

    private void setupFontSizeCard() {
        // Lấy % hiện tại (mặc định 100%)
        int currentPercent = FontSizeManager.getFontPercent(requireContext());

        // Cài đặt giá trị ban đầu cho Slider
        sliderFontSize.setValue(currentPercent);
        refreshFontUI(currentPercent);

        // Lắng nghe sự kiện kéo thả Slider
        sliderFontSize.addOnChangeListener((slider, value, fromUser) -> {
            int percent = (int) value;
            // Lưu mức phần trăm mới vào SharedPreferences
            FontSizeManager.setFontPercent(requireContext(), percent);
            // Cập nhật giao diện
            refreshFontUI(percent);
        });
    }

    /**
     * Cập nhật văn bản % và kích thước text preview mô phỏng theo phần trăm.
     */
    private void refreshFontUI(int percent) {
        if (tvFontPreview == null || tvFontPercent == null) return;

        // Cập nhật text badge (Ví dụ: "120%")
        tvFontPercent.setText(FontSizeManager.getLabel(percent));

        // Mô phỏng cỡ chữ thay đổi. Giả sử cỡ gốc là 16sp.
        // Công thức: Cỡ gốc * (phần trăm / 100)
        float baseSizeSp = 16f;
        float newSizeSp = baseSizeSp * (percent / 100f);

        // Áp dụng cỡ chữ đã quy đổi vào Preview Text
        tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSizeSp);
    }

    // ── RecyclerView bài đã lưu ───────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new SavedNewsAdapter(requireContext(), article ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Bỏ lưu bài viết?")
                        .setMessage(article.getTitle())
                        .setPositiveButton("Bỏ lưu", (d, w) -> {
                            viewModel.unsaveArticle(article);
                            Toast.makeText(requireContext(),
                                    "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show()
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    // ── Login state ───────────────────────────────────────────────────────────

    private void checkLoginState() {
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
            observeSavedArticles();
        }
    }

    private void observeSavedArticles() {
        viewModel.getSavedArticles().observe(getViewLifecycleOwner(), articles -> {
            if (articles != null && !articles.isEmpty()) {
                adapter.setArticles(articles);
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                adapter.setArticles(null);
                tvEmpty.setText("Bạn chưa lưu bài viết nào.\nBấm ❤ trên bài viết để lưu.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}