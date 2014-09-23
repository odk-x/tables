package org.opendatakit.tables.views.webkits;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.UrlUtils;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.activities.WebViewActivity;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.ColumnUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.Constants.RequestCodes;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.ODKDatabaseUtilsWrapper;
import org.opendatakit.tables.utils.SurveyUtil;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;
import org.opendatakit.tables.utils.WebViewUtil;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class Control {

  private static final String TAG = Control.class.getSimpleName();

  protected AbsBaseActivity mActivity;
  protected String mAppName;

  public Object getJavascriptInterfaceWithWeakReference() {
    return new ControlIf(this);
  }

  // hold onto references to all the results returned to the WebKit
  private LinkedList<TableData> queryResults = new LinkedList<TableData>();

  /**
   * This construct requires an activity rather than a context because we want
   * to be able to launch intents for result rather than merely launch them on
   * their own.
   *
   * @param activity
   *          the activity that will be holding the view
   */
  public Control(AbsBaseActivity activity, String appName) {
    this.mActivity = activity;
    this.mAppName = appName;
  }

  /**
   * Retrieve the {@link TableProperties} for the given tableId.
   * @param tableId
   * @return
   */
  TableProperties retrieveTablePropertiesForTable(String tableId) {
    TableProperties result =
        TableProperties.getTablePropertiesForTable(
          this.mActivity,
          this.mAppName,
          tableId);
    return result;
  }

  /**
   * Start the detail view.
   * @param tableId
   * @param rowId
   * @param relativePath
   * @return
   */
  boolean helperLaunchDetailView(
      String tableId,
      String rowId,
      String relativePath) {
    Bundle bundle = new Bundle();
    String appName = retrieveAppName();
    IntentUtil.addDetailViewKeysToIntent(
        bundle,
        appName,
        tableId,
        rowId,
        relativePath);
    Intent intent = this.getTableDisplayActivityIntentWithBundle(bundle);
    this.mActivity.startActivityForResult(intent, RequestCodes.LAUNCH_VIEW);
    return true;
  }

  /**
   * Retrieve the app name that should be used from the parent activity.
   * @return
   */
  String retrieveAppName() {
    if (!(this.mActivity instanceof AbsBaseActivity)) {
      throw new IllegalStateException(
          Control.class.getSimpleName() +
          " must be have an " +
          AbsBaseActivity.class.getSimpleName());
    }
    AbsBaseActivity baseActivity = (AbsBaseActivity) this.mActivity;
    return baseActivity.getAppName();
  }


  /**
   * @see {@link ControlIf#openDetailView(String, String, String)}
   */
  public boolean openDetailViewWithFile(
      String tableId,
      String rowId,
      String relativePath) {
    return this.helperLaunchDetailView(tableId, rowId, relativePath);
  }

  /**
   * Actually open the table. The sql-related parameters are null safe, so
   * only pass them in if necessary.
   *
   * @see {@link ControlIf#openTableWithSqlQuery(String, String, String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTable(
      String tableId,
      String sqlWhereClause,
      String[] sqlSelectionArgs,
      String[] sqlGroupBy,
      String sqlHaving,
      String sqlOrderByElementName,
      String sqlOrderByDirection) {
    return this.helperLaunchDefaultView(
        tableId,
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementName,
        sqlOrderByDirection);
  }

  /**
   * Actually open the table. The sql-related parameters are null safe, so
   * only pass them in if necessary.
   *
   * @see {@link ControlIf#openTableWithSqlQuery(String, String, String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  boolean helperLaunchDefaultView(
      String tableId,
      String sqlWhereClause,
      String[] sqlSelectionArgs,
      String[] sqlGroupBy,
      String sqlHaving,
      String sqlOrderByElementKey,
      String sqlOrderByDirection) {
    return this.helperLaunchView(
        tableId,
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection,
        null,
        null);
  }

  /**
   * Open the table to the given view type. The relativePath parameter is
   * null safe, and if not present will not be added. The default behavior
   * of the corresponding fragment will be followed, which should be to try
   * and use a default file.
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @param sqlGroupBy
   * @param sqlHaving
   * @param sqlOrderByElementKey
   * @param sqlOrderByDirection
   * @param viewType the view type. Cannot be
   *  {@link ViewFragmentType#DETAIL}, which has its own method,
   *  {@link #helperLaunchDefaultView(String, String, String[], String[],
   *  String, String, String)}
   *  with additional parameters.
   * @param relativePath
   * @return
   * @throws IllegalArgumentException if viewType is
   *  {@link ViewFragmentType#DETAIL}.
   */
  boolean helperLaunchView(
      String tableId,
      String sqlWhereClause,
      String[] sqlSelectionArgs,
      String[] sqlGroupBy,
      String sqlHaving,
      String sqlOrderByElementKey,
      String sqlOrderByDirection,
      ViewFragmentType viewType,
      String relativePath) {
    if (viewType == ViewFragmentType.DETAIL) {
      throw new IllegalArgumentException("Cannot use this method to " +
          "launch a detail view. Use helperLaunchDetailView instead.");
    }
    Bundle bundle = new Bundle();
    IntentUtil.addSQLKeysToBundle(
        bundle,
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection);
    IntentUtil.addTableIdToBundle(bundle, tableId);
    IntentUtil.addFragmentViewTypeToBundle(bundle, viewType);
    IntentUtil.addFileNameToBundle(bundle, relativePath);
    Intent intent = getTableDisplayActivityIntentWithBundle(bundle);
    this.mActivity.startActivityForResult(intent, RequestCodes.LAUNCH_VIEW);
    return true;
  }

  /**
   * Create a new {@link Intent} to launch {@link TableDisplayActivity} with
   * the contents of bundle added to the intent's extras. The appName is
   * already added to the bundle.
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
   * {@link ControlIf#launchListView(String, String, String, String[],
   * String[], String, String, String)}
   *
   * @param tableId
   * @param relativePath
   *          the path relative to the app folder
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTableWithFile(
      String tableId,
      String relativePath,
      String sqlWhereClause,
      String[] sqlSelectionArgs,
      String[] sqlGroupBy,
      String sqlHaving,
      String sqlOrderByElementKey,
      String sqlOrderByDirection) {
    // ViewFragmentType.LIST displays a file with information about the
    // entire table, so we use that here.
    return this.helperLaunchView(
        tableId,
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection,
        ViewFragmentType.LIST,
        relativePath);
  }

  /**
   * Open the table to the map view.
   *
   * @see {@link ControlIf#openTableToMapViewWithSqlQuery(String, String,
   * String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTableToMapView(
      String tableId,
      String relativePath,
      String sqlWhereClause,
      String[] sqlSelectionArgs,
      String[] sqlGroupBy,
      String sqlHaving,
      String sqlOrderByElementKey,
      String sqlOrderByDirection) {
    Log.e(TAG, "NOTE THAT THE SPECIFIC MAP VIEW FILE IS NOT SUPPORTED");
    return this.helperLaunchView(
        tableId,
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection,
        ViewFragmentType.MAP,
        relativePath);
  }

  /**
   * Open the table to the spreadsheet view.
   *
   * @see {@link ControlIf#openTableToSpreadsheetViewWithSqlQuery(String,
   * String, String[])}
   * @param tableId
   * @param sqlWhereClause
   * @param sqlSelectionArgs
   * @return
   */
  public boolean helperOpenTableToSpreadsheetView(
      String tableId,
      String sqlWhereClause,
      String[] sqlSelectionArgs,
      String[] sqlGroupBy,
      String sqlHaving,
      String sqlOrderByElementKey,
      String sqlOrderByDirection) {
    // No relativePath for spreadsheet.
    return this.helperLaunchView(
        tableId,
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection,
        ViewFragmentType.SPREADSHEET,
        null);
  }

  /**
   * @see {@link ControlIf#releaseQueryResources(String)}
   */
  public void releaseQueryResources(String tableId) {
    Iterator<TableData> iter = queryResults.iterator();
    while (iter.hasNext()) {
      TableData td = iter.next();
      if (td.getTableId().equals(tableId)) {
        iter.remove();
      }
    }
  }

  /**
   * Query the database using sql. Only returns the columns for the table
   * specified by the tableName parameter.
   * <p>
   * Any arguments in the WHERE statement must be replaced by "?" and
   * contained in order in the selectionArgs array. In this sense it is
   * exactly a sql select minus the WHERE.
   * <p>
   * For example, if you wanted all the rows where the column foo equaled bar,
   * the where clause would be "foo = ? " and the selection args would be
   * ["bar"].
   *
   * @see {@link ControlIf#query(String, String, String[])}
   *
   * @param tableId
   *          the display name of the table for which you want the columns to
   *          be returned.
   * @param whereClause
   *          the where clause for the selection. This is the body of the "WHERE"
   *          clause WITHOUT the "WHERE"
   *          References to other tables, e.g. for joins, in this statement
   *          must use the name of the table as returned by
   *          {@link getDbNameForTable}.
   * @param selectionArgs
   * @return
   */
  public TableData query(
      String tableId,
      String whereClause,
      String[] selectionArgs,
      String[] groupBy,
      String having,
      String orderByElementKey,
      String orderByDirection) {
    TableData tableData = queryForTableData(
        tableId,
        whereClause, selectionArgs,
        groupBy, having, orderByElementKey, orderByDirection);
    /**
     * IMPORTANT: remember the td. The interfaces will hold weak references
     * to them, so we need a strong reference to prevent GC.
     */
    queryResults.add(tableData);
    return tableData;
  }

  /**
   * @see {@link ControlIf#getAllTableIds()}
   */
  public String getAllTableIds() {
    ArrayList<String> tableIdsList = new ArrayList<String>();
    tableIdsList.addAll(TableProperties.getAllTableIds(this.mActivity, this.mAppName));

    JSONArray result = new JSONArray(tableIdsList);
    return result.toString();
  }

  /**
   * @see {@link ControlIf#getElementKey(String, String)}
   * @param tableId
   * @param elementPath
   * @return
   */
  public String getElementKey(String tableId, String elementPath) {
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      return null;
    }
    return tp.getElementKeyFromElementPath(elementPath);
  }

  /**
   * @see {@link ControlIf#getColumnDisplayName(String, String)}
   * @param tableId
   * @param elementPath
   * @return
   */
  public String getColumnDisplayName(String tableId, String elementPath) {
    String elementKey = this.getElementKey(tableId, elementPath);
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      return null;
    }
    try {
      ColumnDefinition cd = tp.getColumnDefinitionByElementKey(elementKey);
      return ColumnUtil.getLocalizedDisplayName(tp, elementKey);
    } catch ( IllegalArgumentException e ) {
      Log.e(TAG, "column with elementKey does not exist: " + elementKey);
      return null;
    }
  }

  /**
   * @see {@link ControlIf#getTableDisplayName(String)}
   * @param tableId
   * @return
   */
  public String getTableDisplayName(String tableId) {
    TableProperties tp = TableProperties.getTablePropertiesForTable(
        this.mActivity,
        this.mAppName,
        tableId);
    return tp.getLocalizedDisplayName();
  }
  
  public boolean updateRow(
      String tableId,
      String stringifiedJSON,
      String rowId) {
    return helperAddOrUpdateRow(tableId, stringifiedJSON, rowId, true);
  }
  
  protected ContentValues getContentValuesFromMap(
      TableProperties tableProperties,
      Map<String, String> elementKeyToValue) {
    return WebViewUtil.getContentValuesFromMap(
        tableProperties,
        elementKeyToValue);
  }
  
  /**
   * Add a row. Returns the id of the added row is successful or null if the
   * add failed.
   * @param tableId
   * @param stringifiedJSON
   * @return
   */
  public String addRow(String tableId, String stringifiedJSON) {
    String rowId = this.generateRowId();
    boolean addSuccessful =
        helperAddOrUpdateRow(tableId, stringifiedJSON, rowId, false);
    if (addSuccessful) {
      return rowId;
    } else {
      return null;
    }
  }
  
  /**
   * Generate a row id. Eventually this should be moved to a common function
   * provided by {@link ODKDatabaseUtils} or something similar so that we can
   * more easily remain consistent.
   * @return
   */
  protected String generateRowId() {
    String result = "uuid:" + UUID.randomUUID().toString();
    return result;
  }
  
  /**
   * Add or update a row. If isUpdate is false, add is called.
   * is called.
   * @param tableId
   * @param stringifiedJSON
   * @param rowId cannot be null.
   * @return
   * @throws IllegalArgumentException if rowId is null.
   */
  protected boolean helperAddOrUpdateRow(
      String tableId,
      String stringifiedJSON,
      String rowId,
      boolean isUpdate) {
    if (rowId == null) {
      throw new IllegalArgumentException("row id cannot be null");
    }
    TableProperties tableProperties =
        this.retrieveTablePropertiesForTable(tableId);
    if (tableProperties == null) {
      Log.e(TAG, "[addRow] cannot find table for id: " + tableId);
      return false;
    }
     Map<String, String> elementKeyToValue;
     if (stringifiedJSON == null) {
       // this case will let us add an empty row if null is passed.
       elementKeyToValue = new HashMap<String, String>();
     } else {
       elementKeyToValue = WebViewUtil.getMapFromJson(stringifiedJSON);
     }
     ContentValues contentValues = getContentValuesFromMap(
         tableProperties,
         elementKeyToValue);
     if (contentValues == null) {
       // something went wrong parsing.
       return false;
     }
     // If we've made it here, all appears to be well.
     SQLiteDatabase writableDatabase = getWritableDatabase();
     ODKDatabaseUtilsWrapper dbUtils = this.getODKDatabaseUtilsWrapper();
     ArrayList<Column> columns = dbUtils.getUserDefinedColumns(writableDatabase, tableId);
     ArrayList<ColumnDefinition> orderedColumns = ColumnDefinition.buildColumnDefinitions(columns);
     if (isUpdate) {
       dbUtils.updateDataInExistingDBTableWithId(
           writableDatabase,
           tableId,
           orderedColumns,
           contentValues,
           rowId);
       
     } else {
       dbUtils.insertDataIntoExistingDBTableWithId(
           writableDatabase,
           tableId,
           orderedColumns,
           contentValues,
           rowId);
     }
     return true;
  }

  /**
   * Very basic method to facilitate testing.
   * @return
   */
  protected SQLiteDatabase getWritableDatabase() {
    DataModelDatabaseHelper dmDbHelper =
        DataModelDatabaseHelperFactory.getDbHelper(mActivity, mAppName);
    SQLiteDatabase result = dmDbHelper.getWritableDatabase();
    return result;
  }

  /**
   * Very basic method to facilitate stubbing and injection of a mock object.
   * @return
   */
  protected ODKDatabaseUtilsWrapper getODKDatabaseUtilsWrapper() {
    ODKDatabaseUtilsWrapper result = new ODKDatabaseUtilsWrapper();
    return result;
  }

  /**
   * @see {@link ControlIf#getPlatformInfo()}
   * @return
   */
  public String getPlatformInfo() {
    // This is based on:
    // org.opendatakit.survey.android.views.ODKShimJavascriptCallback
    Map<String, String> platformInfo = new HashMap<String, String>();
    platformInfo.put(PlatformInfoKeys.VERSION, Build.VERSION.RELEASE);
    platformInfo.put(PlatformInfoKeys.CONTAINER, "Android");
    platformInfo.put(PlatformInfoKeys.APP_NAME, this.mAppName);
    platformInfo.put(PlatformInfoKeys.BASE_URI, getBaseContentUri());
    platformInfo.put(PlatformInfoKeys.LOG_LEVEL, "D");
    JSONObject jsonObject = new JSONObject(platformInfo);
    String result = jsonObject.toString();
    return result;
  }

  /**
   * @see {@link ControlIf#getFileAsUrl(String)}
   * @param relativePath
   * @return
   */
  public String getFileAsUrl(String relativePath) {
    String baseUri = getBaseContentUri();
    String result = baseUri + relativePath;
    return result;
  }

  /**
   * @see ControlIf#columnExists(String, String)
   * @param tableId
   * @param elementPath
   * @return
   */
  public boolean columnExists(String tableId, String elementPath) {
    String elementKey = this.getElementKey(tableId, elementPath);
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      return false;
    }
    try {
      tp.getColumnDefinitionByElementKey(elementKey);
      return true;
    } catch ( IllegalArgumentException e ) {
      return false;
    }
  }

  /**
   * Return the base uri for the Tables app name with a trailing separator.
   *
   * @return
   */
  private String getBaseContentUri() {
    Uri contentUri = UrlUtils.getWebViewContentUri(this.mActivity);
    contentUri = Uri.withAppendedPath(
        contentUri,
        Uri.encode(this.mAppName));
    return contentUri.toString() + File.separator;
  }

  /**
   * Launch the {@link CustomHomeScreenActivity} with the custom filename to
   * display. The return type on this method currently is always true, should
   * probably check if the file exists first.
   *
   * @param relativePath
   */
  public boolean launchHTML(String relativePath) {
    Log.d(TAG, "[launchHTML] launching relativePath: " + relativePath);
    Intent intent = new Intent(this.mActivity, WebViewActivity.class);
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, this.mAppName);
    IntentUtil.addFileNameToBundle(bundle, relativePath);
    intent.putExtras(bundle);
    this.mActivity.startActivityForResult(
        intent,
        Constants.RequestCodes.LAUNCH_WEB_VIEW);
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
   */
  public boolean helperAddRowWithSurvey(
      String tableId,
      String formId,
      String screenPath,
      String jsonMap) {
    // does this "to receive add" call make sense with survey? unclear.
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      Log.e(
          TAG,
          "table [" + tableId + "] could not be found. " + "returning.");
      return false;
    }
    SurveyFormParameters surveyFormParameters = null;
    if (formId == null) {
      surveyFormParameters = SurveyFormParameters
          .constructSurveyFormParameters(tp);
      formId = surveyFormParameters.getFormId();
    } else {
      surveyFormParameters = new SurveyFormParameters(
          true,
          formId,
          screenPath);
    }
    Map<String, String> map = null;
    // Do this null check and only parse and return errors if the jsonMap
    // is not null. This allows other methods doing similar things to call
    // through using this method and passing null values.
    if (jsonMap != null) {
      map = WebViewUtil.getMapFromJson(jsonMap);
      if (map == null) {
        Log.e(TAG, "couldn't parse values into map to give to Survey");
        return false;
      }
    }
    SurveyUtil.addRowWithSurvey(
        this.mActivity,
        this.mAppName,
        tp,
        surveyFormParameters,
        map);
    return true;
  }

  /**
   * Add a row with collect.
   * <p>
   * The check for null on formId is to try and minimize the amount of
   * similarly-named methods in this class, which makes it hard to maintain.
   *
   * @param tableId
   * @param formId
   *          if this is null, it is assumed the caller is not specifying a
   *          specific form, and instead the default form parameters are tried
   *          to be constructed.
   * @param formVersion
   * @param formRootElement
   * @param jsonMap
   *          a json string of values to prepopulate the form with. a null
   *          value won't prepopulate any values.
   * @return true if the launch succeeded, else false
   */
  public boolean helperAddRowWithCollect(
      String tableId,
      String formId,
      String formVersion,
      String formRootElement,
      String jsonMap) {
    // The first thing we need to do is get the correct TableProperties.
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      Log.e(TAG, "table [" + tableId + "] cannot have a row added"
          + " because it could not be found");
      return false;
    }
    Map<String, String> map = null;
    if (jsonMap != null) {
      map = WebViewUtil.getMapFromJson(jsonMap);
      if (map == null) {
        Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
        return false;
      }
    }
    CollectFormParameters formParameters =
        CollectFormParameters.constructCollectFormParameters(tp);
    if (formId != null) {
      formParameters.setFormId(formId);
      if (formVersion != null) {
        formParameters.setFormVersion(formVersion);
      }
      if (formRootElement != null) {
        formParameters.setRootElement(formRootElement);
      }
    }
    this.prepopulateRowAndLaunchCollect(
        formParameters,
        tp,
        map);
    return true;
  }

  /**
   * It prepopulates the form as it needs based on the query (or the
   * elKeyToValueToPrepopulate parameter) and launches the form.
   *
   * @param params
   * @param tp
   * @param elKeyToValueToPrepopulate
   *          a map of element key to value that will prepopulate the Collect
   *          form for the new add row. Must be a map of column element key to
   *          value. If this parameter is null, it prepopulates based on the
   *          searchString, if there is one. If this value is not null, it
   *          ignores the queryString and uses only the map.
   */
  private void prepopulateRowAndLaunchCollect(
      CollectFormParameters params,
      TableProperties tp,
      Map<String, String> elKeyToValueToPrepopulate) {
    Intent addRowIntent;
    if (elKeyToValueToPrepopulate == null) {
      // The prepopulated values we need to get from the query string.
      addRowIntent = CollectUtil.getIntentForOdkCollectAddRowByQuery(
          this.mActivity,
          this.mAppName,
          tp,
          params);
    } else {
      // We've received a map to prepopulate with.
      addRowIntent = CollectUtil.getIntentForOdkCollectAddRow(
          this.mActivity,
          tp,
          params,
          elKeyToValueToPrepopulate);
    }
    // Now just launch the intent to add the row.
    CollectUtil.launchCollectToAddRow(
        this.mActivity,
        addRowIntent,
        tp);
  }

  /**
   * Launch survey to edit the row.
   *
   * @param tableId
   * @param rowId
   * @param formId
   * @param screenPath
   * @return true if the edit was launched successfully, else false
   */
  public boolean helperEditRowWithSurvey(
      String tableId,
      String rowId,
      String formId,
      String screenPath) {
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      Log.e(TAG, "table [" + tableId + "] cannot have a row edited with"
          + " survey because it cannot be found");
      return false;
    }
    SurveyFormParameters surveyFormParameters = null;
    if (formId == null) {
      surveyFormParameters =
          SurveyFormParameters.constructSurveyFormParameters(tp);
    } else {
      surveyFormParameters = new SurveyFormParameters(
          true,
          formId,
          screenPath);
    }
    SurveyUtil.editRowWithSurvey(
        this.mActivity,
        this.mAppName,
        rowId,
        tp,
        surveyFormParameters);
    return true;
  }

  /**
   * Edit the given row with Collect. Returns true if things went well, or
   * false if something went wrong.
   * <p>
   * formId is checked for null--if it is, it tries to use the default form.
   * If not null, it uses that form.
   *
   * @param tableId
   * @param rowId
   * @param formId
   * @param formVersion
   * @param formRootElement
   * @return
   */
  public boolean helperEditRowWithCollect(
      String tableId,
      String rowId,
      String formId,
      String formVersion,
      String formRootElement) {
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      Log.e(
          TAG,
          "[helperEditRowWithCollect] table [" +
              tableId +
              "] cannot have row edited, because it cannot be found");
      return false;
    }
    CollectFormParameters formParameters = null;
    if (formId == null) {
      // Then we want to construct the form parameters using default
      // values.
      formParameters = CollectFormParameters
          .constructCollectFormParameters(tp);
      formId = formParameters.getFormId();
      formVersion = formParameters.getFormVersion();
      formRootElement = formParameters.getRootElement();
    } else {
      formParameters = new CollectFormParameters(
          true,
          formId,
          formVersion,
          formRootElement,
          tp.getLocalizedDisplayName());
    }
    CollectUtil.editRowWithCollect(
        this.mActivity,
        this.mAppName,
        rowId,
        tp,
        formParameters);
    return true;
  }

  private TableData queryForTableData(
      String tableId,
      String sqlWhereClause,
      String[] sqlSelectionArgs,
      String[] sqlGroupBy,
      String sqlHaving,
      String sqlOrderByElementKey,
      String sqlOrderByDirection) {
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    if (tp == null) {
      Log.e(
          TAG,
          "request for table with tableId [" + tableId + "] cannot be found.");
      return null;
    }
    SQLiteDatabase db = null;
    try {
      db = tp.getReadableDatabase();
      UserTable userTable = ODKDatabaseUtils.rawSqlQuery(db, tp.getAppName(),
          tp.getTableId(), tp.getPersistedColumns(),
          sqlWhereClause, sqlSelectionArgs,
          sqlGroupBy, sqlHaving, 
          sqlOrderByElementKey, sqlOrderByDirection);
      TableData tableData = new TableData(userTable);
      return tableData;
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
  }

  /**
   * The keys for the platformInfo json object.
   * @author sudar.sam@gmail.com
   *
   */
  private static class PlatformInfoKeys {
    public static final String CONTAINER = "container";
    public static final String VERSION = "version";
    public static final String APP_NAME = "appName";
    public static final String BASE_URI = "baseUri";
    public static final String LOG_LEVEL = "logLevel";
  }

}
