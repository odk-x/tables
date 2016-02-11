package org.opendatakit.util;

import android.app.Activity;
import android.app.Instrumentation;
import android.support.annotation.Nullable;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.model.Atom;
import android.support.test.espresso.web.model.ElementReference;
import android.support.test.espresso.web.model.Evaluation;
import android.support.test.espresso.web.sugar.Web;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static org.hamcrest.Matchers.not;

public class EspressoUtils {
  /**
   * Returns the String with Id id using an ActivityTestRule
   *
   * @param rule     ActivityTestRule to get String from
   * @param id Id of String to retrieve
   * @return Returns the String
   */
  public static String getString(ActivityTestRule rule, int id) {
    return rule.getActivity().getResources().getString(id);
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

  public static void cancelInternalIntents() {
    intending(not(isInternal()))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));
  }
}
