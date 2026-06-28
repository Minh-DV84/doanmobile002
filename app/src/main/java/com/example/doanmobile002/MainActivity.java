package com.example.doanmobile002;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.doanmobile002.ui.games.GamesFragment;
import com.example.doanmobile002.ui.home.HomeFragment;
import com.example.doanmobile002.ui.login.LoginActivity;
import com.example.doanmobile002.ui.profile.ProfileActivity;
import com.example.doanmobile002.ui.trending.TrendingFragment;
import com.example.doanmobile002.ui.utilities.UtilitiesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ImageView            btnProfile;
    private FirebaseAuth         firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Phải gọi TRƯỚC super.onCreate() và setContentView().
        // Tự động đổi theme từ Theme.Doanmobile002.Splash sang
        // Theme.Doanmobile002 (khai báo qua postSplashScreenTheme trong manifest).
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        btnProfile   = findViewById(R.id.btnProfile);
        bottomNav    = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        updateProfileIcon();

        btnProfile.setOnClickListener(v -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                startActivity(new Intent(this, ProfileActivity.class));
            }
        });

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    loadFragment(new HomeFragment());
                    return true;
                } else if (id == R.id.nav_trending) {
                    loadFragment(new TrendingFragment());
                    return true;
                } else if (id == R.id.nav_games) {
                    loadFragment(new GamesFragment());
                    return true;
                } else if (id == R.id.nav_utilities) {
                    loadFragment(new UtilitiesFragment());
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileIcon();
    }

    private void updateProfileIcon() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(btnProfile);
        } else {
            btnProfile.setImageResource(R.drawable.ic_profile);
            btnProfile.setColorFilter(
                    getResources().getColor(android.R.color.darker_gray, getTheme()));
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}