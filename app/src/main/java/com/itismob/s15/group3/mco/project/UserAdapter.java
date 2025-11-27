package com.itismob.s15.group3.mco.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itismob.s15.group3.mco.project.models.User;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public enum Mode {
        SEARCH,       // Show Add button
        SENT,         // Show Cancel button
        FRIENDS,      // Show Unfriend button
        REQUESTS      // Show Accept / Decline buttons
    }

    public interface UserListener {
        void onAdd(String uid);
        void onCancel(String uid);
        void onUnfriend(String uid);
        void onAccept(String uid);
        void onDecline(String uid);
    }

    private final Context context;
    private ArrayList<User> list;
    private final Mode mode;
    private final UserListener listener;

    public UserAdapter(Context context, ArrayList<User> list, Mode mode, UserListener listener) {
        this.context = context;
        this.list = list;
        this.mode = mode;
        this.listener = listener;
    }

    public void update(ArrayList<User> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (mode) {
            case SEARCH: layoutId = R.layout.item_search_user; break;
            case SENT: layoutId = R.layout.item_sent_request; break;
            case FRIENDS: layoutId = R.layout.item_friend; break;
            case REQUESTS: layoutId = R.layout.item_friend_request; break;
            default: layoutId = R.layout.item_search_user;
        }
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(v, mode);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        User u = list.get(position);

        // Fetch full name from Firebase if empty (only needed for REQUESTS or incomplete User)
        if ((u.fullName == null || u.fullName.isEmpty()) && (mode == Mode.REQUESTS)) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(u.uid)
                    .child("fullName")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String name = snapshot.getValue(String.class);
                            u.fullName = name != null ? name : "Unknown User";
                            h.name.setText(u.fullName);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            h.name.setText("Unknown User");
                        }
                    });
        } else {
            h.name.setText(u.fullName != null ? u.fullName : "Unknown User");
        }

        // Reset previous listeners
        h.primaryBtn.setOnClickListener(null);
        if (h.secondaryBtn != null) h.secondaryBtn.setOnClickListener(null);

        switch (mode) {
            case SEARCH:
                h.primaryBtn.setText("Add");
                h.primaryBtn.setEnabled(true);
                h.primaryBtn.setOnClickListener(v -> {
                    listener.onAdd(u.uid);
                    h.primaryBtn.setText("Requested");  // Update immediately
                    h.primaryBtn.setEnabled(false);
                    Toast.makeText(context, "Friend request sent to " + u.fullName, Toast.LENGTH_SHORT).show();
                });
                break;


            case SENT:
                h.primaryBtn.setText("Cancel");
                h.primaryBtn.setOnClickListener(v -> listener.onCancel(u.uid));
                break;

            case FRIENDS:
                h.primaryBtn.setText("Unfriend");
                h.primaryBtn.setOnClickListener(v -> listener.onUnfriend(u.uid));
                break;

            case REQUESTS:
                if (h.secondaryBtn == null) break;
                h.primaryBtn.setText("Accept");
                h.secondaryBtn.setText("Decline");

                h.primaryBtn.setOnClickListener(v -> listener.onAccept(u.uid));
                h.secondaryBtn.setOnClickListener(v -> listener.onDecline(u.uid));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        Button primaryBtn;
        Button secondaryBtn; // Only for REQUESTS

        ViewHolder(@NonNull View itemView, Mode mode) {
            super(itemView);

            // Assign views based on mode
            switch (mode) {
                case SEARCH:
                    name = itemView.findViewById(R.id.searchUserName);
                    primaryBtn = itemView.findViewById(R.id.btnAddFriend);
                    secondaryBtn = null;
                    break;
                case SENT:
                    name = itemView.findViewById(R.id.sentReqName);
                    primaryBtn = itemView.findViewById(R.id.btnCancelRequest);
                    secondaryBtn = null;
                    break;
                case FRIENDS:
                    name = itemView.findViewById(R.id.friendName);
                    primaryBtn = itemView.findViewById(R.id.btnUnfriend);
                    secondaryBtn = null;
                    break;
                case REQUESTS:
                    name = itemView.findViewById(R.id.reqName);
                    primaryBtn = itemView.findViewById(R.id.acceptBtn);
                    secondaryBtn = itemView.findViewById(R.id.declineBtn);
                    break;
            }
        }
    }
}
