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
public class KeyValueStoreDefault {
  
  public static final String TAG = "KeyValueStoreDefault";
  
  // the name of the table in the database
  private static final String DB_TABLENAME = "keyValueStoreDefault";
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
  
  // seeing if i need to initialize this.
  private  static KeyValueStoreDefault kvs = null;
  
  // Here are the fields copied from ColumnProperties.
  private final DbHelper dbh;
  private final String[] whereArgs;
  private final String tableId;
  
  private void assertRelation(DbHelper dbh, String tableId) {
    if (this.kvs == null) {
      this.kvs = new KeyValueStoreDefault(dbh, tableId, KEY);
    }
  }
  
  public static KeyValueStoreDefault getStore(DbHelper dbh, String tableId) {
    if (kvs == null) {
      kvs = new KeyValueStoreDefault(dbh, tableId, KEY);
    }
    return kvs;
    
  }
  
  private KeyValueStoreDefault(DbHelper dbh, String tableId, 
      String queryColumn) {
    this.dbh = dbh;
    this.tableId = tableId;
    // This is how it is called in ColumnProperties. queryColumn is likely the
    // column you are looking for a particular value of. Most likely this is 
    // going to be KEY.
    whereArgs = new String[] {String.valueOf(tableId), queryColumn};
  }
  
  /**
   * Return a map of key to value for a table's entries in the key value
   * store.
   * @param dbh
   * @param tableId
   * @return
   */
  public Map<String, String> getKeyValuesForTable(DbHelper dbh, 
      String tableId) {
    assertRelation(dbh, tableId);
    SQLiteDatabase db = dbh.getReadableDatabase();
    Cursor c = db.query(DB_TABLENAME, INIT_COLUMNS, TABLE_ID + " = ?", 
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
    db.close();
    return keyValues;
  }
  
  /**
   * Delete all the key value pairs for a certain table.
   * @param dbh
   * @param tableId
   */
  public void clearKeyValuePairsForTable(DbHelper dbh, String tableId) {
    // Hilary passes in the db. Does this matter?
    // First get the key value pairs for this table.
    assertRelation(dbh, tableId);
    Map<String, String> keyValues = getKeyValuesForTable(dbh, tableId);
    SQLiteDatabase db = dbh.getReadableDatabase();
    int count = 0;
    for (String key : keyValues.keySet()) {
      count++;
      db.delete(DB_TABLENAME, WHERE_SQL, 
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
   * @param dbh
   * @param entries List of the entries to be added.
   */
  public void addEntriesFromManifest(DbHelper dbh,
      List<OdkTablesKeyValueStoreEntry> entries,
      String tableId) {
    assertRelation(dbh, tableId);
    SQLiteDatabase db = dbh.getReadableDatabase();
    clearKeyValuePairsForTable(dbh, tableId);
    int numInserted = 0;
    for (OdkTablesKeyValueStoreEntry entry : entries) {
      ContentValues values = new ContentValues();
      values.put(TABLE_ID, entry.tableId);
      values.put(VALUE_TYPE, entry.type);
      values.put(VALUE, entry.value);
      values.put(KEY, entry.key);
      db.insert(DB_TABLENAME, null, values);
      numInserted++;
    }
    Log.d(TAG, "inserted " + numInserted + " key value pairs");
  }

  
  static String getTableCreateSql() {
    return "CREATE TABLE " + DB_TABLENAME + "(" +
               TABLE_ID + " TEXT NOT NULL" +
        ", " + KEY + " TEXT NOT NULL" +
        ", " + VALUE_TYPE + " TEXT NOT NULL" +
        ", " + VALUE + " TEXT NOT NULL" +
        ")";
}
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
}
