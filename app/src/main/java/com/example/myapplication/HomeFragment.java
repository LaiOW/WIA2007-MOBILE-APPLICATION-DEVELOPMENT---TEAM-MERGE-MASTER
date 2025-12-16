package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FeedAdapter adapter;
    private List<Post> postList;
    private ActivityResultLauncher<Intent> createPostLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPostLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String postTitle = data.getStringExtra("postTitle");
                    String postContent = data.getStringExtra("postContent");
                    postList.add(0, new Post("CurrentUser", postTitle, postContent, "General"));
                    adapter.notifyItemInserted(0);
                }
            });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerFeed = view.findViewById(R.id.recyclerFeed);
        recyclerFeed.setLayoutManager(new LinearLayoutManager(getContext()));

        postList = new ArrayList<>();
        // Add some dummy data
        postList.add(new Post("John Doe", "Welcome!", "This is a great community!", "General"));
        postList.add(new Post("Jane Smith", "Project Help", "I need help with my project.", "Help"));

        adapter = new FeedAdapter(postList);
        recyclerFeed.setAdapter(adapter);

        FloatingActionButton fabNewPost = view.findViewById(R.id.fab_new_post);
        fabNewPost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            createPostLauncher.launch(intent);
        });

        return view;
    }
}