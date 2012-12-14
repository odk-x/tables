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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
 * This is a table in the database that stores information with varieties of 
 * metadata about the tables. It is a key value store. For more detailed 
 * comments see {@link KeyValueStoreManager}.
 * @author sudar.sam@gmail.com
 *
 */
public class KeyValueStore {
  
  public static final String TAG = "KeyValueStore";
  
  // The SQL where clause to use for selecting, updating, or deleting the row
  // for a given key.
  protected static final String WHERE_SQL_FOR_PARTITION_ASPECT_KEY = 
      KeyValueStoreManager.TABLE_ID + " = ? AND " + 
      KeyValueStoreManager.PARTITION + " = ? AND " + 
      KeyValueStoreManager.ASPECT + " = ? AND " + 
      KeyValueStoreManager.KEY + " = ?";
  
  /*
   * The SQL where clause for selecting entries from the key value store based
   * on table id, partition, and aspect.
   */
  protected static final String WHERE_SQL_FOR_PARTITION_ASPECT = 
      KeyValueStoreManager.TABLE_ID + " = ? AND " + 
      KeyValueStoreManager.PARTITION + " = ? AND " + 
      KeyValueStoreManager.ASPECT + " = ?";
  
  /*
   * The base where clause for selecting a table.
   */
  protected static final String WHERE_SQL_FOR_TABLE = 
      KeyValueStoreManager.TABLE_ID + " = ?";
  
  /*
   * The base wehre clause for getting only the key values that are contained 
   * in the list of table properties. Its usage must be followed by appending
   * ")".
   */
  protected static final String WHERE_SQL_FOR_PARTITION_ASPECT_KEYS = 
      KeyValueStoreManager.TABLE_ID + " = ? AND " + 
      KeyValueStoreManager.PARTITION + " = ? AND " + 
      KeyValueStoreManager.ASPECT + " = ? AND " + 
      KeyValueStoreManager.KEY + " in (";
  
  protected final DbHelper dbh;
  protected final String tableId;
  // The name of the database table that backs the key value store
  protected final String dbBackingName;
  
  /**
   * Construct a key value store object for interacting with a table's key
   * value store entries.
   * @param dbName name of the db table backing the store
   * @param dbh a DbHelper
   * @param tableId id of the table you are after
   */
  public KeyValueStore(String dbName, DbHelper dbh, String tableId) {
    this.dbBackingName = dbName;
    this.dbh = dbh;
    this.tableId = tableId;
  }
    
  /**
   * Return a map of key to value for the keys in a given partition and aspect
   * for the table specified by this object. It is assumed that the db is open 
   * and closed outside of the
   * method. 
   * <p>
   * The caller must know how to interpret the values, the type of which will
   * all be string, but which will be parseable into their appropriate types.
   * @param db
   * @return
   */
  public Map<String, String> getKeyValues(String partition, String aspect, 
      SQLiteDatabase db) {
    Cursor c = db.query(this.dbBackingName, 
        new String[] {KeyValueStoreManager.KEY, KeyValueStoreManager.VALUE}, 
        WHERE_SQL_FOR_PARTITION_ASPECT, 
        new String[] {this.tableId, partition, aspect}, null, null, null);
    return getKeyValuesFromCursor(c);
  }
  
  /**
   * Delete all the entries in the key value store for the given partition and
   * aspect.
   * @param partition
   * @param aspect
   * @param db
   * @return
   */
  public int clearEntries(String partition, String aspect, SQLiteDatabase db) {
    int count = db.delete(dbBackingName, WHERE_SQL_FOR_PARTITION_ASPECT, 
        new String[] {this.tableId, partition, aspect});
    return count;
  }
  
  /**
   * Return a list of all the OdkTablesKeyValueStoreEntry objects that exist
   * in the key value store for this table.
   * @param db
   * @return
   */
  public List<OdkTablesKeyValueStoreEntry> getEntries(SQLiteDatabase db) {
    Cursor c = db.query(this.dbBackingName,
        new String[] {KeyValueStoreManager.TABLE_ID,
                      KeyValueStoreManager.PARTITION,
                      KeyValueStoreManager.ASPECT,
                      KeyValueStoreManager.KEY,
                      KeyValueStoreManager.VALUE_TYPE,
                      KeyValueStoreManager.VALUE},
        WHERE_SQL_FOR_TABLE,
        new String[] {this.tableId}, null, null, null);
    try {
    	return getEntriesFromCursor(c);
    } finally {
    	if ( c != null && !c.isClosed() ) {
    		c.close();
    	}
    }
  }
  
