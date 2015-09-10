/*
 * Copyright (C) 2015 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.views.webkits;

public class Data {

    private static final String TAG = Data.class.getSimpleName();

    private final ICallbackFragment mFragment;

    // TODO: make this true
    private final ExecutorContext context;

    public Data(ICallbackFragment fragment) {
      mFragment = fragment;
      context = new ExecutorContext(mFragment);
    }

    private void queueRequest(ExecutorRequest request) {
        context.queueRequest(request);
    }

    public Object getJavascriptInterfaceWithWeakReference()
    {
        return new DataIf(this);
    }

  /**
   * Access the result of a request
   *
   * @return null if there is no result, otherwise the responseJSON of the last action
   */
  public String getResponseJSON() {
    return mFragment.getResponseJSON();
  }

    /**
     * Query the database using sql.
     *
     * @param tableId  The table being queried. This is a user-defined table.
     * @param whereClause The where clause for the query
     * @param sqlBindParams The array of bind parameter values (including any in the having clause)
     * @param groupBy The array of columns to group by
     * @param having The having clause
     * @param orderByElementKey The column to order by
     * @param orderByDirection 'ASC' or 'DESC' ordering
     * @param includeKeyValueStoreMap true if the keyValueStoreMap should be returned
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void query(String tableId, String whereClause, String[] sqlBindParams,
                      String[] groupBy, String having, String orderByElementKey, String orderByDirection,
                      boolean includeKeyValueStoreMap,
                      String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(tableId, whereClause, sqlBindParams,
                groupBy, having, orderByElementKey, orderByDirection, includeKeyValueStoreMap,
                callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Raw SQL query
     *
     * @param sqlCommand The Select statement to issue. It can reference any table in the database, including system tables.
     * @param sqlBindParams The array of bind parameter values (including any in the having clause)
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     * @return see description in class header
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void rawQuery(String sqlCommand, String[] sqlBindParams,
                           String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(sqlCommand, sqlBindParams, callbackJSON,
                transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Update a row in the table
     *
     * @param tableId  The table being updated
     * @param stringifiedJSON key-value map of values to store or update. If missing, the value remains unchanged.
     * @param rowId The rowId of the row being changed.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     * @return see description in class header
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void updateRow(String tableId, String stringifiedJSON, String rowId,
                            String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(ExecutorRequestType.USER_TABLE_UPDATE_ROW,
                tableId, stringifiedJSON, rowId, callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Delete a row from the table
     *
     * @param tableId  The table being updated
     * @param stringifiedJSON key-value map of values to store or update. If missing, the value remains unchanged.
     * @param rowId The rowId of the row being deleted.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void deleteRow(String tableId, String stringifiedJSON, String rowId,
                            String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(ExecutorRequestType.USER_TABLE_DELETE_ROW,
                tableId, stringifiedJSON, rowId, callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }


    /**
     * Add a row in the table
     *
     * @param tableId  The table being updated
     * @param stringifiedJSON key-value map of values to store or update. If missing, the value remains unchanged.
     * @param rowId The rowId of the row being added.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void addRow(String tableId, String stringifiedJSON, String rowId,
                         String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(ExecutorRequestType.USER_TABLE_ADD_ROW,
                tableId, stringifiedJSON, rowId, callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Update the row, marking the updates as a checkpoint save.
     *
     * @param tableId  The table being updated
     * @param stringifiedJSON key-value map of values to store or update. If missing, the value remains unchanged.
     * @param rowId The rowId of the row being added.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void addCheckpoint (String tableId, String stringifiedJSON, String rowId,
                                 String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(ExecutorRequestType.USER_TABLE_ADD_CHECKPOINT,
                tableId, stringifiedJSON, rowId, callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Save checkpoint as incomplete. In the process, it applies any changes indicated by the stringifiedJSON.
     *
     * @param tableId  The table being updated
     * @param stringifiedJSON key-value map of values to store or update. If missing, the value remains unchanged.
     * @param rowId The rowId of the row being saved-as-incomplete.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void saveCheckpointAsIncomplete (String tableId, String stringifiedJSON, String rowId,
                                              String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(
                ExecutorRequestType.USER_TABLE_SAVE_CHECKPOINT_AS_INCOMPLETE,
                tableId, stringifiedJSON, rowId, callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Save checkpoint as complete. In the process, it applies any changes indicated by the stringifiedJSON.
     *
     * @param tableId  The table being updated
     * @param stringifiedJSON key-value map of values to store or update. If missing, the value remains unchanged.
     * @param rowId The rowId of the row being marked-as-complete.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void saveCheckpointAsComplete (String tableId, String stringifiedJSON, String rowId,
                                            String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(
                ExecutorRequestType.USER_TABLE_SAVE_CHECKPOINT_AS_COMPLETE,
                tableId, stringifiedJSON, rowId, callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Delete last checkpoint.  Checkpoints accumulate; this removes the most recent one, leaving earlier ones.
     *
     * @param tableId  The table being updated
     * @param rowId The rowId of the row being saved-as-incomplete.
     * @param deleteAllCheckpoints true if all checkpoints should be deleted, not just the last one.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public void deleteLastCheckpoint (String tableId, String rowId, boolean deleteAllCheckpoints,
                                        String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        ExecutorRequest request = new ExecutorRequest(ExecutorRequestType.USER_TABLE_DELETE_LAST_CHECKPOINT,
                tableId, rowId, deleteAllCheckpoints, callbackJSON, transId, leaveTransactionOpen);

        queueRequest(request);
    }

    /**
     * Close transaction
     *
     * @param transId the id of an open transaction.
     * @param commitTransaction true if the transaction should be committed; false if it should be rolled back.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     */
    public void closeTransaction(String transId, boolean commitTransaction, String callbackJSON) {
        ExecutorRequest request = new ExecutorRequest(transId, commitTransaction, callbackJSON);

        queueRequest(request);
    }

}
