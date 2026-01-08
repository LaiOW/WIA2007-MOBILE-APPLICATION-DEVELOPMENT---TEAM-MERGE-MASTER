package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private Uri selectedImageUri;
    private ShapeableImageView ivPreview;
    private Button buttonSubmitPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        TextInputEditText editTextPostTitle = findViewById(R.id.edit_text_post_title);
        TextInputEditText editTextPostContent = findViewById(R.id.edit_text_post_content);
        buttonSubmitPost = findViewById(R.id.button_submit_post);
        Button btnUploadImage = findViewById(R.id.btnUploadImage);
        ivPreview = findViewById(R.id.ivPreview);

        // Image Picker
        ActivityResultLauncher<String> pickImage = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivPreview.setImageURI(uri);
                        ivPreview.setVisibility(View.VISIBLE);
                    }
                }
        );

        btnUploadImage.setOnClickListener(v -> pickImage.launch("image/*"));

        buttonSubmitPost.setOnClickListener(v -> {
            String postTitle = editTextPostTitle.getText() != null ? editTextPostTitle.getText().toString() : "";
            String postContent = editTextPostContent.getText() != null ? editTextPostContent.getText().toString() : "";

            if (!postContent.isEmpty()) {
                // Disable button to prevent double clicks
                buttonSubmitPost.setEnabled(false);
                buttonSubmitPost.setText("Posting...");

                // Get Current User
                String currentUserEmail = SupabaseManager.INSTANCE.getCurrentUserEmail();
                String userName = (currentUserEmail != null) ? currentUserEmail : "Anonymous";
                if (userName.contains("@")) {
                    userName = userName.split("@")[0];
                }

                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri); 
                        uploadImageAndSavePost(userName, postTitle, postContent, inputStream);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
                        buttonSubmitPost.setEnabled(true);
                        buttonSubmitPost.setText("Post");
                    }
                } else {
                    savePostToDatabase(userName, postTitle, postContent, null);
                }

            } else {
                Toast.makeText(this, "Content can't be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageAndSavePost(String userName, String title, String content, InputStream inputStream) {
        // Use UUID for unique filename to prevent collisions and caching issues
        String fileName = UUID.randomUUID().toString() + ".jpg";
        SupabaseManager.INSTANCE.uploadImage(inputStream, fileName, new SupabaseManager.StorageCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                savePostToDatabase(userName, title, content, imageUrl);
            }

            @Override
            public void onError(String message) {
                buttonSubmitPost.setEnabled(true);
                buttonSubmitPost.setText("Post");
                Toast.makeText(CreatePostActivity.this, "Image Upload Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePostToDatabase(String userName, String title, String content, String imageUrl) {     
        Post post = new Post(userName, title, content, "General");
        if (imageUrl != null) {
            post.setImageUri(imageUrl);
        }

        // Get user profile image from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userProfileImage = sharedPreferences.getString("profile_image_uri", null);
        if (userProfileImage != null) {
            post.setUserProfileImage(userProfileImage);
        }

        SupabaseManager.INSTANCE.savePost(post, new SupabaseManager.DatabaseCallback<Post>() {
            @Override
            public void onSuccess(List<Post> data) {
                Toast.makeText(CreatePostActivity.this, "Post Published!", Toast.LENGTH_SHORT).show();    
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                buttonSubmitPost.setEnabled(true);
                buttonSubmitPost.setText("Post");
                Toast.makeText(CreatePostActivity.this, "Database Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
