package org.opendatakit.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowEnvironment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Environment;

/**
 * Various methods that are useful for testing with roboelectric.
 * @author sudar.sam@gmail.com
 *
 */
public class TestCaseUtils {
  
  public static boolean FORM_IS_USER_DEFINED = false;
  public static String FORM_ID = "testFormId";
  public static String FORM_VERSION = "testFormVersion";
  public static String ROOT_ELEMENT = "testRootElement";
  public static String ROW_NAME = "testRowName";
  public static String SCREEN_PATH = "?testKey=testValue";
  
  /**
   * The default app name for tables. Using this rather than the
   * getDefaultAppName method because that dumps the stack trace.
   */
  public static final String TABLES_DEFAULT_APP_NAME = "tables";
  
  /**
   * Get an intent with the app name set to {@link #TABLES_DEFAULT_APP_NAME}.
   * @return
   */
  public static Intent getIntentWithAppNameTables() {
    Intent result = new Intent();
    result.putExtra(
        Constants.IntentKeys.APP_NAME,
        TABLES_DEFAULT_APP_NAME);
    return result;    
  }
  
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
   * Retrieve a {@link CollectFormParameters} object for use in testing.
   * @param isCustom
   * @param FORM_ID
   * @param FORM_VERSION
   * @param formXMLRootElement
   * @param rowDisplayName
   * @return
   */
  public static CollectFormParameters getCollectFormParameters() {
    return new CollectFormParameters(
        FORM_IS_USER_DEFINED,
        FORM_ID,
        FORM_VERSION,
        ROOT_ELEMENT,
        ROW_NAME);
  }
  
  public static SurveyFormParameters getSurveyFormParameters() {
    return new SurveyFormParameters(
        FORM_IS_USER_DEFINED,
        FORM_ID,
        SCREEN_PATH);
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
