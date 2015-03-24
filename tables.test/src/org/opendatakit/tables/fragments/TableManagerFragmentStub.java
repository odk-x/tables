package org.opendatakit.tables.fragments;

import java.util.List;

import org.opendatakit.tables.utils.TableNameStruct;

public class TableManagerFragmentStub extends TableManagerFragment {
  
  List<TableNameStruct> dummyList;
  int numberOfCallsToUpdatePropertiesList;
  
  public TableManagerFragmentStub(List<TableNameStruct> toDisplay) {
    this.dummyList = toDisplay;
    this.numberOfCallsToUpdatePropertiesList = 0;
  }
  
  @Override
  protected void updateTableIdList() {
    this.numberOfCallsToUpdatePropertiesList++;
    super.updateTableIdList();
//    super.setList(dummyList);
  }
  
  /**
   * Get the number of times
   * {@link TableManagerFragment#updateTablePropertiesList()} was called.
   * A helper function that is an alternative to using mockito's mocks and
   * spies.
   * @return
   */
  public int getNumberOfCallsToUpdatePropertiesList() {
    return this.numberOfCallsToUpdatePropertiesList;
  }

}
