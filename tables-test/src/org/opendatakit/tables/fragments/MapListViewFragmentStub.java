package org.opendatakit.tables.fragments;

import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;
import org.opendatakit.testutils.TestConstants;

public class MapListViewFragmentStub extends MapListViewFragment {
  
  public static TableData TABLE_DATA = TestConstants.getTableDataMock();
  public static Control CONTROL = TestConstants.getControlMock();
  public static String FILE_NAME = TestConstants.DEFAULT_FILE_NAME;
  
  public static void resetState() {
    TABLE_DATA = TestConstants.getTableDataMock();
    CONTROL = TestConstants.getControlMock();
    FILE_NAME = TestConstants.DEFAULT_FILE_NAME;
  }
  
  @Override
  public Control createControlObject() {
    return CONTROL;
  }
  
  @Override
  protected TableData createDataObject() {
    return TABLE_DATA;
  }
  
  @Override
  public String getFileName() {
    return FILE_NAME;
  }

}
