package com.example.myapplication;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SupabaseManager.INSTANCE.isLoggedIn()) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            // Start the background SOS monitoring service
            Intent serviceIntent = new Intent(this,

                    SOSNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Intent intent = new Intent(getApplicationContext(), home_page.class);
            startActivity(intent);
            finish();
        }
    }
}
