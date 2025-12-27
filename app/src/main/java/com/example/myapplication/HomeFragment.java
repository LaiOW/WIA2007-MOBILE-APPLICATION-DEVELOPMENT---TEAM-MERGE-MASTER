package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
            result -> { });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            TextView tvUsernameTop = view.findViewById(R.id.tvUsernameTop);
            
            if (tvUsernameTop != null) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    String name = user.getEmail().split("@")[0];
                    tvUsernameTop.setText(name);
                }

                tvUsernameTop.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), ProfileActivity.class);
                    startActivity(intent);
                });
            }

            RecyclerView recyclerFeed = view.findViewById(R.id.recyclerFeed);
            recyclerFeed.setLayoutManager(new LinearLayoutManager(getContext()));

            postList = new ArrayList<>();
            adapter = new FeedAdapter(postList);
            recyclerFeed.setAdapter(adapter);

            DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
            postsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    postList.clear();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        Post post = postSnapshot.getValue(Post.class);
                        if (post != null) {
                            postList.add(0, post); 
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });

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
}