package com.example.myapplication;

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

public class fragment_map extends Fragment {

    private MapView mapView;
    private IMapController controller;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // 🔹 必须先设置 user agent
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = view.findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);

        controller = mapView.getController();
        controller.setZoom(15.0);
        controller.setCenter(new GeoPoint(3.1207, 101.6544)); // Universiti Malaya

        return view;
    }

    public void updateMapLocation(double latitude, double longitude) {
        if (mapView != null && controller != null) {
            GeoPoint point = new GeoPoint(latitude, longitude);
            controller.setCenter(point);
            controller.setZoom(18.0); // 搜索到位置后稍微放大一点
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
