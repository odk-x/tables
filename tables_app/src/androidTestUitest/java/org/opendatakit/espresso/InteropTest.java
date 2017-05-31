package org.opendatakit.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.support.test.filters.LargeTest;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.views.SpreadsheetView;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;
import org.opendatakit.util.DisableAnimationsRule;

import java.net.MalformedURLException;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.*;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.*;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class InteropTest {
  private static final int WAIT = 1000;

  private Boolean initSuccess = null;
  private UiDevice mDevice;

  @ClassRule
  public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

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
  }

  @Test
  public void intent_addRow() throws MalformedURLException, InterruptedException {
    EspressoUtils.cancelExternalIntents();
    EspressoUtils.openTableManagerFromCustomHome();

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
    EspressoUtils.cancelExternalIntents();

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
    EspressoUtils.cancelExternalIntents();

    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
    try {
      Thread.sleep(TABLE_MGR_WAIT);
    } catch (Exception e) {}

    //Open "Tea houses editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //Click the 3rd row
    UAUtils.longPressSpreadsheetRow(mDevice, 3);

    //Edit the row
    onView(withText(EspressoUtils.getString(mActivityRule, R.string.edit_row))).perform(click());

    intended(ODKMatchers.hasTable(
        T_HOUSE_E_TABLE_ID, T_HOUSE_E_TABLE_ID, "1ed5404f-c501-4308-ac0f-a080c13ae5c4"
    ));

    Thread.sleep(WAIT);
  }

  @Test
  public void crossApp_badFormId() {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //go to table pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());

    String currFormId = null;
    try {
      currFormId = FormType.constructFormType(Tables.getInstance(), APP_NAME, T_HOUSE_E_TABLE_ID)
          .getFormId();

      //change form id to something invalid
      onData(withKey(DEFAULT_FORM)).perform(click());
      onView(withId(R.id.edit_form_id))
          .perform(click())
          .perform(clearText())
          .perform(typeText("invalid_form_id"));
      onView(withId(android.R.id.button1)).perform(click());

      //attempt to add row
      pressBack();
      onView(withId(R.id.top_level_table_menu_add)).perform(click());

      //wait for Survey to start
      mDevice.wait(Until.hasObject(By.pkg(SURVEY_PKG_NAME).depth(0)), APP_START_TIMEOUT);
      mDevice.wait(Until.findObject(By.text("OK")), APP_INIT_TIMEOUT).click();

      //check that we're back to Tables
      onView(withClassName(is(SpreadsheetView.class.getName()))).check(matches(isDisplayed()));
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    } finally {
      //restore original formId
      try {
        FormType ft =
            FormType.constructFormType(Tables.getInstance(), APP_NAME, T_HOUSE_E_TABLE_ID);
        ft.setFormId(currFormId);
        ft.persist(Tables.getInstance(), APP_NAME, T_HOUSE_E_TABLE_ID);
      } catch (ServicesAvailabilityException e) {
        e.printStackTrace();
      }
    }
  }
}