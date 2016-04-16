/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.fragments.DetailViewFragment;
import org.opendatakit.tables.fragments.GraphManagerFragment;
import org.opendatakit.tables.fragments.GraphViewFragment;
import org.opendatakit.tables.fragments.ListViewFragment;
import org.opendatakit.tables.fragments.MapListViewFragment;
import org.opendatakit.tables.fragments.SpreadsheetFragment;
import org.opendatakit.tables.fragments.TableMapInnerFragment;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.testutils.TestConstants;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivityStub extends TableDisplayActivity {
  
  // If modified during tests, the APP_NAME and TABLE_PROPERTIES objects should
  // be reset to these default values so that tests begin in a known state.
  public static final String DEFAULT_APP_NAME = 
      TestConstants.TABLES_DEFAULT_APP_NAME;
  public static final String DEFAULT_TABLE_ID = TestConstants.DEFAULT_TABLE_ID;
  public static final SQLQueryStruct DEFAULT_SQL_QUERY_STRUCT = 
      TestConstants.getSQLQueryStructMock();
  public static final UserTable DEFAULT_USER_TABLE = null;
  
  public static String APP_NAME = DEFAULT_APP_NAME;
  public static String TABLE_ID = DEFAULT_TABLE_ID;
  public static SQLQueryStruct SQL_QUERY_STRUCT = DEFAULT_SQL_QUERY_STRUCT;
  public static UserTable USER_TABLE = DEFAULT_USER_TABLE;
  
  public static final boolean DEFAULT_BUILD_MENU_FRAGMENT = false;
  public static final boolean DEFAULT_BUILD_DISPLAY_FRAGMENT = false;
  /**
   *  True if the menu fragment should be initialized with the rest of the
   *  state setup.
   */
  public static boolean BUILD_MENU_FRAGMENT = DEFAULT_BUILD_MENU_FRAGMENT;
  /**
   * True if the display fragments should be initialized.
   */
  public static boolean BUILD_DISPLAY_FRAGMENT = DEFAULT_BUILD_DISPLAY_FRAGMENT;
  
  public static SpreadsheetFragment SPREADSHEET_FRAGMENT = null;
  public static GraphManagerFragment GRAPH_MANAGER_FRAGMENT = null;
  public static GraphViewFragment GRAPH_VIEW_FRAGMENT = null;
  public static MapListViewFragment MAP_LIST_VIEW_FRAGMENT = null;
  public static TableMapInnerFragment MAP_INNER_FRAGMENT = null;
  public static ListViewFragment LIST_VIEW_FRAGMENT = null;
  public static DetailViewFragment DETAIL_VIEW_FRAGMENT = null;
  
  @Override
  String retrieveAppNameFromIntent() {
    return APP_NAME;
  }
  
  @Override
  String retrieveTableIdFromIntent() {
    return TABLE_ID;
  }
  
  @Override
  SQLQueryStruct retrieveSQLQueryStatStructFromIntent() {
    return SQL_QUERY_STRUCT;
  }
  
  @Override
  public UserTable getUserTable() {
    return USER_TABLE;
  }
  
  @Override
  protected void initializeDisplayFragment() {
    if (BUILD_DISPLAY_FRAGMENT) {
      super.initializeDisplayFragment();
    }
  }
  
  @Override
  public void refreshDisplayFragment() {
    if (BUILD_DISPLAY_FRAGMENT) {
      super.refreshDisplayFragment();
    }
  }
  
  @Override
  DetailViewFragment createDetailViewFragment(String fileName, String rowId) {
    return DETAIL_VIEW_FRAGMENT;
  }
  
  @Override
  GraphManagerFragment createGraphManagerFragment() {
    return GRAPH_MANAGER_FRAGMENT;
  }
  
  @Override
  GraphViewFragment createGraphViewFragment(String graphName) {
    return GRAPH_VIEW_FRAGMENT;
  }
  
  @Override
  TableMapInnerFragment createInnerMapFragment() {
    return MAP_INNER_FRAGMENT;
  }
  
  @Override
  MapListViewFragment createMapListViewFragment(String listViewFileName) {
    return MAP_LIST_VIEW_FRAGMENT;
  }
  
  @Override
  SpreadsheetFragment createSpreadsheetFragment() {
    return SPREADSHEET_FRAGMENT;
  }
  
  @Override
  ListViewFragment createListViewFragment(String fileName) {
    // TODO Auto-generated method stub
    return super.createListViewFragment(fileName);
  }
  
  /**
   * Reset the stub's state to the default values. Should be called after each
   * test modifying the object.
   */
  public static void resetState() {
    APP_NAME = DEFAULT_APP_NAME;
    TABLE_ID = DEFAULT_TABLE_ID;
    SQL_QUERY_STRUCT = DEFAULT_SQL_QUERY_STRUCT;
    USER_TABLE = TestConstants.getUserTableMock();
    BUILD_MENU_FRAGMENT = DEFAULT_BUILD_MENU_FRAGMENT;
    BUILD_DISPLAY_FRAGMENT = DEFAULT_BUILD_DISPLAY_FRAGMENT;
    SPREADSHEET_FRAGMENT = null;
    LIST_VIEW_FRAGMENT = null;
    DETAIL_VIEW_FRAGMENT = null;
    GRAPH_MANAGER_FRAGMENT = null;
    GRAPH_VIEW_FRAGMENT = null;
    MAP_INNER_FRAGMENT = null;
    MAP_LIST_VIEW_FRAGMENT = null;
  }

}
