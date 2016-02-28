package org.opendatakit;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.View;
import org.junit.*;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.EspressoUtils;
import org.opendatakit.util.ODKMatchers;
import org.opendatakit.util.UAUtils;

import java.io.File;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
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
  }

  /**
   * This is the same bug as the one we had for "pick view file" in table preferences
   */
  @Test
  public void crashBy_importCsvOutsideAppDir() {
    //stub intent
    intending(hasAction(OI_PICK_FILE)).respondWith(
        new Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            new Intent().setData(Uri.fromFile(new File("/file")))
        )
    );

    //Choose a csv to import
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
    onView(withId(R.id.menu_table_manager_import)).perform(click());
    onView(withText(R.string.import_choose_csv_file)).perform(click());

    //CRASH
  }

  /**
   * This is a variant of the previous "generated form" bug
   */
  @Test
  public void crashBy_editGeneratedForm() {
    //Open "Tea houses"
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
    onData(ODKMatchers.withTable(T_HOUSE_TABLE_ID)).perform(click());

    //Switch to spreadsheet view
    onView(withId(R.id.top_level_table_menu_select_view)).perform(click());
    onView(withText("Spreadsheet")).perform(click());

    //Edit row 3
    UAUtils.longPressSpreadsheetRow(mDevice, 3);
    onView(withText(EspressoUtils.getString(mActivityRule, R.string.edit_row))).perform(click());

    //CRASH
  }
}
