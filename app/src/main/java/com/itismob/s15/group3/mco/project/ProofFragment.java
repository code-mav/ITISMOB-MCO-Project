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
import android.provider.MediaStore;
import android.util.Base64;
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
    Button submitBtn;
    Button snapDemoBtn;
    Bitmap currentBitmap;

    ActivityResultLauncher<Intent> cameraLauncher;
    ActivityResultLauncher<Intent> galleryLauncher;
    DatabaseReference mDatabase;
    String currentUid;

    public ProofFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_proof, container, false);
        Button capture = v.findViewById(R.id.captureBtn);
        snapDemoBtn = v.findViewById(R.id.snapDemoBtn);
        submitBtn = v.findViewById(R.id.submitProofBtn);
        capturedIv = v.findViewById(R.id.capturedIv);
        
        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        currentUid = prefs.getString("uid", null);

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                currentBitmap = (Bitmap) extras.get("data");
                capturedIv.setImageBitmap(currentBitmap);
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri selectedImage = result.getData().getData();
                try {
                    InputStream imageStream = requireActivity().getContentResolver().openInputStream(selectedImage);
                    currentBitmap = BitmapFactory.decodeStream(imageStream);
                    capturedIv.setImageBitmap(currentBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        capture.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            } else {
                openCamera();
            }
        });
        
        snapDemoBtn.setOnClickListener(view -> openGallery());

        submitBtn.setOnClickListener(view -> submitProof());

        return v;
    }

    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(takePicture);
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openGallery() {
        // This intent opens the document picker which often includes Google Drive integration on Android devices
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }
    
    private void submitProof() {
        if (currentBitmap == null) {
            Toast.makeText(getContext(), "Please capture or select a photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUid == null) {
             Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
             return;
        }

        // 1. Convert bitmap to Base64
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream); // Lower quality for smaller size
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

        // 2. Create proof entry
        long timestamp = System.currentTimeMillis();
        String key = mDatabase.child("users").child(currentUid).child("proofs").push().getKey();
        
        Map<String, Object> proof = new HashMap<>();
        proof.put("category", "Gym"); // Defaulting to 'Gym' for now
        proof.put("title", "Daily Gym Session");
        proof.put("timestamp", timestamp);
        proof.put("imageBase64", encodedImage);

        if (key != null) {
            mDatabase.child("users").child(currentUid).child("proofs").child(key).setValue(proof)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Proof submitted!", Toast.LENGTH_SHORT).show();
                    updateStreakAndLog();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void updateStreakAndLog() {
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long lastStreakUpdate = 0;
                if (snapshot.hasChild("lastStreakUpdate")) {
                    lastStreakUpdate = snapshot.child("lastStreakUpdate").getValue(Long.class);
                }
                int currentStreak = 0;
                if (snapshot.hasChild("streak")) {
                    currentStreak = snapshot.child("streak").getValue(Integer.class);
                }

                Calendar last = Calendar.getInstance();
                last.setTimeInMillis(lastStreakUpdate);
                
                Calendar now = Calendar.getInstance();
                
                boolean sameDay = last.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                  last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

                if (!sameDay) {
                    Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DAY_OF_YEAR, -1);
                    
                    boolean isConsecutive = last.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                                            last.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);
                    
                    // If never updated (0), treat as new streak.
                    if (lastStreakUpdate == 0) isConsecutive = true; 
                    
                    int newStreak = currentStreak + 1;
                    
                    // If not consecutive and not first time, reset
                    if (currentStreak > 0 && !isConsecutive && lastStreakUpdate != 0) {
                        // Streak broken
                        newStreak = 1; 
                        mDatabase.child("users").child(currentUid).child("lostStreak").setValue(currentStreak);
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("streak", newStreak);
                    updates.put("lastStreakUpdate", now.getTimeInMillis());
                    
                    mDatabase.child("users").child(currentUid).updateChildren(updates);
                    
                    logActivity("Streak Increased", "Daily streak reached " + newStreak + " days!");
                }
                
                navigateToGallery();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                navigateToGallery();
            }
        });
    }

    private void logActivity(String title, String description) {
        String key = mDatabase.child("activities").child(currentUid).push().getKey();
        if (key != null) {
             UserActivity activity = new UserActivity(title, description, System.currentTimeMillis());
             mDatabase.child("activities").child(currentUid).child(key).setValue(activity);
        }
    }

    private void navigateToGallery() {
        // Go to Gallery Tab
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new GalleryFragment())
                .addToBackStack(null) 
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
