package org.opendatakit.espresso;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.util.ODKMatchers;

import java.io.File;
import java.io.FilenameFilter;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CsvTest {
  private static final String QUALIFIER = "testing";

  @Rule
  public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

  @Before
  public void setup() {
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
    onView(withId(R.id.menu_table_manager_export)).perform(click());
  }

  @After
  public void cleanup() {
    //clean up for exportCsv_validQualifier
    new File(ODKFileUtils.getOutputTablePropertiesCsvFile(
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, QUALIFIER)).delete();
    new File(ODKFileUtils.getOutputTableDefinitionCsvFile(
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, QUALIFIER)).delete();
    new File(ODKFileUtils.getOutputTableCsvFile(
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, QUALIFIER)
        + "/" + T_HOUSE_TABLE_ID + "." + QUALIFIER + ".csv").delete();
    new File(ODKFileUtils.getOutputTableCsvFile(
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, QUALIFIER)).delete();
  }

  @Test
  public void exportCsv_tableList() {
    //open table chooser
    onView(withClassName(endsWith("Spinner"))).perform(click());

    //check the number of tables
    onView(withClassName(endsWith("ListView")))
        .inRoot(isPlatformPopup())
        .check(matches(ODKMatchers.withSize(getTableCount())));
  }

  @Test
  public void exportCsv_validQualifier() {
    final String qualifier = "testing";

    //open table chooser
    onView(withClassName(endsWith("Spinner"))).perform(click());

    //Choose a table
    onData(is("Tea houses")).inRoot(isPlatformPopup()).perform(click());

    //Enter a valid qualifier
    onView(withClassName(endsWith("EditText"))).perform(typeText(qualifier));

    //Click Export
    onView(withClassName(endsWith("Button"))).perform(click());

    //Make sure export is successful
    onView(withId(android.R.id.message)).check(matches(withText(R.string.export_success)));

    //Dismiss dialog
    onView(withId(android.R.id.button3)).perform(click());

    //make sure all the files are there
    assertThat("Csv is missing",
        new File(ODKFileUtils.getOutputTableCsvFile(
            TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, qualifier)
            + "/" + T_HOUSE_TABLE_ID + "." + qualifier + ".csv").exists(),
        is(true));
    assertThat("Definition Csv is missing",
        new File(ODKFileUtils.getOutputTableDefinitionCsvFile(
            TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, qualifier)).exists(),
        is(true));
    assertThat("Properties Csv is missing",
        new File(ODKFileUtils.getOutputTablePropertiesCsvFile(
            TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, qualifier)).exists(),
        is(true));
  }

  private static int getTableCount() {
    return new File(ODKFileUtils.getTablesFolder(TableFileUtils.getDefaultAppName())).list(
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return new File(dir, name + "/properties.csv").exists();
          }
    }).length;
  }
}
