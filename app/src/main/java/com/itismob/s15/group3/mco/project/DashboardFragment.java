package com.itismob.s15.group3.mco.project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itismob.s15.group3.mco.project.models.User;
import com.itismob.s15.group3.mco.project.models.UserActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DashboardFragment extends Fragment {

    private TextView tvStreakTimer, restoresLeftView, lostStreakMessage;
    private ProgressBar streakProgressBar;
    private Button btnRestore;
    private Spinner spinnerStreaks;
    private LinearLayout streaksContainer;
    private SwitchCompat toggleDailySwitch, toggleStreakSwitch;

    private DatabaseReference mDatabase;
    private String currentUid;
    private User currentUser;
    private CountDownTimer timer;

    public DashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize Views
        tvStreakTimer = view.findViewById(R.id.tvStreakTimer);
        streakProgressBar = view.findViewById(R.id.streakProgressBar);
        btnRestore = view.findViewById(R.id.btnRestore);
        restoresLeftView = view.findViewById(R.id.restoresLeft);
        lostStreakMessage = view.findViewById(R.id.lostStreakMessage);
        spinnerStreaks = view.findViewById(R.id.spinnerStreaks);
        streaksContainer = view.findViewById(R.id.currentStreaksCard).findViewById(R.id.streakYou).getParent() instanceof LinearLayout ? 
                (LinearLayout) view.findViewById(R.id.currentStreaksCard).findViewById(R.id.streakYou).getParent() : null;
        
        // If the XML structure is slightly different (LinearLayout inside CardView), finding parent is safer or finding ID if added
        // The XML shows a LinearLayout inside the CardView. We need to target that LinearLayout to add/remove views.
        // For safety, let's find the LinearLayout directly if possible. In the provided XML, the LinearLayout inside `currentStreaksCard` doesn't have an ID.
        // But `streakYou` is inside it.
        View streakYou = view.findViewById(R.id.streakYou);
        if (streakYou != null) {
            streaksContainer = (LinearLayout) streakYou.getParent();
        }

        toggleDailySwitch = view.findViewById(R.id.toggleDailySwitch);
        toggleStreakSwitch = view.findViewById(R.id.toggleStreakSwitch);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        currentUid = prefs.getString("uid", null);

        // Setup Timer
        setupGlobalTimer();

        // Setup UI & Data
        setupSpinner();
        loadUserData();
        
        // Switches logic
        loadSwitchStates();
        toggleDailySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSwitchState("daily_reminder", isChecked));
        toggleStreakSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSwitchState("streak_warning", isChecked));

        // Restore button
        btnRestore.setOnClickListener(v -> restoreStreak());

        return view;
    }

    private void setupGlobalTimer() {
        // Calculate time until next midnight
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        midnight.add(Calendar.DAY_OF_YEAR, 1); // Next day

        long timeUntilMidnight = midnight.getTimeInMillis() - now.getTimeInMillis();
        long totalDayMillis = 24 * 60 * 60 * 1000;

        if (timer != null) timer.cancel();
        timer = new CountDownTimer(timeUntilMidnight, 1000) {
            public void onTick(long millisUntilFinished) {
                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                
                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                tvStreakTimer.setText(time);
                
                // Progress: 100% at 24h remaining, 0% at 0h.
                // millisUntilFinished / totalDayMillis * 100
                int progress = (int) ((millisUntilFinished * 100) / totalDayMillis);
                streakProgressBar.setProgress(progress);
            }
            public void onFinish() {
                tvStreakTimer.setText("00:00:00");
                streakProgressBar.setProgress(0);
                setupGlobalTimer(); // Restart for next day
            }
        }.start();
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sample_streaks, android.R.layout.simple_spinner_item);
        // If sample_streaks doesn't exist or we want custom:
        String[] options = {"Top Friends", "All Friends"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStreaks.setAdapter(spinnerAdapter);

        spinnerStreaks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadFriendsStreaks(position == 0); // 0 = Top Friends, 1 = All
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUserData() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    // Ensure fields that might be null/0 are handled
                    updateRestoreUI();
                    loadFriendsStreaks(spinnerStreaks.getSelectedItemPosition() == 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadFriendsStreaks(boolean topOnly) {
        if (currentUid == null || streaksContainer == null) return;

        mDatabase.child("users").child(currentUid).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> uids = new ArrayList<>();
                uids.add(currentUid); // Add self
                for (DataSnapshot s : snapshot.getChildren()) {
                    uids.add(s.getKey());
                }
                fetchUsersAndDisplay(uids, topOnly);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchUsersAndDisplay(List<String> uids, boolean topOnly) {
        List<User> users = new ArrayList<>();
        // Helper counter to know when all data is fetched
        final int[] loadedCount = {0};

        for (String uid : uids) {
            mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User u = snapshot.getValue(User.class);
                    if (u != null) {
                        if (u.uid == null) u.uid = uid;
                        users.add(u);
                    }
                    loadedCount[0]++;
                    if (loadedCount[0] == uids.size()) {
                        updateStreaksList(users, topOnly);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                }
            });
        }
    }

    private void updateStreaksList(List<User> users, boolean topOnly) {
        Collections.sort(users, (u1, u2) -> Integer.compare(u2.streak, u1.streak));

        // Clear previous list except the title and spinner row (indices 0)
        // The layout has: 0: Title Row, 1: You, 2: Friend1, 3: Friend2
        // We should remove all views starting from index 1
        int childCount = streaksContainer.getChildCount();
        if (childCount > 1) {
            streaksContainer.removeViews(1, childCount - 1);
        }

        int limit = topOnly ? Math.min(3, users.size()) : users.size();

        for (int i = 0; i < limit; i++) {
            User u = users.get(i);
            TextView tv = new TextView(getContext());
            String text = u.fullName + " - " + u.streak + " days";
            if (u.uid != null && u.uid.equals(currentUid)) {
                text += " (You)";
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            tv.setText(text);
            tv.setTextSize(16);
            tv.setTextColor(getResources().getColor(android.R.color.black));
            tv.setPadding(0, 0, 0, 16); // bottom padding

            streaksContainer.addView(tv);
        }
    }

    private void updateRestoreUI() {
        if (currentUser == null) return;

        lostStreakMessage.setText("Lost Streak: " + currentUser.lostStreak + " days");
        restoresLeftView.setText("Restores left for this month: " + currentUser.restoresLeft);

        if (currentUser.restoresLeft > 0 && currentUser.lostStreak > 0) {
            btnRestore.setEnabled(true);
            btnRestore.setBackgroundTintList(getContext().getColorStateList(android.R.color.holo_green_dark));
        } else {
            btnRestore.setEnabled(false);
            btnRestore.setBackgroundTintList(getContext().getColorStateList(android.R.color.darker_gray));
        }
    }

    private void restoreStreak() {
        if (currentUser == null || currentUser.restoresLeft <= 0 || currentUser.lostStreak <= 0) return;

        int newStreak = currentUser.streak + currentUser.lostStreak;
        int newRestores = currentUser.restoresLeft - 1;
        int newLost = 0;

        // Update local object
        currentUser.streak = newStreak;
        currentUser.restoresLeft = newRestores;
        currentUser.lostStreak = newLost;

        // Update Firebase
        mDatabase.child("users").child(currentUid).child("streak").setValue(newStreak);
        mDatabase.child("users").child(currentUid).child("restoresLeft").setValue(newRestores);
        mDatabase.child("users").child(currentUid).child("lostStreak").setValue(newLost)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Streak Restored! 🔥", Toast.LENGTH_SHORT).show();
                    logActivity("Streak Restored", "Restored a streak of " + (newStreak - currentUser.streak) + " days.");
                    updateRestoreUI();
                });
    }

    private void logActivity(String title, String description) {
        if (currentUid == null) return;
        String key = mDatabase.child("activities").child(currentUid).push().getKey();
        if (key == null) return;

        UserActivity activity = new UserActivity(title, description, System.currentTimeMillis());
        mDatabase.child("activities").child(currentUid).child(key).setValue(activity);
    }

    private void loadSwitchStates() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        toggleDailySwitch.setChecked(prefs.getBoolean("daily_reminder", true));
        toggleStreakSwitch.setChecked(prefs.getBoolean("streak_warning", false));
    }

    private void saveSwitchState(String key, boolean value) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
        
        String msg = value ? "enabled" : "disabled";
        String type = key.equals("daily_reminder") ? "Daily reminders" : "Streak warnings";
        Toast.makeText(getContext(), type + " " + msg, Toast.LENGTH_SHORT).show();
    }
}
