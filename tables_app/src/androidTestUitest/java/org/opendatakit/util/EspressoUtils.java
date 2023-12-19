package org.opendatakit.util;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.web.sugar.Web;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.opendatakit.tables.R;

public class EspressoUtils {
  /**
   * Returns the String with Id id using targetContext
   *
   * @param id   Id of String to retrieve
   * @return Returns the String
   */

  final static Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    public static String getString(int id, Object... formatArgs) {

      return context.getResources().getString(id, formatArgs);
}

public static String getString(int id) {
    return context.getResources().getString(id);

}

  public static Web.WebInteraction<Void> delayedFindElement(Locator locator,
                                                            String value,
                                                            int timeout) {
    return delayedFindElement(null, locator, value, timeout);
  }

  /**
   * THIS IS A TEMPORARY SOLUTION
   * TODO: implement an idling webView
   */
  public static Web.WebInteraction<Void> delayedFindElement(Matcher<View> webViewMatcher,
                                                            Locator locator,
                                                            String value,
                                                            int timeout) {
    final int waitTime = 200;
    Web.WebInteraction<Void> wInteraction = null;

    for (int i = 0; i < (timeout / waitTime) + 1; i++) {
      try {
        wInteraction = (webViewMatcher == null ? onWebView() : onWebView(webViewMatcher))
                .withElement(findElement(locator, value));
      } catch (Exception e) {
        //Ignored
      }

      if (wInteraction != null) {
        try {
          //force a wait, sometimes JS is too slow
          Thread.sleep(2 * waitTime);
        } catch (InterruptedException e) {
        }

        return wInteraction;
      }

      try {
        Thread.sleep(waitTime);
      } catch (Exception e) {
        //ignored
      }
    }

    return wInteraction;
  }

  public static void cancelExternalIntents() {
    intending(not(isInternal()))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));
  }

  public static boolean viewExists(Matcher<View> view) {
    final boolean[] exists = new boolean[1];

    onView(view).check((view1, noViewFoundException) -> exists[0] = noViewFoundException == null);

    return exists[0];
  }

  public static int getColor(Matcher<View> matcher, final int x, final int y) {
    final int[] color = new int[1];

    onView(matcher).check((view, noViewFoundException) -> {
      view.setDrawingCacheEnabled(true);
      view.buildDrawingCache(true);

      color[0] = view.getDrawingCache().getPixel(x, y);
    });

    return color[0];
  }

  public static void toastMsgMatcher(View decorView, Matcher<String> matcher) {
          onView(withText(matcher))
                  .inRoot(withDecorView(not(is(decorView))))
                  .check(matches(isDisplayed()));
  }

  public static void openTableManagerFromCustomHome() {
    if (!viewExists(withId(R.id.menu_web_view_activity_table_manager))) {
      throw new IllegalStateException("Not on custom home!");
    }

    int count = 0;
    int limit = 5;

    do {
      onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

      try {
        Thread.sleep(TestConstants.TABLE_MGR_WAIT);
      } catch (Exception e) {
        // ignore
      }
    } while (viewExists(withId(R.id.menu_web_view_activity_table_manager)) && (++count < limit));
  }

  public static ViewInteraction onRecyclerViewText(@StringRes int textId) {
    onView(isAssignableFrom(RecyclerView.class))
        .perform(RecyclerViewActions.scrollTo(hasDescendant(withText(textId))));

    return onView(withText(textId));
  }
}
