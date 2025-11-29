package com.itismob.s15.group3.mco.project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itismob.s15.group3.mco.project.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendsLeaderboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private ArrayList<User> leaderboardList;
    private DatabaseReference usersRef;
    private String currentUid;

    public FriendsLeaderboardFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard_friends, container, false);

        recyclerView = view.findViewById(R.id.friendsLeaderboardRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardList = new ArrayList<>();
        adapter = new LeaderboardAdapter(getContext(), leaderboardList);
        recyclerView.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
            currentUid = prefs.getString("uid", null);
        }

        if (currentUid != null) {
            loadFriendsLeaderboard();
        }

        return view;
    }

    private void loadFriendsLeaderboard() {
        // 1. Get current user's friends
        usersRef.child(currentUid).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return; // Check if fragment is still attached

                Set<String> uidsToLoad = new HashSet<>();
                uidsToLoad.add(currentUid); // Add self explicitly

                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String friendUid = friendSnapshot.getKey();
                    if(friendUid != null) {
                        uidsToLoad.add(friendUid);
                    }
                }
                
                // Convert to list for processing
                fetchUsers(new ArrayList<>(uidsToLoad));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void fetchUsers(List<String> uids) {
        leaderboardList.clear();
        if (uids.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }
        
        // Helper to count completions
        final int total = uids.size();
        final int[] count = {0};

        for (String uid : uids) {
            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded() || getContext() == null) return; // Fragment check

                    // We only need basic info + streak
                    String name = snapshot.child("fullName").getValue(String.class);
                    Integer streakVal = snapshot.child("streak").getValue(Integer.class);
                    int streak = (streakVal != null) ? streakVal : 0;

                    User user = new User();
                    user.uid = uid;
                    user.fullName = name != null ? name : "Unknown";
                    user.streak = streak;
                    
                    // Add to list
                    leaderboardList.add(user);
                    
                    // Check if done
                    count[0]++;
                    if (count[0] == total) {
                        sortAndDisplay();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Even on error, we should count it so we don't hang
                    count[0]++;
                    if (count[0] == total) {
                        sortAndDisplay();
                    }
                }
            });
        }
    }

    private void sortAndDisplay() {
        if (!isAdded() || getContext() == null) return; // Safe check before UI update

        Collections.sort(leaderboardList, (u1, u2) -> Integer.compare(u2.streak, u1.streak)); // Descending
        adapter.notifyDataSetChanged();
    }
}
