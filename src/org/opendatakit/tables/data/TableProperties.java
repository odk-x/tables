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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.tables.Activity.util.SecurityUtil;
import org.opendatakit.tables.Activity.util.ShortcutUtil;
import org.opendatakit.tables.sync.SyncUtil;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and managing table properties.
 * <p>
 * Note: Sam (sudar.sam@gmail.com) has begun to transition this code to use the
 * key value stores rather than an actual table properties table. The idea is
 * that you will be using the "Active" key value store to modify and display
 * your things, and that when you download from the server, these properties
 * will be in the "default" key value store.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 */
public class TableProperties {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String t = "TableProperties";
  
  public static final String TAG = "Table_Properties";

  // names of columns in the table properties table
  public static final String DB_TABLE_ID = "tableId";
  public static final String DB_DB_TABLE_NAME = "dbTableName";
  public static final String DB_DISPLAY_NAME = "displayName";
  public static final String DB_TABLE_TYPE = "type";
  public static final String DB_COLUMN_ORDER = "colOrder";
  public static final String DB_PRIME_COLUMNS = "primeCols";
  public static final String DB_SORT_COLUMN = "sortCol";
  public static final String DB_READ_SECURITY_TABLE_ID = "readAccessTid";
  public static final String DB_WRITE_SECURITY_TABLE_ID = "writeAccessTid";
  public static final String DB_SYNC_TAG = "syncTag";
  public static final String DB_LAST_SYNC_TIME = "lastSyncTime";
  public static final String DB_OV_VIEW_SETTINGS = "ovViewSettings";
  public static final String DB_CO_VIEW_SETTINGS = "coViewSettings";
  public static final String DB_DETAIL_VIEW_FILE = "detailViewFile";
  public static final String DB_SUM_DISPLAY_FORMAT = "summaryDisplayFormat";
  public static final String DB_SYNC_STATE = "syncState";
  public static final String DB_TRANSACTIONING = "transactioning";
  // keys for JSON
  private static final String JSON_KEY_VERSION = "jVersion";
  private static final String JSON_KEY_TABLE_ID = "tableId";
  private static final String JSON_KEY_DB_TABLE_NAME = "dbTableName";
  private static final String JSON_KEY_DISPLAY_NAME = "displayName";
  private static final String JSON_KEY_TABLE_TYPE = "type";
  private static final String JSON_KEY_COLUMN_ORDER = "colOrder";
  private static final String JSON_KEY_COLUMNS = "columns";
  private static final String JSON_KEY_PRIME_COLUMNS = "primeCols";
  private static final String JSON_KEY_SORT_COLUMN = "sortCol";
  private static final String JSON_KEY_READ_SECURITY_TABLE_ID = 
      "readAccessTid";
  private static final String JSON_KEY_WRITE_SECURITY_TABLE_ID = 
      "writeAccessTid";
  private static final String JSON_KEY_OV_VIEW_SETTINGS = "ovViewSettings";
  private static final String JSON_KEY_CO_VIEW_SETTINGS = "coViewSettings";
  private static final String JSON_KEY_DETAIL_VIEW_FILE = "detailViewFile";
  private static final String JSON_KEY_SUM_DISPLAY_FORMAT = 
      "summaryDisplayFormat";

  // the SQL where clause to use for selecting, updating, or deleting the row
  // for a given table
  private static final String ID_WHERE_SQL = DB_TABLE_ID + " = ?";
  // the SQL where clause to use for selecting by table type
  private static final String TYPE_WHERE_SQL = DB_TABLE_TYPE + " = ?";
  // the columns to be selected when initializing TableProperties
  private static final String[] INIT_COLUMNS = { 
    DB_TABLE_ID, 
    DB_DB_TABLE_NAME, 
    DB_DISPLAY_NAME,  
    DB_TABLE_TYPE, 
    DB_COLUMN_ORDER, 
    DB_PRIME_COLUMNS, 
    DB_SORT_COLUMN, 
    DB_READ_SECURITY_TABLE_ID,
    DB_WRITE_SECURITY_TABLE_ID, 
    DB_SYNC_TAG, 
    DB_LAST_SYNC_TIME, 
    DB_OV_VIEW_SETTINGS,
    DB_CO_VIEW_SETTINGS, 
    DB_DETAIL_VIEW_FILE, 
    DB_SUM_DISPLAY_FORMAT, 
    DB_SYNC_STATE,
    DB_TRANSACTIONING};
  // columns included in json properties
  private static final List<String> JSON_COLUMNS = Arrays.asList(new String[] { 
      DB_TABLE_ID,
      DB_DB_TABLE_NAME, 
      DB_DISPLAY_NAME, 
      DB_TABLE_TYPE, 
      DB_COLUMN_ORDER, 
      DB_PRIME_COLUMNS,
      DB_SORT_COLUMN, 
      DB_READ_SECURITY_TABLE_ID, 
      DB_WRITE_SECURITY_TABLE_ID, 
      DB_OV_VIEW_SETTINGS,
      DB_CO_VIEW_SETTINGS, 
      DB_DETAIL_VIEW_FILE, 
      DB_SUM_DISPLAY_FORMAT, });

  public class TableType {
    public static final int DATA = 0;
    public static final int SECURITY = 1;
    public static final int SHORTCUT = 2;

    private TableType() {
    }
  }

  public class ViewType {
    public static final int TABLE = 0;
    public static final int LIST = 1;
    public static final int LINE_GRAPH = 2;
    public static final int COUNT = 3;

    private ViewType() {
    }
  }
  
  // This is the Type of the key value store where the properties reside.
  private final KeyValueStore.Type backingStore;

  private final DbHelper dbh;
  private final String[] whereArgs;

  private final String tableId;
  private String dbTableName;
  private String displayName;
  private int tableType;
  private ColumnProperties[] columns;
  private ArrayList<String> columnOrder;
  private ArrayList<String> primeColumns;
  private String sortColumn;
  private String readSecurityTableId;
  private String writeSecurityTableId;
  private String syncTag;
  private String lastSyncTime;
  private TableViewSettings overviewViewSettings;
  private TableViewSettings collectionViewSettings;
  private String detailViewFilename;
  private String sumDisplayFormat;
  private int syncState;
  private boolean transactioning;

