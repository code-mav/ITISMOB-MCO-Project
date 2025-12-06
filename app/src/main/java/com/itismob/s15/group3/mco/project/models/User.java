package com.itismob.s15.group3.mco.project.models;

import java.util.Map;

public class User {
    public String uid;       // Firebase UID
    public String fullName;
    public String birthday;
    public String email;
    public String password;
    public int streak;       // Current streak count
    public int restoresLeft; // Restores available for the month
    public int lostStreak;   // Value of the lost streak (if any) to be restored
    public long lastStreakUpdate; // Timestamp of last streak update
    public long lastRestoreReset; // Timestamp of last monthly restore reset

    // NEW: per-category streak aggregates stored under /users
    public int fitnessStreak;
    public int learningStreak;
    public int healthStreak;
    public int creativityStreak;
    public int productivityStreak;

    // NEW: scores used by FriendsLeaderboardFragment
    public int totalScore;
    public int fitnessScore;
    public int learningScore;
    public int healthScore;
    public int creativityScore;
    public int productivityScore;

    // Optional extra fields that exist in your DB
    public String bio;
    public Map<String, Boolean> friends;
    public Map<String, Object> categoryRestoreHistory;
    public Map<String, Object> categoryRestores;
    public Map<String, Object> categoryStreaks;
    public Map<String, Object> proofs;

    // Default constructor required for Firebase
    public User() {
        this.streak = 0;
        this.restoresLeft = 0;
        this.lostStreak = 0;
    }

    // Constructor for lists (friends, search results, requests)
    public User(String uid, String fullName) {
        this.uid = uid;
        this.fullName = fullName;
    }

    // Constructor with streak
    public User(String uid, String fullName, int streak) {
        this.uid = uid;
        this.fullName = fullName;
        this.streak = streak;
    }

    // Full constructor
    public User(String uid, String fullName, String birthday, String email, String password) {
        this.uid = uid;
        this.fullName = fullName;
        this.birthday = birthday;
        this.email = email;
        this.password = password;
        this.streak = 0;
        this.restoresLeft = 3; // Default to 3 restores
        this.lostStreak = 0;
    }

    // Constructor for registration (uid not known yet)
    public User(String fullName, String birthday, String email, String password) {
        this.uid = null;  // UID will be assigned later by Firebase
        this.fullName = fullName;
        this.birthday = birthday;
        this.email = email;
        this.password = password;
        this.streak = 0;
        this.restoresLeft = 3;
    }
}
