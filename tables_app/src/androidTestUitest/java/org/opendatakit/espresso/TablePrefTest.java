package org.opendatakit.espresso;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.view.View;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.views.SpreadsheetView;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;
import org.opendatakit.utilities.ODKFileUtils;
import org.opendatakit.utilities.ODKXFileUriUtils;

import java.io.File;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.opendatakit.tables.utils.Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE;
import static org.opendatakit.util.TestConstants.APP_NAME;
import static org.opendatakit.util.TestConstants.T_HOUSE_E_DISPLAY_NAME;
import static org.opendatakit.util.TestConstants.T_HOUSE_E_TABLE_ID;
import static org.opendatakit.util.TestConstants.T_HOUSE_TABLE_ID;
import static org.opendatakit.util.TestConstants.WEB_WAIT_TIMEOUT;

@LargeTest
public class TablePrefTest extends AbsBaseTest {

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
  public void intents_launchFilePicker() {
    //Check intent on "List View File"
    EspressoUtils
        .onRecyclerViewText(R.string.list_view_file)
        .perform(click());
    intended(hasAction(Intent.ACTION_OPEN_DOCUMENT), Intents.times(1));

    //Check intent on "Detail View File"
    EspressoUtils
        .onRecyclerViewText(R.string.detail_view_file)
        .perform(click());
    intended(hasAction(Intent.ACTION_OPEN_DOCUMENT), Intents.times(2));

    //Check intent on "Map List View File"
    EspressoUtils
        .onRecyclerViewText(R.string.map_list_view_file)
        .perform(click());
    intended(hasAction(Intent.ACTION_OPEN_DOCUMENT), Intents.times(3));
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
    EspressoUtils
        .onRecyclerViewText(R.string.change_default_view_type)
        .perform(click());

    //choose Spreadsheet
    onView(withText(R.string.spreadsheet)).perform(click());

    //go to table manager
    Espresso.pressBack();
    Espresso.pressBack();

    //Check new default view type
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());
    try {
      onView(isAssignableFrom(SpreadsheetView.class)).check(matches(isDisplayed()));
    } finally {
      //restore default view type
      onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
      EspressoUtils
          .onRecyclerViewText(R.string.change_default_view_type)
          .perform(click());
      onView(withText(R.string.list)).perform(click());
    }
  }

  @Test
  public void views_columns() {
    final int numCol = 21; // was 19 then we added lat/lon?

    //go to columns
    EspressoUtils
        .onRecyclerViewText(R.string.columns)
        .perform(click());

    //Check num of column
    onView(withId(android.R.id.list)).check(matches(ODKMatchers.withSize(numCol)));
  }

  @Test
  public void intent_tableLevelPref() {
    intended(allOf(hasComponent(TableLevelPreferencesActivity.class.getName()),
        hasExtra(IntentConsts.INTENT_KEY_TABLE_ID, T_HOUSE_E_TABLE_ID),
        hasExtra(TABLE_PREFERENCE_FRAGMENT_TYPE, TableLevelPreferencesActivity.FragmentType.TABLE_PREFERENCE.name())));
  }

  @Test
  public void display_tableIdentifier() {
    //Check that display name and table id are shown correctly
    onView(ODKMatchers.withPref(R.string.display_name, T_HOUSE_E_DISPLAY_NAME))
        .check(matches(isCompletelyDisplayed()));

    onView(ODKMatchers.withPref(R.string.table_id, T_HOUSE_E_TABLE_ID))
        .check(matches(isCompletelyDisplayed()));
  }

  @Test
  public void display_columnIdentifier() {
    EspressoUtils
        .onRecyclerViewText(R.string.columns)
        .perform(click());
    onData(is("House id")).perform(click());

    onView(ODKMatchers.withPref(R.string.display_name, "House id"))
        .check(matches(isCompletelyDisplayed()));

    onView(ODKMatchers.withPref(R.string.element_key, "House_id"))
        .check(matches(isCompletelyDisplayed()));
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

    EspressoUtils
        .onRecyclerViewText(R.string.columns)
        .perform(click());
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
    String listViewPath = "tables/Tea_houses/html/Tea_houses_list.html";
    Uri listViewUri = ODKXFileUriUtils.getConfigUri(APP_NAME).buildUpon().appendPath(listViewPath).build();

    //backup current config
    String currFile = getListViewFile();

    //stub response
    intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
        new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent().setData(listViewUri)));

    //edit list view path
    EspressoUtils
        .onRecyclerViewText(R.string.list_view_file)
        .perform(click());

    //go to list view
    //minor problem, webkit is reloaded but new url not loaded after exiting pref
    pressBack();
    pressBack();
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());
    onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
    onView(withText(R.string.list)).perform(click());

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
    String detailViewPath = "tables/Tea_houses/html/Tea_houses_detail.html";
    String listViewPath = "tables/Tea_houses/html/Tea_houses_list.html";
    Uri detailViewUri = ODKXFileUriUtils.getConfigUri(APP_NAME).buildUpon().appendPath(detailViewPath).build();
    Uri listViewUri = ODKXFileUriUtils.getConfigUri(APP_NAME).buildUpon().appendPath(listViewPath).build();

    //back up current config
    String currDetailFile = getDetailViewFile();
    String currListFile = getListViewFile();

    //stub response
    intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
        new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()
            .setData(detailViewUri)));

    //edit detail view path
    EspressoUtils
        .onRecyclerViewText(R.string.detail_view_file)
        .perform(click());

    intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
        new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()
            .setData(listViewUri)));

    //edit list view path
    EspressoUtils
        .onRecyclerViewText(R.string.list_view_file)
        .perform(click());

    //go to list view
    pressBack();
    pressBack();
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());
    onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
    onView(withText(R.string.list)).perform(click());

    //choose "Tea for All"
    EspressoUtils
        .delayedFindElement(Locator.ID, "924e548d-667f-4c49-91c0-de2885c0dda1", WEB_WAIT_TIMEOUT)
        .perform(webClick());

    try {
      Matcher<View> topWebViewMatcher =
              allOf(withId(R.id.webkit), isDescendantOfA(withId(R.id.top_pane)));

      //check url
      onView(topWebViewMatcher).check(matches(ODKMatchers.withUrl(endsWith(detailViewPath))));

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
      intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
          new Instrumentation.ActivityResult(Activity.RESULT_OK,
              new Intent().setData(Uri.fromFile(new File("/test.html")))));

      EspressoUtils
          .onRecyclerViewText(R.string.list_view_file)
          .perform(click());

      //check toast message
      EspressoUtils.toastMsgMatcher(mIntentsRule, is(EspressoUtils
          .getString(R.string.file_not_under_app_dir,
              ODKFileUtils.getAppFolder(APP_NAME))));
    } finally {
      //restore
      setListViewFile(currListFile);
    }
  }

  @Test
  public void display_badFormId() throws ServicesAvailabilityException {
    // backup
    String currFormId = null;

    try {
      currFormId = FormType
              .constructFormType(mIntentsRule.getActivity(), APP_NAME, T_HOUSE_E_TABLE_ID)
              .getFormId();

      assertThat(currFormId, notNullValue(String.class));

      //change form id to something invalid
      EspressoUtils
          .onRecyclerViewText(R.string.default_form)
          .perform(click());
      onView(isAssignableFrom(AppCompatEditText.class))
              .perform(click())
              .perform(clearText())
              .perform(typeText("invalid_form_id"));
      onView(withId(android.R.id.button1))
              .perform(click());

      EspressoUtils
              .toastMsgMatcher(
                  mIntentsRule,
                      is(EspressoUtils.getString(R.string.invalid_form))
              );
    } finally {
      if (currFormId != null) {
        FormType
            .constructFormType(mIntentsRule.getActivity(), APP_NAME, T_HOUSE_E_TABLE_ID)
            .setFormId(currFormId);
      }
    }
  }
}
