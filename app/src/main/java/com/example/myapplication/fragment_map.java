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

        // 🔹 必须先设置 user agent
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = view.findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);

        controller = mapView.getController();
        controller.setZoom(15.0);
        
        // 🔹 添加用户当前位置覆盖层
        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        
        myLocationOverlay = new MyLocationNewOverlay(provider, mapView);
        myLocationOverlay.enableMyLocation(); 
        myLocationOverlay.enableFollowLocation(); 
        myLocationOverlay.setDrawAccuracyEnabled(true);
        
        // 自定义 "You are here" 标记（带有箭头的图标）
        Bitmap personIcon = createPersonIconWithArrow();
        myLocationOverlay.setPersonIcon(personIcon);
        
        // 指向图标（如罗盘）也使用相同图标
        myLocationOverlay.setDirectionIcon(personIcon); 
        
        // 只有当第一次定位成功时，才将地图中心移动到用户位置
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

    // 绘制一个带有箭头和 "U R Here" 文本的图标
    private Bitmap createPersonIconWithArrow() {
        int width = 150;
        int height = 150;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 绘制箭头（红色三角形）
        Path arrowPath = new Path();
        arrowPath.moveTo(width / 2f, height / 2f); // 底部中心
        arrowPath.lineTo(width / 2f - 20, height / 2f + 40); // 左下
        arrowPath.lineTo(width / 2f, height / 2f - 40); // 顶部尖端
        arrowPath.lineTo(width / 2f + 20, height / 2f + 40); // 右下
        arrowPath.close();

        paint.setColor(Color.BLUE); // 箭头颜色
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, paint);

        // 绘制圆形标记
        paint.setColor(Color.BLUE);
        canvas.drawCircle(width / 2f, height / 2f, 15, paint);
        
        // 绘制文本 "U R Here"
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        // 在图标上方绘制文字
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
