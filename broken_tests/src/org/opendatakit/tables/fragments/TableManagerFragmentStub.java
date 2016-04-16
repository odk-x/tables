/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

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
