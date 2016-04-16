/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.views.webkits;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import org.json.JSONArray;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.views.ODKWebView;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.*;
import org.opendatakit.tables.utils.Constants.RequestCodes;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

import java.lang.ref.WeakReference;
import java.util.*;

public class OdkTables {

  private static final String TAG = OdkTables.class.getSimpleName();

  private WeakReference<ODKWebView> mWebView;
  protected AbsBaseActivity mActivity;
  protected String mDefaultTableId;
  protected Map<String, OrderedColumns> mCachedOrderedDefns = new HashMap<String, OrderedColumns>();
  private List<String> mTableIds = null;

  public OdkTablesIf getJavascriptInterfaceWithWeakReference() {
    return new OdkTablesIf(this);
  }

  /**
   * This construct requires an activity rather than a context because we want
   * to be able to launch intents for result rather than merely launch them on
   * their own.
   *
   * @param activity
   *          the activity that will be holding the view
   * @throws RemoteException 
   */
  public OdkTables(AbsBaseActivity activity, ODKWebView webView, String defaultTableId) {
    this.mActivity = activity;
    this.mWebView = new WeakReference<ODKWebView>(webView);
    this.mDefaultTableId = defaultTableId;
  }

  public boolean isInactive() {
    return (mWebView.get() == null) || mWebView.get().isInactive();
  }

  private List<String> getTableIds() throws RemoteException {
    if ( mTableIds == null ) {
      String appName = mActivity.getAppName();
      OdkDbHandle db = null;
      try {
        db = Tables.getInstance().getDatabase().openDatabase(appName);
        mTableIds = Tables.getInstance().getDatabase().getAllTableIds(appName, db);
      } finally {
        if (db != null) {
          Tables.getInstance().getDatabase().closeDatabase(appName, db);
        }
      }
    }
    return mTableIds;
  }

  /**
   * Return the ordered array of ColumnDefinition objects.
   * 
   * @param tableId
   * @return
   * @throws RemoteException 
   */
  synchronized OrderedColumns retrieveColumnDefinitions(OdkDbHandle db,
      String tableId) throws RemoteException {

    OrderedColumns answer = this.mCachedOrderedDefns.get(tableId);
    if (answer != null) {
      return answer;
    }

    String appName = mActivity.getAppName();
    answer = Tables.getInstance().getDatabase().getUserDefinedColumns(appName, db, tableId);
    this.mCachedOrderedDefns.put(tableId, answer);
    return answer;
  }

  /**
   * Start the detail view.
   * 
   * @param tableId
   * @param rowId
   * @param relativePath
   * @return
   */
  boolean helperLaunchDetailView(String tableId, String rowId, String relativePath) {
    Bundle bundle = new Bundle();
    String appName = retrieveAppName();
    IntentUtil.addDetailViewKeysToIntent(bundle, appName, tableId, rowId, relativePath);
    Intent intent = this.getTableDisplayActivityIntentWithBundle(bundle);
    this.mActivity.startActivityForResult(intent, RequestCodes.LAUNCH_VIEW);
    return true;
  }

  /**
   * Retrieve the app name that should be used from the parent activity.
   * 
   * @return
   */
  String retrieveAppName() {
    if (!(this.mActivity instanceof AbsBaseActivity)) {
      throw new IllegalStateException(OdkTables.class.getSimpleName() + " must be have an "
          + AbsBaseActivity.class.getSimpleName());
    }
    AbsBaseActivity baseActivity = (AbsBaseActivity) this.mActivity;
    return baseActivity.getAppName();
  }

  /**
   * @see {@link OdkTablesIf#openDetailView(String, String, String)}
   */
  public boolean openDetailViewWithFile(String tableId, String rowId, String relativePath) {
    return this.helperLaunchDetailView(tableId, rowId, relativePath);
  }

