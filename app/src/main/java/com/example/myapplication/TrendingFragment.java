package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TrendingFragment extends Fragment implements TrendingAdapter.OnCategoryClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trending, container, false);

        RecyclerView recyclerTrending = view.findViewById(R.id.recyclerTrending);
        recyclerTrending.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Post> trendingList = new ArrayList<>();
        // Add some dummy data
        trendingList.add(new Post("AndroidDev", "Jetpack Compose", "Check out the new Jetpack Compose features!", "Android"));
        trendingList.add(new Post("WebDev", "React Learning", "What is the best way to learn React?", "Web"));

        TrendingAdapter adapter = new TrendingAdapter(trendingList, this);
        recyclerTrending.setAdapter(adapter);

        return view;
    }

    @Override
    public void onCategoryClick(String categoryName) {
        Intent intent = new Intent(getActivity(), CategoryPostActivity.class);
        intent.putExtra("categoryName", categoryName);
        startActivity(intent);

    }
}