package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Start with LoginFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (id == R.id.nav_dashboard) {
                selected = new DashboardFragment();
            } else if (id == R.id.nav_leaderboard) {
                selected = new LeaderboardFragment();
            } else if (id == R.id.nav_friends) {
                selected = new FriendsFragment();
            }
            else if (id == R.id.nav_gallery) {
                selected = new GalleryFragment();
            }
            else if (id == R.id.nav_profile) {
                selected = new ProfileFragment();
            }

            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selected)
                        .commit();
                return true;
            }
            return false;
        });

        // hide bottom nav initially (login screen)
        setBottomNavVisible(false);
    }

    public void setBottomNavVisible(boolean visible) {
        bottomNav.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
