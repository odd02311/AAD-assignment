package com.example.matzip.entity;

public class User {

    private String snsKey;

    private String name;
    private String phoneNumber;
    private String email;

    private String favoriteCategory;

    private long joinTimeMillis;

    public User() {}

    public User(String snsKey, String name, String phoneNumber, String email, long joinTimeMillis) {
        this.snsKey = snsKey;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.joinTimeMillis = joinTimeMillis;
    }

    public String getSnsKey() {
        return this.snsKey;
    }

    public String getName() {
        return this.name;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getEmail() {
        return this.email;
    }

    public String getFavoriteCategory() {
        return this.favoriteCategory;
    }

    public long getJoinTimeMillis() {
        return this.joinTimeMillis;
    }

    public void setFavoriteCategory(String favoriteCategory) {
        this.favoriteCategory = favoriteCategory;
    }
}
