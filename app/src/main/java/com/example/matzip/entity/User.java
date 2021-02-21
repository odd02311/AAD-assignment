package com.example.matzip.entity;

public class User {

    private String snsKey;                  // SNS 로그인 키값(UID)

    private String name;                    // 이름
    private String phoneNumber;             // 휴대번호
    private String email;                   // 이메일

    private String favoriteCategory;        // 좋아하는 카테고리

    private long joinTimeMillis;            // 가입일시를 millisecond 로 표현

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
