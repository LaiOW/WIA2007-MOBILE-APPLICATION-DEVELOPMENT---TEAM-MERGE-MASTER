package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CategoryPostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_posts);

        String categoryName = getIntent().getStringExtra("categoryName");
        setTitle(categoryName);

        RecyclerView recyclerCategoryPosts = findViewById(R.id.recycler_category_posts);
        recyclerCategoryPosts.setLayoutManager(new LinearLayoutManager(this));

        // You would typically fetch posts for the given category from a database or API.
        // For this example, we'll create some dummy data.
        List<Post> postList = new ArrayList<>();
        postList.add(new Post("User1", "First Post", "Post about " + categoryName, categoryName));
        postList.add(new Post("User2", "Second Post", "Another post about " + categoryName, categoryName));

        FeedAdapter adapter = new FeedAdapter(postList);
        recyclerCategoryPosts.setAdapter(adapter);
    }
}
