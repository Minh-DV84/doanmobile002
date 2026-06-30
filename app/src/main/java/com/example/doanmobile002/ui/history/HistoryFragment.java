package com.example.doanmobile002.ui.history;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmobile002.R;
import com.example.doanmobile002.models.NewsArticle; // Import model bài viết để nhận diện dữ liệu khi vuốt

/**
 * Tab Lịch sử đọc — hiển thị tất cả bài đã từng mở (DetailActivity gọi
 * markArticleAsRead() mỗi lần mở bài). Hoạt động cả khi offline vì
 * dữ liệu đọc trực tiếp từ Room, không gọi mạng.
 */
public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private HistoryAdapter   adapter;

    private RecyclerView recyclerView;
    private TextView     tvEmpty;
    private View         emptyLayout; // Khai báo cụm Empty State mới (Tính năng 1)
    private View         btnClearHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        bindViews(view);
        setupRecyclerView();
        observeHistory();
        setupClearButton();
    }

    private void bindViews(View v) {
        recyclerView    = v.findViewById(R.id.historyRecyclerView);
        tvEmpty         = v.findViewById(R.id.historyTvEmpty);
        emptyLayout     = v.findViewById(R.id.historyEmptyLayout); // Ánh xạ cụm trống (Tính năng 1)
        btnClearHistory = v.findViewById(R.id.btnClearHistory);
    }

    private void setupRecyclerView() {
        // Khởi tạo adapter
        adapter = new HistoryAdapter(requireContext(), article -> {
            // Click nút xóa hoặc long-press cũ của bạn
            viewModel.removeFromHistory(article);
            Toast.makeText(requireContext(), "Đã xóa khỏi lịch sử", Toast.LENGTH_SHORT).show();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Cấu hình logic Vuốt để xóa (Swipe to Delete - Tính năng 2)
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // Không dùng tính năng kéo thả sắp xếp thứ tự
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Lấy vị trí item bị vuốt
                int position = viewHolder.getBindingAdapterPosition();

                // Lấy bài viết tại vị trí đó (Yêu cầu bạn đã thêm hàm getArticleAt bên HistoryAdapter)
                NewsArticle article = adapter.getArticleAt(position);

                if (article != null) {
                    // Tiến hành xóa khỏi Room Database
                    viewModel.removeFromHistory(article);
                    Toast.makeText(requireContext(), "Đã xóa khỏi lịch sử", Toast.LENGTH_SHORT).show();
                }
            }

            // Vẽ nền màu đỏ phía sau item khi người dùng đang thực hiện vuốt
            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState,
                                    boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#D32F2F")); // Đỏ sẫm chuyên nghiệp

                if (dX > 0) { // Vuốt từ trái sang phải
                    c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(),
                            dX, (float) itemView.getBottom(), paint);
                } else if (dX < 0) { // Vuốt từ phải sang trái
                    c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                            (float) itemView.getRight(), (float) itemView.getBottom(), paint);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        // Gắn bộ hỗ trợ vuốt chạm vào RecyclerView
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void observeHistory() {
        viewModel.getHistoryArticles().observe(getViewLifecycleOwner(), articles -> {
            if (articles != null && !articles.isEmpty()) {
                adapter.setArticles(articles);
                if (emptyLayout != null) emptyLayout.setVisibility(View.GONE); // Ẩn cụm trống đi
                recyclerView.setVisibility(View.VISIBLE);
                if (btnClearHistory != null) btnClearHistory.setVisibility(View.VISIBLE);
            } else {
                adapter.setArticles(null);
                if (tvEmpty != null) {
                    tvEmpty.setText("Bạn chưa đọc bài viết nào.");
                }
                if (emptyLayout != null) emptyLayout.setVisibility(View.VISIBLE); // Hiện cụm trống (gồm ảnh và chữ)
                recyclerView.setVisibility(View.GONE);
                if (btnClearHistory != null) btnClearHistory.setVisibility(View.GONE);
            }
        });
    }

    private void setupClearButton() {
        if (btnClearHistory == null) return;
        btnClearHistory.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xóa toàn bộ lịch sử?")
                        .setMessage("Các bài đã lưu (❤) sẽ không bị ảnh hưởng.")
                        .setPositiveButton("Xóa", (d, w) -> {
                            viewModel.clearAllHistory();
                            Toast.makeText(requireContext(),
                                    "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show());
    }
}