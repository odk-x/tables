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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.provider.ColumnDefinitionsColumns;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;

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

  // A set of all the column names in this table.
  private static final Set<String> columnNames;

  // Populate the set.
  static {
    columnNames = new HashSet<String>();
    columnNames.add(ColumnDefinitionsColumns.TABLE_ID);
    columnNames.add(ColumnDefinitionsColumns.ELEMENT_KEY);
    columnNames.add(ColumnDefinitionsColumns.ELEMENT_NAME);
    columnNames.add(ColumnDefinitionsColumns.ELEMENT_TYPE);
    columnNames.add(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS);
  }

  /*
   * Get the where sql clause for the table id and the given element key.
   */
  private static final String WHERE_SQL_FOR_ELEMENT =
      ColumnDefinitionsColumns.TABLE_ID + " = ? AND " + ColumnDefinitionsColumns.ELEMENT_KEY + " = ?";

  private static final String WHERE_SQL_FOR_TABLE =
      ColumnDefinitionsColumns.TABLE_ID + " = ?";


  /**
   * Predicate used to determine whether the column property is held in the
   * ColumnDefinitions table or in the key-value aspect for that column.
   *
   * @param propertyName
   * @return
   */
  public static final boolean contains(String propertyName) {
    return columnNames.contains(propertyName);
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
   * @param db
   * @param tableId
   * @return
   */
  public static List<String> getAllColumnNamesForTable(SQLiteDatabase db, String tableId ) {
    Cursor c = null;
    List<String> elementKeys = new ArrayList<String>();
    try {
      c = db.query(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
          new String[] {ColumnDefinitionsColumns.ELEMENT_KEY}, // we only want the element key column
          WHERE_SQL_FOR_TABLE,
          new String[] {tableId}, null, null, null);
      int dbElementKeyIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_KEY);
      c.moveToFirst();
      int j = 0;
      while (j < c.getCount()) {
        elementKeys.add(ODKDatabaseUtils.getIndexAsString(c, dbElementKeyIndex));
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
   * @param db
   * @param tableId
   * @param elementKey
   * @return
   */
  public static Map<String, String> getColumnDefinitionFields(SQLiteDatabase db,
      String tableId, String elementKey) {
    Cursor c = null;
    Map<String, String> columnDefMap = new HashMap<String, String>();
    try {
      String[] arrColumnNames = new String[columnNames.size()];
      int i = 0;
      for (String colName : columnNames) {
        arrColumnNames[i] = colName;
        i++;
      }
      c = db.query(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
          arrColumnNames, WHERE_SQL_FOR_ELEMENT,
          new String[] {tableId, elementKey}, null, null, null);
      int dbTableIdIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.TABLE_ID);
      int dbElementKeyIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_KEY);
      int dbElementNameIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_NAME);
      int dbElementTypeIndex = c.getColumnIndexOrThrow(ColumnDefinitionsColumns.ELEMENT_TYPE);
      int dbListChildElementKeysIndex =
          c.getColumnIndexOrThrow(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS);

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
        columnDefMap.put(ColumnDefinitionsColumns.TABLE_ID, ODKDatabaseUtils.getIndexAsString(c, dbTableIdIndex));
        columnDefMap.put(ColumnDefinitionsColumns.ELEMENT_KEY, ODKDatabaseUtils.getIndexAsString(c, dbElementKeyIndex));
        columnDefMap.put(ColumnDefinitionsColumns.ELEMENT_NAME, ODKDatabaseUtils.getIndexAsString(c, dbElementNameIndex));
        columnDefMap.put(ColumnDefinitionsColumns.ELEMENT_TYPE, ODKDatabaseUtils.getIndexAsString(c, dbElementTypeIndex));
        columnDefMap.put(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS,
            ODKDatabaseUtils.getIndexAsString(c, dbListChildElementKeysIndex));
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
   * @param db
   * @param tableId
   * @param elementKey
   * @param columnName
   * @param newValue
   */
  public static void setValue(SQLiteDatabase db, String tableId, String elementKey,
      String columnName, String newValue) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue);
    db.update(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
        values, WHERE_SQL_FOR_ELEMENT,
        new String[] {tableId, elementKey});
  }

  /**
   * Set the value for the given column name for the row that matches the
   * passed in tableId and the passed in element Key.
   * <p>
   * Does not close the database.
   * @param db
   * @param tableId
   * @param elementKey
   * @param columnName
   * @param newValue
   */
  public static void setValue(SQLiteDatabase db, String tableId, String elementKey,
      String columnName, int newValue) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue);
    db.update(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
        values, WHERE_SQL_FOR_ELEMENT,
        new String[] {tableId, elementKey});
  }

  /**
   * Set the value for the given column name for the row that matches the
   * passed in tableId and the passed in element Key.
   * <p>
   * Does not close the database.
   * @param db
   * @param tableId
   * @param elementKey
   * @param columnName
   * @param newValue
   */
  public static void setValue(SQLiteDatabase db, String tableId, String elementKey,
      String columnName, boolean newValue) {
    ContentValues values = new ContentValues();
    values.put(columnName, newValue ? 1 : 0);
    db.update(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
        values, WHERE_SQL_FOR_ELEMENT,
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
   * @return a map of column names to fields for the new table
   */
  public static void assertColumnDefinition(SQLiteDatabase db,
      String tableId, String elementKey, String elementName,
      ColumnType elementType, String listChild) {
    ContentValues values = new ContentValues();
    values.put(ColumnDefinitionsColumns.ELEMENT_NAME, elementName);
    values.put(ColumnDefinitionsColumns.ELEMENT_TYPE, elementType.name());
    values.put(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS, listChild);

    Cursor c = null;
    try {
	    c = db.query(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
	                 null, WHERE_SQL_FOR_ELEMENT,
	                 new String[] {tableId, elementKey}, null, null, null);
	    int count = c.getCount();
	    c.close();
	    if ( count == 1 ) {
	      // update...
	      db.update(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
	          values, WHERE_SQL_FOR_ELEMENT,
	          new String[] {tableId, elementKey});
	    } else {
	      if ( count > 1 ) {
	        // remove and re-insert...
	        deleteColumnDefinition(db, tableId, elementKey);
	      }
	      // insert...
	      values.put(ColumnDefinitionsColumns.TABLE_ID, tableId);
	      values.put(ColumnDefinitionsColumns.ELEMENT_KEY, elementKey);
	      db.insert(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME, null, values);
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
   * @param db
   * @param tableId
   * @param elementKey
   * @return
   */
  public static int deleteColumnDefinition(SQLiteDatabase db,
      String tableId, String elementKey ) {
    int count = db.delete(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
        WHERE_SQL_FOR_ELEMENT, new String[] {tableId, elementKey});
    if (count != 1) {
      Log.e(TAG, "deleteColumn() deleted " + count + " rows");
    }
    return count;
  }

}
