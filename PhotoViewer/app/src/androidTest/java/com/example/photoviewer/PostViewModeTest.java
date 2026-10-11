package com.example.photoviewer;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class PostViewModeTest {
    private SharedPreferences preferences;
    private boolean hadPreference, originalMode;

    @Before public void prepare() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences("display_settings", Context.MODE_PRIVATE);
        hadPreference = preferences.contains("thumbnail_mode");
        originalMode = preferences.getBoolean("thumbnail_mode", false);
        preferences.edit().putBoolean("thumbnail_mode", false).commit();
    }

    @After public void restorePreference() {
        SharedPreferences.Editor editor = preferences.edit();
        if (hadPreference) editor.putBoolean("thumbnail_mode", originalMode);
        else editor.remove("thumbnail_mode");
        editor.commit();
    }

    private List<PostItem> posts() throws Exception {
        List<PostItem> posts = new ArrayList<>();
        for (int i = 0; i < 12; i++) posts.add(new PostItem(new JSONObject()
                .put("title", "게시물 " + i).put("text", i < 3 ? "고양이 이야기" : "다른 이야기")));
        return posts;
    }

    private void assertImageSize(MainActivity activity, int expectedDp) {
        RecyclerView list = activity.findViewById(R.id.recyclerView);
        RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(
                ((LinearLayoutManager) list.getLayoutManager()).findFirstVisibleItemPosition());
        assertNotNull(holder);
        int expectedPx = Math.round(expectedDp * activity.getResources().getDisplayMetrics().density);
        assertEquals(expectedPx, holder.itemView.findViewById(R.id.imageViewItem).getLayoutParams().height);
    }

    @Test public void switchesLayoutKeepsSearchAndOpensDetail() throws Exception {
        List<PostItem> posts = posts();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.displayPosts(posts));
            onView(withId(R.id.view_mode_large)).check(matches(isChecked()));
            scenario.onActivity(activity -> assertImageSize(activity, 200));
            onView(withId(R.id.search_input)).perform(replaceText("고양이"), closeSoftKeyboard());
            onView(withId(R.id.view_mode_thumbnail)).perform(click());
            onView(withId(R.id.search_summary)).check(matches(withText("검색 결과 3개")));
            scenario.onActivity(activity -> assertImageSize(activity, 80));
            onView(withText("게시물 1")).perform(click());
            onView(withId(R.id.detail_body)).check(matches(withText("고양이 이야기")));
            pressBack();
            onView(withId(R.id.view_mode_thumbnail)).check(matches(isChecked()));
            onView(withId(R.id.search_input)).check(matches(withText("고양이")));
            onView(withId(R.id.view_mode_large)).perform(click());
            onView(withId(R.id.search_summary)).check(matches(withText("검색 결과 3개")));
            scenario.onActivity(activity -> assertImageSize(activity, 200));
        }
    }

    @Test public void keepsVisiblePostAndPersistsModeAcrossActivityLaunches() throws Exception {
        List<PostItem> posts = posts();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.displayPosts(posts);
                ((LinearLayoutManager) ((RecyclerView) activity.findViewById(R.id.recyclerView))
                        .getLayoutManager()).scrollToPositionWithOffset(5, -100);
            });
            onView(withText("게시물 5")).check(matches(isDisplayed()));
            onView(withId(R.id.view_mode_thumbnail)).perform(click());
            onView(withText("게시물 5")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertEquals(5, ((LinearLayoutManager)
                    ((RecyclerView) activity.findViewById(R.id.recyclerView)).getLayoutManager())
                    .findFirstVisibleItemPosition()));
            scenario.recreate();
            onView(withId(R.id.view_mode_thumbnail)).check(matches(isChecked()));
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.view_mode_thumbnail)).check(matches(isChecked()));
        }
    }
}
