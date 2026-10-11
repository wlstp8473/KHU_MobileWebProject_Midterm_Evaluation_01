package com.example.photoviewer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PostDetailActivity extends AppCompatActivity {
    private static final String EXTRA_POST = "post";

    public static Intent createIntent(Context context, PostItem post) {
        // Pass metadata and a cache path, not bitmap bytes, to the detail screen.
        return new Intent(context, PostDetailActivity.class).putExtra(EXTRA_POST, post);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PostItem post = (PostItem) getIntent().getSerializableExtra(EXTRA_POST);
        if (post == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_post_detail);
        View root = findViewById(R.id.detail_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        findViewById(R.id.detail_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.detail_title)).setText(
                post.title.isEmpty() ? getString(R.string.post_untitled) : post.title);
        ((TextView) findViewById(R.id.detail_body)).setText(
                post.text.isEmpty() ? getString(R.string.post_body_empty) : post.text);
        ((TextView) findViewById(R.id.detail_author)).setText(getString(R.string.post_author,
                post.author.isEmpty() ? getString(R.string.post_unknown) : post.author));
        String created = PostItem.formatDate(post.createdDate);
        String published = PostItem.formatDate(post.publishedDate);
        ((TextView) findViewById(R.id.detail_created)).setText(getString(R.string.post_created_date,
                created.isEmpty() ? getString(R.string.post_unknown) : created));
        ((TextView) findViewById(R.id.detail_published)).setText(published.isEmpty()
                ? getString(R.string.post_unpublished) : getString(R.string.post_published_date, published));
        ImageView image = findViewById(R.id.detail_image);
        Bitmap bitmap = decodeImage(post.imagePath);
        if (bitmap != null) image.setImageBitmap(bitmap);
        else {
            image.setImageResource(android.R.drawable.ic_menu_report_image);
            findViewById(R.id.detail_image_error).setVisibility(View.VISIBLE);
        }
    }

    static Bitmap decodeImage(String path) {
        if (path == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) return null;
        options.inSampleSize = 1;
        while (options.outWidth / options.inSampleSize > 1200
                || options.outHeight / options.inSampleSize > 1200) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }
}
