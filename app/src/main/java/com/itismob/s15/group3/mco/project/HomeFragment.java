package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

public class HomeFragment extends Fragment {

    // Match with DashboardFragment + ProofFragment
    private static final boolean DEMO_MODE = false; // true = 1-minute window demo, false = real 24h
    private static final long DAY_WINDOW_MILLIS =
            DEMO_MODE ? 60_000L : 24L * 60L * 60L * 1000L;

    // Fixed categories
    private static final String CAT_FITNESS = "Fitness";
    private static final String CAT_LEARNING = "Learning";
    private static final String CAT_HEALTH = "Health";
    private static final String CAT_CREATIVITY = "Creativity";
    private static final String CAT_PRODUCTIVITY = "Productivity";

    // Streak values (now loaded from Firebase and used to sync /users node)
    private int fitnessStreak = 0;
    private int learningStreak = 0;
    private int healthStreak = 0;
    private int creativityStreak = 0;
    private int productivityStreak = 0;

    private LinearLayout layoutCatFitness;
    private LinearLayout layoutCatLearning;
    private LinearLayout layoutCatHealth;
    private LinearLayout layoutCatCreativity;
    private LinearLayout layoutCatProductivity;

    private TextView textFitnessStreak;
    private TextView textLearningStreak;
    private TextView textHealthStreak;
    private TextView textCreativityStreak;
    private TextView textProductivityStreak;

    private EditText titleEditText;
    private Button submitProofBtn;

    private String selectedCategory = CAT_FITNESS;

    private final SimpleDateFormat dayFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private DatabaseReference mDatabase;
    private String currentUid;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // Category layouts
        layoutCatFitness = v.findViewById(R.id.layout_cat_fitness);
        layoutCatLearning = v.findViewById(R.id.layout_cat_learning);
        layoutCatHealth = v.findViewById(R.id.layout_cat_health);
        layoutCatCreativity = v.findViewById(R.id.layout_cat_creativity);
        layoutCatProductivity = v.findViewById(R.id.layout_cat_productivity);

        // Streak text views
        textFitnessStreak = v.findViewById(R.id.text_cat_fitness_streak);
        textLearningStreak = v.findViewById(R.id.text_cat_learning_streak);
        textHealthStreak = v.findViewById(R.id.text_cat_health_streak);
        textCreativityStreak = v.findViewById(R.id.text_cat_creativity_streak);
        textProductivityStreak = v.findViewById(R.id.text_cat_productivity_streak);

        // Title input and button
        titleEditText = v.findViewById(R.id.edittext_title);
        submitProofBtn = v.findViewById(R.id.submitProofBtn);
        FloatingActionButton infoFab = v.findViewById(R.id.infoFab);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (getActivity() != null) {
            currentUid = getActivity()
                    .getSharedPreferences("user", 0)
                    .getString("uid", null);
        }

        setupCategoryClicks();
        highlightSelectedCategory();
        updateStreakViews();    // show initial zeros
        setupSubmitButton();
        setupInfoFab(infoFab);

        // Load streaks from Firebase
        loadStreaksFromFirebase();

