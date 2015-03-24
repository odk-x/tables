package org.opendatakit.tables.activities;

import org.opendatakit.testutils.TestConstants;

public class AbsBaseActivityStub extends AbsBaseActivity {
  
  public static final String DEFAULT_APP_NAME = 
      TestConstants.TABLES_DEFAULT_APP_NAME;
    
  public static String APP_NAME = DEFAULT_APP_NAME;
    
  @Override
  String retrieveAppNameFromIntent() {
    return APP_NAME;
  }
  
  public static void resetState() {
    APP_NAME = DEFAULT_APP_NAME;
  }

  @Override
  public void databaseAvailable() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void databaseUnavailable() {
    // TODO Auto-generated method stub
    
  }

}
