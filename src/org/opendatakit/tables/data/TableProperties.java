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
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.tables.Activity.util.SecurityUtil;
import org.opendatakit.tables.Activity.util.ShortcutUtil;
import org.opendatakit.tables.exception.TableAlreadyExistsException;
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
 * <p>
 * NB sudar.sam@gmail.com: The properties of a table exist in two different
 * places in the datastore: the immutable (ish) properties that are part of
 * the table's definition (things like db backing name, type of the table, 
 * etc), are stored in the table_definitions table. The ODK Tables-specific
 * properties exist in the key value stores as described above.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 * @author sudar.sam@gmail.com
 */
public class TableProperties {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String t = "TableProperties";
  
  public static final String TAG = "TableProperties";
  
  /***********************************
   *  The partition and aspect of table properties in the key value store.
   ***********************************/
  public static final String KVS_PARTITION = "Table";
  public static final String KVS_ASPECT = "global";

  /***********************************
   *  The names of keys that are defaulted to exist in the key value store.
   ***********************************/
  public static final String KEY_DISPLAY_NAME = "displayName";
  public static final String KEY_COLUMN_ORDER = "colOrder";
  public static final String KEY_PRIME_COLUMNS = "primeCols";
  public static final String KEY_SORT_COLUMN = "sortCol";
  public static final String KEY_INDEX_COLUMN = "indexCol";
  public static final String KEY_CURRENT_VIEW_TYPE = "currentViewType";
//  public static final String KEY_DETAIL_VIEW_FILE = "detailViewFile";
  public static final String KEY_SUM_DISPLAY_FORMAT = "summaryDisplayFormat";
  // this should just be the saved query on a table. it isn't a property, it's
  // just a key. The default is the empty string.
  public static final String KEY_CURRENT_QUERY = "currentQuery";
  /*
   * Keys that can exist in the key value store but are not defaulted to exist
   * can be added her just as a sanity check. Note that they are NOT guaranteed
   * to be here. The general convention is going to be that these keys should
   * be defined in the respective classes that rely on them, and should be 
   * formatted as "Class.keyName." Eg "CollectUtil.formId".
   * 
   * Keys are known to exist in:
   * CollectUtil
   */

  // TODO atm not sure if we want to keep these things here, or if we should
  // be using their names from their respective sources.
  private static final String JSON_KEY_VERSION = "jVersion";
  private static final String JSON_KEY_TABLE_ID = "tableId";
  private static final String JSON_KEY_DB_TABLE_NAME = "dbTableName";
  private static final String JSON_KEY_DISPLAY_NAME = "displayName";
  private static final String JSON_KEY_TABLE_TYPE = "type";
  private static final String JSON_KEY_COLUMN_ORDER = "colOrder";
  private static final String JSON_KEY_COLUMNS = "columns";
  private static final String JSON_KEY_PRIME_COLUMNS = "primeCols";
  private static final String JSON_KEY_SORT_COLUMN = "sortCol";
  private static final String JSON_KEY_INDEX_COLUMN = "indexCol";
  private static final String JSON_KEY_READ_SECURITY_TABLE_ID = 
      "readAccessTid";
  private static final String JSON_KEY_WRITE_SECURITY_TABLE_ID = 
      "writeAccessTid";
  private static final String JSON_KEY_CURRENT_VIEW_TYPE = "currentViewType";
//  private static final String JSON_KEY_CURRENT_OVERVIEW_VIEW_TYPE = 
//      "currentOverviewViewType";
//  private static final String JSON_KEY_CURRENT_COLLECTION_VIEW_TYPE = 
//      "currentCollectionViewType";
//  private static final String JSON_KEY_DETAIL_VIEW_FILE = "detailViewFile";
  private static final String JSON_KEY_SUM_DISPLAY_FORMAT = 
      "summaryDisplayFormat";
  
  
  /***********************************
   *  Default values for those keys which require them.
   *  TODO When the keys in the KVS are moved to the respective classes that 
   *  use them, these should go there most likely.
   ***********************************/
  public static final String DEFAULT_KEY_PRIME_COLUMNS = "";
  public static final String DEFAULT_KEY_SORT_COLUMN = "";
  public static final String DEFAULT_KEY_INDEX_COLUMN = "";
  public static final String DEFAULT_KEY_CO_VIEW_SETTINGS = "";
  public static final String DEFAULT_KEY_CURRENT_VIEW_TYPE =
      TableViewType.Spreadsheet.name();
//  public static final String DEFAULT_KEY_CURRENT_OVERVIEW_VIEW_TYPE = 
//      TableViewType.Spreadsheet.name();
//  public static final String DEFAULT_KEY_CURRENT_COLLECTION_VIEW_TYPE =
//      TableViewType.Spreadsheet.name();
//  public static final String DEFAULT_KEY_DETAIL_VIEW_FILE = "";
  public static final String DEFAULT_KEY_SUM_DISPLAY_FORMAT = "";
  public static final String DEFAULT_KEY_OV_VIEW_SETTINGS = "";
  public static final String DEFAULT_KEY_COLUMN_ORDER = ""; 
  public static final String DEFAULT_KEY_CURRENT_QUERY = "";

  /*
   * These are the keys that exist in the key value store after the creation
   * of a table. In other words they should always exist in the key value 
   * store.
   */
  private static final String[] INIT_KEYS = { 
    KEY_DISPLAY_NAME,  
    KEY_COLUMN_ORDER, 
    KEY_PRIME_COLUMNS, 
    KEY_SORT_COLUMN, 
    KEY_CURRENT_VIEW_TYPE,
    KEY_INDEX_COLUMN,
//    KEY_CURRENT_OVERVIEW_VIEW_TYPE,
//    KEY_CURRENT_COLLECTION_VIEW_TYPE,
//    KEY_DETAIL_VIEW_FILE, 
    KEY_CURRENT_QUERY,
    KEY_SUM_DISPLAY_FORMAT};
  
  // columns included in json properties
  private static final List<String> JSON_COLUMNS = Arrays.asList(new String[] {  
      KEY_DISPLAY_NAME, 
      KEY_COLUMN_ORDER, 
      KEY_PRIME_COLUMNS,
      KEY_SORT_COLUMN, 
      KEY_CURRENT_VIEW_TYPE,
      KEY_INDEX_COLUMN,
//      KEY_CURRENT_OVERVIEW_VIEW_TYPE,
//      KEY_CURRENT_COLLECTION_VIEW_TYPE,
//      KEY_DETAIL_VIEW_FILE, 
      KEY_SUM_DISPLAY_FORMAT, });
  

 
  /***********************************
   *  The fields that make up a TableProperties object.
   ***********************************/  
  /*
   * The fields that belong only to the object, and are not related to the 
   * actual table itself.
   */
  // This is the Type of the key value store where the properties reside.
  private final KeyValueStore.Type backingStore;

  private final DbHelper dbh;
  private final String[] whereArgs;
  /*
   * The fields that reside in TableDefintions
   */
  private final String tableId;
  private String tableKey;
  private String dbTableName;
  private TableType tableType;
  private String accessControls;
  private String syncTag;
  // TODO lastSyncTime should probably eventually be an int?
  // keeping as a string for now to minimize errors.
  private String lastSyncTime;
  private SyncState syncState;
  private boolean transactioning;
  /*
   * The fields that are in the key value store.
   */
  private String displayName;
  private ColumnProperties[] columns;
  private ArrayList<String> columnOrder;
  private ArrayList<String> primeColumns;
  private String sortColumn;
  private String indexColumn;
//  private String readSecurityTableId;
//  private String writeSecurityTableId;
  private TableViewType currentViewType;
//  private TableViewType currentOverviewViewType;
//  private TableViewType currentCollectionViewType;
//  private String detailViewFilename;
  private String sumDisplayFormat;

  private TableProperties(DbHelper dbh, 
      String tableId, 
      String tableKey,
      String dbTableName, 
      String displayName,
      TableType tableType,
      String accessControls,
      ArrayList<String> columnOrder, 
      ArrayList<String> primeColumns, 
      String sortColumn,
      String indexColumn,
      String syncTag, 
      String lastSyncTime,
      TableViewType currentViewType,
//      TableViewType overviewViewType,
//      TableViewType collectionViewType,
//      String detailViewFilename,
      String sumDisplayFormat, 
      SyncState syncState, 
      boolean transactioning,
      KeyValueStore.Type backingStore) {
    this.dbh = dbh;
    whereArgs = new String[] { String.valueOf(tableId) };
    this.tableId = tableId;
    this.tableKey = tableKey;
    this.dbTableName = dbTableName;
    this.displayName = displayName;
    this.tableType = tableType;
    this.accessControls = accessControls;
    columns = null;
    this.columnOrder = columnOrder;
    this.primeColumns = primeColumns;
    this.sortColumn = sortColumn;
    this.indexColumn = indexColumn;
    this.accessControls = accessControls;
    this.syncTag = syncTag;
    this.lastSyncTime = lastSyncTime;
    this.currentViewType = currentViewType;
//    this.currentOverviewViewType = overviewViewType;
//    this.currentCollectionViewType = collectionViewType;
//    this.detailViewFilename = detailViewFilename;
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
    Map<String, String> mapProps = getMapOfPropertiesForTable(dbh, tableId, typeOfStore);
    return constructPropertiesFromMap(dbh, mapProps, typeOfStore);
  }
  
