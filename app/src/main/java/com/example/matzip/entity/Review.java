package com.example.matzip.entity;

public class Review {

    private String shopDocId;                   // 가게 Doc Id
    private String userDocId;                   // 작성 회원 Doc Id

    private int point;                          // 별점
    private String contents;                    // 리뷰 내용

    private long inputTimeMillis;               // 등록일시를 millisecond 로 표현

    /* 파이어스토어에서 객체 매핑을 위해 기본 생성자가 필요함 */
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
