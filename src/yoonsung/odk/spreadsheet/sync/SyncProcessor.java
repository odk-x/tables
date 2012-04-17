package yoonsung.odk.spreadsheet.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.ColumnProperties.ColumnType;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DataUtil;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Table;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.content.ContentValues;
import android.content.SyncResult;
import android.util.Log;

/**
 * SyncProcessor implements the cloud synchronization logic for Tables.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public class SyncProcessor {

  private static final String TAG = SyncProcessor.class.getSimpleName();

  private final DataUtil du;
  private final DataManager dm;
  private final SyncResult syncResult;
  private final Synchronizer synchronizer;

  public SyncProcessor(Synchronizer synchronizer, DataManager dm, SyncResult syncResult) {
    this.du = DataUtil.getDefaultDataUtil();
    this.dm = dm;
    this.syncResult = syncResult;
    this.synchronizer = synchronizer;
  }

  /**
   * Synchronize all synchronized tables with the cloud.
   */
  public void synchronize() {
    Log.i(TAG, "entered synchronize()");
    TableProperties[] tps = dm.getSynchronizedTableProperties();
    for (TableProperties tp : tps) {
      Log.i(TAG, "synchronizing table " + tp.getDisplayName());
      synchronizeTable(tp);
    }
  }

  /**
   * Synchronize the table represented by the given TableProperties with the
   * cloud. If tp.isSynchronized() == false, returns without doing anything.
   * 
   * @param tp
   *          the table to synchronize
   */
  public void synchronizeTable(TableProperties tp) {
    if (!tp.isSynchronized())
      return;

    DbTable table = dm.getDbTable(tp.getTableId());

    boolean success = false;
    beginTableTransaction(tp);
    try {
      switch (tp.getSyncState()) {
      case SyncUtil.State.INSERTING:
        success = synchronizeTableInserting(tp, table);
        break;
      case SyncUtil.State.UPDATING:
        success = synchronizeTableUpdating(tp, table);
        break;
      case SyncUtil.State.DELETING:
        success = synchronizeTableDeleting(tp, table);
        break;
      case SyncUtil.State.REST:
        success = synchronizeTableRest(tp, table);
        break;
      }
      tp.setLastSyncTime(du.formatNowForDb());
    } finally {
      endTableTransaction(tp, success);
    }
  }

  private boolean synchronizeTableUpdating(TableProperties tp, DbTable table) {
    // TODO: update table properties on server
    return false;
  }

  private boolean synchronizeTableInserting(TableProperties tp, DbTable table) {
    String tableId = tp.getTableId();
    Log.i(TAG, "INSERTING " + tp.getDisplayName());
    Map<String, Integer> columns = getColumns(tp);
    List<SyncRow> rowsToInsert = getRows(table, columns, SyncUtil.State.INSERTING);

    boolean success = false;
    beginRowsTransaction(table, getRowIdsAsArray(rowsToInsert));
    try {
      String syncTag = synchronizer.createTable(tableId, columns);
      tp.setSyncTag(syncTag);
      Modification modification = synchronizer.insertRows(tableId, rowsToInsert);
      updateDbFromModification(modification, table, tp);
      success = true;
    } catch (IOException e) {
      Log.e(TAG, "IOException for table: " + tp.getDisplayName(), e);
      syncResult.stats.numIoExceptions++;
      success = false;
    } catch (Exception e) {
      Log.e(TAG, "Unexpected exception in synchronize inserting on table: " + tp.getDisplayName(),
          e);
      syncResult.stats.numSkippedEntries += rowsToInsert.size();
      success = false;
    } finally {
      endRowsTransaction(table, getRowIdsAsArray(rowsToInsert), success);
    }

    return success;
  }

  private boolean synchronizeTableDeleting(TableProperties tp, DbTable table) {
    String tableId = tp.getTableId();
    Log.i(TAG, "DELETING " + tp.getDisplayName());
    boolean success = false;
    try {
      synchronizer.deleteTable(tableId);
      tp.deleteTableActual();
      syncResult.stats.numDeletes++;
      syncResult.stats.numEntries++;
    } catch (IOException e) {
      Log.e(TAG, "IOException for table: " + tp.getDisplayName(), e);
      syncResult.stats.numIoExceptions++;
      success = false;
    } catch (Exception e) {
      Log.e(TAG, "Unexpected exception in synchronize deleting on table: " + tp.getDisplayName(), e);
      success = false;
    }
    return success;
  }

  private boolean synchronizeTableRest(TableProperties tp, DbTable table) {
    String tableId = tp.getTableId();
    Log.i(TAG, "REST " + tp.getDisplayName());
    Map<String, Integer> columns = getColumns(tp);

    List<SyncRow> rowsToInsert = getRows(table, columns, SyncUtil.State.INSERTING);
    List<SyncRow> rowsToUpdate = getRows(table, columns, SyncUtil.State.UPDATING);
    List<SyncRow> rowsToDelete = getRows(table, columns, SyncUtil.State.DELETING);

    List<SyncRow> allRows = new ArrayList<SyncRow>();
    allRows.addAll(rowsToInsert);
    allRows.addAll(rowsToUpdate);
    allRows.addAll(rowsToDelete);
    String[] rowIds = getRowIdsAsArray(allRows);

    boolean success = false;
    beginRowsTransaction(table, rowIds);
    try {
      updateDbFromServer(tp, table);
      Modification modification = synchronizer.insertRows(tableId, rowsToInsert);
      updateDbFromModification(modification, table, tp);
      modification = synchronizer.updateRows(tableId, rowsToUpdate);
      updateDbFromModification(modification, table, tp);
      String syncTag = synchronizer.deleteRows(tableId, getRowIdsAsList(rowsToDelete));
      tp.setSyncTag(syncTag);
      for (String rowId : getRowIdsAsArray(rowsToDelete)) {
        table.deleteRowActual(rowId);
        syncResult.stats.numDeletes++;
        syncResult.stats.numEntries++;
      }
      success = true;
    } catch (IOException e) {
      Log.e(TAG, "IOException for table: " + tp.getDisplayName(), e);
      syncResult.stats.numIoExceptions++;
      success = false;
    } catch (Exception e) {
      Log.e(TAG, "Unexpected exception in synchronize rest on table: " + tp.getDisplayName(), e);
      success = false;
    } finally {
      if (success)
        allRows.removeAll(rowsToDelete);
      rowIds = getRowIdsAsArray(allRows);
      endRowsTransaction(table, rowIds, success);
    }

    return success;
  }

  private void updateDbFromServer(TableProperties tp, DbTable table) throws IOException {

    IncomingModification modification = synchronizer.getUpdates(tp.getTableId(), tp.getSyncTag());
    List<SyncRow> rows = modification.getRows();
    String newSyncTag = modification.getTableSyncTag();

    Table allRowIds = table.getRaw(new String[] { DbTable.DB_SYNC_STATE }, null, null, null);

    List<SyncRow> rowsToConflict = new ArrayList<SyncRow>();
    List<SyncRow> rowsToUpdate = new ArrayList<SyncRow>();
    List<SyncRow> rowsToInsert = new ArrayList<SyncRow>();
    List<SyncRow> rowsToDelete = new ArrayList<SyncRow>();

    for (SyncRow row : rows) {
      boolean found = false;
      for (int i = 0; i < allRowIds.getHeight(); i++) {
        String rowId = allRowIds.getRowId(i);
        int state = Integer.parseInt(allRowIds.getData(i, 0));
        if (row.getRowId().equals(rowId)) {
          found = true;
          if (state == SyncUtil.State.REST) {
            if (row.isDeleted())
              rowsToDelete.add(row);
            else
              rowsToUpdate.add(row);
          } else {
            rowsToConflict.add(row);
          }
        }
      }
      if (!found)
        rowsToInsert.add(row);
    }

    conflictRowsInDb(table, rowsToConflict);
    updateRowsInDb(table, rowsToUpdate);
    insertRowsInDb(table, rowsToInsert);
    deleteRowsInDb(table, rowsToDelete);

    tp.setSyncTag(newSyncTag);
  }

  private void conflictRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      Log.i(TAG, "conflicting row, id=" + row.getRowId() + " syncTag=" + row.getSyncTag());
    }
    // TODO: how to conflict?
    // for (RowResource row : rowsToConflict) {
    // ContentValues values = new ContentValues();
    //
    // values.put(DbTable.DB_ROW_ID, row.getRowId());
    // values.put(DbTable.DB_SYNC_TAG, row.getRowEtag());
    // values.put(DbTable.DB_SYNC_STATE,
    // String.valueOf(SyncUtil.State.CONFLICTING));
    // values.put(DbTable.DB_TRANSACTIONING,
    // String.valueOf(SyncUtil.Transactioning.FALSE));
    // table.actualUpdateRowByRowId(row.getRowId(), values);
    //
    // for (Entry<String, String> entry : row.getValues().entrySet())
    // values.put(entry.getKey(), entry.getValue());
    //
    // table.actualAddRow(values);
    // syncResult.stats.numConflictDetectedExceptions++;
    // syncResult.stats.numEntries += 2;
    // }
  }

  private void insertRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      ContentValues values = new ContentValues();

      values.put(DbTable.DB_ROW_ID, row.getRowId());
      values.put(DbTable.DB_SYNC_TAG, row.getSyncTag());
      values.put(DbTable.DB_SYNC_STATE, SyncUtil.State.REST);
      values.put(DbTable.DB_TRANSACTIONING, SyncUtil.boolToInt(false));

      for (Entry<String, String> entry : row.getValues().entrySet())
        values.put(entry.getKey(), entry.getValue());

      table.actualAddRow(values);
      syncResult.stats.numInserts++;
      syncResult.stats.numEntries++;
    }
  }

  private void updateRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      ContentValues values = new ContentValues();

      values.put(DbTable.DB_SYNC_TAG, row.getSyncTag());
      values.put(DbTable.DB_SYNC_STATE, String.valueOf(SyncUtil.State.REST));
      values.put(DbTable.DB_TRANSACTIONING, String.valueOf(SyncUtil.boolToInt(false)));

      for (Entry<String, String> entry : row.getValues().entrySet())
        values.put(entry.getKey(), entry.getValue());

      table.actualUpdateRowByRowId(row.getRowId(), values);
      syncResult.stats.numUpdates++;
      syncResult.stats.numEntries++;
    }
  }

  private void deleteRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      table.deleteRowActual(row.getRowId());
      syncResult.stats.numDeletes++;
    }
  }

  private void updateDbFromModification(Modification modification, DbTable table, TableProperties tp) {
    for (Entry<String, String> entry : modification.getSyncTags().entrySet()) {
      ContentValues values = new ContentValues();
      values.put(DbTable.DB_SYNC_TAG, entry.getValue());
      table.actualUpdateRowByRowId(entry.getKey(), values);
    }
    tp.setSyncTag(modification.getTableSyncTag());
  }

  private Map<String, Integer> getColumns(TableProperties tp) {
    Map<String, Integer> columns = new HashMap<String, Integer>();
    ColumnProperties[] userColumns = tp.getColumns();
    for (ColumnProperties colProp : userColumns)
      columns.put(colProp.getColumnDbName(), colProp.getColumnType());
    columns.put(DbTable.DB_SRC_PHONE_NUMBER, ColumnType.PHONE_NUMBER);
    columns.put(DbTable.DB_LAST_MODIFIED_TIME, ColumnType.DATE);
    return columns;
  }

  private List<SyncRow> getRows(DbTable table, Map<String, Integer> columns, int state) {

    Set<String> columnSet = new HashSet<String>(columns.keySet());
    columnSet.add(DbTable.DB_SYNC_TAG);
    String[] columnNames = columnSet.toArray(new String[0]);

    Table rows = table.getRaw(columnNames, new String[] { DbTable.DB_SYNC_STATE,
        DbTable.DB_TRANSACTIONING },
        new String[] { String.valueOf(state), String.valueOf(SyncUtil.boolToInt(false)) }, null);

    List<SyncRow> changedRows = new ArrayList<SyncRow>();
    int numRows = rows.getHeight();
    int numCols = rows.getWidth();
    for (int i = 0; i < numRows; i++) {
      String rowId = rows.getRowId(i);
      String syncTag = null;
      Map<String, String> values = new HashMap<String, String>();
      for (int j = 0; j < numCols; j++) {
        String colName = rows.getHeader(j);
        if (colName.equals(DbTable.DB_SYNC_TAG)) {
          syncTag = rows.getData(i, j);
        } else {
          values.put(colName, rows.getData(i, j));
        }
      }
      SyncRow row = new SyncRow(rowId, syncTag, false, values);
      changedRows.add(row);
    }

    return changedRows;
  }

  private String[] getRowIdsAsArray(List<SyncRow> rows) {
    List<String> rowIdsList = getRowIdsAsList(rows);
    String[] rowIds = new String[rowIdsList.size()];
    for (int i = 0; i < rowIds.length; i++)
      rowIds[i] = rowIdsList.get(i);
    return rowIds;
  }

  private List<String> getRowIdsAsList(List<SyncRow> rows) {
    List<String> rowIdsList = new ArrayList<String>();
    for (SyncRow row : rows) {
      rowIdsList.add(row.getRowId());
    }
    return rowIdsList;
  }

  private void beginTableTransaction(TableProperties tp) {
    tp.setTransactioning(true);
  }

  private void endTableTransaction(TableProperties tp, boolean success) {
    if (success)
      tp.setSyncState(SyncUtil.State.REST);
    tp.setTransactioning(false);
  }

  private void beginRowsTransaction(DbTable table, String[] rowIds) {
    updateRowsTransactioning(table, rowIds, SyncUtil.boolToInt(true));
  }

  private void endRowsTransaction(DbTable table, String[] rowIds, boolean success) {
    if (success)
      updateRowsState(table, rowIds, SyncUtil.State.REST);
    updateRowsTransactioning(table, rowIds, SyncUtil.boolToInt(false));
  }

  private void updateRowsState(DbTable table, String[] rowIds, int state) {
    ContentValues values = new ContentValues();
    values.put(DbTable.DB_SYNC_STATE, state);
    for (String rowId : rowIds) {
      table.actualUpdateRowByRowId(rowId, values);
    }
  }

  private void updateRowsTransactioning(DbTable table, String[] rowIds, int transactioning) {
    ContentValues values = new ContentValues();
    values.put(DbTable.DB_TRANSACTIONING, String.valueOf(transactioning));
    for (String rowId : rowIds) {
      table.actualUpdateRowByRowId(rowId, values);
    }
  }
}