  /*
   * Return the map of all the properties for the given table. The properties
   * include both the values of this table's row in TableDefinition and the
   * values in the key value store pointed to by INIT_KEYS. 
   * 
   * Atm this is just
   * key->value. The caller must know the intended type of the value and 
   * parse it correctly. This map should eventually become a key->TypeValuePair
   * or something like that.
   * TODO: make it the above
   * 
   * This deserves its own method b/c to get the properties you are forced to 
   * go through both the key value store and the TableDefinitions table.
   * @param dbh
   * @param tableId
   * @param typeOfStore
   * @return
   */
  private static Map<String, String> getMapOfPropertiesForTable(DbHelper dbh,
      String tableId, KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
       db = dbh.getReadableDatabase();
       KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
       KeyValueStore intendedKVS = kvsm.getStoreForTable(tableId, 
           typeOfStore);
       Map<String, String> tableDefinitionsMap = 
           TableDefinitions.getFields(tableId, db);
       Map<String, String> kvsMap = intendedKVS.getProperties(db);
       Map<String, String> mapProps = new HashMap<String, String>();
       mapProps.putAll(tableDefinitionsMap);
       mapProps.putAll(kvsMap);
       // TODO: fix the when to close problem
//       db.close();
//       db = null;
       return mapProps;
    } finally {
      // TODO: fix the when to close problem
//      if ( db != null ) {
//         db.close();
//      }
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
      // TODO: resolve and fix this
//    	if ( db != null ) {
//    		db.close();
//    	}
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
      // TODO: fix the when to close problem
//    	if ( db != null ) {
//    		db.close();
//    	}
    }
  }
      

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
      // TODO: fix the when to close problem
//    	if ( db != null ) {
//    		db.close();
//    	}
    }
  }

  
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
      // TODO: fix the when to close problem
//    	if ( db != null ) {
//    		db.close();
//    	}
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
      // TODO: fix the when to close problem
//    	if ( db != null ) {
//    		db.close();
//    	}
    }
  }
  
  /*
   * Constructs a table properties object based on a map of key values as
   * would be acquired from the key value store.
   */
  private static TableProperties constructPropertiesFromMap(DbHelper dbh,
      Map<String,String> props, KeyValueStore.Type backingStore) {
    // first we have to get the appropriate type for the non-string fields.
    String tableTypeStr = props.get(TableDefinitions.DB_TYPE);
    TableType tableType = TableType.valueOf(tableTypeStr);
    String syncStateStr = props.get(TableDefinitions.DB_SYNC_STATE);
    SyncState syncState = SyncState.valueOf(syncStateStr);
    String transactioningStr = props.get(TableDefinitions.DB_TRANSACTIONING);
    int transactioningInt = Integer.parseInt(transactioningStr);
    boolean transactioning = SyncUtil.intToBool(transactioningInt);
    String columnOrderValue = props.get(KEY_COLUMN_ORDER);
    String currentViewTypeStr = props.get(KEY_CURRENT_VIEW_TYPE);
    TableViewType currentViewType = TableViewType.valueOf(currentViewTypeStr);
//    String overviewViewTypeStr = props.get(KEY_CURRENT_OVERVIEW_VIEW_TYPE);
//    TableViewType overviewViewType = 
//        TableViewType.valueOf(overviewViewTypeStr);
//    String collectionViewTypeStr = props.get(KEY_CURRENT_COLLECTION_VIEW_TYPE);
//    TableViewType collectionViewType = 
//        TableViewType.valueOf(collectionViewTypeStr);
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
    String primeOrderValue = props.get(KEY_PRIME_COLUMNS);
    if (primeOrderValue == null)
      primeOrderValue = "";
    ArrayList<String> primeList = new ArrayList<String>();
    if ( primeOrderValue.length() != 0) {
    	try {
			primeList = mapper.readValue(primeOrderValue, ArrayList.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json: " + primeList);
		} catch (JsonMappingException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json: " + primeList);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(t, "ignore invalid json: " + primeList);
		}
    }
    return new TableProperties(dbh, 
        props.get(TableDefinitions.DB_TABLE_ID),
        props.get(TableDefinitions.DB_TABLE_KEY),
        props.get(TableDefinitions.DB_DB_TABLE_NAME),
        props.get(KEY_DISPLAY_NAME),
        tableType,
        props.get(TableDefinitions.DB_TABLE_ID_ACCESS_CONTROLS),
        columnOrder,
        primeList,
        props.get(KEY_SORT_COLUMN),
        props.get(KEY_INDEX_COLUMN),
        props.get(TableDefinitions.DB_SYNC_TAG),
        props.get(TableDefinitions.DB_LAST_SYNC_TIME),
        currentViewType,
//        overviewViewType,
//        collectionViewType,
//        props.get(KEY_DETAIL_VIEW_FILE),
        props.get(KEY_SUM_DISPLAY_FORMAT),
        syncState,
        transactioning,
        backingStore);  
  }
  
  
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
//      KeyValueStore intendedKVS = 
//          kvsm.getStoreForTable(tableId, typeOfStore);
//      Map<String, String> propPairs = intendedKVS.getProperties(db);
      Map<String, String> propPairs = 
          getMapOfPropertiesForTable(dbh, tableId, typeOfStore);
      allProps[i] = constructPropertiesFromMap(dbh, propPairs, typeOfStore);
    }
    return allProps;   
  }
  

  /**
   * Returns a legal name for a new table. This method checks all three KVS
   * for possible conflicts. The name is the displayName prepended with an 
   * underscore, non word 
   * characters (as defined by java's "\\W" replaced by underscores, and a 
   * suffix of an integer if there was a 
   * conflict.
   * @param dbh
   * @param displayName
   * @return
   */
  // TODO with addition of the table definitions table, this probably needs to
  // be revised to check displaynames, backing names, etc.
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

  /**
   * Return true if a table in allProps has the dbTableName dbTableName.
   * @param dbTableName
   * @param allProps
   * @return
   */
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
   * Return true if a table in allProps has the id tableId.
   * @param tableId
   * @param allProps
   * @return
   */
  private static boolean tableIdConflict(String tableId, 
      TableProperties[] allProps) {
    for (TableProperties tp : allProps) {
      if (tp.getTableId().equals(tableId)) {
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
      String displayName, TableType tableType, KeyValueStore.Type intendedStore) {
    String id = UUID.randomUUID().toString();
    TableProperties tp = addTable(dbh, dbTableName, displayName, tableType, 
        id, intendedStore);
    SQLiteDatabase db = dbh.getWritableDatabase();
    if (tableType == TableType.shortcut) {
      tp.addColumn(ShortcutUtil.LABEL_COLUMN_NAME,
          ColumnProperties.createDbElementKey(id, 
              ShortcutUtil.LABEL_COLUMN_NAME, db),
          ColumnProperties.createDbElementName(id,
              ShortcutUtil.LABEL_COLUMN_NAME, db));
      tp.addColumn(ShortcutUtil.INPUT_COLUMN_NAME,
          ColumnProperties.createDbElementKey(id, 
              ShortcutUtil.INPUT_COLUMN_NAME, db),
          ColumnProperties.createDbElementName(id,
              ShortcutUtil.INPUT_COLUMN_NAME, db));
      tp.addColumn(ShortcutUtil.OUTPUT_COLUMN_NAME,
          ColumnProperties.createDbElementKey(id, 
              ShortcutUtil.OUTPUT_COLUMN_NAME, db),
          ColumnProperties.createDbElementName(id,
              ShortcutUtil.OUTPUT_COLUMN_NAME, db));
//      tp.addColumn("label", ShortcutUtil.LABEL_COLUMN_NAME);
//      tp.addColumn("input", ShortcutUtil.INPUT_COLUMN_NAME);
//      tp.addColumn("output", ShortcutUtil.OUTPUT_COLUMN_NAME);
    } else if (tableType == TableType.security) {
      tp.addColumn(SecurityUtil.USER_COLUMN_NAME,
          ColumnProperties.createDbElementKey(id, 
              SecurityUtil.USER_COLUMN_NAME, db),
          ColumnProperties.createDbElementName(id,
              SecurityUtil.USER_COLUMN_NAME, db));
      tp.addColumn(SecurityUtil.PHONENUM_COLUMN_NAME,
          ColumnProperties.createDbElementKey(id, 
              SecurityUtil.PHONENUM_COLUMN_NAME, db),
          ColumnProperties.createDbElementName(id,
              SecurityUtil.PHONENUM_COLUMN_NAME, db));
      tp.addColumn(SecurityUtil.PASSWORD_COLUMN_NAME,
          ColumnProperties.createDbElementKey(id, 
              SecurityUtil.PASSWORD_COLUMN_NAME, db),
          ColumnProperties.createDbElementName(id,
              SecurityUtil.PASSWORD_COLUMN_NAME, db));
//      tp.addColumn("user", SecurityUtil.USER_COLUMN_NAME);
//      tp.addColumn("phone_number", SecurityUtil.PHONENUM_COLUMN_NAME);
//      tp.addColumn("password", SecurityUtil.PASSWORD_COLUMN_NAME);
    }
    return tp;
  }
  
  /**
   * Add a table from the JSON representation of a TableProperties object, as
   * set from {@link toJson}. There is currently no control for versioning or 
   * anything of that nature. 
   * <p>
   * This method is equivalent to parsing the json object yourself to get the
   * id and database names, calling the appropriate {@link addTable} method,
   * and then calling {@link setFromJson}.
   * @param dbh
   * @param json
   * @param typeOfStore
   * @return
   * @throws TableAlreadyExistsException if the dbTableName or tableId 
   * specified
   * by the json string conflict with any of the properties from the three
   * key value stores. If so, nothing is done.
   */
  public static TableProperties addTableFromJson(DbHelper dbh, String json,
      KeyValueStore.Type typeOfStore) throws TableAlreadyExistsException {
    // we just need to reclaim the bare minimum that we need to call the other
    // methods.
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
    String tableId = (String) jo.get(JSON_KEY_TABLE_ID);
    String dbTableName = (String) jo.get(JSON_KEY_DB_TABLE_NAME);
    String dbTableType = (String) jo.get(JSON_KEY_TABLE_TYPE);
    String displayName = (String) jo.get(JSON_KEY_DISPLAY_NAME);
    // And now we need to check for conflicts that would mess up the database.
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
    if (nameConflict(dbTableName, allProps) || 
        tableIdConflict(tableId, allProps)) {
      Log.e(TAG, "a table already exists with the dbTableName: " + 
        dbTableName + " or with the tableId: " + tableId);
      throw new TableAlreadyExistsException("a table already exists with the" +
      		" dbTableName: " +  dbTableName + " or with the tableId: "
          + tableId);
    }
    TableProperties tp = addTable(dbh, dbTableName, displayName,
        TableType.valueOf(dbTableType), tableId, typeOfStore);
    tp.setFromJson(json);
    return tp;
  }

  /**
   * Add a table to the database. The intendedStore type exists to force you
   * to be specific to which store you are adding the table to.
   * <p>
   * NB: Currently adds all the keys defined in this class to the key value 
   * store as well. This should likely change.
   * <p>
   * NB: Sets the table_key and db_table_name in TableDefinitions both to
   * the dbTableName parameter.
   * @param dbh
   * @param dbTableName
   * @param displayName
   * @param tableType
   * @param id
   * @param typeOfStore
   * @return
   */
  public static TableProperties addTable(DbHelper dbh, String dbTableName, 
      String displayName, TableType tableType, String id,
      KeyValueStore.Type typeOfStore) {
    // First we will add the entry in TableDefinitions. 
    //  TODO: this should check for duplicate names.
    SQLiteDatabase db = dbh.getWritableDatabase();
    // Now we want to add an entry to the key value store for all of the 
    // keys defined in this class.
    List<OdkTablesKeyValueStoreEntry> values = 
        new ArrayList<OdkTablesKeyValueStoreEntry>();
    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_DISPLAY_NAME, displayName));
    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_COLUMN_ORDER, 
        DEFAULT_KEY_COLUMN_ORDER));
    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_PRIME_COLUMNS, 
        DEFAULT_KEY_PRIME_COLUMNS));
    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_SORT_COLUMN, 
        DEFAULT_KEY_SORT_COLUMN));
    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_INDEX_COLUMN,
        DEFAULT_KEY_INDEX_COLUMN));
    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_CURRENT_VIEW_TYPE,
        DEFAULT_KEY_CURRENT_VIEW_TYPE));
