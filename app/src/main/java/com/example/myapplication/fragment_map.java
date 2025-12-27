package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        
        // Smart Touch Handling: Allow navigation from edges, pan map in center
        mapView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            switch (action) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE:
                    // Define an edge zone (20% of the screen width)
                    int edgeZone = v.getWidth() / 5;
                    float x = event.getX();

                    // If touch is near the left or right edge, allow the ViewPager to intercept (Swipe Page)
                    if (x < edgeZone || x > v.getWidth() - edgeZone) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        // If touch is in the center, Lock the ViewPager (Pan Map)
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    break;
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // Reset on release
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false; // Return false to let the MapView process the touch as well
        });

        controller = mapView.getController();
        controller.setZoom(15.0);

        // Location Overlay
        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());

        myLocationOverlay = new MyLocationNewOverlay(provider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);

        // Custom Icon
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

    private Bitmap createPersonIconWithArrow() {
        int width = 150;
        int height = 150;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Arrow
        Path arrowPath = new Path();
        arrowPath.moveTo(width / 2f, height / 2f);
        arrowPath.lineTo(width / 2f - 20, height / 2f + 40);
        arrowPath.lineTo(width / 2f, height / 2f - 40);
        arrowPath.lineTo(width / 2f + 20, height / 2f + 40);
        arrowPath.close();

        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, paint);

        // Circle
        paint.setColor(Color.BLUE);
        canvas.drawCircle(width / 2f, height / 2f, 15, paint);

        // Text
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("You are here", width / 2f, height / 2f - 50, paint);

        return bitmap;
    }

    public void updateMapLocation(double latitude, double longitude) {
        if (mapView != null && controller != null) {
            GeoPoint point = new GeoPoint(latitude, longitude);

            if (myLocationOverlay != null && myLocationOverlay.isFollowLocationEnabled()) {
                myLocationOverlay.disableFollowLocation();
            }

            controller.setCenter(point);
            controller.setZoom(18.0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }
}