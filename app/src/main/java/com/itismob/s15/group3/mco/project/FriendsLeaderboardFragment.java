package com.itismob.s15.group3.mco.project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import java.util.List;

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
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        currentUid = prefs.getString("uid", null);

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
                List<String> friendUids = new ArrayList<>();
                friendUids.add(currentUid); // Add self

                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    friendUids.add(friendSnapshot.getKey());
                }

                fetchUsers(friendUids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void fetchUsers(List<String> uids) {
        leaderboardList.clear();
        
        // This is a bit inefficient for many users, but works for small friend lists
        // A better approach would be to fetch all users once or structure data differently
        for (String uid : uids) {
            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Ensure uid is set if not in the node
                        if (user.uid == null) user.uid = uid;
                        // Default streak if null (though int defaults to 0)
                        leaderboardList.add(user);
                    }

                    // If we have processed all uids (or enough), sort and update
                    // Ideally we wait for all, but for now, let's update as they come or check size
                    if (leaderboardList.size() == uids.size()) {
                        sortAndDisplay();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void sortAndDisplay() {
        Collections.sort(leaderboardList, (u1, u2) -> Integer.compare(u2.streak, u1.streak)); // Descending
        adapter.notifyDataSetChanged();
    }
}
