package com.example.doanmobile002.ui.utilities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.doanmobile002.ui.home.NewsAdapter;
import com.example.doanmobile002.ui.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UtilitiesFragment extends Fragment {

    private UtilitiesViewModel viewModel;
    private SavedNewsAdapter   adapter;

    private RecyclerView recyclerView;
    private TextView     tvEmpty;
    private View         layoutLoginPrompt;  // card hiện khi chưa đăng nhập
    private Button       btnGoLogin;

    private FirebaseAuth firebaseAuth;

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
        setupRecyclerView();
        checkLoginState();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra lại khi quay về (sau khi đăng nhập)
        checkLoginState();
    }

    private void bindViews(View v) {
        recyclerView      = v.findViewById(R.id.savedRecyclerView);
        tvEmpty           = v.findViewById(R.id.savedTvEmpty);
        layoutLoginPrompt = v.findViewById(R.id.layoutLoginPrompt);
        btnGoLogin        = v.findViewById(R.id.btnGoLogin);
    }

    private void setupRecyclerView() {
        adapter = new SavedNewsAdapter(requireContext(), article -> {
            // Long press → hỏi xoá
            new AlertDialog.Builder(requireContext())
                    .setTitle("Bỏ lưu bài viết?")
                    .setMessage(article.getTitle())
                    .setPositiveButton("Bỏ lưu", (d, w) -> {
                        viewModel.unsaveArticle(article);
                        Toast.makeText(requireContext(), "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void checkLoginState() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            // Chưa đăng nhập → ẩn list, hiện prompt
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            layoutLoginPrompt.setVisibility(View.VISIBLE);

            btnGoLogin.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), LoginActivity.class)));
        } else {
            // Đã đăng nhập → ẩn prompt, hiện danh sách
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