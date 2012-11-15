/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Table;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.SyncState;

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
    //TableProperties[] tps = dm.getSynchronizedTableProperties();
    // we want this call rather than just the getSynchronizedTableProperties,
    // because we only want to push the default to the server.
    TableProperties[] tps = dm.getTablePropertiesForTablesSetToSync(
        KeyValueStore.Type.SERVER);
    for (TableProperties tp : tps) {
      Log.i(TAG, "synchronizing table " + tp.getDisplayName());
      synchronizeTable(tp);
    }
  }

  /**
   * Synchronize the table represented by the given TableProperties with the
   * cloud. (The following old statement is no longer true. It now only looks
   * at the tables that have synchronized set to true:
   * "If tp.isSynchronized() == false, returns without doing anything".)
   * 
   * @param tp
   *          the table to synchronize
   */
  public void synchronizeTable(TableProperties tp) {
    //if (!tp.isSynchronized())
     // return;

    DbTable table = dm.getDbTable(tp.getTableId());

    boolean success = false;
    beginTableTransaction(tp);
    try {
      switch (tp.getSyncState()) {
      case inserting:
        success = synchronizeTableInserting(tp, table);
        break;
      case deleting:
        success = synchronizeTableDeleting(tp, table);
        break;
      case updating:
        success = synchronizeTableUpdating(tp, table);
        if (success)
          success = synchronizeTableRest(tp, table);
        break;
      case rest:
        success = synchronizeTableRest(tp, table);
        break;
      default:
        Log.e(TAG, "got unrecognized syncstate: " + tp.getSyncState());
      }
      if (success)
        tp.setLastSyncTime(du.formatNowForDb());
    } finally {
      endTableTransaction(tp, success);
    }
  }

  private boolean synchronizeTableUpdating(TableProperties tp, DbTable table) {
    String tableId = tp.getTableId();
    Log.i(TAG, "UPDATING " + tp.getDisplayName());

    boolean success = false;
    try {
      updateDbFromServer(tp, table);
      String syncTag = synchronizer.setTableProperties(tableId, tp.getSyncTag(),
          tp.getDisplayName(), tp.toJson());
      tp.setSyncTag(syncTag);
      success = true;
    } catch (IOException e) {
      ioException("synchronizeTableUpdating", tp, e);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableUpdating", tp, e);
      success = false;
    }

    return success;
  }

  private boolean synchronizeTableInserting(TableProperties tp, DbTable table) {
    String tableId = tp.getTableId();
    Log.i(TAG, "INSERTING " + tp.getDisplayName());
    Map<String, ColumnType> columns = getColumns(tp);
    List<SyncRow> rowsToInsert = getRows(table, columns, SyncUtil.State.INSERTING);

    boolean success = false;
    beginRowsTransaction(table, getRowIdsAsArray(rowsToInsert));
    try {
      String syncTag = synchronizer.createTable(tableId, tp.getDisplayName(), columns, tp.toJson());
      tp.setSyncTag(syncTag);
      Modification modification = synchronizer.insertRows(tableId, tp.getSyncTag(), rowsToInsert);
      updateDbFromModification(modification, table, tp);
      success = true;
    } catch (IOException e) {
      ioException("synchronizeTableInserting", tp, e);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableInserting", tp, e);
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
      ioException("synchronizeTableDeleting", tp, e);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableDeleting", tp, e);
      success = false;
    }
    return success;
  }

  /*
   * I think this is the method that's called when the table is dl'd for the 
   * first time from the server? SS
   */
  private boolean synchronizeTableRest(TableProperties tp, DbTable table) {
    String tableId = tp.getTableId();
    Log.i(TAG, "REST " + tp.getDisplayName());
    Map<String, ColumnType> columns = getColumns(tp);

    // get updates from server
    // if we fail here we don't try to continue
    // (do this first because the updates could affect the state of the rows
    // in the db when we query for them in the next step, e.g. turn an INSERTING
    // row into CONFLICTING)
    try {
      updateDbFromServer(tp, table);
    } catch (IOException e) {
      ioException("synchronizeTableRest", tp, e);
      return false;
    } catch (Exception e) {
      exception("synchronizeTableRest", tp, e);
      return false;
    }

    // get changes that need to be pushed up to server
    List<SyncRow> rowsToInsert = getRows(table, columns, SyncUtil.State.INSERTING);
    List<SyncRow> rowsToUpdate = getRows(table, columns, SyncUtil.State.UPDATING);
    List<SyncRow> rowsToDelete = getRows(table, columns, SyncUtil.State.DELETING);

    List<SyncRow> allRows = new ArrayList<SyncRow>();
    allRows.addAll(rowsToInsert);
    allRows.addAll(rowsToUpdate);
    allRows.addAll(rowsToDelete);
    String[] rowIds = getRowIdsAsArray(allRows);

    // push the changes up to the server
    boolean success = false;
    beginRowsTransaction(table, rowIds);
    try {
      Modification modification = synchronizer.insertRows(tableId, tp.getSyncTag(), rowsToInsert);
      updateDbFromModification(modification, table, tp);
      modification = synchronizer.updateRows(tableId, tp.getSyncTag(), rowsToUpdate);
      updateDbFromModification(modification, table, tp);
      String syncTag = synchronizer.deleteRows(tableId, tp.getSyncTag(),
          getRowIdsAsList(rowsToDelete));
      tp.setSyncTag(syncTag);
      for (String rowId : getRowIdsAsArray(rowsToDelete)) {
        table.deleteRowActual(rowId);
        syncResult.stats.numDeletes++;
        syncResult.stats.numEntries++;
      }
      success = true;
    } catch (IOException e) {
      ioException("synchronizeTableRest", tp, e);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableRest", tp, e);
      success = false;
    } finally {
      if (success)
        allRows.removeAll(rowsToDelete);
      rowIds = getRowIdsAsArray(allRows);
      endRowsTransaction(table, rowIds, success);
    }

    return success;
  }

  private void ioException(String method, TableProperties tp, IOException e) {
    Log.e(TAG, String.format("IOException in %s for table: %s", method, tp.getDisplayName()), e);
    syncResult.stats.numIoExceptions++;
  }

  private void exception(String method, TableProperties tp, Exception e) {
    Log.e(TAG,
        String.format("Unexpected exception in %s on table: %s", method, tp.getDisplayName()), e);
  }

  private void updateDbFromServer(TableProperties tp, DbTable table) throws IOException {

    // retrieve updates
    IncomingModification modification = synchronizer.getUpdates(tp.getTableId(), tp.getSyncTag());
    List<SyncRow> rows = modification.getRows();
    String newSyncTag = modification.getTableSyncTag();
    ArrayList<String> columns = new ArrayList<String>();
    columns.add(DbTable.DB_SYNC_STATE);
    // TODO: confirm handling of rows that have pending/unsaved changes from Collect

    Table allRowIds = table.getRaw(columns, 
    		new String[] {DbTable.DB_SAVED},
            new String[] {DbTable.SavedStatus.COMPLETE.name()}, null);

    // update properties if necessary
    // do this before updating data in case columns have changed
    if (modification.hasTablePropertiesChanged())
      tp.setFromJson(modification.getTableProperties());

    // sort data changes into types
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
      if (!found && !row.isDeleted())
        rowsToInsert.add(row);
    }

    // perform data changes
    conflictRowsInDb(table, rowsToConflict);
    updateRowsInDb(table, rowsToUpdate);
    insertRowsInDb(table, rowsToInsert);
    deleteRowsInDb(table, rowsToDelete);

    tp.setSyncTag(newSyncTag);
  }

  private void conflictRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      Log.i(TAG, "conflicting row, id=" + row.getRowId() + " syncTag=" + row.getSyncTag());
      ContentValues values = new ContentValues();

      // delete conflicting row if it already exists
      String whereClause = String.format("%s = ? AND %s = ? AND %s = ?", DbTable.DB_ROW_ID,
          DbTable.DB_SYNC_STATE, DbTable.DB_TRANSACTIONING);
      String[] whereArgs = { row.getRowId(), String.valueOf(SyncUtil.State.DELETING),
          String.valueOf(SyncUtil.boolToInt(true)) };
      table.deleteRowActual(whereClause, whereArgs);
      
      // update existing row
      values.put(DbTable.DB_ROW_ID, row.getRowId());
      values.put(DbTable.DB_SYNC_STATE, String.valueOf(SyncUtil.State.CONFLICTING));
      values.put(DbTable.DB_TRANSACTIONING, String.valueOf(SyncUtil.boolToInt(false)));
      table.actualUpdateRowByRowId(row.getRowId(), values);

      for (Entry<String, String> entry : row.getValues().entrySet())
        values.put(entry.getKey(), entry.getValue());

      // insert conflicting row
      values.put(DbTable.DB_SYNC_TAG, row.getSyncTag());
      values.put(DbTable.DB_SYNC_STATE, String.valueOf(SyncUtil.State.DELETING));
      values.put(DbTable.DB_TRANSACTIONING, SyncUtil.boolToInt(true));
      table.actualAddRow(values);
      syncResult.stats.numConflictDetectedExceptions++;
      syncResult.stats.numEntries += 2;
    }
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

  private Map<String, ColumnType> getColumns(TableProperties tp) {
    Map<String, ColumnType> columns = new HashMap<String, ColumnType>();
    ColumnProperties[] userColumns = tp.getColumns();
    for (ColumnProperties colProp : userColumns) {
      columns.put(colProp.getColumnDbName(), colProp.getColumnType());
    }
//    columns.put(DbTable.DB_URI_USER, ColumnType.PHONE_NUMBER);
//    columns.put(DbTable.DB_LAST_MODIFIED_TIME, ColumnType.DATETIME);
    columns.putAll(DbTable.getColumnsToSync());
    return columns;
  }

  private List<SyncRow> getRows(DbTable table, Map<String, ColumnType> columns, int state) {

    Set<String> columnSet = new HashSet<String>(columns.keySet());
    columnSet.add(DbTable.DB_SYNC_TAG);
    ArrayList<String> columnNames = new ArrayList<String>();
    for ( String s : columnSet ) {
    	columnNames.add(s);
    }
    // TODO: confirm handling of rows that have pending/unsaved changes from Collect
    Table rows = table.getRaw(columnNames, new String[] {DbTable.DB_SAVED, 
    			DbTable.DB_SYNC_STATE, DbTable.DB_TRANSACTIONING },
        new String[] { DbTable.SavedStatus.COMPLETE.name(), 
    			String.valueOf(state), String.valueOf(SyncUtil.boolToInt(false)) }, null);

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
      tp.setSyncState(SyncState.rest);
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
