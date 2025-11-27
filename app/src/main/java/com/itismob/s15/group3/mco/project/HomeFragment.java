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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;

public class HomeFragment extends Fragment {

    // Fixed categories
    private static final String CAT_FITNESS = "Fitness";
    private static final String CAT_LEARNING = "Learning";
    private static final String CAT_HEALTH = "Health";
    private static final String CAT_CREATIVITY = "Creativity";
    private static final String CAT_PRODUCTIVITY = "Productivity";

    // Streak values (now loaded from Firebase)
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

        setupCategoryClicks();
        highlightSelectedCategory();
        updateStreakViews();    // show initial zeros
        setupSubmitButton();
        setupInfoFab(infoFab);

        // Load real streaks from Firebase
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
                    .addToBackStack(null) // Add to backstack
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
        if (getActivity() == null) return;

        String uid = getActivity()
                .getSharedPreferences("user", 0)
                .getString("uid", null);

        if (uid == null) {
            Toast.makeText(getContext(),
                    "User not logged in.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference proofsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
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

                for (DataSnapshot proofSnap : snapshot.getChildren()) {
                    String category = proofSnap.child("category").getValue(String.class);
                    Long ts = proofSnap.child("timestamp").getValue(Long.class);

                    if (category == null || ts == null) continue;

                    String day = dayFormat.format(new Date(ts));

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

                fitnessStreak = computeStreak(fitnessDays);
                learningStreak = computeStreak(learningDays);
                healthStreak = computeStreak(healthDays);
                creativityStreak = computeStreak(creativityDays);
                productivityStreak = computeStreak(productivityDays);

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

    // Count consecutive days up to today where the date is in the set
    private int computeStreak(Set<String> daySet) {
        if (daySet.isEmpty()) return 0;

        Calendar cal = Calendar.getInstance();
        
        // Check today first
        String today = dayFormat.format(cal.getTime());
        
        int streak = 0;
        if (daySet.contains(today)) {
        } else {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String yesterday = dayFormat.format(cal.getTime());
            if (!daySet.contains(yesterday)) {
                return 0; // Streak broken
            }
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
