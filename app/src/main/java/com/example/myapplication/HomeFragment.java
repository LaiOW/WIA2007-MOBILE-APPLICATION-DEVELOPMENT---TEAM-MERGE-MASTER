package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FeedAdapter adapter;
    private List<Post> postList;
    private ActivityResultLauncher<Intent> createPostLauncher;
    private ShapeableImageView ivProfilePicHome;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPostLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Refresh posts when returning from creating a post
                    fetchPosts();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            ivProfilePicHome = view.findViewById(R.id.ivProfilePicHome);

            if (ivProfilePicHome != null) {
                updateProfilePicDisplay();

                ivProfilePicHome.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), ProfileActivity.class);
                    startActivity(intent);
                });
            }

            RecyclerView recyclerFeed = view.findViewById(R.id.recyclerFeed);
            recyclerFeed.setLayoutManager(new LinearLayoutManager(getContext()));

            postList = new ArrayList<>();
            adapter = new FeedAdapter(postList);
            recyclerFeed.setAdapter(adapter);

            fetchPosts();

            FloatingActionButton fabNewPost = view.findViewById(R.id.fab_new_post);
            fabNewPost.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CreatePostActivity.class);
                createPostLauncher.launch(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "HomeFragment Crash: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh username when returning from ProfileActivity
        if (ivProfilePicHome != null) {
            updateProfilePicDisplay();
        }
    }

    private void updateProfilePicDisplay() {
        if (getContext() == null || ivProfilePicHome == null)
            return;

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String imageUriString = sharedPreferences.getString("profile_image_uri", null);

        if (imageUriString != null && !imageUriString.isEmpty()) {
            Picasso.get()
                    .load(imageUriString)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivProfilePicHome);
        } else {
            ivProfilePicHome.setImageResource(R.drawable.default_avatar);
        }
    }

    private void fetchPosts() {
        SupabaseManager.INSTANCE.getPosts(new SupabaseManager.DatabaseCallback<Post>() {
            @Override
            public void onSuccess(List<Post> data) {
                postList.clear();
                if (data != null) {
                    postList.addAll(data);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error fetching posts: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
