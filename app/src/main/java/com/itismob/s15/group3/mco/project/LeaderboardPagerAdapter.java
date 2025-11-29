package com.itismob.s15.group3.mco.project;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class LeaderboardPagerAdapter extends FragmentStateAdapter {

    public LeaderboardPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Only friends leaderboard
        return new FriendsLeaderboardFragment();
    }

    @Override
    public int getItemCount() {
        return 1; // Only Friends
    }
}