//    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
//        TableProperties.KVS_ASPECT, KEY_CURRENT_OVERVIEW_VIEW_TYPE,
//        DEFAULT_KEY_CURRENT_OVERVIEW_VIEW_TYPE));
//    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
//        TableProperties.KVS_ASPECT, KEY_CURRENT_COLLECTION_VIEW_TYPE,
//        DEFAULT_KEY_CURRENT_COLLECTION_VIEW_TYPE));
//    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
//        TableProperties.KVS_ASPECT, KEY_DETAIL_VIEW_FILE, 
//        DEFAULT_KEY_DETAIL_VIEW_FILE));
    values.add(createStringEntry(id, TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_SUM_DISPLAY_FORMAT, 
        DEFAULT_KEY_SUM_DISPLAY_FORMAT));
    Map<String, String> mapProps = new HashMap<String, String>();
    mapProps.put(KEY_DISPLAY_NAME, displayName);
    mapProps.put(KEY_COLUMN_ORDER, DEFAULT_KEY_COLUMN_ORDER);
    mapProps.put(KEY_PRIME_COLUMNS, DEFAULT_KEY_PRIME_COLUMNS);
    mapProps.put(KEY_SORT_COLUMN, DEFAULT_KEY_SORT_COLUMN);
    mapProps.put(KEY_INDEX_COLUMN, DEFAULT_KEY_INDEX_COLUMN);
    mapProps.put(KEY_CURRENT_VIEW_TYPE, DEFAULT_KEY_CURRENT_VIEW_TYPE);
//    mapProps.put(KEY_CURRENT_OVERVIEW_VIEW_TYPE, 
//        DEFAULT_KEY_CURRENT_OVERVIEW_VIEW_TYPE);
//    mapProps.put(KEY_CURRENT_COLLECTION_VIEW_TYPE,
//        DEFAULT_KEY_CURRENT_COLLECTION_VIEW_TYPE);
//    mapProps.put(KEY_DETAIL_VIEW_FILE, DEFAULT_KEY_DETAIL_VIEW_FILE);
    mapProps.put(KEY_SUM_DISPLAY_FORMAT, DEFAULT_KEY_SUM_DISPLAY_FORMAT);
    
