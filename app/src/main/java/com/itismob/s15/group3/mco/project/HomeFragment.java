package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeFragment extends Fragment {
    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        Button createHabit = v.findViewById(R.id.createHabitBtn);
        Button submitProof = v.findViewById(R.id.submitProofBtn);
        FloatingActionButton infoFab = v.findViewById(R.id.infoFab);

        // Navigate to Create Habit
        createHabit.setOnClickListener(view -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CreateHabitFragment())
                .commit());

        // Navigate to Proof Submission
        submitProof.setOnClickListener(view -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ProofFragment())
                .commit());

        // Floating Info Button - About & Tutorial Popup
        infoFab.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("About SnapBit")
                    .setMessage("SnapBit is an application designed for individuals who want to build and maintain meaningful habits that contribute to personal growth. It encourages consistency by tracking streaks and promoting accountability through a connected community of friends.\n\n📘 How to Use SnapBit:\n1️⃣ Create a Habit – Tap 'Create Habit' to start tracking something you want to build.\n2️⃣ Submit Proof – Snap a live photo as proof.\n3️⃣ Track Your Streak – View progress on the Dashboard.\n4️⃣ Restore Missed Days – Use monthly tokens to restore streaks.\n\n💡 Tip: All streak timers reset daily at midnight. Don’t forget to submit your proof before the timer runs out!")
                    .setPositiveButton("Got it", null)
                    .show();
        });

        return v;
    }
}
