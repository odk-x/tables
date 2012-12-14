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
//package org.opendatakit.tables.data;
//
//import java.util.List;
//import java.util.Map;
//
//import android.content.ContentValues;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.util.Log;
//
///**
// * 
// * @author sudar.sam
// *
// */
//public class KeyValueStoreColumn extends KeyValueStore {
//  
//  public static final String TAG = "KeyValueStoreColumn";
//  
//  /*
//   * This is the column for which this KeyValueStore is responsible. This 
//   * corresponds to ColumnProperties.DB_ELEMENT_KEY.
//   */
//  private String elementKey;
//  
//  protected static final String WHERE_SQL_FOR_KEY = 
//      KeyValueStoreManager.TABLE_ID + " = ? AND " + 
//      ColumnDefinitions.DB_ELEMENT_KEY + " = ? AND " +
//      KeyValueStoreManager.KEY + " = ?";
//  
//  protected static final String WHERE_SQL_FOR_KEYS = 
//      KeyValueStoreManager.TABLE_ID + " = ? AND " +
//      ColumnDefinitions.DB_ELEMENT_KEY + " = ? AND " +
//      KeyValueStoreManager.KEY + " in (";
//  
//  protected static final String WHERE_SQL_FOR_COLUMN =
//      KeyValueStoreManager.TABLE_ID + " = ? AND " +
//      ColumnDefinitions.DB_ELEMENT_KEY + " = ?";
//  
//  public KeyValueStoreColumn(String dbName, DbHelper dbh, String tableId,
//      String elementKey) {
//    super(dbName, dbh, tableId);
//    this.elementKey = elementKey;
//  }
//  
//  @Override
//  public Map<String, String> getKeyValues(SQLiteDatabase db) {
//    Cursor c = db.query(this.dbBackingName, 
//        new String[] {KeyValueStoreManager.KEY, KeyValueStoreManager.VALUE},
//        WHERE_SQL_FOR_COLUMN,
//        new String[] {this.tableId, this.elementKey}, null, null, null);
//    return getKeyValuesFromCursor(c);
//  }
//  
//  // TODO: override getEntries to return a list of new 
//  // OdkTablesKeyValueStoreColumnEntry objects.
//  // TODO: KeyValueStoreColumnEntry and OdkTablesKeyValueStoreEntry should both
//  // subclass a keyvaluestoreentry type so that these can override the same
//  // method. These should really both be addEntriesToStore, but can't override
//  // it so have this odd issue.
//  // TODO: does this enforce the set invariant?
//  public void addEntriesToColumnStore(SQLiteDatabase db, 
//      List<KeyValueStoreColumnEntry> entries) {
//    int numInserted = 0;
//    for (KeyValueStoreColumnEntry entry : entries) {
//      if (entry.value == null)
//        entry.value = "";
//      addEntryToStore(db, entry);
//      numInserted++;
//    }
//    Log.d(TAG, "inserted " + numInserted + " entries into a column kvs");
//  }
//  
//  /*
//   * Very basic way to add a key value entry to the store. This is protected
//   * to try and ensure access via access methods to ensure that keys can't be
//   * added willy nilly and break the invariant that the keys are a set.
//   * @param db
//   * @param entry
//   */
//  protected void addEntryToStore(SQLiteDatabase db, 
//      KeyValueStoreColumnEntry entry) {
//    ContentValues values = new ContentValues();
//    values.put(KeyValueStoreManager.TABLE_ID, String.valueOf(entry.tableId));
//    values.put(ColumnDefinitions.DB_ELEMENT_KEY, 
//        String.valueOf(entry.elementKey));
//    values.put(KeyValueStoreManager.KEY, String.valueOf(entry.key));
//    values.put(KeyValueStoreManager.VALUE_TYPE, String.valueOf(entry.type));
//    values.put(KeyValueStoreManager.VALUE, String.valueOf(entry.value));
//    db.insert(this.dbBackingName, null, values);
//  }
//  
//  /**
//   * Return a Map<String, String> of key->value from the key value store for
//   * all the keys defined in {@link ColumnProperties.getInitKeys()}.
//   */
//  @Override
//  public Map<String, String> getProperties(SQLiteDatabase db) {
//    String[] basicProps = ColumnProperties.getInitKeys();
//    String[] desiredKeys = new String[basicProps.length + 2];
//    // we want the first to be the tableId, b/c that is the table we are
//    // querying in the database.
//    desiredKeys[0] = tableId;
//    desiredKeys[1] = 
//    desiredKeys[1] = elementKey;
//    for (int i = 0; i < basicProps.length; i++) {
//      desiredKeys[i+2] = basicProps[i];
//    }
//    String whereClause = WHERE_SQL_FOR_KEYS + 
//        makePlaceHolders(ColumnProperties.getInitKeys().length)+ ")";
//    Cursor c = db.query(this.dbBackingName, 
//        new String[] {KeyValueStoreManager.KEY, KeyValueStoreManager.VALUE},
//        whereClause,
//        desiredKeys,
//        null, null, null); 
//    return getKeyValuesFromCursor(c);
//  }
//  
//  @Override
//  public void insertOrUpdateKey(SQLiteDatabase db, String partition, 
//      String aspect, String key, String type, String value) {
//    // first try to delete the row. If it's not there, no biggie, just 
//    // returns a 0. So you either delete it or it isn't there.
//    this.deleteKey(db, key);
//    if (value == null) {
//      value = "";
//    }
//    KeyValueStoreColumnEntry newEntry = new KeyValueStoreColumnEntry();
//    newEntry.tableId = this.tableId;
//    newEntry.elementKey = this.elementKey;
//    newEntry.type = type;
//    newEntry.key = key;
//    newEntry.value = value;
//    addEntryToStore(db, newEntry);
//  }
//
//  /**
//   * Delete the row from the database for that contains the given key.
//   * @param db
//   * @param key
//   * @return the number of rows affected
//   */
//  @Override
//  public int deleteKey(SQLiteDatabase db, String key) {
//    return db.delete(this.dbBackingName, WHERE_SQL_FOR_KEY,
//        new String[] {this.tableId, this.elementKey, key});
//  }
//  
//  /**
//   * Clear the key value pairs for the given column. 
//   * @return the number of entries deleted.
//   */
//  @Override
//  public int clearKeyValuePairs(SQLiteDatabase db) {
//    Map<String, String> keyValues = getKeyValues(db);
//    int count = 0;
//    for (String key : keyValues.keySet()) {
//      count++;
//      db.delete(dbBackingName, WHERE_SQL_FOR_KEY,
//          new String[] {String.valueOf(this.tableId), 
//                        String.valueOf(this.elementKey),
//                        key});
//    }
//    if (count != keyValues.size()) {
//      Log.e(TAG, "clearKeyValuePairs in column store deleted " 
//          + count + " rows from" +
//          dbBackingName + ", but there were " + keyValues.size() + 
//          " key value pairs for the table " + tableId + " and elementKey +" +
//          		elementKey);    
//    }
//    return count;
//  }
//  
//  // TODO: override getProperties, or resolve, b/c this method does jack for
//  // a column KVS.
//  
//  // TODO: override getEntriesFromCursor as above.
//
//}
