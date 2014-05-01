package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.testutils.TestCaseUtils;

/**
 * Basic implementation of a TableActivity for testing. More complicated
 * table activity stubs should extend this one.
 * <p>
 * Set the static objects here as you need them, but be sure to reset them to
 * their default values.
 * @author sudar.sam@gmail.com
 *
 */
public class TableActivityStub extends AbsTableActivity {
  
  // If modified during tests, the APP_NAME and TABLE_PROPERTIES objects should
  // be reset to these default values so that tests begin in a known state.
  public static final String DEFAULT_APP_NAME = 
      TestCaseUtils.TABLES_DEFAULT_APP_NAME;
  public static final TableProperties DEFAULT_TABLE_PROPERTIES = null;
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

}
