package Yu.code.toolbox.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class SettingsDialogButtonColorTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setup() {
        clearUserProfile();
    }

    private void clearUserProfile() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    @Test
    public void testEditUsernameDialogButtonsVisible() {
        navigateToSettings();

        onView(ViewMatchers.withId(R.id.item_username))
                .perform(scrollTo())
                .perform(click());

        onView(ViewMatchers.withText("保存"))
                .check(matches(isDisplayed()))
                .check(matches(ColorMatcher.withPrimaryColor()))
                .check(matches(ColorMatcher.notWhiteText()));

        onView(ViewMatchers.withText("取消"))
                .check(matches(isDisplayed()))
                .check(matches(ColorMatcher.withSecondaryTextColor()))
                .check(matches(ColorMatcher.notWhiteText()));
    }

    @Test
    public void testEditBioDialogButtonsVisible() {
        navigateToSettings();

        onView(ViewMatchers.withId(R.id.item_bio))
                .perform(scrollTo())
                .perform(click());

        onView(ViewMatchers.withText("保存"))
                .check(matches(isDisplayed()))
                .check(matches(ColorMatcher.withPrimaryColor()))
                .check(matches(ColorMatcher.notWhiteText()));

        onView(ViewMatchers.withText("取消"))
                .check(matches(isDisplayed()))
                .check(matches(ColorMatcher.withSecondaryTextColor()))
                .check(matches(ColorMatcher.notWhiteText()));
    }

    @Test
    public void testAvatarDialogButtonVisible() {
        navigateToSettings();

        onView(ViewMatchers.withId(R.id.item_avatar))
                .perform(scrollTo())
                .perform(click());

        onView(ViewMatchers.withText("确定"))
                .check(matches(isDisplayed()))
                .check(matches(ColorMatcher.withPrimaryColor()))
                .check(matches(ColorMatcher.notWhiteText()));
    }

    private void navigateToSettings() {
        onView(ViewMatchers.withId(R.id.nav_slideshow))
                .perform(click());

        onView(ViewMatchers.withId(R.id.item_settings))
                .perform(click());
    }
}