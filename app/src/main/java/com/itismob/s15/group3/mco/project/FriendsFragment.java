package com.itismob.s15.group3.mco.project;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itismob.s15.group3.mco.project.models.User;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    private EditText searchEt;
    private RecyclerView requestsRv, friendsRv, searchRv;

    private UserAdapter requestsAdapter;
    private UserAdapter friendsAdapter;
    private UserAdapter searchAdapter;

    private FriendsManager friendsManager;
    private String currentUid;

    public FriendsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        // Current UID
        currentUid = requireActivity().getSharedPreferences("user", 0)
                .getString("uid", null);

        friendsManager = new FriendsManager(currentUid);

        // UI
        searchEt = view.findViewById(R.id.searchFriendsEt);
        requestsRv = view.findViewById(R.id.requestsRecycler);
        friendsRv = view.findViewById(R.id.friendsRecycler);
        searchRv = view.findViewById(R.id.searchRecycler);

        requestsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        searchRv.setLayoutManager(new LinearLayoutManager(getContext()));

        // -------------------------
        // Adapters
        // -------------------------
        requestsAdapter = new UserAdapter(getContext(), new ArrayList<>(),
                UserAdapter.Mode.REQUESTS, userListener);
        friendsAdapter = new UserAdapter(getContext(), new ArrayList<>(),
                UserAdapter.Mode.FRIENDS, userListener);
        searchAdapter = new UserAdapter(getContext(), new ArrayList<>(),
                UserAdapter.Mode.SEARCH, userListener);

        requestsRv.setAdapter(requestsAdapter);
        friendsRv.setAdapter(friendsAdapter);
        searchRv.setAdapter(searchAdapter);

        loadFriendRequests();
        loadFriendsList();
        setupSearch();

        return view;
    }

    // -------------------------
// Load friend requests
// -------------------------
    private void loadFriendRequests() {
        friendsManager.listenFriendRequests(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> requests = new ArrayList<>();

                for (DataSnapshot s : snapshot.getChildren()) {
                    String uid = s.getKey();
                    if (uid == null) continue;

                    // Fetch the full name from Firebase
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {
                                    String name = snap.child("fullName").getValue(String.class);
                                    String userName = name != null ? name : "Unknown User";
                                    
                                    // Add to requests list if not already present
                                    boolean exists = false;
                                    for(User u : requests) {
                                        if(u.uid.equals(uid)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    
                                    if(!exists) {
                                        requests.add(new User(uid, userName));
                                        requestsAdapter.update(new ArrayList<>(requests)); 
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
                
                // Clear list if snapshot is empty
                if(!snapshot.exists()) {
                     requestsAdapter.update(new ArrayList<>());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // -------------------------
// Load friends list
// -------------------------
    private void loadFriendsList() {
        friendsManager.loadFriends(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> friends = new ArrayList<>();

                for (DataSnapshot s : snapshot.getChildren()) {
                    String uid = s.getKey();
                    if (uid == null) continue;

                    FirebaseDatabase.getInstance().getReference("users")
                            .child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {
                                    String name = snap.child("fullName").getValue(String.class);
                                    String userName = name != null ? name : "Unknown User";
                                    
                                    boolean exists = false;
                                    for(User u : friends) {
                                        if(u.uid.equals(uid)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    
                                    if(!exists) {
                                        friends.add(new User(uid, userName));
                                        friendsAdapter.update(new ArrayList<>(friends));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
                
                if(!snapshot.exists()) {
                    friendsAdapter.update(new ArrayList<>());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    // -------------------------
    // Search users
    // -------------------------
    private void setupSearch() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                if (keyword.isEmpty()) {
                    searchAdapter.update(new ArrayList<>());
                    searchRv.setVisibility(View.GONE); // Hide if empty
                    return;
                } else {
                    searchRv.setVisibility(View.VISIBLE); // Show if searching
                }

                friendsManager.searchUsers(keyword, new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<User> users = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String uid = snap.getKey();
                            // Don't show self
                            if (uid != null && uid.equals(currentUid)) continue;

                            String fullName = snap.child("fullName").getValue(String.class);
                            String email = snap.child("email").getValue(String.class);
                            
                            // Double check keyword match (case insensitive optional, Firebase startAt is case-sensitive typically)
                            // This logic assumes "fullName" is what we are searching for.
                            
                            users.add(new User(uid, fullName != null ? fullName : "Unknown User"));
                        }
                        searchAdapter.update(new ArrayList<>(users));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        });
    }

    // -------------------------
    // Adapter listener
    // -------------------------
    private final UserAdapter.UserListener userListener = new UserAdapter.UserListener() {
        @Override
        public void onAdd(String uid) {
            friendsManager.sendFriendRequest(uid);
        }

        @Override
        public void onCancel(String uid) {
            friendsManager.cancelFriendRequest(uid);
        }

        @Override
        public void onUnfriend(String uid) {
            friendsManager.unfriend(uid);
        }

        @Override
        public void onAccept(String uid) {
            friendsManager.acceptFriendRequest(uid);
            // Refresh handled by live listeners
        }

        @Override
        public void onDecline(String uid) {
            friendsManager.declineFriendRequest(uid);
            // Refresh handled by live listeners
        }
    };
}
