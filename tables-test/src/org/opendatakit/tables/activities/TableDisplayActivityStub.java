package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.testutils.TestConstants;

public class TableDisplayActivityStub extends TableDisplayActivity {
  
  // If modified during tests, the APP_NAME and TABLE_PROPERTIES objects should
  // be reset to these default values so that tests begin in a known state.
  public static final String DEFAULT_APP_NAME = 
      TestConstants.TABLES_DEFAULT_APP_NAME;
  public static final TableProperties DEFAULT_TABLE_PROPERTIES = 
      TestConstants.TABLE_PROPERTIES_MOCK;
  public static final String DEFAULT_TABLE_ID = "testTableId";
  
  public static String APP_NAME = DEFAULT_APP_NAME;
  public static TableProperties TABLE_PROPERTIES = DEFAULT_TABLE_PROPERTIES;
  public static String TABLE_ID = DEFAULT_TABLE_ID;
  
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
  
  /**
   * Reset the stub's state to the default values. Should be called after each
   * test modifying the object.
   */
  public static void resetState() {
    APP_NAME = DEFAULT_APP_NAME;
    TABLE_ID = DEFAULT_TABLE_ID;
    TABLE_PROPERTIES = DEFAULT_TABLE_PROPERTIES;
  }

}
