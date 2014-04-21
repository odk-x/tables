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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.tables.fragments.TableMapInnerFragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.webkit.WebViewClient;

public class CustomTableView extends CustomView
    implements ExtendedTableControl {

  private static final String DEFAULT_HTML = "<html><body>"
      + "<p>No filename has been specified.</p>" + "</body></html>";

  private UserTable table;
  // IMPORTANT: hold a strong reference to control because Webkit holds a weak
  // reference
  private Control control;
  // IMPORTANT: hold a strong reference to tableData because Webkit holds a
  // weak reference
  private TableData tableData;
  private String filename;
  private Fragment mFragment;

  private CustomTableView(Activity activity, String appName, String filename) {
    super(activity, appName);
    this.filename = filename;
  }

  public static CustomTableView get(Activity activity, String appName,
      UserTable table, String filename) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename);
    ctv.set(table);
    return ctv;
  }

  private void set(UserTable table) {
    this.table = table;
  }

  // //////////////////////////// TEST ///////////////////////////////

  public static CustomTableView get(Activity activity, String appName,
      UserTable table, String filename, int index) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename);

    ArrayList<Integer> indexes = new ArrayList<Integer>();
    indexes.add(index);
    UserTable singleRowTable = new UserTable(table, indexes);

    ctv.set(singleRowTable);
    return ctv;
  }

  /**
   * Return the {@link TableData} object backing this view.
   * @return
   */
  public TableData getTableDataObject() {
    return this.tableData;
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
      UserTable table, String filename, List<Integer> indexes) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename);

    UserTable multiRowTable = new UserTable(table, indexes);

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
      Fragment fragment) {
    CustomTableView ctv = new CustomTableView(activity, appName, filename);

    UserTable multiRowTable = new UserTable(table, indexes);

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
    control = new Control(mParentActivity);
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
    ((TableMapInnerFragment) mFragment).focusOnMarker(
        table.getRowAtIndex(index).getRowId());
    return true;
  }

}
