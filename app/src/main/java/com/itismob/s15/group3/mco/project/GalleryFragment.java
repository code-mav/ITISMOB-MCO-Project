package com.itismob.s15.group3.mco.project;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GalleryFragment extends Fragment {

    private RecyclerView rvPhotos;
    private Spinner spinnerDate, spinnerHabit, spinnerFriend;

    private GalleryAdapter adapter;

    // All proofs from you + friends (raw)
    private final List<GalleryItem> allItems = new ArrayList<>();
    // Currently displayed (after filters)
    private final List<GalleryItem> displayItems = new ArrayList<>();

    private ArrayAdapter<String> dateAdapter;
    private ArrayAdapter<String> habitAdapter;
    private ArrayAdapter<String> friendAdapter;

    private final List<String> dateOptions = new ArrayList<>();
    private final List<String> habitOptions = new ArrayList<>();
    private final List<String> friendOptions = new ArrayList<>();

    private static final String ALL_DATES = "All dates";
    private static final String ALL_HABITS = "All habits";
    private static final String ALL_FRIENDS = "All friends";

    private static final String CAT_FITNESS = "Fitness";
    private static final String CAT_LEARNING = "Learning";
    private static final String CAT_HEALTH = "Health";
    private static final String CAT_CREATIVITY = "Creativity";
    private static final String CAT_PRODUCTIVITY = "Productivity";

    private final SimpleDateFormat dayFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public GalleryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        spinnerDate = view.findViewById(R.id.spinnerDate);
        spinnerHabit = view.findViewById(R.id.spinnerHabit);
        spinnerFriend = view.findViewById(R.id.spinnerFriend);
        rvPhotos = view.findViewById(R.id.rvPhotos);

        String currentUid = requireActivity()
                .getSharedPreferences("user", 0)
                .getString("uid", null);

        // RecyclerView grid setup
        rvPhotos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new GalleryAdapter(displayItems, currentUid, this::confirmDelete);
        rvPhotos.setAdapter(adapter);

        setupSpinners();
        loadUserAndFriendsProofs();

        return view;
    }

    private void confirmDelete(GalleryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo? It will be gone forever.")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem(GalleryItem item) {
        if (item.getKey() == null || item.getUserId() == null) return;

        DatabaseReference proofRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(item.getUserId())
                .child("proofs")
                .child(item.getKey());

        proofRef.removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Photo deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupSpinners() {
        // Initial options
        dateOptions.clear();
        habitOptions.clear();
        friendOptions.clear();

        dateOptions.add(ALL_DATES);
        habitOptions.add(ALL_HABITS);
        
        // Add fixed categories
        habitOptions.add(CAT_FITNESS);
        habitOptions.add(CAT_LEARNING);
        habitOptions.add(CAT_HEALTH);
        habitOptions.add(CAT_CREATIVITY);
        habitOptions.add(CAT_PRODUCTIVITY);
        
        friendOptions.add(ALL_FRIENDS);

        dateAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, dateOptions);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate.setAdapter(dateAdapter);

        habitAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, habitOptions);
        habitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHabit.setAdapter(habitAdapter);

        friendAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, friendOptions);
        friendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFriend.setAdapter(friendAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerDate.setOnItemSelectedListener(listener);
        spinnerHabit.setOnItemSelectedListener(listener);
        spinnerFriend.setOnItemSelectedListener(listener);
    }

    // Load current user, then friends, then their proofs
    private void loadUserAndFriendsProofs() {
        if (getActivity() == null) return;

        String uid = getActivity()
                .getSharedPreferences("user", 0)
                .getString("uid", null);

        if (uid == null) {
            Toast.makeText(getContext(),
                    "User not logged in.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("users");

        // First load current user's friends list and name
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                if (!userSnap.exists()) return;

                String myName = userSnap.child("fullName").getValue(String.class);
                if (myName == null) myName = "You";

                // Collect all user IDs to load: you + friends
                Set<String> idsToLoad = new HashSet<>();
                idsToLoad.add(uid);

                DataSnapshot friendsSnap = userSnap.child("friends");
                for (DataSnapshot f : friendsSnap.getChildren()) {
                    String friendId = f.getKey();
                    if (friendId != null) {
                        idsToLoad.add(friendId);
                    }
                }

                // Attach a real-time listener per user id
                for (String id : idsToLoad) {
                    String overrideName = id.equals(uid) ? "You" : null;
                    attachProofListenerForUser(usersRef, id, overrideName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load friends: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Real-time listener per user (you or friend)
    private void attachProofListenerForUser(DatabaseReference usersRef,
                                            String userId,
                                            @Nullable String overrideName) {
        usersRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                // If user node deleted or null, clear items for this user
                if (!userSnap.exists()) {
                    List<GalleryItem> toRemove = new ArrayList<>();
                    for (GalleryItem item : allItems) {
                        if (userId.equals(item.getUserId())) {
                            toRemove.add(item);
                        }
                    }
                    if (!toRemove.isEmpty()) {
                        allItems.removeAll(toRemove);
                        rebuildFilterOptions();
                        applyFilters();
                    }
                    return;
                }

                String name = overrideName;
                if (name == null) {
                    name = userSnap.child("fullName").getValue(String.class);
                }
                if (name == null) name = "Friend";

                // Remove old items for this user
                List<GalleryItem> toRemove = new ArrayList<>();
                for (GalleryItem item : allItems) {
                    if (userId.equals(item.getUserId())) {
                        toRemove.add(item);
                    }
                }
                allItems.removeAll(toRemove);

                // Add all current proofs for this user
                DataSnapshot proofsSnap = userSnap.child("proofs");
                for (DataSnapshot proofSnap : proofsSnap.getChildren()) {
                    String proofKey = proofSnap.getKey();
                    String category = proofSnap.child("category").getValue(String.class);
                    String title = proofSnap.child("title").getValue(String.class);
                    Long timestampObj = proofSnap.child("timestamp").getValue(Long.class);
                    String imageBase64 = proofSnap.child("imageBase64").getValue(String.class);

                    if (category == null) category = "Unknown";
                    if (title == null) title = "";
                    long timestamp = timestampObj != null ? timestampObj : 0L;

                    Bitmap bitmap = null;
                    if (imageBase64 != null) {
                        try {
                            byte[] bytes = Base64.decode(imageBase64, Base64.DEFAULT);
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    GalleryItem item = new GalleryItem(bitmap, name, category, title, userId, timestamp, proofKey);
                    allItems.add(item);
                }

                // Sort newest first
                Collections.sort(allItems,
                        (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                rebuildFilterOptions();
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load gallery: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rebuildFilterOptions() {
        // Use sets to avoid duplicates
        Set<String> dateSet = new LinkedHashSet<>();
        Set<String> friendSet = new HashSet<>();

        for (GalleryItem item : allItems) {
            // Date by day
            if (item.getTimestamp() > 0) {
                String day = dayFormat.format(new Date(item.getTimestamp()));
                dateSet.add(day);
            }
            friendSet.add(item.getFriendName());
        }

        // Keep previous selections if possible
        String selectedDate = (String) spinnerDate.getSelectedItem();
        String selectedHabit = (String) spinnerHabit.getSelectedItem();
        String selectedFriend = (String) spinnerFriend.getSelectedItem();

        // Update Date options
        List<String> newDates = new ArrayList<>();
        newDates.add(ALL_DATES);
        newDates.addAll(dateSet);
        // Only update adapter if changed to avoid flickering or resetting selection unnecessarily
        if (!newDates.equals(dateOptions)) {
            dateOptions.clear();
            dateOptions.addAll(newDates);
            dateAdapter.notifyDataSetChanged();
             if (selectedDate != null && dateOptions.contains(selectedDate)) {
                spinnerDate.setSelection(dateOptions.indexOf(selectedDate));
            } else {
                spinnerDate.setSelection(0);
            }
        }

        // Habit options are FIXED now, so we don't rebuild them dynamically from items
        // But we ensure the selection is kept
        if (selectedHabit != null && habitOptions.contains(selectedHabit)) {
            spinnerHabit.setSelection(habitOptions.indexOf(selectedHabit));
        } else {
             spinnerHabit.setSelection(0);
        }

        // Update Friend options
        List<String> newFriends = new ArrayList<>();
        newFriends.add(ALL_FRIENDS);
        newFriends.addAll(friendSet);

        if (!newFriends.equals(friendOptions)) {
            friendOptions.clear();
            friendOptions.addAll(newFriends);
            friendAdapter.notifyDataSetChanged();
            if (selectedFriend != null && friendOptions.contains(selectedFriend)) {
                spinnerFriend.setSelection(friendOptions.indexOf(selectedFriend));
            } else {
                spinnerFriend.setSelection(0);
            }
        }
    }

    private void applyFilters() {
        String selectedDate = (String) spinnerDate.getSelectedItem();
        String selectedHabit = (String) spinnerHabit.getSelectedItem();
        String selectedFriend = (String) spinnerFriend.getSelectedItem();

        displayItems.clear();

        for (GalleryItem item : allItems) {
            boolean ok = true;

            // Date filter
            if (selectedDate != null && !selectedDate.equals(ALL_DATES)) {
                if (item.getTimestamp() <= 0) {
                    ok = false;
                } else {
                    String itemDay = dayFormat.format(new Date(item.getTimestamp()));
                    if (!itemDay.equals(selectedDate)) ok = false;
                }
            }

            // Habit filter
            if (ok && selectedHabit != null && !selectedHabit.equals(ALL_HABITS)) {
                if (!item.getHabitType().equals(selectedHabit)) ok = false;
            }

            // Friend filter
            if (ok && selectedFriend != null && !selectedFriend.equals(ALL_FRIENDS)) {
                if (!item.getFriendName().equals(selectedFriend)) ok = false;
            }

            if (ok) {
                displayItems.add(item);
            }
        }

        adapter.notifyDataSetChanged();
    }
}