// don't know why we did it this way...instead just call it through the map
// so we don't have as much redundant code.
//    TableProperties tp = new TableProperties(dbh, id, dbTableName, displayName,
//        tableType, new ArrayList<String>(), new ArrayList<String>(), null, null, null, null, null, 
//        null, null, null, null, SyncUtil.State.INSERTING, false, typeOfStore);
//    TableProperties tp = getTablePropertiesForTable(dbh, id, typeOfStore);
//    tp.getColumns(); // ensuring columns are already initialized
    TableProperties tp = null;
    KeyValueStoreManager kvms = KeyValueStoreManager.getKVSManager(dbh);
    try {
	    db.beginTransaction();
	    try {
	      Map<String, String> tableDefProps = 
	          TableDefinitions.addTable(db, id, dbTableName, dbTableName, 
	          tableType);
	      KeyValueStore typedStore = kvms.getStoreForTable(id, 
	          typeOfStore);
	        typedStore.addEntriesToStore(db, values);
	      mapProps.putAll(tableDefProps);
	        tp = constructPropertiesFromMap(dbh, mapProps, typeOfStore);
	        tp.getColumns();
	        KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(KVS_PARTITION);
	        kvsh.setStringEntry(KEY_CURRENT_QUERY, "");
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
      // TODO: fix the when to close problem
//    	db.close();
    }
  }
  
  /*
   * Creates a key value store entry of the type String.
   */
  private static OdkTablesKeyValueStoreEntry createStringEntry(String tableId,
      String partition, String aspect, String key, String value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = aspect;
    entry.value = value;
    entry.type = ColumnType.TEXT.name();
    entry.key = key;
    return entry;
  }
  
  /*
   * Creates a key value store entry of the type int.
   */
  private static OdkTablesKeyValueStoreEntry createIntEntry(String tableId,
      String partition, String aspect, String key, int value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = aspect;
    entry.value = String.valueOf(value);
    entry.type = ColumnType.INTEGER.name();
    entry.key = key;
    return entry;
  }
  
  /*
   * Creates a key value store of the type boolean.
   */
  private static OdkTablesKeyValueStoreEntry createBoolEntry(String tableId,
      String partition, String aspect, String key, boolean value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = aspect;
    entry.value = String.valueOf(value);
    entry.type = ColumnType.BOOLEAN.name();
    entry.key = key;
    return entry;
  }

  public void deleteTable() {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVSM = kvsm.getSyncStoreForTable(tableId);
    boolean isSetToSync = syncKVSM.isSetToSync();
    // hilary's original
    //if (isSynched && (syncState == SyncUtil.State.REST || syncState == SyncUtil.State.UPDATING))
      if (isSetToSync && (syncState == SyncState.rest 
                           || syncState == SyncState.updating))
      setSyncState(SyncState.deleting);
    // hilary's original
    //else if (!isSynched || syncState == SyncUtil.State.INSERTING)
    else if (!isSetToSync || syncState == SyncState.inserting)
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
	          this.backingStore);
	      activeKVS.clearKeyValuePairs(db);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      e.printStackTrace();
	      Log.e(TAG, "error deleting table: " + this.tableId);
	    } finally{
	      db.endTransaction();
	    }
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
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
    setStringProperty(TableProperties.KVS_PARTITION, 
        TableProperties.KVS_ASPECT, KEY_DISPLAY_NAME, displayName);
    this.displayName = displayName;
  }

  /**
   * @return the table's type
   */
  public TableType getTableType() {
    return tableType;
  }

  /**
   * Sets the table's type.
   * 
   * @param tableType
   *          the new table type
   */
  public void setTableType(TableType tableType) {
    setStringProperty(TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, TableDefinitions.DB_TYPE, 
        tableType.name());
    this.tableType = tableType;
  }
  
