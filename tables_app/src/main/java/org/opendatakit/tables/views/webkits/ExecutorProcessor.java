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

import android.content.ContentValues;
import android.os.RemoteException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.data.Row;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.DataHelper;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.KeyValueStoreEntry;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.utils.TableUtil;

import java.io.IOException;
import java.util.*;

/**
 * @author mitchellsundt@gmail.com
 */
public abstract class ExecutorProcessor implements Runnable {
  private static final String TAG = "ExecutorProcessor";

  private static final List<String> ADMIN_COLUMNS;

  static {
    // everything is a STRING except for
    // CONFLICT_TYPE which is an INTEGER
    // see OdkDatabaseImplUtils.getUserDefinedTableCreationStatement()
    ArrayList<String> adminColumns = new ArrayList<String>();
    adminColumns.add(DataTableColumns.ID);
    adminColumns.add(DataTableColumns.ROW_ETAG);
    adminColumns.add(DataTableColumns.SYNC_STATE); // not exportable
    adminColumns.add(DataTableColumns.CONFLICT_TYPE); // not exportable
    adminColumns.add(DataTableColumns.FILTER_TYPE);
    adminColumns.add(DataTableColumns.FILTER_VALUE);
    adminColumns.add(DataTableColumns.FORM_ID);
    adminColumns.add(DataTableColumns.LOCALE);
    adminColumns.add(DataTableColumns.SAVEPOINT_TYPE);
    adminColumns.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
    adminColumns.add(DataTableColumns.SAVEPOINT_CREATOR);
    Collections.sort(adminColumns);
    ADMIN_COLUMNS = Collections.unmodifiableList(adminColumns);
  }

  private ExecutorContext context;

  private ExecutorRequest request;
  private OdkDbInterface dbInterface;
  private String transId;
  private OdkDbHandle dbHandle;

  protected ExecutorProcessor(ExecutorContext context) {
    this.context = context;
  }

  @Override
  public void run() {
    this.request = context.peekRequest();
    if (request == null) {
      // no work to do...
      return;
    }

    dbInterface = context.getDatabase();
    if (dbInterface == null) {
      // no database to do the work...
      return;
    }

    try {
      // we have a request and a viable database interface...
      transId = request.transId;
      if (transId == null) {
        if (request.executorRequestType == ExecutorRequestType.CLOSE_TRANSACTION) {
          context.reportError(request.callbackJSON, null, "Close Transaction did not specify a transId");
          context.popRequest();
          return;
        }

        dbHandle = dbInterface.openDatabase(context.getAppName(), true);
        if (dbHandle == null) {
          context.reportError(request.callbackJSON, null, "Unable to open database connection");
          context.popRequest();
          return;
        }

        transId = UUID.randomUUID().toString();
        context.registerActiveConnection(transId, dbHandle);
      } else {
        dbHandle = context.getActiveConnection(transId);
        if (dbHandle == null) {
          context.reportError(request.callbackJSON, null, "transId is no longer valid");
          context.popRequest();
          return;
        }
      }

      switch (request.executorRequestType) {
        case UPDATE_EXECUTOR_CONTEXT:
          updateExecutorContext();
          break;
        case RAW_QUERY:
          rawQuery();
          break;
        case USER_TABLE_QUERY:
          userTableQuery();
          break;
        case USER_TABLE_UPDATE_ROW:
          updateRow();
          break;
        case USER_TABLE_DELETE_ROW:
          deleteRow();
          break;
        case USER_TABLE_ADD_ROW:
          addRow();
          break;
        case USER_TABLE_ADD_CHECKPOINT:
          addCheckpoint();
          break;
        case USER_TABLE_SAVE_CHECKPOINT_AS_INCOMPLETE:
          saveCheckpointAsIncomplete();
          break;
        case USER_TABLE_SAVE_CHECKPOINT_AS_COMPLETE:
          saveCheckpointAsComplete();
          break;
        case USER_TABLE_DELETE_LAST_CHECKPOINT:
          deleteLastCheckpoint();
          break;
        case CLOSE_TRANSACTION:
          closeTransaction();
          break;
      }
    } catch (RemoteException e) {
      if (request.transId != null) {
        reportErrorAndCleanUp("unexpected remote exception");
      }
      // TODO: figure out what to do...
    }
  }

