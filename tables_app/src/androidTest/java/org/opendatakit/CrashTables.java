package org.opendatakit;

import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import org.junit.*;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CrashTables {
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
  public void crashBy_statusColColorRule() {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());

    //Open "Edit status column color rules"
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(STATUS_COL_COLOR)).perform(click());

    //Click on first item (or any item, they all crash)
    EspressoUtils.getFirstItem().perform(click());

    //CRASH
  }

  /**
   * WARNING: This only works on 7" devices with 1280 x 800 screen, for example Nexus 7
   * TODO: make this work everywhere
   */
  @Test
  public void crashBy_longPressStatusCol() {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //Long press status column
    UAUtils.longPress(mDevice, 12, 155);
    //XOR
    //UAUtils.longPress(mDevice, 12, 125); //This presses on a different TabularView

    //CRASH
  }

  /**
   * This bug is caused by attempting to modify an unmodifiableList
   * so this bug doesn't happen in table color rule and column color rule
   *
   * Deleting one rule or adding a rule triggers the same bug.
   */
  @Test
  public void crashBy_resetStatusColColorRule() {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());

    //Open "Edit status column color rules"
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(STATUS_COL_COLOR)).perform(click());

    //Click reset and confirm
    onView(withId(R.id.menu_color_rule_list_revert)).perform(click());
    onView(withId(android.R.id.button1)).perform(click());

    //CRASH
  }

  @Test
  public void crashBy_2Finger() {
    //open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //click "Tea Houses Editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //go to table pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());

    //Create a new color rule and open text color dialog
    onData(withKey(TABLE_COLOR)).perform(click());
    onView(withId(R.id.menu_color_rule_list_new)).perform(click());
    onData(withKey(TABLE_COLOR_TEXT)).perform(click());

    //manipulate color picker with 2 fingers
    //pinch, drag, flick all work
    mDevice.wait(Until.findObject(By.clazz(View.class)), OBJ_WAIT_TIMEOUT).pinchOpen(1.0f);

    try {
      Thread.sleep(1000);
    } catch (Exception e) {}

    //CRASH
  }

  @Test
  public void crashBy_useGeneratedForm() {
    //open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //click "Tea Houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());

    //Attempt to add a row
    onView(withId(R.id.top_level_table_menu_add)).perform(click());

    //CRASH
  }

  @Test
  public void crashBy_rotateTablePrefRound2() throws RemoteException, InterruptedException {
    //open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //click "Tea Houses Editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //go to table pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());

    //go to "columns"
    onData(withKey(COLUMNS_LIST)).perform(click());

    mDevice.freezeRotation();
    mDevice.setOrientationRight();
    Thread.sleep(1000);
    mDevice.setOrientationNatural();
    Thread.sleep(1000);

    mDevice.pressBack();
  }
}
