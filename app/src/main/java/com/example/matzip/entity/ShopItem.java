package com.example.matzip.entity;

public class ShopItem {

    public String id;                           // Shop Doc ID
    public Shop shop;                           // Shop 객체
    public double distance;                     // 거리 (나의 위치와의 거리)

    public String filePath;                     // 파일 경로 (다운로드 받은 파일경로)
    public boolean download;                    // 다운로드 시도했는지 체크

    public ShopItem(String id, Shop shop, double distance) {
        this.id = id;
        this.shop = shop;
        this.distance = distance;

        this.filePath = "";
        this.download = false;
    }
}