  /**
   * Handle the open/close transaction treatment for the database and report an error.
   *
   * @param errorMessage
   */
  private void reportErrorAndCleanUp(String errorMessage) {
    String transIdReported = transId;
    try {
      if ((request.leaveTransactionOpen == null) || (request.leaveTransactionOpen == false)) {
        transIdReported = null;
        dbInterface.closeTransactionAndDatabase(context.getAppName(), dbHandle, false);
      }
    } catch (RemoteException e) {
      WebLogger.getLogger(context.getAppName()).printStackTrace(e);
      WebLogger.getLogger(context.getAppName()).w(TAG, "error while releasing database conneciton");
    } finally {
      if (transIdReported == null) {
        context.removeActiveConnection(transId);
      }
      context.reportError(request.callbackJSON, transIdReported, errorMessage);
      context.popRequest();
    }
  }

  /**
   * Handle the open/close transaction treatment for the database and report a success.
   *
   * @param data
   * @param metadata
   */
  private void reportSuccessAndCleanUp(ArrayList<List<Object>> data, Map<String, Object> metadata) {
    boolean successful = false;
    String transIdReported = transId;
    try {
      if ((request.leaveTransactionOpen == null) || (request.leaveTransactionOpen == false)) {
        transIdReported = null;
        dbInterface.closeTransactionAndDatabase(context.getAppName(), dbHandle, true);
      }
      successful = true;
    } catch (RemoteException e) {
      WebLogger.getLogger(context.getAppName()).printStackTrace(e);
      WebLogger.getLogger(context.getAppName()).w(TAG, "error while releasing database connection");
    } finally {
      if (transIdReported == null) {
        context.removeActiveConnection(transId);
      }
      if (successful) {
        context.reportSuccess(request.callbackJSON, transIdReported, data, metadata);
      } else {
        context.reportError(request.callbackJSON, transIdReported,
                "error while commiting transaction and closing database");
      }
      context.popRequest();
    }
  }

  /**
   * Assumes incoming stringifiedJSON map only contains integers, doubles, strings, booleans
   * and arrays or string-value maps.
   *
   * @param columns
   * @param stringifiedJSON
   * @return ContentValues object drawn from stringifiedJSON
   */
  private ContentValues convertJSON(OrderedColumns columns, String stringifiedJSON) {
    ContentValues cvValues = new ContentValues();
    if (stringifiedJSON == null) {
      return cvValues;
    }
    try {
      HashMap map = ODKFileUtils.mapper.readValue(stringifiedJSON, HashMap.class);
      // populate cvValues from the map...
      for (Object okey : map.keySet()) {
        String key = (String) okey;
        Object value = map.get(key);
        if (value == null) {
          cvValues.putNull(key);
        } else if (value instanceof Integer) {
          cvValues.put(key, (Integer) value);
        } else if (value instanceof Double) {
          cvValues.put(key, (Double) value);
        } else if (value instanceof String) {
          cvValues.put(key, (String) value);
        } else if (value instanceof Boolean) {
          cvValues.put(key, (Boolean) value);
        } else if (value instanceof ArrayList) {
          cvValues.put(key, ODKFileUtils.mapper.writeValueAsString(value));
        } else {
          throw new IllegalStateException("unimplemented case");
        }
      }
      return cvValues;
    } catch (IOException e) {
      WebLogger.getLogger(context.getAppName()).printStackTrace(e);
      throw new IllegalStateException("should never be reached");
    }
  }

  private void updateExecutorContext() {
    context.releaseResources("switching to new WebFragment");
    context.popRequest();
  }

  private void rawQuery() {
    // TODO: implement this
  }

