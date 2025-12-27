package com.example.myapplication;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// 🔹 OSM
import org.osmdroid.config.Configuration;

public class home_page extends AppCompatActivity implements FavoritesFragment.OnFavoriteSelectedListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView tvUserAddress;
    private EditText etSearch;

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
        etSearch = findViewById(R.id.ETsearch);

        updateDashboardStats();
        checkLocationPermission();

        // 🔹 在 home_page.xml 的 map_container 开 fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map_container, new fragment_map())
                    .commit();
        }

        // 🔹 设置 Search 输入监听
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                String addressString = etSearch.getText().toString();
                if (!addressString.isEmpty()) {
                    searchLocation(addressString, true); // True means show add to favorites dialog
                }
                
                // 隐藏键盘
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        // 🔹 设置 FAB 点击事件
        FloatingActionButton fabFavorites = findViewById(R.id.fabFavorites);
        fabFavorites.setOnClickListener(v -> getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, new FavoritesFragment())
                .addToBackStack(null)
                .commit());
                
        // 🔹 设置紧急呼叫按钮
        Button btnCall = findViewById(R.id.BTcall);
        btnCall.setOnClickListener(v -> showEmergencyDialog());
    }

    private void showEmergencyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_emergency_options, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Bind clicks
        CardView cardMedical = dialogView.findViewById(R.id.cardMedical);
        CardView cardAccident = dialogView.findViewById(R.id.cardAccident);
        CardView cardMental = dialogView.findViewById(R.id.cardMental);
        CardView cardOther = dialogView.findViewById(R.id.cardOther);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEmergency);

        cardMedical.setOnClickListener(v -> {
            handleEmergencySelection("Medical Emergency");
            dialog.dismiss();
        });

        cardAccident.setOnClickListener(v -> {
            handleEmergencySelection("Accident");
            dialog.dismiss();
        });

        cardMental.setOnClickListener(v -> {
            handleEmergencySelection("Mental Health Crisis");
            dialog.dismiss();
        });

        cardOther.setOnClickListener(v -> {
            dialog.dismiss(); // Close first dialog before opening input
            showOtherEmergencyInput();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    
    private void showOtherEmergencyInput() {
        final EditText input = new EditText(this);
        input.setHint("Describe emergency...");
        
        new AlertDialog.Builder(this)
                .setTitle("Other Emergency")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String description = input.getText().toString();
                    if (!description.isEmpty()) {
                        handleEmergencySelection("Other: " + description);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleEmergencySelection(String emergencyType) {
        Toast.makeText(this, "Emergency Reported: " + emergencyType, Toast.LENGTH_SHORT).show();
        // TODO: Implement actual call or report logic here
    }

    private void searchLocation(String locationName, boolean showAddToFavorites) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                String fullAddress = address.getAddressLine(0);

                // 获取 fragment_map 实例并更新位置
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map_container);
                if (fragment instanceof fragment_map) {
                    ((fragment_map) fragment).updateMapLocation(latitude, longitude);
                }
                
                Toast.makeText(this, "Found: " + fullAddress, Toast.LENGTH_SHORT).show();
                
                // 🔹 询问是否添加到收藏
                if (showAddToFavorites) {
                    showAddToFavoritesDialog(fullAddress);
                }

            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddToFavoritesDialog(String address) {
        new AlertDialog.Builder(this)
                .setTitle("Add to Favorites")
                .setMessage("Do you want to save this location?\n\n" + address)
                .setPositiveButton("Yes", (dialog, which) -> {
                    FavoritesManager.getInstance().addFavorite(address);
                    Toast.makeText(home_page.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
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

    @Override
    public void onFavoriteSelected(String address) {
        // 当从收藏列表选中地址时，复用搜索逻辑但 不 显示添加对话框
        etSearch.setText(address);
        searchLocation(address, false);
    }
}
