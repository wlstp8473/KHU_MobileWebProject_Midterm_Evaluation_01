package com.example.photoviewer;

import android.graphics.Bitmap;
import org.json.JSONObject;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** A post returned by the existing Django API. The bitmap stays in the list screen. */
public class PostItem implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String title, text, author, createdDate, publishedDate, imageUrl;
    public String imagePath;
    public transient Bitmap bitmap;

    public PostItem(JSONObject json) {
        title = json.optString("title", "");
        text = json.optString("text", "");
        author = value(json, "author");
        createdDate = value(json, "created_date");
        publishedDate = value(json, "published_date");
        imageUrl = value(json, "image");
    }

    private static String value(JSONObject json, String key) {
        return json.isNull(key) ? "" : json.optString(key, "");
    }

    public boolean matchesQuery(String query) {
        String keyword = query.trim().toLowerCase(Locale.ROOT);
        return keyword.isEmpty() || title.toLowerCase(Locale.ROOT).contains(keyword)
                || text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    public static String formatDate(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            // DRF may include fractional seconds; show dates in the device timezone.
            String normalized = value.replaceFirst("\\.\\d+(?=Z|[+-]\\d{2}:\\d{2}$)", "");
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(input.parse(normalized));
        } catch (ParseException e) {
            return value;
        }
    }
}
