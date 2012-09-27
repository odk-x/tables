package org.opendatakit.tables.data;

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
  
  // These are the names of the active and the default key value stores as 
  // they will exist in the SQLite db once they are initialized.
  public static final String DEFAULT_DB_NAME = "keyValueStoreDefault";
  public static final String ACTIVE_DB_NAME = "keyValueStoreActive";
  
  // Names of the columns in the key value store
  // The underscores preceding are legacy, and are currently the same for 
  // ease of comparison b/w Aggregate and Tables.
  public static final String TABLE_ID = "TABLE_UUID";
  public static final String KEY = "_KEY";
  public static final String VALUE_TYPE = "_TYPE";
  public static final String VALUE = "VALUE";
  
  // The SQL where clause to use for selecting, updating, or deleting the row
  // for a given key.
  private static final String WHERE_SQL = TABLE_ID + " = ? and " + KEY + 
      " = ?";
  // The columns to be selected when initializing KeyStoreValueDefault.
  private static final String[] INIT_COLUMNS = {
    TABLE_ID,
    KEY,
    VALUE_TYPE,
    VALUE
  };
  
  // These are the key value stores. It is assumed there will only be two:
  // default and active.
  private static KeyValueStore defaultKVS = null;
  private static KeyValueStore activeKVS = null; 
  
  // Here are the fields copied from ColumnProperties.
  private final DbHelper dbh;
  private final String[] whereArgs;
  private final String tableId;
  
  private static void assertDefault(DbHelper dbh, String tableId) {
    if (defaultKVS == null) {
      defaultKVS = new KeyValueStore(DEFAULT_DB_NAME, dbh, tableId, KEY);
    }
  }
  
  private static void assertActive(DbHelper dbh, String tableId) {
    if (activeKVS == null) {
      activeKVS = new KeyValueStore(ACTIVE_DB_NAME, dbh, tableId, KEY);
    }
  }
  
  public static KeyValueStore getDefaultStore(DbHelper dbh, String tableId) {
    if (defaultKVS == null) {
      defaultKVS = new KeyValueStore(DEFAULT_DB_NAME, dbh, tableId, KEY);
    }
    return defaultKVS;
  }
  
  public static KeyValueStore getActiveStore(DbHelper dbh, String tableId) {
   if (activeKVS == null) {
     activeKVS = new KeyValueStore(ACTIVE_DB_NAME, dbh, tableId, KEY);
   }
   return activeKVS;
  }
  
  private KeyValueStore(String dbName, DbHelper dbh, String tableId, 
      String queryColumn) {
    this.dbh = dbh;
    this.tableId = tableId;
    // This is how it is called in ColumnProperties. queryColumn is likely the
    // column you are looking for a particular value of. Most likely this is 
    // going to be KEY.
    whereArgs = new String[] {String.valueOf(tableId), queryColumn};
  }
  
  /**
   * Return a map of key to value for a table's entries in the active key value
   * store. It is assumed that the db is open and closed outside of the
   * method.
   * @param dbh
   * @param tableId
   * @return
   */
  public Map<String, String> getActiveKeyValuesForTable(DbHelper dbh,
      SQLiteDatabase db, String tableId) {
    assertActive(dbh, tableId);
    Cursor c = db.query(ACTIVE_DB_NAME, INIT_COLUMNS, TABLE_ID + " = ?", 
        new String[] {tableId}, null, null, null);
    int keyIndex = c.getColumnIndexOrThrow(KEY);
    int valueIndex = c.getColumnIndexOrThrow(VALUE);
    Map<String, String> keyValues = new HashMap<String, String>();
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      keyValues.put(c.getString(keyIndex), c.getString(valueIndex));
      i++;
      c.moveToNext();
    }
    c.close();
    return keyValues;
  }
  
  /**
   * Return a map of key to value for a table's entries in the default key 
   * value store. It is assumed that the db is open and closed outside of the
   * method.
   * @param dbh
   * @param tableId
   * @return
   */
  public Map<String, String> getDefaultKeyValuesForTable(DbHelper dbh,
      SQLiteDatabase db, String tableId) {
    assertDefault(dbh, tableId);
    Cursor c = db.query(DEFAULT_DB_NAME, INIT_COLUMNS, TABLE_ID + " = ?", 
        new String[] {tableId}, null, null, null);
    int keyIndex = c.getColumnIndexOrThrow(KEY);
    int valueIndex = c.getColumnIndexOrThrow(VALUE);
    Map<String, String> keyValues = new HashMap<String, String>();
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      keyValues.put(c.getString(keyIndex), c.getString(valueIndex));
      i++;
      c.moveToNext();
    }
    c.close();
    return keyValues;
  }
  
  
  
  
  /**
   * Delete all the active key value pairs for a certain table.
   * @param dbh
   * @param db the open database
   * @param tableId
   */
  public void clearActiveKeyValuePairsForTable(DbHelper dbh, SQLiteDatabase db,
      String tableId) {
    boolean testOpen = db.isOpen();
    // Hilary passes in the db. Does this matter?
    // First get the key value pairs for this table.
    assertActive(dbh, tableId);
    Map<String, String> keyValues = 
        getActiveKeyValuesForTable(dbh, db, tableId);
    testOpen = db.isOpen();
    int count = 0;
    for (String key : keyValues.keySet()) {
      count++;
      db.delete(ACTIVE_DB_NAME, WHERE_SQL, 
          new String[] {String.valueOf(tableId),key});
    }
    if (count != keyValues.size()) {
      Log.e(TAG, "clearKeyValuePairsForTable deleted " + count + " rows from" +
          " the KeyValueStoreDefault, but there were " + keyValues.size() + 
          " key value pairs for the table " + tableId);
    }
  }
  
  /**
   * Delete all the default key value pairs for a certain table.
   * @param dbh
   * @param db the open database
   * @param tableId
   */
  public void clearDefaultKeyValuePairsForTable(DbHelper dbh, SQLiteDatabase db,
      String tableId) {
    boolean testOpen = db.isOpen();
    // Hilary passes in the db. Does this matter?
    // First get the key value pairs for this table.
    assertDefault(dbh, tableId);
    Map<String, String> keyValues = 
        getDefaultKeyValuesForTable(dbh, db, tableId);
    testOpen = db.isOpen();
    int count = 0;
    for (String key : keyValues.keySet()) {
      count++;
      db.delete(DEFAULT_DB_NAME, WHERE_SQL, 
          new String[] {String.valueOf(tableId),key});
    }
    if (count != keyValues.size()) {
      Log.e(TAG, "clearKeyValuePairsForTable deleted " + count + " rows from" +
          " the KeyValueStoreDefault, but there were " + keyValues.size() + 
          " key value pairs for the table " + tableId);
    }
  }  
  
  
  /**
   * Add key value pairs to the store from a manifest. It is very important to
   * note that, since you are adding them from a manifest, you are assumed to
   * be wanting to overwrite all the entries currently in the store and 
   * default to the server state. It therefore clears all the entries for
   * each table in tableIds so that it can start with a blank slate. 
   * <p>
   * All of the entries from a manifest are coming from a server, and it is 
   * they are therefore put into the default key value store.
   * @param dbh
   * @param entries List of the entries to be added.
   */
  public void addEntriesFromManifest(DbHelper dbh, SQLiteDatabase db,
      List<OdkTablesKeyValueStoreEntry> entries,
      String tableId) {
    boolean testOpen = db.isOpen();
    assertDefault(dbh, tableId);
    testOpen = db.isOpen();
    clearDefaultKeyValuePairsForTable(dbh, db, tableId);
    testOpen = db.isOpen();
    int numInserted = 0;
    for (OdkTablesKeyValueStoreEntry entry : entries) {
      ContentValues values = new ContentValues();
      values.put(TABLE_ID, String.valueOf(entry.tableId));
      values.put(VALUE_TYPE, String.valueOf(entry.type));
      values.put(VALUE, String.valueOf(entry.value));
      values.put(KEY, String.valueOf(entry.key));
      db.insert(DEFAULT_DB_NAME, null, values);
      numInserted++;
    }
    Log.d(TAG, "inserted " + numInserted + " key value pairs");
  }

  
  static String getDefaultTableCreateSql() {
    return "CREATE TABLE " + DEFAULT_DB_NAME + "(" +
               TABLE_ID + " TEXT NOT NULL" +
        ", " + KEY + " TEXT NOT NULL" +
        ", " + VALUE_TYPE + " TEXT NOT NULL" +
        ", " + VALUE + " TEXT NOT NULL" +
        ")";
  }
  
  static String getActiveTableCreateSql() {
    return "CREATE TABLE " + ACTIVE_DB_NAME + "(" +
               TABLE_ID + " TEXT NOT NULL" +
        ", " + KEY + " TEXT NOT NULL" +
        ", " + VALUE_TYPE + " TEXT NOT NULL" +
        ", " + VALUE + " TEXT NOT NULL" +
        ")";
  }
  
    
}
