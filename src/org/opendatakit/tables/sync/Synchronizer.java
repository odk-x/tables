package org.opendatakit.tables.sync;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.data.ColumnProperties;


/**
 * Synchronizer abstracts synchronization of tables to an external cloud/server.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public interface Synchronizer {

  /**
   * Get a list of all tables in the server.
   * 
   * @return a map from table ids to table names
   */
  public Map<String, String> getTables() throws IOException;

  /**
   * Create a table with the given id on the server.
   * 
   * @param tableId
   *          the unique identifier of the table
   * @param tableName
   *          a human readable name for the table
   * @param cols
   *          a map from column names to column types, see
   *          {@link ColumnProperties.ColumnType}
   * @param tableProperties
   *          the table's properties, serialized as a string
   * @return a string which will be stored as the syncTag of the table
   */
  public String createTable(String tableId, String tableName, Map<String, Integer> cols,
      String tableProperties) throws IOException;

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
   * @param tableName
   *          the name of the table
   * @param tableProperties
   *          the table properties
   * @return a string which will be stored as the syncTag of the table
   * @throws IOException
   */
  public String setTableProperties(String tableId, String currentSyncTag, String tableName,
      String tableProperties) throws IOException;

}