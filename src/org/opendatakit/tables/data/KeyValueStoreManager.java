package org.opendatakit.tables.data;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class manages the key value store. This entails maintaining two 
 * versions of the key value store--a default and an active. The default
 * is the copy that is reflected on the server. The active is the version that
 * is currently being used and modified by the phone. Information will be
 * set between them by "save as default" and a "revert to default" commands.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class KeyValueStoreManager {
  
  public static final String TAG = "KeyValueStoreManager";
  
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
  
  public static final String WHERE_SQL_KEY_VALUE = KEY + " = ? " + " and " +
      VALUE + " = ? ";
    
  // The columns to be selected when initializing KeyStoreValueDefault.
  private static final String[] INIT_COLUMNS = {
    TABLE_ID,
    KEY,
    VALUE_TYPE,
    VALUE
  };
  
  private DbHelper dbh;
  
  // here is the actual manager.
  private static KeyValueStoreManager kvsManager = null;
  
  private KeyValueStoreManager(DbHelper dbh) {
    this.dbh = dbh;
  }
  
  public static KeyValueStoreManager getKVSManager(DbHelper dbh) {
    if (kvsManager == null) {
      kvsManager = new KeyValueStoreManager(dbh);
    }
    return kvsManager;
  }
  
  public KeyValueStore getActiveStoreForTable(String tableId) {
    return new KeyValueStore(ACTIVE_DB_NAME, this.dbh, tableId);
  }
  
  public KeyValueStore getDefaultStoreForTable(String tableId) {
    return new KeyValueStore(DEFAULT_DB_NAME, this.dbh, tableId);
  }
  
  /**
   * Return a list of all the table ids in the active key value store.
   * @param db
   * @return
   */
  public List<String> getActiveTableIds(SQLiteDatabase db) {
    // We want a distinct query over the TABLE_ID column.
    Cursor c = db.query(true, ACTIVE_DB_NAME, new String[] {TABLE_ID},
        null, null, null, null, null, null);
    return getTableIdsFromCursor(c);    
  }
  
  /**
   * Return a list of all the table ids in the default key value store.
   * @param db
   * @return
   */
  public List<String> getDefaultTableIds(SQLiteDatabase db) {
    // We want a distinct query over the TABLE_ID column.
    Cursor c = db.query(true, ACTIVE_DB_NAME, new String[] {TABLE_ID},
        null, null, null, null, null, null);
    return getTableIdsFromCursor(c);     
  }
  
  public List<String> getSynchronizedTableIds(SQLiteDatabase db) {
    // We want a query returning the TABLE_UUID where the key is the 
    // sync state string and the value is true.
    Cursor c = getTableIdsWithKeyValue(db, ACTIVE_DB_NAME,
        TableProperties.DB_IS_SYNCHED, "1");
    return getTableIdsFromCursor(c);
  }
  
  public List<String> getDataTableIds(SQLiteDatabase db) {
    Cursor c = getTableIdsWithKeyValue(db, ACTIVE_DB_NAME,
        TableProperties.DB_TABLE_TYPE, 
        Integer.toString(TableProperties.TableType.DATA));
    return getTableIdsFromCursor(c);
  }
  
  public List<String> getSecurityTableIds(SQLiteDatabase db) {
    Cursor c = getTableIdsWithKeyValue(db, ACTIVE_DB_NAME,
        TableProperties.DB_TABLE_TYPE,
        Integer.toString(TableProperties.TableType.SECURITY));
    return getTableIdsFromCursor(c);
  }
  
  public List<String> getShortcutTableIds(SQLiteDatabase db) {
    Cursor c = getTableIdsWithKeyValue(db, ACTIVE_DB_NAME,
        TableProperties.DB_TABLE_TYPE,
        Integer.toString(TableProperties.TableType.SHORTCUT));
    return getTableIdsFromCursor(c);
  }
  
  /**
   * Remove all the key values from the active key value store and copy all the
   * key value pairs from the default store into the active.
   * @param tableId
   */
  public void revertToDefaultPropertiesForTable(String tableId) {
    // There is some weirdness here. Elsewhere "properties" have been
    // considered to be ONLY those keys that exist in the init columns of 
    // TableProperties. ATM the file pointers for list and box views, etc,
    // are not included there. However, they should definitely be copied over
    // from the default table. Therefore all key value pairs that are in the
    // default store are copied over in this method.
    SQLiteDatabase db = dbh.getWritableDatabase();
    KeyValueStore activeKVS = this.getActiveStoreForTable(tableId);
    KeyValueStore defaultKVS = this.getDefaultStoreForTable(tableId);
    activeKVS.clearKeyValuePairs(db);
    List<OdkTablesKeyValueStoreEntry> defaultEntries = 
        defaultKVS.getEntries(db);
    activeKVS.addEntriesToStore(db, defaultEntries);
  }
  
  /**
   * Remove all the key values for the given table from the default key value 
   * store, and copy in all the key values from the active key value store.
   * <p>
   * This should be a privileged operation, as the defaults will be able to be
   * pushed to the server.
   * @param tableId
   */
  public void setCurrentAsDefaultPropertiesForTable(String tableId) {
    // Remove all the key values from the default key value store for the given
    // table and replace them with the key values from the active store.
    SQLiteDatabase db = dbh.getWritableDatabase();
    KeyValueStore activeKVS = this.getActiveStoreForTable(tableId);
    KeyValueStore defaultKVS = this.getDefaultStoreForTable(tableId);
    defaultKVS.clearKeyValuePairs(db);
    List<OdkTablesKeyValueStoreEntry> activeEntries =
        activeKVS.getEntries(db);
    defaultKVS.addEntriesToStore(db, activeEntries);
  }
  
  /*
   * This does a simple query for all the tables that have rows where the 
   * key and the value equal the passed in parameters.
   */
  private Cursor getTableIdsWithKeyValue(SQLiteDatabase db, String storeName,
      String key, String value) {
    Cursor c = db.query(storeName, 
        new String[] {TABLE_ID}, 
        WHERE_SQL_KEY_VALUE,
        new String[] {key, value},
        null, null, null);
    return c;
  }
  
  /*
   * Get all the table ids from a cursor based. 
   */
  private List<String> getTableIdsFromCursor(Cursor c) {
    List<String> ids = new ArrayList<String>();
    int tableIdIndex = c.getColumnIndexOrThrow(TABLE_ID);
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      ids.add(c.getString(tableIdIndex));
      i++;
      c.moveToNext();
    }
    c.close();
    return ids;   
  }
        
  /**
   * The table creation SQL statement for the default store.
   * @return
   */
  static String getDefaultTableCreateSql() {
    return "CREATE TABLE " + DEFAULT_DB_NAME + "(" +
               TABLE_ID + " TEXT NOT NULL" +
        ", " + KEY + " TEXT NOT NULL" +
        ", " + VALUE_TYPE + " TEXT NOT NULL" +
        ", " + VALUE + " TEXT NOT NULL" +
        ")";
  }
  
  /**
   * The table creation SQL for the active store.
   * @return
   */
  static String getActiveTableCreateSql() {
    return "CREATE TABLE " + ACTIVE_DB_NAME + "(" +
               TABLE_ID + " TEXT NOT NULL" +
        ", " + KEY + " TEXT NOT NULL" +
        ", " + VALUE_TYPE + " TEXT NOT NULL" +
        ", " + VALUE + " TEXT NOT NULL" +
        ")";
  }

}
