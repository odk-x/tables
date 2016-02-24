package org.opendatakit.espresso;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import org.junit.*;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;

import java.io.File;
import java.io.FilenameFilter;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.opendatakit.util.TestConstants.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CsvTest {
  private static final String VALID_QUALIFIER = "TEST_VALID";
  private static final String INVALID_QUALIFIER = "TEST_INVALID/";

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
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, VALID_QUALIFIER)).delete();
    new File(ODKFileUtils.getOutputTableDefinitionCsvFile(
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, VALID_QUALIFIER)).delete();
    new File(ODKFileUtils.getOutputTableCsvFile(
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, VALID_QUALIFIER)
        + "/" + T_HOUSE_TABLE_ID + "." + VALID_QUALIFIER + ".csv").delete();
    new File(ODKFileUtils.getOutputTableCsvFile(
        TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, VALID_QUALIFIER)).delete();
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
    //open table chooser
    onView(withClassName(is(Spinner.class.getName()))).perform(click());

    //Choose a table
    onData(is(T_HOUSE_DISPLAY_NAME)).inRoot(isPlatformPopup()).perform(click());

    //Enter a valid qualifier
    onView(withClassName(is(EditText.class.getName()))).perform(typeText(VALID_QUALIFIER));

    //Click Export
    onView(withClassName(is(Button.class.getName()))).perform(click());

    //Make sure export is successful
    onView(withId(android.R.id.message)).check(matches(withText(R.string.export_success)));

    //Dismiss dialog
    onView(withId(android.R.id.button3)).perform(click());

    //make sure all the files are there
    assertThat("Csv is missing",
        new File(ODKFileUtils.getOutputTableCsvFile(
            TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, VALID_QUALIFIER)
            + "/" + T_HOUSE_TABLE_ID + "." + VALID_QUALIFIER + ".csv").exists(),
        is(true));
    assertThat("Definition Csv is missing",
        new File(ODKFileUtils.getOutputTableDefinitionCsvFile(
            TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, VALID_QUALIFIER)).exists(),
        is(true));
    assertThat("Properties Csv is missing",
        new File(ODKFileUtils.getOutputTablePropertiesCsvFile(
            TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID, VALID_QUALIFIER)).exists(),
        is(true));
  }

  @Test
  public void exportCsv_invalidQualifier() {
    int fileCount = getOutputDirFileCount();

    //open table chooser
    onView(withClassName(is(Spinner.class.getName()))).perform(click());

    //Choose a table
    onData(is(T_HOUSE_DISPLAY_NAME)).inRoot(isPlatformPopup()).perform(click());

    //Enter an invalid qualifier
    onView(withClassName(is(EditText.class.getName()))).perform(typeText(INVALID_QUALIFIER));

    //Click Export
    onView(withClassName(is(Button.class.getName()))).perform(click());

    try {
      //Check that error message is shown
      onView(withText(EspressoUtils.getString(mActivityRule, R.string.export_failure)))
          .check(matches(isCompletelyDisplayed()));

      //Check that csv was not exported
      assertThat(getOutputDirFileCount(), equalTo(fileCount));
    } finally {
      String csvDir = ODKFileUtils.getOutputCsvFolder(TableFileUtils.getDefaultAppName())
          + "/Tea_houses.TEST_INVALID";

      if (new File(csvDir).exists()) {
        new File(csvDir + "/.csv").delete();
        new File(csvDir + "/.definition.csv").delete();
        new File(csvDir + "/.properties.csv").delete();
        new File(csvDir).delete();
      }
    }
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

  private static int getOutputDirFileCount() {
    return new File(ODKFileUtils.getOutputCsvFolder(APP_NAME)).list().length;
  }
}
