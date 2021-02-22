package com.example.matzip.entity;

public class Review {

    private String shopDocId;
    private String userDocId;

    private int point;
    private String contents;

    private long inputTimeMillis;


    public Review() {}

    public Review(String shopDocId, String userDocId, int point, String contents, long inputTimeMillis) {
        this.shopDocId = shopDocId;
        this.userDocId = userDocId;
        this.point = point;
        this.contents = contents;
        this.inputTimeMillis = inputTimeMillis;
    }

    public String getShopDocId() {
        return shopDocId;
    }

    public String getUserDocId() {
        return userDocId;
    }

    public int getPoint() {
        return point;
    }

    public String getContents() {
        return contents;
    }

    public long getInputTimeMillis() {
        return inputTimeMillis;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }
}
