package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {
    private static FavoritesManager instance;
    private List<String> favoritesList;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "FavoritesPrefs";
    private static final String KEY_FAVORITES = "favorites_list";

    private FavoritesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        favoritesList = new ArrayList<>();
        loadFavorites();
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadFavorites() {
        Set<String> set = sharedPreferences.getStringSet(KEY_FAVORITES, null);
        favoritesList.clear();
        if (set != null) {
            favoritesList.addAll(set);
        } else {
            // Default initial favorite
            addFavorite("Universiti Malaya");
        }
    }

    private void saveFavorites() {
        Set<String> set = new HashSet<>(favoritesList);
        sharedPreferences.edit().putStringSet(KEY_FAVORITES, set).apply();
    }

    public List<String> getFavorites() {
        return favoritesList;
    }

    public void addFavorite(String location) {
        for (String fav : favoritesList) {
            if (fav.equalsIgnoreCase(location)) {
                return; // Already exists
            }
        }

        if (!favoritesList.contains(location)) {
            favoritesList.add(location);
            saveFavorites();
        }
    }

    public void removeFavorite(String location) {
        if (favoritesList.remove(location)) {
            saveFavorites();
        }
    }
}
