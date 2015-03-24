package org.opendatakit.tables.fragments;

import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.testutils.TestConstants;

import android.webkit.WebView;

public class WebFragmentStub extends WebFragment {
  
  public static final WebView DEFAULT_WEB_VIEW =
      TestConstants.getWebViewMock();
  public static final String DEFAULT_FILE_NAME =
      TestConstants.DEFAULT_FILE_NAME;
  public static final Control DEFAULT_CONTROL =
      TestConstants.getControlMock();
  
  public static WebView WEB_VIEW = DEFAULT_WEB_VIEW;
  public static String FILE_NAME = DEFAULT_FILE_NAME;
  public static Control CONTROL = DEFAULT_CONTROL;
  
  public static void resetState() {
    WEB_VIEW = DEFAULT_WEB_VIEW;
    FILE_NAME = DEFAULT_FILE_NAME;
  }
  
  
}
