package com.example.photoviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class PostDetailTest {
    private PostItem post(String title, String body) throws Exception {
        return new PostItem(new JSONObject().put("title", title).put("text", body)
                .put("author", 1).put("created_date", "2026-10-11T01:39:00.123456+09:00")
                .put("published_date", "2026-10-11T01:40:00+09:00"));
    }

    @Test
    public void longBodyScrollsAndSurvivesRecreation() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= 120; i++) body.append("긴 본문 ").append(i).append("번째 줄\n");
        PostItem post = post("긴 본문 테스트", body.toString());
        File image = File.createTempFile("detail-test-", ".png", context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(image)) {
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        post.imagePath = image.getAbsolutePath();
        try (ActivityScenario<PostDetailActivity> scenario =
                     ActivityScenario.launch(PostDetailActivity.createIntent(context, post))) {
            onView(withId(R.id.detail_title)).check(matches(withText(post.title)));
            onView(withId(R.id.detail_author)).check(matches(withText("작성자 ID: 1")));
            onView(withId(R.id.detail_body)).check(matches(withText(post.text)));
            onView(withId(R.id.detail_image_error)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.detail_scroll)).perform(swipeUp());
            scenario.onActivity(activity -> assertTrue(
                    ((ScrollView) activity.findViewById(R.id.detail_scroll)).getScrollY() > 0));
            scenario.recreate();
            onView(withId(R.id.detail_body)).check(matches(withText(post.text)));
        } finally {
            image.delete();
        }
    }

    @Test
    public void missingImageAndNullDatesStillShowPost() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PostItem post = new PostItem(new JSONObject().put("title", "이미지 없는 게시물")
                .put("text", "이미지가 없어도 읽을 수 있는 본문")
                .put("created_date", JSONObject.NULL).put("published_date", JSONObject.NULL));
        try (ActivityScenario<PostDetailActivity> scenario =
                     ActivityScenario.launch(PostDetailActivity.createIntent(context, post))) {
            onView(withId(R.id.detail_title)).check(matches(withText(post.title)));
            onView(withId(R.id.detail_published)).check(matches(withText(R.string.post_unpublished)));
            onView(withId(R.id.detail_created)).check(matches(withText("작성 날짜: 정보 없음")));
            onView(withId(R.id.detail_image_error)).check(matches(isDisplayed()));
            onView(withId(R.id.detail_body)).check(matches(withText(post.text)));
        }
    }

    @Test
    public void clickingTitleAndReturningKeepsListPosition() throws Exception {
        List<PostItem> posts = new ArrayList<>();
        for (int i = 0; i < 15; i++) posts.add(post("게시물 " + i, "본문 " + i));
        AtomicInteger position = new AtomicInteger();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.recyclerView);
                list.setAdapter(new ImageAdapter(posts,
                        item -> activity.startActivity(PostDetailActivity.createIntent(activity, item))));
                ((LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(7, 0);
            });
            onView(withText("게시물 7")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> position.set(((LinearLayoutManager)
                    ((RecyclerView) activity.findViewById(R.id.recyclerView)).getLayoutManager())
                    .findFirstVisibleItemPosition()));
            onView(withText("게시물 7")).perform(click());
            onView(withId(R.id.detail_body)).check(matches(withText("본문 7")));
            androidx.test.espresso.Espresso.pressBack();
            onView(withText("게시물 7")).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertEquals(position.get(), ((LinearLayoutManager)
                    ((RecyclerView) activity.findViewById(R.id.recyclerView)).getLayoutManager())
                    .findFirstVisibleItemPosition()));
        }
    }
}
