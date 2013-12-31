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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.provider.ColumnDefinitionsColumns;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Holds the immutable column properties. In essence this represents the
 * backing table in the database. This should be acted upon via
 * {@link ColumnDefinitions}.
 * <p>
 * This is the column analogue to TableDefinitions.
 * @author sudar.sam@gmail.com
 *
 */
public class ColumnDefinitions {

  private static final String TAG = "ColumnDefinitions";

  private static final String DB_BACKING_NAME =
      DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME;

  /***********************************
   *  Default values for those columns which require them.
   ***********************************/
  public static final boolean DEFAULT_DB_IS_UNIT_OF_RETENTION = true;
  public static final ColumnType DEFAULT_DB_ELEMENT_TYPE = ColumnType.NONE;
  public static final String DEFAULT_LIST_CHILD_ELEMENT_KEYS = null;

  // A set of all the column names in this table.
  public static final Set<String> columnNames;

  // Populate the set.
  static {
    columnNames = new HashSet<String>();
    columnNames.add(ColumnDefinitionsColumns.TABLE_ID);
    columnNames.add(ColumnDefinitionsColumns.ELEMENT_KEY);
    columnNames.add(ColumnDefinitionsColumns.ELEMENT_NAME);
    columnNames.add(ColumnDefinitionsColumns.ELEMENT_TYPE);
    columnNames.add(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS);
    columnNames.add(ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION);
  }

  /*
   * Get the where sql clause for the table id and the given element key.
   */
  private static final String WHERE_SQL_FOR_ELEMENT =
      ColumnDefinitionsColumns.TABLE_ID + " = ? AND " + ColumnDefinitionsColumns.ELEMENT_KEY + " = ?";

  private static final String WHERE_SQL_FOR_TABLE_IS_UNIT_OF_RETENTION =
      ColumnDefinitionsColumns.TABLE_ID + " = ? AND " + ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION + " = ?";

  private static final String WHERE_SQL_FOR_TABLE =
      ColumnDefinitionsColumns.TABLE_ID + " = ?";

  /**
   * Return an unmodifiable set that holds all the names for the columns in
   * DB_TABLENAME. Note that these are the column names of the actual table
   * defined in ColumnDefinitions, NOT the user-defined column names that are
   * defined in the ColumnDefinitions table.
   * @return
   */
  public static Set<String> getColumnNames() {
    return Collections.unmodifiableSet(columnNames);
  }

