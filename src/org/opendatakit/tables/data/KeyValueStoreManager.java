package org.opendatakit.tables.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * October, 2012:
 * This seems to be an ever changing design, so I'm including the date this 
 * time.
 * <p>
 * The key value stores are where properties are kept about the tables. This 
 * includes things like metadata, as well as things that are important to 
 * table structure and definition, making them less like "metadata" per se.
 * <p>
 * There are three of these key value stores, and they hold information about
 * the tables in different states. The three are the active, default, and
 * server key value stores. 
 * <p> 
 * The active holds the currently displaying version of the tables, which the
 * user modifies when they make changes. The server holds the version that was
 * pulled down from the server. This is also the key value store that is the
 * source of the table properties at the first synch. After that point, we are
 * currently not allowing metadata being pushed. (Well, technically you can do
 * it, but in our model we are planning to make this a privileged operation, as
 * there are some very important considerations that the values in the key 
 * value stores can have for the actual structure of a table.)
 * <p>
 * The default is a kind of combination between the two. Changes from the 
 * server before the last sync will have to be merged from the server table
 * into the default table. Exactly how this happens is at the moment undefined.
 * Suffice it to say that it is a mixture of the two, and more closely tied
 * to active than is the server. 
 * <p>
 * active<--default<--MERGE--server
 * active-->default-->server
 * The exact transitions here are up in the air as to when they can happen.
 * <p>
 * There is a fourth key value store. This handles only sync properties. It is
 * called the keyValueStoreSync, and it handles only things like "setToSync",
 * whether or not the table should be synched. That kind of thing. It is kind
 * of TBD...
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
  public static final String SERVER_DB_NAME = "keyValueStoreServer";
  public static final String SYNC_DB_NAME = "keyValueStoreSync";
  
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
  
  public KeyValueStore getServerStoreForTable(String tableId) {
    return new KeyValueStore(SERVER_DB_NAME, this.dbh, tableId);
  }
  
  /**
   * Return a key value store object for the sync properties for the given
   * table id.
   * @param tableId
   * @return
   */
  public KeyValueStoreSync getSyncStoreForTable(String tableId) {
    return new KeyValueStoreSync(SYNC_DB_NAME, this.dbh, tableId);
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
  
  /**
   * Return a list of all the table ids in the server key value store.
   * @param db
   * @return
   */
  public List<String> getServerTableIds(SQLiteDatabase db) {
    // We want a distinct query over the TABLE_ID column.
    Cursor c = db.query(true, SERVER_DB_NAME, new String[] {TABLE_ID},
        null, null, null, null, null, null);
    return getTableIdsFromCursor(c);    
  }
  
  /**
   * Return a list of the table ids who have isSetToSync set to true in the 
   * sync KVS.
   * @param db
   * @return
   */
  public List<String> getSynchronizedTableIds(SQLiteDatabase db) {
    // We want a query returning the TABLE_UUID where the key is the 
    // sync state string and the value is true.
    Cursor c = getTableIdsWithKeyValue(db, SYNC_DB_NAME,
        KeyValueStoreSync.SyncPropertiesKeys.IS_SET_TO_SYNC.getKey(), "1");
    return getTableIdsFromCursor(c);
  }
  
  public List<String> getDataTableIds(SQLiteDatabase db) {
    Cursor c = getTableIdsWithKeyValue(db, ACTIVE_DB_NAME,
        TableProperties.DB_TABLE_TYPE, 
        Integer.toString(TableProperties.TableType.DATA));
    return getTableIdsFromCursor(c);
  }
  
  /**
   * Return a list of the table ids for all the tables in the server KVS that
   * are of type data.
   * @param db
   * @return
   */
  public List<String> getServerDataTableIds(SQLiteDatabase db) {
    Cursor c = getTableIdsWithKeyValue(db, SERVER_DB_NAME,
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
   * <p>
   * active<--default
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
    activeKVS.clearKeyValuePairs(db);
    activeKVS.addEntriesToStore(db, defaultEntries);
  }
  
  /**
   * This merges the key values in the server store into the default store. 
   * "Merge" means that, for now (Oct 5, 2012), any keys that are in the both
   * stores have their value overwritten to that of the server store. 
   * Additionally, any keys that are not in the default table, but are in the 
   * server table, will be added to the default table. For now, this is 
   * considered to be a "merge". Eventually this should encompass smarter
   * logic about which values should be overwritten and which should be a true
   * "merge", giving the user an opportunity to determine resolve which value
   * should be kept.
   * <p>
   * In essence, then, the default becomes a union of the server and original
   * default values, with the identical keys being overwritten to the server
   * values.
   * <p>
   * default<--MERGE--server
   * @param tableId
   */
  public void mergeServerToDefaultForTable(String tableId) {
    /*
     * We're just going to go ahead and implement this with a map.
     * We'll add the default entries first. Any that are present also
     * in the server table will overwrite them. In the future, instead of 
     * just adding, logic for checking "user-resolvable" conflicts should be
     * added here.
     */
    Map<String, OdkTablesKeyValueStoreEntry> newDefault =
        new HashMap<String, OdkTablesKeyValueStoreEntry>();
    SQLiteDatabase db = dbh.getWritableDatabase();
    KeyValueStore defaultKVS = this.getDefaultStoreForTable(tableId);
    KeyValueStore serverKVS = this.getServerStoreForTable(tableId);
    List<OdkTablesKeyValueStoreEntry> oldDefaultEntries = 
        defaultKVS.getEntries(db);
    List<OdkTablesKeyValueStoreEntry> serverEntries = 
        serverKVS.getEntries(db);
    for (OdkTablesKeyValueStoreEntry entry : oldDefaultEntries) {
      newDefault.put(entry.key, entry);
    }
    for (OdkTablesKeyValueStoreEntry entry : serverEntries) {
      newDefault.put(entry.key, entry);
    }
    List<OdkTablesKeyValueStoreEntry> defaultList = 
        new ArrayList<OdkTablesKeyValueStoreEntry>();
    for (OdkTablesKeyValueStoreEntry entry : newDefault.values()) {
      defaultList.add(entry);
    }
    // TA-DA! And now we have the merged entries. put them in the store.
    defaultKVS.clearKeyValuePairs(db);
    defaultKVS.addEntriesToStore(db, defaultList);
  }
  
  /**
   * Remove all the key values for the given table from the default key value 
   * store, and copy in all the key values from the active key value store.
   * <p>
   * This should be a privileged operation, as the defaults will be able to be
   * pushed to the server.
   * <p>
   * active-->default
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
    defaultKVS.clearKeyValuePairs(db);
    defaultKVS.addEntriesToStore(db, activeEntries);
  }
  
  /**
   * Copy all of the key value pairs from the default into the server store.
   * First clears all the key values in the server table and then moves them
   * into the server table. At the moment this should really only be called
   * before the first sync. After that it will eventually probably be a 
   * privileged operation for an admin trying to push their table state up to
   * the server? This is not yet determined, though.
   * <p>
   * It is also important to note that if an entry for the table does not exist
   * in the sync key value store, the key isSetToSync will be added an 
   * initialized to 0. If there becomes a way to put entries into the server
   * key value store that does NOT use this method, you must be sure to also
   * add the isSetToSync key to the sync KVS.
   * <p>
   * default-->server
   * @param tableId
   */
  public void copyDefaultToServerForTable(String tableId) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    int numClearedFromServerKVS;
    KeyValueStore defaultKVS = this.getDefaultStoreForTable(tableId);    
    KeyValueStore serverKVS = this.getServerStoreForTable(tableId);
    numClearedFromServerKVS = serverKVS.clearKeyValuePairs(db);
    List<OdkTablesKeyValueStoreEntry> defaultEntries = 
        defaultKVS.getEntries(db);
    serverKVS.addEntriesToStore(db, defaultEntries);
    // and now add an entry to the sync KVS.
    addIsSetToSyncToSyncKVSForTable(tableId);
  }
  
  /**
   * Add the isSetToSynch key to the sync properties key value store for the 
   * given table. The value is initialized to 0, or false. If the key already
   * exists in the sync KVS, nothing happens.
   * @param tableId
   */
  public void addIsSetToSyncToSyncKVSForTable(String tableId) {
    KeyValueStore syncKVS = this.getSyncStoreForTable(tableId);
    SQLiteDatabase db = dbh.getWritableDatabase();
    // Note! If there ever becomes another way to
    // add entries to the server key value store, you must be sure to add the
    // is set to sync key to the sync store.
    List<String> isSetToSyncKey = new ArrayList<String>();
    isSetToSyncKey.add(KeyValueStoreSync.SyncPropertiesKeys
        .IS_SET_TO_SYNC.getKey());
    List<OdkTablesKeyValueStoreEntry> currentIsSetToSync = 
        syncKVS.getEntriesForKeys(db, isSetToSyncKey);
    if (currentIsSetToSync.size() == 0) {
      // we add the value.
      OdkTablesKeyValueStoreEntry newEntry = 
          new OdkTablesKeyValueStoreEntry();
      newEntry.key = 
          KeyValueStoreSync.SyncPropertiesKeys.IS_SET_TO_SYNC.getKey();
      newEntry.tableId = tableId;
      newEntry.type = "Integer";
      newEntry.value = "0";
      List<OdkTablesKeyValueStoreEntry> newKey = 
          new ArrayList<OdkTablesKeyValueStoreEntry>();
      newKey.add(newEntry);
      syncKVS.addEntriesToStore(db, newKey);
    }
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
  
  /**
   * The table creation SQL for the server store.
   */
  static String getServerTableCreateSql() {
    return "CREATE TABLE " + SERVER_DB_NAME + "(" +
               TABLE_ID + " TEXT NOT NULL" +
        ", " + KEY + " TEXT NOT NULL" +
        ", " + VALUE_TYPE + " TEXT NOT NULL" +
        ", " + VALUE + " TEXT NOT NULL" +
        ")";
  }
  
  /**
   * The table creation SQL for the sync store.
   */
  static String getSyncTableCreateSql() {
    return "CREATE TABLE " + SYNC_DB_NAME + "(" +
               TABLE_ID + " TEXT NOT NULL" +
        ", " + KEY + " TEXT NOT NULL" +
        ", " + VALUE_TYPE + " TEXT NOT NULL" +
        ", " + VALUE + " TEXT NOT NULL" +
        ")";
  }  

}
