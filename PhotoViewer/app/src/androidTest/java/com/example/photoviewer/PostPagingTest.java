package com.example.photoviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
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
public class PostPagingTest {
    private SharedPreferences preferences;
    private boolean hadMode, mode, hadOrder, order;

    @Before public void prepare() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences("display_settings", Context.MODE_PRIVATE);
        hadMode = preferences.contains("thumbnail_mode");
        mode = preferences.getBoolean("thumbnail_mode", false);
        hadOrder = preferences.contains("oldest_first");
        order = preferences.getBoolean("oldest_first", false);
        preferences.edit().putBoolean("thumbnail_mode", true).putBoolean("oldest_first", false).commit();
    }

    @After public void restore() {
        SharedPreferences.Editor editor = preferences.edit();
        if (hadMode) editor.putBoolean("thumbnail_mode", mode); else editor.remove("thumbnail_mode");
        if (hadOrder) editor.putBoolean("oldest_first", order); else editor.remove("oldest_first");
        editor.commit();
    }

    private List<PostItem> posts(int count) throws Exception {
        List<PostItem> posts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            PostItem post = new PostItem(new JSONObject().put("title", "페이징 테스트 " + i)
                    .put("text", i <= 7 ? "고양이 사진 기록" : "일상 기록")
                    .put("published_date", String.format(java.util.Locale.US,
                            "2026-10-%02dT12:00:00Z", i)));
            post.bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
            post.bitmap.eraseColor(Color.rgb(90 + i * 8, 90, 170));
            posts.add(post);
        }
        return posts;
    }

    private void assertPage(ActivityScenario<MainActivity> scenario, String label, int count, String first) {
        onView(withId(R.id.page_indicator)).check(matches(withText(label)));
        scenario.onActivity(activity -> {
            RecyclerView list = activity.findViewById(R.id.recyclerView);
            assertEquals(count, list.getAdapter().getItemCount());
            if (count > 0) {
                ImageAdapter.ImageViewHolder holder = (ImageAdapter.ImageViewHolder)
                        list.findViewHolderForAdapterPosition(0);
                assertNotNull(holder);
                assertEquals(first, holder.title.getText().toString());
            }
        });
    }

    // Optional emulator captures use only local fixtures; no server data is changed.
    private void capture(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("capturePaging"))) return;
        Context context = ApplicationProvider.getApplicationContext();
        File folder = new File(context.getExternalFilesDir(null), "paging-evidence");
        if (!folder.exists() && !folder.mkdirs()) throw new java.io.IOException("Cannot create evidence folder");
        Bitmap screen = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screen);
        try (FileOutputStream output = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(screen.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { screen.recycle(); }
    }

    @Test public void navigatesBoundariesAndReturnsFromDetailOnSamePage() throws Exception {
        List<PostItem> posts = posts(12);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.displayPosts(posts));
            assertPage(scenario, "1 / 3 페이지", 5, "페이징 테스트 12");
            onView(withId(R.id.page_previous)).check(matches(not(isEnabled())));
            capture("page-first");
            onView(withId(R.id.page_next)).perform(click());
            assertPage(scenario, "2 / 3 페이지", 5, "페이징 테스트 7");
            capture("page-second");
            onView(withText("페이징 테스트 7")).perform(click());
            onView(withId(R.id.detail_body)).check(matches(withText("고양이 사진 기록")));
            pressBack();
            assertPage(scenario, "2 / 3 페이지", 5, "페이징 테스트 7");
            scenario.recreate();
            assertPage(scenario, "2 / 3 페이지", 5, "페이징 테스트 7");
            onView(withId(R.id.view_mode_large)).perform(click());
            assertPage(scenario, "2 / 3 페이지", 5, "페이징 테스트 7");
            onView(withId(R.id.view_mode_thumbnail)).perform(click());
            onView(withId(R.id.page_next)).perform(click());
            assertPage(scenario, "3 / 3 페이지", 2, "페이징 테스트 2");
            onView(withId(R.id.page_next)).check(matches(not(isEnabled())));
            capture("page-last");
            onView(withId(R.id.page_previous)).perform(click());
            assertPage(scenario, "2 / 3 페이지", 5, "페이징 테스트 7");
        }
    }

    @Test public void searchesWholeListAndResetsPageOnSearchAndSort() throws Exception {
        List<PostItem> posts = posts(12);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.displayPosts(posts));
            onView(withId(R.id.page_next)).perform(click());
            onView(withId(R.id.search_input)).perform(replaceText("고양이"), closeSoftKeyboard());
            assertPage(scenario, "1 / 2 페이지", 5, "페이징 테스트 7");
            onView(withId(R.id.search_summary)).check(matches(withText("검색 결과 7개")));
            capture("page-search");
            onView(withId(R.id.page_next)).perform(click());
            assertPage(scenario, "2 / 2 페이지", 2, "페이징 테스트 2");
            onView(withId(R.id.sort_order)).perform(click());
            onData(allOf(is(instanceOf(String.class)), is("오래된순"))).perform(click());
            assertPage(scenario, "1 / 2 페이지", 5, "페이징 테스트 1");
            onView(withId(R.id.search_clear)).perform(click());
            assertPage(scenario, "1 / 3 페이지", 5, "페이징 테스트 1");
            onView(withId(R.id.search_input)).perform(replaceText("없는검색어"), closeSoftKeyboard());
            assertPage(scenario, "0 / 0 페이지", 0, null);
            onView(withId(R.id.page_previous)).check(matches(not(isEnabled())));
            onView(withId(R.id.page_next)).check(matches(not(isEnabled())));
        }
    }

    @Test public void clampsPageAfterSyncAndHandlesExactSizeAndEmptyList() throws Exception {
        List<PostItem> twelve = posts(12), six = posts(6), five = posts(5), ten = posts(10);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertPage(scenario, "0 / 0 페이지", 0, null);
            scenario.onActivity(activity -> activity.displayPosts(twelve));
            onView(withId(R.id.page_next)).perform(click(), click());
            scenario.onActivity(activity -> activity.displayPosts(six));
            assertPage(scenario, "2 / 2 페이지", 1, "페이징 테스트 1");
            scenario.onActivity(activity -> activity.displayPosts(ten));
            assertPage(scenario, "2 / 2 페이지", 5, "페이징 테스트 5");
            scenario.onActivity(activity -> activity.displayPosts(five));
            assertPage(scenario, "1 / 1 페이지", 5, "페이징 테스트 5");
            onView(withId(R.id.page_previous)).check(matches(not(isEnabled())));
            onView(withId(R.id.page_next)).check(matches(not(isEnabled())));
            scenario.onActivity(activity -> activity.displayPosts(java.util.Collections.emptyList()));
            assertPage(scenario, "0 / 0 페이지", 0, null);
        }
    }
}
