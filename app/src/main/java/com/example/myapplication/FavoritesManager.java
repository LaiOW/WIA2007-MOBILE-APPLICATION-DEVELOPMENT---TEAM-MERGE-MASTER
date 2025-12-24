package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

public class FavoritesManager {
    private static FavoritesManager instance;
    private List<String> favoritesList;

    private FavoritesManager() {
        favoritesList = new ArrayList<>();
        // Add some mock data initially if needed, or keep it empty
        favoritesList.add("Universiti Malaya");
    }

    public static synchronized FavoritesManager getInstance() {
        if (instance == null) {
            instance = new FavoritesManager();
        }
        return instance;
    }

    public List<String> getFavorites() {
        return favoritesList;
    }

    public void addFavorite(String location) {
        if (!favoritesList.contains(location)) {
            favoritesList.add(location);
        }
    }

    public void removeFavorite(String location) {
        favoritesList.remove(location);
    }
}
