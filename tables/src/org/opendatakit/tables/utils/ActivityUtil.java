/*
 * Copyright (C) 2012-2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;

public class ActivityUtil {

  private static final String TAG = ActivityUtil.class.getSimpleName();

  /*
   * Examples for how this is done elsewhere can be found in: Examples for how
   * this is done in Collect can be found in the Collect code in
   * org.odk.collect.android.tasks.SaveToDiskTask.java, in the
   * updateInstanceDatabase() method.
   */
  public static void editRow(AbsBaseActivity activity, String appName, String tableId,
      ArrayList<ColumnDefinition> orderedDefns, Row row) {
    FormType formType = FormType.constructFormType(activity, appName, tableId);
    if (formType.isCollectForm()) {
      Map<String, String> elementKeyToValue = new HashMap<String, String>();

      for (ColumnDefinition cd : orderedDefns) {
        if (cd.isUnitOfRetention()) {
          String value = row.getRawDataOrMetadataByElementKey(cd.getElementKey());
          elementKeyToValue.put(cd.getElementKey(), value);
        }
      }

      Intent intent = CollectUtil.getIntentForOdkCollectEditRow(activity, appName, tableId,
          orderedDefns, elementKeyToValue, null, null, null, row.getRowId());

      if (intent != null) {
        CollectUtil.launchCollectToEditRow(activity, intent, row.getRowId());
      } else {
        WebLogger.getLogger(appName).e(TAG, "intent null when trying to create for edit row.");
      }
    } else {
      SurveyFormParameters params = formType.getSurveyFormParameters();

      Intent intent = SurveyUtil.getIntentForOdkSurveyEditRow(activity, appName, tableId, params,
          row.getRowId());
      if (intent != null) {
        SurveyUtil.launchSurveyToEditRow(activity, tableId, intent, row.getRowId());
      }
    }
  }

  /**
   * Edit a row using the form specified by tableProperties.
   * 
   * @param activity
   *          the activity that should await the return
   * @param tableProperties
   * @param rowId
   */
  public static void editRow(AbsBaseActivity activity, String appName, String tableId,
      ArrayList<ColumnDefinition> orderedDefns, String rowId) {
    FormType formType = FormType.constructFormType(activity, appName, tableId);
    if (formType.isCollectForm()) {
      WebLogger.getLogger(appName).d(TAG, "[editRow] using collect form");
      CollectFormParameters collectFormParameters = CollectFormParameters
          .constructCollectFormParameters(activity, appName, tableId);
      WebLogger.getLogger(appName).d(TAG,
          "[editRow] is custom form: " + collectFormParameters.isCustom());
      CollectUtil.editRowWithCollect(activity, appName, tableId, orderedDefns, rowId,
          collectFormParameters);
    } else {
      WebLogger.getLogger(appName).d(TAG, "[editRow] using survey form");
      SurveyFormParameters surveyFormParameters = SurveyFormParameters
          .constructSurveyFormParameters(activity, appName, tableId);
      WebLogger.getLogger(appName).d(TAG,
          "[editRow] is custom form: " + surveyFormParameters.isUserDefined());
      SurveyUtil.editRowWithSurvey(activity, appName, tableId, rowId, surveyFormParameters);
    }
  }

  /**
   * Add a row to the table represented by tableProperties. The default form
   * settings will be used.
   * 
   * @param activity
   *          the activity to launch and await the return
   * @param tableProperties
   *          the table to which the row should be added. This is used to
   *          determine which form type and which app will perform the add.
   * @param prepopulatedValues
   *          a map of elementKey to value with which the new row should be
   *          prepopulated.
   */
  public static void addRow(AbsBaseActivity activity, String appName, String tableId,
      ArrayList<ColumnDefinition> orderedDefns, Map<String, String> prepopulatedValues) {
    FormType formType = FormType.constructFormType(activity, appName, tableId);
    if (formType.isCollectForm()) {
      WebLogger.getLogger(appName).d(TAG, "[onOptionsItemSelected] using Collect form");
      CollectFormParameters collectFormParameters = formType.getCollectFormParameters();
      WebLogger.getLogger(appName).d(TAG,
          "[onOptionsItemSelected] Collect form is custom: " + collectFormParameters.isCustom());
      CollectUtil.addRowWithCollect(activity, appName, tableId, orderedDefns,
          collectFormParameters, prepopulatedValues);
    } else {
      // survey form
      WebLogger.getLogger(appName).d(TAG, "[onOptionsItemSelected] using Survey form");
      SurveyFormParameters surveyFormParameters = formType.getSurveyFormParameters();
      WebLogger.getLogger(appName).d(TAG,
          "[onOptionsItemSelected] survey form is custom: " + surveyFormParameters.isUserDefined());
      SurveyUtil.addRowWithSurvey(activity, appName, tableId, surveyFormParameters,
          prepopulatedValues);
    }
  }

  /**
   * Launch {@link TableLevelPreferencesActivity} to edit a table's properties.
   * Launches with request code
   * {@link Constants.RequestCodes#LAUNCH_TABLE_PREFS}.
   * 
   * @param activity
   * @param appName
   * @param tableId
   */
  public static void launchTableLevelPreferencesActivity(Activity activity, String appName,
      String tableId, TableLevelPreferencesActivity.FragmentType fragmentTypeToDisplay) {
    Intent intent = new Intent(activity, TableLevelPreferencesActivity.class);
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, appName);
    IntentUtil.addTableIdToBundle(bundle, tableId);
    IntentUtil.addTablePreferenceFragmentTypeToBundle(bundle, fragmentTypeToDisplay);
    intent.putExtras(bundle);
    activity.startActivityForResult(intent, Constants.RequestCodes.LAUNCH_TABLE_PREFS);
  }

  /**
   * Launch {@link TableLevelPreferencesActivity} to edit a column's list of
   * color rules. Launches with request code
   * {@link Constants.RequestCodes#LAUNCH_COLOR_RULE_LIST}.
   * 
   * @param activity
   * @param appName
   * @param tableId
   * @param elementKey
   */
  public static void launchTablePreferenceActivityToEditColumnColorRules(Activity activity,
      String appName, String tableId, String elementKey) {
    Intent intent = new Intent(activity, TableLevelPreferencesActivity.class);
    Bundle extras = new Bundle();
    IntentUtil.addTablePreferenceFragmentTypeToBundle(extras,
        TableLevelPreferencesActivity.FragmentType.COLOR_RULE_LIST);
    IntentUtil.addAppNameToBundle(extras, appName);
    IntentUtil.addTableIdToBundle(extras, tableId);
    IntentUtil.addElementKeyToBundle(extras, elementKey);
    IntentUtil.addColorRuleGroupTypeToBundle(extras, ColorRuleGroup.Type.COLUMN);
    intent.putExtras(extras);
    activity.startActivityForResult(intent, Constants.RequestCodes.LAUNCH_COLOR_RULE_LIST);
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
    boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE);
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
