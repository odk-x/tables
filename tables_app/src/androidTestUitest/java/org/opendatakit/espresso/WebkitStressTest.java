package org.opendatakit.espresso;

import android.Manifest;
import android.webkit.WebView;

import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.UAUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.opendatakit.util.TestConstants.APP_INIT_TIMEOUT;
import static org.opendatakit.util.TestConstants.HOPE_TAB_ID;
import static org.opendatakit.util.TestConstants.LAUNCH_DEMO_ID;
import static org.opendatakit.util.TestConstants.OBJ_WAIT_TIMEOUT;
import static org.opendatakit.util.TestConstants.TABLES_PKG_NAME;
import static org.opendatakit.util.TestConstants.WEB_WAIT_TIMEOUT;


@LargeTest
public class WebkitStressTest {

  private Boolean initSuccess = null;
  private UiDevice mDevice;

  // don't annotate used in chain rule
  private ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<MainActivity>(
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

  // don't annotate used in chain rule
  private GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION
  );

  @Rule
  public TestRule chainedRules = RuleChain
      .outerRule(grantPermissionRule)
      .around(mActivityRule);

  @Before
  public void setup() {
    UAUtils.assertInitSucess(initSuccess);
  }

  @Test
  public void launchSurvey() {
    boolean run = false;

    if (!run) {
      return;
    }

    //Open "Hope"
    EspressoUtils.delayedFindElement(Locator.ID, HOPE_TAB_ID, WEB_WAIT_TIMEOUT).perform(webClick());
    EspressoUtils.delayedFindElement(Locator.ID, LAUNCH_DEMO_ID, WEB_WAIT_TIMEOUT)
        .perform(webClick());

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
