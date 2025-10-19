package com.itismob.s15.group3.mco.project;

public class GalleryItem {
    private int imageRes;
    private String friendName;
    private String habitType;
    private int streakDays;

    public GalleryItem(int imageRes, String friendName, String habitType, int streakDays) {
        this.imageRes = imageRes;
        this.friendName = friendName;
        this.habitType = habitType;
        this.streakDays = streakDays;
    }

    public int getImageRes() { return imageRes; }
    public String getFriendName() { return friendName; }
    public String getHabitType() { return habitType; }
    public int getStreakDays() { return streakDays; }
}
