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
package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.data.LocalKeyValueStoreConstants;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.Constants.IntentKeys;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.webkits.CustomTableView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This class is responsible for the list view of a table.
 *
 * SS: not sure who the original author is. Putting my tag on it because I'm
 * adding some things.
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 *
 */
public class ListDisplayActivity extends Activity implements DisplayActivity {

  private static final String TAG = "ListDisplayActivity";

  /**
   * The filename the list view should be opened with. If not present, default
   * behavior might be to open the default file.
   */
  public static final String INTENT_KEY_FILENAME = "filename";

  private String appName;
  private Controller c;
  private UserTable table;
  private CustomTableView view;
  private KeyValueStoreHelper kvsh;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    setTitle("");
    c = new Controller(this, this, getIntent().getExtras(), savedInstanceState);
    kvsh = c.getTableProperties().getKeyValueStoreHelper(
        LocalKeyValueStoreConstants.ListViews.PARTITION);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    c.onSaveInstanceState(outState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    init();
  }

  /**
   * Return the default list view file name that has been set for the table
   * represented by the given {@link TableProperties} object. May return null if
   * no default list view has been set.
   *
   * @param tableProperties
   * @return
   */
  public static String getDefaultListFileName(TableProperties tableProperties) {
    KeyValueStoreHelper kvsh = tableProperties.getKeyValueStoreHelper(
        LocalKeyValueStoreConstants.ListViews.PARTITION);
    String nameOfView = kvsh.getString(
        LocalKeyValueStoreConstants.ListViews.KEY_LIST_VIEW_NAME);
    if (nameOfView == null) {
      // Then none has been set.
      return null;
    }
    KeyValueStoreHelper namedListViewsPartitionKvsh = tableProperties
        .getKeyValueStoreHelper(
            LocalKeyValueStoreConstants.ListViews.PARTITION_VIEWS);
    AspectHelper viewAspectHelper = namedListViewsPartitionKvsh.getAspectHelper(nameOfView);
    String filename = viewAspectHelper.getString(
        LocalKeyValueStoreConstants.ListViews.KEY_FILENAME);
    return filename;
  }

  @Override
  public void init() {
    // I hate having to do these two refreshes here, but with the code the
    // way it is it seems the only way.
    c.refreshDbTable(c.getTableProperties().getTableId());

    Bundle intentExtras = getIntent().getExtras();
    String sqlWhereClause =
        intentExtras.getString(IntentKeys.SQL_WHERE);
    String[] sqlSelectionArgs = null;
    if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
       sqlSelectionArgs = intentExtras.getStringArray(
          IntentKeys.SQL_SELECTION_ARGS);
    }
    String[] sqlGroupBy = intentExtras.getStringArray(IntentKeys.SQL_GROUP_BY_ARGS);
    String sqlHaving = null;
    if ( sqlGroupBy != null && sqlGroupBy.length != 0 ) {
      sqlHaving = intentExtras.getString(IntentKeys.SQL_HAVING);
    }
    String sqlOrderByElementKey = intentExtras.getString(IntentKeys.SQL_ORDER_BY_ELEMENT_KEY);
    String sqlOrderByDirection = null;
    if ( sqlOrderByElementKey != null && sqlOrderByElementKey.length() != 0 ) {
      sqlOrderByDirection = intentExtras.getString(IntentKeys.SQL_ORDER_BY_DIRECTION);
      if ( sqlOrderByDirection == null || sqlOrderByDirection.length() == 0 ) {
        sqlOrderByDirection = "ASC";
      }
    }

    DbTable dbTable = DbTable.getDbTable(c.getTableProperties());
    table = dbTable.rawSqlQuery(c.getTableProperties().getPersistedColumns(),
        sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);

    String nameOfView = kvsh.getString(
        LocalKeyValueStoreConstants.ListViews.KEY_LIST_VIEW_NAME);
    // The nameOfView can be null in some cases, like if the default list
    // view has been deleted. If this ever occurs, we should just say no
    // filename specified and make them choose one.
    String filename = getIntent().getExtras().getString(INTENT_KEY_FILENAME);
    if (nameOfView != null) {
      if (filename == null) {
        filename = getDefaultListFileName(table.getTableProperties());
      }
    }
    view = CustomTableView.get(this, appName, table, filename);
    // change the info bar text IF necessary
    c.setListViewInfoBarText();
    displayView();
  }

  private void displayView() {
    view.display();
    c.setDisplayView(view);
    setContentView(c.getContainerView());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (c.handleActivityReturn(requestCode, resultCode, data)) {
      return;
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
//    c.buildOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    return c.handleMenuItemSelection(item);
  }
}
