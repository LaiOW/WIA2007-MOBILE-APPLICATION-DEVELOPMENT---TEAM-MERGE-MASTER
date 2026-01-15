package com.example.myapplication;

import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check GPS status
        checkGPSEnabled();
        
        // Immediate check
        if (checkLogin()) return;

        // Second check after a short delay to allow Supabase to load session from storage
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isDestroyed() && !isFinishing()) {
                if (checkLogin()) return;
                
                // If still not logged in, show the UI
                setContentView(R.layout.activity_landing);
            }
        }, 500);
    }

    private void checkGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager != null && 
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        
        if (!isGPSEnabled) {
            new AlertDialog.Builder(this)
                .setTitle("GPS Disabled")
                .setMessage("GPS is currently disabled. Please enable location services for better app experience.")
                .setPositiveButton("Enable GPS", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Some features may not work without GPS", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
        }
    }

    private boolean checkLogin() {
        if (SupabaseManager.INSTANCE.isLoggedIn()) {
            Intent intent = new Intent(this, home_page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return false;
    }
}
