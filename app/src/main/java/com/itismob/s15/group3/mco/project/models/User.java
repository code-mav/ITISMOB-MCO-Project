package com.itismob.s15.group3.mco.project.models;

public class User {
    public String uid;       // Firebase UID
    public String fullName;
    public String birthday;
    public String email;
    public String password;

    // Default constructor required for Firebase
    public User() {}

    // Constructor for lists (friends, search results, requests)
    public User(String uid, String fullName) {
        this.uid = uid;
        this.fullName = fullName;
    }

    // Full constructor
    public User(String uid, String fullName, String birthday, String email, String password) {
        this.uid = uid;
        this.fullName = fullName;
        this.birthday = birthday;
        this.email = email;
        this.password = password;
    }

    // Constructor for registration (uid not known yet)
    public User(String fullName, String birthday, String email, String password) {
        this.uid = null;  // UID will be assigned later by Firebase
        this.fullName = fullName;
        this.birthday = birthday;
        this.email = email;
        this.password = password;
    }
}
