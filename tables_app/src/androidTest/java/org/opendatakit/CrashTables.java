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
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import org.junit.*;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.endsWith;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CrashTables {
  private Boolean initSuccess = null;
  private UiDevice mDevice;

  @BeforeClass
  public static void preCleanup() {
    //for crashBy_invalidCsvQualifier
    String csvDir = ODKFileUtils.getOutputCsvFolder(TableFileUtils.getDefaultAppName())
        + "/Tea_houses.TEST";
    if (new File(csvDir).exists()) {
      Log.e("TEST", "1 " + new File(csvDir + "/.csv").delete());
      Log.e("TEST", "2 " + new File(csvDir + "/.definition.csv").delete());
      Log.e("TEST", "3 " + new File(csvDir + "/.properties.csv").delete());
      Log.e("TEST", "4 " + new File(csvDir).delete());

      try {
        Thread.sleep(1000);
      } catch (Exception e) {/*ignore*/}
    }
  }

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

    //Click on first item (or any item, they all crash)
    onData(anything()).atPosition(0).perform(click());

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

    //Open "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID, T_HOUSE_E_DISPLAY_NAME)).perform(click());

    //Open Spreadsheet view
    onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
    onView(withText(R.string.spreadsheet)).perform(click());

    //Long press status column
    UAUtils.longPress(mDevice, 12, 155);
    //XOR
    //UAUtils.longPress(mDevice, 12, 125); //This presses on a different TabularView

    //CRASH
  }

  @Test
  public void crashBy_invalidCsvQualifier() {
    //Open export csv
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
    onView(withId(R.id.menu_table_manager_export)).perform(click());

    //open table chooser
    onView(withClassName(endsWith("Spinner"))).perform(click());

    //Choose a table
    onData(is("Tea houses")).inRoot(isPlatformPopup()).perform(click());

    //Enter an invalid qualifier
    onView(withClassName(endsWith("EditText"))).perform(typeText("TEST/"));

    //Click Export
    onView(withClassName(endsWith("Button"))).perform(click());

    //CRASH
  }

  /**
   * This bug applies to "Detail View File" and "Map List View File" as well
   */
  @Test
  public void crashBy_fileOutsideAppDirectory() {
    //Open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());

    //Open "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID, T_HOUSE_DISPLAY_NAME)).perform(click());

    //go to choose List View File
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(LIST_VIEW_FILE)).perform(click());

    //Wait for OI File Manager to start
    mDevice.wait(Until.findObject(By.res("android:id/home")), APP_START_TIMEOUT);

    //Go to a path outside app directory
    mDevice.findObject(By.res("org.openintents.filemanager:id/pathbar"))
        .findObject(By.clazz(ImageButton.class)).click();

    //Type in some file name and click "Pick file"
    //(the actual name doesn't matter, Tables doesn't care if the file exists or not)
    mDevice.findObject(By.clazz(EditText.class)).setText("filename");
    mDevice.findObject(By.text("Pick file")).click();

    //take advantage of Espresso's wait for idle feature
    //the actual Espresso call doesn't matter
    onView(withText("dummy"));

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
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID, T_HOUSE_DISPLAY_NAME)).perform(click());

    //Open "Edit status column color rules"
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(STATUS_COL_COLOR)).perform(click());

    //Click reset and confirm
    onView(withId(R.id.menu_color_rule_list_revert)).perform(click());
    onView(withId(android.R.id.button1)).perform(click());

    //CRASH
  }
}
