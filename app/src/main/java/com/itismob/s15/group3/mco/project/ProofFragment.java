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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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
import java.util.HashMap;
import java.util.Map;

public class ProofFragment extends Fragment {

    private static final int REQ_CAMERA = 1001;

    // Match DashboardFragment + HomeFragment
    // true  = 1-minute window demo; false = 24-hour window
    private static final boolean DEMO_MODE = false;
    private static final long DAY_WINDOW_MILLIS =
            DEMO_MODE ? 60_000L : 24L * 60L * 60L * 1000L;

    private ImageView capturedIv;
    private Button submitBtn, snapDemoBtn;

    private Bitmap currentBitmap;

    private ActivityResultLauncher<Void> cameraPreviewLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private DatabaseReference mDatabase;
    private String currentUid;

    private String category = "Fitness";
    private String title = "Daily Streak";

    public ProofFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get arguments from HomeFragment
        if (getArguments() != null) {
            category = getArguments().getString("category", "Fitness");
            title = getArguments().getString("title", "Daily Streak");
        }

        setupCameraLauncher();
        setupGalleryLauncher();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_proof, container, false);

        Button captureBtn = v.findViewById(R.id.captureBtn);
        snapDemoBtn = v.findViewById(R.id.snapDemoBtn);
        submitBtn = v.findViewById(R.id.submitProofBtn);
        capturedIv = v.findViewById(R.id.capturedIv);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        FragmentActivity activity = getActivity();
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences("user", Context.MODE_PRIVATE);
            currentUid = prefs.getString("uid", null);
        }

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

    // -------------------------------------------------------
    // CAMERA + GALLERY
    // -------------------------------------------------------

    private void setupCameraLauncher() {
        cameraPreviewLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (!isAdded()) return;

                    if (bitmap != null) {
                        currentBitmap = bitmap;
                        capturedIv.setImageBitmap(bitmap);
                    } else if (getContext() != null) {
                        Toast.makeText(
                                getContext(),
                                "Camera returned null.\n(Enable emulator camera in AVD settings)",
                                Toast.LENGTH_LONG
                        ).show();
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
                    if (!isAdded()) return;

                    if (result.getResultCode() == Activity.RESULT_OK &&
                            result.getData() != null) {

                        Uri selectedImage = result.getData().getData();

                        try {
                            FragmentActivity activity = getActivity();
                            if (activity == null) return;

                            InputStream stream = activity
                                    .getContentResolver()
                                    .openInputStream(selectedImage);

                            currentBitmap = BitmapFactory.decodeStream(stream);
                            capturedIv.setImageBitmap(currentBitmap);
                        } catch (Exception e) {
                            if (getContext() != null) {
                                Toast.makeText(
                                        getContext(),
                                        "Failed to load image",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
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
            if (getContext() != null) {
                Toast.makeText(getContext(), "Take or select a photo first", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (currentUid == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Convert bitmap to Base64
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

        if (key == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to create proof key", Toast.LENGTH_SHORT).show();
            }
            return;
        }

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
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(getContext(), "Submitted!", Toast.LENGTH_SHORT).show();
                    updateCategoryStreakAndLog();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(
                            getContext(),
                            "Error saving proof: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // -------------------------------------------------------
    // PER-CATEGORY STREAK LOGIC (1 min or 24h window)
    // -------------------------------------------------------
    private void updateCategoryStreakAndLog() {
        if (currentUid == null) {
            navigateToGallerySafe();
            return;
        }

        DatabaseReference categoryRef = mDatabase.child("users")
                .child(currentUid)
                .child("categoryStreaks")
                .child(category);

        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                long now = System.currentTimeMillis();
                long window = DAY_WINDOW_MILLIS; // 1 min in demo, 24h in real

                Long lastUpdate = snapshot.child("lastUpdateMillis").getValue(Long.class);
                Integer currentStreak = snapshot.child("currentStreak").getValue(Integer.class);
                Integer lostStreak = snapshot.child("lostStreak").getValue(Integer.class);
                Integer bestStreak = snapshot.child("bestStreak").getValue(Integer.class);

                if (currentStreak == null) currentStreak = 0;
                if (lostStreak == null) lostStreak = 0;
                if (bestStreak == null) bestStreak = 0;
                if (lastUpdate == null) lastUpdate = 0L;

                int newCurrentStreak;
                int newLostStreak = lostStreak;

                if (lastUpdate == 0L) {
                    // First ever submission for this category
                    newCurrentStreak = 1;
                    newLostStreak = 0;
                } else if (now - lastUpdate <= window) {
                    // Within the current window → streak continues
                    newCurrentStreak = currentStreak + 1;
                } else {
                    // Outside the window → streak broken, remember lost streak
                    if (currentStreak > 0) {
                        newLostStreak = currentStreak;
                    }
                    newCurrentStreak = 1;
                }

                int newBestStreak = Math.max(bestStreak, newCurrentStreak);

                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStreak", newCurrentStreak);
                updates.put("lostStreak", newLostStreak);
                updates.put("bestStreak", newBestStreak);
                updates.put("lastUpdateMillis", now);

                categoryRef.updateChildren(updates);

                // Also update /users/{uid}/lastStreakUpdate for Dashboard reminders
                mDatabase.child("users")
                        .child(currentUid)
                        .child("lastStreakUpdate")
                        .setValue(now);

                // Log activity
                logActivity(
                        "Streak Updated",
                        category + " streak is now " + newCurrentStreak + " days."
                );

                navigateToGallerySafe();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                navigateToGallerySafe();
            }
        });
    }

    private void logActivity(String title, String desc) {
        if (currentUid == null) return;

        String key = mDatabase.child("activities")
                .child(currentUid)
                .push()
                .getKey();

        if (key == null) return;

        UserActivity activity =
                new UserActivity(title, desc, System.currentTimeMillis());

        mDatabase.child("activities")
                .child(currentUid)
                .child(key)
                .setValue(activity);
    }

    private void navigateToGallerySafe() {
        if (!isAdded()) return;

        FragmentActivity activity = getActivity();
        if (activity == null) return;

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new GalleryFragment())
                .addToBackStack(null)
                .commit();
    }
}
