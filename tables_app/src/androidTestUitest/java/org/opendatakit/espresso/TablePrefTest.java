package org.opendatakit.espresso;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.exception.ServicesAvailabilityException;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.views.SpreadsheetView;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;
import org.opendatakit.util.DisableAnimationsRule;

import java.io.File;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.intent.Intents.*;
import static android.support.test.espresso.intent.matcher.IntentMatchers.*;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.web.assertion.WebViewAssertions.*;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.*;
import static org.hamcrest.Matchers.*;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TablePrefTest {
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
    EspressoUtils.cancelExternalIntents();

    //open table manager
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
    try {
      Thread.sleep(TABLE_MGR_TIMEOUT);
    } catch (Exception e) {}

    //click "Tea Houses Editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //go to table pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
  }

  @Test
  public void intents_launchOIFileManager() {
    //Check intent on "List View File"
    onData(withKey(LIST_VIEW_FILE)).perform(click());
    intended(hasAction(OI_PICK_FILE), Intents.times(1));

    //Check intent on "Detail View File"
    onData(withKey(DETAIL_VIEW_FILE)).perform(click());
    intended(hasAction(OI_PICK_FILE), Intents.times(2));

    //Check intent on "Map List View File"
    onData(withKey(MAP_LIST_VIEW_FILE)).perform(click());
    intended(hasAction(OI_PICK_FILE), Intents.times(3));
  }

  @Test
  public void views_changeDefaultViewType() {
    //undo what setup did, this one doesn't work with "Tea Houses Editable"
    pressBack();
    pressBack();

    //click "Tea houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());

    //go to view type pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(DEFAULT_VIEW_TYPE)).perform(click());

    //choose Spreadsheet
    onView(withText("Spreadsheet")).perform(click());

    //go to table manager
    Espresso.pressBack();
    Espresso.pressBack();

    //Check new default view type
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());
    try {
      onView(withClassName(is(SpreadsheetView.class.getName()))).check(matches(isDisplayed()));
    } finally {
      //restore default view type
      onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
      onData(withKey(DEFAULT_VIEW_TYPE)).perform(click());
      onView(withText("List")).perform(click());
    }
  }

  @Test
  public void views_columns() {
    final int numCol = 19;

    //go to columns
    onData(withKey(COLUMNS_LIST)).perform(click());

    //Check num of column
    onView(withId(android.R.id.list)).check(matches(ODKMatchers.withSize(numCol)));
  }

  @Test
  public void intent_tableLevelPref() {
    intended(allOf(
        hasComponent(TableLevelPreferencesActivity.class.getName()),
        hasExtra("tableId", T_HOUSE_E_TABLE_ID),
        hasExtra("tablePreferenceFragmentType", "TABLE_PREFERENCE")
    ));
  }

  @Test
  public void display_tableIdentifier() {
    //Check that display name and table id are shown correctly
    assertThat(EspressoUtils.getPrefSummary(TABLE_DISPLAY_NAME),
        is("\"" + T_HOUSE_E_DISPLAY_NAME + "\""));
    assertThat(EspressoUtils.getPrefSummary(TABLE_ID), is(T_HOUSE_E_TABLE_ID));
  }

  @Test
  public void display_columnIdentifier() {
    if (true) return;
    
    onData(withKey(COLUMNS_LIST)).perform(click());
    onData(is("House id")).perform(click());

    assertThat(EspressoUtils.getPrefSummary(COL_DISPLAY_NAME), is("House id"));
    assertThat(EspressoUtils.getPrefSummary(COL_KEY), is("House_id"));
  }

  @Test
  public void view_rotateScreen() throws RemoteException, InterruptedException {
    mDevice.freezeRotation();

    try {
      //see if this crashes Tables
      mDevice.setOrientationRight();
      Thread.sleep(2000);
    } finally {
      mDevice.setOrientationNatural();
      Thread.sleep(2000);
    }

    onData(withKey(COLUMNS_LIST)).perform(click());
    try {
      //see if this crashes Tables
      mDevice.setOrientationRight();
      Thread.sleep(2000);
    } finally {
      mDevice.setOrientationNatural();
      Thread.sleep(2000);
    }
    //check that we are still in "Columns"
    onData(is("House id")).check(matches(isCompletelyDisplayed()));
    onData(is("State")).check(matches(isCompletelyDisplayed()));
    onData(is("Region")).check(matches(isCompletelyDisplayed()));

    //see if this crashes tables
    pressBack();
    pressBack();

    Thread.sleep(2000);
}

  @Test
  public void intents_listView() {
    final String listViewPath =
        "/" + APP_NAME + "/config/tables/Tea_houses/html/Tea_houses_list.html";

    //backup current config
    String currFile = getListViewFile();

    //stub response
    intending(hasAction(OI_PICK_FILE)).respondWith(
        new Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            new Intent().setData(Uri.fromFile(new File(ODKFileUtils.getOdkFolder() + listViewPath)))
        )
    );

    //edit list view path
    onData(withKey(LIST_VIEW_FILE)).perform(click());

    //go to list view
    //minor problem, webkit is reloaded but new url not loaded after exiting pref
    pressBack();
    pressBack();
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());
    onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
    onView(withText("List")).perform(click());

    try {
      //check that the correct url is loaded
      onView(withId(R.id.webkit)).check(matches(ODKMatchers.withUrl(endsWith(listViewPath))));
    } finally {
      //restore config
      setListViewFile(currFile);
    }
  }

  @Test
  public void intents_detailView() {
    final String detailViewPath =
        "/" + APP_NAME + "/config/tables/Tea_houses/html/Tea_houses_detail.html";
    final String listViewPath =
        "/" + APP_NAME + "/config/tables/Tea_houses/html/Tea_houses_list.html";

    //back up current config
    String currDetailFile = getDetailViewFile();
    String currListFile = getListViewFile();

    //stub response
    intending(hasAction(OI_PICK_FILE)).respondWith(
        new Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            new Intent().setData(Uri.fromFile(new File(ODKFileUtils.getOdkFolder() + detailViewPath)))
        )
    );

    //edit detail view path
    onData(withKey(DETAIL_VIEW_FILE)).perform(click());

    intending(hasAction(OI_PICK_FILE)).respondWith(
        new Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            new Intent().setData(Uri.fromFile(new File(ODKFileUtils.getOdkFolder() + listViewPath)))
        )
    );

    //edit list view path
    onData(withKey(LIST_VIEW_FILE)).perform(click());

    //go to list view
    pressBack();
    pressBack();
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());
    onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
    onView(withText("List")).perform(click());

    //choose "Tea for All"
    EspressoUtils.delayedFindElement(
        Locator.ID, "924e548d-667f-4c49-91c0-de2885c0dda1", WEB_WAIT_TIMEOUT).perform(webClick());

    try {
      //check url
      onView(withId(R.id.webkit)).check(matches(ODKMatchers.withUrl(endsWith(detailViewPath))));

      try {
        Thread.sleep(2000); //need this for older devices
      } catch (Exception e) {}

      EspressoUtils.delayedFindElement(Locator.ID, "TITLE", WEB_WAIT_TIMEOUT)
          .check(webMatches(getText(), is("Tea for All")));
    } finally {
      setDetailViewFile(currDetailFile);
      setListViewFile(currListFile);
    }
  }

  @Test
  public void views_availableViewsOff() {
    //Backup config
    String currListFile = getListViewFile();
    String currMapFile = getMapViewFile();

    try {
      //turn off ListView and MapView
      setListViewFile(null);
      setMapViewFile(null);

      //quit preferences
      pressBack();

      //Check buttons
      onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
      onData(anything()).atPosition(1).check(matches(not(isEnabled())));
      onData(anything()).atPosition(2).check(matches(not(isEnabled())));
    } finally {
      //restore
      setListViewFile(currListFile);
      setMapViewFile(currMapFile);
    }
  }

  @Test
  public void views_availableViewsOn() {
    //Backup config
    String currListFile = getListViewFile();
    String currMapFile = getMapViewFile();

    try {
      //enable both views
      setListViewFile("testing");
      setMapViewFile("testing");

      //quit preferences
      pressBack();

      //Check buttons
      onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
      onData(anything()).atPosition(1).check(matches(isEnabled()));
      onData(anything()).atPosition(2).check(matches(isEnabled()));
    } finally {
      //restore
      setListViewFile(currListFile);
      setMapViewFile(currMapFile);
    }
  }

  @Test
  public void display_outOfAppDirViewFile() {
    //backup
    String currListFile = getListViewFile();

    try {
      //stub intent
      intending(hasAction(OI_PICK_FILE)).respondWith(
          new Instrumentation.ActivityResult(
              Activity.RESULT_OK,
              new Intent().setData(Uri.fromFile(new File("/test.html")))
          )
      );

      onData(withKey(LIST_VIEW_FILE)).perform(click());

      //check toast message
      EspressoUtils.toastMsgMatcher(
          mActivityRule,
          is(EspressoUtils.getString(
              mActivityRule, R.string.file_not_under_app_dir, ODKFileUtils.getAppFolder(APP_NAME))
          )
      );
    } finally {
      //restore
      setListViewFile(currListFile);
    }
  }

  private static String getListViewFile() {
    OdkDbHandle db = null;
    String file = null;

    try{
      db = Tables.getInstance().getDatabase().openDatabase(APP_NAME);
      file = TableUtil.get()
          .getListViewFilename(Tables.getInstance(), APP_NAME, db, T_HOUSE_E_TABLE_ID);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();

      //if RemoteException is thrown, file is guaranteed to be null
      assertThat(file, notNullValue(String.class));
    } finally {
      if (db != null) {
        try {
          Tables.getInstance().getDatabase().closeDatabase(APP_NAME, db);
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }

    return file;
  }

  private static String getDetailViewFile() {
    OdkDbHandle db = null;
    String file = null;

    try{
      db = Tables.getInstance().getDatabase().openDatabase(APP_NAME);
      file = TableUtil.get()
          .getDetailViewFilename(Tables.getInstance(), APP_NAME, db, T_HOUSE_E_TABLE_ID);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();

      //if RemoteException is thrown, file is guaranteed to be null
      assertThat(file, notNullValue(String.class));
    } finally {
      if (db != null) {
        try {
          Tables.getInstance().getDatabase().closeDatabase(APP_NAME, db);
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }

    return file;
  }

  private static String getMapViewFile() {
    OdkDbHandle db = null;
    String file = null;

    try{
      db = Tables.getInstance().getDatabase().openDatabase(APP_NAME);
      file = TableUtil.get()
          .getMapListViewFilename(Tables.getInstance(), APP_NAME, db, T_HOUSE_E_TABLE_ID);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();

      //if RemoteException is thrown, file is guaranteed to be null
      assertThat(file, notNullValue(String.class));
    } finally {
      if (db != null) {
        try {
          Tables.getInstance().getDatabase().closeDatabase(APP_NAME, db);
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }

    return file;
  }

  private static void setListViewFile(String filename) {
    try {
      TableUtil.get().atomicSetListViewFilename(
          Tables.getInstance(), APP_NAME, T_HOUSE_E_TABLE_ID, filename);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    }
  }

  private static void setDetailViewFile(String filename) {
    try {
      TableUtil.get().atomicSetDetailViewFilename(
          Tables.getInstance(), APP_NAME, T_HOUSE_E_TABLE_ID, filename);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    }
  }

  private static void setMapViewFile(String filename) {
    try {
      TableUtil.get().atomicSetMapListViewFilename(
          Tables.getInstance(), APP_NAME, T_HOUSE_E_TABLE_ID, filename);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    }
  }
}
