package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// 🔹 OSM
import org.osmdroid.config.Configuration;

public class home_page extends AppCompatActivity {

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

        updateDashboardStats();

        // 🔹 在 home_page.xml 的 map_container 开 fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_container, new fragment_map())
                .commit();
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
