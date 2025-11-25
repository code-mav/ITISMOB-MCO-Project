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

public class HomeFragment extends Fragment {

    // Fixed categories
    private static final String CAT_FITNESS = "Fitness";
    private static final String CAT_LEARNING = "Learning";
    private static final String CAT_HEALTH = "Health";
    private static final String CAT_CREATIVITY = "Creativity";
    private static final String CAT_PRODUCTIVITY = "Productivity";

    // TODO: replace these with real streak values from backend or database
    private int fitnessStreak = 3;
    private int learningStreak = 5;
    private int healthStreak = 0;
    private int creativityStreak = 2;
    private int productivityStreak = 1;

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
        updateStreakViews();
        setupSubmitButton();
        setupInfoFab(infoFab);

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

        // Default highlight
        highlightSelectedCategory();
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
}
