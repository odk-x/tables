package org.opendatakit.tables.fragments;

import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;
import org.opendatakit.testutils.TestConstants;

public class ListViewFragmentStub extends ListViewFragment {
  
  public static TableData TABLE_DATA = TestConstants.getTableDataMock();
  public static Control CONTROL = TestConstants.getControlMock();
  
  public static void resetState() {
    TABLE_DATA = TestConstants.getTableDataMock();
    CONTROL = TestConstants.getControlMock();
  }
  
  @Override
  public Control createControlObject() {
    return CONTROL;
  }
  
  @Override
  protected TableData createDataObject() {
    return TABLE_DATA;
  }
  
  

}
