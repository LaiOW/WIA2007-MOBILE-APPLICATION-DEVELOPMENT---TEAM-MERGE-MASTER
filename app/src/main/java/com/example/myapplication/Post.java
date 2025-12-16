package com.example.myapplication;

public class Post {
    private String userName;
    private String title;
    private String content;
    private String category;

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
}
