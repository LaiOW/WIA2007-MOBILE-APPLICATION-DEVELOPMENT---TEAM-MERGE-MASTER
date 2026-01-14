package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.TrendingViewHolder> {

    private List<Post> trendingList;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public TrendingAdapter(List<Post> trendingList, OnCategoryClickListener listener) {
        this.trendingList = trendingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending, parent, false);
        return new TrendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
        Post post = trendingList.get(position);
        holder.txtCategory.setText(post.getCategory());
        holder.txtTitle.setText(post.getTitle());
        holder.txtDesc.setText(post.getContent());

        holder.itemView.setOnClickListener(v -> {
            listener.onCategoryClick(post.getCategory());
        });
    }

    @Override
    public int getItemCount() { return trendingList.size(); }

    static class TrendingViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory, txtTitle, txtDesc;
        public TrendingViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDesc = itemView.findViewById(R.id.txtDesc);
        }
    }
}