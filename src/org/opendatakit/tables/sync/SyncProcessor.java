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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.aggregate.odktables.rest.entity.PropertiesResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.KeyValueStoreSync;
import org.opendatakit.tables.data.SyncState;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.data.UserTable.Row;
import org.opendatakit.tables.sync.TableResult.Status;

import android.content.ContentValues;
import android.content.SyncResult;
import android.util.Log;

/**
 * SyncProcessor implements the cloud synchronization logic for Tables.
 *
 * @author the.dylan.price@gmail.com
 * @author sudar.sam@gmail.com
 *
 */
public class SyncProcessor {

  private static final String TAG = SyncProcessor.class.getSimpleName();

  private static final String MSG_DELETED_LOCAL_TABLE = "Deleted Local Table";

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

  public SyncProcessor(DbHelper dbh, Synchronizer synchronizer,
      SyncResult syncResult) {
    this.dbh = dbh;
    this.du = DataUtil.getDefaultDataUtil();
    this.syncResult = syncResult;
    this.synchronizer = synchronizer;
    this.mUserResult = new SynchronizationResult();
  }

  /**
   * Synchronize all synchronized tables with the cloud.
   * <p>
   * This becomes more complicated with the ability to synchronize files. The
   * new order is as follows:
   * <p>
   * 1) Synchronize app-level files. (i.e. those files under the appid directory
   * that are NOT then under the tables, instances, metadata, or logging
   * directories.)
   * This is a multi-part process:
   * a) Get the app-level manifest, download any
   * files that have changed (differing hashes) or that do not exist.
   * b) Upload the files that you have that are not on the manifest. Note that
   * this could be suppressed if the user does not have appropriate
   * permissions.
   * <p>
   * 2) Synchronize the static table files for those tables that are set to
   * sync. (i.e. those files under "appid/tables/tableid"). This follows the
   * same multi-part steps of 1a and 1b.
   * 3) Synchronize the table properties/metadata.
   * 4) Synchronize the table data. This includes the data in the db as well as
   * those files under "appid/instances/tableid". This file synchronization
   * follows the same multi-part steps of 1a and 1b.
   * TODO: step four--the synchronization of instances files--should perhaps
   * also be allowed to be modular and permit things like ODK Submit to handle
   * data and files separately.
   * <p>
   * TODO: This should also somehow account for zipped files, exploding them or
   * what have you.
   */
  public SynchronizationResult synchronize() {
    Log.i(TAG, "entered synchronize()");
    // First we're going to synchronize the app level files.
    try {
      synchronizer.syncAppLevelFiles(true);
    } catch (IOException e) {
      // TODO: update a synchronization result to report back to them as well.
      Log.e(TAG, "[synchronize] error trying to synchronize app-level files.");
      e.printStackTrace();
    }
    //TableProperties[] tps = dm.getSynchronizedTableProperties();
    // we want this call rather than just the getSynchronizedTableProperties,
    // because we only want to push the default to the server.
    TableProperties[] tps =
        TableProperties.getTablePropertiesForSynchronizedTables(dbh,
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
        TableProperties.refreshTablePropertiesForTable(dbh, tp.getTableId(),
            KeyValueStore.Type.SERVER));
    // used to get the above from the ACTIVE store. if things go wonky, maybe
    // check to see if it was ACTIVE rather than SERVER for a reason. can't
    // think of one. one thing is that if it fails you'll see a table but won't
    // be able to open it, as there won't be any KVS stuff appropriate for it.
    boolean success = false;
    // Prepare the tableResult. We'll start it as failure, and only update it
    // if we're successful at the end.
    TableResult tableResult = new TableResult(tp.getDisplayName(),
        tp.getTableId());
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
        // IF we were updating, we made updates to the TableProperties on the
        // server. The subsequent call to synchronizeTableRest will overwrite
        // those calls. So we have to save the values that have been returned.
        if (success) {
          // First update the tp. We need this so that the sync tag is
          // correct.
          tp = TableProperties.refreshTablePropertiesForTable(dbh, tp.getTableId(),
              tp.getBackingStoreType());
          success = synchronizeTableRest(tp, table, false, tableResult);
        }
        break;
      case rest:
        success = synchronizeTableRest(tp, table, downloadingTable,
            tableResult);
        break;
      default:
        Log.e(TAG, "got unrecognized syncstate: " + tp.getSyncState());
      }
      // It is possible the table properties changed. Refresh just in case.
      tp = TableProperties.refreshTablePropertiesForTable(dbh, tp.getTableId(),
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

    // First pass set the action, so that we can tell the user what we did.
    tableResult.setTableAction(SyncState.updating);
    // As far as I can tell, this is only called if the PROPERTIES of the
    // table have changed--NOT if the data has changed.
    tableResult.setHadLocalPropertiesChanges(true);

    boolean success = false;
    try {
      // TODO: what happens here? As far as I can tell, we pull the changes
      // and blow our changes away. We then refresh our table properties, which
      // now match the server's? We then try to set them. This set does nothing
      // and is essentially a no-op, other than that it takes battery, allows
      // the possibility for failure, etc. Should we do something smarter about
      // deciding which gets priorities, or a smarter merge, or something?
      updateDbFromServer(tp, table, false, tableResult);
      // update the tp.
      tp = TableProperties.refreshTablePropertiesForTable(dbh, tp.getTableId(),
          KeyValueStore.Type.SERVER);
      String syncTag = synchronizer.setTableProperties(tableId,
          tp.getSyncTag(), tp.getTableKey(), getAllKVSEntries(tableId,
              KeyValueStore.Type.SERVER));
      // So we've updated the server.
      tableResult.setPushedLocalProperties(true);
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

    // Here we'll sycnhronize the table files.
    try {
      synchronizer.syncTableFiles(tp.getTableId());
    } catch (IOException e) {
      // TODO: update table result.
      Log.e(TAG, "[synchronizeTableInserting] error synching table files");
    }

    // If it was inserting, then we know we had properties changes to add.
    tableResult.setHadLocalPropertiesChanges(true);
    // Set the tableResult action so we can inform the user what we tried to
    // do.
    tableResult.setTableAction(SyncState.inserting);

    List<String> userColumns = tp.getColumnOrder();
    List<SyncRow> rowsToInsert = getRowsToPushToServer(table, userColumns,
        SyncUtil.State.INSERTING);
    // If there are rows, then we have to let tableResult know it.
    if (rowsToInsert.size() > 0) {
      tableResult.setHadLocalDataChanges(true);
    }

    boolean success = false;
    try {
      /**************************
       * PART 1: CREATE THE TABLE
       * First we need to create the table on the server. This comes in two
       * parts--the definition and the properties.
       **************************/
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
      // If we make it here we've set both the definition and the properties,
      // so we can say yes we've added the table to the server.
      tableResult.setPushedLocalProperties(true);
      tp.setSyncTag(syncTagProperties);

      /**************************
       * PART 2: INSERT THE DATA
       * Now we need to put some data on the server.
       **************************/
      Modification modification = synchronizer.insertRows(tableId,
          tp.getSyncTag(), rowsToInsert);
      updateDbFromModification(modification, table, tp);
      // If we made it here, then we know we pushed the data to the server AND
      // updated our db with the synctags appropriately.
      // TODO: is this the correct place to do this? What if we throw an
      // exception modifying the db? We'll have pushed the data but our db
      // might be in an indeterminate state--depends how the endRowsTransaction
      // stuff works.
      tableResult.setPushedLocalData(true);
      success = true;
    } catch (IOException e) {
      ioException("synchronizeTableInserting", tp, e, tableResult);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableInserting", tp, e, tableResult);
      syncResult.stats.numSkippedEntries += rowsToInsert.size();
      success = false;
    } finally {
      if (success) {
        updateRowsState(table, getRowIdsAsArray(rowsToInsert),
            SyncUtil.State.REST);
      }
    }

    return success;
  }

  private boolean synchronizeTableDeleting(TableProperties tp, DbTable table,
      TableResult tableResult) {
    String tableId = tp.getTableId();
    // Set the tableResult action so we can inform the user what we tried to
    // do.
    tableResult.setTableAction(SyncState.deleting);
    // We'll set both the local data and properties changes to true, b/c we're
    // going to blow away whatever is on the server. Note that this might be
    // true even if there are no rows on the server or the phone, in which case
    // the hadLocalData changes might not strictly be true.
    tableResult.setHadLocalDataChanges(true);
    tableResult.setHadLocalPropertiesChanges(true);
    Log.i(TAG, "DELETING " + tp.getDisplayName());
    boolean success = false;
    try {
      synchronizer.deleteTable(tableId);
      // If we didn't error, then we know we pushed our changes, b/c we changed
      // what was on the server.
      tableResult.setPushedLocalData(true);
      tableResult.setPushedLocalProperties(true);
      tp.deleteTableActual();
      // If this worked, then we were successful. No field currently exists for
      // "updated local db" without there being incoming data changes from the
      // server, so we'll rely on an action of deleting and a status of
      // success to mean that yes we deleted it. But, we'll also include a
      // message.
      tableResult.setMessage(MSG_DELETED_LOCAL_TABLE);
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

    try {
      synchronizer.syncTableFiles(tp.getTableId());
    } catch (IOException e) {
      // TODO: update table result.
      Log.e(TAG, "[synchronizeTableRest] error synchronizing table files");
    }

    // First set the action so we can report it back to the user. We don't have
    // to worry about the special case where we are downloading it from the
    // server of the first time, because that takes place through a separate
    // activity. Therefore we'll know that an action of REST is to be expected.
    // We do, however, have to make sure not to overwrite an UPDATING value,
    // as this method is called after the call to update.
    if (tableResult.getTableAction() != SyncState.updating) {
      tableResult.setTableAction(SyncState.rest);
    }
    // It's possible that the call to this method follows a call to
    // synchronizeTableUpdating. For that reason we can't assume there are no
    // properties changes. Thus rely on the fact that the
    // hadLocalPropertiesChanges value in TableResult inits to false, and on
    // the fact that the appropriate method will have updated it to true if
    // necessary. And then don't set anything.

    List<String> userColumns = tp.getColumnOrder();

    // get updates from server
    // if we fail here we don't try to continue
    // (do this first because the updates could affect the state of the rows
    // in the db when we query for them in the next step, e.g. turn an INSERTING
    // row into CONFLICTING)
    try {
      updateDbFromServer(tp, table, downloadingTable, tableResult);
    } catch (IOException e) {
      ioException("synchronizeTableRest", tp, e, tableResult);
      return false;
    } catch (Exception e) {
      exception("synchronizeTableRest", tp, e, tableResult);
      return false;
    }
    // refresh the tp
    tp = TableProperties.refreshTablePropertiesForTable(dbh, tp.getTableId(),
        tp.getBackingStoreType());

    // get changes that need to be pushed up to server
    List<SyncRow> rowsToInsert = getRowsToPushToServer(table, userColumns,
        SyncUtil.State.INSERTING);
    List<SyncRow> rowsToUpdate = getRowsToPushToServer(table, userColumns,
        SyncUtil.State.UPDATING);
    List<SyncRow> rowsToDelete = getRowsToPushToServer(table, userColumns,
        SyncUtil.State.DELETING);

    if (rowsToInsert.size() != 0 || rowsToUpdate.size() != 0 ||
        rowsToDelete.size() != 0) {
      if (tableResult.hadLocalDataChanges()) {
        Log.e(TAG, "synchronizeTableRest hadLocalDataChanges() returned " +
            "true, and we're about to set it" +
            " to true again. Odd.");
      }
      tableResult.setHadLocalDataChanges(true);
    }

    List<SyncRow> allRows = new ArrayList<SyncRow>();
    allRows.addAll(rowsToInsert);
    allRows.addAll(rowsToUpdate);
    allRows.addAll(rowsToDelete);
    String[] rowIds = getRowIdsAsArray(allRows);

    // push the changes up to the server
    boolean success = false;
    try {
      Modification modification = synchronizer.insertRows(tableId,
          tp.getSyncTag(), rowsToInsert);
      updateDbFromModification(modification, table, tp);
      modification = synchronizer.updateRows(tableId, tp.getSyncTag(),
          rowsToUpdate);
      updateDbFromModification(modification, table, tp);
      String syncTag = synchronizer.deleteRows(tableId, tp.getSyncTag(),
          getRowIdsAsList(rowsToDelete));
      // And now update that we've pushed our changes to the server.
      tableResult.setPushedLocalData(true);
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
      if (success) {
        allRows.removeAll(rowsToDelete);
        updateRowsState(table, getRowIdsAsArray(allRows), SyncUtil.State.REST);
      }
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
      boolean downloadingTable, TableResult tableResult) throws IOException {

    // retrieve updates TODO: after adding editing a row and a color rule, then synching, then copying the kvs into the server set and synching, this returned true that tp had changed and that i had a new sync row (the old row). this shouldn't do that.
    IncomingModification modification = synchronizer.getUpdates(
        tp.getTableId(), tp.getSyncTag());
    List<SyncRow> rows = modification.getRows();
    String newSyncTag = modification.getTableSyncTag();
    // Update the tableResult object now. We have enough information to know if
    // the properties or data changed on the server.
    if (modification.hasTablePropertiesChanged()) {
      Log.d(TAG, "updateDbFromServer setServerHadPropertiesChanged(true)");
      tableResult.setServerHadPropertiesChanges(true);
    }
    // Now check if there were rows.
    if (rows.size() > 0) {
      tableResult.setServerHadDataChanges(true);
    }
    List<String> columns = tp.getColumnOrder();
    // TODO: confirm handling of rows that have pending/unsaved changes from Collect

    UserTable allRowIds = table.getRaw(columns,
    		new String[] {DataTableColumns.SAVEPOINT_TYPE},
            new String[] {DbTable.SavedStatus.COMPLETE.name()}, null);

    /**************************
     * PART 1: UPDATE THE TABLE ITSELF.
     * We only do this if necessary. Do this before updating data in case
     * columns have changed or something specific applies.
     * These updates come in two parts: the table definition, and the table
     * properties (i.e. the key value store).
     **************************/
    if (modification.hasTablePropertiesChanged()) {
      // We have two things to worry about. One is if the table definition
      // (i.e. the table datastructure or the table's columns' datastructure)
      // has changed. The other is if the key value store has changed.
      // We'll get both pieces of information from the properties resource.
      TableDefinitionResource definitionResource =
          modification.getTableDefinitionResource();
      PropertiesResource propertiesResource =
          modification.getTableProperties();
      /**************************
       * PART 1A: UPDATE THE DEFINITION.
       * If we're downloading the table, we go ahead and update. Otherwise,
       * we don't allow changes to the definition, so just log an error
       * message.
       **************************/
      if (downloadingTable) {
        // The table is being downloaded for the first time. We first must
        // delete the dummy table we'd created and then add the new table.
        Log.w(TAG, "table: " + tp.getDisplayName() + " is being downloaded " +
            "for the first time. deleting place holder table.");
        tp.deleteTableActual(); // not sure we need this anymore?
        // update the tp
        // SHOULD THIS METHOD ACTUALLY SET THE SYNC TAG?!? IT SHOWS SETS
        // AT THE END OF THIS METHOD.
        tp = addTableFromDefinitionResource(definitionResource, newSyncTag);
        // We've updated the definition, so set this to true. Even though we
        // might not have the KVS values themselves! So might be a bad idea.
        tableResult.setPulledServerProperties(true);
      } else {
        Log.w(TAG, "database properties have changed. " +
            "structural modifications are not allowed. if structure needs" +
            " to be updated, it is not happening.");
      }
      /**************************
       * PART 1B: UPDATE THE PROPERTIES/KVS.
       * So, the properties had changed on the server. We have them in the
       * modification. Note that if we had properties on the server, we'll blow
       * away our modifications that had been in the server store on the phone.
       * That's just the cruel way of the world, and is by design.
       **************************/
      resetKVSForPropertiesResource(tp, propertiesResource);
      tableResult.setPulledServerProperties(true);
    }

    // sort data changes into types
    List<SyncRow> rowsToConflict = new ArrayList<SyncRow>();
    List<SyncRow> rowsToUpdate = new ArrayList<SyncRow>();
    List<SyncRow> rowsToInsert = new ArrayList<SyncRow>();
    List<SyncRow> rowsToDelete = new ArrayList<SyncRow>();
    // This will store the local versions of the rows that we must transition
    // to state conflicting.
    Map<String, Row> localVersionsOfConflictingRows =
        new HashMap<String, Row>();

    for (SyncRow row : rows) {
      boolean found = false;
      for (int i = 0; i < allRowIds.getHeight(); i++) {
        String rowId = allRowIds.getRowId(i);
        String stateStr = allRowIds.getMetadataByElementKey(i,
            DataTableColumns.SYNC_STATE);
        int state = Integer.parseInt(stateStr);
        if (row.getRowId().equals(rowId)) {
          found = true;
          if (state == SyncUtil.State.REST) {
            if (row.isDeleted())
              rowsToDelete.add(row);
            else
              rowsToUpdate.add(row);
          } else {
            localVersionsOfConflictingRows.put(rowId,
                allRowIds.getRowAtIndex(i));
            rowsToConflict.add(row);
          }
        }
      }
      if (!found && !row.isDeleted())
        rowsToInsert.add(row);
    }

    // perform data changes
    conflictRowsInDb(table, rowsToConflict, localVersionsOfConflictingRows);
    updateRowsInDb(table, rowsToUpdate);
    insertRowsInDb(table, rowsToInsert);
    deleteRowsInDb(table, rowsToDelete);

    // If we made it here and there was data, then we successfully updated the
    // data from the server.
    if (rows.size() > 0) {
      tableResult.setPulledServerData(true);
    }

    // We have to set this synctag here so that the server knows we saw its
    // changes. Otherwise it won't let us put up new information.
    tp.setSyncTag(newSyncTag);
  }

  private void conflictRowsInDb(DbTable table, List<SyncRow> rows,
      Map<String, Row> localVersionsOfConflictingRows) {
    for (SyncRow row : rows) {
      Log.i(TAG, "conflicting row, id=" + row.getRowId() + " syncTag=" +
        row.getSyncTag());
      ContentValues values = new ContentValues();

      // delete conflicting row if it already exists
      String whereClause = String.format("%s = ? AND %s = ? AND %s IN " +
      		"( ?, ? )",
          DataTableColumns.ID,
          DataTableColumns.SYNC_STATE, DataTableColumns.CONFLICT_TYPE);
      String[] whereArgs = { row.getRowId(),
          String.valueOf(SyncUtil.State.CONFLICTING),
          String.valueOf(SyncUtil.ConflictType.SERVER_DELETED_OLD_VALUES),
          String.valueOf(SyncUtil.ConflictType.SERVER_UPDATED_UPDATED_VALUES)};
      table.deleteRowActual(whereClause, whereArgs);

      // update existing row
      // Here we are updating the local version of the row. Its sync_state
      // will be CONFLICT. If the row was formerly deleted, then its
      // conflict_type should become LOCAL_DELETED_OLD_VALUES, signifying that
      // that row was deleted locally and contains the values at the time of
      // deletion. If the row was in state updating, that means that its
      // conflict_type should become LOCAL_UPDATED_UPDATED_VALUES, signifying
      // that the local version was in state updating, and the version of the
      // row contains the local changes that had been made.
      Row localRow = localVersionsOfConflictingRows.get(row.getRowId());
      String localRowSyncStateStr =
          localRow.getDataOrMetadataByElementKey(DataTableColumns.SYNC_STATE);
      int localRowSyncState = Integer.parseInt(localRowSyncStateStr);
      int localRowConflictType;
      if (localRowSyncState == SyncUtil.State.UPDATING) {
        localRowConflictType =
            SyncUtil.ConflictType.LOCAL_UPDATED_UPDATED_VALUES;
        Log.i(TAG, "local row was in sync state UPDATING, changing to " +
            "CONFLICT and setting conflict type to: " + localRowConflictType);
      } else if (localRowSyncState == SyncUtil.State.DELETING) {
        localRowConflictType =
            SyncUtil.ConflictType.LOCAL_DELETED_OLD_VALUES;
        Log.i(TAG, "local row was in sync state DELETING, changing to " +
            "CONFLICT and updating conflict type to: " + localRowConflictType);
      } else {
        if (localRowSyncState != SyncUtil.State.CONFLICTING) {
          // If we ever make it here we're potentially in big trouble, as we may
          // have updated the db. At the least, we're disobeying the protocol.
          // Throwing an exception, however, could lead to being in an even worse
          // indeterminate state, so we'll just log an error.
          Log.e(TAG, "a local row was passed to conflictRowsInDb that was" +
          		" not " +
              "a state that should have generated a conflict. Local row " +
              "state " +
              "was: " + localRowSyncState);
        }
        // Would this get passed here? It might. We should leave it in its old
        // conflict type and state.
        String localRowConflictTypeBeforeSyncStr =
            localRow.getDataOrMetadataByElementKey(
                DataTableColumns.CONFLICT_TYPE);
        int localRowConflictTypeBeforeSync =
            Integer.parseInt(localRowConflictTypeBeforeSyncStr);
        localRowConflictType = localRowConflictTypeBeforeSync;
        Log.i(TAG, "local row was in sync state CONFLICTING, leaving as " +
        		"CONFLICTING and leaving conflict type unchanged as: " +
            localRowConflictTypeBeforeSync);
      }
      values.put(DataTableColumns.ID, row.getRowId());
      values.put(DataTableColumns.SYNC_STATE,
          String.valueOf(SyncUtil.State.CONFLICTING));
      values.put(DataTableColumns.CONFLICT_TYPE, localRowConflictType);
      table.actualUpdateRowByRowId(row.getRowId(), values);

      for (Entry<String, String> entry : row.getValues().entrySet()) {
      	String colName = entry.getKey();
      	if ( colName.equals(LAST_MOD_TIME_LABEL)) {
      		String lastModTime = entry.getValue();
      		DateTime dt = du.parseDateTimeFromDb(lastModTime);
      		values.put(DataTableColumns.SAVEPOINT_TIMESTAMP,
      		    Long.toString(dt.getMillis()));
      	} else {
      		values.put(colName, entry.getValue());
      	}
      }

      // insert conflicting row
      // This is inserting the version of the row from the server.
      int serverRowConflictType;
      if (row.isDeleted()) {
        serverRowConflictType =
            SyncUtil.ConflictType.SERVER_DELETED_OLD_VALUES;
      } else {
        serverRowConflictType =
            SyncUtil.ConflictType.SERVER_UPDATED_UPDATED_VALUES;
      }
      values.put(DataTableColumns.SYNC_TAG, row.getSyncTag());
      values.put(DataTableColumns.SYNC_STATE,
          String.valueOf(SyncUtil.State.CONFLICTING));
      values.put(DataTableColumns.CONFLICT_TYPE, serverRowConflictType);
      table.actualAddRow(values);

      // We're going to check our representation invariant here. A local and
      // a server version of the row should only ever be updating/updating,
      // deleted/updating, or updating/deleted. Anything else and we're in
      // trouble.
      if (localRowConflictType ==
          SyncUtil.ConflictType.LOCAL_DELETED_OLD_VALUES &&
          serverRowConflictType !=
            SyncUtil.ConflictType.SERVER_UPDATED_UPDATED_VALUES) {
          Log.e(TAG, "local row conflict type is local_deleted, but server " +
          		"row conflict_type is not server_udpated. These states must" +
          		" go together, something went wrong.");
      } else if (localRowConflictType !=
                 SyncUtil.ConflictType.LOCAL_UPDATED_UPDATED_VALUES) {
        Log.e(TAG, "localRowConflictType was not local_deleted or " +
        		"local_updated! this is an error. local conflict type: " +
            localRowConflictType + ", server conflict type: " +
        		serverRowConflictType);
      }

      // Why are we telling this to syncResult? Are these particular to the
      // syncadapter's own reckoning? E.g. is our number of conflicts the
      // kind of conflict it's trying to keep an eye on? I'm less than sure.
      syncResult.stats.numConflictDetectedExceptions++;
      syncResult.stats.numEntries += 2;
    }
  }

  private void insertRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      ContentValues values = new ContentValues();

      values.put(DataTableColumns.ID, row.getRowId());
      values.put(DataTableColumns.SYNC_TAG, row.getSyncTag());
      values.put(DataTableColumns.SYNC_STATE, SyncUtil.State.REST);

      for (Entry<String, String> entry : row.getValues().entrySet()) {
      	String colName = entry.getKey();
      	if ( colName.equals(LAST_MOD_TIME_LABEL)) {
      		String lastModTime = entry.getValue();
      		DateTime dt = du.parseDateTimeFromDb(lastModTime);
      		values.put(DataTableColumns.SAVEPOINT_TIMESTAMP,
      		    Long.toString(dt.getMillis()));
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
      values.put(DataTableColumns.SYNC_STATE,
          String.valueOf(SyncUtil.State.REST));

      for (Entry<String, String> entry : row.getValues().entrySet()) {
    	String colName = entry.getKey();
    	if ( colName.equals(LAST_MOD_TIME_LABEL)) {
    		String lastModTime = entry.getValue();
    		DateTime dt = du.parseDateTimeFromDb(lastModTime);
    		values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, Long.toString(dt.getMillis()));
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

  private void updateDbFromModification(Modification modification,
      DbTable table, TableProperties tp) {
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
   * NB: See note at bottom that state CONFLICTING is forbidden.
   * <p>
   * Get the sync rows for the user-defined rows specified by the elementkeys
   * of columnsToSync. Returns a list of {@link SyncRow} objects. The rows
   * returned will be only those whose sync state matches the state parameter.
   * The metadata columns that should be synched are also included in the
   * returned {@link SyncRow}s.
   * <p>
   * These rows should only be for pushing to the server. This means that you
   * cannot request state CONFLICTING rows. as this would require additional
   * information as to whether you wanted the local or server versions, or both
   * of them. An IllegalArgumentException will be thrown if you pass a state
   * CONFLICTING.
   * @param table
   * @param columnsToSync the element keys of the user-defined columns to sync.
   * Should likely be all of them.
   * @param state the query of the rows in question. Eg inserting will return
   * only those rows whose sync state is inserting.
   * @return
   * @throws IllegalArgumentException if the requested state is CONFLICTING.
   */
  private List<SyncRow> getRowsToPushToServer(DbTable table,
      List<String> columnsToSync, int state) {
    if (state == SyncUtil.State.CONFLICTING) {
      throw new IllegalArgumentException("requested state CONFLICTING for" +
      		" rows to push to the server.");
    }
    // TODO: confirm handling of rows that have pending/unsaved changes from Collect
    UserTable rows = table.getRaw(columnsToSync, new String[]
        {DataTableColumns.SAVEPOINT_TYPE,
    			DataTableColumns.SYNC_STATE },
        new String[] { DbTable.SavedStatus.COMPLETE.name(),
    			String.valueOf(state) },
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
      String syncTag = rows.getMetadataByElementKey(i,
          DataTableColumns.SYNC_TAG);
      Map<String, String> values = new HashMap<String, String>();

      // precompute the correspondence between the displayed elementKeys and
      // the UserTable userData index
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
          if (metadataElementKey.equals(DataTableColumns.SAVEPOINT_TIMESTAMP)) {
            Long timestamp = Long.valueOf(rows.getMetadataByElementKey(
                i, DataTableColumns.SAVEPOINT_TIMESTAMP));
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

  private void updateRowsState(DbTable table, String[] rowIds, int state) {
    ContentValues values = new ContentValues();
    values.put(DataTableColumns.SYNC_STATE, state);
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
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonParseException
   */
  private TableProperties addTableFromDefinitionResource(
      TableDefinitionResource definitionResource, String syncTag) throws JsonParseException, JsonMappingException, IOException {
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
      List<String> listChildElementKeys = null;
      String lek = col.getListChildElementKeys();
      if ( lek != null && lek.length() != 0) {
        listChildElementKeys = mapper.readValue(lek, List.class);
      }
      tp.addColumn(col.getElementKey(), col.getElementKey(),
          col.getElementName(),
          ColumnType.valueOf(col.getElementType()),
          listChildElementKeys,
          SyncUtil.intToBool(col.getIsPersisted()));
    }
    // Refresh the table properties to get the columns.
    tp = TableProperties.refreshTablePropertiesForTable(dbh,
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
      String listChildElementKeysStr = null;
      try {
        listChildElementKeysStr =
            mapper.writeValueAsString(listChildrenElements);
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
          colType.name(), listChildElementKeysStr,
          (isPersisted != 0));
      columns.add(c);
    }
    return columns;
  }
}
