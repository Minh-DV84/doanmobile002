package com.example.doanmobile002.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.doanmobile002.MainActivity; // Import thêm MainActivity
import com.example.doanmobile002.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    // Lưu lại để cập nhật ngay sau khi sửa tên thành công, không cần findViewById lại
    private TextView tvNameHeader;
    private TextView tvNameRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();

        setupToolbar();
        bindUserInfo();
        setupButtons();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tài khoản");
        }
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindUserInfo() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        ImageView imgAvatar = findViewById(R.id.profileImgAvatar);
        tvNameHeader = findViewById(R.id.profileTvName);
        TextView  tvEmail   = findViewById(R.id.profileTvEmail);
        TextView  tvProvider= findViewById(R.id.profileTvProvider);
        tvNameRow  = findViewById(R.id.profileTvNameRow);
        TextView  tvEmailRow = findViewById(R.id.profileTvEmailRow);

        // Tên
        String displayName = user.getDisplayName() != null ?
                user.getDisplayName() : "Người dùng";
        tvNameHeader.setText(displayName);
        tvNameRow.setText(displayName);

        // Email
        String email = user.getEmail() != null ? user.getEmail() : "Không có email";
        tvEmail.setText(email);
        tvEmailRow.setText(email);

        // Nhà cung cấp đăng nhập
        String provider = "Email/Password";
        if (!user.getProviderData().isEmpty()) {
            String pid = user.getProviderData().get(
                    user.getProviderData().size() - 1).getProviderId();
            if (pid.contains("google"))   provider = "Google";
            else if (pid.contains("facebook")) provider = "Facebook";
        }
        tvProvider.setText("Đăng nhập qua: " + provider);

        // Avatar
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_profile)
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_profile);
        }
    }

    private void setupButtons() {
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        ImageView btnEditName = findViewById(R.id.btnEditName);
        btnEditName.setOnClickListener(v -> showEditNameDialog());
    }

    // Hiện dialog nhập tên mới, có sẵn tên hiện tại trong ô input
    private void showEditNameDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        int marginPx = (int) (20 * getResources().getDisplayMetrics().density);

        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (user.getDisplayName() != null) {
            input.setText(user.getDisplayName());
            input.setSelection(input.getText().length());
        }

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("Họ và tên");
        inputLayout.addView(input);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(marginPx, marginPx / 2, marginPx, 0);
        inputLayout.setLayoutParams(lp);
        container.addView(inputLayout);

        new AlertDialog.Builder(this)
                .setTitle("Sửa họ và tên")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText() != null ?
                            input.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateDisplayName(newName);
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // Gọi Firebase để cập nhật tên, cập nhật UI ngay khi thành công
    private void updateDisplayName(String newName) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(request).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                tvNameHeader.setText(newName);
                tvNameRow.setText(newName);
                Toast.makeText(this, "Đã cập nhật tên", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cập nhật thất bại, thử lại sau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hỏi xác nhận trước khi đăng xuất
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void logout() {
        // 1. Đăng xuất Firebase
        firebaseAuth.signOut();


        // 2. Thay đổi đích đến: Về thẳng MainActivity (Trang chủ) thay vì LoginActivity
        Intent intent = new Intent(this, MainActivity.class);

        // Xóa sạch toàn bộ các activity trước đó để tránh lỗi logic khi nhấn nút Back của điện thoại
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // Đóng ProfileActivity
    }
}