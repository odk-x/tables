package org.opendatakit;

import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.core.deps.guava.primitives.Booleans;
import android.support.test.espresso.core.deps.guava.util.concurrent.ThreadFactoryBuilder;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.*;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class crashTables {
  private Boolean initSuccess = null;
  private UiDevice mDevice;

  @Rule
  public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<MainActivity>(
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
      onWebView().forceJavascriptEnabled();
    }
  };

  @Before
  public void setup() {
    assertThat("Initialization unsuccessful.", initSuccess, is(true));
  }

  @Test
  public void crashBy_changeDefaultViewGraph() {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID, T_HOUSE_DISPLAY_NAME)).perform(click());

    //go to default view type pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(DEFAULT_VIEW_TYPE)).perform(click());

    //choose Graph
    onView(withText("Graph")).perform(click());

    //CRASH
  }

  @Test
  @SuppressWarnings("ConstantConditions") //to get rid of AS's annoying highlighting
  public void crashBy_SIGSEGV() {
    //Open "Hope"
    EspressoUtils.delayedFindElement(Locator.ID, HOPE_TAB_ID, WEB_WAIT_TIMEOUT).perform(webClick());
    EspressoUtils.delayedFindElement(Locator.ID, LAUNCH_DEMO_ID, WEB_WAIT_TIMEOUT)
        .perform(webClick());

    while (true) {
      UiObject2 surveyIcon = mDevice.findObject(By.res(TABLES_PKG_NAME, "menu_edit_row"));

      if (surveyIcon == null) {
        //Click "Follow up..."
        EspressoUtils.delayedFindElement(Locator.ID, "button2", WEB_WAIT_TIMEOUT)
            .perform(webClick());

        //Click on a case
        EspressoUtils.delayedFindElement(Locator.CLASS_NAME, "item_space", WEB_WAIT_TIMEOUT)
            .perform(webClick());

        //Move to Survey
        onView(withId(R.id.menu_edit_row)).perform(click());
      } else {
        surveyIcon.click();
      }

      //Wait for Survey to start
      mDevice.wait(Until.findObject(By.res(SURVEY_PKG_NAME, "webkit_view")), APP_INIT_TIMEOUT);

      //Wait 10 secs for webpage to load
      try {
        Thread.sleep(10000);
      } catch (Exception e) {
      }

      //Go back to Tables
      mDevice.pressBack();
    }
  }

  @Test
  public void crashBy_rotateTablePref() throws RemoteException {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID, T_HOUSE_DISPLAY_NAME)).perform(click());

    //Open pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());

    //Rotate
    try {
      mDevice.freezeRotation();
      mDevice.setOrientationLeft();
    } finally {
      mDevice.setOrientationNatural();
    }

    //CRASH
  }

  @Test
  public void crashBy_statusColColorRule() {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID, T_HOUSE_DISPLAY_NAME)).perform(click());

    //Open "Edit status column color rules"
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(STATUS_COL_COLOR)).perform(click());

    //Click on first item
    onData(anything()).atPosition(0).perform(click());

    //CRASH
  }
}
