package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteManager {
    private static final String OSRM_API_URL = "https://router.project-osrm.org/route/v1/driving/";
    private MapView mapView;
    private Context context;
    private Polyline routeLine;
    private ExecutorService executor;
    private Handler mainHandler;

    public RouteManager(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface RouteCallback {
        void onRouteCalculated(double distanceKm, double durationMinutes);
        void onRouteError(String error);
    }

    public void drawRoute(GeoPoint start, GeoPoint destination, RouteCallback callback) {
        // Remove previous route if exists
        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
        }

        executor.execute(() -> {
            try {
                // Build OSRM API URL
                String urlString = String.format("%s%f,%f;%f,%f?overview=full&geometries=geojson",
                        OSRM_API_URL,
                        start.getLongitude(), start.getLatitude(),
                        destination.getLongitude(), destination.getLatitude());

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());

                    if (jsonResponse.getString("code").equals("Ok")) {
                        JSONArray routes = jsonResponse.getJSONArray("routes");
                        JSONObject route = routes.getJSONObject(0);

                        // Get distance and duration
                        double distanceMeters = route.getDouble("distance");
                        double durationSeconds = route.getDouble("duration");
                        double distanceKm = distanceMeters / 1000.0;
                        double durationMinutes = durationSeconds / 60.0;

                        // Get route coordinates
                        JSONObject geometry = route.getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");

                        List<GeoPoint> routePoints = new ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            double lon = coord.getDouble(0);
                            double lat = coord.getDouble(1);
                            routePoints.add(new GeoPoint(lat, lon));
                        }

                        // Draw route on map on main thread
                        final double finalDistanceKm = distanceKm;
                        final double finalDurationMinutes = durationMinutes;

                        mainHandler.post(() -> {
                            routeLine = new Polyline(mapView);
                            routeLine.setPoints(routePoints);
                            routeLine.setColor(Color.parseColor("#2196F3")); // Blue color
                            routeLine.setWidth(8f);
                            routeLine.getOutlinePaint().setColor(Color.parseColor("#1976D2"));
                            routeLine.getOutlinePaint().setStrokeWidth(12f);

                            mapView.getOverlays().add(routeLine);
                            mapView.invalidate();

                            if (callback != null) {
                                callback.onRouteCalculated(finalDistanceKm, finalDurationMinutes);
                            }
                        });
                    } else {
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onRouteError("Route not found");
                            }
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onRouteError("Server error: " + responseCode);
                        }
                    });
                }
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onRouteError("Failed to calculate route: " + e.getMessage());
                    }
                });
            }
        });
    }

    public void clearRoute() {
        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
            mapView.invalidate();
        }
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}