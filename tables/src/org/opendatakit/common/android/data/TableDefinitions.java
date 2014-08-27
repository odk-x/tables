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
package org.opendatakit.common.android.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.provider.TableDefinitionsColumns;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;

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
  private static final String DB_BACKING_NAME =
      DataModelDatabaseHelper.TABLE_DEFS_TABLE_NAME;

  /***********************************
   *  Default values for those columns which require them.
   ***********************************/
  public static final int DEFAULT_DB_LAST_SYNC_TIME = -1;
  public static final String DEFAULT_DB_SYNC_TAG = "";
  public static final SyncState DEFAULT_DB_SYNC_STATE = SyncState.new_row;
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

  static {
    columnNames = new HashSet<String>();
    columnNames.add(TableDefinitionsColumns.TABLE_ID);
    columnNames.add(TableDefinitionsColumns.DB_TABLE_NAME);
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
   * @param db
   * @param tableId
   * @return
   */
  public static Map<String, String> getFields(SQLiteDatabase db, String tableId ) {
    Cursor c = null;
    Map<String, String> tableDefMap = new HashMap<String, String>();
    try {
      c = db.query(DB_BACKING_NAME, null, WHERE_SQL_FOR_TABLE,
          new String[] {tableId}, null, null, null);

      if (c.getCount() > 1) {
        Log.e(TAG, "query for tableId: " + tableId + " returned >1 row in" +
        		"TableDefinitions");
        throw new IllegalStateException("Multiple TableDefinitions for " + tableId);
      }
      if (c.getCount() < 1) {
        Log.e(TAG, "query for tableId: " + tableId + " returned <1 row in" +
            "TableDefinitions");
        return tableDefMap;
      }

      c.moveToFirst();
      for ( int j = 0 ; j < c.getColumnCount() ; ++j ) {
    	String columnName = c.getColumnName(j);
    	String value = ODKDatabaseUtils.getIndexAsString(c, j);
    	if ( columnNames.contains(columnName) ) {
    		tableDefMap.put(columnName, value);
    	}
      }

      return tableDefMap;
    } finally {
        if (c != null && !c.isClosed()) {
          c.close();
        }
    }
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
  public static void setValue(SQLiteDatabase db,
      String tableId, String columnName, int newValue) {
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
  public static void setValue(SQLiteDatabase db,
      String tableId, String columnName, String newValue) {
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
   * Does not close the passed in database.
   *
   * @param db
   * @param tableId
   * @param dbTableName
   */
  public static void addTable(SQLiteDatabase db, String tableId, String dbTableName) {
    ContentValues values = new ContentValues();
    values.put(TableDefinitionsColumns.TABLE_ID, tableId);
    values.put(TableDefinitionsColumns.DB_TABLE_NAME, dbTableName);
    values.put(TableDefinitionsColumns.SYNC_TAG, DEFAULT_DB_SYNC_TAG);
    values.put(TableDefinitionsColumns.LAST_SYNC_TIME, DEFAULT_DB_LAST_SYNC_TIME);
    values.put(TableDefinitionsColumns.SYNC_STATE, DEFAULT_DB_SYNC_STATE.name());
    values.put(TableDefinitionsColumns.TRANSACTIONING, DEFAULT_DB_TRANSACTIONING);
    db.insert(DB_BACKING_NAME, null, values);
  }

  public static List<String> getAllTableIds(SQLiteDatabase db) {
    Cursor c = null;
    try {
        c = db.query(true,  DB_BACKING_NAME, new String[] { TableDefinitionsColumns.TABLE_ID },
                null, null, null, null, null, null);
	    List<String> tableIds = new ArrayList<String>();
	    int dbTableIdIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.TABLE_ID);
	    if ( c.getCount() > 0 ) {
	      c.moveToFirst();
	      do {
	        String tableId = ODKDatabaseUtils.getIndexAsString(c, dbTableIdIndex);
	        tableIds.add(tableId);
	      } while ( c.moveToNext() );
	    }
	    return tableIds;
    } finally {
    	if ( c != null && !c.isClosed()) {
    		c.close();
    	}
    }
  }

  public static List<String> getAllDbTableNames(SQLiteDatabase db) {
    Cursor c = null;
    try {
        c = db.query(true,  DB_BACKING_NAME, new String[] { TableDefinitionsColumns.DB_TABLE_NAME },
                null, null, null, null, null, null);
       List<String> tableNames = new ArrayList<String>();
       int dbTableNameIndex = c.getColumnIndexOrThrow(TableDefinitionsColumns.DB_TABLE_NAME);
       if ( c.getCount() > 0 ) {
         c.moveToFirst();
         do {
           String tableName = ODKDatabaseUtils.getIndexAsString(c, dbTableNameIndex);
           tableNames.add(tableName);
         } while ( c.moveToNext() );
       }
       return tableNames;
    } finally {
      if ( c != null && !c.isClosed()) {
         c.close();
      }
    }
  }

  /**
   * Remove the given tableId from the TableDefinitions table. This does NOT
   * handle any of the necessary deletion of the table's information in other
   * tables.
   * <p>
   * Does not close the database.
   * @param tableId
   * @param db
   * @return
   */
  public static int deleteTableFromTableDefinitions(SQLiteDatabase db, String tableId) {
    int count = db.delete(DB_BACKING_NAME, WHERE_SQL_FOR_TABLE,
        new String[] {tableId});
    if (count != 1) {
      Log.e(TAG, "deleteTable() for tableId [" + tableId + "] deleted " +
          " rows");
    } else {
      Log.d(TAG, "deleted table with id: " + tableId);
    }
    return count;
  }

}
