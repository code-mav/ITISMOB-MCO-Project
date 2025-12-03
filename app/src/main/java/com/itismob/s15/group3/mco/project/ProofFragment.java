package com.itismob.s15.group3.mco.project;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itismob.s15.group3.mco.project.models.UserActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ProofFragment extends Fragment {

    private static final int REQ_CAMERA = 1001;

    ImageView capturedIv;
    Button submitBtn, snapDemoBtn;

    Bitmap currentBitmap;

    ActivityResultLauncher<Void> cameraPreviewLauncher;
    ActivityResultLauncher<Intent> galleryLauncher;

    DatabaseReference mDatabase;
    String currentUid;

    String category = "Fitness";
    String title = "Daily Streak";

    public ProofFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get arguments
        if (getArguments() != null) {
            category = getArguments().getString("category", "Fitness");
            title = getArguments().getString("title", "Daily Streak");
        }

        setupCameraLauncher();
        setupGalleryLauncher();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_proof, container, false);

        Button captureBtn = v.findViewById(R.id.captureBtn);
        snapDemoBtn = v.findViewById(R.id.snapDemoBtn);
        submitBtn = v.findViewById(R.id.submitProofBtn);
        capturedIv = v.findViewById(R.id.capturedIv);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        currentUid = prefs.getString("uid", null);

        captureBtn.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            } else {
                openCamera();
            }
        });

        snapDemoBtn.setOnClickListener(view -> openGallery());
        submitBtn.setOnClickListener(view -> submitProof());

        return v;
    }

    private void setupCameraLauncher() {
        cameraPreviewLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        currentBitmap = bitmap;
                        capturedIv.setImageBitmap(bitmap);
                    } else {
                        Toast.makeText(getContext(),
                                "Camera returned null.\n(Enable emulator camera in AVD settings)",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void openCamera() {
        cameraPreviewLauncher.launch(null);
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK &&
                            result.getData() != null) {

                        Uri selectedImage = result.getData().getData();

                        try {
                            InputStream stream = requireActivity()
                                    .getContentResolver()
                                    .openInputStream(selectedImage);

                            currentBitmap = BitmapFactory.decodeStream(stream);
                            capturedIv.setImageBitmap(currentBitmap);
                        } catch (Exception e) {
                            Toast.makeText(getContext(),
                                    "Failed to load image",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // -------------------------------------------------------
    // SUBMIT PROOF
    // -------------------------------------------------------
    private void submitProof() {

        if (currentBitmap == null) {
            Toast.makeText(getContext(), "Take or select a photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUid == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        String encodedImage = android.util.Base64.encodeToString(
                baos.toByteArray(), android.util.Base64.DEFAULT);

        long timestamp = System.currentTimeMillis();

        String key = mDatabase.child("users")
                .child(currentUid)
                .child("proofs")
                .push()
                .getKey();

        Map<String, Object> proof = new HashMap<>();
        proof.put("category", category);
        proof.put("title", title);
        proof.put("timestamp", timestamp);
        proof.put("imageBase64", encodedImage);

        mDatabase.child("users")
                .child(currentUid)
                .child("proofs")
                .child(key)
                .setValue(proof)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Submitted!", Toast.LENGTH_SHORT).show();
                    updateStreakAndLog();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateStreakAndLog() {

        mDatabase.child("users").child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        long lastUpdate = snapshot.child("lastStreakUpdate").getValue(Long.class) != null
                                ? snapshot.child("lastStreakUpdate").getValue(Long.class)
                                : 0;

                        int streak = snapshot.child("streak").getValue(Integer.class) != null
                                ? snapshot.child("streak").getValue(Integer.class)
                                : 0;

                        long now = System.currentTimeMillis();

                        // 1 minute = 60,000 milliseconds
                        long oneMinute = 60 * 1000;

                        int newStreak;

                        if (lastUpdate == 0) {
                            // First ever submission
                            newStreak = 1;

                        } else if (now - lastUpdate <= oneMinute) {
                            // Within 1 minute -> consecutive!
                            newStreak = streak + 1;

                        } else {
                            // More than 1 minute -> streak broken
                            if (streak > 0) {
                                mDatabase.child("users").child(currentUid)
                                        .child("lostStreak").setValue(streak);
                            }
                            newStreak = 1;
                        }

                        // Save new streak + update timestamp
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("streak", newStreak);
                        updates.put("lastStreakUpdate", now);

                        mDatabase.child("users").child(currentUid).updateChildren(updates);

                        // Log activity
                        logActivity("Streak Updated",
                                "Your streak is now " + newStreak + " steps.");

                        navigateToGallery();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        navigateToGallery();
                    }
                });
    }


    private void logActivity(String title, String desc) {
        String key = mDatabase.child("activities")
                .child(currentUid)
                .push()
                .getKey();

        UserActivity activity = new UserActivity(title, desc, System.currentTimeMillis());
        mDatabase.child("activities").child(currentUid).child(key).setValue(activity);
    }

    private void navigateToGallery() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new GalleryFragment())
                .addToBackStack(null)
                .commit();
    }
}
