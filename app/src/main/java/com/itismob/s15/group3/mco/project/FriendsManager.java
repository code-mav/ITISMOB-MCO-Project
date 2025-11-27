package com.itismob.s15.group3.mco.project;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FriendsManager {

    private final DatabaseReference db = FirebaseDatabase.getInstance().getReference("users");
    private final String currentUid;

    public FriendsManager(String currentUid) {
        this.currentUid = currentUid;
    }

    // SEND FRIEND REQUEST
    public void sendFriendRequest(String targetUid) {
        db.child(targetUid)
                .child("friendRequests")
                .child("from")
                .child(currentUid)
                .setValue(true);
    }

    // CANCEL SENT REQUEST
    public void cancelFriendRequest(String targetUid) {
        db.child(targetUid)
                .child("friendRequests")
                .child("from")
                .child(currentUid)
                .removeValue();
    }

    // ACCEPT REQUEST
    public void acceptFriendRequest(String fromUid) {
        db.child(currentUid).child("friends").child(fromUid).setValue(true);
        db.child(fromUid).child("friends").child(currentUid).setValue(true);
        db.child(currentUid).child("friendRequests").child("from").child(fromUid).removeValue();
    }

    // DECLINE REQUEST
    public void declineFriendRequest(String fromUid) {
        db.child(currentUid).child("friendRequests").child("from").child(fromUid).removeValue();
    }

    // UNFRIEND
    public void unfriend(String uid) {
        db.child(currentUid).child("friends").child(uid).removeValue();
        db.child(uid).child("friends").child(currentUid).removeValue();
    }

    // LISTEN FOR FRIEND REQUESTS
    public void listenFriendRequests(ValueEventListener listener) {
        db.child(currentUid)
                .child("friendRequests")
                .child("from")
                .addValueEventListener(listener);
    }

    // LOAD FRIENDS LIST
    public void loadFriends(ValueEventListener listener) {
        db.child(currentUid)
                .child("friends")
                .addValueEventListener(listener);
    }

    // SEARCH USERS BY NAME
    public void searchUsers(String keyword, ValueEventListener listener) {
        db.orderByChild("fullName")
                .startAt(keyword)
                .endAt(keyword + "\uf8ff")
                .addListenerForSingleValueEvent(listener);
    }
}
