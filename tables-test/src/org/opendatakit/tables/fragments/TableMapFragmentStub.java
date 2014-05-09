package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.testutils.TestConstants;

public class TableMapFragmentStub extends TableMapFragment {
  
  public static final TableProperties DEFAULT_TABLE_PROPERTIES =
      TestConstants.getTablePropertiesMock();
  public static final String DEFAULT_FILE_NAME =
      TestConstants.DEFAULT_FILE_NAME;
  
  public static TableProperties TABLE_PROPERTIES =
      DEFAULT_TABLE_PROPERTIES;
  public static String FILE_NAME = DEFAULT_FILE_NAME;
  
  public static void resetState() {
    TABLE_PROPERTIES = DEFAULT_TABLE_PROPERTIES;
    FILE_NAME = DEFAULT_FILE_NAME;
  }
  
  @Override
  TableMapInnerFragment createInnerMapFragment() {
    return super.createInnerMapFragment();
  }
  
  @Override
  MapListViewFragment createMapListViewFragment(String listViewFileName) {
    return super.createMapListViewFragment(listViewFileName);
  }
  
  
  

}
