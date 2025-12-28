package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is already logged in
        if (SupabaseManager.INSTANCE.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class); // Changed home_page.class to MainActivity.class as per convention
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_landing);
    }
}