  private void userTableQuery() throws RemoteException {
    if (request.tableId == null) {
      reportErrorAndCleanUp("tableId cannot be null");
      return;
    }
    OrderedColumns columns = context.getOrderedColumns(request.tableId);
    if (columns == null) {
      columns = dbInterface.getUserDefinedColumns(context.getAppName(), dbHandle, request.tableId);
      context.putOrderedColumns(request.tableId, columns);
    }
    UserTable userTable = dbInterface.rawSqlQuery(context.getAppName(), dbHandle,
            request.tableId, columns, request.whereClause, request.sqlBindParams,
            request.groupBy, request.having, request.orderByElementKey, request.orderByDirection);


    List<KeyValueStoreEntry> entries = null;
    if (request.includeKeyValueStoreMap) {
      entries = dbInterface.getDBTableMetadata(context.getAppName(), dbHandle, request.tableId, null, null, null);
    }

    // assemble the data and metadata objects
    ArrayList<List<Object>> data = new ArrayList<List<Object>>();
    Map<String, Integer> elementKeyToIndexMap = userTable.getElementKeyMap();
    Map<String, Integer> rowIdMap = new HashMap<String, Integer>();

    OrderedColumns columnDefinitions = userTable.getColumnDefinitions();

    for (int i = 0; i < userTable.getNumberOfRows(); ++i) {
      Row r = userTable.getRowAtIndex(i);
      rowIdMap.put(r.getRowId(), i);
      List<Object> values = Arrays.asList(new Object[ADMIN_COLUMNS.size() + elementKeyToIndexMap.size()]);
      data.add(values);

      for (String name : ADMIN_COLUMNS) {
        int idx = elementKeyToIndexMap.get(name);
        if (name.equals(DataTableColumns.CONFLICT_TYPE)) {
          Integer value = r.getRawDataType(name, Integer.class);
          values.set(idx, value);
        } else {
          String value = r.getRawDataType(name, String.class);
          values.set(idx, value);
        }
      }

      ArrayList<String> elementKeys = columnDefinitions.getRetentionColumnNames();

      for (String name : elementKeys) {
        int idx = elementKeyToIndexMap.get(name);
        ColumnDefinition defn = columnDefinitions.find(name);
        ElementDataType dataType = defn.getType().getDataType();
        Class<?> clazz = ColumnUtil.get().getDataType(dataType);
        Object value = r.getRawDataType(name, clazz);
        values.set(idx, value);
      }
    }

    Map<String, Object> metadata = new HashMap<String, Object>();
    // rowId -> index into row list
    metadata.put("rowIdMap", rowIdMap);
    // elementKey -> index in row within row list
    metadata.put("elementKeyMap", elementKeyToIndexMap);
    // orderedColumns -- JS nested schema struct { elementName : extended_JS_schema_struct, ...}
    TreeMap<String, Object> orderedColumns = columnDefinitions.getDataModel();
    metadata.put("orderedColumns", orderedColumns);
    // keyValueStoreList
    if (entries != null) {
      // It is unclear how to most easily represent the KVS for access.
      // We use the convention in ODK Survey, which is a list of maps,
      // one per KVS row, with integer, number and boolean resolved to JS
      // types. objects and arrays are left unchanged.
      List<Map<String, Object>> kvsArray = new ArrayList<Map<String, Object>>();
      for (KeyValueStoreEntry entry : entries) {
        Object value = null;
        try {
          ElementDataType type = ElementDataType.valueOf(entry.type);
          if (entry.value != null) {
            if (type == ElementDataType.integer) {
              value = Integer.parseInt(entry.value);
            } else if (type == ElementDataType.bool) {
              value = DataHelper.intToBool(Integer.parseInt(entry.value));
            } else if (type == ElementDataType.number) {
              value = Double.parseDouble(entry.value);
            } else if (type == ElementDataType.string) {
              value = entry.value;
            } else {
              // array, object, rowpath, configpath
              value = entry.value;
            }
          }
        } catch (IllegalArgumentException e) {
          // ignore?
          value = entry.value;
          WebLogger.getLogger(context.getAppName()).e(TAG, "Unrecognized ElementDataType: " + entry.type);
          WebLogger.getLogger(context.getAppName()).printStackTrace(e);
        }

        Map<String, Object> anEntry = new HashMap<String, Object>();
        anEntry.put("partition", entry.partition);
        anEntry.put("aspect", entry.aspect);
        anEntry.put("key", entry.key);
        anEntry.put("type", entry.type);
        anEntry.put("value", value);

        kvsArray.add(anEntry);
      }
      metadata.put("keyValueStoreList", kvsArray);
    }

    // extend the metadata with whatever else this app needs....
    // e.g., row and column color maps
    extendQueryMetadata(entries, userTable, metadata);
    reportSuccessAndCleanUp(data, metadata);
  }

  protected abstract void extendQueryMetadata(List<KeyValueStoreEntry> entries, UserTable userTable, Map<String, Object> metadata);

