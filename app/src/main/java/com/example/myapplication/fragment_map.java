package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.Marker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class fragment_map extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private IMapController controller;
    private MyLocationNewOverlay myLocationOverlay;
    private TextView tvUserName;
    private TextView tvCasesCount;
    private TextView tvDateDay;
    private TextView tvUserCount;
    private TextView tvDateMonthYear;
    private TextView tvMapCoordinates;
    private EditText searchEditText;
    private ListView suggestionsList;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabReturnToLocation; private com.google.android.material.floatingactionbutton.FloatingActionButton fabFavorites;
    private RouteManager routeManager;
    private IconManager iconManager;
    private ArrayAdapter<String> suggestionsAdapter;
    private List<SearchResult> searchResults;
    private Handler searchHandler;
    private ExecutorService executor;
    private Marker searchMarker;
    private List<Marker> sosMarkers = new ArrayList<>();
    private List<SOSCall> allSOSCalls = new ArrayList<>();
    private com.google.android.material.button.MaterialButton btnAcceptCase;
    private SOSCall selectedSOSCall;
    private Marker selectedMarker;
    private long lastClickTime = 0;
    private Marker lastClickedMarker = null;
    private List<Marker> acceptedMarkers = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Configuration
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = view.findViewById(R.id.mapView);
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mapView.setMultiTouchControls(true);

        // Disable built-in zoom controls
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        // UI Overlay Elements
        tvUserName = view.findViewById(R.id.tvUserName);
        tvCasesCount = view.findViewById(R.id.tvCasesCount);
        tvDateDay = view.findViewById(R.id.tvDateDay);
        tvUserCount = view.findViewById(R.id.tvUserCount);
        tvDateMonthYear = view.findViewById(R.id.tvDateMonthYear);
        tvMapCoordinates = view.findViewById(R.id.tvMapCoordinates);
        View cardProfile = view.findViewById(R.id.cardProfile);
        searchEditText = view.findViewById(R.id.ETsearch);
        suggestionsList = view.findViewById(R.id.suggestionsList);
        fabReturnToLocation = view.findViewById(R.id.fabReturnToLocation);
        fabFavorites = view.findViewById(R.id.fabFavorites);
        btnAcceptCase = view.findViewById(R.id.btnAcceptCase);

        // Setup Favorites FAB
        if (fabFavorites != null) {
            fabFavorites.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main, new FavoritesFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // Setup return to location button
        setupReturnToLocationButton();

        // Setup accept case button
        setupAcceptCaseButton();

        // Auto-select all text when clicking search bar
        searchEditText.setSelectAllOnFocus(true);

        updateUsernameDisplay();
        updateDateDisplay();
        loadStatistics();
        loadAndPlotSOSCalls();
        setupSearchAutocomplete();
        setupStatsSectionListeners(view);

        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            startActivity(intent);
        });

        // SOS Button Listener
        view.findViewById(R.id.BTSOScall).setOnClickListener(v -> {
            // Check for location permissions first
            if (getContext() != null &&
                    (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                // Request permissions
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            recordSOSLocation();
        });

        // Close suggestions when touching map
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (suggestionsList.getVisibility() == View.VISIBLE) {
                    suggestionsList.setVisibility(View.GONE);
                    searchEditText.clearFocus();
                    hideKeyboard();
                }
            }
            // Return false to let map handle all touch events normally
            return false;
        });

        // Add map click listener to close info windows when tapping empty space
        org.osmdroid.events.MapEventsReceiver mapEventsReceiver = new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Close all open info windows when map (not marker) is clicked
                try {
                    for (Marker marker : sosMarkers) {
                        marker.closeInfoWindow();
                    }
                } catch (Exception e) {
                    // Ignore errors
                }

                // Reset double-click tracking
                lastClickTime = 0;
                lastClickedMarker = null;

                // Hide accept case button when clicking elsewhere
                if (btnAcceptCase != null && btnAcceptCase.getVisibility() == View.VISIBLE) {
                    btnAcceptCase.setVisibility(View.GONE);
                    selectedSOSCall = null;
                    selectedMarker = null;
                }

                // Remove accepted markers when clicking elsewhere
                if (!acceptedMarkers.isEmpty()) {
                    // Clear route
                    if (routeManager != null) {
                        routeManager.clearRoute();
                    }

                    for (Marker acceptedMarker : acceptedMarkers) {
                        mapView.getOverlays().remove(acceptedMarker);
                        sosMarkers.remove(acceptedMarker);
                    }
                    acceptedMarkers.clear();
                    mapView.invalidate();
                    // Reload all SOS calls to sync with database
                    loadAndPlotSOSCalls();
                }

                return false; // Return false to allow other overlays to handle the event
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        org.osmdroid.views.overlay.MapEventsOverlay mapEventsOverlay =
                new org.osmdroid.views.overlay.MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay); // Add at index 0 so it's processed last

        controller = mapView.getController();
        // Don't set default zoom - let it zoom to user location automatically

        // Initialize IconManager
        iconManager = new IconManager(requireContext());

        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        myLocationOverlay = new MyLocationNewOverlay(provider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);

        // Use IconManager to create person icon
        Bitmap personIcon = iconManager.createPersonIconWithArrow();
        myLocationOverlay.setPersonIcon(personIcon);
        myLocationOverlay.setDirectionIcon(personIcon);

        myLocationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    controller.animateTo(myLocationOverlay.getMyLocation());
                    controller.setZoom(15.0);
                });
            }
        });

        mapView.getOverlays().add(myLocationOverlay);
        // Don't set a default center - let it use user's actual location

        // Initialize RouteManager
        routeManager = new RouteManager(requireContext(), mapView);

        // Add scroll listener to update coordinates display
        mapView.addMapListener(new org.osmdroid.events.MapListener() {
            @Override
            public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                updateMapCoordinatesDisplay();
                return true;
            }

            @Override
            public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                updateMapCoordinatesDisplay();
                return true;
            }
        });

        // Initial coordinate display update
        updateMapCoordinatesDisplay();

        ImageView searchbutton = view.findViewById(R.id.searchIcon);
        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If suggestions are visible, navigate to first result
                if (suggestionsList.getVisibility() == View.VISIBLE && !searchResults.isEmpty()) {
                    searchHandler.removeCallbacksAndMessages(null);
                    suggestionsList.setVisibility(View.GONE);
                    searchEditText.clearFocus();
                    hideKeyboard();
                    SearchResult result = searchResults.get(0);
                    moveToLocation(result.lat, result.lon, result.displayName);
                } else {
                    // Focus and select search bar text for new search
                    searchEditText.requestFocus();
                    searchEditText.selectAll();
                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        return view;
    }

    private void updateUsernameDisplay() {
        if (getContext() == null) return;
        String currentUserEmail = SupabaseManager.INSTANCE.getCurrentUserEmail();
        if (currentUserEmail != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String savedName = sharedPreferences.getString("display_name", null);
            if (savedName != null) {
                tvUserName.setText(savedName);
            } else {
                tvUserName.setText(currentUserEmail.split("@")[0]);
            }
        }
    }

    private void updateDateDisplay() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMM\nyyyy", Locale.getDefault());

        String day = dayFormat.format(calendar.getTime());
        String monthYear = monthYearFormat.format(calendar.getTime());

        tvDateDay.setText(day);
        tvDateMonthYear.setText(monthYear);
    }

    private void updateMapCoordinatesDisplay() {
        if (mapView != null && tvMapCoordinates != null) {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            String coordText = String.format(Locale.getDefault(),
                    "Lat: %.6f, Lon: %.6f",
                    center.getLatitude(),
                    center.getLongitude());
            tvMapCoordinates.setText(coordText);
        }
    }

    private void loadStatistics() {
        // Start with empty displays - no placeholders
        tvCasesCount.setText("");
        tvUserCount.setText("");

        SupabaseManager.INSTANCE.getStatistics(new SupabaseManager.StatsCallback() {
            @Override
            public void onSuccess(int casesCount, int userCount) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        tvCasesCount.setText(String.valueOf(casesCount));
                        tvUserCount.setText(String.valueOf(userCount));
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        tvCasesCount.setText("");
                        tvUserCount.setText("");
                        Toast.makeText(getContext(), "Failed to load statistics: " + message,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    private void loadAndPlotSOSCalls() {
        SupabaseManager.INSTANCE.getAllSOSCalls(new SupabaseManager.SOSCallsCallback() {
            @Override
            public void onSuccess(List<SOSCall> sosCalls) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // Store all SOS calls for distance calculations
                        allSOSCalls.clear();
                        allSOSCalls.addAll(sosCalls);

                        // Clear existing SOS markers
                        for (Marker marker : sosMarkers) {
                            mapView.getOverlays().remove(marker);
                        }
                        sosMarkers.clear();

                        // Add markers for each SOS call
                        for (SOSCall sosCall : sosCalls) {
                            // GeoPoint takes (latitude, longitude) = (y, x)
                            GeoPoint point = new GeoPoint(sosCall.getX_coordinate(), sosCall.getY_coordinate());
                            Marker marker = new Marker(mapView);
                            marker.setPosition(point);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setTitle("SOS Call");
                            marker.setSnippet("User: " + sosCall.getUsername() + "\nTime: " + sosCall.getTime());
                            // Use IconManager to set alert icon
                            iconManager.setAlertIcon(marker);
                            mapView.getOverlays().add(marker);
                            sosMarkers.add(marker);
                        }
                        mapView.invalidate();

                        // Automatically highlight nearest 5 SOS calls
                        autoHighlightNearestSOSCalls();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to load SOS calls: " + message,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void setupStatsSectionListeners(View view) {
        View layoutCasesSection = view.findViewById(R.id.layoutCasesSection);
        View layoutDateSection = view.findViewById(R.id.layoutDateSection);
        View layoutUserSection = view.findViewById(R.id.layoutUserSection);

        View.OnClickListener nearestSOSListener = v -> showNearestSOSCalls();

        // Set click listeners for all three sections
        if (layoutCasesSection != null) {
            layoutCasesSection.setOnClickListener(nearestSOSListener);
        }
        if (layoutDateSection != null) {
            layoutDateSection.setOnClickListener(nearestSOSListener);
        }
        if (layoutUserSection != null) {
            layoutUserSection.setOnClickListener(nearestSOSListener);
        }
    }

    private void autoHighlightNearestSOSCalls() {
        // Get current user location
        GeoPoint userLocation = null;
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            userLocation = myLocationOverlay.getMyLocation();
        } else if (getContext() != null) {
            LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation == null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastLocation != null) {
                    userLocation = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                }
            }
        }

        if (userLocation == null || allSOSCalls.isEmpty()) {
            return; // Silently return if no location or no SOS calls
        }

        // Calculate distances and sort
        final GeoPoint finalUserLocation = userLocation;
        List<NearestSOSDialog.SOSCallWithDistance> sosCallsWithDistances = new ArrayList<>();

        for (SOSCall sosCall : allSOSCalls) {
            // GeoPoint takes (latitude, longitude) = (y, x) but we store as (x, y)
            GeoPoint sosLocation = new GeoPoint(sosCall.getX_coordinate(), sosCall.getY_coordinate());
            double distance = calculateDistance(finalUserLocation, sosLocation);
            sosCallsWithDistances.add(new NearestSOSDialog.SOSCallWithDistance(sosCall, distance));
        }

        // Sort by distance and get top 5
        sosCallsWithDistances.sort((a, b) -> Double.compare(a.distance, b.distance));
        List<NearestSOSDialog.SOSCallWithDistance> nearest5 = sosCallsWithDistances.subList(0, Math.min(5, sosCallsWithDistances.size()));

        // Automatically highlight on map without showing dialog
        highlightNearestSOSCalls(nearest5, finalUserLocation);
    }

    private void showNearestSOSCalls() {
        // Get current user location
        GeoPoint userLocation = null;
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            userLocation = myLocationOverlay.getMyLocation();
        } else if (getContext() != null) {
            LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation == null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastLocation != null) {
                    userLocation = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                }
            }
        }

        if (userLocation == null) {
            Toast.makeText(getContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (allSOSCalls.isEmpty()) {
            Toast.makeText(getContext(), "No SOS calls available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate distances and sort
        final GeoPoint finalUserLocation = userLocation;
        List<NearestSOSDialog.SOSCallWithDistance> sosCallsWithDistances = new ArrayList<>();

        for (SOSCall sosCall : allSOSCalls) {
            // GeoPoint takes (latitude, longitude) = (y, x) but we store as (x, y)
            GeoPoint sosLocation = new GeoPoint(sosCall.getX_coordinate(), sosCall.getY_coordinate());
            double distance = calculateDistance(finalUserLocation, sosLocation);
            sosCallsWithDistances.add(new NearestSOSDialog.SOSCallWithDistance(sosCall, distance));
        }

        // Sort by distance and get top 5
        sosCallsWithDistances.sort((a, b) -> Double.compare(a.distance, b.distance));
        List<NearestSOSDialog.SOSCallWithDistance> nearest5 = sosCallsWithDistances.subList(0, Math.min(5, sosCallsWithDistances.size()));

        // Immediately highlight on map
        highlightNearestSOSCalls(nearest5, finalUserLocation);

        // Show dialog using separate class
        NearestSOSDialog.show(getContext(), nearest5, finalUserLocation, (sosCall, sosPoint) -> {
            // Disable follow location to prevent map from springing back to user location
            if (myLocationOverlay != null) {
                myLocationOverlay.disableFollowLocation();
            }

            // Find the corresponding marker for this SOS call
            Marker correspondingMarker = null;
            for (Marker marker : sosMarkers) {
                GeoPoint markerPos = marker.getPosition();
                if (Math.abs(markerPos.getLatitude() - sosCall.getX_coordinate()) < 0.0001 &&
                        Math.abs(markerPos.getLongitude() - sosCall.getY_coordinate()) < 0.0001) {
                    correspondingMarker = marker;
                    break;
                }
            }

            // Store selected SOS call and marker for accept button
            selectedSOSCall = sosCall;
            selectedMarker = correspondingMarker;

            // Show the accept case button
            if (btnAcceptCase != null) {
                btnAcceptCase.setText("Accept Case: " + sosCall.getUsername());
                btnAcceptCase.setVisibility(View.VISIBLE);
            }

            // Calculate and draw route
            if (routeManager != null && myLocationOverlay.getMyLocation() != null) {
                GeoPoint currentLocation = myLocationOverlay.getMyLocation();

                Toast.makeText(getContext(), "Calculating route...", Toast.LENGTH_SHORT).show();

                routeManager.drawRoute(currentLocation, sosPoint, new RouteManager.RouteCallback() {
                    @Override
                    public void onRouteCalculated(double distanceKm, double durationMinutes) {
                        Toast.makeText(getContext(),
                                String.format(java.util.Locale.getDefault(),
                                        "Route: %.1f km, ~%.0f min",
                                        distanceKm, durationMinutes),
                                Toast.LENGTH_LONG).show();

                        // Show return to location button
                        if (fabReturnToLocation != null) {
                            fabReturnToLocation.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onRouteError(String error) {
                        Toast.makeText(getContext(),
                                "Route calculation failed: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Navigate to the location
            controller.animateTo(sosPoint);
            controller.setZoom(15.0);

            // Force map refresh
            mapView.invalidate();
        });
    }

    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        // Haversine formula to calculate distance in meters
        double earthRadius = 6371000; // meters
        double lat1 = Math.toRadians(point1.getLatitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double deltaLat = Math.toRadians(point2.getLatitude() - point1.getLatitude());
        double deltaLon = Math.toRadians(point2.getLongitude() - point1.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    private void highlightNearestSOSCalls(List<NearestSOSDialog.SOSCallWithDistance> nearest5, GeoPoint userLocation) {
        // Reset all SOS markers to default red alert icon with transparency
        for (Marker marker : sosMarkers) {
            iconManager.resetToDefaultIcon(marker);
            // Remove any existing click listener
            marker.setOnMarkerClickListener(null);
        }

        // Highlight the nearest 5 SOS calls with star icon and full opacity
        for (NearestSOSDialog.SOSCallWithDistance item : nearest5) {
            for (Marker marker : sosMarkers) {
                GeoPoint markerPos = marker.getPosition();
                // Check if this marker matches the nearest SOS call (comparing with x=lat, y=lon)
                if (Math.abs(markerPos.getLatitude() - item.sosCall.getX_coordinate()) < 0.0001 &&
                        Math.abs(markerPos.getLongitude() - item.sosCall.getY_coordinate()) < 0.0001) {
                    // Highlight this marker with star icon
                    iconManager.highlightAsNearest(marker);
                    // Update the snippet to show distance
                    marker.setSnippet("User: " + item.sosCall.getUsername() +
                            "\nTime: " + item.sosCall.getTime() +
                            "\nDistance: " + String.format(Locale.getDefault(), "%.2f km", item.distance / 1000.0));

                    // Create custom info window for this marker
                    IconManager.CustomInfoWindow infoWindow = new IconManager.CustomInfoWindow(mapView, requireContext());

                    // Store references for callback
                    final Marker finalMarker = marker;
                    final SOSCall finalSosCall = item.sosCall;

                    // Set callback for when bubble "button" is clicked
                    infoWindow.setOnRouteClickListener(() -> {
                        // Calculate and draw route
                        if (routeManager != null && myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                            GeoPoint currentLocation = myLocationOverlay.getMyLocation();
                            GeoPoint destination = finalMarker.getPosition();

                            // Disable follow location
                            myLocationOverlay.disableFollowLocation();

                            Toast.makeText(getContext(), "Calculating route to " + finalSosCall.getUsername() + "...", Toast.LENGTH_SHORT).show();

                            routeManager.drawRoute(currentLocation, destination, new RouteManager.RouteCallback() {
                                @Override
                                public void onRouteCalculated(double distanceKm, double durationMinutes) {
                                    Toast.makeText(getContext(),
                                            String.format(Locale.getDefault(),
                                                    "Route to %s: %.1f km, ~%.0f min",
                                                    finalSosCall.getUsername(), distanceKm, durationMinutes),
                                            Toast.LENGTH_LONG).show();

                                    // Show return to location button
                                    if (fabReturnToLocation != null) {
                                        fabReturnToLocation.setVisibility(View.VISIBLE);
                                    }

                                    // Close info window after route is shown
                                    finalMarker.closeInfoWindow();
                                }

                                @Override
                                public void onRouteError(String error) {
                                    Toast.makeText(getContext(),
                                            "Route calculation failed: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            // Zoom to show both locations
                            controller.setZoom(14.0);
                        }
                    });

                    // Set the custom info window on the marker
                    marker.setInfoWindow(infoWindow);

                    // Click listener - double click shows accept case button
                    marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                        long currentTime = System.currentTimeMillis();

                        // Check if this is a double click (within 500ms on same marker)
                        if (lastClickedMarker == clickedMarker && (currentTime - lastClickTime) < 500) {
                            // Double click detected - show accept case button
                            selectedSOSCall = finalSosCall;
                            selectedMarker = finalMarker;

                            // Show the accept case button
                            if (btnAcceptCase != null) {
                                btnAcceptCase.setText("Accept Case: " + finalSosCall.getUsername());
                                btnAcceptCase.setVisibility(View.VISIBLE);
                            }

                            // Calculate and draw route
                            if (routeManager != null && myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                                GeoPoint currentLocation = myLocationOverlay.getMyLocation();
                                GeoPoint destination = clickedMarker.getPosition();

                                // Disable follow location
                                myLocationOverlay.disableFollowLocation();

                                Toast.makeText(getContext(), "Calculating route to " + finalSosCall.getUsername() + "...", Toast.LENGTH_SHORT).show();

                                routeManager.drawRoute(currentLocation, destination, new RouteManager.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, double durationMinutes) {
                                        Toast.makeText(getContext(),
                                                String.format(Locale.getDefault(),
                                                        "Route to %s: %.1f km, ~%.0f min",
                                                        finalSosCall.getUsername(), distanceKm, durationMinutes),
                                                Toast.LENGTH_LONG).show();

                                        // Show return to location button
                                        if (fabReturnToLocation != null) {
                                            fabReturnToLocation.setVisibility(View.VISIBLE);
                                        }

                                        // Close info window after route is shown
                                        clickedMarker.closeInfoWindow();
                                    }

                                    @Override
                                    public void onRouteError(String error) {
                                        Toast.makeText(getContext(),
                                                "Route calculation failed: " + error,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                                // Zoom to show both locations
                                controller.setZoom(14.0);
                            }

                            // Close the info window
                            clickedMarker.closeInfoWindow();

                            // Reset click tracking
                            lastClickTime = 0;
                            lastClickedMarker = null;
                            return true; // Event consumed
                        } else {
                            // First click - show info window and track for double click
                            clickedMarker.showInfoWindow();
                            lastClickTime = currentTime;
                            lastClickedMarker = clickedMarker;
                            return true; // Event consumed
                        }
                    });

                    break;
                }
            }
        }

        // Refresh the map to show the changes
        mapView.invalidate();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        updateUsernameDisplay();
        updateDateDisplay();
        loadStatistics();
        loadAndPlotSOSCalls();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
        if (routeManager != null) routeManager.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try SOS call again
                recordSOSLocation();
            } else {
                Toast.makeText(getContext(), "Location permission is required for SOS functionality", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void recordSOSLocation() {
        double latitude = 0;
        double longitude = 0;
        boolean locationFound = false;

        // Try 1: Get from myLocationOverlay
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            latitude = myLocationOverlay.getMyLocation().getLatitude();
            longitude = myLocationOverlay.getMyLocation().getLongitude();
            locationFound = true;
        }
        // Try 2: Get last known location from LocationManager
        else if (getContext() != null) {
            LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation == null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastLocation != null) {
                    latitude = lastLocation.getLatitude();
                    longitude = lastLocation.getLongitude();
                    locationFound = true;
                }
            }
        }

        if (!locationFound) {
            Toast.makeText(getContext(), "Unable to get current location. Please wait for GPS to acquire signal.", Toast.LENGTH_LONG).show();
            return;
        }

        // Record SOS call to Supabase
        final double finalLat = latitude;
        final double finalLon = longitude;
        SupabaseManager.INSTANCE.recordSOSCall(latitude, longitude, new SupabaseManager.AuthCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                if (success) {
                    Toast.makeText(getContext(), "SOS Emergency Call Initiated!\nLocation: " +
                            String.format(Locale.getDefault(), "%.6f, %.6f", finalLat, finalLon), Toast.LENGTH_LONG).show();
                    // Reload SOS calls to show the new marker
                    loadAndPlotSOSCalls();
                } else {
                    Toast.makeText(getContext(), "SOS call failed: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSearchAutocomplete() {
        searchResults = new ArrayList<>();
        suggestionsAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, new ArrayList<>());
        suggestionsList.setAdapter(suggestionsAdapter);
        searchHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacksAndMessages(null);
                if (s.length() > 2) {
                    searchHandler.postDelayed(() -> searchLocation(s.toString()), 500);
                } else {
                    suggestionsList.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
            SearchResult result = searchResults.get(position);
            moveToLocation(result.lat, result.lon, result.displayName);
            searchEditText.setText(result.displayName);
            suggestionsList.setVisibility(View.GONE);
            searchEditText.clearFocus();
            hideKeyboard();
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                // Cancel any pending search operations
                searchHandler.removeCallbacksAndMessages(null);
                suggestionsList.setVisibility(View.GONE);
                searchEditText.clearFocus();
                hideKeyboard();
                // Search first result if available
                if (!searchResults.isEmpty()) {
                    SearchResult result = searchResults.get(0);
                    moveToLocation(result.lat, result.lon, result.displayName);
                }
                return true;
            }
            return false;
        });

        // Close suggestions when search box loses focus
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                suggestionsList.setVisibility(View.GONE);
            }
        });
    }

    private void hideKeyboard() {
        if (getActivity() != null && searchEditText != null) {
            InputMethodManager imm = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
    }

    private void setupReturnToLocationButton() {
        if (fabReturnToLocation != null) {
            fabReturnToLocation.setOnClickListener(v -> {
                // Clear any active route
                if (routeManager != null) {
                    routeManager.clearRoute();
                }

                // Re-enable follow location
                if (myLocationOverlay != null) {
                    myLocationOverlay.enableFollowLocation();
                    if (myLocationOverlay.getMyLocation() != null) {
                        controller.animateTo(myLocationOverlay.getMyLocation());
                        controller.setZoom(15.0);
                    }
                }
                // Hide the button after returning to location
                fabReturnToLocation.setVisibility(View.GONE);

                // Also hide accept case button
                if (btnAcceptCase != null) {
                    btnAcceptCase.setVisibility(View.GONE);
                }

                Toast.makeText(getContext(), "Returned to your location", Toast.LENGTH_SHORT).show();
            });
        }
    }
    private void setupAcceptCaseButton() {
        if (btnAcceptCase != null) {
            btnAcceptCase.setOnClickListener(v -> {
                if (selectedSOSCall != null && selectedMarker != null) {
                    acceptSOSCase(selectedSOSCall, selectedMarker);
                    // Hide the button after accepting
                    btnAcceptCase.setVisibility(View.GONE);
                    selectedSOSCall = null;
                    selectedMarker = null;
                }
            });
        }
    }
    public void searchLocation(String query) {
        executor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String urlString = "https://nominatim.openstreetmap.org/search?format=json&q="
                        + encodedQuery + "&limit=5&addressdetails=1";

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", requireContext().getPackageName());
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray results = new JSONArray(response.toString());
                List<SearchResult> newResults = new ArrayList<>();
                List<String> displayNames = new ArrayList<>();

                for (int i = 0; i < results.length(); i++) {
                    JSONObject place = results.getJSONObject(i);
                    SearchResult result = new SearchResult(
                            place.getString("display_name"),
                            place.getDouble("lat"),
                            place.getDouble("lon")
                    );
                    newResults.add(result);
                    displayNames.add(result.displayName);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        searchResults.clear();
                        searchResults.addAll(newResults);
                        suggestionsAdapter.clear();
                        suggestionsAdapter.addAll(displayNames);
                        suggestionsAdapter.notifyDataSetChanged();
                        suggestionsList.setVisibility(displayNames.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                }

            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Search failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void moveToLocation(double lat, double lon, String name) {
        GeoPoint point = new GeoPoint(lat, lon);

        // Disable follow location when searching
        if (myLocationOverlay != null) {
            myLocationOverlay.disableFollowLocation();
        }

        controller.animateTo(point);
        controller.setZoom(17.0);

        // Show return to location button
        if (fabReturnToLocation != null) {
            fabReturnToLocation.setVisibility(View.VISIBLE);
        }

        // Remove previous search marker
        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
        }

        // Add new marker
        searchMarker = new Marker(mapView);
        searchMarker.setPosition(point);
        searchMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        searchMarker.setTitle(name);
        mapView.getOverlays().add(searchMarker);
        mapView.invalidate();
    }

    private static class SearchResult {
        String displayName;
        double lat;
        double lon;

        SearchResult(String displayName, double lat, double lon) {
            this.displayName = displayName;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private void acceptSOSCase(SOSCall sosCall, Marker marker) {
        if (sosCall.getId() == null) {
            Toast.makeText(getContext(), "Cannot accept case: Invalid SOS ID", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseManager.INSTANCE.deleteSOSCall(sosCall.getId(), new SupabaseManager.AuthCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "SOS case accepted. Tap map to clear.", Toast.LENGTH_LONG).show();
                            // Don't remove marker immediately - add to accepted list
                            acceptedMarkers.add(marker);
                            // Reload statistics only
                            loadStatistics();
                        } else {
                            Toast.makeText(getContext(), "Failed to accept case: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void showRouteToSOS(GeoPoint userLocation, GeoPoint sosLocation) {
        // Center map to show both points
        double centerLat = (userLocation.getLatitude() + sosLocation.getLatitude()) / 2;
        double centerLon = (userLocation.getLongitude() + sosLocation.getLongitude()) / 2;
        GeoPoint center = new GeoPoint(centerLat, centerLon);

        controller.animateTo(center);

        // Calculate appropriate zoom level
        double latDiff = Math.abs(userLocation.getLatitude() - sosLocation.getLatitude());
        double lonDiff = Math.abs(userLocation.getLongitude() - sosLocation.getLongitude());
        double maxDiff = Math.max(latDiff, lonDiff);

        int zoom = 15;
        if (maxDiff > 0.1) zoom = 11;
        else if (maxDiff > 0.05) zoom = 12;
        else if (maxDiff > 0.02) zoom = 13;
        else if (maxDiff > 0.01) zoom = 14;

        controller.setZoom((double) zoom);

        Toast.makeText(getContext(), "Showing route to SOS location", Toast.LENGTH_SHORT).show();
    }
}