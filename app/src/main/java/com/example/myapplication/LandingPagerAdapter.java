package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class LandingPagerAdapter extends FragmentStateAdapter {

    public LandingPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new landing1();
            case 1:
            default:
                return new landing2();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // number of landing pages
    }
}

