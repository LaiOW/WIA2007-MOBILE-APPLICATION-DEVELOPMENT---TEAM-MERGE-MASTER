package com.example.myapplication;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// 🔹 OSM
import org.osmdroid.config.Configuration;

public class home_page extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView tvUserAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.home_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🔹 初始化 osmdroid
        Configuration.getInstance()
                .setUserAgentValue(getPackageName());

        tvUserAddress = findViewById(R.id.tvUserAddress);

        updateDashboardStats();
        checkLocationPermission();

        // 🔹 在 home_page.xml 的 map_container 开 fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_container, new fragment_map())
                .commit();

        // 🔹 设置 FAB 点击事件
        FloatingActionButton fabFavorites = findViewById(R.id.fabFavorites);
        fabFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开 FavoritesFragment
                // 为了覆盖整个页面，我们可以用一个新的 container，或者简单的覆盖掉 map_container，
                // 但用户可能希望全屏展示收藏。
                // 观察布局，map_container只占一部分。
                // 通常跳转到新Fragment可以替换整个content view或者启动新Activity。
                // 这里我们替换R.id.main (ConstraintLayout) 的内容或者添加一个Fragment在上层。
                // 但R.id.main是ConstraintLayout，不能直接作为fragment container。
                // 更好的做法是：
                // 1. 如果是简单的跳转，可以启动一个新的Activity。
                // 2. 或者在布局中预留一个全屏的FrameLayout作为Fragment Container。
                // 3. 或者使用 add/replace 到 android.R.id.content (但这会覆盖ActionBar等，如果是AppCompatActivity)。
                
                // 鉴于当前架构，我将启动一个新的Fragment，替换掉 R.id.map_container，
                // 但map_container比较小。
                // 让我们尝试添加一个全屏的 container 或者 替换掉整个布局内容？
                // 为了简单起见，且不破坏现有布局结构，我将使用 addToBackStack 并尝试替换 R.id.map_container
                // 等等，用户说跳转到另一个Fragment。
                // 如果只替换 map_container，只有上面那一小块变了。
                // 建议：启动一个新的Activity来承载FavoritesFragment，或者修改布局以支持全屏Fragment切换。
                
                // 考虑到用户需求是“跳转”，且可能是全屏列表。
                // 我将在当前Activity中替换 map_container 看起来不太对劲因为那只是个地图框。
                // 我会选择替换 R.id.main 的内容，虽然 R.id.main 是 ConstraintLayout，但 FragmentTransaction 可以替换 View。
                // 不过替换 ConstraintLayout 可能会有问题。
                
                // 让我们简单点：创建一个新的 Activity `FavoritesActivity` 来展示这个 Fragment。
                // 或者，如果不允许新建Activity，我们可以在 home_page.xml 加一个全屏的 FrameLayout (elevation很高)，平时隐藏，用时显示。
                
                // 既然用户说是 Fragment，通常是在当前Activity管理。
                // 我将动态添加一个 Fragment 到 android.R.id.content，这样它会浮在最上面。
                
                getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new FavoritesFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            getUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getUserLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (location != null) {
                updateAddressUI(location);
            } else {
                tvUserAddress.setText("Location not found");
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void updateAddressUI(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0); // Get full address
                tvUserAddress.setText(addressText);
            } else {
                tvUserAddress.setText("Address not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvUserAddress.setText("Error getting address");
        }
    }

    private void updateDashboardStats() {

        TextView tvCasesCount = findViewById(R.id.tvCasesCount);
        TextView tvDateDay = findViewById(R.id.tvDateDay);
        TextView tvDateMonthYear = findViewById(R.id.tvDateMonthYear);
        TextView tvUserCount = findViewById(R.id.tvUserCount);

        Date currentDate = new Date();

        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.ENGLISH);
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);

        if (tvDateDay != null) {
            tvDateDay.setText(dayFormat.format(currentDate));
        }
        if (tvDateMonthYear != null) {
            tvDateMonthYear.setText(monthYearFormat.format(currentDate));
        }

        if (tvCasesCount != null) {
            tvCasesCount.setText("0");
        }
        if (tvUserCount != null) {
            tvUserCount.setText("0");
        }
    }
}
