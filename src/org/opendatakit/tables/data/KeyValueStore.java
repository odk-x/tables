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
 * THESE COMMENTS ARE OUT OF DATE AFTER THE ADDITION OF THE 
 * KEYVALUESTOREMANAGER AND THE ARRIVAL OF THE ACTIVE/DEFAULT OPTIONS.
 * This is a table in the database that stores information with varieties of 
 * metadata about the tables. It is essentially a key value store. It is the
 * phone-side version of DbTableFileInfo on the server. An important 
 * distinction is that only the key-value pairs associated with tables on the 
 * phone that are set to be synched will be included. In this way it is not
 * a direct copy of DbTableFileInfo.
 * <p>
 * An additional important distinction is the way in which "FILE" values are
 * stored. On the server, if the VALUE_TYPE is "FILE", the VALUE field itself
 * holds the uuid that is necessary to access the blob on the server. Here, it 
 * will instead hold the path to the file on disk.
 * <p> 
 * It is important to distinguish this table from KeyValueStoreActive. This is
 * where things are saved as defaults and stored when downloaded from the
 * server. KeyValueStoreActive, on the other hand, is where the currently
 * modifiable values are stored. Data can be moved between these two tables
 * using the functionality provided by setCurrentAsDefault and 
 * revertToDefault.
 * <p>
 * It is modeled off of the implementation seen in {@ ColumnProperties}.
 * @author sudar.sam@gmail.com
 *
 */
public class KeyValueStore {
  
  public static final String TAG = "KeyValueStore";
  
  // The SQL where clause to use for selecting, updating, or deleting the row
  // for a given key.
  private static final String WHERE_SQL_FOR_KEY = 
      KeyValueStoreManager.TABLE_ID + " = ? and " + KeyValueStoreManager.KEY + 
      " = ?";
  
  /*
   * The base where clause for selecting a table.
   */
  private static final String WHERE_SQL_FOR_TABLE = 
      KeyValueStoreManager.TABLE_ID + " = ?";
  
  /*
   * The base wehre clause for getting only the key values that are contained 
   * in the list of table properties. Its usage must be followed by appending
   * ")".
   */
  private static final String WHERE_SQL_FOR_PROPS = 
      KeyValueStoreManager.TABLE_ID + " = ? " + " AND " + 
      KeyValueStoreManager.KEY + " in (";
  
