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

import org.opendatakit.data.TableViewType;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains information about which {@see TableViewType}s are valid for a
 * table based on its configuration. A List view may only be appropriate if a
 * list file has been set, for example.
 * <p>
 * See {@see TableViewTypeAdapter}, {@see DefaultViewTypePreference}
 *
 * @author sudar.sam@gmail.com
 */
public class PossibleTableViewTypes {

  private boolean mSpreadsheetIsValid;
  private boolean mListIsValid;
  private String mListFileName;
  private boolean mMapIsValid;
  private String mMapListFileName;

  private ViewFragmentType mDefaultViewType;
  private String mDetailFileName;

  /**
   * Constructs a new PossibleTableViewTypes object, which determines whether the user is able to
   * select a particular view type (spreadsheet, map, etc...)
   *
   * @param dbInterface  The database interface to use
   * @param appName      the app name
   * @param db           a database handle
   * @param tableId      the id of the table we're editing
   * @param orderedDefns the columns in the table
   * @throws ServicesAvailabilityException if the database is down
   */
  public PossibleTableViewTypes(UserDbInterface dbInterface, String appName, DbHandle db,
      String tableId, OrderedColumns orderedDefns) throws ServicesAvailabilityException {
    TableViewType defaultViewType = TableUtil.get()
        .getDefaultViewType(dbInterface, appName, db, tableId);
    if (defaultViewType != null) {
      switch (defaultViewType) {
      case SPREADSHEET:
        mDefaultViewType = ViewFragmentType.SPREADSHEET;
        break;
      case MAP:
        mDefaultViewType = ViewFragmentType.MAP;
        break;
      case LIST:
        mDefaultViewType = ViewFragmentType.LIST;
        break;
      }
    } else {
      mDefaultViewType = ViewFragmentType.SPREADSHEET;
    }

    mSpreadsheetIsValid = true; // always
    mListFileName = TableUtil.get().getListViewFilename(dbInterface, appName, db, tableId);
    mListIsValid = null != mListFileName;
    mMapListFileName = TableUtil.get().getMapListViewFilename(dbInterface, appName, db, tableId);
    mMapIsValid = null != mMapListFileName && orderedDefns.mapViewIsPossible();

    mDetailFileName = TableUtil.get().getDetailViewFilename(dbInterface, appName, db, tableId);
  }

  /**
   * Get a set with all the {@see TableViewType}s that are valid. If only a
   * spreadsheet and list view are possible, for instance, it will contain
   * {@see TableViewType#SPREADSHEET} and {@see TableViewType#LIST}.
   *
   * @return a {@link Set} of the possible view types.
   */
  public Set<TableViewType> getAllPossibleViewTypes() {
    Set<TableViewType> result = new HashSet<>();
    if (this.spreadsheetViewIsPossible()) {
      result.add(TableViewType.SPREADSHEET);
    }
    if (this.listViewIsPossible()) {
      result.add(TableViewType.LIST);
    }
    if (this.mapViewIsPossible()) {
      result.add(TableViewType.MAP);
    }
    return result;
  }

  /**
   * @return the default view type for this table
   */
  public ViewFragmentType getDefaultViewType() {
    return mDefaultViewType;
  }

  /**
   * @return The default list view filename
   */
  public String getDefaultListViewFileName() {
    return mListFileName;
  }

  /**
   * @return the default map list view filename
   */
  public String getDefaultMapListViewFileName() {
    return mMapListFileName;
  }

  /**
   * @return the default detail view filename
   */
  public String getDefaultDetailFileName() {
    return mDetailFileName;
  }

  /**
   * @return true if the table can be viewed as a spreadsheet
   */
  public boolean spreadsheetViewIsPossible() {
    return this.mSpreadsheetIsValid;
  }

  /**
   * @return true if the table can be displayed as a list
   */
  public boolean listViewIsPossible() {
    return this.mListIsValid;
  }

  /**
   * @return true if the table can be displayed as a map
   */
  public boolean mapViewIsPossible() {
    return this.mMapIsValid;
  }

  /**
   * @return true if the table can be displayed as a map with navigation
   */
  public boolean navigateViewIsPossible() {
    // This is identical to mapViewIsPossible, since any map view could also be used for navigation
    return this.mMapIsValid;
  }

}
