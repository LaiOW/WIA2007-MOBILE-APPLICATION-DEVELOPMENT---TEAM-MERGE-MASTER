package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CreatePostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        EditText editTextPostTitle = findViewById(R.id.edit_text_post_title);
        EditText editTextPostContent = findViewById(R.id.edit_text_post_content);
        Button buttonSubmitPost = findViewById(R.id.button_submit_post);

        buttonSubmitPost.setOnClickListener(v -> {
            String postTitle = editTextPostTitle.getText().toString();
            String postContent = editTextPostContent.getText().toString();

            if (!postTitle.isEmpty() && !postContent.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("postTitle", postTitle);
                resultIntent.putExtra("postContent", postContent);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Title and content can\'t be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }
}