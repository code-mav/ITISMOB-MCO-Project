package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itismob.s15.group3.mco.project.utils.HashUtil;

public class LoginFragment extends Fragment {

    EditText emailEt, passwordEt;
    Button loginBtn, createAccountBtn;

    public LoginFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_login, container, false);

        emailEt = v.findViewById(R.id.emailEt);
        passwordEt = v.findViewById(R.id.passwordEt);
        loginBtn = v.findViewById(R.id.loginBtn);
        createAccountBtn = v.findViewById(R.id.createAccountBtn);

        loginBtn.setOnClickListener(view -> loginUser());

        createAccountBtn.setOnClickListener(view -> requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new RegisterFragment())
                .commit());

        return v;
    }

    private void loginUser() {
        String email = emailEt.getText().toString().trim();
        String pass = passwordEt.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(getContext(), "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        boolean found = false;
                        String uid = null;

                        for (DataSnapshot s : snapshot.getChildren()) {
                            String dbEmail = s.child("email").getValue(String.class);
                            String dbPass = s.child("password").getValue(String.class);
                            String hashedPass = HashUtil.hash(pass);

                            if (email.equals(dbEmail) && hashedPass.equals(dbPass)) {
                                found = true;
                                uid = s.getKey(); // save the UID
                                break;
                            }
                        }

                        if (found && uid != null) {
                            Toast.makeText(getContext(), "Login Successful!", Toast.LENGTH_SHORT).show();

                            requireActivity()
                                    .getSharedPreferences("user", 0)
                                    .edit()
                                    .putString("email", email)
                                    .putString("uid", uid)
                                    .apply();

                            // Show bottom nav
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).setBottomNavVisible(true);
                            }

                            // Go to Home
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new HomeFragment())
                                    .commit();

                        } else {
                            Toast.makeText(getContext(), "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }
}
