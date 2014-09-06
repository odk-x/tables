package org.opendatakit.tables.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.tables.application.Tables;

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

  private final String[] header;
  private final String[] spreadsheetIndexToElementKey;
  private final int[] spreadsheetIndexToUserTableIndexRemap;
  private final Map<String,Integer> elementKeyToSpreadsheetIndex;
  private final UserTable table;

  public SpreadsheetUserTable(UserTable table) {
    this.table = table;
    TableProperties tp = getTableProperties();

    List<String> colOrder = tp.getColumnOrder();
    header = new String[colOrder.size()];
    spreadsheetIndexToUserTableIndexRemap = new int[colOrder.size()];
    spreadsheetIndexToElementKey = new String[colOrder.size()];
    elementKeyToSpreadsheetIndex = new HashMap<String,Integer>();
    for ( int i = 0 ; i < colOrder.size(); ++i ) {
      String elementKey = colOrder.get(i);
      spreadsheetIndexToUserTableIndexRemap[i] = this.table.getColumnIndexOfElementKey(elementKey);
      ColumnProperties cp = tp.getColumnByElementKey(elementKey);
      header[i] = cp.getLocalizedDisplayName();
      spreadsheetIndexToElementKey[i] = elementKey;
      elementKeyToSpreadsheetIndex.put(elementKey, i);
    }
  }

  private TableProperties getTableProperties() {
    TableProperties tp = 
        TableProperties.getTablePropertiesForTable(
            Tables.getInstance().getApplicationContext(), table.getAppName(), table.getTableId());
    return tp;
  }
  
  public KeyValueStoreHelper getKeyValueStoreHelper(String partition) {
    return getTableProperties().getKeyValueStoreHelper(partition);
  }

  public String getTableId() {
    return table.getTableId();
  }

  public String getAppName() {
    return table.getAppName();
  }

  public Map<String, ColumnProperties> getAllColumns() {
    return getTableProperties().getAllColumns();
  }

  public ColorRuleGroup getColumnColorRuleGroup(String elementKey) {
    return ColorRuleGroup.getColumnColorRuleGroup(getTableProperties(), elementKey);
  }

  public ColorRuleGroup getStatusColumnRuleGroup() {
    return ColorRuleGroup.getStatusColumnRuleGroup(getTableProperties());
  }

  public ColorRuleGroup getTableColorRuleGroup() {
    return ColorRuleGroup.getTableColorRuleGroup(getTableProperties());
  }

  int getNumberOfRows() {
    return table.getNumberOfRows();
  }

  public Row getRowAtIndex(int index) {
    return table.getRowAtIndex(index);
  }

  /////////////////////////////////////////////////////////////////////////////
  // Whether or not we have a frozen column...


  public String getIndexedColumnElementKey() {
    return this.getTableProperties().getIndexColumn();
  }

  boolean isIndexed() {
    return getIndexedColumnElementKey() != null && getIndexedColumnElementKey().length() != 0;
  }

  /////////////////////////////////////
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
    ColumnProperties cp = getTableProperties().getColumnByElementKey(cellInfo.elementKey);
    cell.displayText = cell.row.getDisplayTextOfData(context, cp.getColumnType(), cellInfo.elementKey, true);
    cell.value = cell.row.getRawDataOrMetadataByElementKey(cellInfo.elementKey);
    return cell;
  }

  public ColumnProperties getColumnByIndex(int headerCellNum) {
    return getTableProperties().getColumnByElementKey(spreadsheetIndexToElementKey[headerCellNum]);
  }

  public ColumnProperties getColumnByElementKey(String elementKey) {
    return getTableProperties().getColumnByElementKey(elementKey);
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
