package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