  /**
   * Actually open the table. The sql-related parameters are null safe, so only
   * pass them in if necessary.
   *
   * @see {@see OdkTablesIf#openTableWithSqlQuery(String, String, String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTable(String tableId, String sqlWhereClause, String[] sqlSelectionArgs,
      String[] sqlGroupBy, String sqlHaving, String sqlOrderByElementName,
      String sqlOrderByDirection) {
    return this.helperLaunchDefaultView(tableId, sqlWhereClause, sqlSelectionArgs, sqlGroupBy,
        sqlHaving, sqlOrderByElementName, sqlOrderByDirection);
  }

  /**
   * Actually open the table. The sql-related parameters are null safe, so only
   * pass them in if necessary.
   *
   * @see {@see OdkTablesIf#openTableWithSqlQuery(String, String, String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  boolean helperLaunchDefaultView(String tableId, String sqlWhereClause, String[] sqlSelectionArgs,
      String[] sqlGroupBy, String sqlHaving, String sqlOrderByElementKey, String sqlOrderByDirection) {
    return this.helperLaunchView(tableId, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection, null, null);
  }

  /**
   * Open the table to the given view type. The relativePath parameter is null
   * safe, and if not present will not be added. The default behavior of the
   * corresponding fragment will be followed, which should be to try and use a
   * default file.
   * 
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @param sqlGroupBy
   * @param sqlHaving
   * @param sqlOrderByElementKey
   * @param sqlOrderByDirection
   * @param viewType
   *          the view type. Cannot be {@link ViewFragmentType#DETAIL}, which
   *          has its own method,
   *          {@link #helperLaunchDefaultView(String, String, String[], String[], String, String, String)}
   *          with additional parameters.
   * @param relativePath
   * @return
   * @throws IllegalArgumentException
   *           if viewType is {@link ViewFragmentType#DETAIL}.
   */
  boolean helperLaunchView(String tableId, String sqlWhereClause, String[] sqlSelectionArgs,
      String[] sqlGroupBy, String sqlHaving, String sqlOrderByElementKey,
      String sqlOrderByDirection, ViewFragmentType viewType, String relativePath) {
    if (viewType == ViewFragmentType.DETAIL) {
      throw new IllegalArgumentException("Cannot use this method to "
          + "launch a detail view. Use helperLaunchDetailView instead.");
    }
    Bundle bundle = new Bundle();
    IntentUtil.addSQLKeysToBundle(bundle, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection);
    IntentUtil.addTableIdToBundle(bundle, tableId);
    IntentUtil.addFragmentViewTypeToBundle(bundle, viewType);
    IntentUtil.addFileNameToBundle(bundle, relativePath);
    Intent intent = getTableDisplayActivityIntentWithBundle(bundle);
    this.mActivity.startActivityForResult(intent, RequestCodes.LAUNCH_VIEW);
    return true;
  }

  /**
   * Create a new {@link Intent} to launch {@link TableDisplayActivity} with the
   * contents of bundle added to the intent's extras. The appName is already
   * added to the bundle.
   * 
   * @param bundle
   * @return
   */
  private Intent getTableDisplayActivityIntentWithBundle(Bundle bundle) {
    Intent intent = new Intent(this.mActivity, TableDisplayActivity.class);
    intent.putExtras(bundle);
    String appName = retrieveAppName();
    IntentUtil.addAppNameToBundle(intent.getExtras(), appName);
    return intent;
  }

