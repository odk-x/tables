package org.opendatakit.espresso;

import android.graphics.Rect;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.*;
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
    intended(ODKMatchers.hasTable(T_HOUSE_E_TABLE_ID, T_HOUSE_E_TABLE_ID, null));

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

    intended(ODKMatchers.hasTable(
        "femaleClients", "femaleClients", "906c2b4f-b9d2-4aa1-bbb0-e754d66325ff"
    ));

    //Some background tasks are slow (for example ColorRule), force a wait
    Thread.sleep(WAIT);
  }

  @Test
  public void intent_spreadsheetEditRow() throws InterruptedException {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //Find all views that can potentially be a tabular view
    List<UiObject2> tabularViews =
        mDevice.wait(Until.findObjects(By.clazz(View.class)), OBJ_WAIT_TIMEOUT);
    Map<UiObject2, Rect> bounds = new HashMap<>();

    //Find the relevant tabular views using their area
    UiObject2 minArea = null; //upper left corner
    UiObject2 maxArea = null; //main tabular view
    for (int i = 0; i < tabularViews.size(); i++) {
      UiObject2 view = tabularViews.get(i);

      if (view.getChildCount() == 0) {
        bounds.put(view, view.getVisibleBounds());

        if (maxArea == null || getArea(bounds.get(view)) > getArea(bounds.get(maxArea))) {
          maxArea = view;
        }
        if (minArea == null || getArea(bounds.get(view)) < getArea(bounds.get(minArea))) {
          minArea = view;
        }
      }
    }

    int rowHeight = bounds.get(minArea).height();

    //Long press the 3rd row
    UAUtils.longPress(mDevice, bounds.get(maxArea).centerX(), rowHeight * 3 - rowHeight / 2 +
        bounds.get(maxArea).top);

    onView(withText(EspressoUtils.getString(mActivityRule, R.string.edit_row))).perform(click());

    intended(ODKMatchers.hasTable(
        T_HOUSE_E_TABLE_ID, T_HOUSE_E_TABLE_ID, "1ed5404f-c501-4308-ac0f-a080c13ae5c4"
    ));

    Thread.sleep(WAIT);
  }

  private static int getArea(Rect rect) {
    return rect.width() * rect.height();
  }
}
