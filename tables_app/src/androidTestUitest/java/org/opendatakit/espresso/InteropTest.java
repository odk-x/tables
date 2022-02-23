package org.opendatakit.espresso;

import android.Manifest;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import java.net.MalformedURLException;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.opendatakit.util.TestConstants.HOPE_TAB_ID;
import static org.opendatakit.util.TestConstants.LAUNCH_DEMO_ID;
import static org.opendatakit.util.TestConstants.TABLE_MGR_WAIT;
import static org.opendatakit.util.TestConstants.T_HOUSE_E_TABLE_ID;
import static org.opendatakit.util.TestConstants.WEB_WAIT_TIMEOUT;

@LargeTest
public class InteropTest extends AbsBaseTest {

  private static final int WAIT = 1000;

  private Boolean initSuccess = null;
  private UiDevice mDevice;

  // don't annotate used in chain rule
  private IntentsTestRule<MainActivity> mIntentsRule = new IntentsTestRule<MainActivity>(
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
      .around(mIntentsRule);

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

    intended(ODKMatchers
        .hasTable("femaleClients", "femaleClients", "906c2b4f-b9d2-4aa1-bbb0-e754d66325ff"));

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
    } catch (Exception e) {
    }

    //Open "Tea houses editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //Click the 3rd row
    UAUtils.longPressSpreadsheetRow(mDevice, 3);

    //Edit the row
    onView(withText(EspressoUtils.getString(R.string.edit_row))).perform(click());

    intended(ODKMatchers
        .hasTable(T_HOUSE_E_TABLE_ID, T_HOUSE_E_TABLE_ID, "1ed5404f-c501-4308-ac0f-a080c13ae5c4"));

    Thread.sleep(WAIT);
  }
}