/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.data;

import java.util.HashSet;
import java.util.Set;

import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.utils.TableUtil;

import android.os.RemoteException;

/**
 * Contains information about which {@link TableViewType}s are valid for a
 * table based on its configuration. A List view may only be appropriate if a
 * list file has been set, for example.
 * @author sudar.sam@gmail.com
 *
 */
public class PossibleTableViewTypes {
  
  private boolean mSpreadsheetIsValid;
  private boolean mListIsValid;
  private boolean mMapIsValid;
  private boolean mGraphIsValid;
  
  public PossibleTableViewTypes(String appName, OdkDbHandle db, String tableId, OrderedColumns orderedDefns) throws RemoteException {
    this.mSpreadsheetIsValid = true; // always
    this.mListIsValid = (null != TableUtil.get().getListViewFilename(appName, db, tableId));
    this.mMapIsValid = (null != TableUtil.get().getMapListViewFilename(appName, db, tableId)) &&
        orderedDefns.mapViewIsPossible();
    this.mGraphIsValid = orderedDefns.graphViewIsPossible();
  }
  
  /**
   * Get a set with all the {@link TableViewType}s that are valid. If only a
   * spreadsheet and list view are possible, for instance, it will contain
   * {@link TableViewType#SPREADSHEET} and {@link TableViewType#LIST}.
   * @return a {@link Set} of the possible view types.
   */
  public Set<TableViewType> getAllPossibleViewTypes() {
    Set<TableViewType> result = new HashSet<TableViewType>();
    if (this.spreadsheetViewIsPossible()) {
      result.add(TableViewType.SPREADSHEET);
    }
    if (this.listViewIsPossible()) {
      result.add(TableViewType.LIST);
    }
    if (this.mapViewIsPossible()) {
      result.add(TableViewType.MAP);
    }
    if (this.graphViewIsPossible()) {
      result.add(TableViewType.GRAPH);
    }
    return result;
  }
  
  /**
   * 
   * @return true if the table can be viewed as a spreadsheet
   */
  public boolean spreadsheetViewIsPossible() {
    return this.mSpreadsheetIsValid;
  }
  
  /**
   * 
   * @return true if the table can be displayed as a list
   */
  public boolean listViewIsPossible() {
    return this.mListIsValid;
  }
  
  /**
   * 
   * @return true if the table can be displayed as a map
   */
  public boolean mapViewIsPossible() {
    return this.mMapIsValid;
  }
  
  /**
   * 
   * @return true if the table can be displayed as a graph
   */
  public boolean graphViewIsPossible() {
    return this.mGraphIsValid;
  }
  

}
