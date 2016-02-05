/*
 * Copyright (C) 2013 University of Washington
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

import android.os.RemoteException;
import android.widget.Toast;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;

import java.lang.ref.WeakReference;

/**
 * This object is handed to all the javascript views as "control".
 * 
 * @author mitchellsundt@gmail.com
 * @author sudar.sam@gmail.com
 *
 */
public class OdkTablesIf {

  public static final String TAG = "OdkTablesIf";
  
  private WeakReference<OdkTables> weakControl;

  OdkTablesIf(OdkTables odkTables) {
    weakControl = new WeakReference<OdkTables>(odkTables);
  }

  /**
   * Open the table with the given id.
   *
   * This synchronously launches an intent to open the table in its default view.
   *
   * @param tableId
   *          the table id of the table to open
   * @param whereClause
   *          If null will not restrict the results.
   * @param selectionArgs
   *          an array of selection arguments, one for each "?" in whereClause.
   *          If null will not restrict the results.
   * @return true if the open succeeded
   */
  @android.webkit.JavascriptInterface
  public boolean openTable(String tableId, String whereClause, String[] selectionArgs) {
    // TODO: convert to element keys
    return weakControl.get().helperOpenTable(tableId, whereClause, selectionArgs, null, null, null,
        null);
  }

  /**
   * Open the given table with the given list view, restricted by given query.
   *
   * This synchronously launches an intent to open the table in its list view.
   *
   * @see #openTable(String, String, String[])
   * @param tableId
   *          the tableId of the table to open
   * @param whereClause
   *          If null will not restrict the results.
   * @param selectionArgs
   *          an array of selection arguments, one for each "?" in whereClause.
   *          If null will not restrict the results.
   * @param relativePath
   *          the name of the file specifying the list view, relative to the app
   *          folder.
   * @return true if the open succeeded
   */
  @android.webkit.JavascriptInterface
  public boolean openTableToListView(String tableId, String whereClause, String[] selectionArgs,
      String relativePath) {
    return weakControl.get().helperOpenTableWithFile(tableId, relativePath, whereClause,
        selectionArgs, null, null, null, null);
  }

  /**
   * Open the given table to the map view, restricted with the given SQL query.
   *
   * This synchronously launches an intent to open the table in its map view.
   *
   * @see #openTable(String, String, String[])
   * @param tableId
   *          the tableId of the table to open
   * @param whereClause
   *          If null will not restrict the results.
   * @param selectionArgs
   *          an array of selection arguments, one for each "?" in whereClause.
   *          If null will not restrict the results.
   * @param relativePath
   *          NOT YET SUPPORTED
   * @return true if the open succeeded
   */
  @android.webkit.JavascriptInterface
  public boolean openTableToMapView(String tableId, String whereClause, String[] selectionArgs,
      String relativePath) {
    return weakControl.get().helperOpenTableToMapView(tableId, relativePath, whereClause,
        selectionArgs, null, null, null, null);
  }

  /**
   * Open the table to spreadsheet view, restricting by the given SQL query.
   *
   * This synchronously launches an intent to open the table in its java spreadsheet view.
   *
   * @see #openTable(String, String, String[])
   * @param tableId
   *          the tableId of the table to open
   * @param whereClause
   *           If null will not restrict the results.
   * @param selectionArgs
   *          an array of selection arguments, one for each "?" in whereClause.
   *          If null will not restrict the results.
   * @return true if the open succeeded
   */
  @android.webkit.JavascriptInterface
  public boolean openTableToSpreadsheetView(String tableId, String whereClause,
      String[] selectionArgs) {
    return weakControl.get().helperOpenTableToSpreadsheetView(tableId, whereClause, selectionArgs,
        null, null, null, null);
  }

