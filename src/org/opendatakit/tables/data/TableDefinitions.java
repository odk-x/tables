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
package org.opendatakit.tables.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendatakit.common.android.provider.TableDefinitionsColumns;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Responsible for the table_definitions table in the datastore. This table
 * holds the information about individual table entries--that is, user-defined
 * tables will all be represented here with a single row.
 * <p>
 * This table works together with the key value store to define a table's
 * properties. The traits here are considered part of the definition of the
 * table as opposed to various display and ODK Tables-specific properties.
 * @author sudar.sam@gmail.com
 *
 */
public class TableDefinitions {

  private static final String TAG = "TableDefinitions";

  /*
   * The name of the table in the database.
   */
  private static final String DB_BACKING_NAME = "table_definitions";

  /***********************************
   *  Default values for those columns which require them.
   ***********************************/
  public static final int DEFAULT_DB_LAST_SYNC_TIME = -1;
  public static final String DEFAULT_DB_SYNC_TAG = "";
  public static final SyncState DEFAULT_DB_SYNC_STATE = SyncState.inserting;
  public static final int DEFAULT_DB_TRANSACTIONING = 0;
  public static final String DEFAULT_DB_TABLE_ID_ACCESS_CONTROLS = "";

  /***********************************
   *  The type of the table.
   ***********************************/

  // A set of all the column names in this table.
  public static final Set<String> columnNames;

  /*
   * The SQL query for selecting the row specified by the table id.
   */
  private static final String WHERE_SQL_FOR_TABLE = TableDefinitionsColumns.TABLE_ID + " = ?";

  private static final String WHERE_SQL_FOR_TABLE_TYPE = TableDefinitionsColumns.TYPE + " = ? ";

  static {
    columnNames = new HashSet<String>();
    columnNames.add(TableDefinitionsColumns.TABLE_ID);
    columnNames.add(TableDefinitionsColumns.TABLE_KEY);
    columnNames.add(TableDefinitionsColumns.DB_TABLE_NAME);
    columnNames.add(TableDefinitionsColumns.TYPE);
    columnNames.add(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS);
    columnNames.add(TableDefinitionsColumns.SYNC_TAG);
    columnNames.add(TableDefinitionsColumns.LAST_SYNC_TIME);
    columnNames.add(TableDefinitionsColumns.SYNC_STATE);
    columnNames.add(TableDefinitionsColumns.TRANSACTIONING);

  }

  /**
   * Return an unmodifiable set that holds all the names for all the columns
   * in table_definitions.
   * @return
   */
  public static Set<String> getColumnNames() {
    return Collections.unmodifiableSet(columnNames);
  }

