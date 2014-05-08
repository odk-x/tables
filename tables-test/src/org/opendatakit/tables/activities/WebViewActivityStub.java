package org.opendatakit.tables.activities;

import org.opendatakit.testutils.TestConstants;

public class WebViewActivityStub extends WebViewActivity {
  
  public static final String DEFAULT_APP_NAME =
      TestConstants.TABLES_DEFAULT_APP_NAME;
  
  public static String APP_NAME = DEFAULT_APP_NAME;
  
  public static void resetState() {
    APP_NAME = DEFAULT_APP_NAME;
  }
  
  @Override
  public String getAppName() {
    return APP_NAME;
  }

}
