package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment.ITopLevelTableMenuActivity;
import org.opendatakit.testutils.TestConstants;

/**
 * Basic implementation of a TableActivity for testing. More complicated
 * table activity stubs should extend this one.
 * <p>
 * Set the static objects here as you need them, but be sure to reset them to
 * their default values.
 * @author sudar.sam@gmail.com
 *
 */
public class AbsTableActivityStub extends AbsTableActivity implements 
    ITopLevelTableMenuActivity {
  
  // If modified during tests, the APP_NAME and TABLE_PROPERTIES objects should
  // be reset to these default values so that tests begin in a known state.
  public static final String DEFAULT_APP_NAME = 
      TestConstants.TABLES_DEFAULT_APP_NAME;
  public static final TableProperties DEFAULT_TABLE_PROPERTIES = 
      TestConstants.getTablePropertiesMock();
  public static final String DEFAULT_TABLE_ID = "testTableId";
  public static final ViewFragmentType DEFAULT_FRAGMENT_TYPE =
      ViewFragmentType.SPREADSHEET;
  
  public static String APP_NAME = DEFAULT_APP_NAME;
  public static TableProperties TABLE_PROPERTIES = DEFAULT_TABLE_PROPERTIES;
  public static String TABLE_ID = DEFAULT_TABLE_ID;
  public static ViewFragmentType FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
  
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

  @Override
  public ViewFragmentType getCurrentFragmentType() {
    return FRAGMENT_TYPE;
  }

}
