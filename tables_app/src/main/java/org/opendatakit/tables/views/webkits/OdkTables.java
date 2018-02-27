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

import android.content.Context;
import android.os.Bundle;
import org.opendatakit.database.queries.BindArgs;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.data.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.views.ODKWebView;

import java.lang.ref.WeakReference;

/**
 * TODO what does this class do?
 */
class OdkTables {

  /**
   * Used for logging
   */
  @SuppressWarnings("unused")
  private static final String TAG = OdkTables.class.getSimpleName();
  private Context mActivity;
  private WeakReference<ODKWebView> mWebView;

  /**
   * Constructs
   *
   * @param context the activity that will be holding the view
   * @param webView the webview to hold
   */
  OdkTables(Context context, ODKWebView webView) {
    this.mActivity = context;
    this.mWebView = new WeakReference<>(webView);
  }

  boolean isInactive() {
    return mWebView.get() == null || mWebView.get().isInactive();
  }

  OdkTablesIf getJavascriptInterfaceWithWeakReference() {
    return new OdkTablesIf(this);
  }

  /**
   * Set the list view contents for a detail with list view
   *
   * @param tableId              the table id
   * @param relativePath         the path relative to the app folder
   * @param sqlWhereClause       an sql selection parameter to limit what rows get shown
   * @param sqlSelectionArgsJSON -- JSON.stringify of an Object[] array that can contain integer,
   *                             numeric, boolean and string types.
   * @param sqlGroupBy           an array of column IDs (element keys) to group by
   * @param sqlHaving            a sql having argument
   * @param sqlOrderByElementKey the id of the column the result set will be sorted by
   * @param sqlOrderByDirection  ASC for ascending DESC for descending
   */
  void helperSetSubListView(String tableId, String relativePath, String sqlWhereClause,
      String sqlSelectionArgsJSON, String[] sqlGroupBy, String sqlHaving,
      String sqlOrderByElementKey, String sqlOrderByDirection) {
    helperUpdateView(tableId, sqlWhereClause, sqlSelectionArgsJSON, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection, ViewFragmentType.SUB_LIST, relativePath);
  }

  /**
   * Send a bundle to update a view without opening a new activity.
   *
   * @param tableId              the table id
   * @param sqlWhereClause       an sql selection parameter to limit what rows get shown
   * @param sqlSelectionArgsJSON -- JSON.stringify of an Object[] array that can contain integer,
   *                             numeric, boolean and string types.
   * @param sqlGroupBy           an array of column IDs (element keys) to group by
   * @param sqlHaving            a sql having argument
   * @param sqlOrderByElementKey the id of the column the result set will be sorted by
   * @param sqlOrderByDirection  ASC for ascending DESC for descending
   * @param viewType             Must be ViewFragmentType.SUB_LIST right now
   * @param relativePath         the path relative to the app folder
   * @throws IllegalArgumentException if viewType is not a sub view
   */
  private void helperUpdateView(String tableId, String sqlWhereClause, String sqlSelectionArgsJSON,
      String[] sqlGroupBy, String sqlHaving, String sqlOrderByElementKey,
      String sqlOrderByDirection, ViewFragmentType viewType, String relativePath) {
    if (viewType != ViewFragmentType.SUB_LIST) {
      throw new IllegalArgumentException("Cannot use this method to update a view that doesn't "
          + "support updates. Currently only DetailWithListView's Sub List supports this action");
    }
    BindArgs bindArgs = new BindArgs(sqlSelectionArgsJSON);
    final Bundle bundle = new Bundle();

    IntentUtil.addSQLKeysToBundle(bundle, sqlWhereClause, bindArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection);
    IntentUtil.addTableIdToBundle(bundle, tableId);
    IntentUtil.addFragmentViewTypeToBundle(bundle, viewType);
    IntentUtil.addFileNameToBundle(bundle, relativePath);

    if (mActivity instanceof TableDisplayActivity) {
      final TableDisplayActivity activity = (TableDisplayActivity) mActivity;
      // Run on ui thread to try and prevent a race condition with the two webkits
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.updateFragment(Constants.FragmentTags.DETAIL_WITH_LIST_LIST, bundle);
        }
      });
    } else {
      throw new IllegalArgumentException(
          "Cannot update an activity without an updateFragment " + "method");
    }
  }

  /**
   * Set the list view contents for a detail with list view with an arbitrary query
   *
   * @param tableId              the table id
   * @param relativePath         the path relative to the app folder
   * @param sqlCommand           arbitrary sql command to execute
   * @param sqlSelectionArgsJSON -- JSON.stringify of an Object[] array that can contain integer,
   *                             numeric, boolean and string types.
   */
  void helperSetSubListView(String tableId, String relativePath, String sqlCommand,
      String sqlSelectionArgsJSON) {
    helperUpdateView(tableId, sqlCommand, sqlSelectionArgsJSON,
        ViewFragmentType.SUB_LIST, relativePath);
  }


  /**
   * Send a bundle to update a view without opening a new activity, using an abtirary query.
   *
   * @param tableId              the table id
   * @param sqlCommand           the arbitrary sql query to run
   * @param sqlSelectionArgsJSON -- JSON.stringify of an Object[] array that can contain integer,
   * @param viewType             Must be ViewFragmentType.SUB_LIST right now
   * @param relativePath         the path relative to the app folder
   * @throws IllegalArgumentException if viewType is not a sub view
   */
  private void helperUpdateView(String tableId, String sqlCommand, String sqlSelectionArgsJSON,
      ViewFragmentType viewType, String relativePath) {
    if (viewType != ViewFragmentType.SUB_LIST) {
      throw new IllegalArgumentException("Cannot use this method to update a view that doesn't "
          + "support updates. Currently only DetailWithListView's Sub List supports this action");
    }
    BindArgs bindArgs = new BindArgs(sqlSelectionArgsJSON);
    final Bundle bundle = new Bundle();

    IntentUtil.addArbitraryQueryToBundle(bundle, sqlCommand, bindArgs);
    IntentUtil.addTableIdToBundle(bundle, tableId);
    IntentUtil.addFragmentViewTypeToBundle(bundle, viewType);
    IntentUtil.addFileNameToBundle(bundle, relativePath);

    if (mActivity instanceof TableDisplayActivity) {
      final TableDisplayActivity activity = (TableDisplayActivity) mActivity;
      // Run on ui thread to try and prevent a race condition with the two webkits
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.updateFragment(Constants.FragmentTags.DETAIL_WITH_LIST_LIST, bundle);
        }
      });
    } else {
      throw new IllegalArgumentException(
          "Cannot update an activity without an updateFragment " + "method");
    }
  }

}
