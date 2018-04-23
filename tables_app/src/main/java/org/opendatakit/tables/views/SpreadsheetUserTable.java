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
package org.opendatakit.tables.views;

import android.app.Activity;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.*;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.activities.ISpreadsheetFragmentContainer;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.AbsTableDisplayFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for UserTable that presents the table in the way that the
 * configuration says the UserTable should be presented.
 *
 * @author Administrator
 */
public class SpreadsheetUserTable implements ISpreadsheetFragmentContainer {
  // A fragment that has the ability to display a table
  private final AbsTableDisplayFragment fragment;

  // Which column is indexed, if any
  private final String indexColumnElementKey;
  // The localized display names for the columns of the table
  private final String[] header;
  private final String[] header_keys;
  //
  private final String[] spreadsheetIndexToElementKey;
  private final Map<String, Integer> elementKeyToSpreadsheetIndex;
  private SpreadsheetProps props;
  private UserTable userTable;

  /**
   * Constructs a SpreadsheetUserTable
   *
   * @param frag the fragment we're embedded in
   * @throws ServicesAvailabilityException if the database is down
   */
  public SpreadsheetUserTable(AbsTableDisplayFragment frag) throws ServicesAvailabilityException {
    this.fragment = frag;
    props = null;
    if (frag == null) {
      throw new IllegalStateException("Must have a fragment to get appname to open database");
    }
    Activity act = frag.getActivity();
    if (act instanceof ISpreadsheetFragmentContainer) {
      props = ((ISpreadsheetFragmentContainer) act).getProps();
    }
    PropertiesSingleton props = CommonToolProperties.get(frag.getCommonApplication(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    ArrayList<String> colOrder;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(frag.getAppName());
      userTable = getUserTable();
      if (this.props != null) {
        indexColumnElementKey = this.props.getFrozen();
      } else {
        indexColumnElementKey = TableUtil.get()
            .getIndexColumn(dbInterface, getAppName(), db, getTableId());
        //indexColumnElementKey = null;
      }
      colOrder = TableUtil.get()
          .getColumnOrder(dbInterface, frag.getAppName(), db, frag.getTableId(),
              frag.getColumnDefinitions());

      header = new String[colOrder.size()];
      header_keys = new String[colOrder.size()];
      spreadsheetIndexToElementKey = new String[colOrder.size()];
      elementKeyToSpreadsheetIndex = new HashMap<>();

      for (int i = 0; i < colOrder.size(); ++i) {
        String elementKey = colOrder.get(i);
        String localizedDisplayName;
        localizedDisplayName = ColumnUtil.get()
            .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, getAppName(), db,
                frag.getTableId(), elementKey);

        header[i] = localizedDisplayName;
        header_keys[i] = elementKey;
        spreadsheetIndexToElementKey[i] = elementKey;
        elementKeyToSpreadsheetIndex.put(elementKey, i);
      }
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(frag.getAppName(), db);
      }
    }
  }

  public SpreadsheetProps getProps() {
    return props;
  }

  public String getTableId() {
    return fragment.getTableId();
  }

  public String getAppName() {
    return fragment.getAppName();
  }

  public OrderedColumns getColumnDefinitions() {
    return fragment.getColumnDefinitions();
  }

  /**
   * TODO document
   *
   * @param dbInterface
   * @param db
   * @param elementKey
   * @param adminColumns
   * @return
   * @throws ServicesAvailabilityException
   */
  ColorRuleGroup getColumnColorRuleGroup(UserDbInterface dbInterface, DbHandle db,
      String elementKey, String[] adminColumns) throws ServicesAvailabilityException {
    return ColorRuleGroup
        .getColumnColorRuleGroup(dbInterface, getAppName(), db, getTableId(), elementKey,
            adminColumns);
  }

  int getNumberOfRows() {
    UserTable table = fragment.getUserTable();
    if (table == null) {
      return 0;
    }
    return table.getNumberOfRows();
  }

  /**
   * Gets the row at the requested index from the table, or null if the index is out of bounds
   *
   * @param index the index of the row
   * @return the requested row or null
   */
  public TypedRow getRowAtIndex(int index) {
    UserTable table = fragment.getUserTable();
    if (table == null) {
      return null;
    }
    return table.getRowAtIndex(index);
  }

  UserTable getUserTable() {
    return fragment.getUserTable();
  }

  UserTable getCachedUserTable() {
    return userTable;
  }

  // Whether or not we have a frozen column...

  String getIndexedColumnElementKey() {
    return indexColumnElementKey;
  }

  boolean isIndexed() {
    String elementKey = getIndexedColumnElementKey();
    return elementKey != null && !elementKey.isEmpty();
  }

  // These need to be re-worked...

  /**
   * Tries to determine if the table has any data in it or not
   *
   * @return whether there is data in the user table
   */
  public boolean hasData() {
    UserTable table = fragment.getUserTable();
    return !(table == null || header.length == 0);
  }

  /**
   * Gets a cell from the given CellInfo object. Used in SpreadsheetFragment
   *
   * @param cellInfo an object that has a row id and column (elementKey) in it
   * @return a SpreadsheetCell object from the CellInfo object
   */
  public SpreadsheetCell getSpreadsheetCell(CellInfo cellInfo) {
    SpreadsheetCell cell = new SpreadsheetCell();
    userTable = getUserTable();
    cell.rowNum = cellInfo.rowId;
    cell.row = userTable.getRowAtIndex(cellInfo.rowId);
    cell.elementKey = cellInfo.elementKey;
    OrderedColumns orderedDefns = getColumnDefinitions();
    ColumnDefinition cd = orderedDefns.find(cellInfo.elementKey);
    getTableId();
    cell.displayText = userTable
        .getDisplayTextOfData(cellInfo.rowId, cd.getType(), cellInfo.elementKey);
    cell.value = cell.row.getStringValueByKey(cellInfo.elementKey);
    return cell;
  }

  /**
   * TODO document
   *
   * @param headerCellNum
   * @return
   */
  ColumnDefinition getColumnByIndex(int headerCellNum) {
    return getColumnByElementKey(spreadsheetIndexToElementKey[headerCellNum]);
  }

  /**
   * Finds a column definition given a column key
   *
   * @param elementKey the id of the requested column
   * @return a column definition object for the column with that id
   */
  public ColumnDefinition getColumnByElementKey(String elementKey) {
    return getColumnDefinitions().find(elementKey);
  }

  public int getWidth() {
    return header.length;
  }

  /**
   * TODO document
   *
   * @param elementKey
   * @return
   */
  Integer getColumnIndexOfElementKey(String elementKey) {
    return elementKeyToSpreadsheetIndex.get(elementKey);
  }

  int getNumberOfDisplayColumns() {
    return header.length;
  }

  String getHeader(int colNum) {
    return header[colNum];
  }

  String getHeaderKey(int colNum) {
    if (colNum < 0 || colNum >= header_keys.length)
      return null;
    return header_keys[colNum];
  }

  /**
   * A class that holds a row, column id, value, row number and some display text
   */
  public static class SpreadsheetCell {
    /**
     * The row of the cell
     */
    public TypedRow row; // the row
    /**
     * The column of the cell
     */
    public String elementKey; // of the column
    /**
     * The actual data in the cell
     */
    public String value;
    /**
     * TODO document
     */
    int rowNum; // of the row
    /**
     * TODO document
     */
    String displayText;
  }
}