  /**
   * Get the table ids of all the tables in the database.
   * 
   * @return a stringified json array of the table ids
   */
  @android.webkit.JavascriptInterface
  public String getAllTableIds() {
    try {
      return weakControl.get().getAllTableIds();
    } catch (RemoteException e) {
      String appName = weakControl.get().retrieveAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakControl.get().mActivity, R.string.error_accessing_database, Toast.LENGTH_LONG).show();
      return "[]";
    }
  }

  /**
   * Launch an arbitrary HTML file specified by filename.
   *
   * This synchronously launches an intent to open the URL.
   *
   * @param relativePath
   *          file name relative to the ODK Tables folder.
   * @return true if the file was launched, false if something went wrong
   */
  @android.webkit.JavascriptInterface
  public boolean launchHTML(String relativePath) {
    return weakControl.get().launchHTML(relativePath);
  }

  /**
   * Open the item specified by the index to the detail view.
   *
   * This synchronously launches an intent to open the rowId in its detail view
   * <p>
   * The relativePath parameter is optional, and if null an attempt will be made
   * to use the default file.
   * 
   * @param tableId
   * @param rowId
   * @param relativePath
   *          the name of the file specifying the detail view, relative to the
   *          app folder. If not present, the default detail view file will be
   *          used
   * @return true if the open succeeded
   */
  @android.webkit.JavascriptInterface
  public boolean openDetailView(String tableId, String rowId, String relativePath) {
    return weakControl.get().openDetailViewWithFile(tableId, rowId, relativePath);
  }

  /**
   * Add a row using Collect and the default form.
   *
   * This synchronously launches an intent to open ODK Collect to insert a new row in the table.
   *
   * @param tableId
   *          the tableId of the table to receive the add.
   * @return true if the activity was launched, false if something went wrong
   * @deprecated
   */
  @android.webkit.JavascriptInterface
  public boolean addRowWithCollectDefault(String tableId) {
    return this.addRowWithCollect(tableId, null, null, null, null);
  }

  /**
   * Add a row using Collect, a specific form, and a map of prepopulated values.
   *
   * This synchronously launches an intent to open ODK Collect to insert a new row in the table.
   * <p>
   * The form must have been added to Collect and visible in the "Fill Blank
   * Forms" screen.
   * 
   * @param tableId
   * @param formId
   *          if null, will launch the default form
   * @param formVersion
   * @param formRootElement
   * @param jsonMap
   *          a JSON map of element key to value, as retrieved by
   *          {@link #getElementKey(String, String)}. The map can then be
   *          converted to a String using JSON.stringify() and passed to this
   *          method. A null value will not prepopulate any values.
   * @return true if the activity was launched, false if something went wrong
   * @deprecated
   */
  @android.webkit.JavascriptInterface
  public boolean addRowWithCollect(String tableId, String formId, String formVersion,
      String formRootElement, String jsonMap) {
    try {
      return weakControl.get().helperAddRowWithCollect(tableId, formId, formVersion, formRootElement,
          jsonMap);
    } catch (RemoteException e) {
      String appName = weakControl.get().retrieveAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakControl.get().mActivity, R.string.error_accessing_database, Toast.LENGTH_LONG).show();
      return false;
    }
  }

  /**
   * Edit the given row using Collect.
   *
   * This synchronously launches an intent to open ODK Collect to edit a row in the table.
   *
   * @param tableId
   * @param rowId
   * @return true if the activity was launched, false if something went wrong
   * @deprecated
   */
  @android.webkit.JavascriptInterface
  public boolean editRowWithCollectDefault(String tableId, String rowId) {
    return this.editRowWithCollect(tableId, rowId, null, null, null);
  }

  /**
   * Edit the given row using Collect and a specific form.
   *
   * This synchronously launches an intent to open ODK Collect to edit a row in the table.
   *
   * @param tableId
   * @param rowId
   * @param formId
   *          if null, uses the default form
   * @param formVersion
   * @param formRootElement
   * @return true if the activity was launched, false if something went wrong
   * @deprecated
   */
  @android.webkit.JavascriptInterface
  public boolean editRowWithCollect(String tableId, String rowId, String formId,
      String formVersion, String formRootElement) {
    try {
      return weakControl.get().helperEditRowWithCollect(tableId, rowId, formId, formVersion,
          formRootElement);
    } catch (RemoteException e) {
      String appName = weakControl.get().retrieveAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakControl.get().mActivity, R.string.error_accessing_database, Toast.LENGTH_LONG).show();
      return false;
    }
  }

  /**
   * Edit the given row using Survey and the default form.
   *
   * This synchronously launches an intent to open ODK Survey to edit a row in the table.
   *
   * @param tableId
   * @param rowId
   * @return true if the activity was launched, false if something went wrong
   */
  @android.webkit.JavascriptInterface
  public boolean editRowWithSurveyDefault(String tableId, String rowId) {
    return editRowWithSurvey(tableId, rowId, null, null);
  }

  /**
   * Edit the given row using Survey and a specific form.
   *
   * This synchronously launches an intent to open ODK Survey to edit a row in the table.
   *
   * @param tableId
   * @param rowId
   * @param formId
   * @param screenPath
   * @return true if the activity was launched, false if something went wrong
   */
  @android.webkit.JavascriptInterface
  public boolean editRowWithSurvey(String tableId, String rowId, String formId, String screenPath) {
    try {
      return weakControl.get().helperEditRowWithSurvey(tableId, rowId, formId, screenPath);
    } catch (RemoteException e) {
      String appName = weakControl.get().retrieveAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakControl.get().mActivity, R.string.error_accessing_database, Toast.LENGTH_LONG).show();
      return false;
    }
  }

  /**
   * Add a row with Survey and the default form.
   *
   * This synchronously launches an intent to open ODK Survey to add a row in the table.
   *
   * @param tableId
   *          the table to receive the add
   * @return true if Survey was launched, else false
   */
  @android.webkit.JavascriptInterface
  public boolean addRowWithSurveyDefault(String tableId) {
    return this.addRowWithSurvey(tableId, null, null, null);
  }

  /**
   * Add a row using Survey.
   *
   * This synchronously launches an intent to open ODK Survey to add a row in the table.
   *
   * @param tableId
   * @param formId
   *          if null, the default form will be used
   * @param screenPath
   * @param jsonMap
   *          a stringified json object matching element key to the value to
   *          prepopulate in the new row
   * @return true if the activity was launched, false if something went wrong
   */
  @android.webkit.JavascriptInterface
  public boolean addRowWithSurvey(String tableId, String formId, String screenPath, String jsonMap) {
    try {
      return weakControl.get().helperAddRowWithSurvey(tableId, formId, screenPath, jsonMap);
    } catch (RemoteException e) {
      String appName = weakControl.get().retrieveAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakControl.get().mActivity, R.string.error_accessing_database, Toast.LENGTH_LONG).show();
      return false;
    }
  }

  /**
   * Return the element key for the column with the given element path.
   * 
   * @param tableId
   * @param elementPath
   * @return the element key for the column, or null if a table cannot be found
   *         with the existing tableId.
   */
  @android.webkit.JavascriptInterface
  public String getElementKey(String tableId, String elementPath) {
    return weakControl.get().getElementKey(tableId, elementPath);
  }

  /**
   * Get the display name for the given column.
   * 
   * @param tableId
   * @param elementPath
   * @return the display name for the given column
   */
  @android.webkit.JavascriptInterface
  public String getColumnDisplayName(String tableId, String elementPath) {
    try {
      return weakControl.get().getColumnDisplayName(tableId, elementPath);
    } catch (RemoteException e) {
      String appName = weakControl.get().retrieveAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakControl.get().mActivity, R.string.error_accessing_database, Toast.LENGTH_LONG).show();
      return elementPath;
    }
  }

  /**
   * Retrieve the display name for the given table.
   * <p>
   * If the display name has been localized, it returns the json representation
   * of the display name.
   * 
   * @param tableId
   * @return the display name for the table, in stringified json form if the
   *         name has been internationalized
   */
  @android.webkit.JavascriptInterface
  public String getTableDisplayName(String tableId) {
    try {
      return weakControl.get().getTableDisplayName(tableId);
    } catch (RemoteException e) {
      String appName = weakControl.get().retrieveAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakControl.get().mActivity, R.string.error_accessing_database, Toast.LENGTH_LONG).show();
      return tableId;
    }
  }
}