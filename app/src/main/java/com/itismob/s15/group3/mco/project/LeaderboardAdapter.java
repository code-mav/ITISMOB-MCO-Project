package com.itismob.s15.group3.mco.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s15.group3.mco.project.models.User;

import java.util.ArrayList;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<User> users;

    public LeaderboardAdapter(Context context, ArrayList<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.rankTv.setText(String.valueOf(position + 1));
        holder.nameTv.setText(user.fullName != null ? user.fullName : "Unknown");
        holder.streakTv.setText(user.streak + " days");
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankTv, nameTv, streakTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankTv = itemView.findViewById(R.id.rankTextView);
            nameTv = itemView.findViewById(R.id.userNameTextView);
            streakTv = itemView.findViewById(R.id.streakTextView);
        }
    }
}
