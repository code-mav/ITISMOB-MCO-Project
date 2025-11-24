package com.itismob.s15.group3.mco.project.models;
public class User {
    public String fullName;
    public String birthday;
    public String email;
    public String password;

    public User() {}

    public User(String fullName, String birthday, String email, String password) {
        this.fullName = fullName;
        this.birthday = birthday;
        this.email = email;
        this.password = password;
    }
}

