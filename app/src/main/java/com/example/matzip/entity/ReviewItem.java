package com.example.matzip.entity;

public class ReviewItem {

    public String id;                       // Review Doc Id
    public Review review;                   // 리뷰 객체

    public ReviewItem(String id, Review review) {
        this.id = id;
        this.review = review;
    }
}
