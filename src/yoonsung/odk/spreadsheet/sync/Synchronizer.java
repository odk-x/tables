package yoonsung.odk.spreadsheet.sync;

import java.util.List;
import java.util.Map;

import yoonsung.odk.spreadsheet.data.ColumnProperties;

/**
 * Synchronizer abstracts synchronization of tables to an external cloud/server.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public interface Synchronizer {

  /**
   * Create a table with the given id on the server.
   * 
   * @param tableId
   *          the unique identifier of the table
   * @param cols
   *          a map from column names to column types, see
   *          {@link ColumnProperties.ColumnType}
   * @return a string which will be stored as the syncTag of the table
   */
  public String createTable(String tableId, Map<String, Integer> cols);

  /**
   * Delete the table with the given id from the server.
   * 
   * @param tableId
   *          the unique identifier of the table
   */
  public void deleteTable(String tableId);

  /**
   * Retrieve changes in the server state since the last synchronization.
   * 
   * @param tableId
   *          the unique identifier of the table
   * @param currentSyncTag
   *          the last value that was stored as the syncTag
   * @return an IncomingModification representing the latest state of the table
   */
  public IncomingModification getUpdates(String tableId, String currentSyncTag);

  /**
   * Insert the given rows in the table on the server.
   * 
   * @param tableId
   *          the unique identifier of the table
   * @param rowsToInsert
   *          the rows to insert
   * @return a Modification of the syncTags to save with the rows and table
   */
  public Modification insertRows(String tableId, List<SyncRow> rowsToInsert);

  /**
   * Update the given rows in the table on the server.
   * 
   * @param tableId
   *          the unique identifier of the table
   * @param rowsToUpdate
   *          the rows to update
   * @return a Modification of the syncTags to save with the rows and table
   */
  public Modification updateRows(String tableId, List<SyncRow> rowsToUpdate);

  /**
   * Delete the given row ids from the server.
   * 
   * @param tableId
   *          the unique identifier of the table
   * @param rowIds
   *          the row ids of the rows to delete
   * @return a string which will be stored as the syncTag of the table
   */
  public String deleteRows(String tableId, List<String> rowIds);

}