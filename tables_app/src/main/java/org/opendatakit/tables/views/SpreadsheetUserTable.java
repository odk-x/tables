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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.data.UserTable;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.data.Row;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.AbsTableDisplayFragment;

import android.content.Context;

/**
 * Wrapper class for UserTable that presents the table in the way that the
 * configuration says the UserTable should be presented.
 *
 * @author Administrator
 *
 */
public class SpreadsheetUserTable {
  @SuppressWarnings("unused")
  private static final String TAG = "SpreadsheetUserTable";

  private final AbsTableDisplayFragment fragment;
  private final String indexColumnElementKey;
  private final String[] header;
  private final String[] spreadsheetIndexToElementKey;
  private final Map<String, Integer> elementKeyToSpreadsheetIndex;
  private final Map<String, ArrayList<Map<String,Object>>> elementKeyToDisplayChoicesList;
  UserTable userTable;

  public SpreadsheetUserTable(AbsTableDisplayFragment frag) throws ServicesAvailabilityException {
    PropertiesSingleton props = CommonToolProperties.get(Tables.getInstance(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
    this.fragment = frag;

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    ArrayList<String> colOrder;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(frag.getAppName());
      userTable = getUserTable();
      indexColumnElementKey = TableUtil.get().getIndexColumn(dbInterface, getAppName(), db, getTableId());
      colOrder = TableUtil.get().getColumnOrder(dbInterface, frag.getAppName(), db, frag.getTableId(),
              frag.getColumnDefinitions());

      header = new String[colOrder.size()];
      spreadsheetIndexToElementKey = new String[colOrder.size()];
      elementKeyToSpreadsheetIndex = new HashMap<String, Integer>();
      elementKeyToDisplayChoicesList = new HashMap<String, ArrayList<Map<String,Object>>>();

      for (int i = 0; i < colOrder.size(); ++i) {
        String elementKey = colOrder.get(i);
        String localizedDisplayName;
        localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(userSelectedDefaultLocale,
            dbInterface, getAppName(), db, frag.getTableId(), elementKey);

        header[i] = localizedDisplayName;
        spreadsheetIndexToElementKey[i] = elementKey;
        elementKeyToSpreadsheetIndex.put(elementKey, i);

        ArrayList<Map<String,Object>> choices =
        ColumnUtil.get().getDisplayChoicesList(dbInterface, getAppName(), db, frag.getTableId(), elementKey);
        elementKeyToDisplayChoicesList.put(elementKey, choices);
      }
    } finally {
      if ( db != null ) {
        dbInterface.closeDatabase(frag.getAppName(), db);
      }
    }
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

  public ArrayList<Map<String,Object>> getColumnDisplayChoicesList(String elementKey) {
    return elementKeyToDisplayChoicesList.get(elementKey);
  }

  public ColorRuleGroup getColumnColorRuleGroup(UserDbInterface dbInterface,
      DbHandle db, String elementKey, String[] adminColumns) throws
      ServicesAvailabilityException {
    return ColorRuleGroup.getColumnColorRuleGroup(dbInterface,
        getAppName(), db, getTableId(), elementKey, adminColumns);
  }

  public ColorRuleGroup getStatusColumnRuleGroup(UserDbInterface dbInterface,
      DbHandle db, String[] adminColumns) throws ServicesAvailabilityException {
    return ColorRuleGroup.getStatusColumnRuleGroup(dbInterface,
        getAppName(), db, getTableId(), adminColumns);
  }

  public ColorRuleGroup getTableColorRuleGroup(UserDbInterface dbInterface,
      DbHandle db, String[] adminColumns) throws ServicesAvailabilityException {
    return ColorRuleGroup.getTableColorRuleGroup(dbInterface,
        getAppName(), db, getTableId(), adminColumns);
  }

  int getNumberOfRows() {
    UserTable table = fragment.getUserTable();
    if ( table == null ) {
      return 0;
    }
    return table.getNumberOfRows();
  }

  public Row getRowAtIndex(int index) {
    UserTable table = fragment.getUserTable();
    if ( table == null ) {
      return null;
    }
    return table.getRowAtIndex(index);
  }

  public UserTable getUserTable() {
    return fragment.getUserTable();
  }

  public UserTable getCachedUserTable() {
    return userTable;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Whether or not we have a frozen column...

  public String getIndexedColumnElementKey() {
    return indexColumnElementKey;
  }

  boolean isIndexed() {
    String elementKey = getIndexedColumnElementKey();
    return elementKey != null && elementKey.length() != 0;
  }

  // ///////////////////////////////////
  // These need to be re-worked...

  public boolean hasData() {
    UserTable table = fragment.getUserTable();
    return !(table == null || (header.length == 0));
  }

  public static class SpreadsheetCell {
    public int rowNum; // of the row
    public Row row; // the row
    public String elementKey; // of the column
    public String displayText;
    public String value;
  }

  public SpreadsheetCell getSpreadsheetCell(Context context, CellInfo cellInfo) {
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
    cell.value = cell.row.getDataByKey(cellInfo.elementKey);
    return cell;
  }

  public ColumnDefinition getColumnByIndex(int headerCellNum) {
    return getColumnByElementKey(spreadsheetIndexToElementKey[headerCellNum]);
  }

  public ColumnDefinition getColumnByElementKey(String elementKey) {
    OrderedColumns orderedDefns = getColumnDefinitions();
    return orderedDefns.find(elementKey);
  }

  public int getWidth() {
    return header.length;
  }

  Integer getColumnIndexOfElementKey(String elementKey) {
    return elementKeyToSpreadsheetIndex.get(elementKey);
  }

  public int getNumberOfDisplayColumns() {
    return header.length;
  }

  String getHeader(int colNum) {
    return header[colNum];
  }
}