  /**
   * Returns true if there are entries for the table in the key value store.
   * @param db
   * @return
   */
  public boolean entriesExist(SQLiteDatabase db) {
    List<OdkTablesKeyValueStoreEntry> allEntries = getEntries(db);
    return allEntries.size() > 0;
  }
  
  /**
   * Return a map of only the properties from the store backing this
   * object. These
   * are the properties as defined as the {@link INIT_KEYS} in TableProperties. 
   * Empty strings are returned as null.
   * @param db
   * @return
   */
  public Map<String, String>  getProperties(SQLiteDatabase db) {
    String[] basicProps = TableProperties.getInitKeys();
    String[] desiredKeys = new String[basicProps.length + 3];
    // we want the first to be the tableId, b/c that is the table id we are
    // querying over in the database.
    desiredKeys[0] = tableId;
    desiredKeys[1] = TableProperties.KVS_PARTITION;
    desiredKeys[2] = TableProperties.KVS_ASPECT;
    for (int i = 0; i < basicProps.length; i++) {
      desiredKeys[i+3] = basicProps[i]; 
    }
    String whereClause = WHERE_SQL_FOR_PARTITION_ASPECT_KEYS + 
        makePlaceHolders(TableProperties.getInitKeys().length) + ")";
    Cursor c = db.query(this.dbBackingName, 
        new String[] {KeyValueStoreManager.KEY, KeyValueStoreManager.VALUE}, 
        whereClause, 
        desiredKeys, null, null, null);
    return getKeyValuesFromCursor(c);    
  }
  
