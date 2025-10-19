package com.itismob.s15.group3.mco.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private List<GalleryItem> items;

    public GalleryAdapter(List<GalleryItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_grid, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GalleryItem item = items.get(position);
        holder.imgPhoto.setImageResource(item.getImageRes());
        holder.tvFriend.setText(item.getFriendName());
        holder.tvHabit.setText(item.getHabitType());
        holder.tvStreak.setText(item.getStreakDays() + "-day streak");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        TextView tvFriend, tvHabit, tvStreak;

        ViewHolder(View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgPhoto);
            tvFriend = itemView.findViewById(R.id.tvFriend);
            tvHabit = itemView.findViewById(R.id.tvHabit);
            tvStreak = itemView.findViewById(R.id.tvStreak);
        }
    }
}