  private TableProperties(DbHelper dbh, 
      String tableId, 
      String dbTableName, 
      String displayName,
      int tableType, 
      ArrayList<String> columnOrder, 
      ArrayList<String> primeColumns, 
      String sortColumn,
      String readSecurityTableId, 
      String writeSecurityTableId, 
      String syncTag, 
      String lastSyncTime,
      String ovViewSettingsDbString, 
      String coViewSettingsDbString, 
      String detailViewFilename,
      String sumDisplayFormat, 
      int syncState, 
      boolean transactioning,
      KeyValueStore.Type backingStore) {
    this.dbh = dbh;
    whereArgs = new String[] { String.valueOf(tableId) };
    this.tableId = tableId;
    this.dbTableName = dbTableName;
    this.displayName = displayName;
    this.tableType = tableType;
    columns = null;
    this.columnOrder = columnOrder;
    this.primeColumns = primeColumns;
    this.sortColumn = sortColumn;
    this.readSecurityTableId = readSecurityTableId;
    this.writeSecurityTableId = writeSecurityTableId;
    this.syncTag = syncTag;
    this.lastSyncTime = lastSyncTime;
    this.overviewViewSettings = 
        TableViewSettings.newOverviewTVS(this, ovViewSettingsDbString);
    this.collectionViewSettings = 
        TableViewSettings.newCollectionTVS(this, coViewSettingsDbString);
    this.detailViewFilename = detailViewFilename;
    this.sumDisplayFormat = sumDisplayFormat;
    this.syncState = syncState;
    this.transactioning = transactioning;
    this.backingStore = backingStore;
  }

  /**
   * Return the TableProperties for the given table id.
   * @param dbh
   * @param tableId
   * @param typeOfStore the store from which to get the properties
   * @return
   */
  public static TableProperties getTablePropertiesForTable(DbHelper dbh,
      String tableId, KeyValueStore.Type typeOfStore) {
	    SQLiteDatabase db = null;
	    try {
	        db = dbh.getReadableDatabase();
		    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
		    KeyValueStore intendedKVS = kvsm.getStoreForTable(tableId, 
		        typeOfStore);
		    Map<String, String> mapProps = intendedKVS
		        .getProperties(db);
		    db.close();
		    db = null;
		    return constructPropertiesFromMap(dbh, mapProps, typeOfStore);
	    } finally {
	    	if ( db != null ) {
	    		db.close();
	    	}
	    }
  }
  