  /*
   * Return a map of key to value from a cursor that has queried the database
   * backing the key value store.
   */
  protected Map<String, String> getKeyValuesFromCursor(Cursor c) {
    int keyIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.KEY);
    int valueIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.VALUE);
    Map<String, String> keyValues = new HashMap<String, String>();
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      String value = c.getString(valueIndex);
      if (value.equals("")) 
        value = null;
      keyValues.put(c.getString(keyIndex), value);
      i++;
      c.moveToNext();
    }
    c.close();
    return keyValues;   
  }
  
  /*
   * Get the full entries from the table. These are the full entries, with
   * tableId and type information.
   */
  protected List<OdkTablesKeyValueStoreEntry> getEntriesFromCursor(Cursor c) {
    List<OdkTablesKeyValueStoreEntry> entries = 
        new ArrayList<OdkTablesKeyValueStoreEntry>();
    int idIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.TABLE_ID);
    int partitionIndex = 
        c.getColumnIndexOrThrow(KeyValueStoreManager.PARTITION);
    int aspectIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.ASPECT);
    int keyIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.KEY);
    int valueIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.VALUE);
    int typeIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.VALUE_TYPE);
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
      entry.tableId = c.getString(idIndex);
      entry.partition = c.getString(partitionIndex);
      entry.aspect = c.getString(aspectIndex);
      entry.key = c.getString(keyIndex);
      entry.type = c.getString(typeIndex);
      entry.value = c.getString(valueIndex);
      entries.add(entry);
      i++;
      c.moveToNext();
    }
    c.close();
    return entries;
  }
  
  /**
   * Delete all the key value pairs for the table.
   * @param db the open database
   * @return the number of entries deleted from the store.
   */
  public int clearKeyValuePairs(SQLiteDatabase db) {
    int count = db.delete(dbBackingName, WHERE_SQL_FOR_TABLE, 
        new String[] {this.tableId});
    return count;
  }
  
  /**
   * Return the entries in the key value store with the keys specified in the 
   * list desiredKeys from the given partition and aspect.
   * @param db
   * @param partition
   * @param aspect
   * @param keys
   * @return
   */
  public List<OdkTablesKeyValueStoreEntry> getEntriesForKeys(SQLiteDatabase db,
      String partition, String aspect, List<String> keys) {
    String[] desiredKeys = new String[keys.size() + 3];
    // we want the first to be the tableId, b/c that is the table id we are
    // querying over in the database.
    desiredKeys[0] = tableId;
    desiredKeys[1] = partition;
    desiredKeys[2] = aspect;
    for (int i = 0; i < keys.size(); i++) {
      desiredKeys[i+3] = keys.get(i); 
    }
    String whereClause = WHERE_SQL_FOR_PARTITION_ASPECT_KEYS + 
        makePlaceHolders(keys.size()) + ")";
    Cursor c = db.query(this.dbBackingName, 
        new String[] {KeyValueStoreManager.TABLE_ID,
                      KeyValueStoreManager.PARTITION,
                      KeyValueStoreManager.ASPECT,
                      KeyValueStoreManager.KEY,
                      KeyValueStoreManager.VALUE_TYPE,
                      KeyValueStoreManager.VALUE}, 
        whereClause, 
        desiredKeys, null, null, null);    
    try {
    	return getEntriesFromCursor(c);
    } finally {
    	if ( c != null && ! c.isClosed() ) {
    		c.close();
    	}
    }
  }
 
  /**
   * Add key value pairs to the store. Null values are inserted as an empty 
   * string.
   * <p>
   * Does not close the database.
   * @param dbh
   * @param entries List of the entries to be added.
   */
  public void addEntriesToStore(SQLiteDatabase db,
      List<OdkTablesKeyValueStoreEntry> entries) {
    int numInserted = 0;
    for (OdkTablesKeyValueStoreEntry entry : entries) {
      if (entry.value == null)
        entry.value = "";
      // We're going to go through insertOrUpdate key to ensure that the set
      // invariant of the keys in the kvs is enforced.
      insertOrUpdateKey(db, entry.partition, entry.aspect, entry.key, 
          entry.type, entry.value);
      numInserted++;
    }
    Log.d(TAG, "inserted " + numInserted + " key value pairs to kvs");
  }
  
  /**
   * Delete the row from the database for that contains the given key.
   * @param db
   * @param partition
   * @param aspect
   * @param key
   * @return the number of rows affected--should only ever be one
   */
  public int deleteKey(SQLiteDatabase db, String partition, String aspect,
      String key) {
    int numDeleted = db.delete(this.dbBackingName, 
        WHERE_SQL_FOR_PARTITION_ASPECT_KEY, 
        new String[] {this.tableId, partition, aspect, key});
    if (numDeleted > 1) {
      Log.e(TAG, "deleted > 1 entry from the key value store with name: " +
          this.dbBackingName + " and key: " + key);
    }
    return numDeleted;
  }
  

  /**
   * Add the typed key value store to the database, inserting or deleting the
   * key as needed. Null "value" entries are changed to the empty string.
   * @param db
   * @param partition
   * @param aspect
   * @param key
   * @param type
   * @param value
   */
  public void insertOrUpdateKey(SQLiteDatabase db, String partition, 
      String aspect, String key, String type, String value) {
    // first try to delete the row. If it's not there, no biggie, just 
    // returns a 0. So you either delete it or it isn't there.
    this.deleteKey(db, partition, aspect, key);
    if (value == null)
      value = "";
    OdkTablesKeyValueStoreEntry newEntry = new OdkTablesKeyValueStoreEntry();
    newEntry.tableId = this.tableId;
    newEntry.partition = partition;
    newEntry.aspect = aspect;
    newEntry.key = key;
    newEntry.type = type;
    newEntry.value = value;
    addEntryToStore(db, newEntry);
  }
  
  /*
   * Very basic way to add a key value entry to the store. This is private
   * because it should only be called via insertOrUpdateKey
   * to ensure that there the keys remain a set and that there are no other
   * invariants broken by direct manipulation of the database.
   * 
   * If you find that you are using this directly, rather than through 
   * insertOrUpdateKey, you are responsible for ensuring that the keys for the
   * table remain a set.
   */
  private void addEntryToStore(SQLiteDatabase db, 
      OdkTablesKeyValueStoreEntry entry) {
    ContentValues values = new ContentValues();
    values.put(KeyValueStoreManager.TABLE_ID, String.valueOf(entry.tableId));
    values.put(KeyValueStoreManager.PARTITION, 
        String.valueOf(entry.partition));
    values.put(KeyValueStoreManager.ASPECT, String.valueOf(entry.aspect));
    values.put(KeyValueStoreManager.VALUE_TYPE, String.valueOf(entry.type));
    values.put(KeyValueStoreManager.VALUE, String.valueOf(entry.value));
    values.put(KeyValueStoreManager.KEY, String.valueOf(entry.key));
    db.insert(this.dbBackingName, null, values);
  }
  
  /**
   * Returns a string of question marks separated by commas for use in an
   * android sqlite query.
   * @param numArgs number of question marks
   * @return
   */
  public static String makePlaceHolders(int numArgs) {
    String holders = "";
    if (numArgs == 0)
      return holders;
    for (int i = 0; i < numArgs; i++) {
      holders = holders + "?,";
    }
    holders = holders.substring(0, holders.length()-1);
    return holders;   
  }
  
  public static enum Type {
    ACTIVE(KeyValueStoreManager.ACTIVE_DB_NAME),
    DEFAULT(KeyValueStoreManager.DEFAULT_DB_NAME),
    SERVER(KeyValueStoreManager.SERVER_DB_NAME);
    
    private String backingName;
    
    private Type(String backingName) {
      this.backingName = backingName;
    }
    
    public String getBackingName() {
      return backingName;
    }
  }
    
}
