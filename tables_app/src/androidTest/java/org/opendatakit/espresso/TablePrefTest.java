package org.opendatakit.espresso;

import android.app.Activity;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.*;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TablePrefTest {
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
  };

  @Before
  public void setup() {
    assertThat("Initialization unsuccessful.", initSuccess, is(true));

    intending(not(isInternal()))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));

    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
  }

  @Test
  public void intents_addDataSurvey() {
    //click "Tea Houses Editable"
    onData(ODKMatchers.withTable(T_HOUSE_E_TABLE_ID)).perform(click());

    //click "plus" and check intent
    onView(withId(R.id.top_level_table_menu_add)).perform(click());
    intended(toPackage(SURVEY_PKG_NAME));
  }

  @Test
  public void intents_launchOIFileManager() {
    //click "Tea inventory"
    onData(ODKMatchers.withTable(T_INVENTORY_TABLE_ID)).perform(click());

    //go to table pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());

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
    //click "Tea inventory"
    onData(ODKMatchers.withTable(T_INVENTORY_TABLE_ID)).perform(click());

    //go to view type pref
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(DEFAULT_VIEW_TYPE)).perform(click());

    //choose Spreadsheet
    onView(withText("Spreadsheet")).perform(click());

    //go to table manager
    Espresso.pressBack();
    Espresso.pressBack();

    //Check new default view type
    onData(ODKMatchers.withTable(T_INVENTORY_TABLE_ID)).perform(click());
    try {
      onView(withClassName(endsWith("SpreadsheetView"))).check(matches(isDisplayed()));
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

    //click "Tea Houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());

    //go to columns
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());
    onData(withKey(COLUMNS_LIST)).perform(click());

    //Check num of column
    onView(withId(android.R.id.list)).check(matches(ODKMatchers.withSize(numCol)));
  }

  @Test
  public void intent_tableLevelPref() {
    //click "Tea Houses"
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());

    //go to table preferences
    onView(withId(R.id.top_level_table_menu_table_properties)).perform(click());

    intended(allOf(
        hasComponent(TableLevelPreferencesActivity.class.getName()),
        hasExtra("tableId", T_HOUSE_TABLE_ID)));
  }
}
