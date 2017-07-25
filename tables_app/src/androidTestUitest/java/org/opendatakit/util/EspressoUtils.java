package org.opendatakit.util;

import android.app.Activity;
import android.app.Instrumentation;
import android.preference.Preference;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.sugar.Web;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.opendatakit.tables.R;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static org.hamcrest.Matchers.*;

public class EspressoUtils {
  /**
   * Returns the String with Id id using an ActivityTestRule
   *
   * @param rule ActivityTestRule to get String from
   * @param id   Id of String to retrieve
   * @return Returns the String
   */
  public static String getString(ActivityTestRule rule, int id, Object... formatArgs) {
    return rule.getActivity().getResources().getString(id, formatArgs);
  }

  public static String getString(ActivityTestRule rule, int id) {
    return rule.getActivity().getResources().getString(id);
  }

  public static String getString(IntentsTestRule rule, int id, Object... formatArgs) {
    return rule.getActivity().getResources().getString(id, formatArgs);
  }

  public static String getString(IntentsTestRule rule, int id) {
    return rule.getActivity().getResources().getString(id);
  }

  /**
   * THIS IS A TEMPORARY SOLUTION
   * TODO: implement an idling webView
   */
  public static Web.WebInteraction<Void> delayedFindElement(Locator locator, String value,
      int timeout) {
    final int waitTime = 200;
    Web.WebInteraction<Void> wInteraction = null;

    for (int i = 0; i < (timeout / waitTime) + 1; i++) {
      try {
        wInteraction = onWebView().withElement(findElement(locator, value));
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

  public static ViewAssertion dummyViewAssertion() {
    return new ViewAssertion() {
      @Override
      public void check(View view, NoMatchingViewException noView) {
        //Do nothing
      }
    };
  }

  public static boolean viewExists(Matcher<View> view) {
    final boolean[] exists = new boolean[1];

    onView(view).check(new ViewAssertion() {
      @Override
      public void check(View view, NoMatchingViewException noViewFoundException) {
        exists[0] = noViewFoundException == null;
      }
    });

    return exists[0];
  }

  public static DataInteraction getFirstItem() {
    return onData(anything()).atPosition(0);
  }

  public static String getPrefSummary(final String key) {
    final String[] summary = new String[1];
    final Matcher<String> keyMatcher = is(key);

    onData(new TypeSafeMatcher<Preference>() {
      @Override
      protected boolean matchesSafely(Preference item) {
        boolean match = keyMatcher.matches(item.getKey());

        if (match) {
          summary[0] = item.getSummary().toString();
        }

        return match;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(" preference with key matching: ");
        keyMatcher.describeTo(description);
      }
    }).check(dummyViewAssertion());

    return summary[0];
  }

  public static int getColor(Matcher<View> matcher, final int x, final int y) {
    final int[] color = new int[1];

    onView(matcher).check(new ViewAssertion() {
      @Override
      public void check(View view, NoMatchingViewException noViewFoundException) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);

        color[0] = view.getDrawingCache().getPixel(x, y);
      }
    });

    return color[0];
  }

  public static ViewInteraction toastMsgMatcher(IntentsTestRule rule, Matcher<String> matcher) {
    return onView(withText(matcher))
        .inRoot(withDecorView(not(is(rule.getActivity().getWindow().getDecorView()))))
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
}
