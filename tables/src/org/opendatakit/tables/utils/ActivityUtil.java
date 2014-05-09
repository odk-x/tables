package org.opendatakit.tables.utils;

import java.util.HashMap;
import java.util.Map;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

public class ActivityUtil {

  private static final String TAG = ActivityUtil.class.getSimpleName();

  /*
   * Examples for how this is done elsewhere can be found in:
   * Examples for how this is done in Collect can be found in the Collect code
   * in org.odk.collect.android.tasks.SaveToDiskTask.java, in the
   * updateInstanceDatabase() method.
   */
  public static void editRow(
      AbsBaseActivity activity, TableProperties tp, Row row) {
    FormType formType = FormType.constructFormType(tp);
    if ( formType.isCollectForm() ) {
      Map<String, String> elementKeyToValue = new HashMap<String, String>();
      for (String elementKey : tp.getPersistedColumns()) {
        String value = row.getDataOrMetadataByElementKey(elementKey);
        elementKeyToValue.put(elementKey, value);
      }

      Intent intent = CollectUtil.getIntentForOdkCollectEditRow(
          activity,
          tp,
          elementKeyToValue,
          null,
          null,
          null,
          row.getRowId());

      if (intent != null) {
        CollectUtil.launchCollectToEditRow(activity, intent,
            row.getRowId());
      } else {
        Log.e(TAG, "intent null when trying to create for edit row.");
      }
    } else {
      SurveyFormParameters params = formType.getSurveyFormParameters();

      Intent intent = SurveyUtil.getIntentForOdkSurveyEditRow(
          activity,
          tp,
          activity.getAppName(),
          params,
          row.getRowId());
      if ( intent != null ) {
        SurveyUtil.launchSurveyToEditRow(activity, intent, tp,
            row.getRowId());
      }
    }
  }
  
  /**
   * Launch {@link TableLevelPreferencesActivity} to edit a table's
   * properties. Launches with request code
   * {@link Constants.RequestCodes#LAUNCH_TABLE_PREFS}.
   * @param activity
   * @param appName
   * @param tableId
   */
  public static void launchTableLevelPreferencesActivity(
      Activity activity,
      String appName,
      String tableId) {
    Intent intent = new Intent(activity, TableLevelPreferencesActivity.class);
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, appName);
    IntentUtil.addTableIdToBundle(bundle, tableId);
    intent.putExtras(bundle);
    activity.startActivityForResult(
        intent,
        Constants.RequestCodes.LAUNCH_TABLE_PREFS);
  }
  
  /**
   * Checks if the device is a tablet or a phone
   *
   * @param activityContext
   *          The Activity Context.
   * @return Returns true if the device is a Tablet
   */
  public static boolean isTabletDevice(Context activityContext) {
    // Verifies if the Generalized Size of the device is XLARGE to be
    // considered a Tablet
    boolean xlarge =
        ((activityContext.getResources().getConfiguration().screenLayout &
          Configuration.SCREENLAYOUT_SIZE_MASK) >=
          Configuration.SCREENLAYOUT_SIZE_LARGE);
    // If XLarge, checks if the Generalized Density is at least MDPI
    // (160dpi)
    if (xlarge) {
      DisplayMetrics metrics = new DisplayMetrics();
      Activity activity = (Activity) activityContext;
      activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
      // MDPI=160, DEFAULT=160, DENSITY_HIGH=240, DENSITY_MEDIUM=160,
      // DENSITY_TV=213, DENSITY_XHIGH=320
      if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT
          || metrics.densityDpi == DisplayMetrics.DENSITY_HIGH
          || metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM
          || metrics.densityDpi == DisplayMetrics.DENSITY_TV
          || metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) {
        // Yes, this is a tablet!
        return true;
      }
    }
    // No, this is not a tablet!
    return false;
  }

}
