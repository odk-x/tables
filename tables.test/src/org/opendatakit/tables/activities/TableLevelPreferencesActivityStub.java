package org.opendatakit.tables.activities;

import org.opendatakit.testutils.TestConstants;

import android.os.Bundle;


/**
 * Basic stub for testing {@link TableLevelPreferencesActivity}. We need to do
 * things like return a known TableProperties object.
 * <p>
 * Set the static objects here as you need them, but be sure to reset them
 * to their default values.
 * @author sudar.sam@gmail.com
 *
 */
public class TableLevelPreferencesActivityStub 
    extends TableLevelPreferencesActivity {
  
  // It is ugly, but I think that this has to be duplicated in each activity,
  // since java doesn't support multiple inheritance. Ideally we'd be extending
  // both the class under test and the TableActivityStub here.
  
  // If modified during tests, the APP_NAME and TABLE_PROPERTIES objects should
  // be reset to these default values so that tests begin in a known state.
  public static final String DEFAULT_APP_NAME = 
      TestConstants.TABLES_DEFAULT_APP_NAME;
  public static final String DEFAULT_TABLE_ID = "testTableId";
  public static final FragmentType DEFAULT_FRAGMENT_TYPE =
      FragmentType.TABLE_PREFERENCE;
  
  public static FragmentType FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
  public static String APP_NAME = DEFAULT_APP_NAME;
  public static String TABLE_ID = DEFAULT_TABLE_ID;
  
  public static void resetState() {
    FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
    APP_NAME = DEFAULT_APP_NAME;
    TABLE_ID = DEFAULT_TABLE_ID;
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
  FragmentType retrieveFragmentTypeFromBundleOrActivity(Bundle savedInstanceState) {
    return FRAGMENT_TYPE;
  }
  

}
