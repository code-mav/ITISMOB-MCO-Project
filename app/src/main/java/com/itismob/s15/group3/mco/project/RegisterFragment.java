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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.itismob.s15.group3.mco.project.models.User;
import com.itismob.s15.group3.mco.project.utils.HashUtil;

public class RegisterFragment extends Fragment {

    EditText fullNameEt, birthdayEt, emailEt, passwordEt;
    Button registerBtn;

    public RegisterFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_register, container, false);

        fullNameEt = v.findViewById(R.id.fullNameEt);
        birthdayEt = v.findViewById(R.id.birthdayEt);
        emailEt = v.findViewById(R.id.emailEt);
        passwordEt = v.findViewById(R.id.passwordEt);
        registerBtn = v.findViewById(R.id.registerBtn);

        registerBtn.setOnClickListener(view -> registerUser());

        return v;
    }

    private void registerUser() {
        String name = fullNameEt.getText().toString();
        String birthday = birthdayEt.getText().toString();
        String email = emailEt.getText().toString();
        String pass = HashUtil.hash(passwordEt.getText().toString());

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

        String userId = ref.push().getKey();

        User user = new User(name, birthday, email, pass);

        ref.child(userId).setValue(user)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "Registered!", Toast.LENGTH_SHORT).show();

                    // Go to Login screen
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new LoginFragment())
                            .commit();
                });
    }
}
