package org.opendatakit.tables.fragments;

import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.views.webkits.CustomView;
import org.opendatakit.testutils.TestConstants;

public class AbsWebTableFragmentStub extends AbsWebTableFragment {
  
  public static final CustomView DEFAULT_CUSTOM_VIEW = 
      TestConstants.getCustomViewMock();
  public static final String DEFAULT_FILE_NAME =
      TestConstants.DEFAULT_FILE_NAME;
  public static final ViewFragmentType DEFAULT_FRAGMENT_TYPE =
      ViewFragmentType.SPREADSHEET;
  
  public static CustomView CUSTOM_VIEW = DEFAULT_CUSTOM_VIEW;
  public static String FILE_NAME = DEFAULT_FILE_NAME;
  public static ViewFragmentType FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
  
  public static void resetState() {
    CUSTOM_VIEW = DEFAULT_CUSTOM_VIEW;
    FILE_NAME = DEFAULT_FILE_NAME;
    FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
  }
  
  @Override
  public CustomView buildView() {
    return CUSTOM_VIEW;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    // TODO Auto-generated method stub
    return null;
  }

}
