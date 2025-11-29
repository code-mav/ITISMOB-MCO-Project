package com.itismob.s15.group3.mco.project;

import android.graphics.Bitmap;

public class GalleryItem {
    private Bitmap imageBitmap;
    private String friendName;   // who posted 
    private String habitType;    // category
    private String title;        // snap title
    private String userId;       // owner id
    private long timestamp;      // when it was posted (millis)
    private String key;          // Firebase key

    public GalleryItem(Bitmap imageBitmap,
                       String friendName,
                       String habitType,
                       String title,
                       String userId,
                       long timestamp,
                       String key) {
        this.imageBitmap = imageBitmap;
        this.friendName = friendName;
        this.habitType = habitType;
        this.title = title;
        this.userId = userId;
        this.timestamp = timestamp;
        this.key = key;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public String getFriendName() {
        return friendName;
    }

    public String getHabitType() {
        return habitType;
    }

    public String getTitle() {
        return title;
    }

    public String getUserId() {
        return userId;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    public String getKey() {
        return key;
    }
}
