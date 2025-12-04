package com.itismob.s15.group3.mco.project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DashboardFragment extends Fragment {

    private TextView tvStreakTimer, restoresLeftView, lostStreakMessage;
    private ProgressBar streakProgressBar;
    private Button btnRestore;
    private Spinner spinnerStreaks;
    private LinearLayout streaksContainer;
    private SwitchCompat toggleDailySwitch, toggleStreakSwitch;
    private View streakTimerCard, previousStreaksCard;

    private DatabaseReference mDatabase;
    private String currentUid;
    private User currentUser;
    private CountDownTimer timer;

    private static final String CHANNEL_ID = "SNAPBIT_CHANNEL";
    private boolean dailyNotified = false;
    private boolean streakWarningNotified = false;

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
        
        streakTimerCard = view.findViewById(R.id.streakTimerCard);
        previousStreaksCard = view.findViewById(R.id.previousStreaksCard);
        
        View streakYou = view.findViewById(R.id.streakYou);
        if (streakYou != null) {
            streaksContainer = (LinearLayout) streakYou.getParent();
        }

        toggleDailySwitch = view.findViewById(R.id.toggleDailySwitch);
        toggleStreakSwitch = view.findViewById(R.id.toggleStreakSwitch);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (getActivity() != null) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
            currentUid = prefs.getString("uid", null);
        }

        createNotificationChannel();

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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SnapBit Notifications";
            String description = "Reminders for streaks";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String title, String content) {
        if (!isAdded() || getContext() == null) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon if available
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
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
        long sixHoursMillis = 6 * 60 * 60 * 1000;

        if (timer != null) timer.cancel();
        timer = new CountDownTimer(timeUntilMidnight, 1000) {
            public void onTick(long millisUntilFinished) {
                if (!isAdded() || getContext() == null) {
                    cancel();
                    return;
                }

                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                
                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                if (tvStreakTimer != null) tvStreakTimer.setText(time);
                
                int progress = (int) ((millisUntilFinished * 100) / totalDayMillis);
                if (streakProgressBar != null) streakProgressBar.setProgress(progress);

                // Logic for reminders
                checkReminders(millisUntilFinished, sixHoursMillis);
            }
            public void onFinish() {
                if (!isAdded() || getContext() == null) return;

                if (tvStreakTimer != null) tvStreakTimer.setText("00:00:00");
                if (streakProgressBar != null) streakProgressBar.setProgress(0);
                dailyNotified = false;
                streakWarningNotified = false;
                setupGlobalTimer(); // Restart for next day
            }
        }.start();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) {
            timer.cancel();
        }
    }

    private void checkReminders(long millisUntilFinished, long sixHoursMillis) {
        if (!isAdded() || getActivity() == null) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean dailyEnabled = prefs.getBoolean("daily_reminder", true);
        boolean streakEnabled = prefs.getBoolean("streak_warning", false);

        // Check if streak maintained today
        boolean maintainedToday = false;
        if (currentUser != null) {
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(currentUser.lastStreakUpdate);
            Calendar now = Calendar.getInstance();
            maintainedToday = last.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                              last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        }
        
        // If maintained today, do not send warnings
        if (maintainedToday) return;

        // 6 hours left (allow some buffer for timer tick)
        if (millisUntilFinished <= sixHoursMillis && millisUntilFinished > (sixHoursMillis - 60000)) {
            if (dailyEnabled && !dailyNotified) {
                sendNotification("New day", "A new day is up and you should do your streak! 6 hours left.");
                dailyNotified = true;
            }
            if (streakEnabled && !streakWarningNotified) {
                sendNotification("Streak Warning", "Your streak is about to end!");
                streakWarningNotified = true;
            }
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sample_streaks, android.R.layout.simple_spinner_item);
        
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
                if (!isAdded()) return;
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    checkMonthlyRestoreReset();
                    updateRestoreUI();
                    loadFriendsStreaks(spinnerStreaks.getSelectedItemPosition() == 0);
                    updateCardsVisibility();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Restore reset logic: Every month reset to 3, do not stack
    private void checkMonthlyRestoreReset() {
        if (currentUser == null) return;
        
        Calendar now = Calendar.getInstance();
        Calendar lastReset = Calendar.getInstance();
        lastReset.setTimeInMillis(currentUser.lastRestoreReset);
        
        // If never reset (0) or different month/year
        if (currentUser.lastRestoreReset == 0 || 
            now.get(Calendar.MONTH) != lastReset.get(Calendar.MONTH) ||
            now.get(Calendar.YEAR) != lastReset.get(Calendar.YEAR)) {
            
            // Reset to 3
            currentUser.restoresLeft = 3;
            currentUser.lastRestoreReset = now.getTimeInMillis();
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("restoresLeft", 3);
            updates.put("lastRestoreReset", now.getTimeInMillis());
            
            mDatabase.child("users").child(currentUid).updateChildren(updates);
        }
    }

    private void loadFriendsStreaks(boolean topOnly) {
        if (currentUid == null || streaksContainer == null) return;

        mDatabase.child("users").child(currentUid).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
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
                    if (!isAdded()) return;
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
        if (getContext() == null) return;
        Collections.sort(users, (u1, u2) -> Integer.compare(u2.streak, u1.streak));

        // Clear previous list except the title and spinner row (indices 0)
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
        if (currentUser == null || getContext() == null) return;

        lostStreakMessage.setText("Lost Streak: " + currentUser.lostStreak + " days");
        restoresLeftView.setText("Restores left for this month: " + currentUser.restoresLeft);
        
        // Check if streak maintained today
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(currentUser.lastStreakUpdate);
        Calendar now = Calendar.getInstance();
        
        boolean sameDay = last.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                          last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

        if (sameDay) {
             // Streak maintained today, no need to restore
             btnRestore.setEnabled(false);
             btnRestore.setText("Streak Maintained");
             btnRestore.setBackgroundTintList(getContext().getColorStateList(android.R.color.darker_gray));
        } else {
            if (currentUser.restoresLeft > 0 && currentUser.lostStreak > 0) {
                btnRestore.setEnabled(true);
                btnRestore.setText("Restore Streak");
                btnRestore.setBackgroundTintList(getContext().getColorStateList(android.R.color.holo_green_dark));
            } else {
                btnRestore.setEnabled(false);
                btnRestore.setText("Restore Streak");
                btnRestore.setBackgroundTintList(getContext().getColorStateList(android.R.color.darker_gray));
            }
        }
    }

    private void restoreStreak() {
        if (currentUser == null || currentUser.restoresLeft <= 0 || currentUser.lostStreak <= 0) return;
        
        // Double check if maintained today
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(currentUser.lastStreakUpdate);
        Calendar now = Calendar.getInstance();
        boolean sameDay = last.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                          last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        
        if (sameDay) {
            Toast.makeText(getContext(), "Streak already maintained for today.", Toast.LENGTH_SHORT).show();
            return;
        }

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
        if (!isAdded() || getActivity() == null) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean daily = prefs.getBoolean("daily_reminder", true);
        boolean streakWarning = prefs.getBoolean("streak_warning", false);
        
        toggleDailySwitch.setChecked(daily);
        toggleStreakSwitch.setChecked(streakWarning);
        
        updateCardsVisibility();
    }

    private void saveSwitchState(String key, boolean value) {
        if (!isAdded() || getActivity() == null) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
        
        String msg = value ? "enabled" : "disabled";
        String type = key.equals("daily_reminder") ? "Daily reminders" : "Streak warnings";
        Toast.makeText(getContext(), type + " " + msg, Toast.LENGTH_SHORT).show();
        
        updateCardsVisibility();
    }
    
    private void updateCardsVisibility() {
        if (!isAdded() || getActivity() == null) return;
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean dailyEnabled = prefs.getBoolean("daily_reminder", true);
        boolean streakWarningEnabled = prefs.getBoolean("streak_warning", false);
        
        int currentStreak = (currentUser != null) ? currentUser.streak : 0;
        
        // First card: Show only if daily reminders ON
        // We removed the condition 'currentStreak > 0' as per user request
        if (streakTimerCard != null) {
            if (dailyEnabled) {
                streakTimerCard.setVisibility(View.VISIBLE);
            } else {
                streakTimerCard.setVisibility(View.GONE);
            }
        }
        
        // Third card: Show only if streak warnings ON
        if (previousStreaksCard != null) {
            if (streakWarningEnabled) {
                previousStreaksCard.setVisibility(View.VISIBLE);
            } else {
                previousStreaksCard.setVisibility(View.GONE);
            }
        }
    }
}
