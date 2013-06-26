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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.provider.KeyValueStoreColumns;
import org.opendatakit.tables.sync.SyncUtil;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * October, 2012:
 * This seems to be an ever changing design, so I'm including the date this
 * time.
 * <p>
 * The key value stores are where ODKTables-specific properties are kept about
 * the tables. In general this is things like "displayName" and eventually
 * view settings that do NOT affect the definition of a table.
 * <p>
 * The columns in the KVS are as follows:
 * tableId,
 * partition,
 * aspect,
 * key,
 * type,
 * value
 * <p>
 * These columns intend to restrict the scope of a key and to prevent collision
 * of keys. The primary key is essentially a composite of tableId, partition,
 * aspect, and key.
 * <p>
 * The tableId is the id of the table to which the key
 * applies.
 * <p>
 * The partition defines roughly to whom the key is relevant. A key that
 * matters to the Table as a whole (eg display name) is given the partition
 * "Table". A key that matters only to a column (eg display name of that
 * column) will be given the partition "Column". Additional partitions will be
 * generated as new classes are created that need to store information in the
 * KVS. For instance, "ListView" and "CollectUtil" would be the partitions that
 * store data for the ListView and CollectUtil classes, respectively.
 * Generally, with the exceptions of Table and Column, a rule of thumb is that
 * partitions should be class names for clarity.
 * <p>
 * The aspect is roughly equivalent to a particular instance of a partition. In
 * another sense, it refers to which specific view, or aspect, of the data the
 * key is associated with.
 * In many cases this will simply be "default". The display name of the table
 * would exist in partition=Table, aspect=default. A column's aspect would be
 * the element_key of the column. The element_key is fixed, and with the table
 * id forms a composite key for the column. The display name of a column with
 * the element key "weight" would exist in partition=Column, aspect=weight.
 * The aspect of a particular list view or graph is the name of that view. At
 * the moment (Dec, 2012) we are not allowing named list views or graph views.
 * Currently, therefore, the aspect for these views is assigned "default". A
 * font size in the list view (if such a property was able to be set) would
 * exist in partition=ListView, aspect=default, key=fontSize. If a graph view
 * was named
 * Fridge Count, the x axis would exist in partition=GraphView,
 * aspect=Fridge Count, key=xAxis. (In this example both the name of graph view
 * and the key xAxis were manufactured and do not reflect the true state of
 * the code. Further, it remains to be seen if we'll actually want to allow
 * spaces.)
 * <p>
 * Key is the key assigned to a particular property and must be unique within
 * a partition and aspect.
 * <p>
 * Type is the type of the value. The value field is set as a string, and this
 * type column allows appropriate interpretation of the string residing in
 * value. Obvious types are text, integer, double, and boolean. The complete
 * list of types is reflected in {@link KeyValueStoreEntryType}. For now, the
 * entry object itself comes from aggregate as a JAR. It is essentially just a
 * struct.
 * <p>
 * Value is the actual value of the key. A key specifying font size might have
 * a value of 12. A key specifying display name might have a value of
 * "Admin Areas". The class using the key must know how to interpret the value.
 * <p>
 * Taken in sum, the entry for a table's display name might be as follows:
 * tableId=myUUID, partition=Table, aspect=default, key=displayName, type=text,
 * value=My Custom Name.
 * <p>
 * The font size of the font size in a list view (which is currently not
 * nameable) might be:
 * tableId=myUUID, partition=ListView, aspect=default, key=fontSize,
 * type=integer, value=12.
 * <p>
 * The x axis of a graph view named Fridge Count (although graph views are
 * not currently nameable) might be (assuming that GraphView.java is the class
 * responsible for managing graph views):
 * tableId=myUUID, partition=GraphView, aspect=Fridge Count, key=xAxis,
 * type=text, value=count.
 * <p>
 * There are three of these key value stores, and they hold information about
 * the tables in different states. The three are the active, default, and
 * server key value stores.
 * <p>
 * The active holds the currently displaying version of the tables, which the
 * user modifies when they make changes. The server holds the version that was
 * pulled down from the server. This is also the key value store that is the
 * source of the table properties at the first sync. After that point, we are
 * currently not allowing definition changes to be pushed.
 * (Well, technically you can do
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
 * whether or not the table should be synched.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class KeyValueStoreManager {

  public static final String TAG = "KeyValueStoreManager";


  public static final String WHERE_SQL_KEY_VALUE = KeyValueStoreColumns.KEY + " = ? " + " and " +
      KeyValueStoreColumns.VALUE + " = ? ";

  // The columns to be selected when initializing KeyStoreValueDefault.
  private static final String[] INIT_COLUMNS = {
    KeyValueStoreColumns.TABLE_ID,
    KeyValueStoreColumns.PARTITION,
    KeyValueStoreColumns.ASPECT,
    KeyValueStoreColumns.KEY,
    KeyValueStoreColumns.VALUE_TYPE,
    KeyValueStoreColumns.VALUE
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

  /**
   * Return the typed key value store for the given table id.
   * @param tableId
   * @param typeOfStore
   * @return
   */
  public KeyValueStore getStoreForTable(String tableId,
      KeyValueStore.Type typeOfStore) {
    String backingName = getBackingNameForStore(typeOfStore);
    return new KeyValueStore(backingName, this.dbh, tableId);
  }

  /**
   * Return a key value store object for the sync properties for the given
   * table id.
   * @param tableId
   * @return
   */
  public KeyValueStoreSync getSyncStoreForTable(String tableId) {
    return new KeyValueStoreSync(DataModelDatabaseHelper.KEY_VALULE_STORE_SYNC_TABLE_NAME, this.dbh, tableId);
  }

  /**
   * Get all the tableIds from the specified store.
   * @param db
   * @param typeOfStore
   * @return
   */
  public List<String> getAllIdsFromStore(SQLiteDatabase db,
      KeyValueStore.Type typeOfStore) {
    String backingName = getBackingNameForStore(typeOfStore);
    Cursor c = db.query(true, backingName, new String[] {KeyValueStoreColumns.TABLE_ID},
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
    Cursor c = getTableIdsWithKeyValue(db, DataModelDatabaseHelper.KEY_VALULE_STORE_SYNC_TABLE_NAME,
        KeyValueStoreSync.SyncPropertiesKeys.IS_SET_TO_SYNC.getKey(), "1");
    return getTableIdsFromCursor(c);
  }

  /**
   * Get the ids of all the data tables that have entries in the given store.
   * <p>
   * Does not close the database.
   * {@link TableDefinitions.getTableIdsForType}
   * @param db
   * @param typeOfStore
   * @return
   */
  public List<String> getDataTableIds(SQLiteDatabase db,
      KeyValueStore.Type typeOfStore) {
    // the backing name of the store from which you'll check the ids against.
//    String backingName = getBackingNameForStore(typeOfStore);
//    Cursor c = getTableIdsWithKeyValue(db, backingName,
//        TableProperties.DB_TABLE_TYPE,
//        Integer.toString(TableProperties.TableType.DATA));
    // all the datatables in the db.
    List<String> tableDefinitionIds =
        TableDefinitions.getTableIdsForType(TableType.data, db);
    // the datatables that have entries in this key value store.
    List<String> kvsIds = getAllIdsFromStore(db, typeOfStore);
    Set<String> setTableDefIds = new HashSet<String>(tableDefinitionIds);
    List<String> dataTablesInThisStore = new ArrayList<String>();
    for (String id : kvsIds) {
      if (setTableDefIds.contains(id)) {
        dataTablesInThisStore.add(id);
      }
    }
//    return getTableIdsFromCursor(c);
    return dataTablesInThisStore;
  }


  /**
   * Get the ids of all the security tables that have entries in the given
   * <p>
   * Does not close the database.
   * {@link TableDefinitions.getTableIdsForType}
   * @param db
   * @param typeOfStore
   * @return
   */
  public List<String> getSecurityTableIds(SQLiteDatabase db,
      KeyValueStore.Type typeOfStore) {
//    String backingName = getBackingNameForStore(typeOfStore);
//    Cursor c = getTableIdsWithKeyValue(db, backingName,
//        TableDefinitions.DB_TYPE,
//        Integer.toString(TableType.security));
//    return getTableIdsFromCursor(c);
    List<String> tableDefinitionIds =
        TableDefinitions.getTableIdsForType(TableType.security, db);
    // the datatables that have entries in this key value store.
    List<String> kvsIds = getAllIdsFromStore(db, typeOfStore);
    Set<String> setTableDefIds = new HashSet<String>(tableDefinitionIds);
    List<String> securityTablesInThisStore = new ArrayList<String>();
    for (String id : kvsIds) {
      if (setTableDefIds.contains(id)) {
        securityTablesInThisStore.add(id);
      }
    }
//    return getTableIdsFromCursor(c);
    return securityTablesInThisStore;
  }

  /**
   * Get the ids of all the data tables that have entries in the given store.
   * <p>
   * Does not close the database.
   * {@link TableDefinitions.getTableIdsForType}
   * @param db
   * @param typeOfStore
   * @return
   */
  public List<String> getShortcutTableIds(SQLiteDatabase db,
      KeyValueStore.Type typeOfStore) {
//    String backingName = getBackingNameForStore(typeOfStore);
//    Cursor c = getTableIdsWithKeyValue(db, backingName,
//        TableDefinitions.DB_TYPE,
//        Integer.toString(TableProperties.TableType.SHORTCUT));
//    return getTableIdsFromCursor(c);
    List<String> tableDefinitionIds =
        TableDefinitions.getTableIdsForType(TableType.shortcut, db);
    // the datatables that have entries in this key value store.
    List<String> kvsIds = getAllIdsFromStore(db, typeOfStore);
    Set<String> setTableDefIds = new HashSet<String>(tableDefinitionIds);
    List<String> shortcutTablesInThisStore = new ArrayList<String>();
    for (String id : kvsIds) {
      if (setTableDefIds.contains(id)) {
        shortcutTablesInThisStore.add(id);
      }
    }
//    return getTableIdsFromCursor(c);
    return shortcutTablesInThisStore;
  }

  /*
   * Return the database backing name for the given type of KVS. This is just
   * intended as a convenience method to avoid having switch statements all
   * over the place.
   */
  private String getBackingNameForStore(KeyValueStore.Type typeOfStore) {
    switch (typeOfStore) {
    case ACTIVE:
      return DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME;
    case DEFAULT:
      return DataModelDatabaseHelper.KEY_VALUE_STORE_DEFAULT_TABLE_NAME;
    case SERVER:
      return DataModelDatabaseHelper.KEY_VALUE_STORE_SERVER_TABLE_NAME;
    default:
      Log.e(TAG, "nonexistent store rquested: " +
          typeOfStore.name());
      throw new IllegalArgumentException("nonexistent requested: "
          + typeOfStore.name());
    }
  }

  /**
   * Remove all the key values from the active key value store and copy all the
   * key value pairs from the default store into the active.
   * <p>
   * active<--default
   * @param tableId
   */
  public void copyDefaultToActiveForTable(String tableId) {
    // There is some weirdness here. Elsewhere "properties" have been
    // considered to be ONLY those keys that exist in the init columns of
    // TableProperties. ATM the file pointers for list and box views, etc,
    // are not included there. However, they should definitely be copied over
    // from the default table. Therefore all key value pairs that are in the
    // default store are copied over in this method.
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    KeyValueStore activeKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.ACTIVE);
	    KeyValueStore defaultKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.DEFAULT);
	    activeKVS.clearKeyValuePairs(db);
	    List<OdkTablesKeyValueStoreEntry> defaultEntries =
	        defaultKVS.getEntries(db);
	    activeKVS.clearKeyValuePairs(db);
	    activeKVS.addEntriesToStore(db, defaultEntries);
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
      TableProperties.markStaleCache(dbh, KeyValueStore.Type.ACTIVE);
    }
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
    // We're going to use a TreeSet because we need each
    // OdkTablesKeyValueStoreEntry object to be dependent only on the
    // partition, aspect, and key. The value should be ignored in the merge.
    // If we didn't do this, then we would end up not overwriting values as
    // expected.
    Set<OdkTablesKeyValueStoreEntry> newDefault =
        new TreeSet<OdkTablesKeyValueStoreEntry>(
             new SyncUtil.KVSEntryComparator() );
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    KeyValueStore defaultKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.DEFAULT);
	    KeyValueStore serverKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.SERVER);
	    List<OdkTablesKeyValueStoreEntry> oldDefaultEntries =
	        defaultKVS.getEntries(db);
	    List<OdkTablesKeyValueStoreEntry> serverEntries =
	        serverKVS.getEntries(db);
	    // First we get all the server entries as a set. We'll then add all the
	    // default values. A set is unchanged if the entry is already there, so
	    // the default entries that already have entries will simply be gone.
	    for (OdkTablesKeyValueStoreEntry entry : serverEntries) {
	      newDefault.add(entry);
	    }
	    for (OdkTablesKeyValueStoreEntry entry : oldDefaultEntries) {
	      newDefault.add(entry);
	    }
	    List<OdkTablesKeyValueStoreEntry> defaultList =
	        new ArrayList<OdkTablesKeyValueStoreEntry>();
	    for (OdkTablesKeyValueStoreEntry entry : newDefault) {
	      defaultList.add(entry);
	    }
	    // TA-DA! And now we have the merged entries. put them in the store.
	    defaultKVS.clearKeyValuePairs(db);
	    defaultKVS.addEntriesToStore(db, defaultList);
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
      TableProperties.markStaleCache(dbh, KeyValueStore.Type.DEFAULT);
    }
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
    try {
	    KeyValueStore activeKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.ACTIVE);
	    KeyValueStore defaultKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.DEFAULT);
	    defaultKVS.clearKeyValuePairs(db);
	    List<OdkTablesKeyValueStoreEntry> activeEntries =
	        activeKVS.getEntries(db);
	    defaultKVS.clearKeyValuePairs(db);
	    defaultKVS.addEntriesToStore(db, activeEntries);
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
      TableProperties.markStaleCache(dbh, KeyValueStore.Type.DEFAULT);
    }
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
   * Also increments the properties tag for the dataproperties and sets the
   * table state to updating--both only if it has
   * already been synched.
   * default-->server
   * @param tableId
   */
  public void copyDefaultToServerForTable(String tableId) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    int numClearedFromServerKVS;
	    KeyValueStore defaultKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.DEFAULT);
	    KeyValueStore serverKVS = this.getStoreForTable(tableId,
	        KeyValueStore.Type.SERVER);
	    numClearedFromServerKVS = serverKVS.clearKeyValuePairs(db);
	    List<OdkTablesKeyValueStoreEntry> defaultEntries =
	        defaultKVS.getEntries(db);
	    serverKVS.addEntriesToStore(db, defaultEntries);
	    // and now add an entry to the sync KVS.
	    addIsSetToSyncToSyncKVSForTable(tableId);
	    // Now try to update the properties tag.
	    TableProperties tp = TableProperties.getTablePropertiesForTable(dbh,
	        tableId, KeyValueStore.Type.SERVER);
	    String syncTagStr = tp.getSyncTag();
	    if (syncTagStr == null || syncTagStr.equals("")) {
	      // Then it's not been synched and we can rely on it to first be inited
	      // during the sync.
	    } else {
	      // We don't update the properties etag, which should only ever be set
	      // from the server. The SyncState.updating flag is sufficient to mark
	      // it as dirty.
	      tp.setSyncState(SyncState.updating);
	    }
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
      TableProperties.markStaleCache(dbh, null); // all are stale because of sync state change
    }
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
    try {
	    // Note! If there ever becomes another way to
	    // add entries to the server key value store, you must be sure to add the
	    // is set to sync key to the sync store.
	    List<String> isSetToSyncKey = new ArrayList<String>();
	    isSetToSyncKey.add(KeyValueStoreSync.SyncPropertiesKeys
	        .IS_SET_TO_SYNC.getKey());
	    List<OdkTablesKeyValueStoreEntry> currentIsSetToSync =
	        syncKVS.getEntriesForKeys(db, KeyValueStoreSync.KVS_PARTITION,
	            KeyValueStoreSync.KVS_ASPECT, isSetToSyncKey);
	    if (currentIsSetToSync.size() == 0) {
	      // we add the value.
	      OdkTablesKeyValueStoreEntry newEntry =
	          new OdkTablesKeyValueStoreEntry();
	      newEntry.key =
	          KeyValueStoreSync.SyncPropertiesKeys.IS_SET_TO_SYNC.getKey();
	      newEntry.tableId = tableId;
	      newEntry.type = ColumnType.INTEGER.name();
	      newEntry.value = "0";
	      List<OdkTablesKeyValueStoreEntry> newKey =
	          new ArrayList<OdkTablesKeyValueStoreEntry>();
	      newKey.add(newEntry);
	      syncKVS.addEntriesToStore(db, newKey);
	    }
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
    }
  }

  /*
   * This does a simple query for all the tables that have rows where the
   * key and the value equal the passed in parameters.
   */
  private Cursor getTableIdsWithKeyValue(SQLiteDatabase db, String storeName,
      String key, String value) {
    Cursor c = db.query(storeName,
        new String[] {KeyValueStoreColumns.TABLE_ID},
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
    int tableIdIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.TABLE_ID);
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
    return KeyValueStoreColumns.getTableCreateSql(DataModelDatabaseHelper.KEY_VALUE_STORE_DEFAULT_TABLE_NAME);
  }

  /**
   * The table creation SQL for the active store.
   * @return
   */
  static String getActiveTableCreateSql() {
	return KeyValueStoreColumns.getTableCreateSql(DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME);
  }

  /**
   * The table creation SQL for the server store.
   */
  static String getServerTableCreateSql() {
	return KeyValueStoreColumns.getTableCreateSql(DataModelDatabaseHelper.KEY_VALUE_STORE_SERVER_TABLE_NAME);
  }

  /**
   * The table creation SQL for the sync store.
   */
  static String getSyncTableCreateSql() {
	return KeyValueStoreColumns.getTableCreateSql(DataModelDatabaseHelper.KEY_VALULE_STORE_SYNC_TABLE_NAME);
  }

}
