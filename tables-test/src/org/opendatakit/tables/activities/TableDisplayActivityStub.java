package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
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
  public static final TableProperties DEFAULT_TABLE_PROPERTIES = 
      TestConstants.getTablePropertiesMock();
  public static final String DEFAULT_TABLE_ID = TestConstants.DEFAULT_TABLE_ID;
  public static final SQLQueryStruct DEFAULT_SQL_QUERY_STRUCT = 
      TestConstants.getSQLQueryStructMock();
  public static final UserTable DEFAULT_USER_TABLE = 
      TestConstants.getUserTableMock();
  
  public static String APP_NAME = DEFAULT_APP_NAME;
  public static TableProperties TABLE_PROPERTIES = DEFAULT_TABLE_PROPERTIES;
  public static String TABLE_ID = DEFAULT_TABLE_ID;
  public static SQLQueryStruct SQL_QUERY_STRUCT = DEFAULT_SQL_QUERY_STRUCT;
  public static UserTable USER_TABLE = DEFAULT_USER_TABLE;
  
  public static final boolean DEFAULT_BUILD_MENU_FRAGMENT = true;
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
  
  @Override
  TableProperties retrieveTablePropertiesForId(String tableId) {
    return TABLE_PROPERTIES;
  }
  
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
  UserTable retrieveUserTable() {
    return USER_TABLE;
  }
  
  @Override
  protected void initializeMenuFragment() {
    if (BUILD_MENU_FRAGMENT) {
      super.initializeMenuFragment();
    }
  }
  
  @Override
  protected void initializeDisplayFragment() {
    if (BUILD_DISPLAY_FRAGMENT) {
      super.initializeDisplayFragment();
    }
  }
  
  /**
   * Reset the stub's state to the default values. Should be called after each
   * test modifying the object.
   */
  public static void resetState() {
    APP_NAME = DEFAULT_APP_NAME;
    TABLE_ID = DEFAULT_TABLE_ID;
    TABLE_PROPERTIES = DEFAULT_TABLE_PROPERTIES;
    SQL_QUERY_STRUCT = DEFAULT_SQL_QUERY_STRUCT;
    USER_TABLE = DEFAULT_USER_TABLE;
    BUILD_MENU_FRAGMENT = DEFAULT_BUILD_MENU_FRAGMENT;
    BUILD_DISPLAY_FRAGMENT = DEFAULT_BUILD_DISPLAY_FRAGMENT;
  }

}
