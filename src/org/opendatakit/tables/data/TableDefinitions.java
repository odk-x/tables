package org.opendatakit.tables.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * @author sudars
 *
 */
public class TableDefinitions {
  
  private static final String TAG = "TableDefinitions";
  
  /*
   * The name of the table in the database.
   */
  private static final String DB_BACKING_NAME = "table_definitions";
  
  /***********************************
   *  The names of columns in the table.
   ***********************************/
  public static final String DB_TABLE_ID = "table_id";
  public static final String DB_TABLE_KEY = "table_key";
  public static final String DB_DB_TABLE_NAME = "db_table_name";
  // DB_TYPE entries must be one of the types defined in TableType.
  public static final String DB_TYPE = "type";
  public static final String DB_TABLE_ID_ACCESS_CONTROLS = 
      "table_id_access_controls";
  public static final String DB_SYNC_TAG = "sync_tag";
  public static final String DB_LAST_SYNC_TIME = "last_sync_time";
  public static final String DB_SYNC_STATE = "sync_state";
  public static final String DB_TRANSACTIONING = "transactioning";
 
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
  private static final String WHERE_SQL_FOR_TABLE = DB_TABLE_ID + " = ?";
  
  private static final String WHERE_SQL_FOR_TABLE_TYPE = DB_TYPE + " = ? ";
  
  static {
    columnNames = new HashSet<String>();
    columnNames.add(DB_TABLE_ID);
    columnNames.add(DB_TABLE_KEY);
    columnNames.add(DB_DB_TABLE_NAME);
    columnNames.add(DB_TYPE);
    columnNames.add(DB_TABLE_ID_ACCESS_CONTROLS);
    columnNames.add(DB_SYNC_TAG);
    columnNames.add(DB_LAST_SYNC_TIME);
    columnNames.add(DB_SYNC_STATE);
    columnNames.add(DB_TRANSACTIONING);
        
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
      int dbTableIdIndex = c.getColumnIndexOrThrow(DB_TABLE_ID);
      int dbTableKeyIndex = c.getColumnIndexOrThrow(DB_TABLE_KEY);
      int dbDbTableNameIndex = c.getColumnIndexOrThrow(DB_DB_TABLE_NAME);
      int dbTypeIndex = c.getColumnIndexOrThrow(DB_TYPE);
      int dbTableIdAccessControlsIndex = 
          c.getColumnIndexOrThrow(DB_TABLE_ID_ACCESS_CONTROLS);
      int dbSyncTagIndex = c.getColumnIndexOrThrow(DB_SYNC_TAG);
      int dbLastSyncTimeIndex = c.getColumnIndexOrThrow(DB_LAST_SYNC_TIME);
      int dbSyncStateIndex = c.getColumnIndexOrThrow(DB_SYNC_STATE);
      int dbTransactioningIndex = c.getColumnIndexOrThrow(DB_TRANSACTIONING);
      
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
        tableDefMap.put(DB_TABLE_ID, c.getString(dbTableIdIndex));
        tableDefMap.put(DB_TABLE_KEY, c.getString(dbTableKeyIndex));
        tableDefMap.put(DB_DB_TABLE_NAME, c.getString(dbDbTableNameIndex));
        tableDefMap.put(DB_TYPE, c.getString(dbTypeIndex));
        tableDefMap.put(DB_TABLE_ID_ACCESS_CONTROLS, 
            c.getString(dbTableIdAccessControlsIndex));
        tableDefMap.put(DB_SYNC_TAG, c.getString(dbSyncTagIndex));
        tableDefMap.put(DB_LAST_SYNC_TIME, c.getString(dbLastSyncTimeIndex));
        tableDefMap.put(DB_SYNC_STATE, c.getString(dbSyncStateIndex));
        tableDefMap.put(DB_TRANSACTIONING, 
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
    values.put(DB_TABLE_ID, tableId);
    values.put(DB_TABLE_KEY, tableKey);
    values.put(DB_DB_TABLE_NAME, dbTableName);
    values.put(DB_TYPE, tableType.name());
    values.put(DB_TABLE_ID_ACCESS_CONTROLS, 
        DEFAULT_DB_TABLE_ID_ACCESS_CONTROLS);
    values.put(DB_SYNC_TAG, DEFAULT_DB_SYNC_TAG);
    values.put(DB_LAST_SYNC_TIME, DEFAULT_DB_LAST_SYNC_TIME);
    values.put(DB_SYNC_STATE, DEFAULT_DB_SYNC_STATE.name());
    values.put(DB_TRANSACTIONING, DEFAULT_DB_TRANSACTIONING);
    db.insert(DB_BACKING_NAME, null, values);    
    // Take care of the return.
    Map<String, String> valueMap = new HashMap<String, String>();
    valueMap.put(DB_TABLE_ID, tableId);
    valueMap.put(DB_TABLE_KEY, tableKey);
    valueMap.put(DB_DB_TABLE_NAME, dbTableName);
    valueMap.put(DB_TYPE, tableType.name());
    valueMap.put(DB_TABLE_ID_ACCESS_CONTROLS, 
        DEFAULT_DB_TABLE_ID_ACCESS_CONTROLS);
    valueMap.put(DB_SYNC_TAG, DEFAULT_DB_SYNC_TAG);
    valueMap.put(DB_LAST_SYNC_TIME, 
        Integer.toString(DEFAULT_DB_LAST_SYNC_TIME));
    valueMap.put(DB_SYNC_STATE, DEFAULT_DB_SYNC_STATE.name());
    valueMap.put(DB_TRANSACTIONING, 
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
        new String[] {DB_TABLE_ID}, 
        WHERE_SQL_FOR_TABLE_TYPE, 
        new String[] {tableType.name()}, null, null, null);
    int dbTableIdIndex = c.getColumnIndexOrThrow(DB_TABLE_ID);
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
    String create = "CREATE TABLE IF NOT EXISTS " + DB_BACKING_NAME + "(" +
        DB_TABLE_ID + " TEXT NOT NULL" + 
        ", " + DB_TABLE_KEY + " TEXT NOT NULL" +
        ", " + DB_DB_TABLE_NAME + " TEXT NOT NULL" +
        ", " + DB_TYPE + " TEXT NOT NULL" +
        ", " + DB_TABLE_ID_ACCESS_CONTROLS + " TEXT NOT NULL" +
        ", " + DB_SYNC_TAG + " TEXT NOT NULL" +
        // TODO last sync time should probably become an int?
        ", " + DB_LAST_SYNC_TIME + " TEXT NOT NULL" +
        ", " + DB_SYNC_STATE + " TEXT NOT NULL" +
        ", " + DB_TRANSACTIONING + " INTEGER NOT NULL" +
        ")";
    return create;
  }

}
