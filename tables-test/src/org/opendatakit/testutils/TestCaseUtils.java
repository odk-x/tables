package org.opendatakit.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowEnvironment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Environment;

/**
 * Various methods that are useful for testing with roboelectric.
 * @author sudar.sam@gmail.com
 *
 */
public class TestCaseUtils {
  
  /**
   * Make the external storage as mounted. This is required by classes that
   * call through to {@link ODKFileUtils#verifyExternalStorageAvailability()}.
   */
  public static void setExternalStorageMounted() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
  }
  
  public static void startFragmentForMainActivity(Fragment fragment) {
    MainActivity mainActivity = Robolectric.buildActivity(MainActivity.class)
        .create().start().resume().attach().get();
    FragmentManager fm = mainActivity.getFragmentManager();
    fm.beginTransaction().replace(
        R.id.main_activity_frame_layout,
        fragment,
        "TEST_TAG").commit();
  }
  
  /**
   * Because our database layer makes liberal use of static fields, tests run
   * sequentially that call these classes see the state modified by the
   * previous tests and fail. This resets the state.
   */
  public static void resetStaticDatabaseVariables() {
    // Because of the annoying fact that things are stored in a static final
    // dbHlelpers field in DataModelDatabaseHelperFactory, we need to reset
    // this variable manually.
    Field staticDbHelpersField;
    try {
      staticDbHelpersField = 
          DataModelDatabaseHelperFactory.class.getDeclaredField("dbHelpers");
      staticDbHelpersField.setAccessible(true);
      // And now make it not final so we can reset it.
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(
          staticDbHelpersField,
          staticDbHelpersField.getModifiers() & ~Modifier.FINAL);
      staticDbHelpersField.set(null, new HashMap<String, DataModelDatabaseHelper>());
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

}
