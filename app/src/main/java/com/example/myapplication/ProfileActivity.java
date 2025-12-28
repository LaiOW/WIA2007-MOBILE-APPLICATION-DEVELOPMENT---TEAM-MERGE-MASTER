package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvEmail;
    private TextInputEditText etName;
    private Button btnSave, btnLogout;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvEmail = findViewById(R.id.tv_email);
        etName = findViewById(R.id.et_name);
        btnSave = findViewById(R.id.btn_save);
        btnLogout = findViewById(R.id.btn_logout);

        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // Load User Info
        loadUserInfo();

        // Save Name functionality
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() != null ? etName.getText().toString() : "";
            if (!newName.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("display_name", newName);
                editor.apply();
                Toast.makeText(this, "Name saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout functionality
        btnLogout.setOnClickListener(v -> {
            SupabaseManager.INSTANCE.signOut(new SupabaseManager.AuthCallback() {
                @Override
                public void onComplete(boolean success, String message) {
                    if (success) {
                        // Clear local prefs on logout if desired
                        // sharedPreferences.edit().clear().apply(); 
                        
                        Intent intent = new Intent(ProfileActivity.this, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Logout failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void loadUserInfo() {
        String email = SupabaseManager.INSTANCE.getCurrentUserEmail();
        if (email != null) {
            tvEmail.setText(email);
            
            // Load saved name or default to email handle
            String savedName = sharedPreferences.getString("display_name", null);
            if (savedName == null) {
                if (email.contains("@")) {
                    savedName = email.split("@")[0];
                } else {
                    savedName = email;
                }
            }
            etName.setText(savedName);
        } else {
            // Not logged in?
            tvEmail.setText("Not logged in");
            btnLogout.setText("Go to Login");
            btnLogout.setOnClickListener(v -> {
                startActivity(new Intent(this, Login.class));
                finish();
            });
        }
    }
}
