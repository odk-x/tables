package org.opendatakit.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import java.net.MalformedURLException;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.*;
import static android.support.test.espresso.intent.matcher.UriMatchers.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.*;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SurveyInteropTest {
  private static final int WAIT = 1000;

  private Boolean initSuccess = null;
  private UiDevice mDevice;

  @Rule
  public IntentsTestRule<MainActivity> mActivityRule = new IntentsTestRule<MainActivity>(
      MainActivity.class) {
    @Override
    protected void beforeActivityLaunched() {
      super.beforeActivityLaunched();

      if (initSuccess == null) {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        initSuccess = UAUtils.turnOnCustomHome(mDevice);
      }
    }

    @Override
    protected void afterActivityLaunched() {
      super.afterActivityLaunched();

      onWebView().forceJavascriptEnabled();
    }
  };

  @Before
  public void setup() {
    UAUtils.assertInitSucess(initSuccess);
    EspressoUtils.cancelExternalIntents();
  }

  @Test
  public void intent_addRow() throws MalformedURLException, InterruptedException {
    //open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //click "Tea Houses Editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //click "plus" and check intent
    onView(withId(R.id.top_level_table_menu_add)).perform(click());

    //check intent
    intended(allOf(
        hasAction("android.intent.action.EDIT"),
        toPackage(SURVEY_PKG_NAME),
        hasData(allOf(
            hasHost("org.opendatakit.common.android.provider.forms"),
            hasPath("/tables/Tea_houses_editable/Tea_houses_editable/")
        ))
    ));

    //Some background tasks are slow (for example ColorRule), force a wait
    Thread.sleep(WAIT);
  }

  @Test
  public void intent_editRow() throws InterruptedException {
    //Open "Hope"
    EspressoUtils.delayedFindElement(Locator.ID, HOPE_TAB_ID, WEB_WAIT_TIMEOUT).perform(webClick());
    EspressoUtils.delayedFindElement(Locator.ID, LAUNCH_DEMO_ID, WEB_WAIT_TIMEOUT)
        .perform(webClick());

    //Click "Follow up..."
    EspressoUtils.delayedFindElement(Locator.ID, "button2", WEB_WAIT_TIMEOUT).perform(webClick());

    //Click on a case
    EspressoUtils.delayedFindElement(Locator.CLASS_NAME, "item_space", WEB_WAIT_TIMEOUT)
        .perform(webClick());

    //Move to Survey
    onView(withId(R.id.menu_edit_row)).perform(click());

    intended(allOf(
        hasAction("android.intent.action.EDIT"),
        toPackage(SURVEY_PKG_NAME),
        hasData("content://org.opendatakit.common.android.provider.forms"
            + "/tables/femaleClients/femaleClients/"
            + "#instanceId=906c2b4f-b9d2-4aa1-bbb0-e754d66325ff")
    ));

    //Some background tasks are slow (for example ColorRule), force a wait
    Thread.sleep(WAIT);
  }
}
