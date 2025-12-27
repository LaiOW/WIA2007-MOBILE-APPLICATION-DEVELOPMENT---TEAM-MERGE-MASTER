package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
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
        holder.user_name.setText(post.getUserName());
        
        if (post.getTitle() != null && !post.getTitle().isEmpty()) {
            holder.post_title.setText(post.getTitle());
            holder.post_title.setVisibility(View.VISIBLE);
        } else {
            holder.post_title.setVisibility(View.GONE);
        }

        holder.post_text.setText(post.getContent());

        if (post.getImageUri() != null && !post.getImageUri().isEmpty()) {
            holder.post_image.setVisibility(View.VISIBLE);
            // Use Glide to load the image from Firebase URL safely
            Picasso.get()
                 .load(post.getImageUri())
                 .placeholder(R.drawable.ic_launcher_background)
                 .into(holder.post_image);
        } else {
            holder.post_image.setVisibility(View.GONE);
        }

        holder.like_count.setText(String.valueOf(post.getLikeCount()));
        updateHeartIcon(holder.ic_heart, post.isLiked());

        holder.ic_heart.setOnClickListener(v -> {
            boolean newState = !post.isLiked();
            post.setLiked(newState);
            post.setLikeCount(post.getLikeCount() + (newState ? 1 : -1));
            holder.like_count.setText(String.valueOf(post.getLikeCount()));
            updateHeartIcon(holder.ic_heart, newState);
        });
    }

    private void updateHeartIcon(ImageView view, boolean isLiked) {
        if (isLiked) {
            view.setImageResource(R.drawable.ic_heart_filled);
        } else {
            view.setImageResource(R.drawable.ic_heart_outline);
        }
    }

    @Override
    public int getItemCount() { return postList.size(); }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView user_name, post_title, post_text, like_count;
        ImageView profile_image, post_image, ic_heart;
        Button btn_on_my_way;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            user_name = itemView.findViewById(R.id.user_name);
            post_title = itemView.findViewById(R.id.post_title);
            post_text = itemView.findViewById(R.id.post_text);
            profile_image = itemView.findViewById(R.id.profile_image);
            post_image = itemView.findViewById(R.id.post_image);
            ic_heart = itemView.findViewById(R.id.ic_heart);
            like_count = itemView.findViewById(R.id.like_count);
            btn_on_my_way = itemView.findViewById(R.id.btn_on_my_way);
        }
    }
}