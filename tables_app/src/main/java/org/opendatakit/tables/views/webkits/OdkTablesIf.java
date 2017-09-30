/*
 * Copyright (C) 2017 University of Washington
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

/**
 * TODO what does this class do?
 */

class OdkTablesIf {

  /**
   * Used for logging
   */
  public static final String TAG = OdkTablesIf.class.getSimpleName();

  private WeakReference<OdkTables> weakControl;

  /**
   * TODO needs documentation
   *
   * @param odkTables
   */
  OdkTablesIf(OdkTables odkTables) {
    weakControl = new WeakReference<>(odkTables);
  }

  private boolean isInactive() {
    return weakControl.get() == null || weakControl.get().isInactive();
  }

  /**
   * Set list view portion of a DetailWithList view, restricted by given query.
   *
   * @param tableId              the tableId of the table to open
   * @param whereClause          If null will not restrict the results.
   * @param sqlSelectionArgsJSON -- JSON.stringify of an Object[] array that can contain integer,
   *                             numeric, boolean and string types, one for each "?" in whereClause.
   *                             If null will not restrict the results.
   * @param relativePath         the name of the file specifying the list view, relative to the app
   *                             folder.
   * @return true if the open succeeded
   */
  @android.webkit.JavascriptInterface
  public boolean setSubListView(String tableId, String whereClause, String sqlSelectionArgsJSON,
      String relativePath) {
    if (isInactive())
      return false;
    weakControl.get()
        .helperSetSubListView(tableId, relativePath, whereClause, sqlSelectionArgsJSON, null, null,
            null, null);
    return true;
  }

  /**
   * Set list view portion of a DetailWithList view, restricted by given query.
   *
   * @param tableId              the tableId of the table to open
   * @param sqlCommand           the sql command to execute
   * @param sqlSelectionArgsJSON -- JSON.stringify of an Object[] array that can contain integer,
   *                             numeric, boolean and string types, one for each "?" in whereClause.
   *                             If null will not restrict the results.
   * @param relativePath         the name of the file specifying the list view, relative to the app
   *                             folder.
   * @return true if the open succeeded
   */
  @android.webkit.JavascriptInterface
  public boolean setSubListViewArbitraryQuery(String tableId, String sqlCommand,
      String sqlSelectionArgsJSON, String relativePath) {
    if (isInactive())
      return false;
    weakControl.get()
        .helperSetSubListView(tableId, relativePath, sqlCommand, sqlSelectionArgsJSON);
    return true;
  }
}
