package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;

public class ProfileFragment extends Fragment {
    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        // Edit Profile Button
        Button edit = v.findViewById(R.id.editProfileBtn);
        edit.setOnClickListener(view -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new EditProfileFragment())
                .commit());

        // Logout Button
        Button logoutBtn = v.findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new LoginFragment())
                                .commit();

                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).setBottomNavVisible(false);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return v;
    }
}
