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

import java.lang.ref.WeakReference;

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;

import android.os.RemoteException;
import android.widget.Toast;

/**
 * This class is handed to the javascript as "data" when displaying a table in a
 * List View. It is a way to get at the data in a table, allowing for the
 * display of a table to be customized.
 * <p>
 * Standard practice is to choose several columns which are enough to provide a
 * summary of each row, and then iterate over all the rows of the table using
 * {@link #getCount()} and {@link #getData(int, String)}, rendering each row as
 * an item. A click handler can then be added to call
 * {@link ControlIf#openDetailView(String, String, String)} to launch a Detail
 * View for a clicked row.
 * <p>
 * This class then serves as a summary and an access point to the Detail View.
 * 
 * @author Mitch Sundt
 * @author sudar.sam@gmail.com
 *
 */
public class TableDataIf {
  private static final String TAG = "TableDataIf";

  private WeakReference<TableData> weakTable;

  TableDataIf(TableData table) {
    this.weakTable = new WeakReference<TableData>(table);
  }

  /**
   * Returns the number of rows in the table being viewed as restricted by the
   * current query, be it SQL or a query string.
   * 
   * @return the number of rows in the table
   */
  // @JavascriptInterface
  public int getCount() {
    return weakTable.get().getCount();
  }

  /**
   * Returns a stringified JSONArray of all the values in the given column, or
   * null if the column cannot be found.
   * 
   * @param elementPath
   *          the element path of the column
   * @return JSONArray of all the data in the column
   */
  // @JavascriptInterface
  public String getColumnData(String elementPath) {
    return weakTable.get().getColumnData(elementPath);
  }

  /**
   * Returns the tableId of the table being displayed.
   * 
   * @return the tabeId
   */
  // @JavascriptInterface
  public String getTableId() {
    return weakTable.get().getTableId();
  }

  /**
   * Get the id for the row at the given index.
   * 
   * @param rowNumber
   *          the row number
   * @return the rowId of the row number
   */
  // @JavascriptInterface
  public String getRowId(int rowNumber) {
    return weakTable.get().getRowId(rowNumber);
  }

  /**
   * Return a stringified JSON object mapping elementKey its column type.
   * 
   * @return Stringified JSON map of element key to column type
   */
  // @JavascriptInterface
  public String getColumns() {
    return weakTable.get().getColumns();
  }

  /**
   * Get the text color of the given column for the given value. Uses the color
   * rules of the column. The default value is -16777216.
   * 
   * @param rowNumber
   *          the row to evaluate
   * @param elementPath
   *          the element path of the column
   * @return String representation of the text color
   */
  // @JavascriptInterface
  public String getColumnForegroundColor(int rowNumber, String elementPath) {
    try {
      return weakTable.get().getColumnForegroundColor(rowNumber, elementPath);
    } catch (RemoteException e) {
      String appName = weakTable.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakTable.get().getContext(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  /**
   * Get the text color of the status column. Uses the color rules of the status
   * column. The default value is -16777216.
   * 
   * @param rowNumber
   *          the row to evaluate
   * @return String representation of the text color
   */
  // @JavascriptInterface
  public String getStatusForegroundColor(int rowNumber) {
    try {
      return weakTable.get().getStatusForegroundColor(rowNumber);
    } catch (RemoteException e) {
      String appName = weakTable.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakTable.get().getContext(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  /**
   * Get the text color of the row. Uses the color rules of the table. The
   * default value is -16777216.
   * 
   * @param rowNumber
   *          the row to evaluate
   * @return String representation of the text color
   */
  // @JavascriptInterface
  public String getRowForegroundColor(int rowNumber) {
    try {
      return weakTable.get().getRowForegroundColor(rowNumber);
    } catch (RemoteException e) {
      String appName = weakTable.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakTable.get().getContext(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  /**
   * Returns true if the table has been grouped by a column.
   * 
   * @return true if index else false
   */
  // @JavascriptInterface
  public boolean isGroupedBy() {
    return weakTable.get().isGroupedBy();
  }

  /**
   * Retrieve the datum at the given row in the given column name. The rows are
   * zero-indexed, meaning the first row is 0. For example, if you were
   * displaying a list view and had a column titled "Age", you would retrieve
   * the "Age" value for the second row by calling getData(1, "Age").
   * <p>
   * The null value is returned if the column could not be found, or if the
   * value in the database is null.
   * <p>
   * If {@link #isGroupedBy()} returns true, a valid elementPath is __count,
   * which will return the number of rows with the grouped by value.
   * 
   * @param rowNumber
   *          the row number
   * @param elementPath
   *          the element path of the column
   * @return the String representation of the datum at the given row in the
   *         given column, or null if the value in the database is null or the
   *         column does not exist
   */
  // @JavascriptInterface
  public String getData(int rowNumber, String elementPath) {
    return weakTable.get().getData(rowNumber, elementPath);
  }

  /**
   * Retrieve the datum in the given column from the first row. This is a
   * convenience method when operating in a detail view and is equivalent to
   * calling {@link #getData(int, String)} with a rowNum of 0.
   * 
   * @see #getData(int, String)
   * @param elementPath
   * @return the String representation of the datum in the given column at the
   *         first row of the table
   */
  public String get(String elementPath) {
    return this.getData(0, elementPath);
  }

}