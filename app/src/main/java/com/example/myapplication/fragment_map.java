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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class fragment_map extends Fragment {

    private MapView mapView;
    private IMapController controller;
    private MyLocationNewOverlay myLocationOverlay;
    private TextView tvUserName;

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

        // UI Overlay Elements
        tvUserName = view.findViewById(R.id.tvUserName);
        View cardProfile = view.findViewById(R.id.cardProfile);
        
        updateUsernameDisplay();

        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            startActivity(intent);
        });

        // Smart Touch Handling
        mapView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            switch (action) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE:
                    int edgeZone = v.getWidth() / 5;
                    float x = event.getX();
                    if (x < edgeZone || x > v.getWidth() - edgeZone) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
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
