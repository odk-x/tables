package org.opendatakit.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.webkit.WebView;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.DisableAnimationsRule;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.UAUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WebkitStressTest {
  @ClassRule
  public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();
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
      super.afterActivityLaunched();

      onWebView().forceJavascriptEnabled();
    }
  };

  @Before
  public void setup() {
    UAUtils.assertInitSucess(initSuccess);
  }

  @Test
  public void launchSurvey() {
    //Open "Hope"
    EspressoUtils.delayedFindElement(Locator.ID, HOPE_TAB_ID, WEB_WAIT_TIMEOUT).perform(webClick());
    EspressoUtils.delayedFindElement(Locator.ID, LAUNCH_DEMO_ID, WEB_WAIT_TIMEOUT)
        .perform(webClick());

    boolean run = false;

    while (run) {
      UiObject2 surveyIcon = mDevice
          .wait(Until.findObject(By.res(TABLES_PKG_NAME, "menu_edit_row")), OBJ_WAIT_TIMEOUT);

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
      mDevice.wait(Until.findObject(By.clazz(WebView.class)), APP_INIT_TIMEOUT);

      //Wait 10 secs for webpage to load
      try {
        Thread.sleep(10000);
      } catch (Exception e) {
      }

      //Go back to Tables
      mDevice.pressBack();
      mDevice.wait(Until.findObject(By.text("Ignore Changes")), OBJ_WAIT_TIMEOUT).click();
    }
  }
}