//  /**
//   * Get the current view type for the overview of this table.
//   * <p>
//   * The overview
//   * view is the one that shows all the rows. This stands in contrast to the
//   * collection view, which shows only a single row of the "prime column",
//   * similar to a conversation in gmail where only the most recent is 
//   * displayed.
//   * @return
//   */
//  public TableViewType getCurrentOverviewViewType() {
//    return this.currentOverviewViewType;
//  }
//  
//  /**
//   * Set the overview view type for the table.
//   * <p>
//   * The overview
//   * view is the one that shows all the rows. This stands in contrast to the
//   * collection view, which shows only a single row of the "prime column",
//   * similar to a conversation in gmail where only the most recent is 
//   * displayed.
//   * @param viewType
//   */
//  public void setCurrentOverviewViewType(TableViewType viewType) {
//    setStringProperty(TableProperties.KVS_PARTITION,
//        TableProperties.KVS_ASPECT, 
//        TableProperties.KEY_CURRENT_OVERVIEW_VIEW_TYPE,
//        viewType.name());
//    this.currentOverviewViewType = viewType;
//  }
//  
//  /**
//   * Get the current view type for the collection view of this table.
//   * <p>
//   * The overview
//   * view is the one that shows all the rows. This stands in contrast to the
//   * collection view, which shows only a single row of the "prime column",
//   * similar to a conversation in gmail where only the most recent is 
//   * displayed.
//   * @return
//   */
//  public TableViewType getCurrentCollectionViewType() {
//    return this.currentCollectionViewType;
//  }
//  
//  /**
//   * Set the current collection view type for this table.
//   * <p>
//   * The overview
//   * view is the one that shows all the rows. This stands in contrast to the
//   * collection view, which shows only a single row of the "prime column",
//   * similar to a conversation in gmail where only the most recent is 
//   * displayed.
//   * @param viewType
//   */
//  public void setCurrentCollectionViewType(TableViewType viewType) {
//    setStringProperty(TableProperties.KVS_PARTITION,
//        TableProperties.KVS_ASPECT, 
//        TableProperties.KEY_CURRENT_COLLECTION_VIEW_TYPE,
//        viewType.name());   
//    this.currentCollectionViewType = viewType;
//  }
  
  /**
   * Get the current view type of the table. 
   * @return
   */
  public TableViewType getCurrentViewType() {
    return this.currentViewType;
  }
  
  /**
   * Set the current view type of the table.
   * @param viewType
   */
  public void setCurrentViewType(TableViewType viewType) {
    setStringProperty(TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT,
        TableProperties.KEY_CURRENT_VIEW_TYPE,
        viewType.name());
    this.currentViewType = viewType;
  }

  /**
   * Return an unordered array of a table's columns. If something has happened
   * to a column that did not go through TableProperties, update row also needs
   * to be called.
   * @return an unordered array of the table's columns
   */
  public ColumnProperties[] getColumns() {
    if (columns == null) {
      columns = ColumnProperties.getColumnPropertiesForTable(dbh, tableId, 
          backingStore);
      orderColumns();
    }
    return columns;
 }
  
  /**
   * Pulls the columns from the database into this TableProperties and orders
   * them according to the column order. This is an optimization and should 
   * only be done if necessary.
   */
  public void refreshColumns() {
    columns = ColumnProperties.getColumnPropertiesForTable(dbh, tableId, 
        backingStore);
    orderColumns();
  }

  private void orderColumns() {
    ColumnProperties[] newColumns = new ColumnProperties[columns.length];
    for (int i = 0; i < columnOrder.size(); i++) {
      for (int j = 0; j < columns.length; j++) {
        if (columns[j].getElementKey().equals(columnOrder.get(i))) {
          newColumns[i] = columns[j];
          break;
        }
      }
    }
    columns = newColumns;
  }

  public ColumnProperties getColumnByElementKey(String colElementKey) {
    int colIndex = getColumnIndex(colElementKey);
    if (colIndex < 0) {
      return null;
    }
    return getColumns()[colIndex];
  }
  
  /**
   * Return a column properties object for the column at the given index.
   * @param index
   * @return
   */
  public ColumnProperties getColumnByIndex(int index) {
    if (index < 0) {
      return null;
    }
    return getColumns()[index];
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

  /**
   * Return the element key of the column with the given display name. This 
   * behavior is undefined if there are two columns with the same name. This
   * means that all the methods in {@link ColumnProperties} for creating a 
   * column must be used for creation and changing of display names to ensure
   * there are no collisions.
   * @param displayName
   * @return
   */
  public String getColumnByDisplayName(String displayName) {
    ColumnProperties[] cps = getColumns();
    for (ColumnProperties cp : cps) {
      String cdn = cp.getDisplayName();
      if ((cdn != null) && (cdn.equalsIgnoreCase(displayName))) {
        return cp.getElementKey();
      }
    }
    return null;
  }

  public String getColumnByAbbreviation(String abbreviation) {
    ColumnProperties[] cps = getColumns();
    for (ColumnProperties cp : cps) {
      String ca = cp.getSmsLabel();
      if ((ca != null) && (ca.equalsIgnoreCase(abbreviation))) {
        return cp.getElementKey();
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
   * Check all the columns to see if "name" conflicts with any existing element
   * keys.
   * @param name
   * @return
   */
  private boolean columnNameConflict(String name) {
    for (ColumnProperties cp : columns) {
      if (cp.getElementKey().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a column to the table. 
   * <p>
   * The column is set to the default visibility. The column is added to the 
   * active column store.
   * <p>
   * The elementKey and elementName must be unique to a given table. If you 
   * are not ensuring this yourself, you should pass in null values and it will
   * generate names based on the displayName via 
   * {@link ColumnProperties.createDbElementKey} and 
   * {@link ColumnProperties.createDbElementName}.
   * 
   * @param displayName
   *          the column's display name
   * @param elementKey 
   *           should either be received from the server or null
   * @param elementName should either be received from the server or null
   * @return ColumnProperties for the new table
   */
  public ColumnProperties addColumn(String displayName, String elementKey,
      String elementName) {
    // ensuring columns is initialized
    getColumns();
    // preparing column order
    ColumnProperties[] newColumns = new ColumnProperties[columns.length + 1];
    ArrayList<String> newColumnOrder = new ArrayList<String>();
    for (int i = 0; i < columns.length; i++) {
      newColumns[i] = columns[i];
      newColumnOrder.add(columnOrder.get(i));
    }
    // adding column
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
      if (elementKey == null) {
        elementKey = 
            ColumnProperties.createDbElementKey(tableId, displayName, db);
      }
      if (elementName == null) {
        elementName = 
            ColumnProperties.createDbElementName(tableId, displayName, db);
      }
	    ColumnProperties cp = null;
	    db.beginTransaction();
	    try {
	      cp = ColumnProperties.addColumn(dbh, db, tableId, 
	          displayName, 
	          elementKey,
	          elementName,
	          ColumnProperties.DEFAULT_KEY_VISIBLE, 
//	          KeyValueStore.Type.COLUMN_ACTIVE);
	          this.getBackingStoreType());
	      db.execSQL("ALTER TABLE \"" + dbTableName + "\" ADD COLUMN \"" 
	          + cp.getElementKey() + "\"");
	      newColumnOrder.add(cp.getElementKey());
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
      // TODO: fix the when to close problem
//    	db.close();
    }
  }

  /**
   * Deletes a column from the table.
   * 
   * @param elementKey
   *          the elementKey of the column to delete
   */
  public void deleteColumn(String elementKey) {
    // ensuring columns is initialized
    getColumns();
    // finding the index of the column in columns
    int colIndex = 0;
    for (ColumnProperties cp : columns) {
      if (cp.getElementKey().equals(elementKey)) {
        break;
      } else {
        colIndex++;
      }
    }
    if (colIndex == columns.length) {
      Log.e(TableProperties.class.getName(), 
          "deleteColumn() did not find the column");
      return;
    }
    // forming a comma-separated list of columns to keep
    String csv = DbTable.DB_CSV_COLUMN_LIST;
    for (int i = 0; i < columns.length; i++) {
      if (i == colIndex) {
        continue;
      }
      csv += ", " + columns[i].getElementKey();
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
      if (col.equals(elementKey)) {
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
	      Log.e(TAG, "error deleting column: " + elementKey);
	    } finally {
	      db.endTransaction();
	    }
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
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
      // TODO: fix the when to close problem
//    	db.close();
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
    setStringProperty(TableProperties.KVS_PARTITION, 
        TableProperties.KVS_ASPECT, KEY_COLUMN_ORDER, colOrderList, db);
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
//    String str = "";
//    for (String cdb : primes) {
//      str += cdb + "/";
//    }
//    if (str.length() > 0) {
//      str = str.substring(0, str.length() - 1);
//    }
    String primesStr;
    try {
      primesStr = mapper.writeValueAsString(primes);
      setStringProperty(TableProperties.KVS_PARTITION, 
          TableProperties.KVS_ASPECT, KEY_PRIME_COLUMNS, primesStr);
      this.primeColumns = primes;
    } catch (JsonGenerationException e) {
      e.printStackTrace();
      Log.e(TAG, "problem mapping prime colums");
    } catch (JsonMappingException e) {
      Log.e(TAG, "problem mapping prime colums");
      e.printStackTrace();
    } catch (IOException e) {
      Log.e(TAG, "problem mapping prime colums");
      e.printStackTrace();
    }

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
    setStringProperty(TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, KEY_SORT_COLUMN, sortColumn);
    this.sortColumn = sortColumn;
  }
  
  /**
   * Return the display name of the index column.
   * TODO: make this work with element key
   * @return
   */
  public String getIndexColumn() {
    return this.indexColumn;
  }
  
  /**
   * Set the index column for the table. This should be set by the display name
   * of the column. A null value will set the index column back to the default
   * value.
   * TODO: make this use the element key
   * @param indexColumnElementKey
   */
  public void setIndexColumn(String indexColumnElementKey) {
    if ((indexColumnElementKey == null)) {
      indexColumnElementKey = DEFAULT_KEY_INDEX_COLUMN;
    }
    setStringProperty(TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT,
        KEY_INDEX_COLUMN, indexColumnElementKey);
    this.indexColumn = indexColumnElementKey;
  }

  /**
   * @return the ID of the read security table, or null if there is none
   */
//  public String getReadSecurityTableId() {
//    return readSecurityTableId;
//  }
  
  public String getAccessControls() {
    return accessControls;
  }
  
  public void setAccessControls(String accessControls) {
    setStringProperty(TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT,
        TableDefinitions.DB_TABLE_ID_ACCESS_CONTROLS, accessControls);
    this.accessControls = accessControls;
  }
  
  // TODO: fix how these security tables are accessed. need to figure this out
  // first.

//  /**
//   * Sets the table's read security table.
//   * 
//   * @param tableId
//   *          the ID of the new read security table (or null to set no read
//   *          security table)
//   */
//  public void setReadSecurityTableId(String tableId) {
//    setStringProperty(DB_READ_SECURITY_TABLE_ID, tableId);
//    this.readSecurityTableId = tableId;
//  }

//  /**
//   * @return the ID of the write security table, or null if there is none
//   */
//  public String getWriteSecurityTableId() {
//    return writeSecurityTableId;
//  }

  /**
   * Sets the table's write security table.
   * 
   * @param tableId
   *          the ID of the new write security table (or null to set no write
   *          security table)
   */
//  public void setWriteSecurityTableId(String tableId) {
//    setStringProperty(DB_WRITE_SECURITY_TABLE_ID, tableId);
//    this.writeSecurityTableId = tableId;
//  }

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
    setStringProperty(TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, TableDefinitions.DB_SYNC_TAG, syncTag);
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
    setStringProperty(TableProperties.KVS_PARTITION,
        TableProperties.KVS_ASPECT, TableDefinitions.DB_LAST_SYNC_TIME, time);
    this.lastSyncTime = time;
  }
  
  public KeyValueStore.Type getBackingStoreType() {
    return this.backingStore;
  }

//  /**
//   * @return the detail view filename
//   */
//  public String getDetailViewFilename() {
//    return detailViewFilename;
//
//  }

  /**
   * Sets the table's detail view filename.
   * 
   * @param filename
   *          the new filename
   */
//  public void setDetailViewFilename(String filename) {
//    setStringProperty(TableProperties.KVS_PARTITION,
//        TableProperties.KVS_ASPECT, KEY_DETAIL_VIEW_FILE, filename);
//    this.detailViewFilename = filename;
//  }

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
    setStringProperty(TableProperties.KVS_PARTITION, 
        TableProperties.KVS_ASPECT, KEY_SUM_DISPLAY_FORMAT, format);
    this.sumDisplayFormat = format;
  }

  /**
   * @return the synchronization state
   */
  public SyncState getSyncState() {
    return syncState;
  }

  /**
   * Sets the table's synchronization state. Can only move to or from the REST
   * state (e.g., no skipping straight from INSERTING to UPDATING).
   * 
   * @param state
   *          the new synchronization state
   */
  public void setSyncState(SyncState state) {
    if (state == SyncState.rest || this.syncState == SyncState.rest) {
      setStringProperty(TableProperties.KVS_PARTITION, 
          TableProperties.KVS_ASPECT, TableDefinitions.DB_SYNC_STATE, 
          state.name());
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
    setIntProperty(TableProperties.KVS_PARTITION, TableProperties.KVS_ASPECT,
        TableDefinitions.DB_TRANSACTIONING, 
        SyncUtil.boolToInt(transactioning));
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
    // I think this removes exceptions from not having getters/setters...
    mapper.setVisibilityChecker(mapper.getVisibilityChecker()
        .withFieldVisibility(Visibility.ANY));
    ArrayList<String> colOrder = new ArrayList<String>();
    ArrayList<Object> cols = new ArrayList<Object>();
    for (ColumnProperties cp : columns) {
      colOrder.add(cp.getElementKey());
      cols.add(cp.toJson());
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
	  jo.put(TableDefinitions.DB_TABLE_ID_ACCESS_CONTROLS, accessControls);
	  jo.put(JSON_KEY_COLUMN_ORDER, colOrder);
	  jo.put(JSON_KEY_COLUMNS, cols);
	  jo.put(JSON_KEY_PRIME_COLUMNS, primes);
	  jo.put(JSON_KEY_SORT_COLUMN, sortColumn);
	  jo.put(JSON_KEY_INDEX_COLUMN, indexColumn);
	  // TODO
	  // TODO
	  jo.put(JSON_KEY_CURRENT_VIEW_TYPE, currentViewType.name());
//	  jo.put(JSON_KEY_CURRENT_OVERVIEW_VIEW_TYPE, 
//	      currentOverviewViewType.name());
//	  jo.put(JSON_KEY_CURRENT_COLLECTION_VIEW_TYPE,
//	      currentCollectionViewType.name());
//	  jo.put(JSON_KEY_DETAIL_VIEW_FILE, detailViewFilename);
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
    
    ArrayList<String> colOrder = 
        (ArrayList<String>) jo.get(JSON_KEY_COLUMN_ORDER);
    ArrayList<String> primes = 
        (ArrayList<String>) jo.get(JSON_KEY_PRIME_COLUMNS);
    
	  setDisplayName((String) jo.get(JSON_KEY_DISPLAY_NAME));
	  setTableType(TableType.valueOf((String)jo.get(JSON_KEY_TABLE_TYPE)));
	  setPrimeColumns(primes);
	  setSortColumn((String) jo.get(JSON_KEY_SORT_COLUMN));
	  setIndexColumn((String)jo.get(JSON_KEY_INDEX_COLUMN));
	  setAccessControls(
	      (String) jo.get(TableDefinitions.DB_TABLE_ID_ACCESS_CONTROLS));
//	  if (jo.containsKey(JSON_KEY_OV_VIEW_SETTINGS)) {
//	    // TODO
//	  }
//	  if (jo.containsKey(JSON_KEY_CO_VIEW_SETTINGS)) {
//	    // TODO
//	  }
	  setCurrentViewType(TableViewType.valueOf(
	      (String)jo.get(JSON_KEY_CURRENT_VIEW_TYPE)));
//	  setCurrentOverviewViewType(TableViewType.valueOf(
//	      (String)jo.get(JSON_KEY_CURRENT_OVERVIEW_VIEW_TYPE)));
//	  setCurrentOverviewViewType(TableViewType.valueOf(
//	      (String)jo.get(JSON_KEY_CURRENT_COLLECTION_VIEW_TYPE)));
//	  setDetailViewFilename((String) jo.get(JSON_KEY_DETAIL_VIEW_FILE));
	  setSummaryDisplayFormat((String) jo.get(JSON_KEY_SUM_DISPLAY_FORMAT));
	  Set<String> columnsToDelete = new HashSet<String>();
	  for (String cdn : columnOrder) {
	    columnsToDelete.add(cdn);
	  }
	  ArrayList<Object> colJArr = (ArrayList<Object>) jo.get(JSON_KEY_COLUMNS);
	  for (int i = 0; i < colOrder.size(); i++) {
	    //Map<String,Object> colJo = (Map<String, Object>) colJArr.get(i);
	    Map<String, Object> colJo = null;
      try {
        colJo = mapper.readValue((String) colJArr.get(i), Map.class);
      } catch (JsonParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (JsonMappingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (colJo == null) {
        throw new IllegalStateException("problem reclaiming column from json" +
        		" in TableProperties.setFromJson");
      }
	    //String colJo = (String) colJArr.get(i);
		  ColumnProperties cp = getColumnByElementKey(colOrder.get(i));
          if (cp == null) {
            // then we need to create the bolumn.
            String coDispName = 
                (String) colJo.get(ColumnProperties.KEY_DISPLAY_NAME);
            String coElKey = 
                (String) colJo.get(ColumnProperties.JSON_KEY_ELEMENT_KEY);
            String coElName = 
                (String) colJo.get(ColumnProperties.JSON_KEY_ELEMENT_NAME);
//            cp = ColumnProperties.constructColumnPropertiesFromJson(dbh, 
//                colJo);
//            cp = addColumn(colOrder.get(i));
            cp = addColumn(coDispName, coElKey, coElName);
//            addColumnFromProperties(cp);
            cp.setFromJson((String) colJArr.get(i));
          }
          //cp.setFromJson(colJo);
          columnsToDelete.remove(colOrder.get(i));
      }
      for (String columnToDelete : columnsToDelete) {
        deleteColumn(columnToDelete);
      }
      setColumnOrder(colOrder);
      orderColumns();
  }
  
  /**
   * Get the possible view types for this table. This is modeled after a method
   * in TableViewSettings. It used to perform checks on column types, but for
   * now it just returns spreadsheet, list, and graph views. It does not return
   * map.
   * @return
   */
  public TableViewType[] getPossibleViewTypes() {
    int numericColCount = 0;
    int locationColCount = 0;
    int dateColCount = 0;
    for (ColumnProperties cp : this.getColumns()) {
      if (cp.getColumnType() == ColumnType.NUMBER || cp.getColumnType() == ColumnType.INTEGER) {
        numericColCount++;
      } else if (cp.getColumnType() == ColumnType.GEOPOINT) {
        locationColCount++;
      } else if (cp.getColumnType() == ColumnType.DATE || cp.getColumnType() == ColumnType.DATETIME
          || cp.getColumnType() == ColumnType.TIME) {
        dateColCount++;
      }
    }
    List<TableViewType> list = new ArrayList<TableViewType>();
    list.add(TableViewType.Spreadsheet);
    list.add(TableViewType.List);
    list.add(TableViewType.Graph);
//    List<Integer> list = new ArrayList<Integer>();
//    list.add(Type.SPREADSHEET);
//    list.add(Type.LIST);
//    if ((numericColCount >= 2) || ((numericColCount >= 1) && (dateColCount >= 1))) {
//      list.add(Type.LINE_GRAPH);
//    }
//    if (numericColCount >= 1) {
//      list.add(Type.BOX_STEM);
//    }
    // Not adding this b/c it's not working atm.
//    list.add(Type.BAR_GRAPH);
//    if (locationColCount >= 1) {
//      list.add(Type.MAP);
//    }
//    int[] arr = new int[list.size()];
//    for (int i = 0; i < list.size(); i++) {
//      arr[i] = list.get(i);
//    }
    TableViewType[] arr = new TableViewType[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i);
    }
    return arr;
  }
  
  /**
   * Retrieve a String from the key value store that backs this TableProperties
   * object. NB: Special care should be taken when using this method, as it is 
   * currently in transition. If there is an accessor defined for the property
   * you are interested in, you should use that method instead.
   * @param partition
   * @param aspect
   * @param key
   * @return the value for the key if the key is found, else null
   * @throws illegal argument exception if the entry in the store is not of
   * type TEXT.
   */
  /*
   * This is a risky method to add at this time. Eventually it should be 
   * called "getStringProperty", and should be retried from a map of key to
   * keyValueStoreEntry objects, or something along those lines. This method
   * would then retrieve it from the map, check that the type specified matches
   * the type of the data (which it will always do for String, but might not 
   * for boolean, for instance), and thow an exception if not.
   * 
   * At the time
   * of this writing, however, most properties are stored as fields within the
   * TableProperties object. This has several downsides. One is notably that
   * for non-required keys, you would have to keep around these fields even 
   * if they weren't in use. Instead, we're going to make a request to the 
   * key value store here.
   * 
   * Note also that in absence of the key in the key value store, null is 
   * returned. This is to try and provide the same functionality as getting
   * a map of keys to values from the key value store, which replaces "" value
   * entries with null. However, exactly what to return where in the event of
   * absence is still a work in progress.
   */
//  public String getStringEntry(String partition, String aspect, String key) {
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore store = kvsm.getStoreForTable(tableId, backingStore);
//    SQLiteDatabase db = dbh.getReadableDatabase();
//    List<String> keyList = new ArrayList<String>();
//    keyList.add(key);
//    List<OdkTablesKeyValueStoreEntry> entries = 
//        store.getEntriesForKeys(db, partition, aspect, keyList);
//    // Do some sanity checking. There should only ever be one entry per key.
//    if (entries.size() > 1) {
//      Log.e(TAG, "request for key: " + key + " in KVS " + backingStore +
//          " for table: " + tableId + " returned " + entries.size() + 
//          "entries. It should return at most 1, as it is a key in a set.");
//    }
//    // Since we know that the value field in the KVS is of type String/TEXT,
//    // there is no worry that it will not exist. We can just go ahead and 
//    // return the value as we find it.
//    if (entries.size() == 0) {
//      return null;
//    } else {
//      if (!entries.get(0).type.equals(
//          KeyValueStoreEntryType.TEXT.getLabel())) {
//        throw new IllegalArgumentException("requested string entry for " +
//          "key: " + key + ", but the corresponding entry in the store was " +
//            "not of type: " + KeyValueStoreEntryType.TEXT.getLabel());
//      }
//      return entries.get(0).value;
//    }
//  }
  
  /**
   * Return a list object from the key value store. The caller will have to 
   * know what the Objects are in the list. Returns null if there is no 
   * corresponding entry in the key value store.
   * @param partition
   * @param aspect
   * @param key
   * @return
   */
//  public ArrayList<Object> getListEntry(String partition, String aspect, 
//      String key) {
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore store = kvsm.getStoreForTable(tableId, backingStore);
//    SQLiteDatabase db = dbh.getReadableDatabase();
//    List<String> keyList = new ArrayList<String>();
//    keyList.add(key);
//    List<OdkTablesKeyValueStoreEntry> entries = 
//        store.getEntriesForKeys(db, partition, aspect, keyList);
//    ArrayList<Object> result = null;
//    // Do some sanity checking. There should only ever be one entry per key.
//    if (entries.size() > 1) {
//      Log.e(TAG, "request for key: " + key + " in KVS " + backingStore +
//          " for table: " + tableId + " returned " + entries.size() + 
//          "entries. It should return at most 1, as it is a key in a set.");
//    }
//    if (entries.size() == 0) {
//      return null;
//    } else {
//      if (!entries.get(0).type.equals(
//          KeyValueStoreEntryType.ARRAYLIST.getLabel())) {
//        throw new IllegalArgumentException("requested list entry for " +
//            "key: " + key + ", but the corresponding entry in the store was " +
//              "not of type: " + KeyValueStoreEntryType.ARRAYLIST.getLabel());      
//      }
//      try {
//        result = mapper.readValue(entries.get(0).value, ArrayList.class);
//        return result;
//      } catch (JsonParseException e) {
//        Log.e(TAG, "problem parsing json list entry from the kvs");
//        e.printStackTrace();
//      } catch (JsonMappingException e) {
//        Log.e(TAG, "problem mapping json list entry from the kvs");
//        e.printStackTrace();
//      } catch (IOException e) {
//        Log.e(TAG, "i/o problem with json for list entry from the kvs");
//        e.printStackTrace();
//      }
//    }
//    return result;   
//  }
  
  /**
   * Get the string representation of an object in the key value store. The
   * caller is expected to have to do the mapping back to an object.
   * @param partition
   * @param aspect
   * @param key
   * @return
   */
//  public String getObjectEntry(String partition, String aspect, String key) {
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore store = kvsm.getStoreForTable(tableId, backingStore);
//    SQLiteDatabase db = dbh.getReadableDatabase();
//    List<String> keyList = new ArrayList<String>();
//    keyList.add(key);
//    List<OdkTablesKeyValueStoreEntry> entries = 
//        store.getEntriesForKeys(db, partition, aspect, keyList);
//    // Do some sanity checking. There should only ever be one entry per key.
//    if (entries.size() > 1) {
//      Log.e(TAG, "request for key: " + key + " in KVS " + backingStore +
//          " for table: " + tableId + " returned " + entries.size() + 
//          "entries. It should return at most 1, as it is a key in a set.");
//    }
//    if (entries.size() == 0) {
//      return null;
//    } else {
//      if (!entries.get(0).type.equals(
//          KeyValueStoreEntryType.OBJECT.getLabel())) {
//        throw new IllegalArgumentException("requested object entry for " +
//            "key: " + key + ", but the corresponding entry in the store was " +
//              "not of type: " + KeyValueStoreEntryType.OBJECT.getLabel());      
//      }
//      return entries.get(0).value;
//    }
//  }
  
  /**
   * Get an int entry from the key value store. If there is an accessor method
   * for the particular key or property you are requesting, you must use that
   * instead. 
   * <p>
   * If the entry does not exist, it returns null cast to type Integer.
   * @param partition
   * @param aspect
   * @param key
   * @return
   * @throws IllegalArgumentException if the key returns an entry that is not
   * of type INTEGER.
   */
//  public Integer getIntegerEntry(String partition, String aspect, String key) {
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore store = kvsm.getStoreForTable(tableId, backingStore);
//    SQLiteDatabase db = dbh.getReadableDatabase();
//    List<String> keyList = new ArrayList<String>();
//    keyList.add(key);
//    List<OdkTablesKeyValueStoreEntry> entries = 
//        store.getEntriesForKeys(db, partition, aspect, keyList);
//    // Do some sanity checking. There should only ever be one entry per key.
//    if (entries.size() > 1) {
//      Log.e(TAG, "request for key: " + key + " in KVS " + backingStore +
//          " for table: " + tableId + " returned " + entries.size() + 
//          "entries. It should return at most 1, as it is a key in a set.");
//    }
//    // Since we know that the value field in the KVS is of type String/TEXT,
//    // there is no worry that it will not exist. We can just go ahead and 
//    // return the value as we find it.
//    if (entries.size() == 0) {
//      return (Integer) null;
//    } else {
//      if (!entries.get(0).type.equals(
//          KeyValueStoreEntryType.INTEGER.getLabel())) {
//        throw new IllegalArgumentException("requested string entry for " +
//          "key: " + key + ", but the corresponding entry in the store was " +
//            "not of type: " + KeyValueStoreEntryType.INTEGER.getLabel());
//      }
//      return Integer.parseInt(entries.get(0).value);
//    }
//  }
  
  /**
   * Get a boolean entry from the key value store. If there is an accessor 
   * method for the particular key or property you are requesting, you must use 
   * that instead. 
   * <p>
   * If the entry does not exist, it returns null cast to type Boolean.
   * @param partition
   * @param aspect
   * @param key
   * @return
   * @throws IllegalArgumentException if the key corresponds to an entry that 
   * is not of type BOOLEAN.
   */
//  public boolean getBooleanEntry(String partition, String aspect, String key) {
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore store = kvsm.getStoreForTable(tableId, backingStore);
//    SQLiteDatabase db = dbh.getReadableDatabase();
//    List<String> keyList = new ArrayList<String>();
//    keyList.add(key);
//    List<OdkTablesKeyValueStoreEntry> entries = 
//        store.getEntriesForKeys(db, partition, aspect, keyList);
//    // Do some sanity checking. There should only ever be one entry per key.
//    if (entries.size() > 1) {
//      Log.e(TAG, "request for key: " + key + " in KVS " + backingStore +
//          " for table: " + tableId + " returned " + entries.size() + 
//          "entries. It should return at most 1, as it is a key in a set.");
//    }
//    // Since we know that the value field in the KVS is of type String/TEXT,
//    // there is no worry that it will not exist. We can just go ahead and 
//    // return the value as we find it.
//    if (entries.size() == 0) {
//      return (Boolean) null;
//    } else {
//      if (!entries.get(0).type.equals(
//          KeyValueStoreEntryType.BOOLEAN.getLabel())) {
//        throw new IllegalArgumentException("requested string entry for " +
//          "key: " + key + ", but the corresponding entry in the store was " +
//            "not of type: " + KeyValueStoreEntryType.BOOLEAN.getLabel());
//      }
//      return SyncUtil.intToBool(Integer.parseInt(entries.get(0).value));
//    }
//  }
  
  /**
   * Get an int entry from the key value store. If there is an accessor method
   * for the particular key or property you are requesting, you must use that
   * instead. 
   * <p>
   * If the entry does not exist, it returns null cast to type Double.
   * @param partition
   * @param aspect
   * @param key
   * @return
   * @throws IllegalArgumentException if the type of the entry in the store
   * is not of type NUMBER.
   */
//  public double getNumericEntry(String partition, String aspect, String key) {
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore store = kvsm.getStoreForTable(tableId, backingStore);
//    SQLiteDatabase db = dbh.getReadableDatabase();
//    List<String> keyList = new ArrayList<String>();
//    keyList.add(key);
//    List<OdkTablesKeyValueStoreEntry> entries = 
//        store.getEntriesForKeys(db, partition, aspect, keyList);
//    // Do some sanity checking. There should only ever be one entry per key.
//    if (entries.size() > 1) {
//      Log.e(TAG, "request for key: " + key + " in KVS " + backingStore +
//          " for table: " + tableId + " returned " + entries.size() + 
//          "entries. It should return at most 1, as it is a key in a set.");
//    }
//    // Since we know that the value field in the KVS is of type String/TEXT,
//    // there is no worry that it will not exist. We can just go ahead and 
//    // return the value as we find it.
//    if (entries.size() == 0) {
//      return (Double) null;
//    } else {
//      if (!entries.get(0).type.equals(
//          KeyValueStoreEntryType.NUMBER.getLabel())) {
//        throw new IllegalArgumentException("requested string entry for " +
//          "key: " + key + ", but the corresponding entry in the store was " +
//            "not of type: " + KeyValueStoreEntryType.NUMBER.getLabel());
//      }
//      return Double.parseDouble(entries.get(0).value);
//    }
//  }

  private void setIntProperty(String partition, String aspect, String property,
      int value) {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVS = kvsm.getSyncStoreForTable(tableId);
    boolean isSetToSync = syncKVS.isSetToSync();
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      setIntProperty(partition, aspect, property, value, db);
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
	    if (isSetToSync && syncState == SyncState.rest 
	        && JSON_COLUMNS.contains(property)) {
	      setSyncState(SyncState.updating);
	    }
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
    }
  }
  
//  /**
//   * Set an integer entry in the key value store. If an entry already exists
//   * it will be overwritten.
//   * @param partition
//   * @param aspect
//   * @param key
//   * @param value
//   */
//  public void setIntegerEntry(String partition, String aspect, 
//      String key, int value) {
//    SQLiteDatabase db = dbh.getWritableDatabase();
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore backingKVS = kvsm.getStoreForTable(tableId, backingStore);
//    backingKVS.insertOrUpdateKey(db, partition, aspect, key, 
//        KeyValueStoreEntryType.INTEGER.getLabel(), Integer.toString(value));
//    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
//        aspect + ", key: " + key + " to " + value);
//  }
//  
//  /**
//   * Set a numeric entry in the key value store. If an entry already exists
//   * it will be overwritten.
//   * @param partition
//   * @param aspect
//   * @param key
//   * @param value
//   */
//  public void setNumericEntry(String partition, String aspect, 
//      String key, double value) {
//    SQLiteDatabase db = dbh.getWritableDatabase();
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore backingKVS = kvsm.getStoreForTable(tableId, backingStore);
//    backingKVS.insertOrUpdateKey(db, partition, aspect, key, 
//        KeyValueStoreEntryType.NUMBER.getLabel(), Double.toString(value));
//    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
//        aspect + ", key: " + key + " to " + value);
//  }
//  
//  /**
//   * Set an object entry in the key value store. The caller is expected to 
//   * first map the object to JSON.
//   * @param partition
//   * @param aspect
//   * @param key
//   * @param jsonOfObject
//   */
//  public void setObjectEntry(String partition, String aspect, String key,
//      String jsonOfObject) {
//    SQLiteDatabase db = dbh.getWritableDatabase();
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore backingKVS = kvsm.getStoreForTable(tableId, backingStore);
//    backingKVS.insertOrUpdateKey(db, partition, aspect, key, 
//        KeyValueStoreEntryType.OBJECT.getLabel(), jsonOfObject);
//    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
//        aspect + ", key: " + key + " to " + jsonOfObject);    
//  }
//  
//  /**
//   * Set a boolean entry in the key value store. If an entry already exists
//   * it will be overwritten.
//   * @param partition
//   * @param aspect
//   * @param key
//   * @param value
//   */
//  public void setBooleanEntry(String partition, String aspect, 
//      String key, boolean value) {
//    SQLiteDatabase db = dbh.getWritableDatabase();
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore backingKVS = kvsm.getStoreForTable(tableId, backingStore);
//    backingKVS.insertOrUpdateKey(db, partition, aspect, key, 
//        KeyValueStoreEntryType.BOOLEAN.getLabel(), 
//        Integer.toString(SyncUtil.boolToInt(value)));
//    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
//        aspect + ", key: " + key + " to " + value);
//  }
//  
//  /**
//   * Set a String entry in the key value store. If an entry already exists
//   * it will be overwritten.
//   * @param partition
//   * @param aspect
//   * @param key
//   * @param value
//   */
//  public void setStringEntry(String partition, String aspect, 
//      String key, String value) {
//    SQLiteDatabase db = dbh.getWritableDatabase();
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore backingKVS = kvsm.getStoreForTable(tableId, backingStore);
//    backingKVS.insertOrUpdateKey(db, partition, aspect, key, 
//        KeyValueStoreEntryType.TEXT.getLabel(), value);
//    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
//        aspect + ", key: " + key + " to " + value);
//  }
//  
//  /**
//   * Set an ArrayList entry in the key value store.
//   * @param partition
//   * @param aspect
//   * @param key
//   * @param value
//   */
//  public void setListEntry(String partition, String aspect, String key, 
//      ArrayList<Object> value) {
//    SQLiteDatabase db = dbh.getWritableDatabase();
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    KeyValueStore backingKVS = kvsm.getStoreForTable(tableId, backingStore);
//    String entryValue = null;
//    try {
//      entryValue = mapper.writeValueAsString(value);
//    } catch (JsonGenerationException e) {
//      Log.e(TAG, "problem parsing json list entry while writing to the kvs");
//      e.printStackTrace();
//    } catch (JsonMappingException e) {
//      Log.e(TAG, "problem mapping json list entry while writing to the kvs");
//      e.printStackTrace();
//    } catch (IOException e) {
//      Log.e(TAG, "i/o exception with json list entry while writing to the" +
//      		" kvs");
//      e.printStackTrace();
//    }
//    backingKVS.insertOrUpdateKey(db, partition, aspect, key, 
//        KeyValueStoreEntryType.ARRAYLIST.getLabel(), entryValue);
//  }
  
  /**
   * Remove the given key from the KVS backing this TableProperties. 
   * @param partition
   * @param aspect
   * @param key
   * @return
   */
  public int removeEntry(String partition, String aspect, String key) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore backingKVS = kvsm.getStoreForTable(tableId, backingStore);
    return backingKVS.deleteKey(db, partition, aspect, key);
  }
  
  
  /**
   * Actually handle the updating of the property. Checks whether this property
   * resides in TableDefinitions or the KVS, and sets to the correct place. If
   * the property resides in TableDefinitions, the partition and aspect are 
   * unused and may be null.
   * @param property
   * @param value
   * @param db
   */
  private void setIntProperty(String partition, String aspect,
      String property, int value, SQLiteDatabase db) {
    // First check if the property is in TableDefinitions.
    if (TableDefinitions.columnNames.contains(property)) {
      // We need to do it through TableDefinitions.
      TableDefinitions.setValue(tableId, property, value, db);
    } else {
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStore backingKVS = kvsm.getStoreForTable(this.tableId,
          this.backingStore);
      backingKVS.insertOrUpdateKey(db, partition,
          aspect, property, KeyValueStoreEntryType.INTEGER.getLabel(),
          Integer.toString(value));
    }
    Log.d(TAG, "updated int " + property + " to " + value + "for " + 
      this.tableId);
  }

  private void setStringProperty(String partition, String aspect, 
      String property, String value) {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVS = kvsm.getSyncStoreForTable(tableId);
    boolean isSetToSync = syncKVS.isSetToSync();
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      setStringProperty(partition, aspect, property, value, db);
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      Log.e(TAG, "error setting string property " + property + " to " 
	          + value + " for " + this.tableId);
	    } finally {
	      db.endTransaction();
	    }
	    // hilary's original
	    //if (isSynched && syncState == SyncUtil.State.REST && JSON_COLUMNS.contains(property))
	    if (isSetToSync && syncState == SyncState.rest 
	        && JSON_COLUMNS.contains(property)) {
	      setSyncState(SyncState.updating);
	    }
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
    }
  }

  //TODO this should maybe be only transactionally?
  /**
   * Actually handle the updating of the property. Checks whether this property
   * resides in TableDefinitions of the KVS, and sets to the correct place.
   * @param property
   * @param value
   * @param db
   */
  private void setStringProperty(String partition, String aspect, 
      String property, String value, SQLiteDatabase db) {
    if (TableDefinitions.columnNames.contains(property)) {
      // We need to do it through TableDefinitions.
      TableDefinitions.setValue(tableId, property, value, db);
    } else {
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStore intendedKVS = kvsm.getStoreForTable(this.tableId,
          this.backingStore);
      intendedKVS.insertOrUpdateKey(db, partition,
          aspect, property, KeyValueStoreEntryType.TEXT.getLabel(), value);
    }
    Log.d(TAG, "updated string " + property + " to " + value + " for " 
      + this.tableId);
  }
  
  /**
   * Get the accessor object for persisted values in the key value store.
   * @param partition
   * @return
   */
  public KeyValueStoreHelper getKeyValueStoreHelper(String partition) {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore backingStore = kvsm.getStoreForTable(this.tableId, 
        this.backingStore);
    return new KeyValueStoreHelper(backingStore, partition);
  }
  
  /**
   * Returns an array of the initialized properties. These are the keys that
   * exist in the key value store for any table.
   * @return
   */
  public static String[] getInitKeys() {
    return INIT_KEYS;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TableProperties)) {
      return false;
    }
    TableProperties other = (TableProperties) obj;
    return tableId.equals(other.tableId);
  }

  // TODO: this is a crap hash function given all the information that this
  // object contains. It should really be updated.
  @Override
  public int hashCode() {
    return tableId.hashCode();
  }

  @Override
  public String toString() {
    return displayName;
  }
  
}
