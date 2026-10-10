package com.example.photoviewer;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class UploadFormTest {
    @Test
    public void validatesRequiredFieldsBeforeNetworkRequest() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btn_save)).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            onView(withId(R.id.upload_title)).check(matches(hasErrorText("제목을 입력해주세요.")));
            onView(withId(R.id.upload_title)).perform(replaceText("UI 게시 테스트"), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());
            onView(withId(R.id.upload_text)).check(matches(hasErrorText("본문을 입력해주세요.")));
            onView(withId(R.id.upload_text)).perform(replaceText("사용자가 입력한 본문"), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());
            onView(withId(R.id.upload_error)).check(matches(withText(R.string.upload_image_required)));
        }
    }

    @Test
    public void keepsDraftAfterActivityRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btn_save)).perform(click());
            onView(withId(R.id.upload_title)).perform(replaceText("보존할 제목"), closeSoftKeyboard());
            onView(withId(R.id.upload_text)).perform(replaceText("보존할 본문"), closeSoftKeyboard());
            scenario.recreate();
            onView(withId(R.id.upload_title)).check(matches(withText("보존할 제목")));
            onView(withId(R.id.upload_text)).check(matches(withText("보존할 본문")));
        }
    }
}
