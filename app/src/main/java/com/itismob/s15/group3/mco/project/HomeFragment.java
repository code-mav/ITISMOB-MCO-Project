package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class HomeFragment extends Fragment {
    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        Button createHabit = v.findViewById(R.id.createHabitBtn);
        Button submitProof = v.findViewById(R.id.submitProofBtn);

        createHabit.setOnClickListener(view -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CreateHabitFragment())
                .commit());

        submitProof.setOnClickListener(view -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ProofFragment())
                .commit());

        return v;
    }
}

