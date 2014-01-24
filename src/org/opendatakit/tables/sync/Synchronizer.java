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
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.sync.aggregate.SyncTag;
import org.springframework.web.client.ResourceAccessException;

/**
 * Synchronizer abstracts synchronization of tables to an external cloud/server.
 *
 * @author the.dylan.price@gmail.com
 * @author sudar.sam@gmail.com
 *
 */
public interface Synchronizer {

  /**
   * Get a list of all tables in the server.
   *
   * @return a list of the table resources on the server
   */
  public List<TableResource> getTables() throws IOException;

  public TableResource getTable(String tableId) throws IOException;

  public TableDefinitionResource getTableDefinition(String tableDefinitionUri);

  /**
   * Create a table with the given id on the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param cols
   *          a map from column names to column types, see {@link ColumnType}
   * @return the SyncTag for the table
   */
  public SyncTag createTable(String tableId, ArrayList<Column> columns)
      throws IOException;

  /**
   * Delete the table with the given id from the server.
   *
   * @param tableId
   *          the unique identifier of the table
   */
  public void deleteTable(String tableId) throws IOException;

  /**
   * Retrieve changes in the server state since the last synchronization.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag, or null if this is
   *          the first synchronization
   * @return an IncomingModification representing the latest state of the table
   */
  public IncomingModification getUpdates(String tableId, SyncTag currentSyncTag) throws IOException;

  /**
   * Insert the given rows in the table on the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param rowsToInsert
   *          the rows to insert
   * @return a Modification of the syncTags to save with the rows and table
   */
  public Modification insertRows(String tableId, SyncTag currentSyncTag, List<SyncRow> rowsToInsert)
      throws IOException;

  /**
   * Update the given rows in the table on the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param rowsToUpdate
   *          the rows to update
   * @return a Modification of the syncTags to save with the rows and table
   */
  public Modification updateRows(String tableId, SyncTag currentSyncTag, List<SyncRow> rowsToUpdate)
      throws IOException;

  /**
   * Delete the given row ids from the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param rowIds
   *          the row ids of the rows to delete
   * @return the updated syncTag of the table
   */
  public SyncTag deleteRows(String tableId, SyncTag currentSyncTag, List<String> rowIds)
      throws IOException;

  /**
   * Sets the table name and table properties on the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param tableKey
   *          the tableKey of the table (from the definitions tables)
   * @param kvsEntries
   *          all the entries in the key value store for this table. Should
   *          be of the server kvs, since this is for synchronization.
   * @return the syncTag of the table
   * @throws IOException
   */
  public SyncTag setTableProperties(String tableId, SyncTag currentSyncTag, String tableName,
                                   ArrayList<OdkTablesKeyValueStoreEntry> kvsEntries) throws IOException;

  /**
   * Synchronize all the files in an app, including both app-level and table-
   * level files, but not those files that are in unsynched directories.
   *
   * @throws ResourceAccessException
   */
  public void syncAllFiles() throws ResourceAccessException;

  /**
   * Synchronizes the app level files. This includes any files that are not
   * associated with a particular table--i.e. those that are not in the
   * directory appid/tables/. It also excludes those files that are in a set of
   * directories that do not sync--appid/metadata, appid/logging, etc.
   *
   * @param true if local files should be pushed. Otherwise they are only
   *        pulled down.
   * @throws ResourceAccessException
   */
  public void syncAppLevelFiles(boolean pushLocalFiles) throws ResourceAccessException;

  /**
   * Sync only the files associated with the specified table. This does NOT
   * sync any media files associated with individual rows of the table.
   *
   * @param tableId
   * @param pushLocal
   *          true if the local files should be pushed
   * @throws ResourceAccessException
   */
  public void syncNonRowDataTableFiles(String tableId, boolean pushLocal) throws ResourceAccessException;

  /**
   * Sync only the media files associated with individual rows of a table.
   * This includes things like any pictures that have been collected as part
   * of a form. I.e. those files that are considered data.
   *
   * @param tableId
   * @throws ResourceAccessException
   */
  public void syncRowDataFiles(String tableId) throws ResourceAccessException;

}