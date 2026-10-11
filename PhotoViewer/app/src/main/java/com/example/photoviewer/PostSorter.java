package com.example.photoviewer;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sort a filtered copy; leave the synced source and equal-date order unchanged. */
final class PostSorter {
    private static final Pattern DATE = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?(Z|[+-]\\d{2}:\\d{2})$");

    static void sort(List<PostItem> posts, boolean oldestFirst) {
        Map<PostItem, Long> dates = new IdentityHashMap<>();
        for (PostItem post : posts) dates.put(post, timestamp(post.publishedDate));
        posts.sort((left, right) -> {
            Long a = dates.get(left), b = dates.get(right);
            if (a == null) return b == null ? 0 : 1;
            if (b == null) return -1;
            return oldestFirst ? Long.compare(a, b) : Long.compare(b, a);
        });
    }

    private static Long timestamp(String value) {
        Matcher match = DATE.matcher(value);
        if (!match.matches()) return null;
        String fraction = match.group(2);
        String milliseconds = fraction == null ? "000" : (fraction + "000").substring(0, 3);
        String normalized = match.group(1) + "." + milliseconds + match.group(3);
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        parser.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date date = parser.parse(normalized, position);
        return date != null && position.getIndex() == normalized.length() ? date.getTime() : null;
    }
}
