package org.opendatakit.espresso;

import android.Manifest;
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
import android.support.test.filters.LargeTest;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.views.SpreadsheetView;
import org.opendatakit.util.DisableAnimationsRule;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.opendatakit.util.TestConstants.APP_NAME;
import static org.opendatakit.util.TestConstants.COLUMNS_LIST;
import static org.opendatakit.util.TestConstants.COL_DISPLAY_NAME;
import static org.opendatakit.util.TestConstants.COL_KEY;
import static org.opendatakit.util.TestConstants.DEFAULT_FORM;
import static org.opendatakit.util.TestConstants.DEFAULT_VIEW_TYPE;
import static org.opendatakit.util.TestConstants.DETAIL_VIEW_FILE;
import static org.opendatakit.util.TestConstants.LIST_VIEW_FILE;
import static org.opendatakit.util.TestConstants.MAP_LIST_VIEW_FILE;
import static org.opendatakit.util.TestConstants.OI_PICK_FILE;
import static org.opendatakit.util.TestConstants.TABLE_DISPLAY_NAME;
import static org.opendatakit.util.TestConstants.TABLE_ID;
import static org.opendatakit.util.TestConstants.T_HOUSE_E_DISPLAY_NAME;
import static org.opendatakit.util.TestConstants.T_HOUSE_E_TABLE_ID;
import static org.opendatakit.util.TestConstants.T_HOUSE_TABLE_ID;
import static org.opendatakit.util.TestConstants.WEB_WAIT_TIMEOUT;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TablePrefTest extends AbsBaseTest {
  @ClassRule
  public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

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

  private static String getListViewFile() {
    DbHandle db = null;
    String file = null;

    try {
      db = c.getDatabase().openDatabase(APP_NAME);
      file = TableUtil.get().getListViewFilename(c.getDatabase(), APP_NAME, db,
          T_HOUSE_E_TABLE_ID);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();

      //if RemoteException is thrown, file is guaranteed to be null
      assertThat(file, notNullValue(String.class));
    } finally {
      if (db != null) {
        try {
          c.getDatabase().closeDatabase(APP_NAME, db);
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }

    return file;
  }

  private static void setListViewFile(String filename) {
    try {
      TableUtil.get().atomicSetListViewFilename(c.getDatabase(), APP_NAME,
          T_HOUSE_E_TABLE_ID, filename);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    }
  }

  private static String getDetailViewFile() {
    DbHandle db = null;
    String file = null;

    try {
      db = c.getDatabase().openDatabase(APP_NAME);
      file = TableUtil.get().getDetailViewFilename(c.getDatabase(), APP_NAME, db,
          T_HOUSE_E_TABLE_ID);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();

      //if RemoteException is thrown, file is guaranteed to be null
      assertThat(file, notNullValue(String.class));
    } finally {
      if (db != null) {
        try {
          c.getDatabase().closeDatabase(APP_NAME, db);
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }

    return file;
  }

  private static void setDetailViewFile(String filename) {
    try {
      TableUtil.get().atomicSetDetailViewFilename(c.getDatabase(), APP_NAME,
          T_HOUSE_E_TABLE_ID, filename);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    }
  }

  private static String getMapViewFile() {
    DbHandle db = null;
    String file = null;

    try {
      db = c.getDatabase().openDatabase(APP_NAME);
      file = TableUtil.get()
          .getMapListViewFilename(c.getDatabase(), APP_NAME, db,
              T_HOUSE_E_TABLE_ID);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();

      //if RemoteException is thrown, file is guaranteed to be null
      assertThat(file, notNullValue(String.class));
    } finally {
      if (db != null) {
        try {
          c.getDatabase().closeDatabase(APP_NAME, db);
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }

    return file;
  }

  private static void setMapViewFile(String filename) {
    try {
      TableUtil.get().atomicSetMapListViewFilename(c.getDatabase(), APP_NAME,
          T_HOUSE_E_TABLE_ID, filename);
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    }
  }

  @Before
  public void setup() {
    UAUtils.assertInitSucess(initSuccess);
    EspressoUtils.cancelExternalIntents();
    EspressoUtils.openTableManagerFromCustomHome();

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
    final int numCol = 21; // was 19 then we added lat/lon?

    //go to columns
    onData(withKey(COLUMNS_LIST)).perform(click());

    //Check num of column
    onView(withId(android.R.id.list)).check(matches(ODKMatchers.withSize(numCol)));
  }

  @Test
  public void intent_tableLevelPref() {
    intended(allOf(hasComponent(TableLevelPreferencesActivity.class.getName()),
        hasExtra("tableId", T_HOUSE_E_TABLE_ID),
        hasExtra("tablePreferenceFragmentType", "TABLE_PREFERENCE")));
  }

  @Test
  public void display_tableIdentifier() {
    //Check that display name and table id are shown correctly
    assertThat(EspressoUtils.getPrefSummary(TABLE_DISPLAY_NAME), is(T_HOUSE_E_DISPLAY_NAME));
    assertThat(EspressoUtils.getPrefSummary(TABLE_ID), is(T_HOUSE_E_TABLE_ID));
  }

  @Test
  public void display_columnIdentifier() {
    //if (true) return;

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
        new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()
            .setData(Uri.fromFile(new File(ODKFileUtils.getOdkFolder() + listViewPath)))));

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
        new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()
            .setData(Uri.fromFile(new File(ODKFileUtils.getOdkFolder() + detailViewPath)))));

    //edit detail view path
    onData(withKey(DETAIL_VIEW_FILE)).perform(click());

    intending(hasAction(OI_PICK_FILE)).respondWith(
        new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()
            .setData(Uri.fromFile(new File(ODKFileUtils.getOdkFolder() + listViewPath)))));

    //edit list view path
    onData(withKey(LIST_VIEW_FILE)).perform(click());

    //go to list view
    pressBack();
    pressBack();
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());
    onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
    onView(withText("List")).perform(click());

    //choose "Tea for All"
    EspressoUtils
        .delayedFindElement(Locator.ID, "924e548d-667f-4c49-91c0-de2885c0dda1", WEB_WAIT_TIMEOUT)
        .perform(webClick());

    try {
      Matcher<View> topWebViewMatcher =
              allOf(withId(R.id.webkit), isDescendantOfA(withId(R.id.top_pane)));

      //check url
      onView(topWebViewMatcher)
              .check(matches(ODKMatchers.withUrl(endsWith(detailViewPath))));

      EspressoUtils
              .delayedFindElement(topWebViewMatcher, Locator.ID, "TITLE", WEB_WAIT_TIMEOUT)
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
      setListViewFile("config/tables/Tea_houses/html/Tea_houses_list.html");
      setMapViewFile("config/tables/Tea_houses/html/Tea_houses_list.html");

      //quit to table manager
      pressBack();
      pressBack();

      // reopen Tea Houses Editable
      onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

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
          new Instrumentation.ActivityResult(Activity.RESULT_OK,
              new Intent().setData(Uri.fromFile(new File("/test.html")))));

      onData(withKey(LIST_VIEW_FILE)).perform(click());

      //check toast message
      EspressoUtils.toastMsgMatcher(mIntentsRule, is(EspressoUtils
          .getString(mIntentsRule, R.string.file_not_under_app_dir,
              ODKFileUtils.getAppFolder(APP_NAME))));
    } finally {
      //restore
      setListViewFile(currListFile);
    }
  }

  @Test
  public void display_badFormId() {
    // backup
    String currFormId = null;

    try {
      currFormId = FormType
              .constructFormType(mIntentsRule.getActivity(), APP_NAME, T_HOUSE_E_TABLE_ID)
              .getFormId();

      assertThat(currFormId, notNullValue(String.class));

      //change form id to something invalid
      onData(withKey(DEFAULT_FORM))
              .perform(click());
      onView(withId(R.id.edit_form_id))
              .perform(click())
              .perform(clearText())
              .perform(typeText("invalid_form_id"));
      onView(withId(android.R.id.button1))
              .perform(click());

      EspressoUtils
              .toastMsgMatcher(
                  mIntentsRule,
                      is(EspressoUtils.getString(mIntentsRule, R.string.invalid_form))
              );
    } catch (ServicesAvailabilityException e) {
      e.printStackTrace();
    } finally {
      if (currFormId != null) {
        try {
          FormType
                  .constructFormType(mIntentsRule.getActivity(), APP_NAME, T_HOUSE_E_TABLE_ID)
                  .setFormId(currFormId);
        } catch (ServicesAvailabilityException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
