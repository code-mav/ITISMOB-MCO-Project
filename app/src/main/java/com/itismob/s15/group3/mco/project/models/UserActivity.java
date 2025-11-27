package com.itismob.s15.group3.mco.project.models;

public class UserActivity {
    public String title;
    public String description;
    public long timestamp;

    public UserActivity() {}

    public UserActivity(String title, String description, long timestamp) {
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
    }
}
