package org.opendatakit.tables.views.webkits;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utils.NameUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.CustomHomeScreenActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.activities.TableManager;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.Constants.RequestCodes;
import org.opendatakit.tables.utils.CustomViewUtil;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SurveyUtil;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class Control {
  
  private static final String TAG = Control.class.getSimpleName();
  
  protected AbsBaseActivity mActivity;

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
  public Control(AbsBaseActivity activity) {
    this.mActivity = activity;
    Log.d(TAG, "calling Control Constructor");
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
          this.mActivity.getAppName(),
          tableId);
    return result;
  }
  
  /**
   * Retrieve the {@link TableProperties} for all the user-defined tables in
   * the database.
   * @return
   */
  TableProperties[] retrieveTablePropertiesForAllTables() {
    TableProperties[] result = TableProperties.getTablePropertiesForAll(
        this.mActivity,
        this.mActivity.getAppName());
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
      throw new IllegalStateException("CustomView must be have an " +
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
   * Returns all the metadata and user-defined data element keys to value in a
   * map.
   *
   * @param tableId
   * @param rowId
   * @return
   */
  private Map<String, String> getElementKeyToValues(
      String tableId,
      String rowId) {
    TableProperties tp = this.retrieveTablePropertiesForTable(tableId);
    String sqlQuery = DataTableColumns.ID + " = ? ";
    String[] selectionArgs = { rowId };
    DbTable dbTable = DbTable.getDbTable(tp);
    UserTable userTable = dbTable.rawSqlQuery(
        sqlQuery,
        selectionArgs,
        null,
        null,
        null,
        null);
    if (userTable.getNumberOfRows() > 1) {
      Log.e(TAG, "query returned > 1 rows for tableId: " +
          tableId +
          " and " +
          "rowId: " +
          rowId);
    } else if (userTable.getNumberOfRows() == 0) {
      Log.e(TAG, "query returned no rows for tableId: " +
          tableId +
          " and rowId: " +
          rowId);
    }
    Map<String, String> elementKeyToValue = new HashMap<String, String>();
    Row requestedRow = userTable.getRowAtIndex(0);
    List<String> userDefinedElementKeys = 
        userTable.getTableProperties().getColumnOrder();
    Set<String> metadataElementKeys = 
        userTable.getMapOfUserDataToIndex().keySet();
    List<String> allElementKeys = new ArrayList<String>();
    allElementKeys.addAll(userDefinedElementKeys);
    allElementKeys.addAll(metadataElementKeys);
    for (String elementKey : allElementKeys) {
      elementKeyToValue.put(
          elementKey,
          requestedRow.getDataOrMetadataByElementKey(elementKey));
    }
    return elementKeyToValue;
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
    TableProperties[] tpAll = this.retrieveTablePropertiesForAllTables();
    ArrayList<String> tableIdsList = new ArrayList<String>();
    for ( TableProperties tp : tpAll ) {
      tableIdsList.add(tp.getTableId());
    }
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
    ColumnProperties columnProperties = tp.getColumnByElementKey(elementKey);
    if (columnProperties == null) {
      Log.e(TAG, "column with elementKey does not exist: " + elementKey);
      return null;
    }
    String displayName = columnProperties.getLocalizedDisplayName();
    return displayName;
  }

  /**
   * @see {@link ControlIf#getTableDisplayName(String)}
   * @param tableId
   * @return
   */
  public String getTableDisplayName(String tableId) {
    TableProperties tp = TableProperties.getTablePropertiesForTable(
        this.mActivity,
        this.mActivity.getAppName(),
        tableId);
    return tp.getLocalizedDisplayName();
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
    platformInfo.put(PlatformInfoKeys.APP_NAME, this.mActivity.getAppName());
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
    ColumnProperties columnProperties = tp.getColumnByElementKey(elementKey);
    return columnProperties != null;
  }

  /**
   * Return the base uri for the Tables app name with a trailing separator.
   *
   * @return
   */
  private String getBaseContentUri() {
    Uri contentUri = FileProvider.getWebViewContentUri(this.mActivity);
    contentUri = Uri.withAppendedPath(
        contentUri,
        Uri.encode(this.mActivity.getAppName()));
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
    Log.d(TAG, "in launchHTML with filename: " + relativePath);
    String pathToTablesFolder = ODKFileUtils.getAppFolder(
        this.mActivity.getAppName());
    String pathToFile = pathToTablesFolder + File.separator + relativePath;
    Intent i = new Intent(mActivity, CustomHomeScreenActivity.class);
    i.putExtra(Constants.IntentKeys.APP_NAME, this.mActivity.getAppName());
    i.putExtra(CustomHomeScreenActivity.INTENT_KEY_FILENAME, pathToFile);
    mActivity.startActivity(i);
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
      map = CustomViewUtil.getMapFromJson(jsonMap);
      if (map == null) {
        Log.e(TAG, "couldn't parse values into map to give to Survey");
        return false;
      }
    }
    SurveyUtil.addRowWithSurvey(
        this.mActivity,
        this.mActivity.getAppName(),
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
      map = CustomViewUtil.getMapFromJson(jsonMap);
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
          this.mActivity.getAppName(),
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
        this.mActivity.getAppName(),
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
      Log.e(TAG, "table [" + tableId + "] cannot have row edited, "
          + "because it cannot be found");
      return false;
    }
    if (formId == null) {
      // Then we want to construct the form parameters using default
      // values.
      CollectFormParameters formParameters = CollectFormParameters
          .constructCollectFormParameters(tp);
      formId = formParameters.getFormId();
      formVersion = formParameters.getFormVersion();
      formRootElement = formParameters.getRootElement();
    }
    Map<String, String> elementKeyToValue = 
        getElementKeyToValues(tableId, rowId);
    Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
        this.mActivity,
        tp,
        elementKeyToValue,
        formId,
        formVersion,
        formRootElement,
        rowId);
    if (editRowIntent == null) {
      Log.e(
          TAG,
          "the edit row with collect intent was null, returning false");
      return false;
    }
    CollectUtil.launchCollectToEditRow(
        this.mActivity,
        editRowIntent,
        rowId);
    return true;
  }

  /**
   * Create an alert that will allow for a new table name. This might be to
   * rename an existing table, if isNewTable false, or it could be a new
   * table, if isNewTable is true.
   * <p>
   * This method is based on {@link TableManager.alertForNewTableName}. The
   * parameters are the same for the sake of consistency.
   * <p>
   * As this method does not access the javascript, the caller is responsible
   * for refreshing the displayed information.
   *
   * @param isNewTable
   * @param tp
   * @param givenTableName
   */
  public void alertForNewTableName(
      final boolean isNewTable,
      final TableProperties tp,
      String givenTableName) {
    Log.d(TAG, "alertForNewTableName called");
    Log.d(TAG, "isNewTable: " + Boolean.toString(isNewTable));
    Log.d(TAG, "tp: " + tp);
    Log.d(TAG, "givenTableName: " + givenTableName);
    AlertDialog newTableAlert;
    AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
    alert.setTitle(mActivity.getString(R.string.name_of_new_table));
    // An edit text for getting user input.
    final EditText input = new EditText(mActivity);
    alert.setView(input);
    if (givenTableName != null) {
      input.setText(givenTableName);
    }
    // OK Action: create a new table.
    alert.setPositiveButton(mActivity.getString(R.string.ok),
        new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            String newTableName = input.getText().toString().trim();
            if (newTableName == null || newTableName.equals("")) {
              Toast.makeText(
                  mActivity,
                  mActivity.getString(R.string.error_table_name_empty),
                  Toast.LENGTH_LONG).show();
            } else {
              if (isNewTable) {
                // TODO: prompt for this!
                String tableId = NameUtil.createUniqueTableId(
                    Control.this.mActivity,
                    Control.this.mActivity.getAppName(),
                    newTableName);
                addTable(newTableName, tableId);
              } else {
                SQLiteDatabase db = tp.getWritableDatabase();
                try {
                  db.beginTransaction();
                  tp.setDisplayName(db, newTableName);
                  db.setTransactionSuccessful();
                } catch ( Exception e ) {
                  e.printStackTrace();
                  Log.e(TAG, "Unable to change display name: " + e.toString());
                  Toast.makeText(
                      Control.this.mActivity,
                      "Unable to change display name",
                      Toast.LENGTH_LONG).show();
                } finally {
                  db.endTransaction();
                  db.close();
                }
              }
            }
          }
        });

    alert.setNegativeButton(
        R.string.cancel,
        new DialogInterface.OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        // Cancel it, do nothing.
      }
    });
    newTableAlert = alert.create();
    newTableAlert.getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    newTableAlert.show();
  }

  private void addTable(String tableName, String tableId) {
    // TODO: if this avenue to create a table remains, we need to also
    // prompt them for a tableId name.
    String dbTableName = NameUtil.createUniqueDbTableName(
        this.mActivity,
        this.mActivity.getAppName(),
        tableName);
    @SuppressWarnings("unused")
    TableProperties tp = TableProperties.addTable(
        this.mActivity,
        this.mActivity.getAppName(),
        dbTableName,
        tableName,
        tableId);
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
    DbTable dbTable = DbTable.getDbTable(tp);
    UserTable userTable = dbTable.rawSqlQuery(
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection);
    TableData tableData = new TableData(this.mActivity, userTable);
    return tableData;
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
