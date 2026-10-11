package com.example.photoviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter
        extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final List<PostItem> posts;
    private final OnPostClickListener listener;
    private final boolean thumbnailMode;

    public interface OnPostClickListener {
        void onPostClick(PostItem post);
    }

    public ImageAdapter(List<PostItem> posts, OnPostClickListener listener) {
        this(posts, listener, false);
    }

    public ImageAdapter(List<PostItem> posts, OnPostClickListener listener, boolean thumbnailMode) {
        this.posts = posts;
        this.listener = listener;
        this.thumbnailMode = thumbnailMode;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(thumbnailMode ? R.layout.item_post_thumbnail : R.layout.item_image, parent, false);

        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ImageViewHolder holder,
            int position
    ) {
        PostItem post = posts.get(position);
        if (post.bitmap != null) holder.imageView.setImageBitmap(post.bitmap);
        else holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        holder.title.setText(post.title.isEmpty()
                ? holder.itemView.getContext().getString(R.string.post_untitled) : post.title);
        String date = PostItem.formatDate(post.publishedDate);
        holder.date.setText(date.isEmpty()
                ? holder.itemView.getContext().getString(R.string.post_unpublished)
                : holder.itemView.getContext().getString(R.string.post_published_date, date));
        holder.title.setOnClickListener(v -> listener.onPostClick(post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public static class ImageViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView title, date;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(
                    R.id.imageViewItem
            );
            title = itemView.findViewById(R.id.post_title);
            date = itemView.findViewById(R.id.post_date);
        }
    }
}