  /**
   * Return the TableProperties for all the tables in the specified KVS.
   * store.
   * @param dbh
   * @param typeOfStore the KVS from which to get the store
   * @return
   */
  public static TableProperties[] getTablePropertiesForAll(DbHelper dbh,
      KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
        db = dbh.getReadableDatabase();
	    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	    List<String> allIds = kvsm.getAllIdsFromStore(db, typeOfStore);
	    return constructPropertiesFromIds(allIds, dbh, db, kvsm, typeOfStore);
    } finally {
    	if ( db != null ) {
    		db.close();
    	}
    }
  }

  /**
   * Get the TableProperties for all the tables that have synchronized set
   * to true in the sync KVS. typeOfStore tells you the KVS (active, default,
   * or server) from which to construct the properties.
   * @param dbh
   * @param typeOfStore the KVS from which to get the properties
   * @return
   */
  public static TableProperties[] getTablePropertiesForSynchronizedTables(
      DbHelper dbh, KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
        db = dbh.getReadableDatabase();
	    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	    List<String> synchedIds = kvsm.getSynchronizedTableIds(db);    
	    return constructPropertiesFromIds(synchedIds, dbh, db, kvsm, typeOfStore);
    } finally {
    	if ( db != null ) {
    		db.close();
    	}
    }
  }
  
  /**
   * Get the default TableProperties for all the tables that have 
   * synchronized set to true.
   * @param dbh
   * @param intendedStore the KVS from which to get the properties
   */
  /*public static TableProperties[] getDefaultPropertiesForSynchronizedTables(
      DbHelper dbh, KeyValueStore.Type intendedStore) {
    SQLiteDatabase db = dbh.getReadableDatabase();
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    List<String> synchedIds = kvsm.getSynchronizedTableIds(db);
    return constructDefaultPropertiesFromIds(synchedIds, dbh, db, kvsm);
  }*/
  
  /**
   * Get the sever TableProperties for all the tables that have synchronized
   * set to true in the active store.
   */
  /*public static TableProperties[] getServerPropertiesForSynchronizedTables(
      DbHelper dbh) {
    SQLiteDatabase db = dbh.getReadableDatabase();
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    List<String> synchedIds = kvsm.getSynchronizedTableIds(db);
    return constructServerPropertiesFromIds(synchedIds, dbh, db, kvsm);
  }*/
      

  /**
   * Get the TableProperties for all the tables of all the type data
   * in the intended store.
   * @param dbh
   * @param typeOfStore the KVS from which to get the properties
   * @return
   */
  public static TableProperties[] getTablePropertiesForDataTables(
      DbHelper dbh, KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
        db = dbh.getReadableDatabase();
	    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	    List<String> dataIds = kvsm.getDataTableIds(db, typeOfStore);
	    return constructPropertiesFromIds(dataIds, dbh, db, kvsm, typeOfStore);
    } finally {
    	if ( db != null ) {
    		db.close();
    	}
    }
  }
  
  /**
   * Return all the TableProperties for the tables in the server KVS that are
   * of type DATA.
   * @param dbh
   * @return
   */
  /*public static TableProperties[] getTablePropertiesForServerDataTables(
      DbHelper dbh) {
    SQLiteDatabase db = dbh.getReadableDatabase();
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    List<String> dataIds = kvsm.getServerDataTableIds(db);
    return constructActivePropertiesFromIds(dataIds, dbh, db, kvsm);  }*/

  
  /**
   * Get the TableProperties for all the tables of all the type security
   * in the active key value store.
   * @param dbh
   * @param typeOfStore the KVS from which to get the properties
   * @return
   */
  public static TableProperties[] getTablePropertiesForSecurityTables(
      DbHelper dbh, KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
        db = dbh.getReadableDatabase();
	    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	    List<String> securityIds = kvsm.getSecurityTableIds(db, typeOfStore);
	    return constructPropertiesFromIds(securityIds, dbh, db, kvsm, typeOfStore);
    } finally {
    	if ( db != null ) {
    		db.close();
    	}
    }
  }

  /**
   * Get the TableProperties for all the tables of all the type shortcut
   * in the intended store.
   * @param dbh
   * @param typeOfStore the KVS from which to get the properties
   * @return
   */
  public static TableProperties[] getTablePropertiesForShortcutTables(
      DbHelper dbh, KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
    	db = dbh.getReadableDatabase();
	    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	    List<String> shortcutIds = kvsm.getShortcutTableIds(db, typeOfStore);
	    return constructPropertiesFromIds(shortcutIds, dbh, db, kvsm, typeOfStore);
    } finally {
    	if ( db != null ) {
    		db.close();
    	}
    }
  }
  
  /*
   * Constructs a table properties object based on a map of key values as
   * would be acquired from the key value store.
   */
  private static TableProperties constructPropertiesFromMap(DbHelper dbh,
      Map<String,String> props, KeyValueStore.Type backingStore) {
    // first we have to get the appropriate type for the non-string fields.
    String tableTypeStr = props.get(DB_TABLE_TYPE);
    int tableType = Integer.parseInt(tableTypeStr);
    String syncStateStr = props.get(DB_SYNC_STATE);
    int syncState = Integer.parseInt(syncStateStr);
    String transactioningStr = props.get(DB_TRANSACTIONING);
    int transactioningInt = Integer.parseInt(transactioningStr);
    boolean transactioning = SyncUtil.intToBool(transactioningInt);
    String columnOrderValue = props.get(DB_COLUMN_ORDER);
    // for legacy reasons, the code expects the DB_COLUMN_ORDER and
    // DB_PRIME_COLUMN values to be empty strings, not null. However, when
    // retrieving values from the key value store, empty strings are converted
    // to null, because many others expect null values. For that reason, first
    // check here to set null values for these columns to empty strings.
    if (columnOrderValue == null)
      columnOrderValue = "";
    ArrayList<String> columnOrder = new ArrayList<String>();
    if ( columnOrderValue.length() != 0) {
    	try {
			columnOrder = mapper.readValue(columnOrderValue, ArrayList.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json: " + columnOrderValue);
		} catch (JsonMappingException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json: " + columnOrderValue);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json: " + columnOrderValue);
		}
    }
    String primeOrderValue = props.get(DB_PRIME_COLUMNS);
    if (primeOrderValue == null)
      primeOrderValue = "";
    ArrayList<String> primeList = new ArrayList<String>();
    if ( primeOrderValue.length() != 0) {
    	try {
			primeList = mapper.readValue(primeOrderValue, ArrayList.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json");
		} catch (JsonMappingException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json");
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json");
		}
    }
    return new TableProperties(dbh, 
        props.get(DB_TABLE_ID),
        props.get(DB_DB_TABLE_NAME),
        props.get(DB_DISPLAY_NAME),
        tableType,
        columnOrder,
        primeList,
        props.get(DB_SORT_COLUMN),
        props.get(DB_READ_SECURITY_TABLE_ID),
        props.get(DB_WRITE_SECURITY_TABLE_ID),
        props.get(DB_SYNC_TAG),
        props.get(DB_LAST_SYNC_TIME),
        props.get(DB_OV_VIEW_SETTINGS),
        props.get(DB_CO_VIEW_SETTINGS),
        props.get(DB_DETAIL_VIEW_FILE),
        props.get(DB_SUM_DISPLAY_FORMAT),
        syncState,
        transactioning,
        backingStore);  
  }
  
  /*
   * Construct an array of TableProperties based on a list of table ids that
   * you want from the database. Gets from the active store.
   */
  /*private static TableProperties[] constructActivePropertiesFromIds(
      List<String> ids, DbHelper dbh, SQLiteDatabase db, 
      KeyValueStoreManager kvsm) {
    TableProperties[] allProps = new TableProperties[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      String tableId = ids.get(i);
      KeyValueStore currentActive = 
          kvsm.getActiveStoreForTable(tableId);
      Map<String, String> propPairs = currentActive.getProperties(db);
      allProps[i] = constructPropertiesFromMap(dbh, propPairs);
    }
    return allProps;
  }*/
  
  /*
   * Construct an array of table properties for the given ids. The properties
   * are collected from the intededStore.
   */
  private static TableProperties[] constructPropertiesFromIds(
      List<String> ids, DbHelper dbh, SQLiteDatabase db,
      KeyValueStoreManager kvsm, KeyValueStore.Type typeOfStore) {
    TableProperties[] allProps = new TableProperties[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      String tableId = ids.get(i);
      KeyValueStore intendedKVS = 
          kvsm.getStoreForTable(tableId, typeOfStore);
      Map<String, String> propPairs = intendedKVS.getProperties(db);
      allProps[i] = constructPropertiesFromMap(dbh, propPairs, typeOfStore);
    }
    return allProps;   
  }
  
  /*
   * Construct an array of TableProperties based on the list of table ids. 
   * It is very important to note that these properties will be as are 
   * reflected in the DEFAULT key value store. 
   */
  /*private static TableProperties[] constructDefaultPropertiesFromIds(
      List<String> ids, DbHelper dbh, SQLiteDatabase db,
      KeyValueStoreManager kvsm) {
    TableProperties[] allProps = new TableProperties[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      String tableId = ids.get(i);
      KeyValueStore currentDefault = 
          kvsm.getDefaultStoreForTable(tableId);
      Map<String, String> propPairs = currentDefault.getProperties(db);
      allProps[i] = constructPropertiesFromMap(dbh, propPairs);
    }
    return allProps;     
  }*/
  
  /*
   * Construct an array of TableProperties based on the list of table ids.
   * It is very important to note that these properties will be as they are
   * reflected in the SERVER key value store.
   */
  /*private static TableProperties[] constructServerPropertiesFromIds(
      List<String> ids, DbHelper dbh, SQLiteDatabase db, 
      KeyValueStoreManager kvsm) {
    TableProperties[] allProps = new TableProperties[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      String tableId = ids.get(i);
      KeyValueStore currentServer = 
          kvsm.getServerStoreForTable(tableId);
      Map<String, String> propPairs = currentServer.getProperties(db);
      allProps[i] = constructPropertiesFromMap(dbh, propPairs);
    }
    return allProps;
  }*/

  /**
   * Returns a legal name for a new table. This method checks all three KVS
   * for possible conflicts.
   * @param dbh
   * @param displayName
   * @return
   */
  public static String createDbTableName(DbHelper dbh, String displayName) {
    // so we want all properties. we're just going to check all of them, which
    // probably isn't the most efficient way this could be done, as you could
    // end up with triplicate table names. Going to not worry about it, as 
    // this is probably just O(3N)?
    TableProperties[] activeProps = getTablePropertiesForAll(dbh,
        KeyValueStore.Type.ACTIVE);
    TableProperties[] defaultProps = getTablePropertiesForAll(dbh,
        KeyValueStore.Type.DEFAULT);
    TableProperties[] serverProps = getTablePropertiesForAll(dbh,
        KeyValueStore.Type.SERVER);
    // this arraylist will hold all the properties.
    ArrayList<TableProperties> listProps = new ArrayList<TableProperties>();
    listProps.addAll(Arrays.asList(activeProps));
    listProps.addAll(Arrays.asList(defaultProps));
    listProps.addAll(Arrays.asList(serverProps));
    TableProperties[] allProps = 
        listProps.toArray(new TableProperties[listProps.size()]);
    // You cannot start with a digit, and you can only have alphanumerics
    // in SQLite. We are going to thus make the basename the displayName
    // prepended with an underscore, and replace all non-word characters
    // with an underscore.
    String baseName = "_" + displayName.replaceAll("\\W", "_");
    if (!nameConflict(baseName, allProps)) {
      return baseName;
    }
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!nameConflict(nextName, allProps)) {
        return nextName;
      }
      suffix++;
    }
  }

  private static boolean nameConflict(String dbTableName, 
      TableProperties[] allProps) {
    for (TableProperties tp : allProps) {
      if (tp.getDbTableName().equals(dbTableName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add a table to the database. The intendedStore type exists to force you
   * to be specific to which store you are adding the table to.
   * @param dbh
   * @param dbTableName
   * @param displayName
   * @param tableType
   * @param intendedStore type of the store to which you're adding. 
   * @return
   */
  public static TableProperties addTable(DbHelper dbh, String dbTableName, 
      String displayName, int tableType, KeyValueStore.Type intendedStore) {
    String id = UUID.randomUUID().toString();
    TableProperties tp = addTable(dbh, dbTableName, displayName, tableType, 
        id, intendedStore);
    if (tableType == TableType.SHORTCUT) {
      tp.addColumn("label", ShortcutUtil.LABEL_COLUMN_NAME);
      tp.addColumn("input", ShortcutUtil.INPUT_COLUMN_NAME);
      tp.addColumn("output", ShortcutUtil.OUTPUT_COLUMN_NAME);
    } else if (tableType == TableType.SECURITY) {
      tp.addColumn("user", SecurityUtil.USER_COLUMN_NAME);
      tp.addColumn("phone_number", SecurityUtil.PHONENUM_COLUMN_NAME);
      tp.addColumn("password", SecurityUtil.PASSWORD_COLUMN_NAME);
    }
    return tp;
  }

  /**
   * Add a table to the database. The intendedStore type exists to force you
   * to be specific to which store you are adding the table to.
   * @param dbh
   * @param dbTableName
   * @param displayName
   * @param tableType
   * @param id
   * @param typeOfStore
   * @return
   */
  public static TableProperties addTable(DbHelper dbh, String dbTableName, 
      String displayName, int tableType, String id, 
      KeyValueStore.Type typeOfStore) {
    // We want an entry for each of these values.
    List<OdkTablesKeyValueStoreEntry> values = 
        new ArrayList<OdkTablesKeyValueStoreEntry>();
    values.add(createStringEntry(id, DB_TABLE_ID, id));
    values.add(createStringEntry(id, DB_DB_TABLE_NAME, dbTableName));
    values.add(createStringEntry(id, DB_DISPLAY_NAME, displayName));
    values.add(createIntEntry(id, DB_TABLE_TYPE, Integer.toString(tableType)));
    values.add(createStringEntry(id, DB_COLUMN_ORDER, ""));
    values.add(createStringEntry(id, DB_PRIME_COLUMNS, ""));
    values.add(createStringEntry(id, DB_SORT_COLUMN, ""));
    values.add(createStringEntry(id, DB_READ_SECURITY_TABLE_ID, ""));
    values.add(createStringEntry(id, DB_WRITE_SECURITY_TABLE_ID, ""));
    values.add(createStringEntry(id, DB_SYNC_TAG, ""));
    values.add(createIntEntry(id, DB_LAST_SYNC_TIME, Integer.toString(-1)));
    values.add(createStringEntry(id, DB_OV_VIEW_SETTINGS, ""));
    values.add(createStringEntry(id, DB_CO_VIEW_SETTINGS, ""));
    values.add(createStringEntry(id, DB_DETAIL_VIEW_FILE, ""));
    values.add(createStringEntry(id, DB_SUM_DISPLAY_FORMAT, ""));
    values.add(createIntEntry(id, DB_SYNC_STATE, 
        Integer.toString(SyncUtil.State.INSERTING)));
    values.add(createIntEntry(id, DB_TRANSACTIONING, "0"));
    TableProperties tp = new TableProperties(dbh, id, dbTableName, displayName,
        tableType, new ArrayList<String>(), new ArrayList<String>(), null, null, null, null, null, 
        null, null, null, null, SyncUtil.State.INSERTING, false, typeOfStore);
    tp.getColumns(); // ensuring columns are already initialized
    KeyValueStoreManager kvms = KeyValueStoreManager.getKVSManager(dbh);
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      KeyValueStore typedStore = kvms.getStoreForTable(id, 
	          typeOfStore);
	      typedStore.addEntriesToStore(db, values);
	      Log.d(TAG, "adding table: " + dbTableName);
	      DbTable.createDbTable(db, tp);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      e.printStackTrace();
	    } finally {
	      db.endTransaction();
	    }
	    return tp;
    } finally {
    	db.close();
    }
  }
  
  /*
   * Creates a key value store entry of the type String.
   */
  private static OdkTablesKeyValueStoreEntry createStringEntry(String tableId,
      String key, String value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.type = ColumnType.TEXT.name();
    entry.value = value;
    entry.key = key;
    return entry;
  }
  
  /*
   * Creates a key value store entry of the type int.
   */
  private static OdkTablesKeyValueStoreEntry createIntEntry(String tableId,
      String key, String value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.type = ColumnType.INTEGER.name();
    entry.value = value;
    entry.key = key;
    return entry;
  }
  
  /*
   * Creates a key value store of the type boolean.
   */
  private static OdkTablesKeyValueStoreEntry createBoolEntry(String tableId,
      String key, String value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.type = "Boolean";
    entry.value = value;
    entry.key = key;
    return entry;
  }

  public void deleteTable() {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVSM = kvsm.getSyncStoreForTable(tableId);
    boolean isSetToSync = syncKVSM.isSetToSync();
    // hilary's original
    //if (isSynched && (syncState == SyncUtil.State.REST || syncState == SyncUtil.State.UPDATING))
      if (isSetToSync && (syncState == SyncUtil.State.REST 
                           || syncState == SyncUtil.State.UPDATING))
      setSyncState(SyncUtil.State.DELETING);
    // hilary's original
    //else if (!isSynched || syncState == SyncUtil.State.INSERTING)
    else if (!isSetToSync || syncState == SyncUtil.State.INSERTING)
      deleteTableActual();
  }

  /**
   * Remove the table from the database. This cannot be undone.
   */
  public void deleteTableActual() {
    // Two things must be done: delete all the key value pairs from the active
    // key value store and drop the table holding the data from the database.
    ColumnProperties[] columns = getColumns();
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      db.execSQL("DROP TABLE " + dbTableName);
	      for (ColumnProperties cp : columns) {
	        cp.deleteColumn(db);
	      }
	      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	      KeyValueStore activeKVS = kvsm.getStoreForTable(this.tableId,
	          KeyValueStore.Type.ACTIVE);
	      activeKVS.clearKeyValuePairs(db);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      e.printStackTrace();
	      Log.e(TAG, "error deleting table: " + this.tableId);
	    } finally{
	      db.endTransaction();
	    }
    } finally {
    	db.close();
    }
  }

  public String getTableId() {
    return tableId;
  }

  /**
   * @return the table's name in the database
   */
  public String getDbTableName() {
    return dbTableName;
  }

  /**
   * @return the table's display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the table's display name.
   * 
   * @param displayName
   *          the new display name
   */
  public void setDisplayName(String displayName) {
    setStringProperty(DB_DISPLAY_NAME, displayName);
    this.displayName = displayName;
  }

  /**
   * @return the table's type
   */
  public int getTableType() {
    return tableType;
  }

  /**
   * Sets the table's type.
   * 
   * @param tableType
   *          the new table type
   */
  public void setTableType(int tableType) {
    setIntProperty(DB_TABLE_TYPE, tableType);
    this.tableType = tableType;
  }

  /**
   * @return an unordered array of the table's columns
   */
  public ColumnProperties[] getColumns() {
    if (columns == null) {
      columns = ColumnProperties.getColumnPropertiesForTable(dbh, tableId);
      orderColumns();
    }
    return columns;
  }

  private void orderColumns() {
    ColumnProperties[] newColumns = new ColumnProperties[columns.length];
    for (int i = 0; i < columnOrder.size(); i++) {
      for (int j = 0; j < columns.length; j++) {
        if (columns[j].getColumnDbName().equals(columnOrder.get(i))) {
          newColumns[i] = columns[j];
          break;
        }
      }
    }
    columns = newColumns;
  }

  public ColumnProperties getColumnByDbName(String colDbName) {
    int colIndex = getColumnIndex(colDbName);
    if (colIndex < 0) {
      return null;
    }
    return getColumns()[colIndex];
  }

  public int getColumnIndex(String colDbName) {
    ArrayList<String> colOrder = getColumnOrder();
    for (int i = 0; i < colOrder.size(); i++) {
      if (colOrder.get(i).equals(colDbName)) {
        return i;
      }
    }
    return -1;
  }

  public String getColumnByDisplayName(String displayName) {
    ColumnProperties[] cps = getColumns();
    for (ColumnProperties cp : cps) {
      String cdn = cp.getDisplayName();
      if ((cdn != null) && (cdn.equalsIgnoreCase(displayName))) {
        return cp.getColumnDbName();
      }
    }
    return null;
  }

  public String getColumnByAbbreviation(String abbreviation) {
    ColumnProperties[] cps = getColumns();
    for (ColumnProperties cp : cps) {
      String ca = cp.getSmsLabel();
      if ((ca != null) && (ca.equalsIgnoreCase(abbreviation))) {
        return cp.getColumnDbName();
      }
    }
    return null;
  }

  public ColumnProperties getColumnByUserLabel(String name) {
    ColumnProperties[] cps = getColumns();
    for (ColumnProperties cp : cps) {
      String cdn = cp.getDisplayName();
      if (cdn.equalsIgnoreCase(name)) {
        return cp;
      }
    }
    for (ColumnProperties cp : cps) {
      String ca = cp.getSmsLabel();
      if ((ca != null) && ca.equalsIgnoreCase(name)) {
        return cp;
      }
    }
    return null;
  }

  /**
   * Adds a column to the table using a default database name.
   * 
   * @param displayName
   *          the column's display name
   * @return ColumnProperties for the new table
   */
  public ColumnProperties addColumn(String displayName) {
    // ensuring columns is initialized
    getColumns();
    // determining a database name for the column
    String baseName = "_" + displayName.toLowerCase().replace(' ', '_');
    if (!columnNameConflict(baseName)) {
      return addColumn(displayName, baseName);
    }
    int suffix = 1;
    while (true) {
      String name = baseName + suffix;
      if (!columnNameConflict(name)) {
        return addColumn(displayName, name);
      }
      suffix++;
    }
  }

  private boolean columnNameConflict(String name) {
    for (ColumnProperties cp : columns) {
      if (cp.getColumnDbName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a column to the table.
   * 
   * @param displayName
   *          the column's display name
   * @param dbName
   *          the database name for the new column
   * @return ColumnProperties for the new table
   */
  public ColumnProperties addColumn(String displayName, String dbName) {
    // ensuring columns is initialized
    getColumns();
    // preparing column order
    ColumnProperties[] newColumns = new ColumnProperties[columns.length + 1];
    ArrayList<String> newColumnOrder = new ArrayList<String>();
    for (int i = 0; i < columns.length; i++) {
      newColumns[i] = columns[i];
      newColumnOrder.add(columnOrder.get(i));
    }
    newColumnOrder.add(dbName);
    // adding column
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    ColumnProperties cp = null;
	    db.beginTransaction();
	    try {
	      cp = ColumnProperties.addColumn(dbh, db, tableId, dbName, displayName);
	      db.execSQL("ALTER TABLE " + dbTableName + " ADD COLUMN " + dbName);
	      setColumnOrder(newColumnOrder, db);
	      Log.d("TP", "here we are");
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      e.printStackTrace();
	      Log.e(TAG, "error adding column: " + displayName);
	    } finally {
	      db.endTransaction();
	    }
	    // updating TableProperties
	    newColumns[columns.length] = cp;
	    columns = newColumns;
	    // returning new ColumnProperties
	    return cp;
    } finally {
    	db.close();
    }
  }

  /**
   * Deletes a column from the table.
   * 
   * @param columnDbName
   *          the database name of the column to delete
   */
  public void deleteColumn(String columnDbName) {
    // ensuring columns is initialized
    getColumns();
    // finding the index of the column in columns
    int colIndex = 0;
    for (ColumnProperties cp : columns) {
      if (cp.getColumnDbName().equals(columnDbName)) {
        break;
      } else {
        colIndex++;
      }
    }
    if (colIndex == columns.length) {
      Log.e(TableProperties.class.getName(), "deleteColumn() did not find the column");
      return;
    }
    // forming a comma-separated list of columns to keep
    String csv = DbTable.DB_CSV_COLUMN_LIST;
    for (int i = 0; i < columns.length; i++) {
      if (i == colIndex) {
        continue;
      }
      csv += ", " + columns[i].getColumnDbName();
    }
    // updating TableProperties
    ColumnProperties[] newColumns = new ColumnProperties[columns.length - 1];
    int index = 0;
    for (int i = 0; i < columns.length; i++) {
      if (i == colIndex) {
        continue;
      }
      newColumns[index] = columns[i];
      index++;
    }
    ColumnProperties colToDelete = columns[colIndex];
    columns = newColumns;
    ArrayList<String> newColumnOrder = new ArrayList<String>();
    index = 0;
    for (String col : columnOrder) {
      if (col.equals(columnDbName)) {
        continue;
      }
      newColumnOrder.add(col);
      index++;
    }
    setColumnOrder(newColumnOrder);
    // deleting the column
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      colToDelete.deleteColumn(db);
	      reformTable(db, columnOrder);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      e.printStackTrace();
	      Log.e(TAG, "error deleting column: " + columnDbName);
	    } finally {
	      db.endTransaction();
	    }
    } finally {
    	db.close();
    }
  }

  /**
   * Reforms the table.
   */
  public void reformTable(SQLiteDatabase db, ArrayList<String> columnOrder2) {
    StringBuilder csvBuilder = new StringBuilder(DbTable.DB_CSV_COLUMN_LIST);
    for (String col : columnOrder2) {
      csvBuilder.append(", " + col);
    }
    String csv = csvBuilder.toString();
    db.execSQL("CREATE TEMPORARY TABLE backup_(" + csv + ")");
    db.execSQL("INSERT INTO backup_ SELECT " + csv + " FROM " + dbTableName);
    db.execSQL("DROP TABLE " + dbTableName);
    DbTable.createDbTable(db, this);
    db.execSQL("INSERT INTO " + dbTableName + " SELECT " + csv + " FROM backup_");
    db.execSQL("DROP TABLE backup_");
  }

  /**
   * @return an ordered array of the database names of the table's columns
   */
  public ArrayList<String> getColumnOrder() {
    return columnOrder;
  }

  /**
   * Sets the column order.
   * 
   * @param colOrder
   *          an ordered array of the database names of the table's columns
   */
  public void setColumnOrder(ArrayList<String> colOrder) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
    	setColumnOrder(colOrder, db);
    } finally {
    	db.close();
    }
  }

  private void setColumnOrder(ArrayList<String> columnOrder, SQLiteDatabase db) {
	String colOrderList = null;
	try {
		colOrderList = mapper.writeValueAsString(columnOrder);
	} catch (JsonGenerationException e) {
		e.printStackTrace();
		Log.e(t, "illegal json ignored");
	} catch (JsonMappingException e) {
		e.printStackTrace();
		Log.e(t, "illegal json ignored");
	} catch (IOException e) {
		e.printStackTrace();
		Log.e(t, "illegal json ignored");
	}
    setStringProperty(DB_COLUMN_ORDER, colOrderList, db);
    this.columnOrder = columnOrder;
  }

  /**
   * @return an array of the database names of the prime columns
   */
  public ArrayList<String> getPrimeColumns() {
    return primeColumns;
  }

  public boolean isColumnPrime(String colDbName) {
    for (String prime : primeColumns) {
      if (prime.equals(colDbName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the table's prime columns.
   * 
   * @param primes
   *          an array of the database names of the table's prime columns
   */
  public void setPrimeColumns(ArrayList<String> primes) {
    String str = "";
    for (String cdb : primes) {
      str += cdb + "/";
    }
    if (str.length() > 0) {
      str = str.substring(0, str.length() - 1);
    }
    setStringProperty(DB_PRIME_COLUMNS, str);
    this.primeColumns = primes;
  }

  /**
   * @return the database name of the sort column (or null for no sort column)
   */
  public String getSortColumn() {
    return sortColumn;
  }

  /**
   * Sets the table's sort column.
   * 
   * @param sortColumn
   *          the database name of the new sort column (or null for no sort
   *          column)
   */
  public void setSortColumn(String sortColumn) {
    if ((sortColumn != null) && (sortColumn.length() == 0)) {
      sortColumn = null;
    }
    setStringProperty(DB_SORT_COLUMN, sortColumn);
    this.sortColumn = sortColumn;
  }

  /**
   * @return the ID of the read security table, or null if there is none
   */
  public String getReadSecurityTableId() {
    return readSecurityTableId;
  }

  /**
   * Sets the table's read security table.
   * 
   * @param tableId
   *          the ID of the new read security table (or null to set no read
   *          security table)
   */
  public void setReadSecurityTableId(String tableId) {
    setStringProperty(DB_READ_SECURITY_TABLE_ID, tableId);
    this.readSecurityTableId = tableId;
  }

  /**
   * @return the ID of the write security table, or null if there is none
   */
  public String getWriteSecurityTableId() {
    return writeSecurityTableId;
  }

  /**
   * Sets the table's write security table.
   * 
   * @param tableId
   *          the ID of the new write security table (or null to set no write
   *          security table)
   */
  public void setWriteSecurityTableId(String tableId) {
    setStringProperty(DB_WRITE_SECURITY_TABLE_ID, tableId);
    this.writeSecurityTableId = tableId;
  }

  /**
   * @return the sync tag (or null if the table has never been synchronized)
   */
  public String getSyncTag() {
    return syncTag;
  }

  /**
   * Sets the table's sync tag.
   * 
   * @param syncTag
   *          the new sync tag
   */
  public void setSyncTag(String syncTag) {
    setStringProperty(DB_SYNC_TAG, syncTag);
    this.syncTag = syncTag;
  }

  /**
   * @return the last synchronization time (in the format of
   *         {@link DataUtil#getNowInDbFormat()}.
   */
  public String getLastSyncTime() {
    return lastSyncTime;
  }

  /**
   * Sets the table's last synchronization time.
   * 
   * @param time
   *          the new synchronization time (in the format of
   *          {@link DataUtil#getNowInDbFormat()}).
   */
  public void setLastSyncTime(String time) {
    setStringProperty(DB_LAST_SYNC_TIME, time);
    this.lastSyncTime = time;
  }

  /**
   * @return the overview view settings
   */
  public TableViewSettings getOverviewViewSettings() {
    return overviewViewSettings;
  }

  /**
   * Sets the overview view settings.
   * 
   * @param dbString
   *          the string to put in the database
   */
  void setOverviewViewSettings(String dbString) {
    setStringProperty(DB_OV_VIEW_SETTINGS, dbString);
  }

  /**
   * @return the collection view settings
   */
  public TableViewSettings getCollectionViewSettings() {
    return collectionViewSettings;
  }

  /**
   * Sets the collection view settings.
   * 
   * @param dbString
   *          the string to put in the database
   */
  void setCollectionViewSettings(String dbString) {
    setStringProperty(DB_CO_VIEW_SETTINGS, dbString);
  }

  /**
   * @return the detail view filename
   */
  public String getDetailViewFilename() {
    return detailViewFilename;
  }

  /**
   * Sets the table's detail view filename.
   * 
   * @param filename
   *          the new filename
   */
  public void setDetailViewFilename(String filename) {
    setStringProperty(DB_DETAIL_VIEW_FILE, filename);
    this.detailViewFilename = filename;
  }

  /**
   * @return the format for summary displays
   */
  public String getSummaryDisplayFormat() {
    return sumDisplayFormat;
  }

  /**
   * Sets the table's summary display format.
   * 
   * @param format
   *          the new summary display format
   */
  public void setSummaryDisplayFormat(String format) {
    setStringProperty(DB_SUM_DISPLAY_FORMAT, format);
    this.sumDisplayFormat = format;
  }

  /**
   * @return the synchronization state
   */
  public int getSyncState() {
    return syncState;
  }

  /**
   * Sets the table's synchronization state. Can only move to or from the REST
   * state (e.g., no skipping straight from INSERTING to UPDATING).
   * 
   * @param state
   *          the new synchronization state
   */
  public void setSyncState(int state) {
    if (state == SyncUtil.State.REST || this.syncState == SyncUtil.State.REST) {
      setIntProperty(DB_SYNC_STATE, state);
      this.syncState = state;
    }
  }

  /**
   * @return the transactioning status
   */
  public boolean isTransactioning() {
    return transactioning;
  }

  /**
   * Sets the transactioning status.
   * 
   * @param transactioning
   *          the new transactioning status
   */
  public void setTransactioning(boolean transactioning) {
    setIntProperty(DB_TRANSACTIONING, SyncUtil.boolToInt(transactioning));
    this.transactioning = transactioning;
  }

  /**
   * Whether or not the table is set to be synched with the server.
   * @return
   */
  //public boolean isSynchronized() {
  //  return isSynched;
  //}

  /**
   * Set whether or not the table is due to be synced with the server.
   * @param isSynchronized
   */
  //public void setSynchronized(boolean isSynchronized) {
  //  setIntProperty(DB_IS_SYNCHED, SyncUtil.boolToInt(isSynchronized));
  //  this.isSynched = isSynchronized;
  //}

  public String toJson() {
    getColumns(); // ensuring columns is initialized
    ArrayList<String> colOrder = new ArrayList<String>();
    ArrayList<Object> cols = new ArrayList<Object>();
    for (ColumnProperties cp : columns) {
      colOrder.add(cp.getColumnDbName());
      cols.add(cp.toJsonObject());
    }
    ArrayList<String> primes = new ArrayList<String>();
    for (String prime : primeColumns) {
      primes.add(prime);
    }
    Map<String,Object> jo = new HashMap<String,Object>();
	  jo.put(JSON_KEY_VERSION, 1);
	  jo.put(JSON_KEY_TABLE_ID, tableId);
	  jo.put(JSON_KEY_DB_TABLE_NAME, dbTableName);
	  jo.put(JSON_KEY_DISPLAY_NAME, displayName);
	  jo.put(JSON_KEY_TABLE_TYPE, tableType);
	  jo.put(JSON_KEY_COLUMN_ORDER, colOrder);
	  jo.put(JSON_KEY_COLUMNS, cols);
	  jo.put(JSON_KEY_PRIME_COLUMNS, primes);
	  jo.put(JSON_KEY_SORT_COLUMN, sortColumn);
	  jo.put(JSON_KEY_READ_SECURITY_TABLE_ID, readSecurityTableId);
	  jo.put(JSON_KEY_WRITE_SECURITY_TABLE_ID, writeSecurityTableId);
	  // TODO
	  jo.put(JSON_KEY_OV_VIEW_SETTINGS, overviewViewSettings.toJsonObject());
	  // TODO
	  jo.put(JSON_KEY_CO_VIEW_SETTINGS, collectionViewSettings.toJsonObject());
	  jo.put(JSON_KEY_DETAIL_VIEW_FILE, detailViewFilename);
	  jo.put(JSON_KEY_SUM_DISPLAY_FORMAT, sumDisplayFormat);
	  
	  String toReturn = null;
	try {
		toReturn = mapper.writeValueAsString(jo);
	} catch (JsonGenerationException e) {
		e.printStackTrace();
	} catch (JsonMappingException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
    Log.d("TP", "json: " + toReturn);
    return toReturn;
  }

  /**
   * Called from CSV import and server synchronization primitives
   * 
   * @param json
   */
  public void setFromJson(String json) {
    getColumns();
    @SuppressWarnings("unchecked")
	Map<String, Object> jo;
	try {
		jo = mapper.readValue(json, Map.class);
	} catch (JsonParseException e) {
		e.printStackTrace();
		throw new IllegalArgumentException("invalid json: " + json);
	} catch (JsonMappingException e) {
		e.printStackTrace();
		throw new IllegalArgumentException("invalid json: " + json);
	} catch (IOException e) {
		e.printStackTrace();
		throw new IllegalArgumentException("invalid json: " + json);
	}
    
    ArrayList<String> colOrder = (ArrayList<String>) jo.get(JSON_KEY_COLUMN_ORDER);
    ArrayList<String> primes = (ArrayList<String>) jo.get(JSON_KEY_PRIME_COLUMNS);
    
	  setDisplayName((String) jo.get(JSON_KEY_DISPLAY_NAME));
	  setTableType((Integer) jo.get(JSON_KEY_TABLE_TYPE));
	  setPrimeColumns(primes);
	  setSortColumn((String) jo.get(JSON_KEY_SORT_COLUMN));
	  setReadSecurityTableId((String) jo.get(JSON_KEY_READ_SECURITY_TABLE_ID));
	  setWriteSecurityTableId((String) jo.get(JSON_KEY_WRITE_SECURITY_TABLE_ID));
	  if (jo.containsKey(JSON_KEY_OV_VIEW_SETTINGS)) {
	    // TODO
	  }
	  if (jo.containsKey(JSON_KEY_CO_VIEW_SETTINGS)) {
	    // TODO
	  }
	  setDetailViewFilename((String) jo.get(JSON_KEY_DETAIL_VIEW_FILE));
	  setSummaryDisplayFormat((String) jo.get(JSON_KEY_SUM_DISPLAY_FORMAT));
	  Set<String> columnsToDelete = new HashSet<String>();
	  for (String cdn : columnOrder) {
	    columnsToDelete.add(cdn);
	  }
	  ArrayList<Object> colJArr = (ArrayList<Object>) jo.get(JSON_KEY_COLUMNS);
	  for (int i = 0; i < colOrder.size(); i++) {
		  Map<String,Object> colJo = (Map<String, Object>) colJArr.get(i);
		  ColumnProperties cp = getColumnByDbName(colOrder.get(i));
          if (cp == null) {
            cp = addColumn(colOrder.get(i), colOrder.get(i));
          }
          cp.setFromJsonObject(colJo);
          columnsToDelete.remove(colOrder.get(i));
      }
      for (String columnToDelete : columnsToDelete) {
        deleteColumn(columnToDelete);
      }
      setColumnOrder(colOrder);
      orderColumns();
  }

  private void setIntProperty(String property, int value) {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVS = kvsm.getSyncStoreForTable(tableId);
    boolean isSetToSync = syncKVS.isSetToSync();
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      setIntProperty(property, value, db);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      e.printStackTrace();
	      Log.e(TAG, "error setting int property " + property + " with value " + 
	        value + " to table " + tableId);
	    } finally {
	      db.endTransaction();
	    }
	    // hilary's original:
	    //if (isSynched && syncState == SyncUtil.State.REST && JSON_COLUMNS.contains(property))
	    if (isSetToSync && syncState == SyncUtil.State.REST 
	        && JSON_COLUMNS.contains(property)) {
	      setSyncState(SyncUtil.State.UPDATING);
	    }
    } finally {
    	db.close();
    }
  }
  
  private void setIntProperty(String property, int value,
      SQLiteDatabase db) {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore backingKVS = kvsm.getStoreForTable(this.tableId,
        this.backingStore);
    backingKVS.insertOrUpdateKey(db, ColumnType.INTEGER.name(), property, 
        Integer.toString(value));
    Log.d(TAG, "updated int " + property + " to " + value + "for " + 
      this.tableId);
  }

  private void setStringProperty(String property, String value) {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVS = kvsm.getSyncStoreForTable(tableId);
    boolean isSetToSync = syncKVS.isSetToSync();
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      setStringProperty(property, value, db);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      Log.e(TAG, "error setting string property " + property + " to " + value
	          + " for " + this.tableId);
	    } finally {
	      db.endTransaction();
	    }
	    // hilary's original
	    //if (isSynched && syncState == SyncUtil.State.REST && JSON_COLUMNS.contains(property))
	    if (isSetToSync && syncState == SyncUtil.State.REST 
	        && JSON_COLUMNS.contains(property)) {
	      setSyncState(SyncUtil.State.UPDATING);
	    }
    } finally {
    	db.close();
    }
  }

  //TODO this should maybe be only transactionally?
  private void setStringProperty(String property, String value, 
      SQLiteDatabase db) {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore intendedKVS = kvsm.getStoreForTable(this.tableId,
        this.backingStore);
    intendedKVS.insertOrUpdateKey(db, ColumnType.TEXT.name(), property, value);
    Log.d(TAG, "updated string " + property + " to " + value + " for " 
      + this.tableId);
  }
  
  /**
   * Returns an array of the initialized properties. These are the keys that
   * exist in the key value store for any table.
   * @return
   */
  public static String[] getInitColumns() {
    return INIT_COLUMNS;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TableProperties)) {
      return false;
    }
    TableProperties other = (TableProperties) obj;
    return tableId.equals(other.tableId);
  }

  @Override
  public int hashCode() {
    return tableId.hashCode();
  }

  @Override
  public String toString() {
    return displayName;
  }
  
}
