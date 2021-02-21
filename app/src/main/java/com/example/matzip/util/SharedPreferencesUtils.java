package com.example.matzip.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

public class SharedPreferencesUtils {
    private volatile static SharedPreferencesUtils _instance = null;

    private final SharedPreferences cache;

    public static SharedPreferencesUtils getInstance(Context context) {
        if (_instance == null) {
            synchronized (SharedPreferencesUtils.class) {
                if (_instance == null) {
                    _instance = new SharedPreferencesUtils(context, "MatZip");
                }
            }
        }

        return _instance;
    }

    private SharedPreferencesUtils(Context context, String cacheName) {
        Context applicationContext = context.getApplicationContext();
        context = applicationContext != null ? applicationContext : context;
        this.cache = context.getSharedPreferences(cacheName, Context.MODE_PRIVATE);
    }

    public void clearAll() {
        this.cache.edit().clear().apply();
    }

    public void clear(final List<String> keysToClear) {
        SharedPreferences.Editor editor = this.cache.edit();
        for(String key : keysToClear){
            editor.remove(key);
        }
        editor.apply();
    }

    /* String */
    public void put(String key, String value) {
        SharedPreferences.Editor editor = this.cache.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /* String */
    public String get(String key) {
        return this.cache.getString(key, null);
    }

    /* boolean */
    public void put(String key, boolean value) {
        SharedPreferences.Editor editor = this.cache.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /* boolean */
    public boolean get(String key, boolean def) {
        return this.cache.getBoolean(key, def);
    }

    /* int */
    public void put(String key, int value) {
        SharedPreferences.Editor editor = this.cache.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /* int */
    public int get(String key, int def) {
        return this.cache.getInt(key, def);
    }

    /* long */
    public void put(String key, long value) {
        SharedPreferences.Editor editor = this.cache.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /* long */
    public long get(String key, long def) {
        return this.cache.getLong(key, def);
    }
}