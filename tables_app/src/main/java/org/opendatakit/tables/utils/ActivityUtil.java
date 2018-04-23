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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import org.opendatakit.consts.RequestCodeConsts;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.database.data.Row;
import org.opendatakit.database.data.TypedRow;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.provider.DataTableColumns;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

import java.util.Map;

/**
 * Some utilities, mostly used for launching survey
 */
public final class ActivityUtil {

  /**
   * Used for logging
   */
  private static final String TAG = ActivityUtil.class.getSimpleName();

  /**
   * Do not instantiate this
   */
  private ActivityUtil() {
  }

  /**
   * Switch to survey to edit a row in SpreadsheetFragment
   *
   * @param activity the activity to use to get the intent to switch to survey
   * @param appName  the app name
   * @param tableId  the id for the table that contains the row to edit
   * @param row      the exact row to edit
   * @throws ServicesAvailabilityException if the database is down
   */
  public static void editRow(AbsBaseActivity activity, String appName, String tableId, TypedRow row)
      throws ServicesAvailabilityException {
    FormType formType = FormType.constructFormType(activity, appName, tableId);

    // If no formId has been specified, show toast and exit
    if (formType.getFormId() == null) {
      Toast.makeText(activity, R.string.no_form_id_specified, Toast.LENGTH_LONG).show();
      return;
    }

    SurveyFormParameters params = formType.getSurveyFormParameters();

    Intent intent = SurveyUtil.getIntentForOdkSurveyEditRow(appName, tableId, params,
        row.getStringValueByKey(DataTableColumns.ID));
    if (intent != null) {
      SurveyUtil
          .launchSurveyToEditRow(activity, tableId, intent, row.getStringValueByKey(DataTableColumns.ID));
    }
  }

  /**
   * Edit a row using the form specified by tableProperties. Currently used by TableDisplayActivity
   *
   * @param activity the activity to use to make the survey form parameters
   * @param appName  the app name
   * @param tableId  the id of the table that contains the row to edit
   * @param rowId    the id of the row to edit
   * @throws ServicesAvailabilityException if the database is down
   */
  public static void editRow(AbsBaseActivity activity, String appName, String tableId, String rowId)
      throws ServicesAvailabilityException {
    WebLogger.getLogger(appName).d(TAG, "[editRow] using survey form");
    SurveyFormParameters surveyFormParameters = SurveyFormParameters
        .constructSurveyFormParameters(activity, appName, tableId);

    // If no formId has been specified, show toast and exit
    if (surveyFormParameters.getFormId() == null) {
      Toast.makeText(activity, R.string.no_form_id_specified, Toast.LENGTH_LONG).show();
      return;
    }

    WebLogger.getLogger(appName)
        .d(TAG, "[editRow] is custom form: " + surveyFormParameters.isUserDefined());
    SurveyUtil.editRowWithSurvey(activity, appName, tableId, rowId, surveyFormParameters);
  }

  /**
   * Add a row to the table represented by tableProperties. The default form
   * settings will be used.  Currently used by TableDisplayActivity and LocationDialogFragment
   *
   * @param activity           the activity to launch and await the return
   * @param appName            the app name
   * @param tableId            the id of the table to add a row to
   * @param prepopulatedValues a map of elementKey to value with which the new row should be
   *                           prepopulated.
   * @throws ServicesAvailabilityException if the database is down
   */
  public static void addRow(AbsBaseActivity activity, String appName, String tableId,
      Map<String, Object> prepopulatedValues) throws ServicesAvailabilityException {
    FormType formType = FormType.constructFormType(activity, appName, tableId);

    // If no formId has been specified, show toast and exit
    if (formType.getFormId() == null) {
      Toast.makeText(activity, R.string.no_form_id_specified, Toast.LENGTH_LONG).show();
      return;
    }
    // survey form
    WebLogger.getLogger(appName).d(TAG, "[onOptionsItemSelected] using Survey form");
    SurveyFormParameters surveyFormParameters = formType.getSurveyFormParameters();
    WebLogger.getLogger(appName).d(TAG,
        "[onOptionsItemSelected] survey form is custom: " + surveyFormParameters.isUserDefined());
    SurveyUtil
        .addRowWithSurvey(activity, appName, tableId, surveyFormParameters, prepopulatedValues);
  }

  /**
   * Launch {@link TableLevelPreferencesActivity} to edit a table's properties.
   * Launches with request code
   * {@link RequestCodeConsts.RequestCodes#LAUNCH_TABLE_PREFS}.
   *
   * @param activity              the activity to start
   * @param appName               the app name
   * @param tableId               the id of the table to edit the preferences of
   * @param fragmentTypeToDisplay put in the bundle
   */
  public static void launchTableLevelPreferencesActivity(Activity activity, String appName,
      String tableId, TableLevelPreferencesActivity.FragmentType fragmentTypeToDisplay) {
    Intent intent = new Intent(activity, TableLevelPreferencesActivity.class);
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, appName);
    IntentUtil.addTableIdToBundle(bundle, tableId);
    IntentUtil.addTablePreferenceFragmentTypeToBundle(bundle, fragmentTypeToDisplay);
    intent.putExtras(bundle);
    activity.startActivityForResult(intent, RequestCodeConsts.RequestCodes.LAUNCH_TABLE_PREFS);
  }

  /**
   * Launch {@link TableLevelPreferencesActivity} to edit a column's list of
   * color rules. Launches with request code
   * {@link RequestCodeConsts.RequestCodes#LAUNCH_COLOR_RULE_LIST}.
   *
   * @param activity   the activity to launch
   * @param appName    the app name
   * @param tableId    the id of the table to edit
   * @param elementKey put in the bundle
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
    activity.startActivityForResult(intent, RequestCodeConsts.RequestCodes.LAUNCH_COLOR_RULE_LIST);
  }

  /**
   * Launch {@link TableLevelPreferencesActivity} to edit a column's preferences. Launches with
   * request code {@link RequestCodeConsts.RequestCodes#LAUNCH_COLUMN_PREFS}.
   *
   * @param activity   the activity to launch
   * @param appName    the app name
   * @param tableId    the id of the table to edit
   * @param elementKey put in the bundle
   */
  public static void launchTablePreferenceActivityToEditColumn(Activity activity, String appName,
      String tableId, String elementKey) {
    Intent intent = new Intent(activity, TableLevelPreferencesActivity.class);
    Bundle extras = new Bundle();
    IntentUtil.addTablePreferenceFragmentTypeToBundle(extras,
        TableLevelPreferencesActivity.FragmentType.COLUMN_PRFERENCE);
    IntentUtil.addAppNameToBundle(extras, appName);
    IntentUtil.addTableIdToBundle(extras, tableId);
    IntentUtil.addElementKeyToBundle(extras, elementKey);
    intent.putExtras(extras);
    activity.startActivityForResult(intent, RequestCodeConsts.RequestCodes.LAUNCH_COLOR_RULE_LIST);
  }

}
