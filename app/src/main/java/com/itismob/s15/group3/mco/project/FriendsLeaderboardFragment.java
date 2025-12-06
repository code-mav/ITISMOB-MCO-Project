package com.itismob.s15.group3.mco.project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendsLeaderboardFragment extends Fragment {

    private static final int MODE_TOTAL = 0;
    private static final int MODE_FITNESS = 1;
    private static final int MODE_LEARNING = 2;
    private static final int MODE_HEALTH = 3;
    private static final int MODE_CREATIVITY = 4;
    private static final int MODE_PRODUCTIVITY = 5;

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private ArrayList<User> leaderboardList;
    private DatabaseReference usersRef;
    private String currentUid;

    private Spinner spinnerViewMode;
    private int currentMode = MODE_TOTAL;

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

        spinnerViewMode = view.findViewById(R.id.spinnerViewMode);
        setupViewModeSpinner();

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

    private void setupViewModeSpinner() {
        if (getContext() == null) return;

        String[] modes = new String[]{
                "Total streak",
                "Fitness",
                "Learning",
                "Health",
                "Creativity",
                "Productivity"
        };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                modes
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerViewMode.setAdapter(spinnerAdapter);

        spinnerViewMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentMode = MODE_TOTAL;
                        break;
                    case 1:
                        currentMode = MODE_FITNESS;
                        break;
                    case 2:
                        currentMode = MODE_LEARNING;
                        break;
                    case 3:
                        currentMode = MODE_HEALTH;
                        break;
                    case 4:
                        currentMode = MODE_CREATIVITY;
                        break;
                    case 5:
                        currentMode = MODE_PRODUCTIVITY;
                        break;
                    default:
                        currentMode = MODE_TOTAL;
                }
                sortAndDisplay(); // Re-sort whenever mode changes
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadFriendsLeaderboard() {
        usersRef.child(currentUid).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                Set<String> uidsToLoad = new HashSet<>();
                uidsToLoad.add(currentUid); // include self

                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String friendUid = friendSnapshot.getKey();
                    if (friendUid != null) {
                        uidsToLoad.add(friendUid);
                    }
                }

                fetchUsers(new ArrayList<>(uidsToLoad));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ignore for now
            }
        });
    }

    private void fetchUsers(List<String> uids) {
        leaderboardList.clear();
        if (uids.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        final int total = uids.size();
        final int[] count = {0};

        for (String uid : uids) {
            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded() || getContext() == null) return;

                    String name = snapshot.child("fullName").getValue(String.class);

                    // Per-category CURRENT streaks (written from HomeFragment)
                    Integer fitVal = snapshot.child("fitnessStreak").getValue(Integer.class);
                    Integer learnVal = snapshot.child("learningStreak").getValue(Integer.class);
                    Integer healthVal = snapshot.child("healthStreak").getValue(Integer.class);
                    Integer creatVal = snapshot.child("creativityStreak").getValue(Integer.class);
                    Integer prodVal = snapshot.child("productivityStreak").getValue(Integer.class);

                    int fitness = (fitVal != null) ? fitVal : 0;
                    int learning = (learnVal != null) ? learnVal : 0;
                    int health = (healthVal != null) ? healthVal : 0;
                    int creativity = (creatVal != null) ? creatVal : 0;
                    int productivity = (prodVal != null) ? prodVal : 0;

                    // Total = sum of current category streaks
                    int totalCurrentStreak = fitness + learning + health + creativity + productivity;

                    User user = new User();
                    user.uid = uid;
                    user.fullName = (name != null) ? name : "Unknown";

                    // Store scores in the extra fields (all are CURRENT streaks)
                    user.totalScore = totalCurrentStreak;
                    user.fitnessScore = fitness;
                    user.learningScore = learning;
                    user.healthScore = health;
                    user.creativityScore = creativity;
                    user.productivityScore = productivity;

                    // Default visible "streak" is totalScore (for adapter)
                    user.streak = user.totalScore;

                    leaderboardList.add(user);

                    count[0]++;
                    if (count[0] == total) {
                        sortAndDisplay();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    count[0]++;
                    if (count[0] == total) {
                        sortAndDisplay();
                    }
                }
            });
        }
    }

    private int getScoreForCurrentMode(User u) {
        switch (currentMode) {
            case MODE_FITNESS:
                return u.fitnessScore;       // current Fitness streak
            case MODE_LEARNING:
                return u.learningScore;      // current Learning streak
            case MODE_HEALTH:
                return u.healthScore;        // current Health streak
            case MODE_CREATIVITY:
                return u.creativityScore;    // current Creativity streak
            case MODE_PRODUCTIVITY:
                return u.productivityScore;  // current Productivity streak
            case MODE_TOTAL:
            default:
                return u.totalScore;         // sum of all current streaks
        }
    }

    private void applyCurrentModeToUsers() {
        for (User u : leaderboardList) {
            // This is what the adapter shows as the "streak" value
            u.streak = getScoreForCurrentMode(u);
        }
    }

    private void sortAndDisplay() {
        if (!isAdded() || getContext() == null) return;

        applyCurrentModeToUsers();

        Collections.sort(leaderboardList, (u1, u2) -> Integer.compare(u2.streak, u1.streak));
        adapter.notifyDataSetChanged();
    }
}
