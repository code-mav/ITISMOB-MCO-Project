package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class LoginFragment extends Fragment {
    public LoginFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        Button loginBtn = v.findViewById(R.id.loginBtn);
        Button createAccount = v.findViewById(R.id.createAccountBtn);

        loginBtn.setOnClickListener(view -> {
            // "Login" -> show HomeFragment and bottom nav
            ((MainActivity) requireActivity()).setBottomNavVisible(true);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        });

        createAccount.setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RegisterFragment())
                    .commit();
        });

        return v;
    }
}