        return v;
    }

    private void setupCategoryClicks() {
        View.OnClickListener listener = view -> {
            int id = view.getId();
            if (id == R.id.layout_cat_fitness) {
                selectedCategory = CAT_FITNESS;
            } else if (id == R.id.layout_cat_learning) {
                selectedCategory = CAT_LEARNING;
            } else if (id == R.id.layout_cat_health) {
                selectedCategory = CAT_HEALTH;
            } else if (id == R.id.layout_cat_creativity) {
                selectedCategory = CAT_CREATIVITY;
            } else if (id == R.id.layout_cat_productivity) {
                selectedCategory = CAT_PRODUCTIVITY;
            }
            highlightSelectedCategory();
        };

        layoutCatFitness.setOnClickListener(listener);
        layoutCatLearning.setOnClickListener(listener);
        layoutCatHealth.setOnClickListener(listener);
        layoutCatCreativity.setOnClickListener(listener);
        layoutCatProductivity.setOnClickListener(listener);
    }

    private void highlightSelectedCategory() {
        // Simple visual feedback using alpha
        layoutCatFitness.setAlpha(selectedCategory.equals(CAT_FITNESS) ? 1.0f : 0.7f);
        layoutCatLearning.setAlpha(selectedCategory.equals(CAT_LEARNING) ? 1.0f : 0.7f);
        layoutCatHealth.setAlpha(selectedCategory.equals(CAT_HEALTH) ? 1.0f : 0.7f);
        layoutCatCreativity.setAlpha(selectedCategory.equals(CAT_CREATIVITY) ? 1.0f : 0.7f);
        layoutCatProductivity.setAlpha(selectedCategory.equals(CAT_PRODUCTIVITY) ? 1.0f : 0.7f);
    }

    private void updateStreakViews() {
        textFitnessStreak.setText("Streak: " + fitnessStreak);
        textLearningStreak.setText("Streak: " + learningStreak);
        textHealthStreak.setText("Streak: " + healthStreak);
        textCreativityStreak.setText("Streak: " + creativityStreak);
        textProductivityStreak.setText("Streak: " + productivityStreak);
    }

    private void setupSubmitButton() {
        submitProofBtn.setOnClickListener(view -> {
            String title = titleEditText.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please enter a title before submitting proof.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass category + title into ProofFragment
            ProofFragment proofFragment = new ProofFragment();
            Bundle args = new Bundle();
            args.putString("category", selectedCategory);
            args.putString("title", title);
            proofFragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, proofFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupInfoFab(FloatingActionButton infoFab) {
        infoFab.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("About SnapBit")
                    .setMessage("SnapBit helps you stay consistent with photo-based streaks.\n\n"
                            + "1. Pick a category.\n"
                            + "2. Enter a custom title.\n"
                            + "3. Submit your daily photo.\n\n"
                            + "Your streak resets every midnight, so do not miss a day!")
                    .setPositiveButton("Got it", null)
                    .show();
        });
    }

    private void loadStreaksFromFirebase() {
        if (currentUid == null) {
            Toast.makeText(getContext(),
                    "User not logged in.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // DEMO MODE: Just mirror what /users already has so everything matches
        if (DEMO_MODE) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUid);

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer fit = snapshot.child("fitnessStreak").getValue(Integer.class);
                    Integer learn = snapshot.child("learningStreak").getValue(Integer.class);
                    Integer health = snapshot.child("healthStreak").getValue(Integer.class);
                    Integer creat = snapshot.child("creativityStreak").getValue(Integer.class);
                    Integer prod = snapshot.child("productivityStreak").getValue(Integer.class);

                    fitnessStreak = (fit != null) ? fit : 0;
                    learningStreak = (learn != null) ? learn : 0;
                    healthStreak = (health != null) ? health : 0;
                    creativityStreak = (creat != null) ? creat : 0;
                    productivityStreak = (prod != null) ? prod : 0;

                    updateStreakViews();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(),
                            "Failed to load streaks: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

            return; // Skip proofs-based computation in demo mode
        }

        // REAL MODE: original logic based on proof timestamps per day
        DatabaseReference proofsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUid)
                .child("proofs");

        proofsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // Sets of days (yyyy-MM-dd) where there is at least one proof for that category
                Set<String> fitnessDays = new HashSet<>();
                Set<String> learningDays = new HashSet<>();
                Set<String> healthDays = new HashSet<>();
                Set<String> creativityDays = new HashSet<>();
                Set<String> productivityDays = new HashSet<>();

                // For calculating total valid days regardless of category
                Set<String> allValidDays = new HashSet<>();

                for (DataSnapshot proofSnap : snapshot.getChildren()) {
                    String category = proofSnap.child("category").getValue(String.class);
                    Long ts = proofSnap.child("timestamp").getValue(Long.class);

                    if (category == null || ts == null) continue;

                    String day = dayFormat.format(new Date(ts));
                    allValidDays.add(day);

                    switch (category) {
                        case CAT_FITNESS:
                            fitnessDays.add(day);
                            break;
                        case CAT_LEARNING:
                            learningDays.add(day);
                            break;
                        case CAT_HEALTH:
                            healthDays.add(day);
                            break;
                        case CAT_CREATIVITY:
                            creativityDays.add(day);
                            break;
                        case CAT_PRODUCTIVITY:
                            productivityDays.add(day);
                            break;
                    }
                }

                // Compute per-category streaks
                fitnessStreak = computeStreak(fitnessDays);
                learningStreak = computeStreak(learningDays);
                healthStreak = computeStreak(healthDays);
                creativityStreak = computeStreak(creativityDays);
                productivityStreak = computeStreak(productivityDays);

                // Global total streak = "as long as you uploaded something
                // in any category on that day"
                int totalStreak = computeStreak(allValidDays);

                // Sync into /users/{uid} so Dashboard + FriendsLeaderboard can read
                updateTotalStreakOnDashboard(totalStreak);

                // Update UI
                updateStreakViews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load streaks: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotalStreakOnDashboard(int calculatedStreak) {
        if (currentUid == null) return;

        // Build a single update map for Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("streak", calculatedStreak);
        updates.put("fitnessStreak", fitnessStreak);
        updates.put("learningStreak", learningStreak);
        updates.put("healthStreak", healthStreak);
        updates.put("creativityStreak", creativityStreak);
        updates.put("productivityStreak", productivityStreak);

        // We let ProofFragment handle lastStreakUpdate when a proof is submitted,
        // so we do not overwrite it here.

        mDatabase.child("users")
                .child(currentUid)
                .updateChildren(updates);
    }

    // Count consecutive days up to today where the date is in the set
    private int computeStreak(Set<String> daySet) {
        if (daySet.isEmpty()) return 0;

        Calendar cal = Calendar.getInstance();

        String today = dayFormat.format(cal.getTime());
        int streak = 0;

        // If today is present, we start the streak from today.
        // If not, we check from yesterday. If yesterday is missing too, streak is 0.
        boolean streakActiveToday = daySet.contains(today);

        if (!streakActiveToday) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String yesterday = dayFormat.format(cal.getTime());
            if (!daySet.contains(yesterday)) {
                return 0; // Broken
            }
        }

        // Reset baseline to today or yesterday depending on streakActiveToday
        cal = Calendar.getInstance();
        if (!streakActiveToday) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        while (true) {
            String day = dayFormat.format(cal.getTime());
            if (daySet.contains(day)) {
                streak++;
                cal.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }

        return streak;
    }
}
