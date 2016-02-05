package org.opendatakit.espresso;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.util.ODKMatchers;

import java.io.File;
import java.io.FilenameFilter;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CsvTest {
  @Rule
  public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

  @Before
  public void setup() {
    onView(withId(R.id.menu_web_view_activity_table_manager)).perform(click());
    onView(withId(R.id.menu_table_manager_export)).perform(click());
  }

  /**
   * WIP
   */
  @Test
  public void exportCsv_tableList() {
    assert(true);

    //open table chooser
    onView(withClassName(endsWith("Spinner"))).perform(click());

    //check the number of tables
    onView(withClassName(endsWith("ListView")))
        .inRoot(isPlatformPopup())
        .check(matches(ODKMatchers.withSize(getTableCount())));
  }

  private static int getTableCount() {
    return new File(ODKFileUtils.getTablesFolder(TableFileUtils.getDefaultAppName()))
        .list(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return (new File(dir, name)).isDirectory();
          }
    }).length;
  }
}
