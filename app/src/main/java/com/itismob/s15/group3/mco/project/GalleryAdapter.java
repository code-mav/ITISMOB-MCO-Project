package com.itismob.s15.group3.mco.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private List<GalleryItem> items;
    private String currentUserId;
    private OnDeleteListener deleteListener;

    // Formatter for accurate date + time
    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnDeleteListener {
        void onDelete(GalleryItem item);
    }

    public GalleryAdapter(List<GalleryItem> items, String currentUserId, OnDeleteListener deleteListener) {
        this.items = items;
        this.currentUserId = currentUserId;
        this.deleteListener = deleteListener;
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

        // Image
        if (item.getImageBitmap() != null) {
            holder.imgPhoto.setImageBitmap(item.getImageBitmap());
        } else {
            holder.imgPhoto.setImageResource(android.R.color.darker_gray);
        }

        // Who posted – already includes "You (Name)" for you
        holder.tvFriend.setText(item.getFriendName());

        // Habit category (Fitness, Learning, etc.)
        holder.tvHabit.setText(item.getHabitType());

        // Image "name" – using title field
        holder.tvStreak.setText(item.getTitle());

        // Accurate date + time from timestamp
        if (item.getTimestamp() > 0) {
            String dateTime = dateTimeFormat.format(new Date(item.getTimestamp()));
            holder.tvDateTime.setText(dateTime);
        } else {
            holder.tvDateTime.setText("");
        }

        // Show delete button only if current user owns the item
        if (currentUserId != null && currentUserId.equals(item.getUserId())) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(item);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        ImageView btnDelete;
        TextView tvFriend, tvHabit, tvStreak, tvDateTime;

        ViewHolder(View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgPhoto);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            tvFriend = itemView.findViewById(R.id.tvFriend);
            tvHabit = itemView.findViewById(R.id.tvHabit);
            tvStreak = itemView.findViewById(R.id.tvStreak);      // image name/title
            tvDateTime = itemView.findViewById(R.id.tvDateTime);  // date/time label
        }
    }
}
