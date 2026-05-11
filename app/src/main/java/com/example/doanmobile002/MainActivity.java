package com.example.doanmobile002;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dòng này cực kỳ quan trọng để hiển thị file activity_main.xml lên màn hình
        setContentView(R.layout.activity_main);

        // Chỉ cần khai báo để tránh lỗi, chưa cần viết logic điều hướng
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Để thanh menu sáng lên khi bạn bấm vào (dù chưa chuyển trang)
        // mặc định Android sẽ tự xử lý việc đổi màu icon khi click.
    }
}