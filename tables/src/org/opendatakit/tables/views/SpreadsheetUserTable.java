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

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.AbsTableDisplayFragment;
import org.opendatakit.tables.utils.TableUtil;

import android.content.Context;
import android.os.RemoteException;

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

  public SpreadsheetUserTable(AbsTableDisplayFragment frag) throws RemoteException {
    this.fragment = frag;

    // UserTable table = frag.getUserTable();
    ArrayList<String> colOrder;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(frag.getAppName(), false);
      colOrder = TableUtil.get().getColumnOrder(frag.getAppName(), db, frag.getTableId());
      indexColumnElementKey = TableUtil.get().getIndexColumn(getAppName(), db, getTableId());

      if (colOrder.isEmpty()) {
        OrderedColumns orderedDefns = fragment.getColumnDefinitions();
        for (ColumnDefinition cd : orderedDefns.getColumnDefinitions()) {
          if ( cd.isUnitOfRetention() ) {
            colOrder.add(cd.getElementKey());
          }
        }
      }

      header = new String[colOrder.size()];
      spreadsheetIndexToElementKey = new String[colOrder.size()];
      elementKeyToSpreadsheetIndex = new HashMap<String, Integer>();

      for (int i = 0; i < colOrder.size(); ++i) {
        String elementKey = colOrder.get(i);
        String localizedDisplayName;
        localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(Tables.getInstance(),
            getAppName(), db, frag.getTableId(),
            elementKey);

        header[i] = localizedDisplayName;
        spreadsheetIndexToElementKey[i] = elementKey;
        elementKeyToSpreadsheetIndex.put(elementKey, i);
      }
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(frag.getAppName(), db);
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

  public ColorRuleGroup getColumnColorRuleGroup(OdkDbHandle db, String elementKey, String[] adminColumns) throws RemoteException {
    return ColorRuleGroup.getColumnColorRuleGroup(Tables.getInstance(),
        getAppName(), db, getTableId(), elementKey, adminColumns);
  }

  public ColorRuleGroup getStatusColumnRuleGroup(OdkDbHandle db, String[] adminColumns) throws RemoteException {
    return ColorRuleGroup.getStatusColumnRuleGroup(Tables.getInstance(),
        getAppName(), db, getTableId(), adminColumns);
  }

  public ColorRuleGroup getTableColorRuleGroup(OdkDbHandle db, String[] adminColumns) throws RemoteException {
    return ColorRuleGroup.getTableColorRuleGroup(Tables.getInstance(),
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
  };

  public SpreadsheetCell getSpreadsheetCell(Context context, CellInfo cellInfo) {
    SpreadsheetCell cell = new SpreadsheetCell();
    cell.rowNum = cellInfo.rowId;
    cell.row = getRowAtIndex(cellInfo.rowId);
    cell.elementKey = cellInfo.elementKey;
    OrderedColumns orderedDefns = getColumnDefinitions();
    ColumnDefinition cd = orderedDefns.find(cellInfo.elementKey);
    cell.displayText = cell.row.getDisplayTextOfData(cd.getType(), cellInfo.elementKey);
    cell.value = cell.row.getRawDataOrMetadataByElementKey(cellInfo.elementKey);
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
