package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private List<Post> postList;

    public FeedAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.txtUserName.setText(post.getUserName());
        holder.txtPostTitle.setText(post.getTitle());
        holder.txtContent.setText(post.getContent());
    }

    @Override
    public int getItemCount() { return postList.size(); }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName, txtPostTitle, txtContent;
        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtPostTitle = itemView.findViewById(R.id.txtPostTitle);
            txtContent = itemView.findViewById(R.id.txtContent);
        }
    }
}