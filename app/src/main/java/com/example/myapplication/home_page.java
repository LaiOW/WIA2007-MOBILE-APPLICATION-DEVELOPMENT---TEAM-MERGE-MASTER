package com.example.myapplication;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.Fragment;

public class home_page extends AppCompatActivity implements FavoritesFragment.OnFavoriteSelectedListener {

    private ViewPager2 viewPager;

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

        viewPager = findViewById(R.id.view_pager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Keep 1 page alive on either side (prevents Map fragment from being destroyed)
        viewPager.setOffscreenPageLimit(1);
        viewPager.setUserInputEnabled(true);
    }

    @Override
    public void onFavoriteSelected(String address) {
        // Switch to Map tab (index 0)
        viewPager.setCurrentItem(0, true);

        // Find the map fragment and trigger search
        // Since ViewPager2 manages fragments, we need to find it in the FragmentManager
        // A simple way is to iterate or check if we can get it via the adapter, 
        // but adapter creates new instances.
        
        // We can try to find it by type
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof fragment_map) {
                ((fragment_map) fragment).searchLocation(address);
                break;
            }
        }
    }
}
