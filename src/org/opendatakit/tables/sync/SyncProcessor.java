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
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.aggregate.odktables.rest.entity.PropertiesResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.common.android.provider.ConflictType;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.SyncState;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.KeyValueStoreSync;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.data.UserTable.Row;
import org.opendatakit.tables.sync.TableResult.Status;
import org.opendatakit.tables.sync.aggregate.SyncTag;
import org.opendatakit.tables.sync.exceptions.SchemaMismatchException;
import org.opendatakit.tables.utils.TableFileUtils;
import org.springframework.web.client.ResourceAccessException;

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

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.setVisibilityChecker(mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
  }

  private final DataUtil du;
  private final SyncResult syncResult;
  private final Synchronizer synchronizer;
  private final DbHelper dbh;
  /**
   * The results of the synchronization that we will pass back to the user. Note
   * that this is NOT the same as the {@link SyncResult} object, which is used
   * to inform the android SyncAdapter how the sync process has gone.
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
   * <p>
   * This becomes more complicated with the ability to synchronize files. The
   * new order is as follows:
   * <ol>
   * <li>Synchronize app-level files. (i.e. those files under the appid
   * directory that are NOT then under the tables, instances, metadata, or
   * logging directories.) This is a multi-part process:
   * <ol>
   * <li>Get the app-level manifest, download any files that have changed
   * (differing hashes) or that do not exist.</li>
   * <li>Upload the files that you have that are not on the manifest. Note that
   * this could be suppressed if the user does not have appropriate permissions.
   * </li>
   * </ol>
   * </li>
   *
   * <li>Synchronize the static table files for those tables that are set to
   * sync. (i.e. those files under "appid/tables/tableid"). This follows the
   * same multi-part steps above (1a and 1b).</li>
   *
   * <li>Synchronize the table properties/metadata.</li>
   *
   * <li>Synchronize the table data. This includes the data in the db as well as
   * those files under "appid/instances/tableid". This file synchronization
   * follows the same multi-part steps above (1a and 1b).</li>
   *
   * <li>TODO: step four--the synchronization of instances files--should perhaps
   * also be allowed to be modular and permit things like ODK Submit to handle
   * data and files separately.</li>
   * </ol>
   * <p>
   * TODO: This should also somehow account for zipped files, exploding them or
   * what have you.
   * </p>
   */
  public SynchronizationResult synchronize(boolean pushLocalAppLevelFiles,
                                           boolean pushLocalTableNonMediaFiles,
                                           boolean syncMediaFiles) {
    Log.i(TAG, "entered synchronize()");
    // First we're going to synchronize the app level files.
    try {
      synchronizer.syncAppLevelFiles(pushLocalAppLevelFiles);
    } catch (ResourceAccessException e) {
      // TODO: update a synchronization result to report back to them as well.
      Log.e(TAG, "[synchronize] error trying to synchronize app-level files.");
      e.printStackTrace();
    }
    // TableProperties[] tps = dm.getSynchronizedTableProperties();
    // we want this call rather than just the getSynchronizedTableProperties,
    // because we only want to push the default to the server.
    TableProperties[] tps = TableProperties.getTablePropertiesForSynchronizedTables(dbh,
        KeyValueStore.Type.SERVER);
    for (TableProperties tp : tps) {
      Log.i(TAG, "synchronizing table " + tp.getDisplayName());
      synchronizeTable(tp, pushLocalTableNonMediaFiles, syncMediaFiles);
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
   * <p>
   * This method does NOT synchronize the application files. This means that if
   * any html files downloaded require the {@link TableFileUtils#DIR_FRAMEWORK}
   * directory, for instance, the caller must ensure that the app files are
   * synchronized as well.
   *
   * @param tp
   *          the table to synchronize
   * @param downloadingTable
   *          flag saying whether or not the table is being downloaded for the
   *          first time. Only applies to tables have their sync state set to
   *          {@link SyncState#rest}.
   * @param pushLocalNonMediaFiles
   *          true if local non media files should be pushed--e.g. any html
   *          files on the device should be pushed to the server
   * @param syncMediaFiles
   *          if media files should also be synchronized. Media files are
   *          defined as files that are part of the actual data of a table, e.g.
   *          pictures that have been collected.
   */
  public void synchronizeTable(TableProperties tp,
                               boolean pushLocalNonMediaFiles, boolean syncMediaFiles) {
    // TODO the order of synching should probably be re-arranged so that you
    // first
    // get the table properties and column entries (ie the table definition) and
    // THEN get the row data. This would make it more resilient to network
    // failures
    // during the process. along those lines, the same process should exist in
    // the
    // table creation on the phone. or rather, THAT should try and follow the
    // same
    // order.
    // TableProperties tp = TableProperties.addTable(dbh, dbTableName,
    // displayName, TableType.data, tableId, KeyValueStore.Type.SERVER);
    // tp.setSyncState(SyncState.rest);
    // tp.setSyncTag(null);

    // is this necessary?
    tp = TableProperties.refreshTablePropertiesForTable(dbh, tp.getTableId(),
        KeyValueStore.Type.SERVER);

    DbTable table = DbTable.getDbTable(dbh, tp);
    // used to get the above from the ACTIVE store. if things go wonky, maybe
    // check to see if it was ACTIVE rather than SERVER for a reason. can't
    // think of one. one thing is that if it fails you'll see a table but won't
    // be able to open it, as there won't be any KVS stuff appropriate for it.
    boolean success = false;
    // Prepare the tableResult. We'll start it as failure, and only update it
    // if we're successful at the end.
    TableResult tableResult = new TableResult(tp.getDisplayName(), tp.getTableId());
    beginTableTransaction(tp);
    try {
      switch (tp.getSyncState()) {
      case deleting:
        success = synchronizeTableDeleting(tp, table, tableResult);
        break;
      case inserting:
      case updating:
      case rest:
        // presume success...
        tableResult.setStatus(Status.SUCCESS);
        success = synchronizeTablePreserving(tp, table, tableResult,
                          pushLocalNonMediaFiles, syncMediaFiles);
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
          Log.e(TAG, "tableResult status for table: " + tp.getDbTableName()
              + " was EXCEPTION, and yet success returned true. This shouldn't be possible.");
        } else {
          tableResult.setStatus(Status.SUCCESS);
        }
      }
      mUserResult.addTableResult(tableResult);
    }
  }

  /**
   * This is broken -- we can delete tables locally without deleting them on the
   * server. We need a new mechanism.
   *
   * @param tp
   * @param table
   * @param tableResult
   * @return
   */
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
      tp.deleteTable();
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
   * This method is called when pushing any sort of change up to the server.
   *
   * If the server does not have the table, if the table was previously sync'd
   * (i.e., is not 'inserting') it returns an error and sets the table syncState
   * to 'inserting' so that on a subsequent sync will create it on the server.
   *
   * Otherwise, if the table is 'inserting', then we create the schema and set
   * the properties.
   *
   * Regardless, we then pull down all information about the table from the server.
   *
   * , it can be created.  eventually called when a table is first downloaded from the
   * server.
   * <p>
   * Note that WHENEVER this method is called, if updates to the key value store
   * or TableDefinition have been made, the tp parameter will become out of
   * date. Therefore after calling this method, the caller should refresh their
   * TableProperties.
   *
   * @param tp
   * @param table
   * @param downloadingTable
   * @return
   */
  private boolean synchronizeTablePreserving(TableProperties tp, DbTable table,
                                       TableResult tableResult, boolean pushLocalNonMediaFiles,
                                       boolean syncMediaFiles) {
    String tableId = tp.getTableId();
    Log.i(TAG, "REST " + tp.getDisplayName());

    try {
      synchronizer.syncNonRowDataTableFiles(tp.getTableId(), pushLocalNonMediaFiles);
    } catch (ResourceAccessException e) {
      resourceAccessException("synchronizeTableRest--nonMediaFiles", tp, e, tableResult);
      Log.e(TAG, "[synchronizeTableRest] error synchronizing table files");
      return false;
    } catch (Exception e) {
      exception("synchronizeTableRest--nonMediaFiles", tp, e, tableResult);
      Log.e(TAG, "[synchronizeTableRest] error synchronizing table files");
      return false;
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
      updateDbFromServer(tp, table, tableResult);
      if ( tableResult.getStatus() != Status.SUCCESS ) {
        return false;
      }
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
    List<SyncRow> rowsToInsert = getRowsToPushToServer(table, userColumns, SyncState.inserting);
    List<SyncRow> rowsToUpdate = getRowsToPushToServer(table, userColumns, SyncState.updating);
    List<SyncRow> rowsToDelete = getRowsToPushToServer(table, userColumns, SyncState.deleting);

    if (rowsToInsert.size() != 0 || rowsToUpdate.size() != 0 || rowsToDelete.size() != 0) {
      if (tableResult.hadLocalDataChanges()) {
        Log.e(TAG, "synchronizeTableRest hadLocalDataChanges() returned "
            + "true, and we're about to set it to true again. Odd.");
      }
      tableResult.setHadLocalDataChanges(true);
    }

    // push the changes up to the server
    boolean success = false;
    try {
      SyncTag revisedTag = tp.getSyncTag();
      for (SyncRow syncRow : rowsToInsert) {
        RowModification rm = synchronizer.insertOrUpdateRow(tableId, revisedTag, syncRow);

        ContentValues values = new ContentValues();
        values.put(DataTableColumns.ROW_ETAG, rm.getRowETag());
        values.put(DataTableColumns.SYNC_STATE, SyncState.rest.name());
        table.actualUpdateRowByRowId(rm.getRowId(), values);
        syncResult.stats.numInserts++;
        syncResult.stats.numEntries++;
        revisedTag = rm.getTableSyncTag();
        tp.setSyncTag(revisedTag);
      }
      for (SyncRow syncRow : rowsToUpdate) {
        RowModification rm = synchronizer.insertOrUpdateRow(tableId, revisedTag, syncRow);

        ContentValues values = new ContentValues();
        values.put(DataTableColumns.ROW_ETAG, rm.getRowETag());
        values.put(DataTableColumns.SYNC_STATE, SyncState.rest.name());
        table.actualUpdateRowByRowId(rm.getRowId(), values);
        syncResult.stats.numUpdates++;
        syncResult.stats.numEntries++;
        revisedTag = rm.getTableSyncTag();
        tp.setSyncTag(revisedTag);
      }
      for (SyncRow syncRow : rowsToDelete) {
        RowModification rm = synchronizer.deleteRow(tableId, revisedTag, syncRow);
        table.deleteRowActual(rm.getRowId());
        syncResult.stats.numDeletes++;
        syncResult.stats.numEntries++;
        revisedTag = rm.getTableSyncTag();
        tp.setSyncTag(revisedTag);
      }
      // And now update that we've pushed our changes to the server.
      tableResult.setPushedLocalData(true);
      // And now try to push up the media files, if necessary.
      if (syncMediaFiles) {
        try {
          Log.d(TAG, "[synchronizeTableRest] synching media files");
          synchronizer.syncRowDataFiles(tp.getTableId());
        } catch (ResourceAccessException e) {
          resourceAccessException("synchronizeTableRest--mediaFiles", tp, e, tableResult);
          Log.e(TAG, "[synchronizeTableRest] error synchronizing media files");
          return false;
        } catch (Exception e) {
          exception("synchronizeTableRest--mediaFiles", tp, e, tableResult);
          Log.e(TAG, "[synchronizeTableRest] error synchronizing media files");
          return false;
        }
      } else {
        Log.d(TAG, "[synchronizeTableRest] NOT synching media files");
      }
      success = true;
    } catch (IOException e) {
      ioException("synchronizeTableRest", tp, e, tableResult);
      success = false;
    } catch (Exception e) {
      exception("synchronizeTableRest", tp, e, tableResult);
      success = false;
    }

    return success;
  }

  private void resourceAccessException(String method, TableProperties tp,
                                       ResourceAccessException e, TableResult tableResult) {
    Log.e(TAG,
        String.format("ResourceAccessException in %s for table: %s", method, tp.getDisplayName()),
        e);
    tableResult.setStatus(Status.EXCEPTION);
    tableResult.setMessage(e.getMessage());
    syncResult.stats.numIoExceptions++;
  }

  private void
      ioException(String method, TableProperties tp, IOException e, TableResult tableResult) {
    Log.e(TAG, String.format("IOException in %s for table: %s", method, tp.getDisplayName()), e);
    tableResult.setStatus(Status.EXCEPTION);
    tableResult.setMessage(e.getMessage());
    syncResult.stats.numIoExceptions++;
  }

  private void exception(String method, TableProperties tp, Exception e, TableResult tableResult) {
    Log.e(TAG,
        String.format("Unexpected exception in %s on table: %s", method, tp.getDisplayName()), e);
    tableResult.setStatus(Status.EXCEPTION);
    tableResult.setMessage(e.getMessage());
  }

  /**
   * Update the database based on the server.
   *
   * @param tp
   *          modified in place if the table properties are changed
   * @param table
   * @param downloadingTable
   *          whether or not the table is being downloaded for the first time.
   * @throws IOException
   */
  private void updateDbFromServer(TableProperties tp, DbTable table,
                                  TableResult tableResult) throws IOException {

    // retrieve updates TODO: after adding editing a row and a color rule, then
    // synching, then copying the kvs into the server set and synching, this
    // returned true that tp had changed and that i had a new sync row (the old
    // row). this shouldn't do that.
    tableResult.setTableAction(SyncState.inserting);
    TableResource resource = synchronizer.getTableOrNull(tp.getTableId());

    if ( resource == null || resource.getPropertiesETag() == null ) {
      // the insert of the table was incomplete -- try again
      if ( tp.getSyncState() == SyncState.inserting ) {
        // we are creating data on the server
        // change our 'rest' state rows into 'inserting' rows
        DbTable dbt = DbTable.getDbTable(dbh, tp);
        dbt.changeRestRowsToInserting();
        // we need to clear out the dataETag and propertiesETag so
        // that we will pull all server changes and sync our properties.
        SyncTag newSyncTag = new SyncTag(null, null, tp.getSyncTag().getSchemaETag());
        tp.setSyncTag(newSyncTag);
        /**************************
         * PART 1A: CREATE THE TABLE First we need to create the table on the
         * server. This comes in two parts--the definition and the properties.
         **************************/
        // First create the table definition on the server.
        SyncTag syncTag;
        try {
          syncTag = synchronizer.createTable(tp.getTableId(), newSyncTag,
                                                    getColumnsForTable(tp));
        } catch (Exception e) {
          String msg = e.getMessage();
          if ( msg == null ) msg = e.toString();
          tableResult.setMessage(msg);
          tableResult.setStatus(Status.EXCEPTION);
          return;
        }

        /***************************
         * PART 1B: SET PROPERTIES Set the table properties on the server
         * *************************/
        SyncTag syncTagProperties;
        try {
          syncTagProperties = synchronizer.setTableProperties(tp.getTableId(),
            syncTag, getAllKVSEntries(tp.getTableId(), KeyValueStore.Type.SERVER));
        } catch (Exception e) {
          String msg = e.getMessage();
          if ( msg == null ) msg = e.toString();
          tableResult.setMessage(msg);
          tableResult.setStatus(Status.EXCEPTION);
          return;
        }

        // and update local tp to reflect that we have the same values as the server
        newSyncTag = new SyncTag(null, syncTagProperties.getPropertiesETag(), syncTagProperties.getSchemaETag());
        tp.setSyncTag(newSyncTag);

        // If we make it here we've set both the definition and the properties,
        // so we can say yes we've added the table to the server.
        tableResult.setPushedLocalProperties(true);

        // refresh the resource
        try {
          resource = synchronizer.getTableOrNull(tp.getTableId());
        } catch (Exception e) {
          String msg = e.getMessage();
          if ( msg == null ) msg = e.toString();
          tableResult.setMessage(msg);
          tableResult.setStatus(Status.EXCEPTION);
          return;
        }

        if ( resource == null || resource.getPropertiesETag() == null) {
          tableResult.setMessage("Unexpected error -- table should have been created!");
          tableResult.setStatus(Status.FAILURE);
          return;
        }

        // otherwise drop through and continue with insert...
      } else {
        // the table on the server is missing. Need to ask user what to do...
        tableResult.setServerHadSchemaChanges(true);
        tableResult.setMessage("Server no longer has table! Marking it as insert locally. Re-sync to upload.");
        tableResult.setStatus(Status.FAILURE);
        tp.setSyncState(SyncState.inserting);
        return;
      }
    } else {
      // the server has a table that matches
      if ( tp.getSyncState() == SyncState.inserting ) {
        // we are actually merging our data with a pre-existing table
        // on the server. Mark our 'rest' rows to be 'inserting' rows.
        DbTable dbt = DbTable.getDbTable(dbh, tp);
        dbt.changeRestRowsToInserting();
        // we need to clear out the dataETag and propertiesETag so
        // that we will pull all server changes down first. I.e.,
        // do not presume that we are the authority.
        SyncTag newSyncTag = new SyncTag(null, null, tp.getSyncTag().getSchemaETag());
        tp.setSyncTag(newSyncTag);
      }
      tableResult.setTableAction(tp.getSyncState());
    }

    // we found the matching resource on the server and we have set up our
    // local table to be ready for any data merge with the server's table.

    /**************************
     * PART 1A: UPDATE THE TABLE SCHEMA. We only do this if necessary. Do this
     * before updating data in case columns have changed or something specific
     * applies. These updates come in two parts: the table definition, and the
     * table properties (i.e. the key value store).
     **************************/
    if (!resource.getSchemaETag().equals(tp.getSyncTag().getSchemaETag()) ) {
      Log.d(TAG, "updateDbFromServer setServerHadSchemaChanges(true)");
      tableResult.setServerHadSchemaChanges(true);

      // fetch the table definition
      TableDefinitionResource definitionResource;
      try {
        definitionResource = synchronizer.getTableDefinition(resource.getDefinitionUri());
      } catch (Exception e) {
        String msg = e.getMessage();
        if ( msg == null ) msg = e.toString();
        tableResult.setMessage(msg);
        tableResult.setStatus(Status.EXCEPTION);
        return;
      }
      // record that we have pulled it
      tableResult.setPulledServerSchema(true);
      try {
        SyncTag newSyncTag = new SyncTag( tp.getSyncTag().getDataETag(), tp.getSyncTag().getPropertiesETag(), definitionResource.getSchemaETag());
        // apply changes
        tp = addTableFromDefinitionResource(definitionResource, newSyncTag);
        // schema changes are indistinguishable from a delete of the table
        // on the server and a create by another device.  Treat this as if
        // we are merging our data into the data already up on the server
        // change our 'rest' state rows into 'inserting' rows
        DbTable dbt = DbTable.getDbTable(dbh, tp);
        dbt.changeRestRowsToInserting();
        // we need to clear out the dataETag so that
        // we will pull all server changes and sync our properties.
        newSyncTag = new SyncTag(null, tp.getSyncTag().getPropertiesETag(), definitionResource.getSchemaETag());
        tp.setSyncTag(newSyncTag);
        // on the off-chance that this has changed
        resource.setSchemaETag(definitionResource.getSchemaETag());

        Log.w(TAG, "database schema has changed. "
            + "structural modifications, if any, were successful.");
      } catch (SchemaMismatchException e) {
        Log.w(TAG, "database properties have changed. "
            + "structural modifications were not successful. You must delete the table"
            + " and download it to receive the updates.");
        tableResult.setMessage(e.toString());
        tableResult.setStatus(Status.FAILURE);
        return;
      }
    }

    /**************************
     * PART 1B: UPDATE THE TABLE PROPERTIES (i.e., the key value store).
     * We only do this if necessary. Do this before updating data.
     **************************/
    if (!resource.getPropertiesETag().equals(tp.getSyncTag().getPropertiesETag()) ) {

      Log.d(TAG, "updateDbFromServer setServerHadPropertiesChanges(true)");
      tableResult.setServerHadPropertiesChanges(true);

      SyncTag resourceSyncTag = new SyncTag( resource.getDataETag(),
                    resource.getPropertiesETag(), resource.getSchemaETag());
      // We have two things to worry about. One is if the table definition
      // (i.e. the table datastructure or the table's columns' datastructure)
      // has changed. The other is if the key value store has changed.
      // We'll get both pieces of information from the properties resource.
      PropertiesResource propertiesResource;
      try {
        propertiesResource = synchronizer.getTableProperties(resource.getPropertiesUri(), resourceSyncTag);
      } catch (Exception e) {
        String msg = e.getMessage();
        if ( msg == null ) msg = e.toString();
        tableResult.setMessage(msg);
        tableResult.setStatus(Status.EXCEPTION);
        return;
      }

      tableResult.setPulledServerProperties(true);

      SyncTag newSyncTag = new SyncTag( tp.getSyncTag().getDataETag(), resource.getPropertiesETag(), resource.getSchemaETag());
      resetKVSForPropertiesResource(tp, propertiesResource, newSyncTag);

    } else if ( tp.getSyncState() != SyncState.rest ) {

      // we have table properties that have changed.
      // try to push these up to the server.
      SyncTag syncTag;
      try {
        syncTag = synchronizer.setTableProperties(tp.getTableId(), tp.getSyncTag(),
                                getAllKVSEntries(tp.getTableId(), KeyValueStore.Type.SERVER));
      } catch (Exception e) {
        String msg = e.getMessage();
        if ( msg == null ) msg = e.toString();
        tableResult.setMessage(msg);
        tableResult.setStatus(Status.EXCEPTION);
        return;
      }

      SyncTag newSyncTag = new SyncTag( tp.getSyncTag().getDataETag(), syncTag.getPropertiesETag(), syncTag.getSchemaETag());
      tp.setSyncTag(newSyncTag);
      tp.setSyncState(SyncState.rest);
      // We've updated the server.
      tableResult.setPushedLocalProperties(true);
    }

    // we should be up-to-date on the schema and properties
    // now fetch all the changed rows...
    IncomingRowModifications modification;
    try {
      modification = synchronizer.getUpdates(tp.getTableId(), tp.getSyncTag());
    } catch (Exception e) {
      String msg = e.getMessage();
      if ( msg == null ) msg = e.toString();
      tableResult.setMessage(msg);
      tableResult.setStatus(Status.EXCEPTION);
      return;
    }

    /**************************
     * PART 2: UPDATE THE DATA
     **************************/
    if (modification.hasTableDataChanged()) {
      Log.d(TAG, "updateDbFromServer setServerHadDataChanges(true)");
      tableResult.setServerHadDataChanges(true);

      List<SyncRow> rows = modification.getRows();
      List<String> columns = tp.getColumnOrder();
      // TODO: confirm handling of rows that have pending/unsaved changes from
      // Collect

      UserTable allRowIds = table.getRaw(columns, new String[] { DataTableColumns.SAVEPOINT_TYPE
      }, new String[] { DbTable.SavedStatus.COMPLETE.name()
      }, null);

      // sort data changes into types
      List<SyncRow> rowsToConflict = new ArrayList<SyncRow>();
      List<SyncRow> rowsToUpdate = new ArrayList<SyncRow>();
      List<SyncRow> rowsToInsert = new ArrayList<SyncRow>();
      List<SyncRow> rowsToDelete = new ArrayList<SyncRow>();
      // This will store the local versions of the rows that we must transition
      // to state conflicting.
      Map<String, Row> localVersionsOfConflictingRows = new HashMap<String, Row>();

      for (SyncRow row : rows) {
        boolean found = false;
        for (int i = 0; i < allRowIds.getNumberOfRows(); i++) {
          String rowId = allRowIds.getRowAtIndex(i).getRowId();
          if (row.getRowId().equals(rowId)) {
            String stateStr = allRowIds.getMetadataByElementKey(i, DataTableColumns.SYNC_STATE);
            SyncState state = SyncState.valueOf(stateStr);
            found = true;
            if (state == state.rest) {
              if (row.isDeleted())
                rowsToDelete.add(row);
              else
                rowsToUpdate.add(row);
            } else {
              localVersionsOfConflictingRows.put(rowId, allRowIds.getRowAtIndex(i));
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
      tp.setSyncTag(modification.getTableSyncTag());
    }
  }

  private void conflictRowsInDb(DbTable table, List<SyncRow> rows,
                                Map<String, Row> localVersionsOfConflictingRows) {
    for (SyncRow row : rows) {
      Log.i(TAG, "conflicting row, id=" + row.getRowId() + " rowETag=" + row.getRowETag());
      ContentValues values = new ContentValues();

      // delete conflicting row if it already exists
      String whereClause = String.format("%s = ? AND %s = ? AND %s IN " + "( ?, ? )",
          DataTableColumns.ID, DataTableColumns.SYNC_STATE, DataTableColumns.CONFLICT_TYPE);
      String[] whereArgs = { row.getRowId(), SyncState.conflicting.name(),
          String.valueOf(ConflictType.SERVER_DELETED_OLD_VALUES),
          String.valueOf(ConflictType.SERVER_UPDATED_UPDATED_VALUES)
      };
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
      String localRowSyncStateStr = localRow
          .getDataOrMetadataByElementKey(DataTableColumns.SYNC_STATE);
      SyncState localRowSyncState = SyncState.valueOf(localRowSyncStateStr);
      int localRowConflictType;
      if (localRowSyncState == SyncState.updating) {
        localRowConflictType = ConflictType.LOCAL_UPDATED_UPDATED_VALUES;
        Log.i(TAG, "local row was in sync state UPDATING, changing to "
            + "CONFLICT and setting conflict type to: " + localRowConflictType);
      } else if (localRowSyncState == SyncState.deleting) {
        localRowConflictType = ConflictType.LOCAL_DELETED_OLD_VALUES;
        Log.i(TAG, "local row was in sync state DELETING, changing to "
            + "CONFLICT and updating conflict type to: " + localRowConflictType);
      } else {
        if (localRowSyncState != SyncState.conflicting) {
          // If we ever make it here we're potentially in big trouble, as we may
          // have updated the db. At the least, we're disobeying the protocol.
          // Throwing an exception, however, could lead to being in an even
          // worse
          // indeterminate state, so we'll just log an error.
          Log.e(TAG, "a local row was passed to conflictRowsInDb that was" + " not "
              + "a state that should have generated a conflict. Local row " + "state " + "was: "
              + localRowSyncState);
        }
        // Would this get passed here? It might. We should leave it in its old
        // conflict type and state.
        String localRowConflictTypeBeforeSyncStr = localRow
            .getDataOrMetadataByElementKey(DataTableColumns.CONFLICT_TYPE);
        int localRowConflictTypeBeforeSync = Integer.parseInt(localRowConflictTypeBeforeSyncStr);
        localRowConflictType = localRowConflictTypeBeforeSync;
        Log.i(TAG, "local row was in sync state CONFLICTING, leaving as "
            + "CONFLICTING and leaving conflict type unchanged as: "
            + localRowConflictTypeBeforeSync);
      }
      values.put(DataTableColumns.ID, row.getRowId());
      values.put(DataTableColumns.SYNC_STATE, SyncState.conflicting.name());
      values.put(DataTableColumns.CONFLICT_TYPE, localRowConflictType);
      table.actualUpdateRowByRowId(row.getRowId(), values);

      for (Entry<String, String> entry : row.getValues().entrySet()) {
        String colName = entry.getKey();
        values.put(colName, entry.getValue());
      }

      // insert conflicting row
      // This is inserting the version of the row from the server.
      int serverRowConflictType;
      if (row.isDeleted()) {
        serverRowConflictType = ConflictType.SERVER_DELETED_OLD_VALUES;
      } else {
        serverRowConflictType = ConflictType.SERVER_UPDATED_UPDATED_VALUES;
      }
      values.put(DataTableColumns.ROW_ETAG, row.getRowETag());
      values.put(DataTableColumns.SYNC_STATE, SyncState.conflicting.name());
      values.put(DataTableColumns.CONFLICT_TYPE, serverRowConflictType);
      values.put(DataTableColumns.FORM_ID, row.getFormId());
      values.put(DataTableColumns.LOCALE, row.getLocale());
      values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, row.getSavepointTimestamp());
      values.put(DataTableColumns.SAVEPOINT_CREATOR, row.getSavepointCreator());
      table.actualAddRow(values);

      // We're going to check our representation invariant here. A local and
      // a server version of the row should only ever be updating/updating,
      // deleted/updating, or updating/deleted. Anything else and we're in
      // trouble.
      if (localRowConflictType == ConflictType.LOCAL_DELETED_OLD_VALUES
          && serverRowConflictType != ConflictType.SERVER_UPDATED_UPDATED_VALUES) {
        Log.e(TAG, "local row conflict type is local_deleted, but server "
            + "row conflict_type is not server_udpated. These states must"
            + " go together, something went wrong.");
      } else if (localRowConflictType != ConflictType.LOCAL_UPDATED_UPDATED_VALUES) {
        Log.e(TAG, "localRowConflictType was not local_deleted or "
            + "local_updated! this is an error. local conflict type: " + localRowConflictType
            + ", server conflict type: " + serverRowConflictType);
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
      values.put(DataTableColumns.ROW_ETAG, row.getRowETag());
      values.put(DataTableColumns.SYNC_STATE, SyncState.rest.name());
      values.put(DataTableColumns.FORM_ID, row.getFormId());
      values.put(DataTableColumns.LOCALE, row.getLocale());
      values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, row.getSavepointTimestamp());
      values.put(DataTableColumns.SAVEPOINT_CREATOR, row.getSavepointCreator());

      for (Entry<String, String> entry : row.getValues().entrySet()) {
        String colName = entry.getKey();
        values.put(colName, entry.getValue());
      }

      table.actualAddRow(values);
      syncResult.stats.numInserts++;
      syncResult.stats.numEntries++;
    }
  }

  private void updateRowsInDb(DbTable table, List<SyncRow> rows) {
    for (SyncRow row : rows) {
      ContentValues values = new ContentValues();

      values.put(DataTableColumns.ROW_ETAG, row.getRowETag());
      values.put(DataTableColumns.SYNC_STATE, SyncState.rest.name());
      values.put(DataTableColumns.FORM_ID, row.getFormId());
      values.put(DataTableColumns.LOCALE, row.getLocale());
      values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, row.getSavepointTimestamp());
      values.put(DataTableColumns.SAVEPOINT_CREATOR, row.getSavepointCreator());

      for (Entry<String, String> entry : row.getValues().entrySet()) {
        String colName = entry.getKey();
        values.put(colName, entry.getValue());
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

  /**
   * Returns all the columns that should be synched, including metadata columns.
   * Returns as a map of element key to {@link ColumnType}.
   *
   * @param tp
   * @return
   */
  private Map<String, ColumnType> getColumns(TableProperties tp) {
    Map<String, ColumnType> columns = new HashMap<String, ColumnType>();
    for (ColumnProperties colProp : tp.getDatabaseColumns().values()) {
      columns.put(colProp.getElementKey(), colProp.getColumnType());
    }
    columns.putAll(DbTable.getColumnsToSync());
    return columns;
  }

  /**
   * NB: See note at bottom that state CONFLICTING is forbidden.
   * <p>
   * Get the sync rows for the user-defined rows specified by the elementkeys of
   * columnsToSync. Returns a list of {@link SyncRow} objects. The rows returned
   * will be only those whose sync state matches the state parameter. The
   * metadata columns that should be synched are also included in the returned
   * {@link SyncRow}s.
   * <p>
   * These rows should only be for pushing to the server. This means that you
   * cannot request state CONFLICTING rows. as this would require additional
   * information as to whether you wanted the local or server versions, or both
   * of them. An IllegalArgumentException will be thrown if you pass a state
   * CONFLICTING.
   *
   * @param table
   * @param columnsToSync
   *          the element keys of the user-defined columns to sync. Should
   *          likely be all of them.
   * @param state
   *          the query of the rows in question. Eg inserting will return only
   *          those rows whose sync state is inserting.
   * @return
   * @throws IllegalArgumentException
   *           if the requested state is CONFLICTING.
   */
  private List<SyncRow> getRowsToPushToServer(DbTable table, List<String> columnsToSync,
                                              SyncState state) {
    if (state == SyncState.conflicting) {
      throw new IllegalArgumentException("requested state CONFLICTING for"
          + " rows to push to the server.");
    }
    // TODO: confirm handling of rows that have pending/unsaved changes from
    // Collect
    UserTable rows = table.getRaw(columnsToSync, new String[] { DataTableColumns.SAVEPOINT_TYPE,
        DataTableColumns.SYNC_STATE
    }, new String[] { DbTable.SavedStatus.COMPLETE.name(), state.name()
    }, null);

    List<SyncRow> changedRows = new ArrayList<SyncRow>();
    int numRows = rows.getNumberOfRows();
    int numCols = rows.getWidth();
    if (numCols != columnsToSync.size()) {
      Log.e(TAG, "number of user-defined columns returned in getRows() does "
          + "not equal the number of user-defined element keys requested (" + numCols + " != "
          + columnsToSync.size() + ")");
    }
    // And now for each row we need to add both the user columns AND the
    // columns to sync, AND the sync tag for the row.
    for (int i = 0; i < numRows; i++) {
      String rowId = rows.getRowAtIndex(i).getRowId();
      String rowETag = rows.getMetadataByElementKey(i, DataTableColumns.ROW_ETAG);
      Map<String, String> values = new HashMap<String, String>();

      // precompute the correspondence between the displayed elementKeys and
      // the UserTable userData index
      int[] userDataIndex = new int[numCols];
      for (int j = 0; j < numCols; ++j) {
        Integer idx = rows.getColumnIndexOfElementKey(columnsToSync.get(j));
        userDataIndex[j] = (idx == null) ? -1 : idx;
      }

      for (int j = 0; j < numCols; j++) {
        // We know that the columnsToSync should be metadata keys for the user-
        // defined columns. If they're not present we know there is a problem,
        String columnElementKey = columnsToSync.get(j);
        values.put(columnElementKey, rows.getData(i, userDataIndex[j]));
      }
      SyncRow row = new SyncRow(
                                rowId,
                                rowETag,
                                false,
                                rows.getMetadataByElementKey(i, DataTableColumns.FORM_ID),
                                rows.getMetadataByElementKey(i, DataTableColumns.LOCALE),
                                rows.getMetadataByElementKey(i,
                                    DataTableColumns.SAVEPOINT_TIMESTAMP),
                                rows.getMetadataByElementKey(i, DataTableColumns.SAVEPOINT_CREATOR),
                                values);
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

  private void updateRowsState(DbTable table, String[] rowIds, SyncState state) {
    ContentValues values = new ContentValues();
    values.put(DataTableColumns.SYNC_STATE, state.name());
    for (String rowId : rowIds) {
      table.actualUpdateRowByRowId(rowId, values);
    }
  }

  /**
   * Update the database to reflect the new structure.
   * <p>
   * This should be called when downloading a table from the server, which is
   * why the syncTag is separate. TODO: pass the db around rather than dbh so we
   * can do this transactionally
   *
   * @param definitionResource
   * @param syncTag
   *          the syncTag belonging to the modification from which you acquired
   *          the {@link TableDefinitionResource}.
   * @return the new {@link TableProperties} for the table.
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonParseException
   * @throws SchemaMismatchException
   */
  public TableProperties addTableFromDefinitionResource(TableDefinitionResource definitionResource,
                                                        SyncTag syncTag) throws JsonParseException,
      JsonMappingException, IOException, SchemaMismatchException {
    KeyValueStore.Type kvsType = KeyValueStore.Type.SERVER;
    TableProperties tp = TableProperties.refreshTablePropertiesForTable(dbh,
        definitionResource.getTableId(), kvsType);
    if (tp == null) {
      tp = TableProperties
          .addTable(dbh, definitionResource.getTableId(), definitionResource.getTableId(),
              TableType.data, definitionResource.getTableId(), kvsType);
      for (Column col : definitionResource.getColumns()) {
        // TODO: We aren't handling types correctly here. Need to have a mapping
        // on the server as well so that you can pull down the right thing.
        // TODO: add an addcolumn method to allow setting all of the
        // dbdefinition
        // fields.
        List<String> listChildElementKeys = null;
        String lek = col.getListChildElementKeys();
        if (lek != null && lek.length() != 0) {
          listChildElementKeys = mapper.readValue(lek, List.class);
        }
        tp.addColumn(col.getElementKey(), col.getElementKey(), col.getElementName(),
            ColumnType.valueOf(col.getElementType()), listChildElementKeys,
            SyncUtil.intToBool(col.getIsUnitOfRetention()));
      }
    } else {
      // see if the server copy matches our local schema
      boolean found = false;
      for (Column col : definitionResource.getColumns()) {
        List<String> listChildElementKeys;
        String lek = col.getListChildElementKeys();
        if (lek != null && lek.length() != 0) {
          listChildElementKeys = mapper.readValue(lek, List.class);
        } else {
          listChildElementKeys = new ArrayList<String>();
        }
        ColumnProperties cp = tp.getColumnByElementKey(col.getElementKey());
        if (cp == null) {
          // we can support modifying of schema via adding of columns
          tp.addColumn(col.getElementKey(), col.getElementKey(), col.getElementName(),
              ColumnType.valueOf(col.getElementType()), listChildElementKeys,
              SyncUtil.intToBool(col.getIsUnitOfRetention()));
        } else {
          List<String> cpListChildElementKeys = cp.getListChildElementKeys();
          if ( cpListChildElementKeys == null ) {
            cpListChildElementKeys = new ArrayList<String>();
          }
          if (!(cp.getElementName().equals(col.getElementName())
            && cp.isUnitOfRetention() == SyncUtil.intToBool(col.getIsUnitOfRetention())
            && cpListChildElementKeys.size() == listChildElementKeys.size()
            && cpListChildElementKeys.containsAll(listChildElementKeys) )) {
            throw new SchemaMismatchException("Server schema differs from local schema");
          } else if (!cp.getColumnType().equals(ColumnType.valueOf(col.getElementType()))) {
            // we have a column datatype change.
            // we should be able to handle this for simple types (unknown -> text
            // or text -> integer)
            throw new SchemaMismatchException("Server schema differs from local schema (column datatype change)");
          }
        }
      }
    }
    // Refresh the table properties to get the columns.
    tp = TableProperties.refreshTablePropertiesForTable(dbh, definitionResource.getTableId(),
        kvsType);
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVS = kvsm.getSyncStoreForTable(definitionResource.getTableId());
    syncKVS.setIsSetToSync(true);
    tp.setSyncTag(syncTag);
    return tp;
  }

  public TableProperties assertTableDefinition(String tableDefinitionUri)
      throws JsonParseException, JsonMappingException, IOException, SchemaMismatchException {
    TableDefinitionResource definitionResource = synchronizer
        .getTableDefinition(tableDefinitionUri);

    SyncTag newTag = new SyncTag(null, null, definitionResource.getSchemaETag());

    TableProperties tp = addTableFromDefinitionResource(definitionResource, newTag);
    tp.setSyncState(SyncState.rest);

    return tp;
  }

  /**
   * Wipe all of the existing key value entries in the server kvs and replace
   * them with the entries in the {@link PropertiesResource}.
   *
   * @param tp
   * @param propertiesResource
   * @param newTag -- the tag correlated with these changes
   */
  private void resetKVSForPropertiesResource(TableProperties tp,
                                             PropertiesResource propertiesResource, SyncTag newTag) {
    KeyValueStore.Type kvsType = KeyValueStore.Type.SERVER;
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore kvs = kvsm.getStoreForTable(tp.getTableId(), kvsType);
    kvs.clearKeyValuePairs(dbh.getWritableDatabase());
    kvs.addEntriesToStore(dbh.getWritableDatabase(), propertiesResource.getKeyValueStoreEntries());
    tp.setSyncTag(newTag);
  }

  /**
   * Return a list of all the entries in the key value store for the given table
   * and type.
   *
   * @param tableId
   * @param typeOfStore
   * @return
   */
  private ArrayList<OdkTablesKeyValueStoreEntry> getAllKVSEntries(String tableId,
                                                                  KeyValueStore.Type typeOfStore) {
    KeyValueStore kvs = KeyValueStoreManager.getKVSManager(dbh).getStoreForTable(tableId,
        typeOfStore);
    ArrayList<OdkTablesKeyValueStoreEntry> allEntries = kvs.getEntries(dbh.getReadableDatabase());
    return allEntries;
  }

  /**
   * Return a list of {@link Column} objects (representing the column
   * definition) for each of the columns associated with this table.
   *
   * @param tp
   * @return
   */
  private ArrayList<Column> getColumnsForTable(TableProperties tp) {
    ArrayList<Column> columns = new ArrayList<Column>();
    for (ColumnProperties cp : tp.getAllColumns().values()) {
      String elementKey = cp.getElementKey();
      String elementName = cp.getElementName();
      ColumnType colType = cp.getColumnType();
      List<String> listChildrenElements = cp.getListChildElementKeys();
      int isUnitOfRetention = SyncUtil.boolToInt(cp.isUnitOfRetention());
      String listChildElementKeysStr = null;
      try {
        listChildElementKeysStr = mapper.writeValueAsString(listChildrenElements);
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
      // Column c = new Column(tp.getTableId(), elementKey, elementName,
      // colType.name(), listChildElementKeysStr,
      // (isUnitOfRetention != 0), joinsStr);
      Column c = new Column(tp.getTableId(), elementKey, elementName, colType.name(),
                            listChildElementKeysStr, (isUnitOfRetention != 0));
      columns.add(c);
    }
    return columns;
  }
}
