package com.example.matzip.entity;

public class Shop {

    private String shopId;                  // Shop Account
    private String password;                // Password

    private String name;                    // Shop name
    private String phoneNumber;             // Phone number
    private String address;                 // Address

    private String foodCategory;

    private String menuTable;
    private String memo;

    private double latitude;
    private double longitude;

    private String imageFileName;

    private int reviewPoint;
    private int reviewCount;
    
    private long joinTimeMillis;

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
