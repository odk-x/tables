package org.opendatakit.espresso;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.opendatakit.util.TestConstants.APP_NAME;
import static org.opendatakit.util.TestConstants.T_HOUSE_DISPLAY_NAME;
import static org.opendatakit.util.TestConstants.T_HOUSE_TABLE_ID;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.ImportCSVActivity;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;
import java.io.FilenameFilter;


@LargeTest
public class CsvTest {

  private static final String VALID_QUALIFIER = "TEST_VALID";
  private static final String INVALID_QUALIFIER = "TEST_INVALID/";

  private Boolean initSuccess = null;
  private UiDevice mDevice;
  private ActivityScenario<MainActivity> scenario;
  private View decorView;



    private void beforeActivityLaunched(){
      if (initSuccess == null) {
          mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
          initSuccess = UAUtils.turnOnCustomHome(mDevice);
      }
  }

  @Rule
  public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION
  );


  private static int getTableCount() {
    return new File(ODKFileUtils.getTablesFolder(TableFileUtils.getDefaultAppName()))
        .list(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return new File(dir, name + "/properties.csv").exists();
          }
        }).length;
  }

  private static int getOutputDirFileCount() {
    return new File(ODKFileUtils.getOutputCsvFolder(APP_NAME)).list().length;
  }

  @Before
  public void setup() {
    beforeActivityLaunched();
    scenario = ActivityScenario.launch(MainActivity.class);
    Intents.init();
    scenario.onActivity(activity ->
              decorView = activity.getWindow().getDecorView());
    UAUtils.assertInitSucess(initSuccess);
    EspressoUtils.openTableManagerFromCustomHome();

  }

  @After
  public void cleanup() {
    //clean up for exportCsv_validQualifier
    new File(ODKFileUtils
        .getOutputTablePropertiesCsvFile(TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID,
            VALID_QUALIFIER)).delete();
    new File(ODKFileUtils
        .getOutputTableDefinitionCsvFile(TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID,
            VALID_QUALIFIER)).delete();
    new File(ODKFileUtils
        .getOutputTableCsvFile(TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID,
            VALID_QUALIFIER) + "/" + T_HOUSE_TABLE_ID + "." + VALID_QUALIFIER + ".csv").delete();
    new File(ODKFileUtils
        .getOutputTableCsvFile(TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID,
            VALID_QUALIFIER)).delete();
      Intents.release();
      scenario.close();
  }

  @Test
  public void exportCsv_tableList() {
    onView(withId(R.id.menu_table_manager_export)).perform(click());
    //open table chooser
    onView(withClassName(endsWith("Spinner"))).perform(click());

    //check the number of tables
    onView(withClassName(endsWith("ListView"))).inRoot(isPlatformPopup())
        .check(matches(ODKMatchers.withSize(getTableCount())));
  }

  @Test
  public void exportCsv_validQualifier() {
    onView(withId(R.id.menu_table_manager_export)).perform(click());
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
    assertThat("Csv is missing", new File(ODKFileUtils
            .getOutputTableCsvFile(TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID,
                VALID_QUALIFIER) + "/" + T_HOUSE_TABLE_ID + "." + VALID_QUALIFIER + ".csv").exists(),
        is(true));
    assertThat("Definition Csv is missing", new File(ODKFileUtils
        .getOutputTableDefinitionCsvFile(TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID,
            VALID_QUALIFIER)).exists(), is(true));
    assertThat("Properties Csv is missing", new File(ODKFileUtils
        .getOutputTablePropertiesCsvFile(TableFileUtils.getDefaultAppName(), T_HOUSE_TABLE_ID,
            VALID_QUALIFIER)).exists(), is(true));
  }

  @Test
  public void exportCsv_invalidQualifier() {
    int fileCount = getOutputDirFileCount();

    onView(withId(R.id.menu_table_manager_export)).perform(click());

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
      onView(withText(EspressoUtils.getString(R.string.export_failure)))
          .check(matches(isCompletelyDisplayed()));

      //Check that csv was not exported
      assertThat("Csv export produced files in output directory", getOutputDirFileCount(),
          equalTo(fileCount));
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

  @Test
  public void importCsv_fileOutOfAppDir() {
    //stub intent
    intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
        new Instrumentation.ActivityResult(Activity.RESULT_OK,
            new Intent().setData(Uri.fromFile(new File("/file")))));

    //go to csv import
    onView(withId(R.id.menu_table_manager_import)).perform(click());
    onView(withText(R.string.import_choose_csv_file)).perform(click());

    //check toast
    EspressoUtils.toastMsgMatcher(decorView, is(ImportCSVActivity.IMPORT_FILE_MUST_RESIDE_IN_OPENDATAKIT_FOLDER));
  }

  @Test
  public void importCsv_checkOdkxFolder() {
    // Define the source CSV file (a test CSV file to be copied)
    File sourceCsvFile = new File("/opendatakit/default/config/tables/visit/properties.csv");
    // Define the target directory where you want to check for the existence of the CSV file
    File odkxFolder = new File("/opendatakit/default/config/assets/csv");

    try {
        // Copy the test CSV file to the target directory
        Files.copy(sourceCsvFile.toPath(), new File(odkxFolder, "properties.csv").toPath());

        // Check if the /opendatakit/default/config/assets/csv directory exists
        boolean odkxFolderExists = odkxFolder.exists() && odkxFolder.isDirectory();

        // Check if the properties CSV file exists within the /csv folder
        boolean csvFileExists = new File(odkxFolder, "properties.csv").exists();

        // Assert that the directory exists and contains .csv files
        assertThat("/opendatakit/default/config/assets/csv directory does not exist or is not a directory", odkxFolderExists, is(true));
        assertThat("properties.csv file does not exist in the /csv folder", csvFileExists, is(true));
    } catch (IOException e) {
        // Handle the exception that might occur during the file copy operation
        fail("An exception occurred during the file copy operation: " + e.getMessage());
    } finally {
        // Clean up: Delete the properties CSV file after the test is complete
        File testCsvFile = new File(odkxFolder, "properties.csv");
        if (testCsvFile.exists()) {
            testCsvFile.delete();
        }
    }
  }
}