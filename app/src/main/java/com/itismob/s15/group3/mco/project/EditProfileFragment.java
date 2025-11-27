package com.itismob.s15.group3.mco.project;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class EditProfileFragment extends Fragment {

    private EditText nameEt, bioEt;
    private Button saveBtn;
    private ImageView profileIv;

    private Uri selectedImageUri;

    private ActivityResultLauncher<String> pickImageLauncher;

    public EditProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        profileIv = v.findViewById(R.id.profileImageView);
        nameEt = v.findViewById(R.id.editNameEt);
        bioEt = v.findViewById(R.id.editBioEt);
        saveBtn = v.findViewById(R.id.saveProfileBtn);

        // Load current profile data
        loadExistingProfile();
        loadProfilePicture();

        // Initialize image picker
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        profileIv.setImageURI(uri); // show immediately
                        uploadProfilePic(uri);
                    }
                }
        );

        profileIv.setOnClickListener(view -> pickImageLauncher.launch("image/*"));

        saveBtn.setOnClickListener(view -> saveProfileChanges());

        return v;
    }

    private void loadExistingProfile() {
        String email = requireActivity()
                .getSharedPreferences("user", 0)
                .getString("email", null);
        if (email == null) return;

        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot s : snapshot.getChildren()) {
                            if (email.equals(s.child("email").getValue(String.class))) {
                                String name = s.child("fullName").getValue(String.class);
                                String bio = s.child("bio").getValue(String.class);

                                nameEt.setText(name != null ? name : "");
                                bioEt.setText(bio != null ? bio : "");
                                break;
                            }
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadProfilePicture() {
        String uid = requireActivity()
                .getSharedPreferences("user", 0)
                .getString("uid", null);
        if (uid == null) return;

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("profile_pics/" + uid + ".jpg");

        storageRef.getDownloadUrl()
                .addOnSuccessListener(uri ->
                        Picasso.get()
                                .load(uri)
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(profileIv))
                .addOnFailureListener(e ->
                        profileIv.setImageResource(android.R.drawable.sym_def_app_icon));
    }

    private void uploadProfilePic(Uri imageUri) {
        String uid = requireActivity()
                .getSharedPreferences("user", 0)
                .getString("uid", null);
        if (uid == null) return;

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("profile_pics/" + uid + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri ->
                                Picasso.get()
                                        .load(uri)
                                        .placeholder(android.R.drawable.sym_def_app_icon)
                                        .into(profileIv))
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileChanges() {
        String newName = nameEt.getText().toString().trim();
        String newBio = bioEt.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = requireActivity()
                .getSharedPreferences("user", 0)
                .getString("email", null);
        if (email == null) return;

        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot s : snapshot.getChildren()) {
                            if (email.equals(s.child("email").getValue(String.class))) {
                                s.getRef().child("fullName").setValue(newName);
                                s.getRef().child("bio").setValue(newBio);

                                Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();

                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, new ProfileFragment())
                                        .commit();
                                return;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
