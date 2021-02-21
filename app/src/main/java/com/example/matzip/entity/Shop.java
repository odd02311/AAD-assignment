package com.example.matzip.entity;

public class Shop {

    private String shopId;                  // 아이디
    private String password;                // 비밀번호

    private String name;                    // 상호
    private String phoneNumber;             // 전화번호
    private String address;                 // 주소

    private String foodCategory;            // 음식 카테고리

    private String menuTable;               // 메뉴 (TEXT)
    private String memo;                    // 가게 설명

    private double latitude;                // 위도
    private double longitude;               // 경도

    private String imageFileName;           // 가게 대표 사진 파일명 (이미지는 Storage 에 저장됨)

    private int reviewPoint;                // 총 별점 합산점수
    private int reviewCount;                // 리뷰 수
    
    private long joinTimeMillis;            // 가입일시를 millisecond 로 표현

    public Shop() {}

    public Shop(String shopId, String password, String name, String phoneNumber, String address, String foodCategory,
                String menuTable, String memo, String imageFileName, double latitude, double longitude, long joinTimeMillis) {
        this.shopId = shopId;
        this.password = password;

        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;

        this.foodCategory = foodCategory;

        this.menuTable = menuTable;
        this.memo = memo;

        this.imageFileName = imageFileName;

        this.latitude = latitude;
        this.longitude = longitude;

        this.joinTimeMillis = joinTimeMillis;

        this.reviewPoint = 0;
        this.reviewCount = 0;
    }

    public String getShopId() {
        return shopId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public String getMenuTable() {
        return menuTable;
    }

    public String getMemo() {
        return memo;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getReviewPoint() {
        return reviewPoint;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public long getJoinTimeMillis() {
        return joinTimeMillis;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public void setMenuTable(String menuTable) {
        this.menuTable = menuTable;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public void setReviewPoint(int reviewPoint) {
        this.reviewPoint = reviewPoint;
    }
}
