package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    // UI Components updated to match new XML types
    private ShapeableImageView ivProfilePic; // Changed to ShapeableImageView
    private FloatingActionButton btnChangeImage; // Changed from TextView to FAB
    private TextInputEditText tvEmail; // Email is now inside an InputEditText
    private TextInputEditText etName, etBio;
    private TextView tvCert1, tvCert2, tvCert3;
    private Button btnSave, btnLogout;
    private LinearLayout badgeContainer;

    private SharedPreferences sharedPreferences;
    private Set<String> selectedCerts = new HashSet<>();
    private Map<String, Integer> certBadgeMap = new HashMap<>();

    private ActivityResultLauncher<String> pickImageLauncher;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Views (Updated IDs)
        ivProfilePic = findViewById(R.id.iv_profile_pic);
        btnChangeImage = findViewById(R.id.btn_change_image); // New ID for Camera Button

        tvEmail = findViewById(R.id.tv_email);
        etName = findViewById(R.id.et_name);
        etBio = findViewById(R.id.et_bio); // Updated ID from tv_bio to et_bio

        btnSave = findViewById(R.id.btn_save);
        btnLogout = findViewById(R.id.btn_logout);

        // Note: Ensure your XML has this LinearLayout if you want to see the badges
        badgeContainer = findViewById(R.id.ll_badges);

        tvCert1 = findViewById(R.id.tv_cert_1);
        tvCert2 = findViewById(R.id.tv_cert_2);
        tvCert3 = findViewById(R.id.tv_cert_3);

        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // Certificate badges mapping
        certBadgeMap.put("Helped more than 10 incidents", R.drawable.ic_heart);
        certBadgeMap.put("Have knowledge in First Aid", R.drawable.ic_stethoscope);
        certBadgeMap.put("Posted more than 10 incidents", R.drawable.ic_badge);

        // Load saved user info
        loadUserInfo();

        // Certificate click listener
        TextView.OnClickListener certClickListener = v -> {
            TextView tv = (TextView) v;
            String cert = tv.getText().toString();
            if (selectedCerts.contains(cert)) {
                selectedCerts.remove(cert);
                tv.setSelected(false);
            } else {
                selectedCerts.add(cert);
                tv.setSelected(true);
            }
            updateBadges();
        };

        tvCert1.setOnClickListener(certClickListener);
        tvCert2.setOnClickListener(certClickListener);
        tvCert3.setOnClickListener(certClickListener);

        // Save profile
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
            String bioText = etBio.getText() != null ? etBio.getText().toString().trim() : "";

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("display_name", newName);
            editor.putString("bio", bioText);
            editor.putString("certificates", String.join(",", selectedCerts));
            editor.apply();

            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
        });

        // Logout
        btnLogout.setOnClickListener(v -> {
            SupabaseManager.INSTANCE.signOut((success, message) -> {
                if (success) {
                    Intent intent = new Intent(ProfileActivity.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, "Logout failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Image picker setup
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivProfilePic.setImageURI(uri);
                        sharedPreferences.edit().putString("profile_image_uri", uri.toString()).apply();
                    }
                }
        );

        // Change profile image click (Now attached to the FloatingActionButton)
        btnChangeImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void loadUserInfo() {
        String email = SupabaseManager.INSTANCE.getCurrentUserEmail();
        if (email != null) {
            tvEmail.setText(email);

            // Load name
            String savedName = sharedPreferences.getString("display_name", email.split("@")[0]);
            etName.setText(savedName);

            // Load bio
            String savedBio = sharedPreferences.getString("bio", "");
            etBio.setText(savedBio);

            // Load profile image
            String imageUriString = sharedPreferences.getString("profile_image_uri", null);
            if (imageUriString != null) {
                ivProfilePic.setImageURI(Uri.parse(imageUriString));
            }

            // Load certificates
            String savedCerts = sharedPreferences.getString("certificates", "");
            if (!savedCerts.isEmpty()) {
                String[] certArray = savedCerts.split(",");
                for (String c : certArray) {
                    selectedCerts.add(c);
                    if (c.equals(tvCert1.getText().toString())) tvCert1.setSelected(true);
                    if (c.equals(tvCert2.getText().toString())) tvCert2.setSelected(true);
                    if (c.equals(tvCert3.getText().toString())) tvCert3.setSelected(true);
                }
            }

            // Show badges
            updateBadges();
        } else {
            tvEmail.setText("Not logged in");
            btnLogout.setText("Go to Login");
            btnLogout.setOnClickListener(v -> {
                startActivity(new Intent(this, Login.class));
                finish();
            });
        }
    }

    private void updateBadges() {
        // Safety check in case you didn't add the linear layout back to XML
        if (badgeContainer == null) return;

        badgeContainer.removeAllViews();
        for (String c : selectedCerts) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
            params.setMargins(8, 0, 8, 0);
            card.setLayoutParams(params);
            card.setRadius(24);
            card.setCardElevation(4);
            // Ensure no extra padding interferes with our custom internal layout
            card.setContentPadding(0,0,0,0);

            // Set card background color based on certificate
            switch (c) {
                case "Helped more than 10 incidents":
                    card.setCardBackgroundColor(0xFFFFA07A); // light red
                    break;
                case "Have knowledge in First Aid":
                    card.setCardBackgroundColor(0xFFADD8E6); // light blue
                    break;
                case "Posted more than 10 incidents":
                    card.setCardBackgroundColor(0xFFFFE082); // light yellow
                    break;
                default:
                    card.setCardBackgroundColor(0xFFB3E5FC); // default very light blue
            }

            LinearLayout innerLayout = new LinearLayout(this);
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));
            innerLayout.setGravity(android.view.Gravity.CENTER);

            // Safety check for map values
            Integer drawableId = certBadgeMap.get(c);
            if (drawableId != null) {
                ImageView badgeIcon = new ImageView(this);
                badgeIcon.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
                badgeIcon.setImageResource(drawableId);
                badgeIcon.setPadding(8, 8, 8, 8);
                badgeIcon.setColorFilter(0xFFFFFFFF); // White icon
                innerLayout.addView(badgeIcon);
            }

            TextView label = new TextView(this);
            // Make label shorter for the small card if needed
            label.setText("Badge");
            label.setTextSize(10f);
            label.setTextColor(0xFFFFFFFF);
            label.setGravity(android.view.Gravity.CENTER);

            innerLayout.addView(label);
            card.addView(innerLayout);
            badgeContainer.addView(card);
        }
    }
}