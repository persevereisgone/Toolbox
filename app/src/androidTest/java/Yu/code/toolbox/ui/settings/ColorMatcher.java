package Yu.code.toolbox.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import Yu.code.toolbox.R;

public class ColorMatcher {

    public static Matcher<View> withTextColor(final int expectedColorResId) {
        return new TypeSafeMatcher<View>() {
            private int expectedColor;
            private int actualColor;

            @Override
            protected boolean matchesSafely(View view) {
                Context context = ApplicationProvider.getApplicationContext();
                expectedColor = context.getResources().getColor(expectedColorResId);
                
                if (view instanceof TextView) {
                    actualColor = ((TextView) view).getCurrentTextColor();
                    return actualColor == expectedColor;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with text color: " + Integer.toHexString(expectedColor)
                        + " (expected) but was: " + Integer.toHexString(actualColor));
            }
        };
    }

    public static Matcher<View> withPrimaryColor() {
        return withTextColor(R.color.app_primary);
    }

    public static Matcher<View> withSecondaryTextColor() {
        return withTextColor(R.color.app_text_secondary);
    }

    public static Matcher<View> notWhiteText() {
        return new TypeSafeMatcher<View>() {
            private int actualColor;

            @Override
            protected boolean matchesSafely(View view) {
                if (view instanceof TextView) {
                    actualColor = ((TextView) view).getCurrentTextColor();
                    return actualColor != Color.WHITE;
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("text color is white: " + Integer.toHexString(actualColor));
            }
        };
    }
}