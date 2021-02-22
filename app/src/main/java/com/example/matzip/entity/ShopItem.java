package com.example.matzip.entity;

public class ShopItem {

    public String id;
    public Shop shop;
    public double distance;

    public String filePath;
    public boolean download;

    public ShopItem(String id, Shop shop, double distance) {
        this.id = id;
        this.shop = shop;
        this.distance = distance;

        this.filePath = "";
        this.download = false;
    }
}
