package com.example.photoviewer;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class PostSearchTest {
    private PostItem post(String title, String body) throws Exception {
        return new PostItem(new JSONObject().put("title", title).put("text", body));
    }

    @Test
    public void titleAndBodySearchClearAndDetailReturn() throws Exception {
        PostItem titleMatch = post("고양이 사진", "햇빛 아래에서 찍었습니다.");
        PostItem bodyMatch = post("오늘의 기록", "고양이와 산책했습니다.");
        PostItem other = post("강아지 사진", "공원에서 찍었습니다.");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.displayPosts(Arrays.asList(titleMatch, bodyMatch, other)));
            onView(withId(R.id.search_input)).perform(replaceText("  고양이  "), closeSoftKeyboard());
            onView(withId(R.id.search_summary)).check(matches(withText("검색 결과 2개")));
            scenario.onActivity(activity -> assertEquals(2, ((RecyclerView)
                    activity.findViewById(R.id.recyclerView)).getAdapter().getItemCount()));
            onView(withText("고양이 사진")).perform(click());
            onView(withId(R.id.detail_body)).check(matches(withText(titleMatch.text)));
            pressBack();
            onView(withId(R.id.search_input)).check(matches(withText("  고양이  ")));
            onView(withId(R.id.search_summary)).check(matches(withText("검색 결과 2개")));
            onView(withId(R.id.search_input)).perform(replaceText("없는검색어"), closeSoftKeyboard());
            onView(withId(R.id.search_summary)).check(matches(withText(R.string.search_empty)));
            scenario.onActivity(activity -> assertEquals(0, ((RecyclerView)
                    activity.findViewById(R.id.recyclerView)).getAdapter().getItemCount()));
            onView(withId(R.id.search_clear)).perform(click());
            onView(withId(R.id.search_input)).check(matches(withText("")));
            onView(withId(R.id.search_summary)).check(matches(withText("전체 게시물 3개")));
            scenario.onActivity(activity -> assertEquals(3, ((RecyclerView)
                    activity.findViewById(R.id.recyclerView)).getAdapter().getItemCount()));
        }
    }

    @Test
    public void retainsQueryAfterRecreationAndFiltersNewSyncedPosts() throws Exception {
        PostItem first = post("사진", "오늘의 기록");
        PostItem replacement = post("새 게시물", "오늘의 기록이 갱신되었습니다.");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.search_summary)).check(matches(withText(R.string.search_sync_first)));
            onView(withId(R.id.search_input)).perform(replaceText("기록"), closeSoftKeyboard());
            scenario.onActivity(activity -> activity.displayPosts(Collections.singletonList(first)));
            scenario.recreate();
            onView(withId(R.id.search_input)).check(matches(withText("기록")));
            onView(withId(R.id.search_summary)).check(matches(withText("검색 결과 1개")));
            scenario.onActivity(activity -> activity.displayPosts(Collections.singletonList(replacement)));
            onView(withText("새 게시물")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> activity.displayPosts(Collections.emptyList()));
            onView(withId(R.id.search_summary)).check(matches(withText(R.string.sync_empty)));
        }
    }

    @Test
    public void englishSearchIsCaseInsensitiveRegardlessOfDeviceLocale() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertTrue(post("IMAGE", "").matchesQuery(" image "));
            assertTrue(post("", "Photo Blog").matchesQuery("BLOG"));
            assertTrue(post("제목", "본문").matchesQuery("  "));
            assertFalse(post("강아지", "산책").matchesQuery("고양이"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
