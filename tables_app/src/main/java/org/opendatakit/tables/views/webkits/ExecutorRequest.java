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

/**
 * @author mitchellsundt@gmail.com
 */
public class ExecutorRequest {


    public final ExecutorRequestType executorRequestType;

    // To clear an older context
    public final ExecutorContext oldContext;

    // For raw query interaction
    public final String sqlCommand;
    // String[] sqlBindParams; // shared

    // For user table interactions
    public final String tableId;

    // For user table query interaction
    public final String whereClause;
    public final String[] sqlBindParams;
    public final String[] groupBy;
    public final String having;
    public final String orderByElementKey;
    public final String orderByDirection;
    public final boolean includeKeyValueStoreMap;

    // For user table modification interactions
    public final String stringifiedJSON;
    public final String rowId;

    // For checkpoint delete interations
    public final boolean deleteAllCheckpoints;

    // For commit interaction
    public final boolean commitTransaction;

    // For most interactions
    public final String callbackJSON;
    public final String transId;
    public final Boolean leaveTransactionOpen;

    public ExecutorRequest(ExecutorContext oldContext) {
        this.executorRequestType = ExecutorRequestType.UPDATE_EXECUTOR_CONTEXT;
        this.oldContext = oldContext;

        // unused:
        this.sqlCommand = null;
        this.sqlBindParams = null;
        this.callbackJSON = null;
        this.transId = null;
        this.leaveTransactionOpen = null;
        this.tableId = null;
        this.whereClause = null;
        this.groupBy = null;
        this.having = null;
        this.orderByElementKey = null;
        this.orderByDirection = null;
        this.includeKeyValueStoreMap = false;
        this.stringifiedJSON = null;
        this.rowId = null;
        this.deleteAllCheckpoints = false;
        this.commitTransaction = false;
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
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public ExecutorRequest(String sqlCommand, String[] sqlBindParams,
                           String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        this.executorRequestType = ExecutorRequestType.RAW_QUERY;
        this.sqlCommand = sqlCommand;
        this.sqlBindParams = sqlBindParams;
        this.callbackJSON = callbackJSON;
        this.transId = transId;
        this.leaveTransactionOpen = leaveTransactionOpen;

        // unused:
        this.oldContext = null;
        this.tableId = null;
        this.whereClause = null;
        this.groupBy = null;
        this.having = null;
        this.orderByElementKey = null;
        this.orderByDirection = null;
        this.includeKeyValueStoreMap = false;
        this.stringifiedJSON = null;
        this.rowId = null;
        this.deleteAllCheckpoints = false;
        this.commitTransaction = false;
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
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public ExecutorRequest(String tableId, String whereClause, String[] sqlBindParams,
                           String[] groupBy, String having, String orderByElementKey, String orderByDirection,
                           boolean includeKeyValueStoreMap,
                           String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        this.executorRequestType = ExecutorRequestType.USER_TABLE_QUERY;
        this. tableId = tableId;
        this.whereClause = whereClause;
        this.sqlBindParams = sqlBindParams;
        this.groupBy = groupBy;
        this.having = having;
        this.orderByElementKey = orderByElementKey;
        this.orderByDirection = orderByDirection;
        this.includeKeyValueStoreMap = includeKeyValueStoreMap;
        this.callbackJSON = callbackJSON;
        this.transId = transId;
        this.leaveTransactionOpen = leaveTransactionOpen;

        // unused:
        this.oldContext = null;
        this.sqlCommand = null;
        this.stringifiedJSON = null;
        this.rowId = null;
        this.deleteAllCheckpoints = false;
        this.commitTransaction = false;
    }

    /**
     * Add or modify a row in the table
     *
     * @param executorRequestType The type of request. One of:
     *                    <ul><li>USER_TABLE_UPDATE_ROW</li>
     *                    <li>USER_TABLE_DELETE_ROW</li>
     *                    <li>USER_TABLE_ADD_ROW</li>
     *                    <li>USER_TABLE_ADD_CHECKPOINT</li>
     *                    <li>USER_TABLE_SAVE_CHECKPOINT_AS_INCOMPLETE</li>
     *                    <li>USER_TABLE_SAVE_CHECKPOINT_AS_COMPLETE</li>
     *                    <li>USER_TABLE_DELETE_LAST_CHECKPOINT</li></ul>
     * @param tableId  The table being updated
     * @param stringifiedJSON key-value map of values to store or update. If missing, the value remains unchanged.
     *                        This field is ignored when performing USER_TABLE_DELETE_LAST_CHECKPOINT.
     * @param rowId The rowId of the row being deleted.
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public ExecutorRequest(ExecutorRequestType executorRequestType, String tableId, String stringifiedJSON, String rowId,
                            String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        this.executorRequestType = executorRequestType;
        this.tableId = tableId;
        this.stringifiedJSON = stringifiedJSON;
        this.rowId = rowId;
        this.callbackJSON = callbackJSON;
        this.transId = transId;
        this.leaveTransactionOpen = leaveTransactionOpen;

        // unused:
        this.oldContext = null;
        this.sqlCommand = null;
        this.whereClause = null;
        this.sqlBindParams = null;
        this.groupBy = null;
        this.having = null;
        this.orderByElementKey = null;
        this.orderByDirection = null;
        this.includeKeyValueStoreMap = false;
        this.deleteAllCheckpoints = false;
        this.commitTransaction = false;
    }

    /**
     * Add or modify a row in the table
     *
     * @param executorRequestType The type of request. Must be:
     *                    <ul><li>USER_TABLE_DELETE_LAST_CHECKPOINT</li></ul>
     * @param tableId  The table being updated
     * @param rowId The rowId of the row being deleted.
     * @param deleteAllCheckpoints true if all checkpoints should be deleted, not just the last one.
     * @param transId null or the id of an open transaction if action should occur on an existing transaction.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     * @param leaveTransactionOpen null or false close the transaction or use a transient one. true will return
     *                             the transId and leave transaction open.
     *
     * transId and leaveTransactionOpen are used only if the user wants to explicitly control db transactions
     */
    public ExecutorRequest(ExecutorRequestType executorRequestType, String tableId, String rowId, boolean deleteAllCheckpoints,
                           String callbackJSON, String transId, Boolean leaveTransactionOpen) {
        if ( executorRequestType != ExecutorRequestType.USER_TABLE_DELETE_LAST_CHECKPOINT) {
            throw new IllegalArgumentException("expected USER_TABLE_DELETE_LAST_CHECKPOINT");
        }
        this.executorRequestType = executorRequestType;
        this.tableId = tableId;
        this.rowId = rowId;
        this.deleteAllCheckpoints = deleteAllCheckpoints;
        this.callbackJSON = callbackJSON;
        this.transId = transId;
        this.leaveTransactionOpen = leaveTransactionOpen;

        // unused:
        this.oldContext = null;
        this.sqlCommand = null;
        this.whereClause = null;
        this.sqlBindParams = null;
        this.groupBy = null;
        this.having = null;
        this.orderByElementKey = null;
        this.orderByDirection = null;
        this.includeKeyValueStoreMap = false;
        this.stringifiedJSON = null;
        this.commitTransaction = false;
    }

    /**
     * Close transaction
     *
     * @param transId the id of an open transaction.
     * @param commitTransaction true if the transaction should be committed; false if it should be rolled back.
     * @param callbackJSON The JSON object used by the JS layer to recover the callback function
     *                     that can process the response
     */
    public ExecutorRequest(String transId, boolean commitTransaction, String callbackJSON) {
        this.executorRequestType = ExecutorRequestType.CLOSE_TRANSACTION;
        this.transId = transId;
        this.commitTransaction = false;
        this.callbackJSON = callbackJSON;

        // unused:
        this.oldContext = null;
        this.sqlCommand = null;
        this.tableId = null;
        this.whereClause = null;
        this.sqlBindParams = null;
        this.groupBy = null;
        this.having = null;
        this.orderByElementKey = null;
        this.orderByDirection = null;
        this.includeKeyValueStoreMap = false;
        this.stringifiedJSON = null;
        this.rowId = null;
        this.deleteAllCheckpoints = false;
        this.leaveTransactionOpen = null;
    }
}