  /**
   * Actually open the table with the file. see
   * {@see OdkTablesIf#launchListView(String, String, String, String[], String[], String, String, String)}
   *
   * @param tableId
   * @param relativePath
   *          the path relative to the app folder
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTableWithFile(String tableId, String relativePath,
      String sqlWhereClause, String[] sqlSelectionArgs, String[] sqlGroupBy, String sqlHaving,
      String sqlOrderByElementKey, String sqlOrderByDirection) {
    // ViewFragmentType.LIST displays a file with information about the
    // entire table, so we use that here.
    return this.helperLaunchView(tableId, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection, ViewFragmentType.LIST, relativePath);
  }

  /**
   * Open the table to the map view.
   *
   * @see {@see OdkTablesIf#openTableToMapViewWithSqlQuery(String, String, String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTableToMapView(String tableId, String relativePath,
      String sqlWhereClause, String[] sqlSelectionArgs, String[] sqlGroupBy, String sqlHaving,
      String sqlOrderByElementKey, String sqlOrderByDirection) {
    String appName = mActivity.getAppName();
    WebLogger.getLogger(appName).e(TAG, "NOTE THAT THE SPECIFIC MAP VIEW FILE IS NOT SUPPORTED");
    return this.helperLaunchView(tableId, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection, ViewFragmentType.MAP, relativePath);
  }

  /**
   * Open the table to the spreadsheet view.
   *
   * @see {@see OdkTablesIf#openTableToSpreadsheetViewWithSqlQuery(String, String, String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTableToSpreadsheetView(String tableId, String sqlWhereClause,
      String[] sqlSelectionArgs, String[] sqlGroupBy, String sqlHaving,
      String sqlOrderByElementKey, String sqlOrderByDirection) {
    // No relativePath for spreadsheet.
    return this.helperLaunchView(tableId, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection, ViewFragmentType.SPREADSHEET, null);
  }

  /**
   * Launch the with the custom filename to
   * display. The return type on this method currently is always true, should
   * probably check if the file exists first.
   *
   * @param relativePath
   */
  public boolean launchHTML(String relativePath) {
    String appName = mActivity.getAppName();
    WebLogger.getLogger(appName).d(TAG, "[launchHTML] launching relativePath: " + relativePath);
    Intent intent = new Intent(this.mActivity, MainActivity.class);
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, appName);
    IntentUtil.addFileNameToBundle(bundle, relativePath);
    intent.putExtras(bundle);
    this.mActivity.startActivityForResult(intent, Constants.RequestCodes.LAUNCH_WEB_VIEW);
    return true;
  }

  /**
   * Add a row with survey using the specified formId and screenPath. The
   * jsonMap should be a Stringified json map mapping elementName to values to
   * prepopulate with the add row request.
   *
   * @param tableId
   * @param formId
   *          if null, uses the default form
   * @param screenPath
   * @param jsonMap
   * @return true if the launch succeeded, false if something went wrong
   * @throws RemoteException 
   */
  public boolean helperAddRowWithSurvey(String tableId, String formId, String screenPath,
      String jsonMap) throws RemoteException {
    String appName = mActivity.getAppName();
    // does this "to receive add" call make sense with survey? unclear.
    if (!getTableIds().contains(tableId)) {
      WebLogger.getLogger(appName).e(TAG,
          "table [" + tableId + "] could not be found. " + "returning.");
      return false;
    }
    SurveyFormParameters surveyFormParameters = null;
    if (formId == null) {
      surveyFormParameters = SurveyFormParameters.constructSurveyFormParameters(mActivity,
          appName, tableId);
      formId = surveyFormParameters.getFormId();
    } else {
      surveyFormParameters = new SurveyFormParameters(true, formId, screenPath);
    }
    Map<String, String> map = null;
    // Do this null check and only parse and return errors if the jsonMap
    // is not null. This allows other methods doing similar things to call
    // through using this method and passing null values.
    if (jsonMap != null) {
      map = WebViewUtil.getMapFromJson(appName, jsonMap);
      if (map == null) {
        WebLogger.getLogger(appName).e(TAG, "couldn't parse values into map to give to Survey");
        return false;
      }
    }
    SurveyUtil.addRowWithSurvey(this.mActivity, appName, tableId, surveyFormParameters, map);
    return true;
  }

  /**
   * Launch survey to edit the row.
   *
   * @param tableId
   * @param rowId
   * @param formId
   * @param screenPath
   * @return true if the edit was launched successfully, else false
   * @throws RemoteException 
   */
  public boolean helperEditRowWithSurvey(String tableId, String rowId, String formId,
      String screenPath) throws RemoteException {
    String appName = mActivity.getAppName();
    if (!getTableIds().contains(tableId)) {
      WebLogger.getLogger(appName).e(TAG,
          "table [" + tableId + "] could not be found. " + "returning.");
      return false;
    }
    SurveyFormParameters surveyFormParameters = null;
    if (formId == null) {
      surveyFormParameters = SurveyFormParameters.constructSurveyFormParameters(mActivity,
          appName, tableId);
    } else {
      surveyFormParameters = new SurveyFormParameters(true, formId, screenPath);
    }
    SurveyUtil.editRowWithSurvey(this.mActivity, appName, tableId, rowId,
        surveyFormParameters);
    return true;
  }
}
