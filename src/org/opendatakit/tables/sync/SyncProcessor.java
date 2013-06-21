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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.opendatakit.aggregate.odktables.entity.Column;
import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.aggregate.odktables.entity.api.PropertiesResource;
import org.opendatakit.aggregate.odktables.entity.api.TableDefinitionResource;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.JoinColumn;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.KeyValueStoreSync;
import org.opendatakit.tables.data.SyncState;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.sync.TableResult.Status;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.tables.sync.aggregate.SyncTag;

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

  private static final String LAST_MOD_TIME_LABEL = "last_mod_time";

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.setVisibilityChecker(mapper.getVisibilityChecker()
        .withFieldVisibility(Visibility.ANY));
  }

  private final DataUtil du;
  private final SyncResult syncResult;
  private final Synchronizer synchronizer;
  private final DbHelper dbh;
  /** 
   * The results of the synchronization that we will pass back to the user. 
   * Note that this is NOT the same as the {@link SyncResult} object, which is
   * used to inform the android SyncAdapter how the sync process has gone.
   */
  private final SynchronizationResult mUserResult;

  public SyncProcessor(DbHelper dbh, Synchronizer synchronizer, SyncResult syncResult) {
    this.dbh = dbh;
    this.du = DataUtil.getDefaultDataUtil();
    this.syncResult = syncResult;
    this.synchronizer = synchronizer;
    this.mUserResult = new SynchronizationResult();
  }

  /**
   * Synchronize all synchronized tables with the cloud.
   */
  public SynchronizationResult synchronize() {
    Log.i(TAG, "entered synchronize()");
    //TableProperties[] tps = dm.getSynchronizedTableProperties();
    // we want this call rather than just the getSynchronizedTableProperties,
    // because we only want to push the default to the server.
    TableProperties[] tps = TableProperties.getTablePropertiesForSynchronizedTables(dbh,
        KeyValueStore.Type.SERVER);
    for (TableProperties tp : tps) {
      Log.i(TAG, "synchronizing table " + tp.getDisplayName());
      synchronizeTable(tp, false);
    }
    return mUserResult;
  }

  /**
   * Synchronize the table represented by the given TableProperties with the
   * cloud.
   * <p>
   * Note that if the db changes under you when calling this method, the tp
   * parameter will become out of date. It should be refreshed after calling
   * this method.
   *
   * @param tp
   *          the table to synchronize
   * @param downloadingTable
   *          flag saying whether or not the table is being downloaded for the
   *          first time. Only applies to tables have their sync state set to
   *          {@link SyncState#rest}.
   */
  public void synchronizeTable(TableProperties tp, boolean downloadingTable) {
    DbTable table = DbTable.getDbTable(dbh,
        TableProperties.getTablePropertiesForTable(dbh, tp.getTableId(),
            KeyValueStore.Type.ACTIVE));// TODO: should this be SERVER or ACTIVE?
    boolean success = false;
    // Prepare the tableResult. We'll start it as failure, and only update it
    // if we're successful at the end.
    TableResult tableResult = new TableResult(tp.getDbTableName(), 
        TableResult.Status.FAILURE);
    beginTableTransaction(tp);
    try {
      switch (tp.getSyncState()) {
      case inserting:
        success = synchronizeTableInserting(tp, table, tableResult);
        break;
      case deleting:
        success = synchronizeTableDeleting(tp, table, tableResult);
        break;
      case updating:
        success = synchronizeTableUpdating(tp, table, tableResult);
        if (success)
          success = synchronizeTableRest(tp, table, false, tableResult);
        break;
      case rest:
        success = synchronizeTableRest(tp, table, downloadingTable, 
            tableResult);
        break;
      default:
        Log.e(TAG, "got unrecognized syncstate: " + tp.getSyncState());
      }
      // It is possible the table properties changed. Refresh just in case.
      tp = TableProperties.getTablePropertiesForTable(dbh, tp.getTableId(),
          tp.getBackingStoreType());
      if (success && tp != null) // null in case we deleted the tp.
        tp.setLastSyncTime(du.formatNowForDb());
    } finally {
      endTableTransaction(tp, success);
      // Here we also want to add the TableResult to the value.
      if (success) {
        // Then we should have updated the db and shouldn't have set the 
        // TableResult to be exception.
        if (tableResult.getStatus() == Status.EXCEPTION) {
          Log.e(TAG, "tableResult status for table: " + tp.getDbTableName() +
              " was EXCEPTION, and yet success returned true. This shouldn't" +
              " be possible.");
        } else {
          tableResult.setStatus(Status.SUCCESS);
        }
      }
      mUserResult.addTableResult(tableResult);
    }
  }

  private boolean synchronizeTableUpdating(TableProperties tp, DbTable table,
      TableResult tableResult) {
    String tableId = tp.getTableId();
    Log.i(TAG, "UPDATING " + tp.getDisplayName());
//    // We want to remember the state we were at before the update. If the
//    // properties etag is different it means we had changes of our own to give
//    // to the server, so we'll have to send after we receive. If you don't do
//    // this you get into the situation where your synctag is overwritten with
//    String syncTagStr = tp.getSyncTag();
//    SyncTag syncTagBeforeUpdate = null;
//    if (syncTagStr != null) {
//      syncTagBeforeUpdate = SyncTag.valueOf(syncTagStr);
//    }

    boolean success = false;
    try {
      updateDbFromServer(tp, table, false);
      // update the tp.
      tp = TableProperties.getTablePropertiesForTable(dbh, tp.getTableId(),
          KeyValueStore.Type.SERVER);
      String syncTag = synchronizer.setTableProperties(tableId,
          tp.getSyncTag(), tp.getTableKey(), getAllKVSEntries(tableId,
              KeyValueStore.Type.SERVER));
      tp.setSyncTag(syncTag);
      success = true;
    } catch (IOException e) {
      ioException("synchronizeTableUpdating", tp, e, tableResult);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableUpdating", tp, e, tableResult);
      success = false;
    }

    return success;
  }

  private boolean synchronizeTableInserting(TableProperties tp,
      DbTable table, TableResult tableResult) {
    String tableId = tp.getTableId();
    Log.i(TAG, "INSERTING " + tp.getDisplayName());
//    Map<String, ColumnType> columns = getColumns(tp);
    List<String> userColumns = tp.getColumnOrder();
    List<SyncRow> rowsToInsert = getRows(table, userColumns,
        SyncUtil.State.INSERTING);

    boolean success = false;
    beginRowsTransaction(table, getRowIdsAsArray(rowsToInsert));
    try {
      // First create the table definition on the server.
      String syncTag = synchronizer.createTable(tableId,
          getColumnsForTable(tp), tp.getTableKey(), tp.getDbTableName(),
          SyncUtil.transformClientTableType(tp.getTableType()),
          tp.getAccessControls());
      // now create the TableProperties on the server.
      List<OdkTablesKeyValueStoreEntry> kvsEntries =
          getAllKVSEntries(tp.getTableId(), KeyValueStore.Type.SERVER);
      String syncTagProperties = synchronizer.setTableProperties(
          tp.getTableId(), syncTag, tp.getTableKey(), kvsEntries);
      tp.setSyncTag(syncTagProperties);
      Modification modification = synchronizer.insertRows(tableId, tp.getSyncTag(), rowsToInsert);
      updateDbFromModification(modification, table, tp);
      success = true;
    } catch (IOException e) {
      ioException("synchronizeTableInserting", tp, e, tableResult);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableInserting", tp, e, tableResult);
      syncResult.stats.numSkippedEntries += rowsToInsert.size();
      success = false;
    } finally {
      endRowsTransaction(table, getRowIdsAsArray(rowsToInsert), success);
    }

    return success;
  }

  private boolean synchronizeTableDeleting(TableProperties tp, DbTable table,
      TableResult tableResult) {
    String tableId = tp.getTableId();
    Log.i(TAG, "DELETING " + tp.getDisplayName());
    boolean success = false;
    try {
      synchronizer.deleteTable(tableId);
      tp.deleteTableActual();
      syncResult.stats.numDeletes++;
      syncResult.stats.numEntries++;
    } catch (IOException e) {
      ioException("synchronizeTableDeleting", tp, e, tableResult);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableDeleting", tp, e, tableResult);
      success = false;
    }
    return success;
  }

  /**
   * This method is eventually called when a table is first downloaded from the
   * server.
   * <p>
   * Note that WHENEVER this method is called, if updates to the key value
   * store or TableDefinition have been made, the tp parameter will become
   * out of date. Therefore after calling this method, the caller should
   * refresh their TableProperties.
   * @param tp
   * @param table
   * @param downloadingTable
   * @return
   */
  private boolean synchronizeTableRest(TableProperties tp, DbTable table,
      boolean downloadingTable, TableResult tableResult) {
    String tableId = tp.getTableId();
    Log.i(TAG, "REST " + tp.getDisplayName());
//    Map<String, ColumnType> columns = getColumns(tp);
    List<String> userColumns = tp.getColumnOrder();

    // get updates from server
    // if we fail here we don't try to continue
    // (do this first because the updates could affect the state of the rows
    // in the db when we query for them in the next step, e.g. turn an INSERTING
    // row into CONFLICTING)
    try {
      updateDbFromServer(tp, table, downloadingTable);
    } catch (IOException e) {
      ioException("synchronizeTableRest", tp, e, tableResult);
      return false;
    } catch (Exception e) {
      exception("synchronizeTableRest", tp, e, tableResult);
      return false;
    }
    // refresh the tp
    tp = TableProperties.getTablePropertiesForTable(dbh, tp.getTableId(),
        tp.getBackingStoreType());

    // get changes that need to be pushed up to server
    List<SyncRow> rowsToInsert = getRows(table, userColumns, SyncUtil.State.INSERTING);
    List<SyncRow> rowsToUpdate = getRows(table, userColumns, SyncUtil.State.UPDATING);
    List<SyncRow> rowsToDelete = getRows(table, userColumns, SyncUtil.State.DELETING);

    List<SyncRow> allRows = new ArrayList<SyncRow>();
    allRows.addAll(rowsToInsert);
    allRows.addAll(rowsToUpdate);
    allRows.addAll(rowsToDelete);
    String[] rowIds = getRowIdsAsArray(allRows);

    // push the changes up to the server
    boolean success = false;
    beginRowsTransaction(table, rowIds);
    try {
      Modification modification = synchronizer.insertRows(tableId,
          tp.getSyncTag(), rowsToInsert);
      updateDbFromModification(modification, table, tp);
      modification = synchronizer.updateRows(tableId, tp.getSyncTag(),
          rowsToUpdate);
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
      ioException("synchronizeTableRest", tp, e, tableResult);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableRest", tp, e, tableResult);
      success = false;
    } finally {
      if (success)
        allRows.removeAll(rowsToDelete);
      rowIds = getRowIdsAsArray(allRows);
      endRowsTransaction(table, rowIds, success);
    }

    return success;
  }

  private void ioException(String method, TableProperties tp, IOException e,
      TableResult tableResult) {
    Log.e(TAG, String.format("IOException in %s for table: %s", method, 
        tp.getDisplayName()), e);
    tableResult.setStatus(Status.EXCEPTION);
    tableResult.setMessage(e.getMessage());
    syncResult.stats.numIoExceptions++;
  }

  private void exception(String method, TableProperties tp, Exception e,
      TableResult tableResult) {
    Log.e(TAG,
        String.format("Unexpected exception in %s on table: %s", method, 
            tp.getDisplayName()), e);
    tableResult.setStatus(Status.EXCEPTION);
    tableResult.setMessage(e.getMessage());
  }

  /**
   * Update the database based on the server.
   * @param tp modified in place if the table properties are changed
   * @param table
   * @param downloadingTable whether or not the table is being downloaded for
   * the first time.
   * @throws IOException
   */
  private void updateDbFromServer(TableProperties tp, DbTable table,
      boolean downloadingTable) throws IOException {

    // retrieve updates
    IncomingModification modification = synchronizer.getUpdates(
        tp.getTableId(), tp.getSyncTag());
    List<SyncRow> rows = modification.getRows();
    String newSyncTag = modification.getTableSyncTag();
//    ArrayList<String> columns = new ArrayList<String>();
    List<String> columns = tp.getColumnOrder();
    // TODO: confirm handling of rows that have pending/unsaved changes from Collect

    UserTable allRowIds = table.getRaw(columns,
    		new String[] {DataTableColumns.SAVED},
            new String[] {DbTable.SavedStatus.COMPLETE.name()}, null);

    // update properties if necessary
    // do this before updating data in case columns have changed
    if (modification.hasTablePropertiesChanged()) {
      // We have two things to worry about. One is if the table definition
      // (i.e. the table datastructure or the table's columns' datastructure)
      // has changed. The other is if the key value store has changed.
      TableDefinitionResource definitionResource =
          modification.getTableDefinitionResource();
      PropertiesResource propertiesResource =
          modification.getTableProperties();
      if (downloadingTable) {
        // The table is being downloaded for the first time. We first must
        // delete the dummy table we'd created and then add the new table.
        Log.w(TAG, "table: " + tp.getDisplayName() + " is being downloaded " +
            "for the first time. deleting place holder table.");
        tp.deleteTableActual();
        // update the tp
        // SHOULD THIS METHOD ACTUALLY SET THE SYNC TAG?!? IT SHOWS SETS
        // AT THE END OF THIS METHOD.
        tp = addTableFromDefinitionResource(definitionResource, newSyncTag);
      } else {
        Log.w(TAG, "database properties have changed. " +
            "structural modifications are not allowed. if structure needs" +
            " to be updated, it is not happening.");
      }
      // We only want to update the properties if we haven't made local
      // changes. Otherwise we'd overwrite our local ones and would never be
      // able to put new ones on the server. We'll do this based on the long.
      // If the server was udated more recently in time, we'll take theirs.
      // Otherwise, we'll give the server ours. This isn't a perfect system.
      long serverPropertiesTagTime =
          Long.parseLong(SyncTag.valueOf(newSyncTag).getPropertiesEtag());
      long localPropertiesTagTime =
          Long.parseLong(SyncTag.valueOf(tp.getSyncTag()).getPropertiesEtag());
      if (serverPropertiesTagTime > localPropertiesTagTime
          || downloadingTable) {
        resetKVSForPropertiesResource(tp, propertiesResource);
      } else {
        // We do nothing.
        Log.i(TAG, "the local properties etag was greater than the server's, "
        		+ "so the properties are not being pulled from the server.");
      }

    }

    // sort data changes into types
    List<SyncRow> rowsToConflict = new ArrayList<SyncRow>();
    List<SyncRow> rowsToUpdate = new ArrayList<SyncRow>();
    List<SyncRow> rowsToInsert = new ArrayList<SyncRow>();
    List<SyncRow> rowsToDelete = new ArrayList<SyncRow>();

    for (SyncRow row : rows) {
      boolean found = false;
      for (int i = 0; i < allRowIds.getHeight(); i++) {
        String rowId = allRowIds.getRowId(i);
        String stateStr = allRowIds.getMetadataByElementKey(i, 
            DataTableColumns.SYNC_STATE);
        int state = Integer.parseInt(stateStr);
// TODO: a parse exception here was throwing an exception and silently
// failing. Caused by line shown below.
//        int state = Integer.parseInt(allRowIds.getData(i, 0));
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

    // We have to set this synctag here so that the server knows we saw its
    // changes. Otherwise it won't let us put up new information.
    tp.setSyncTag(newSyncTag);
  }

  private void conflictRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      Log.i(TAG, "conflicting row, id=" + row.getRowId() + " syncTag=" + row.getSyncTag());
      ContentValues values = new ContentValues();

      // delete conflicting row if it already exists
      String whereClause = String.format("%s = ? AND %s = ? AND %s = ?", DataTableColumns.ROW_ID,
          DataTableColumns.SYNC_STATE, DataTableColumns.TRANSACTIONING);
      String[] whereArgs = { row.getRowId(), String.valueOf(SyncUtil.State.DELETING),
          String.valueOf(SyncUtil.boolToInt(true)) };
      table.deleteRowActual(whereClause, whereArgs);

      // update existing row
      values.put(DataTableColumns.ROW_ID, row.getRowId());
      values.put(DataTableColumns.SYNC_STATE, String.valueOf(SyncUtil.State.CONFLICTING));
      values.put(DataTableColumns.TRANSACTIONING, String.valueOf(SyncUtil.boolToInt(false)));
      table.actualUpdateRowByRowId(row.getRowId(), values);

      for (Entry<String, String> entry : row.getValues().entrySet()) {
      	String colName = entry.getKey();
      	if ( colName.equals(LAST_MOD_TIME_LABEL)) {
      		String lastModTime = entry.getValue();
      		DateTime dt = du.parseDateTimeFromDb(lastModTime);
      		values.put(DataTableColumns.TIMESTAMP, Long.toString(dt.getMillis()));
      	} else {
      		values.put(colName, entry.getValue());
      	}
      }

      // insert conflicting row
      values.put(DataTableColumns.SYNC_TAG, row.getSyncTag());
      values.put(DataTableColumns.SYNC_STATE, String.valueOf(SyncUtil.State.DELETING));
      values.put(DataTableColumns.TRANSACTIONING, SyncUtil.boolToInt(true));
      table.actualAddRow(values);
      syncResult.stats.numConflictDetectedExceptions++;
      syncResult.stats.numEntries += 2;
    }
  }

  private void insertRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      ContentValues values = new ContentValues();

      values.put(DataTableColumns.ROW_ID, row.getRowId());
      values.put(DataTableColumns.SYNC_TAG, row.getSyncTag());
      values.put(DataTableColumns.SYNC_STATE, SyncUtil.State.REST);
      values.put(DataTableColumns.TRANSACTIONING, SyncUtil.boolToInt(false));

      for (Entry<String, String> entry : row.getValues().entrySet()) {
      	String colName = entry.getKey();
      	if ( colName.equals(LAST_MOD_TIME_LABEL)) {
      		String lastModTime = entry.getValue();
      		DateTime dt = du.parseDateTimeFromDb(lastModTime);
      		values.put(DataTableColumns.TIMESTAMP, Long.toString(dt.getMillis()));
      	} else {
      		values.put(colName, entry.getValue());
      	}
      }

      table.actualAddRow(values);
      syncResult.stats.numInserts++;
      syncResult.stats.numEntries++;
    }
  }

  private void updateRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      ContentValues values = new ContentValues();

      values.put(DataTableColumns.SYNC_TAG, row.getSyncTag());
      values.put(DataTableColumns.SYNC_STATE, String.valueOf(SyncUtil.State.REST));
      values.put(DataTableColumns.TRANSACTIONING, String.valueOf(SyncUtil.boolToInt(false)));

      for (Entry<String, String> entry : row.getValues().entrySet()) {
    	String colName = entry.getKey();
    	if ( colName.equals(LAST_MOD_TIME_LABEL)) {
    		String lastModTime = entry.getValue();
    		DateTime dt = du.parseDateTimeFromDb(lastModTime);
    		values.put(DataTableColumns.TIMESTAMP, Long.toString(dt.getMillis()));
    	} else {
    		values.put(colName, entry.getValue());
    	}
      }

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
      values.put(DataTableColumns.SYNC_TAG, entry.getValue());
      table.actualUpdateRowByRowId(entry.getKey(), values);
    }
    tp.setSyncTag(modification.getTableSyncTag());
  }

  /**
   * Returns all the columns that should be synched, including metadata
   * columns. Returns as a map of element key to {@link ColumnType}.
   * @param tp
   * @return
   */
  private Map<String, ColumnType> getColumns(TableProperties tp) {
    Map<String, ColumnType> columns = new HashMap<String, ColumnType>();
    for (ColumnProperties colProp : tp.getColumns().values()) {
      columns.put(colProp.getElementKey(), colProp.getColumnType());
    }
    columns.putAll(DbTable.getColumnsToSync());
    return columns;
  }

  /**
   * Get the sync rows for the user-defined rows specified by the elementkeys
   * of columnsToSync. Returns a list of {@link SyncRow} objects. The rows
   * returned will be only those whose sync state matches the state parameter.
   * The metadata columns that should be synched are also included in the
   * returned {@link SyncRow}s.
   * @param table
   * @param columnsToSync the element keys of the user-defined columns to sync.
   * Should likely be all of them.
   * @param state the query of the rows in question. Eg inserting will return
   * only those rows whose sync state is inserting.
   * @return
   */
  private List<SyncRow> getRows(DbTable table, List<String> columnsToSync,
      int state) {

//    Set<String> columnSet = new HashSet<String>(columns.keySet());
//    columnSet.add(DataTableColumns.SYNC_TAG);
//    ArrayList<String> columnNames = new ArrayList<String>();
//    for ( String s : columnsToSync ) {
//    	columnNames.add(s);
//    }
    // TODO: confirm handling of rows that have pending/unsaved changes from Collect
    UserTable rows = table.getRaw(columnsToSync, new String[]
        {DataTableColumns.SAVED,
    			DataTableColumns.SYNC_STATE, DataTableColumns.TRANSACTIONING },
        new String[] { DbTable.SavedStatus.COMPLETE.name(),
    			String.valueOf(state), String.valueOf(SyncUtil.boolToInt(false)) },
    			null);

    List<SyncRow> changedRows = new ArrayList<SyncRow>();
    int numRows = rows.getHeight();
    int numCols = rows.getWidth();
    if (numCols != columnsToSync.size()) {
      Log.e(TAG, "number of user-defined columns returned in getRows() does " +
      		"not equal the number of user-defined element keys requested (" +
      		numCols + " != " + columnsToSync.size() + ")");
    }
    // And now for each row we need to add both the user columns AND the
    // columns to sync, AND the sync tag for the row.
    Map<String, ColumnType> cachedColumnsToSync = DbTable.getColumnsToSync();
    for (int i = 0; i < numRows; i++) {
      String rowId = rows.getRowId(i);
      String syncTag = rows.getMetadataByElementKey(i, DataTableColumns.SYNC_TAG);
      Map<String, String> values = new HashMap<String, String>();

      // precompute the correspondence between the displayed elementKeys and the UserTable userData index
      int[] userDataIndex = new int[numCols];
      for ( int j = 0 ; j < numCols ; ++j ) {
        Integer idx = rows.getColumnIndexOfElementKey(columnsToSync.get(j));
        userDataIndex[j] = (idx == null) ? -1 : idx;
      }

      for (int j = 0; j < numCols; j++) {
        // We know that the columnsToSync should be metadata keys for the user-
        // defined columns. If they're not present we know there is a problem,
        String columnElementKey = columnsToSync.get(j);
        values.put(columnElementKey, rows.getData(i, userDataIndex[j]));
          // And now add the necessary metadata. This will be based on the
          // columns specified as synchable metadata columns in DbTable.
        for (String metadataElementKey : cachedColumnsToSync.keySet()) {
          // Special check for the timestamp to format correctly.
          if (metadataElementKey.equals(DataTableColumns.TIMESTAMP)) {
            Long timestamp = Long.valueOf(rows.getMetadataByElementKey(
                i, DataTableColumns.TIMESTAMP));
            DateTime dt = new DateTime(timestamp);
            String lastModTime = du.formatDateTimeForDb(dt);
            values.put(LAST_MOD_TIME_LABEL, lastModTime);
          } else {
            values.put(metadataElementKey,
               rows.getMetadataByElementKey(i, metadataElementKey));
          }
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
    if (success && tp != null) // might be null if table was deleted.
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
    values.put(DataTableColumns.SYNC_STATE, state);
    for (String rowId : rowIds) {
      table.actualUpdateRowByRowId(rowId, values);
    }
  }

  private void updateRowsTransactioning(DbTable table, String[] rowIds, int transactioning) {
    ContentValues values = new ContentValues();
    values.put(DataTableColumns.TRANSACTIONING, String.valueOf(transactioning));
    for (String rowId : rowIds) {
      table.actualUpdateRowByRowId(rowId, values);
    }
  }

  /**
   * Update the database to reflect the new structure.
   * <p>
   * This should be called when downloading a table from the server, which is
   * why the syncTag is separate.
   * TODO: pass the db around rather than dbh so we can do this transactionally
   * @param definitionResource
   * @param syncTag the syncTag belonging to the modification from which you
   * acquired the {@link TableDefinitionResource}.
   * @return the new {@link TableProperties} for the table.
   */
  private TableProperties addTableFromDefinitionResource(
      TableDefinitionResource definitionResource, String syncTag) {
    KeyValueStore.Type kvsType = KeyValueStore.Type.SERVER;
    TableProperties tp = TableProperties.addTable(dbh,
        definitionResource.getTableKey(),
        definitionResource.getDbTableName(),
        definitionResource.getTableKey(),
        SyncUtil.transformServerTableType(definitionResource.getType()),
        definitionResource.getTableId(),
        kvsType);
    for (Column col : definitionResource.getColumns()) {
      // TODO: We aren't handling types correctly here. Need to have a mapping
      // on the server as well so that you can pull down the right thing.
      // TODO: add an addcolumn method to allow setting all of the dbdefinition
      // fields.
      tp.addColumn(col.getElementKey(), col.getElementKey(),
          col.getElementName(), SyncUtil.
          getTablesColumnTypeFromServerColumnType(col.getElementType()),
          col.getListChildElementKeys(),
          SyncUtil.intToBool(col.getIsPersisted()), col.getJoins());
    }
    // Refresh the table properties to get the columns.
    tp = TableProperties.getTablePropertiesForTable(dbh,
        definitionResource.getTableId(), kvsType);
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVS = kvsm.getSyncStoreForTable(
        definitionResource.getTableId());
    syncKVS.setIsSetToSync(true);
    tp.setSyncState(SyncState.rest);
    tp.setSyncTag(syncTag);
    return tp;
  }

  /**
   * Wipe all of the existing key value entries in the server kvs and replace
   * them with the entries in the {@link PropertiesResource}.
   * @param tp
   * @param propertiesResource
   */
  private void resetKVSForPropertiesResource(TableProperties tp,
      PropertiesResource propertiesResource) {
    KeyValueStore.Type kvsType = KeyValueStore.Type.SERVER;
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore kvs = kvsm.getStoreForTable(tp.getTableId(), kvsType);
    kvs.clearKeyValuePairs(dbh.getWritableDatabase());
    kvs.addEntriesToStore(dbh.getWritableDatabase(),
        propertiesResource.getKeyValueStoreEntries());
  }

  /**
   * Return a list of all the entries in the key value store for the given
   * table and type.
   * @param tableId
   * @param typeOfStore
   * @return
   */
  private List<OdkTablesKeyValueStoreEntry> getAllKVSEntries(String tableId,
      KeyValueStore.Type typeOfStore) {
    KeyValueStore kvs = KeyValueStoreManager.getKVSManager(dbh)
        .getStoreForTable(tableId, typeOfStore);
    List<OdkTablesKeyValueStoreEntry> allEntries =
        kvs.getEntries(dbh.getReadableDatabase());
    return allEntries;
  }

  /**
   * Return a list of {@link Column} objects (representing the column
   * definition) for each of the columns associated with this table.
   * @param tp
   * @return
   */
  private List<Column> getColumnsForTable(TableProperties tp) {
    List<Column> columns = new ArrayList<Column>();
    for (ColumnProperties cp : tp.getColumns().values()) {
      String elementKey = cp.getElementKey();
      String elementName = cp.getElementName();
      ColumnType colType = cp.getColumnType();
      List<String> listChildrenElements =
          cp.getListChildElementKeys();
      int isPersisted = SyncUtil.boolToInt(cp.isPersisted());
      JoinColumn joins = cp.getJoins();
      String listChildElementKeysStr = null;
      String joinsStr = null;
      try {
        listChildElementKeysStr =
            mapper.writeValueAsString(listChildrenElements);
        joinsStr = mapper.writeValueAsString(joins);
      } catch (JsonGenerationException e) {
        Log.e(TAG, "problem parsing json list entry during sync");
        e.printStackTrace();
      } catch (JsonMappingException e) {
        Log.e(TAG, "problem mapping json list entry during sync");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e(TAG, "i/o exception with json list entry during sync");
        e.printStackTrace();
      }
      Column c = new Column(tp.getTableId(), elementKey, elementName,
          AggregateSynchronizer.types.get(colType), listChildElementKeysStr,
          isPersisted, joinsStr);
      columns.add(c);
    }
    return columns;
  }
}
