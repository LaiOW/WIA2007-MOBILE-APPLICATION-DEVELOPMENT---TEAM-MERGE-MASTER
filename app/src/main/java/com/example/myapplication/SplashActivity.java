package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        android.widget.ImageView ivLogo = findViewById(R.id.iv_logo);
        if (ivLogo != null) {
            com.squareup.picasso.Picasso.get()
                    .load(R.drawable.ic_app_logo)
                    .resize(500, 500) // Downsample to reasonable size
                    .centerInside()
                    .into(ivLogo);
        }

        // Delay for 1 second (1000 milliseconds)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
            startActivity(intent);
            // Add shallow fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1000);
    }
}