  /**
   * Return a map of columnName->Value for the row with the given table id.
   * TODO: perhaps this should become columnName->TypevValuePair like the rest
   * of these maps.
   * @param tableId
   * @param db
   * @return
   */
  public static Map<String, String> getFields(String tableId,
      SQLiteDatabase db) {
    Cursor c = null;
    Map<String, String> tableDefMap = new HashMap<String, String>();
    try {
      String[] arrColumnNames = new String[columnNames.size()];
      int i = 0;
      for (String colName : columnNames) {
        arrColumnNames[i] = colName;
        i++;
      }
      c = db.query(DB_BACKING_NAME, arrColumnNames, WHERE_SQL_FOR_TABLE,
          new String[] {tableId}, null, null, null);
      int dbTableIdIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.TABLE_ID);
      int dbTableKeyIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.TABLE_KEY);
      int dbDbTableNameIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.DB_TABLE_NAME);
      int dbTypeIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.TYPE);
      int dbTableIdAccessControlsIndex =
          c.getColumnIndexOrThrow(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS);
      int dbSyncTagIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.SYNC_TAG);
      int dbLastSyncTimeIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.LAST_SYNC_TIME);
      int dbSyncStateIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.SYNC_STATE);
      int dbTransactioningIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.TRANSACTIONING);

      if (c.getCount() > 1) {
        Log.e(TAG, "query for tableId: " + tableId + " returned >1 row in" +
        		"TableDefinitions");
      }
      if (c.getCount() < 1) {
        Log.e(TAG, "query for tableId: " + tableId + " returned <1 row in" +
            "TableDefinitions");
      }
      c.moveToFirst();
      int j = 0;
      while (j < c.getCount()) {
        tableDefMap.put(TableDefinitionsColumns.TABLE_ID, c.getString(dbTableIdIndex));
        tableDefMap.put(TableDefinitionsColumns.TABLE_KEY, c.getString(dbTableKeyIndex));
        tableDefMap.put(TableDefinitionsColumns.DB_TABLE_NAME, c.getString(dbDbTableNameIndex));
        tableDefMap.put(TableDefinitionsColumns.TYPE, c.getString(dbTypeIndex));
        tableDefMap.put(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS,
            c.getString(dbTableIdAccessControlsIndex));
        tableDefMap.put(TableDefinitionsColumns.SYNC_TAG, c.getString(dbSyncTagIndex));
        tableDefMap.put(TableDefinitionsColumns.LAST_SYNC_TIME, c.getString(dbLastSyncTimeIndex));
        tableDefMap.put(TableDefinitionsColumns.SYNC_STATE, c.getString(dbSyncStateIndex));
        tableDefMap.put(TableDefinitionsColumns.TRANSACTIONING,
            Integer.toString(c.getInt(dbTransactioningIndex)));
        c.moveToNext();
        j++;
      }
    } finally {
      try {
        if (c != null && !c.isClosed()) {
          c.close();
        }
      } finally {
        if (db != null) {
          // TODO: fix the when to close problem
//          db.close();
        }
      }

    }
    return tableDefMap;
  }

  /**
   * Set the value of the givenColumn name for the row where table_id matches
   * the passed in tableId.
   * <p>
   * Does not close the database.
   * @param tableId
   * @param columnName
   * @param newValue
   * @param db
   */
  public static void setValue(String tableId, String columnName, int newValue,
      SQLiteDatabase db) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue);
    db.update(DB_BACKING_NAME, values, WHERE_SQL_FOR_TABLE,
        new String[] {tableId});
  }

  /**
   * Set the value of the givenColumn name for the row where table_id matches
   * the passed in tableId.
   * <p>
   * Does not close the database.
   * @param tableId
   * @param columnName
   * @param newValue
   * @param db
   */
  public static void setValue(String tableId, String columnName,
      String newValue, SQLiteDatabase db) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue);
    db.update(DB_BACKING_NAME, values, WHERE_SQL_FOR_TABLE,
        new String[] {tableId});
  }

  /**
   * Add the table definition of the table to the TableDefinitions table in
   * the SQLite database. All of the values in {@TableDefinitions.columnNames}
   * are added. Those with values not passed in are set to their default values
   * as defined in TableDefinitions.
   * <p>
   * Returns a Map<String,String> of columnName->value. This is intended to
   * play nicely with {@link TableProperties}, which requires construction of
   * a TableProperties object with this information during table adding.
   * <p>
   * Does not close the passed in database.
   * TODO: check for redundant names
   * TODO: make this method also create the actual table for the rows.
   * TODO: make this return String->TypeValuePair, not the String represenation
   * of all the types.
   * @param db
   * @param tableId
   * @param tableKey
   * @param dbTableName
   * @param tableType
   * @param accessControls
   * @return a map of column names to fields for the new table
   */
  public static Map<String, String> addTable(SQLiteDatabase db, String tableId,
      String tableKey, String dbTableName, TableType tableType) {
    ContentValues values = new ContentValues();
    values.put(TableDefinitionsColumns.TABLE_ID, tableId);
    values.put(TableDefinitionsColumns.TABLE_KEY, tableKey);
    values.put(TableDefinitionsColumns.DB_TABLE_NAME, dbTableName);
    values.put(TableDefinitionsColumns.TYPE, tableType.name());
    values.put(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS,
        DEFAULT_DB_TABLE_ID_ACCESS_CONTROLS);
    values.put(TableDefinitionsColumns.SYNC_TAG, DEFAULT_DB_SYNC_TAG);
    values.put(TableDefinitionsColumns.LAST_SYNC_TIME, DEFAULT_DB_LAST_SYNC_TIME);
    values.put(TableDefinitionsColumns.SYNC_STATE, DEFAULT_DB_SYNC_STATE.name());
    values.put(TableDefinitionsColumns.TRANSACTIONING, DEFAULT_DB_TRANSACTIONING);
    db.insert(DB_BACKING_NAME, null, values);
    // Take care of the return.
    Map<String, String> valueMap = new HashMap<String, String>();
    valueMap.put(TableDefinitionsColumns.TABLE_ID, tableId);
    valueMap.put(TableDefinitionsColumns.TABLE_KEY, tableKey);
    valueMap.put(TableDefinitionsColumns.DB_TABLE_NAME, dbTableName);
    valueMap.put(TableDefinitionsColumns.TYPE, tableType.name());
    valueMap.put(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS,
        DEFAULT_DB_TABLE_ID_ACCESS_CONTROLS);
    valueMap.put(TableDefinitionsColumns.SYNC_TAG, DEFAULT_DB_SYNC_TAG);
    valueMap.put(TableDefinitionsColumns.LAST_SYNC_TIME,
        Integer.toString(DEFAULT_DB_LAST_SYNC_TIME));
    valueMap.put(TableDefinitionsColumns.SYNC_STATE, DEFAULT_DB_SYNC_STATE.name());
    valueMap.put(TableDefinitionsColumns.TRANSACTIONING,
        Integer.toString(DEFAULT_DB_TRANSACTIONING));
    return valueMap;
  }

  /**
   * Get all the tables of the given TableType that exist in the
   * TableDefinitions table.
   * <p>
   * NB: Does not close the database.
   * @param tableType
   * @return
   */
  public static List<String> getTableIdsForType(TableType tableType,
      SQLiteDatabase db) {
    Cursor c = db.query(DB_BACKING_NAME,
        new String[] {TableDefinitionsColumns.TABLE_ID},
        WHERE_SQL_FOR_TABLE_TYPE,
        new String[] {tableType.name()}, null, null, null);
    int dbTableIdIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.TABLE_ID);
    List<String> tableIds = new ArrayList<String>();
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      String tableId = c.getString(dbTableIdIndex);
      tableIds.add(tableId);
      i++;
      c.moveToNext();
    }
    c.close();
    return tableIds;
  }


  public static String getTableCreateSql() {
	return TableDefinitionsColumns.getTableCreateSql(DB_BACKING_NAME);
  }

}
