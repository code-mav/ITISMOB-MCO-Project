package com.itismob.s15.group3.mco.project;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class ProfileFragment extends Fragment {

    private TextView nameTv, bioTv;
    private ImageView profileIv;

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        nameTv = v.findViewById(R.id.nameTextView);
        bioTv = v.findViewById(R.id.bioTextView);
        profileIv = v.findViewById(R.id.profileImageView);
        
        // Recent activities are hidden via XML (visibility="gone")

        loadProfile();
        loadProfilePic();

        // Edit button navigates to EditProfileFragment
        Button edit = v.findViewById(R.id.editProfileBtn);
        edit.setOnClickListener(view -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new EditProfileFragment())
                .commit());

        // Logout button
        Button logoutBtn = v.findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(view -> new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    requireActivity().getSharedPreferences("user", 0).edit().clear().apply();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new LoginFragment())
                            .commit();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setBottomNavVisible(false);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show());

        return v;
    }

    private void loadProfile() {
        if (getActivity() == null) return;
        String email = requireActivity()
                .getSharedPreferences("user", 0)
                .getString("email", null);
        if (email == null) return;

        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String name = s.child("fullName").getValue(String.class);
                            String bio = s.child("bio").getValue(String.class);
                            nameTv.setText(name != null ? name : "");
                            bioTv.setText(bio != null ? bio : "");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadProfilePic() {
        if (getActivity() == null) return;
        String uid = requireActivity()
                .getSharedPreferences("user", 0)
                .getString("uid", null);
        if (uid == null) return;

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("profile_pics/" + uid + ".jpg");

        storageRef.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get()
                                .load(uri)
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(profileIv);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        profileIv.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                });
    }
}
