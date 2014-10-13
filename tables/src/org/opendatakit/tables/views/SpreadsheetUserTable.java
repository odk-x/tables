package org.opendatakit.tables.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.fragments.AbsTableDisplayFragment;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

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
  private final UserTable table;
  private final String[] header;
  private final String[] spreadsheetIndexToElementKey;
  private final int[] spreadsheetIndexToUserTableIndexRemap;
  private final Map<String, Integer> elementKeyToSpreadsheetIndex;

  public SpreadsheetUserTable(AbsTableDisplayFragment frag, UserTable table) {
    this.fragment = frag;
    this.table = table;
    Context context = fragment.getActivity();

    ArrayList<String> colOrder;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(context, table.getAppName());
      colOrder = TableUtil.get().getColumnOrder(db, table.getTableId());
    } finally {
      if ( db != null ) {
        db.close();
      }
    }

    if (colOrder.isEmpty()) {
      ArrayList<ColumnDefinition> orderedDefns = fragment.getColumnDefinitions();
      for (ColumnDefinition cd : orderedDefns) {
        if ( cd.isUnitOfRetention() ) {
          colOrder.add(cd.getElementKey());
        }
      }
    }

    header = new String[colOrder.size()];
    spreadsheetIndexToUserTableIndexRemap = new int[colOrder.size()];
    spreadsheetIndexToElementKey = new String[colOrder.size()];
    elementKeyToSpreadsheetIndex = new HashMap<String, Integer>();
    db = null;
    try {
      db = DatabaseFactory.get().getDatabase(context, table.getAppName());
      for (int i = 0; i < colOrder.size(); ++i) {
        String elementKey = colOrder.get(i);
        spreadsheetIndexToUserTableIndexRemap[i] = this.table
            .getColumnIndexOfElementKey(elementKey);

        String localizedDisplayName;
        localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(db, table.getTableId(),
            elementKey);

        header[i] = localizedDisplayName;
        spreadsheetIndexToElementKey[i] = elementKey;
        elementKeyToSpreadsheetIndex.put(elementKey, i);
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public String getTableId() {
    return table.getTableId();
  }

  public String getAppName() {
    return table.getAppName();
  }

  public ArrayList<ColumnDefinition> getColumnDefinitions() {
    return fragment.getColumnDefinitions();
  }

  public ColorRuleGroup getColumnColorRuleGroup(String elementKey) {
    return ColorRuleGroup.getColumnColorRuleGroup(fragment.getActivity(),
        getAppName(), getTableId(), elementKey);
  }

  public ColorRuleGroup getStatusColumnRuleGroup() {
    return ColorRuleGroup.getStatusColumnRuleGroup(fragment.getActivity(),
        getAppName(), getTableId());
  }

  public ColorRuleGroup getTableColorRuleGroup() {
    return ColorRuleGroup.getTableColorRuleGroup(fragment.getActivity(),
        getAppName(), getTableId());
  }

  int getNumberOfRows() {
    return table.getNumberOfRows();
  }

  public Row getRowAtIndex(int index) {
    return table.getRowAtIndex(index);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Whether or not we have a frozen column...

  public String getIndexedColumnElementKey() {
    String indexColumn;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(fragment.getActivity(), 
          getAppName());
      indexColumn = TableUtil.get().getIndexColumn(db, getTableId());
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    return indexColumn;
  }

  boolean isIndexed() {
    return getIndexedColumnElementKey() != null && getIndexedColumnElementKey().length() != 0;
  }

  // ///////////////////////////////////
  // These need to be re-worked...

  public boolean hasData() {
    return !(table == null || (spreadsheetIndexToUserTableIndexRemap.length == 0));
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
    ArrayList<ColumnDefinition> orderedDefns = getColumnDefinitions();
    ColumnDefinition cd = ColumnDefinition.find(orderedDefns, cellInfo.elementKey);
    cell.displayText = cell.row.getDisplayTextOfData(context, cd.getType(), cellInfo.elementKey,
        true);
    cell.value = cell.row.getRawDataOrMetadataByElementKey(cellInfo.elementKey);
    return cell;
  }

  public ColumnDefinition getColumnByIndex(int headerCellNum) {
    return getColumnByElementKey(spreadsheetIndexToElementKey[headerCellNum]);
  }

  public ColumnDefinition getColumnByElementKey(String elementKey) {
    ArrayList<ColumnDefinition> orderedDefns = getColumnDefinitions();
    return ColumnDefinition.find(orderedDefns, elementKey);
  }

  public int getWidth() {
    return spreadsheetIndexToUserTableIndexRemap.length;
  }

  Integer getColumnIndexOfElementKey(String elementKey) {
    return elementKeyToSpreadsheetIndex.get(elementKey);
  }

  public int getNumberOfDisplayColumns() {
    return spreadsheetIndexToUserTableIndexRemap.length;
  }

  String getHeader(int colNum) {
    return header[colNum];
  }
}
