/*
 * Copyright (C) 2012 University of Washington
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.fragments.TableMapInnerFragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.webkit.WebViewClient;

public class CustomTableView extends CustomView 
    implements ExtendedTableControl {

  private static final String DEFAULT_HTML = "<html><body>"
      + "<p>No filename has been specified.</p>" + "</body></html>";

  private Map<String, Integer> colIndexTable;
  private UserTable table;
  // IMPORTANT: hold a strong reference to control because Webkit holds a weak
  // reference
  private Control control;
  // IMPORTANT: hold a strong reference to tableData because Webkit holds a
  // weak reference
  private TableData tableData;
  private String filename;
  private Fragment mFragment;

  private CustomTableView(Activity activity, String appName, String filename,
                          CustomViewCallbacks callbacks) {
    super(activity, appName, callbacks);
    this.filename = filename;
    colIndexTable = new HashMap<String, Integer>();
  }

  public static CustomTableView get(Activity activity, String appName, 
      UserTable table, String filename, CustomViewCallbacks callbacks) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename, 
        callbacks);
    ctv.set(table);
    return ctv;
  }

  private void set(UserTable table) {
    this.table = table;
    colIndexTable.clear();
    Map<String, ColumnProperties> elementKeyToColumnProperties = 
        table.getTableProperties()
        .getDatabaseColumns();
    colIndexTable.putAll(table.getMapOfUserDataToIndex());
    for (ColumnProperties cp : elementKeyToColumnProperties.values()) {
      String smsLabel = cp.getSmsLabel();
      if (smsLabel != null) {
        // TODO: this doesn't look to ever be used, and ignores the
        // possibility
        // of conflicting element keys and sms labels.
        colIndexTable.put(smsLabel, colIndexTable.get(cp.getElementKey()));
      }
    }
  }

  // //////////////////////////// TEST ///////////////////////////////

  public static CustomTableView get(Activity activity, String appName, 
      UserTable table, String filename, int index, Controller controller) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename, 
        controller);
    // Create a new table with only the row specified at index.
    // Create all of the arrays necessary to create a UserTable.
    String[] rowIds = new String[1];
    String[] headers = new String[table.getWidth()];
    String[][] data = new String[1][table.getWidth()];
    String[][] metadata = new String[1][table.getNumberOfMetadataColumns()];
    String[] footers = new String[table.getWidth()];
    // Set all the data for the table.
    rowIds[0] = table.getRowId(index);
    for (int i = 0; i < table.getWidth(); i++) {
      headers[i] = table.getHeader(i);
      data[0][i] = table.getData(index, i);
      footers[i] = table.getFooter(i);
      metadata[0] = table.getAllMetadataForRow(i);
    }
    UserTable singleRowTable = new UserTable(table.getTableProperties(), 
        rowIds, headers, data, table.getElementKeysForIndex(),
        table.getMapOfUserDataToIndex(), metadata,
        table.getMapOfMetadataToIndex(), footers);
    // UserTable singleRowTable = new UserTable(rowIds, headers, data,
    // footers);

    ctv.set(singleRowTable);
    return ctv;
  }

  /**
   * Returns a custom view based on the list of indexes. The rows will be
   * ordered by the order of the list of indexes.
   *
   * @param context
   *          The context that wants to display this custom view.
   * @param tp
   *          The table properties of the table being displayed.
   * @param table
   *          The full table that we want to display a portion of.
   * @param filename
   *          The filename of the view we want to create.
   * @param indexes
   *          The indexes, of what rows, and in what order, we want to show
   *          them.
   * @return The custom view that represents the indexes in the table.
   */
  public static CustomTableView get(Activity activity, String appName, 
      UserTable table, String filename, List<Integer> indexes, 
      Controller controller) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename, 
        controller);
    // Create all of the arrays necessary to create a UserTable.
    String[] rowIds = new String[indexes.size()];
    String[] headers = new String[table.getWidth()];
    String[][] data = new String[indexes.size()][table.getWidth()];
    String[][] metadata = 
        new String[indexes.size()][table.getNumberOfMetadataColumns()];
    String[] footers = new String[table.getWidth()];
    // Set all the data for the table.
    for (int i = 0; i < table.getWidth(); i++) {
      headers[i] = table.getHeader(i);
      for (int j = 0; j < indexes.size(); j++) {
        rowIds[j] = table.getRowId(indexes.get(j));
        data[j][i] = table.getData(indexes.get(j), i);
        metadata[j] = table.getAllMetadataForRow(indexes.get(j));
      }
      footers[i] = table.getFooter(i);
    }
    UserTable multiRowTable = new UserTable(table.getTableProperties(), rowIds,
        headers, data, table.getElementKeysForIndex(),
        table.getMapOfUserDataToIndex(), metadata,
        table.getMapOfMetadataToIndex(), footers);
    ctv.set(multiRowTable);
    return ctv;
  }

  /**
   * Returns a custom view based on the list of indexes. The rows will be
   * ordered by the order of the list of indexes.
   *
   * @param context
   *          The context that wants to display this custom view.
   * @param tp
   *          The table properties of the table being displayed.
   * @param table
   *          The full table that we want to display a portion of.
   * @param filename
   *          The filename of the view we want to create.
   * @param indexes
   *          The indexes, of what rows, and in what order, we want to show
   *          them.
   * @return The custom view that represents the indexes in the table.
   */
  public static CustomTableView get(Activity activity, String appName, 
      UserTable table, String filename, List<Integer> indexes, 
      Fragment fragment, CustomViewCallbacks callbacks) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename, 
        callbacks);
    // Create all of the arrays necessary to create a UserTable.
    String[] rowIds = new String[indexes.size()];
    String[] headers = new String[table.getWidth()];
    String[][] data = new String[indexes.size()][table.getWidth()];
    String[][] metadata = 
        new String[indexes.size()][table.getNumberOfMetadataColumns()];
    String[] footers = new String[table.getWidth()];
    // Set all the data for the table.
    for (int i = 0; i < table.getWidth(); i++) {
      headers[i] = table.getHeader(i);
      for (int j = 0; j < indexes.size(); j++) {
        rowIds[j] = table.getRowId(indexes.get(j));
        data[j][i] = table.getData(indexes.get(j), i);
        metadata[j] = table.getAllMetadataForRow(indexes.get(j));
      }
      footers[i] = table.getFooter(i);
    }
    UserTable multiRowTable = 
        new UserTable(table.getTableProperties(), rowIds, headers, data,
            table.getElementKeysForIndex(),
            table.getMapOfUserDataToIndex(), metadata,
            table.getMapOfMetadataToIndex(), footers);

    ctv.set(multiRowTable);
    ctv.mFragment = fragment;
    return ctv;
  }

  public void setOnFinishedLoaded(WebViewClient client) {
    webView.setWebViewClient(client);
  }

  // ////////////////////////// END TEST /////////////////////////////

  public void display() {
    // Load a basic screen as you're getting the other stuff ready to
    // clear the old data.
    control = new Control(mParentActivity, table);
    tableData = new TableData(table);
    addJavascriptInterface(control.getJavascriptInterfaceWithWeakReference(), 
        "control");
    addJavascriptInterface(tableData.getJavascriptInterfaceWithWeakReference(),
        "data");
    if (filename != null) {
      String fullPath = FileProvider.getAsWebViewUri(getContext(), mAppName,
          filename);
      load(fullPath);
    } else {
      loadData(DEFAULT_HTML, "text/html", null);
    }
    initView();
  }

  @Override
  public boolean selectItem(int index) {
    ((TableMapInnerFragment) mFragment).focusOnMarker(table.getRowId(index));
    return true;
  }

}
