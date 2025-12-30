package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class fragment_map extends Fragment {

    private MapView mapView;
    private IMapController controller;
    private MyLocationNewOverlay myLocationOverlay;
    private TextView tvUserName;
    private EditText searchEditText;
    private ListView suggestionsList;
    private ArrayAdapter<String> suggestionsAdapter;
    private List<SearchResult> searchResults;
    private Handler searchHandler;
    private ExecutorService executor;
    private Marker searchMarker;

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
        View cardProfile = view.findViewById(R.id.cardProfile);
        searchEditText = view.findViewById(R.id.ETsearch);
        suggestionsList = view.findViewById(R.id.suggestionsList);
        
        // Auto-select all text when clicking search bar
        searchEditText.setSelectAllOnFocus(true);
        
        updateUsernameDisplay();
        setupSearchAutocomplete();

        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            startActivity(intent);
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

        controller = mapView.getController();
        controller.setZoom(15.0);

        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        myLocationOverlay = new MyLocationNewOverlay(provider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);

        Bitmap personIcon = createPersonIconWithArrow();
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
        controller.setCenter(new GeoPoint(3.1207, 101.6544));

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

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        updateUsernameDisplay();
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

    private void searchLocation(String query) {
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
        controller.animateTo(point);
        controller.setZoom(17.0);

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

    private Bitmap createPersonIconWithArrow() {
        int width = 150; int height = 150;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Path arrowPath = new Path();
        arrowPath.moveTo(width / 2f, height / 2f);
        arrowPath.lineTo(width / 2f - 20, height / 2f + 40);
        arrowPath.lineTo(width / 2f, height / 2f - 40);
        arrowPath.lineTo(width / 2f + 20, height / 2f + 40);
        arrowPath.close();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, paint);
        paint.setColor(Color.BLUE);
        canvas.drawCircle(width / 2f, height / 2f, 15, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("You are here", width / 2f, height / 2f - 50, paint);
        return bitmap;
    }
}
