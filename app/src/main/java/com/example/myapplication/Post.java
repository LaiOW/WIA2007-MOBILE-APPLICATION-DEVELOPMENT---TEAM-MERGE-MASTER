package com.example.myapplication;

public class Post {
    private String userName;
    private String title;
    private String content;
    private String category;
    private String imageUri; // Null if no image
    private int likeCount = 0;
    private boolean isLiked = false;

    public Post() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Post(String userName, String content, String category) {
        this.userName = userName;
        this.content = content;
        this.category = category;
    }

    public Post(String userName, String title, String content, String category) {
        this.userName = userName;
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public String getUserName() {
        return userName;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
}