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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itismob.s15.group3.mco.project.models.User;
import com.itismob.s15.group3.mco.project.models.UserActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DashboardFragment extends Fragment {

    // ----------------------------------------------------
    // DEMO MODE TOGGLE
    // ----------------------------------------------------
    // true  = 1-minute "day" with 30-second warning (for demo)
    // false = real 24-hour day with 6-hour warning
    //
    // TURN THIS TO false AGAIN BEFORE FINAL SUBMISSION.
    private static final boolean DEMO_MODE = false;

    private static final long DAY_WINDOW_MILLIS =
            DEMO_MODE ? 60_000L : 24L * 60L * 60L * 1000L;

    private static final long WARNING_WINDOW_MILLIS =
            DEMO_MODE ? 30_000L : 6L * 60L * 60L * 1000L;

    // UI
    private TextView tvStreakTimer;
    private TextView restoresLeftView;
    private TextView textSelectedCategoryStreak;
    private TextView textBestSelectedStreak;

    private ProgressBar streakProgressBar;
    private Button btnRestore;
    private Spinner spinnerCategory;
    private Spinner spinnerBestCategory;
    private SwitchCompat toggleDailySwitch;
    private SwitchCompat toggleStreakSwitch;

    // Firebase
    private DatabaseReference mDatabase;
    private String currentUid;
    public User currentUser;   // made public so other classes can read if needed

    // Timer
    private CountDownTimer timer;

    // Per-category streaks (current, lost, best)
    // currentX = CURRENT streak from /users/{uid} (computed by HomeFragment)
    // bestX    = LIFETIME best from /users/{uid}/categoryStreaks
    private int fitnessCurrent, learningCurrent, healthCurrent, creativityCurrent, productivityCurrent;
    private int fitnessLost, learningLost, healthLost, creativityLost, productivityLost;
    private int fitnessBest, learningBest, healthBest, creativityBest, productivityBest;

    // Notifications
    private static final String CHANNEL_ID = "SNAPBIT_CHANNEL";
    private boolean dailyNotified = false;
    private boolean streakWarningNotified = false;

    // Optional login-reminder limiter if reused
    private static String reminderShownForUid = null;

    public DashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Find UI
        tvStreakTimer = view.findViewById(R.id.tvStreakTimer);
        streakProgressBar = view.findViewById(R.id.streakProgressBar);

        btnRestore = view.findViewById(R.id.btnRestore);
        restoresLeftView = view.findViewById(R.id.restoresLeft);

        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerBestCategory = view.findViewById(R.id.spinnerBestCategory);

        textSelectedCategoryStreak = view.findViewById(R.id.textSelectedCategoryStreak);
        textBestSelectedStreak = view.findViewById(R.id.textBestSelectedStreak);

        toggleDailySwitch = view.findViewById(R.id.toggleDailySwitch);
        toggleStreakSwitch = view.findViewById(R.id.toggleStreakSwitch);

        // Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (getActivity() != null) {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("user", Context.MODE_PRIVATE);
            currentUid = prefs.getString("uid", null);
        }

        createNotificationChannel();
        setupSpinners();
        loadSwitchStates();

        if (currentUid != null) {
            loadUserData();
        }

        // Start timer
        setupGlobalTimer();

        toggleDailySwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> saveSwitchState("daily_reminder", isChecked)
        );
        toggleStreakSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> saveSwitchState("streak_warning", isChecked)
        );

        btnRestore.setOnClickListener(v -> restoreStreak());

        return view;
    }

    // ---------------------------
    // NOTIFICATION CHANNEL
    // ---------------------------
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SnapBit Notifications";
            String description = "Reminders for streaks and warnings";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String title, String content) {
        if (!isAdded() || getContext() == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager =
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // ---------------------------
    // TIMER (24H OR 1-MIN DEMO)
    // ---------------------------
    private void setupGlobalTimer() {
        if (!isAdded()) return;

        long durationMillis;
        long totalDayMillis;

        if (DEMO_MODE) {
            // 1-minute "day" buckets based on current time
            long now = System.currentTimeMillis();
            long remainder = now % DAY_WINDOW_MILLIS;
            long millisUntilEnd = DAY_WINDOW_MILLIS - remainder;
            durationMillis = millisUntilEnd;
            totalDayMillis = DAY_WINDOW_MILLIS;
        } else {
            // Real mode: time until midnight
            Calendar nowCal = Calendar.getInstance();
            Calendar midnight = Calendar.getInstance();
            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);
            midnight.add(Calendar.DAY_OF_YEAR, 1);

            durationMillis = midnight.getTimeInMillis() - nowCal.getTimeInMillis();
            totalDayMillis = DAY_WINDOW_MILLIS; // 24 hours
        }

        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded() || getContext() == null) {
                    cancel();
                    return;
                }

                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;

                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                tvStreakTimer.setText(time);

                int progress = (int) ((millisUntilFinished * 100L) / totalDayMillis);
                streakProgressBar.setProgress(progress);

                checkReminders(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                if (!isAdded() || getContext() == null) return;

                tvStreakTimer.setText("00:00:00");
                streakProgressBar.setProgress(0);
                dailyNotified = false;
                streakWarningNotified = false;

                // For demo + real, we do not forcibly change streaks here.
                // Streaks are computed from proofs via HomeFragment.
                setupGlobalTimer();
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

    // ---------------------------
    // REMINDER LOGIC
    // ---------------------------
    private void checkReminders(long millisUntilFinished) {
        if (!isAdded() || getActivity() == null) return;

        SharedPreferences prefs =
                requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean dailyEnabled = prefs.getBoolean("daily_reminder", true);
        boolean streakEnabled = prefs.getBoolean("streak_warning", false);

        boolean maintainedThisWindow = false;

        // In DEMO mode, always allow reminders every minute,
        // so we skip the "already done in this window" check.
        if (!DEMO_MODE && currentUser != null && currentUser.lastStreakUpdate != 0L) {
            long now = System.currentTimeMillis();
            long diff = now - currentUser.lastStreakUpdate;

            // "Same day" = within current window (24h in real)
            maintainedThisWindow = diff >= 0 && diff < DAY_WINDOW_MILLIS;
        }

        // In real mode: If user already did something in this window, no warning needed
        if (!DEMO_MODE && maintainedThisWindow) return;

        if (millisUntilFinished <= WARNING_WINDOW_MILLIS) {

            if (dailyEnabled && !dailyNotified) {
                sendNotification(
                        DEMO_MODE ? "Demo daily reminder" : "Daily reminder",
                        DEMO_MODE
                                ? "Demo: your 1-minute day is about to end."
                                : "A new day to continue your streak!"
                );
                dailyNotified = true;
            }

            if (streakEnabled
                    && !streakWarningNotified
                    && currentUser != null
                    && currentUser.streak > 0) {

                sendNotification(
                        DEMO_MODE ? "Demo streak warning" : "Streak warning",
                        DEMO_MODE
                                ? "Demo: your streak window is about to end!"
                                : "Your streak is about to run out!"
                );
                streakWarningNotified = true;
            }
        }
    }

    // ---------------------------
    // SPINNERS (CATEGORY SELECTION)
    // ---------------------------
    private void setupSpinners() {
        if (getContext() == null) return;

        String[] categories = new String[]{
                "Fitness",
                "Learning",
                "Health",
                "Creativity",
                "Productivity"
        };

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerCategory.setAdapter(catAdapter);
        spinnerBestCategory.setAdapter(catAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSelectedCategoryStreakUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerBestCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateBestCategoryUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateSelectedCategoryStreakUI() {
        if (!isAdded() || spinnerCategory.getSelectedItem() == null) return;

        Context ctx = getContext();
        if (ctx == null) return;

        String selected = spinnerCategory.getSelectedItem().toString();
        int current = getCurrentForCategory(selected);
        int lost = getLostForCategory(selected);

        textSelectedCategoryStreak.setText("Streak: " + current + " days");

        boolean canRestore = (currentUser != null
                && currentUser.restoresLeft > 0
                && lost > 0);

        btnRestore.setEnabled(canRestore);

        btnRestore.setBackgroundTintList(
                ContextCompat.getColorStateList(
                        ctx,
                        canRestore ? android.R.color.holo_green_dark : android.R.color.darker_gray
                )
        );

        if (currentUser != null) {
            restoresLeftView.setText(
                    "Restores left this month: " + currentUser.restoresLeft + " / 3"
            );
        } else {
            restoresLeftView.setText("Restores left this month: 0 / 3");
        }
    }

    private void updateBestCategoryUI() {
        if (!isAdded() || spinnerBestCategory.getSelectedItem() == null) return;
        String selected = spinnerBestCategory.getSelectedItem().toString();
        int best = getBestForCategory(selected);
        textBestSelectedStreak.setText(selected + ": " + best + " days");
    }

    private int getCurrentForCategory(String cat) {
        switch (cat) {
            case "Fitness": return fitnessCurrent;
            case "Learning": return learningCurrent;
            case "Health": return healthCurrent;
            case "Creativity": return creativityCurrent;
            case "Productivity": return productivityCurrent;
        }
        return 0;
    }

    private int getLostForCategory(String cat) {
        switch (cat) {
            case "Fitness": return fitnessLost;
            case "Learning": return learningLost;
            case "Health": return healthLost;
            case "Creativity": return creativityLost;
            case "Productivity": return productivityLost;
        }
        return 0;
    }

    private int getBestForCategory(String cat) {
        switch (cat) {
            case "Fitness": return fitnessBest;
            case "Learning": return learningBest;
            case "Health": return healthBest;
            case "Creativity": return creativityBest;
            case "Productivity": return productivityBest;
        }
        return 0;
    }

    // ---------------------------
    // LOAD USER + CATEGORY STREAKS
    // ---------------------------
    private void loadUserData() {
        mDatabase.child("users").child(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUser = snapshot.getValue(User.class);
                        if (currentUser == null) return;

                        // 1) Sync CURRENT streaks from /users/{uid}
                        //    These are written by HomeFragment's computeStreak()
                        fitnessCurrent = currentUser.fitnessStreak;
                        learningCurrent = currentUser.learningStreak;
                        healthCurrent = currentUser.healthStreak;
                        creativityCurrent = currentUser.creativityStreak;
                        productivityCurrent = currentUser.productivityStreak;

                        // 2) Monthly restore reset
                        checkMonthlyRestoreReset();

                        // 3) Load BEST + LOST per category from /users/{uid}/categoryStreaks
                        loadCategoryStreaks();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadCategoryStreaks() {
        if (currentUid == null) return;

        mDatabase.child("users")
                .child(currentUid)
                .child("categoryStreaks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        // Reset
                        fitnessLost = learningLost = healthLost = creativityLost = productivityLost = 0;
                        fitnessBest = learningBest = healthBest = creativityBest = productivityBest = 0;

                        for (DataSnapshot catSnap : snapshot.getChildren()) {
                            String key = catSnap.getKey();
                            Integer lost = catSnap.child("lostStreak").getValue(Integer.class);
                            Integer best = catSnap.child("bestStreak").getValue(Integer.class);

                            int l = (lost != null) ? lost : 0;
                            int b = (best != null) ? best : 0;

                            // currentFromHome = current streak that HomeFragment computed from proofs
                            int currentFromHome = 0;
                            if ("Fitness".equals(key)) {
                                currentFromHome = fitnessCurrent;
                            } else if ("Learning".equals(key)) {
                                currentFromHome = learningCurrent;
                            } else if ("Health".equals(key)) {
                                currentFromHome = healthCurrent;
                            } else if ("Creativity".equals(key)) {
                                currentFromHome = creativityCurrent;
                            } else if ("Productivity".equals(key)) {
                                currentFromHome = productivityCurrent;
                            }

                            // Lifetime best = max(old best, current streak)
                            int bestFinal = Math.max(b, currentFromHome);

                            // Save into local variables
                            if ("Fitness".equals(key)) {
                                fitnessLost = l;
                                fitnessBest = bestFinal;
                            } else if ("Learning".equals(key)) {
                                learningLost = l;
                                learningBest = bestFinal;
                            } else if ("Health".equals(key)) {
                                healthLost = l;
                                healthBest = bestFinal;
                            } else if ("Creativity".equals(key)) {
                                creativityLost = l;
                                creativityBest = bestFinal;
                            } else if ("Productivity".equals(key)) {
                                productivityLost = l;
                                productivityBest = bestFinal;
                            }

                            // If we improved the best, write it back to Firebase
                            if (bestFinal != b) {
                                catSnap.getRef().child("bestStreak").setValue(bestFinal);
                            }
                        }

                        updateSelectedCategoryStreakUI();
                        updateBestCategoryUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ---------------------------
    // MONTHLY RESET FOR RESTORES
    // ---------------------------
    private void checkMonthlyRestoreReset() {
        if (currentUser == null || currentUid == null) return;

        Calendar now = Calendar.getInstance();
        Calendar lastReset = Calendar.getInstance();
        lastReset.setTimeInMillis(currentUser.lastRestoreReset);

        if (currentUser.lastRestoreReset == 0L
                || now.get(Calendar.MONTH) != lastReset.get(Calendar.MONTH)
                || now.get(Calendar.YEAR) != lastReset.get(Calendar.YEAR)) {

            currentUser.restoresLeft = 3;
            currentUser.lastRestoreReset = now.getTimeInMillis();

            Map<String, Object> updates = new HashMap<>();
            updates.put("restoresLeft", 3);
            updates.put("lastRestoreReset", now.getTimeInMillis());

            mDatabase.child("users").child(currentUid).updateChildren(updates);
        }

        restoresLeftView.setText("Restores left this month: " + currentUser.restoresLeft + " / 3");
    }

    // ---------------------------
    // PER-CATEGORY RESTORE
    // ---------------------------
    private void restoreStreak() {
        if (currentUser == null || currentUid == null) {
            Toast.makeText(getContext(), "User not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.restoresLeft <= 0) {
            Toast.makeText(getContext(), "No restores left this month.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerCategory.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Select a category first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selected = spinnerCategory.getSelectedItem().toString();
        int lost = getLostForCategory(selected);
        int current = getCurrentForCategory(selected);

        if (lost <= 0) {
            Toast.makeText(getContext(), "No lost streak to restore for " + selected + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        int newCurrent = current + lost;
        int newLost = 0;

        setCurrentForCategory(selected, newCurrent);
        setLostForCategory(selected, newLost);

        currentUser.restoresLeft = currentUser.restoresLeft - 1;

        Map<String, Object> catUpdates = new HashMap<>();
        catUpdates.put("currentStreak", newCurrent);
        catUpdates.put("lostStreak", newLost);

        mDatabase.child("users")
                .child(currentUid)
                .child("categoryStreaks")
                .child(selected)
                .updateChildren(catUpdates);

        mDatabase.child("users")
                .child(currentUid)
                .child("restoresLeft")
                .setValue(currentUser.restoresLeft);

        logActivity("Streak Restored",
                "Restored " + lost + " days for " + selected + ".");

        Toast.makeText(getContext(), "Streak restored for " + selected + "!", Toast.LENGTH_SHORT).show();

        updateSelectedCategoryStreakUI();
    }

    private void setCurrentForCategory(String cat, int value) {
        switch (cat) {
            case "Fitness": fitnessCurrent = value; break;
            case "Learning": learningCurrent = value; break;
            case "Health": healthCurrent = value; break;
            case "Creativity": creativityCurrent = value; break;
            case "Productivity": productivityCurrent = value; break;
        }
    }

    private void setLostForCategory(String cat, int value) {
        switch (cat) {
            case "Fitness": fitnessLost = value; break;
            case "Learning": learningLost = value; break;
            case "Health": healthLost = value; break;
            case "Creativity": creativityLost = value; break;
            case "Productivity": productivityLost = value; break;
        }
    }

    private void logActivity(String title, String description) {
        if (currentUid == null) return;

        String key = mDatabase.child("activities")
                .child(currentUid)
                .push()
                .getKey();

        if (key == null) return;

        UserActivity activity =
                new UserActivity(title, description, System.currentTimeMillis());

        mDatabase.child("activities")
                .child(currentUid)
                .child(key)
                .setValue(activity);
    }

    // ---------------------------
    // SWITCHES (SETTINGS)
    // ---------------------------
    private void loadSwitchStates() {
        if (!isAdded() || getActivity() == null) return;

        SharedPreferences prefs =
                requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        toggleDailySwitch.setChecked(prefs.getBoolean("daily_reminder", true));
        toggleStreakSwitch.setChecked(prefs.getBoolean("streak_warning", false));
    }

    private void saveSwitchState(String key, boolean value) {
        if (!isAdded() || getActivity() == null) return;

        SharedPreferences prefs =
                requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        prefs.edit().putBoolean(key, value).apply();

        String label;
        if ("daily_reminder".equals(key)) {
            label = "Daily reminders";
        } else if ("streak_warning".equals(key)) {
            label = "Streak warnings";
        } else {
            label = "Setting";
        }

        Toast.makeText(getContext(),
                label + (value ? " enabled" : " disabled"),
                Toast.LENGTH_SHORT).show();
    }
}
