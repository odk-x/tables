package org.opendatakit.tables.fragments;

import java.util.List;

import org.opendatakit.common.android.data.TableProperties;

public class TableManagerFragmentStub extends TableManagerFragment {
  
  List<TableProperties> dummyList;
  
  public TableManagerFragmentStub(List<TableProperties> toDisplay) {
    this.dummyList = toDisplay;
  }
  
  @Override
  List<TableProperties> retrieveContentsToDisplay() {
    return this.dummyList;
  }

}
