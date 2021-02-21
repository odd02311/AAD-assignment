package com.example.matzip.util;

import com.example.matzip.entity.Shop;
import com.example.matzip.entity.ShopItem;
import com.example.matzip.entity.User;

import java.util.ArrayList;

public class GlobalVariable {

    public static String userDocumentId;        // 일반 Doc ID
    public static User user;                    // 일반 객체

    public static String shopDocumentId;        // 업주(가게) Doc ID
    public static Shop shop;                    // 업주(가게) 객체

    // 음식점 위치를 지도로 볼 때 사용할 음식점 array list 객체
    public static ArrayList<ShopItem> shopItems;
}