  private void updateRow() throws RemoteException {
    if (request.tableId == null) {
      reportErrorAndCleanUp("tableId cannot be null");
      return;
    }
    if (request.rowId == null) {
      reportErrorAndCleanUp("rowId cannot be null");
      return;
    }
    OrderedColumns columns = context.getOrderedColumns(request.tableId);
    if (columns == null) {
      columns = dbInterface.getUserDefinedColumns(context.getAppName(), dbHandle, request.tableId);
      context.putOrderedColumns(request.tableId, columns);
    }

    ContentValues cvValues = convertJSON(columns, request.stringifiedJSON);
    dbInterface.updateDataInExistingDBTableWithId(context.getAppName(), dbHandle, request.tableId,
            columns, cvValues, request.rowId);

    reportSuccessAndCleanUp(null, null);
  }

  private void deleteRow() throws RemoteException {
    if (request.tableId == null) {
      reportErrorAndCleanUp("tableId cannot be null");
      return;
    }
    if (request.rowId == null) {
      reportErrorAndCleanUp("rowId cannot be null");
      return;
    }
    OrderedColumns columns = context.getOrderedColumns(request.tableId);
    if (columns == null) {
      columns = dbInterface.getUserDefinedColumns(context.getAppName(), dbHandle, request.tableId);
      context.putOrderedColumns(request.tableId, columns);
    }

    ContentValues cvValues = convertJSON(columns, request.stringifiedJSON);
    dbInterface.deleteDataInExistingDBTableWithId(context.getAppName(), dbHandle, request.tableId,
            request.rowId);

    reportSuccessAndCleanUp(null, null);
  }

  private void addRow() throws RemoteException {
    if (request.tableId == null) {
      reportErrorAndCleanUp("tableId cannot be null");
      return;
    }
    if (request.rowId == null) {
      reportErrorAndCleanUp("rowId cannot be null");
      return;
    }
    OrderedColumns columns = context.getOrderedColumns(request.tableId);
    if (columns == null) {
      columns = dbInterface.getUserDefinedColumns(context.getAppName(), dbHandle, request.tableId);
      context.putOrderedColumns(request.tableId, columns);
    }

    ContentValues cvValues = convertJSON(columns, request.stringifiedJSON);
    dbInterface.insertDataIntoExistingDBTableWithId(context.getAppName(), dbHandle, request.tableId,
            columns, cvValues, request.rowId);

    reportSuccessAndCleanUp(null, null);
  }

  private void addCheckpoint() {
    // TODO: implement this
  }

  private void saveCheckpointAsIncomplete() throws RemoteException {
    if (request.tableId == null) {
      reportErrorAndCleanUp("tableId cannot be null");
      return;
    }
    if (request.rowId == null) {
      reportErrorAndCleanUp("rowId cannot be null");
      return;
    }
    OrderedColumns columns = context.getOrderedColumns(request.tableId);
    if (columns == null) {
      columns = dbInterface.getUserDefinedColumns(context.getAppName(), dbHandle, request.tableId);
      context.putOrderedColumns(request.tableId, columns);
    }

    ContentValues cvValues = convertJSON(columns, request.stringifiedJSON);
    dbInterface.saveAsIncompleteMostRecentCheckpointDataInDBTableWithId(context.getAppName(), dbHandle, request.tableId,
            request.rowId);

    reportSuccessAndCleanUp(null, null);
  }

  private void saveCheckpointAsComplete() {
    // TODO: implement this
  }

  private void deleteLastCheckpoint() throws RemoteException {
    if (request.tableId == null) {
      reportErrorAndCleanUp("tableId cannot be null");
      return;
    }
    if (request.rowId == null) {
      reportErrorAndCleanUp("rowId cannot be null");
      return;
    }
    // TODO: implement this
    if (request.deleteAllCheckpoints != true) {
      reportErrorAndCleanUp("not yet implemented");
      return;
    }
    OrderedColumns columns = context.getOrderedColumns(request.tableId);
    if (columns == null) {
      columns = dbInterface.getUserDefinedColumns(context.getAppName(), dbHandle, request.tableId);
      context.putOrderedColumns(request.tableId, columns);
    }

    ContentValues cvValues = convertJSON(columns, request.stringifiedJSON);
    dbInterface.deleteCheckpointRowsWithId(context.getAppName(), dbHandle, request.tableId,
            request.rowId);

    reportSuccessAndCleanUp(null, null);
  }

  private void closeTransaction() throws RemoteException {
    dbInterface.closeTransactionAndDatabase(context.getAppName(), dbHandle, request.commitTransaction);
    context.removeActiveConnection(transId);
    context.reportSuccess(request.callbackJSON, null, null, null);
    context.popRequest();
  }

}
