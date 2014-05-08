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
import android.content.Intent;
import android.os.Bundle;
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

}
