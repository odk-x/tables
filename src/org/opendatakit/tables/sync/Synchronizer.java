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
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.entity.Column;
import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.tables.data.ColumnType;


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
   * @return a map from table ids to tableKeys
   */
  public Map<String, String> getTables() throws IOException;

  /**
   * Create a table with the given id on the server.
   * 
   * @param tableId
   *          the unique identifier of the table
   * @param cols
   *          a map from column names to column types, see
   *          {@link ColumnType}
   * @param tableKey
   *          the tableKey (as of May6 is the display name on the server)
   * @param dbTable name
   * @param type
   *           {@link org.opendatakit.aggregate.odktables.entity.api.TableType} 
   *           represetenting the table type
   * @param tableIdAccessControls
   *           the tableId of the table holding access control information on
   *           the table.
   * @return a string which will be stored as the syncTag of the table
   */
  public String createTable(String tableId, List<Column> columns, 
      String tableKey, String dbTableName, 
      org.opendatakit.aggregate.odktables.entity.api.TableType type, 
      String tableIdAccessControls) throws IOException;

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
  public IncomingModification getUpdates(String tableId, String currentSyncTag) throws IOException;

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
  public Modification insertRows(String tableId, String currentSyncTag, List<SyncRow> rowsToInsert)
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
  public Modification updateRows(String tableId, String currentSyncTag, List<SyncRow> rowsToUpdate)
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
   * @return a string which will be stored as the syncTag of the table
   */
  public String deleteRows(String tableId, String currentSyncTag, List<String> rowIds)
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
   *           all the entries in the key value store for this table. Should 
   *           be of the server kvs, since this is for synchronization.
   * @return a string which will be stored as the syncTag of the table
   * @throws IOException
   */
  public String setTableProperties(String tableId, String currentSyncTag, 
      String tableName, List<OdkTablesKeyValueStoreEntry> kvsEntries) 
          throws IOException;

}