  private final DbHelper dbh;
  //private final String[] whereArgs;
  private final String tableId;
  // The name of the database table that backs the key value store
  private final String dbBackingName;
  
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
    //this.whereArgs = new String[] {String.valueOf(tableId)};
  }
    
  /**
   * Return a map of key to value for a table's entries in the active key value
   * store. It is assumed that the db is open and closed outside of the
   * method.
   * @param db
   * @return
   */
  public Map<String, String> getKeyValues(SQLiteDatabase db) {
    Cursor c = db.query(this.dbBackingName, 
        new String[] {KeyValueStoreManager.KEY, KeyValueStoreManager.VALUE}, 
        WHERE_SQL_FOR_TABLE, 
        new String[] {this.tableId}, null, null, null);
    return getKeyValuesFromCursor(c);
  }
  
  /**
   * Return a list of all the OdkTablesKeyValueStoreEntry objects that exist
   * in the key value store.
   * @param db
   * @return
   */
  public List<OdkTablesKeyValueStoreEntry> getEntries(SQLiteDatabase db) {
    Cursor c = db.query(this.dbBackingName,
        new String[] {KeyValueStoreManager.TABLE_ID,
                      KeyValueStoreManager.KEY,
                      KeyValueStoreManager.VALUE_TYPE,
                      KeyValueStoreManager.VALUE},
        WHERE_SQL_FOR_TABLE,
        new String[] {this.tableId}, null, null, null);
    return getEntriesFromCursor(c);
  }
  
  /**
   * Returns true if there are entries for the table in the key value store.
   * @param db
   * @return
   */
  public boolean entriesExist(SQLiteDatabase db) {
    Map<String, String> entries = getKeyValues(db);
    return (entries.size() != 0);
  }
  
  /**
   * Return a map of only the properties in the active key value store. These
   * are the properties as defined as the INIT_COLUMNS in TableProperties. The
   * rest of the key value pairs are considered to be associated with the 
   * table, but not "properties" per se. Empty strings are returned as null.
   * @param db
   * @return
   */
  public Map<String, String>  getProperties(SQLiteDatabase db) {
    String[] basicProps = TableProperties.getInitColumns();
    String[] desiredKeys = new String[basicProps.length + 1];
    // we want the first to be the tableId, b/c that is the table id we are
    // querying over in the database.
    desiredKeys[0] = tableId;
    for (int i = 0; i < basicProps.length; i++) {
      desiredKeys[i+1] = basicProps[i]; 
    }
    String whereClause = WHERE_SQL_FOR_PROPS + 
        makePlaceHolders(TableProperties.getInitColumns().length) + ")";
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
  private Map<String, String> getKeyValuesFromCursor(Cursor c) {
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
  private List<OdkTablesKeyValueStoreEntry> getEntriesFromCursor(Cursor c) {
    List<OdkTablesKeyValueStoreEntry> entries = 
        new ArrayList<OdkTablesKeyValueStoreEntry>();
    int idIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.TABLE_ID);
    int keyIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.KEY);
    int valueIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.VALUE);
    int typeIndex = c.getColumnIndexOrThrow(KeyValueStoreManager.VALUE_TYPE);
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
      entry.key = c.getString(keyIndex);
      entry.tableId = c.getString(idIndex);
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
   * Delete all the active key value pairs for the table.
   * @param dbh
   * @param db the open database
   * @param tableId
   */
  public void clearKeyValuePairs(SQLiteDatabase db) {
    // First get the key value pairs for this table.
    Map<String, String> keyValues = 
        getKeyValues(db);
    int count = 0;
    for (String key : keyValues.keySet()) {
      count++;
      db.delete(dbBackingName, WHERE_SQL_FOR_KEY, 
          new String[] {String.valueOf(this.tableId),key});
    }
    if (count != keyValues.size()) {
      Log.e(TAG, "clearKeyValuePairsForTable deleted " + count + " rows from" +
          " the KeyValueStoreDefault, but there were " + keyValues.size() + 
          " key value pairs for the table " + tableId);
    }
  }
 
  /**
   * Add key value pairs to the store. Null values are inserted as an empty 
   * string.
   * @param dbh
   * @param entries List of the entries to be added.
   */
  public void addEntriesToStore(SQLiteDatabase db,
      List<OdkTablesKeyValueStoreEntry> entries) {
    int numInserted = 0;
    for (OdkTablesKeyValueStoreEntry entry : entries) {
      if (entry.value == null)
        entry.value = "";
      addEntryToStore(db, entry);
      numInserted++;
    }
    Log.d(TAG, "inserted " + numInserted + " key value pairs to default kvs");
  }
  
  /**
   * Delete the row from the database for that contains the given key.
   * @param db
   * @param key
   * @return the number of rows affected
   */
  public int deleteKey(SQLiteDatabase db, String key) {
    return db.delete(this.dbBackingName, WHERE_SQL_FOR_KEY, 
        new String[] {this.tableId, key});
  }
  

  /**
   * Add the typed key value store to the database, inserting or deleting the
   * key as needed. Null "value" entries are changed to the empty string.
   * @param db
   * @param type
   * @param key
   * @param value
   */
  public void insertOrUpdateKey(SQLiteDatabase db, String type,
      String key, String value) {
    // first try to delete the row. If it's not there, no biggie, just 
    // returns a 0. So you either delete it or it isn't there.
    this.deleteKey(db, key);
    if (value == null)
      value = "";
    OdkTablesKeyValueStoreEntry newEntry = new OdkTablesKeyValueStoreEntry();
    newEntry.key = key;
    newEntry.tableId = this.tableId;
    newEntry.type = type;
    newEntry.value = value;
    addEntryToStore(db, newEntry);
  }
  
  /*
   * Very basic way to add a key value entry to the store. This is private
   * because it should only be called via appropriate accessor methods
   * to ensure that there the keys remain a set and that there are no other
   * invariants broken my direct manipulation of the database.
   */
  private void addEntryToStore(SQLiteDatabase db, 
      OdkTablesKeyValueStoreEntry entry) {
    ContentValues values = new ContentValues();
    values.put(KeyValueStoreManager.TABLE_ID, String.valueOf(entry.tableId));
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
  
    
}
