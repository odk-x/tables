package org.opendatakit.tables.fragments;

import java.util.List;

import org.opendatakit.common.android.data.TableProperties;

public class TableManagerFragmentStub extends TableManagerFragment {
  
  List<TableProperties> dummyList;
  int numberOfCallsToUpdatePropertiesList;
  
  public TableManagerFragmentStub(List<TableProperties> toDisplay) {
    this.dummyList = toDisplay;
    this.numberOfCallsToUpdatePropertiesList = 0;
  }
  
  @Override
  List<TableProperties> retrieveContentsToDisplay() {
    return this.dummyList;
  }
  
  @Override
  protected void updateTablePropertiesList() {
    this.numberOfCallsToUpdatePropertiesList++;
    super.updateTablePropertiesList();
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
