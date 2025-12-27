package com.example.myapplication;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                String userName = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail() : "Anonymous";
                if (userName.contains("@")) {
                    userName = userName.split("@")[0];
                }

                if (selectedImageUri != null) {
                    uploadImageAndSavePost(userName, postTitle, postContent, selectedImageUri);
                } else {
                    savePostToDatabase(userName, postTitle, postContent, null);
                }

            } else {
                Toast.makeText(this, "Content can't be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageAndSavePost(String userName, String title, String content, Uri imageUri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("post_images");
        StorageReference imageRef = storageRef.child(System.currentTimeMillis() + ".jpg");

        imageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    savePostToDatabase(userName, title, content, uri.toString());
                });
            })
            .addOnFailureListener(e -> {
                buttonSubmitPost.setEnabled(true);
                buttonSubmitPost.setText("Post");
                Toast.makeText(this, "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void savePostToDatabase(String userName, String title, String content, String imageUrl) {
        Post post = new Post(userName, title, content, "General");
        if (imageUrl != null) {
            post.setImageUri(imageUrl);
        }

        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
        String postId = postsRef.push().getKey();
        
        if (postId != null) {
            postsRef.child(postId).setValue(post)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post Published!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonSubmitPost.setEnabled(true);
                    buttonSubmitPost.setText("Post");
                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }
}