  /**
   * Return all the column names for the given table.  These will be
   * the element keys that are 'units of retention' (stored as columns in
   * the database) AND the element keys that define super- or sub- structural
   * elements such as composite types whose sub-elements are written
   * individually to the database (e.g., geopoint) or subsumed by the
   * enclosing element (e.g., lists of items).
   * <p>
   * Does not close the passed in database.
   * @param tableId
   * @param db
   * @return
   */
  public static List<String> getAllColumnNamesForTable(String tableId,
      SQLiteDatabase db) {
    Cursor c = null;
    List<String> elementKeys = new ArrayList<String>();
    try {
      c = db.query(DB_BACKING_NAME,
          new String[] {ColumnDefinitionsColumns.ELEMENT_KEY}, // we only want the element key column
          WHERE_SQL_FOR_TABLE,
          new String[] {tableId}, null, null, null);
      int dbElementKeyIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_KEY);
      c.moveToFirst();
      int j = 0;
      while (j < c.getCount()) {
        elementKeys.add(c.getString(dbElementKeyIndex));
        c.moveToNext();
        j++;
      }
    } finally {
        if (c != null && !c.isClosed()) {
          c.close();
        }
    }
    return elementKeys;
  }

  /**
   * Return a map of columnName->Value for the row with the given table id and
   * elementKey.
   * TODO: perhaps this should become columnName->TypevValuePair like the rest
   * of these maps.
   * @param tableId
   * @param db
   * @return
   */
  public static Map<String, String> getColumnDefinitionFields(String tableId,
      String elementKey, SQLiteDatabase db) {
    Cursor c = null;
    Map<String, String> columnDefMap = new HashMap<String, String>();
    try {
      String[] arrColumnNames = new String[columnNames.size()];
      int i = 0;
      for (String colName : columnNames) {
        arrColumnNames[i] = colName;
        i++;
      }
      c = db.query(DB_BACKING_NAME, arrColumnNames, WHERE_SQL_FOR_ELEMENT,
          new String[] {tableId, elementKey}, null, null, null);
      int dbTableIdIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.TABLE_ID);
      int dbElementKeyIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_KEY);
      int dbElementNameIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_NAME);
      int dbElementTypeIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_TYPE);
      int dbListChildElementKeysIndex =
          c.getColumnIndexOrThrow(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS);
      int dbUnitOfRetentionIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION);

      if (c.getCount() > 1) {
        Log.e(TAG, "query for tableId: " + tableId + "and elementKey: " +
            elementKey + " returned >1 row in" +
            "ColumnDefinitions");
      }
      if (c.getCount() < 1) {
        Log.e(TAG, "query for tableId: " + tableId + "and elementKey: " +
            elementKey + " returned <1 row in" +
            "ColumnDefinitions");
      }

      c.moveToFirst();
      int j = 0;
      while (j < c.getCount()) {
        columnDefMap.put(ColumnDefinitionsColumns.TABLE_ID, c.getString(dbTableIdIndex));
        columnDefMap.put(ColumnDefinitionsColumns.ELEMENT_KEY, c.getString(dbElementKeyIndex));
        columnDefMap.put(ColumnDefinitionsColumns.ELEMENT_NAME, c.getString(dbElementNameIndex));
        columnDefMap.put(ColumnDefinitionsColumns.ELEMENT_TYPE, c.getString(dbElementTypeIndex));
        columnDefMap.put(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS,
            c.getString(dbListChildElementKeysIndex));
        columnDefMap.put(ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION, Boolean.toString(
            c.getInt(dbUnitOfRetentionIndex) == 1));
        c.moveToNext();
        j++;
      }
    } finally {
      if (c != null && !c.isClosed()) {
        c.close();
      }
    }
    return columnDefMap;
  }

  /**
   * Set the value for the given column name for the row that matches the
   * passed in tableId and the passed in element Key.
   * <p>
   * Does not close the database.
   * @param tableId
   * @param elementKey
   * @param columnName
   * @param newValue
   * @param db
   */
  public static void setValue(String tableId, String elementKey,
      String columnName, String newValue, SQLiteDatabase db) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue);
    db.update(DB_BACKING_NAME, values, WHERE_SQL_FOR_ELEMENT,
        new String[] {tableId, elementKey});
  }

  /**
   * Set the value for the given column name for the row that matches the
   * passed in tableId and the passed in element Key.
   * <p>
   * Does not close the database.
   * @param tableId
   * @param elementKey
   * @param columnName
   * @param newValue
   * @param db
   */
  public static void setValue(String tableId, String elementKey,
      String columnName, int newValue, SQLiteDatabase db) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue);
    db.update(DB_BACKING_NAME, values, WHERE_SQL_FOR_ELEMENT,
        new String[] {tableId, elementKey});
  }

  /**
   * Set the value for the given column name for the row that matches the
   * passed in tableId and the passed in element Key.
   * <p>
   * Does not close the database.
   * @param tableId
   * @param elementKey
   * @param columnName
   * @param newValue
   * @param db
   */
  public static void setValue(String tableId, String elementKey,
      String columnName, boolean newValue, SQLiteDatabase db) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue ? 1 : 0);
    db.update(DB_BACKING_NAME, values, WHERE_SQL_FOR_ELEMENT,
        new String[] {tableId, elementKey});
  }

  /**
   * Add the definition of the column to the column_definitions table in
   * the SQLite database. All of the values in {@ColumnDefinitions.columnNames}
   * are added. Those with values not passed in are set to their default values
   * as defined in ColumnDefinitions.
   * <p>
   * Does NOT restructure the user table to add the column. Only adds a column
   * to column_definitions.
   * <p>
   * Returns a Map<String,String> of columnName->value. This is intended to
   * play nicely with {@link ColumnProperties}, which requires construction of
   * a ColumnProperties object with this information during table adding.
   * <p>
   * Does not close the passed in database.
   * TODO: check for redundant names
   * TODO: make this method also create the actual table for the rows.
   * TODO: make this return String->TypeValuePair, not the String represenation
   * of all the types.
   * @param db
   * @param tableId
   * @param elementKey
   * @param elementName
   * @param elementType type of the column. null values will be converted to
   * DEFAULT_DB_ELEMENT_TYPE
   * @param listChild
   * @param isUnitOfRetention
   * @return a map of column names to fields for the new table
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonGenerationException
   */
  public static void assertColumnDefinition(SQLiteDatabase db,
      String tableId, String elementKey, String elementName,
      ColumnType elementType, String listChild, boolean isUnitOfRetention)
          throws JsonGenerationException, JsonMappingException, IOException {
    ContentValues values = new ContentValues();
    values.put(ColumnDefinitionsColumns.ELEMENT_NAME, elementName);
    values.put(ColumnDefinitionsColumns.ELEMENT_TYPE, elementType.name());
    values.put(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS, listChild);
    values.put(ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION, isUnitOfRetention ? 1 : 0);

    Cursor c = null;
    try {
	    c = db.query(DB_BACKING_NAME, null, WHERE_SQL_FOR_ELEMENT,
	                  new String[] {tableId, elementKey}, null, null, null);
	    int count = c.getCount();
	    c.close();
	    if ( count == 1 ) {
	      // update...
	      db.update(DB_BACKING_NAME, values, WHERE_SQL_FOR_ELEMENT,
	          new String[] {tableId, elementKey});
	    } else {
	      if ( count > 1 ) {
	        // remove and re-insert...
	        deleteColumnDefinition(tableId, elementKey, db);
	      }
	      // insert...
	      values.put(ColumnDefinitionsColumns.TABLE_ID, tableId);
	      values.put(ColumnDefinitionsColumns.ELEMENT_KEY, elementKey);
	      db.insert(DB_BACKING_NAME, null, values);
	    }
    } finally {
    	if ( c != null && !c.isClosed()) {
    		c.close();
    	}
    }
  }

  /**
   * Delete the column definition from the column definitions table by using
   * its element key. This truly deletes the column.
   * @param tableId
   * @param elementKey
   * @param db
   * @return
   */
  public static int deleteColumnDefinition(String tableId, String elementKey,
      SQLiteDatabase db) {
    int count = db.delete(DB_BACKING_NAME, WHERE_SQL_FOR_ELEMENT,
        new String[] {tableId, elementKey});
    if (count != 1) {
      Log.e(TAG, "deleteColumn() deleted " + count + " rows");
    }
    return count;
  }

}
