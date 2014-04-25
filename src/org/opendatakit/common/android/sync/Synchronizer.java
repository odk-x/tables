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
package org.opendatakit.common.android.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.aggregate.odktables.rest.entity.PropertiesResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.sync.aggregate.SyncTag;
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

  /**
   * Discover the current sync state of a given tableId.
   * This may throw an exception if the table is not found on
   * the server.
   *
   * @param tableId
   * @return
   * @throws IOException
   */
  public TableResource getTable(String tableId) throws IOException;

  /**
   * Returns the given tableId resource or null if the resource
   * does not exist on the server.
   *
   * @param tableId
   * @return
   * @throws IOException
   */
  public TableResource getTableOrNull(String tableId) throws IOException;

  /**
   * Discover the schema for a table resource.
   *
   * @param tableDefinitionUri
   * @return
   */
  public TableDefinitionResource getTableDefinition(String tableDefinitionUri);

  /**
   * Assert that a table with the given id and schema exists on the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the current SyncTag for the table
   * @param cols
   *          a map from column names to column types, see {@link ColumnType}
   * @return the TableResource for the table (the server may return different SyncTag values)
   */
  public TableResource createTable(String tableId, SyncTag currentSyncTag, ArrayList<Column> columns)
      throws IOException;

  /**
   * Delete the table with the given id from the server.
   *
   * @param tableId
   *          the unique identifier of the table
   */
  public void deleteTable(String tableId) throws IOException;

  /**
   * Sets the table display name and table properties on the server.
   *
   * @param propertiesUri
   *          the URI to this table's properties on the server
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @return
   *          all the properties for the given table
   *
   * @throws IOException
   */
  public PropertiesResource getTablePropertiesResource(String propertiesUri, SyncTag currentSyncTag) throws IOException;


  /**
   * Sets the table display name and table properties on the server.
   *
   * @param propertiesUri
   *          the URI to this table's properties on the server
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param tableId
   *          the tableId of this table
   * @param kvsEntries
   *          all the entries in the key value store for this table. Should
   *          be of the server kvs, since this is for synchronization.
   * @return the syncTag of the table
   * @throws IOException
   */
  public SyncTag setTablePropertiesResource(String propertiesUri, SyncTag currentSyncTag, String tableId,
                                   ArrayList<OdkTablesKeyValueStoreEntry> kvsEntries) throws IOException;

  /**
   * Retrieve changes in the server state since the last synchronization.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag, or null if this is
   *          the first synchronization
   * @return an IncomingModification representing the latest state of the table
   *         on server since the last sync or null if the table does not exist
   *         on the server.
   *
   */
  public IncomingRowModifications getUpdates(String tableId, SyncTag currentSyncTag) throws IOException;

  /**
   * Insert or update the given row in the table on the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param rowToInsertOrUpdate
   *          the row to insert or update
   * @return a RowModification containing the (rowId, rowETag, table dataETag) after the modification
   */
  public RowModification insertOrUpdateRow(String tableId, SyncTag currentSyncTag, SyncRow rowToInsertOrUpdate)
      throws IOException;


  /**
   * Delete the given row ids from the server.
   *
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @param rowToDelete
   *          the row to delete
   * @return a RowModification containing the (rowId, null, table dataETag) after the modification
   */
  public RowModification deleteRow(String tableId, SyncTag currentSyncTag, SyncRow rowToDelete)
      throws IOException;

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