package com.example.photoviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Spinner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class PostSortTest {
    private SharedPreferences preferences;
    private boolean hadPreference, originalOrder;

    @Before public void prepare() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences("display_settings", Context.MODE_PRIVATE);
        hadPreference = preferences.contains("oldest_first");
        originalOrder = preferences.getBoolean("oldest_first", false);
        preferences.edit().putBoolean("oldest_first", false).commit();
    }

    @After public void restore() {
        SharedPreferences.Editor editor = preferences.edit();
        if (hadPreference) editor.putBoolean("oldest_first", originalOrder);
        else editor.remove("oldest_first");
        editor.commit();
    }

    private PostItem post(String title, String date) throws Exception {
        return new PostItem(new JSONObject().put("title", title).put("text", "검색 대상")
                .put("published_date", date));
    }

    private List<PostItem> posts() throws Exception {
        return Arrays.asList(post("중간 게시물", "2026-10-10T12:00:00Z"),
                post("새 게시물", "2026-10-11T12:00:00Z"),
                post("이전 게시물", "2026-10-09T12:00:00Z"));
    }

    private void choose(String label) {
        onView(withId(R.id.sort_order)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is(label))).perform(click());
    }

    private void assertFirst(ActivityScenario<MainActivity> scenario, String title) {
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
        scenario.onActivity(activity -> {
            RecyclerView list = activity.findViewById(R.id.recyclerView);
            ImageAdapter.ImageViewHolder holder = (ImageAdapter.ImageViewHolder)
                    list.findViewHolderForAdapterPosition(0);
            assertNotNull(holder);
            assertEquals(title, holder.title.getText().toString());
        });
    }

    @Test public void comparesInstantsAndKeepsMissingDatesLast() throws Exception {
        PostItem a = post("a", "2026-10-11T00:00:00+09:00");
        PostItem b = post("b", "2026-10-10T16:00:00.123456Z");
        PostItem c = post("c", "2026-10-10T16:00:00.1Z");
        PostItem equal = post("equal", "2026-10-10T15:00:00Z");
        PostItem missing = post("missing", "");
        PostItem invalid = post("invalid", "2026-02-30T00:00:00Z");
        List<PostItem> source = Arrays.asList(missing, a, b, c, equal, invalid);
        List<PostItem> sorted = new ArrayList<>(source);
        PostSorter.sort(sorted, false);
        assertEquals(Arrays.asList(b, c, a, equal, missing, invalid), sorted);
        sorted = new ArrayList<>(source);
        PostSorter.sort(sorted, true);
        assertEquals(Arrays.asList(a, equal, c, b, missing, invalid), sorted);
        assertSame(missing, source.get(0));
    }

    @Test public void sortsSearchResultsBothViewsAndNewSync() throws Exception {
        List<PostItem> posts = posts();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.displayPosts(posts));
            assertFirst(scenario, "새 게시물");
            choose("오래된순");
            assertFirst(scenario, "이전 게시물");
            onView(withId(R.id.search_input)).perform(replaceText("검색"), closeSoftKeyboard());
            onView(withId(R.id.view_mode_thumbnail)).perform(click());
            assertFirst(scenario, "이전 게시물");
            choose("최신순");
            assertFirst(scenario, "새 게시물");
            onView(withText("새 게시물")).perform(click());
            onView(withId(R.id.detail_body)).check(matches(withText("검색 대상")));
            pressBack();
            onView(withId(R.id.search_input)).check(matches(withText("검색")));
            onView(withId(R.id.view_mode_large)).perform(click());
            assertFirst(scenario, "새 게시물");
            choose("오래된순");
            scenario.onActivity(activity -> activity.displayPosts(posts));
            assertFirst(scenario, "이전 게시물");
            onView(withId(R.id.search_summary)).check(matches(withText("검색 결과 3개")));
        }
    }

    @Test public void persistsOrderAcrossRecreationAndLaunch() throws Exception {
        List<PostItem> posts = posts();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.displayPosts(posts));
            choose("오래된순");
            scenario.recreate();
            assertFirst(scenario, "이전 게시물");
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals(1, ((Spinner) activity.findViewById(R.id.sort_order)).getSelectedItemPosition());
                activity.displayPosts(posts);
            });
            assertFirst(scenario, "이전 게시물");
        }
    }
}
