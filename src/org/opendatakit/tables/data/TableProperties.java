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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.opendatakit.common.android.provider.TableDefinitionsColumns;
import org.opendatakit.tables.exceptions.TableAlreadyExistsException;
import org.opendatakit.tables.sync.SyncUtil;
import org.opendatakit.tables.utils.ColorRuleUtil;
import org.opendatakit.tables.utils.SecurityUtil;
import org.opendatakit.tables.utils.ShortcutUtil;

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

  private static final ObjectMapper mapper;
  private static final String t = "TableProperties";

  public static final String TAG = "TableProperties";

  /***********************************
   *  The partition and aspect of table properties in the key value store.
   ***********************************/
  public static final String KVS_PARTITION = "Table";

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

  static {
    mapper = new ObjectMapper();
    mapper.setVisibilityChecker(mapper.getVisibilityChecker()
        .withFieldVisibility(Visibility.ANY));
  }

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
  //private ColumnProperties[] columns;
  /**
   * Maps the elementKey of a column to its ColumnProperties object.
   */
  private Map<String, ColumnProperties> mElementKeyToColumnProperties;
  private boolean staleColumnsInOrder = true;
  private List<ColumnProperties> columnsInOrder = new ArrayList<ColumnProperties>();

  private List<String> columnOrder;
  private List<String> primeColumns;
  private String sortColumn;
  private String indexColumn;
//  private String readSecurityTableId;
//  private String writeSecurityTableId;
  private TableViewType currentViewType;
//  private TableViewType currentOverviewViewType;
//  private TableViewType currentCollectionViewType;
//  private String detailViewFilename;
  private String sumDisplayFormat;
  private KeyValueStoreHelper tableKVSH;

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
    whereArgs = new String[] { tableId };
    this.tableId = tableId;
    this.tableKey = tableKey;
    this.dbTableName = dbTableName;
    this.displayName = displayName;
    this.tableType = tableType;
    this.accessControls = accessControls;
//    columns = null;
    this.mElementKeyToColumnProperties = null;
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
    this.tableKVSH =
        this.getKeyValueStoreHelper(TableProperties.KVS_PARTITION);
	refreshColumns();
    if ( columnOrder.size() == 0 ) {

        for ( ColumnProperties cp : mElementKeyToColumnProperties.values() ) {
          columnOrder.add(cp.getElementKey());
        }
        Collections.sort(columnOrder, new Comparator<String>(){

          @Override
          public int compare(String lhs, String rhs) {
            return lhs.compareTo(rhs);
          }});
    }
    this.columnOrder = columnOrder;
    this.staleColumnsInOrder = true;
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
    Map<String, String> mapProps = getMapOfPropertiesForTable(dbh, tableId,
        typeOfStore);
    TableProperties tp = constructPropertiesFromMap(dbh, mapProps, typeOfStore);
    if ( typeOfStore == KeyValueStore.Type.ACTIVE ) {
      activeTableIdMap.put(tp.getTableId(), tp);
    }
    return tp;
  }

  private static Map<String,TableProperties> activeTableIdMap = new HashMap<String, TableProperties>();
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
       // table definitions wins -- apply it 2nd
       mapProps.putAll(kvsMap);
       mapProps.putAll(tableDefinitionsMap);
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
	    if ( typeOfStore == KeyValueStore.Type.ACTIVE ) {
	      if ( allIds.size() != activeTableIdMap.size() ) {
    	      activeTableIdMap.clear();
    	      TableProperties[] tps = constructPropertiesFromIds(allIds, dbh, db, kvsm, typeOfStore);
    	      for ( TableProperties tp : tps ) {
    	        activeTableIdMap.put(tp.getTableId(), tp);
    	      }
	      }
	      return activeTableIdMap.values().toArray(new TableProperties[activeTableIdMap.size()]);
	    }
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
	    return constructPropertiesFromIds(synchedIds, dbh, db, kvsm,
	        typeOfStore);
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
	    return constructPropertiesFromIds(securityIds, dbh, db, kvsm,
	        typeOfStore);
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
	    return constructPropertiesFromIds(shortcutIds, dbh, db, kvsm,
	        typeOfStore);
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
    String tableTypeStr = props.get(TableDefinitionsColumns.TYPE);
    TableType tableType = TableType.valueOf(tableTypeStr);
    String syncStateStr = props.get(TableDefinitionsColumns.SYNC_STATE);
    SyncState syncState = SyncState.valueOf(syncStateStr);
    String transactioningStr =
        props.get(TableDefinitionsColumns.TRANSACTIONING);
    int transactioningInt = Integer.parseInt(transactioningStr);
    boolean transactioning = SyncUtil.intToBool(transactioningInt);
    String columnOrderValue = props.get(KEY_COLUMN_ORDER);
    String currentViewTypeStr = props.get(KEY_CURRENT_VIEW_TYPE);
    TableViewType currentViewType;
    if ( currentViewTypeStr == null ) {
      currentViewType = TableViewType.Spreadsheet;
      props.put(KEY_CURRENT_VIEW_TYPE, TableViewType.Spreadsheet.name());
    } else {
      try {
        currentViewType = TableViewType.valueOf(currentViewTypeStr);
      } catch (Exception e) {
        currentViewType = TableViewType.Spreadsheet;
        props.put(KEY_CURRENT_VIEW_TYPE, TableViewType.Spreadsheet.name());
      }
    }
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
        props.get(TableDefinitionsColumns.TABLE_ID),
        props.get(TableDefinitionsColumns.TABLE_KEY),
        props.get(TableDefinitionsColumns.DB_TABLE_NAME),
        props.get(KEY_DISPLAY_NAME),
        tableType,
        props.get(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS),
        columnOrder,
        primeList,
        props.get(KEY_SORT_COLUMN),
        props.get(KEY_INDEX_COLUMN),
        props.get(TableDefinitionsColumns.SYNC_TAG),
        props.get(TableDefinitionsColumns.LAST_SYNC_TIME),
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
  public static TableProperties addTable(DbHelper dbh, String tableKey,
      String dbTableName, String displayName, TableType tableType,
      KeyValueStore.Type intendedStore) {
    String id = UUID.randomUUID().toString();
    TableProperties tp = addTable(dbh, tableKey, dbTableName, displayName,
        tableType, id, intendedStore);
    SQLiteDatabase db = dbh.getWritableDatabase();
    if (tableType == TableType.shortcut) {
      tp.addColumn(ShortcutUtil.LABEL_COLUMN_NAME,
          tp.createDbElementKey(ShortcutUtil.LABEL_COLUMN_NAME),
          tp.createDbElementName(ShortcutUtil.LABEL_COLUMN_NAME));
      tp.addColumn(ShortcutUtil.INPUT_COLUMN_NAME,
		  tp.createDbElementKey(ShortcutUtil.INPUT_COLUMN_NAME),
		  tp.createDbElementName(ShortcutUtil.INPUT_COLUMN_NAME));
      tp.addColumn(ShortcutUtil.OUTPUT_COLUMN_NAME,
		  tp.createDbElementKey(ShortcutUtil.OUTPUT_COLUMN_NAME),
		  tp.createDbElementName(ShortcutUtil.OUTPUT_COLUMN_NAME));
//      tp.addColumn("label", ShortcutUtil.LABEL_COLUMN_NAME);
//      tp.addColumn("input", ShortcutUtil.INPUT_COLUMN_NAME);
//      tp.addColumn("output", ShortcutUtil.OUTPUT_COLUMN_NAME);
    } else if (tableType == TableType.security) {
      tp.addColumn(SecurityUtil.USER_COLUMN_NAME,
		  tp.createDbElementKey(SecurityUtil.USER_COLUMN_NAME),
		  tp.createDbElementName(SecurityUtil.USER_COLUMN_NAME));
      tp.addColumn(SecurityUtil.PHONENUM_COLUMN_NAME,
		  tp.createDbElementKey(SecurityUtil.PHONENUM_COLUMN_NAME),
		  tp.createDbElementName(SecurityUtil.PHONENUM_COLUMN_NAME));
      tp.addColumn(SecurityUtil.PASSWORD_COLUMN_NAME,
		  tp.createDbElementKey(SecurityUtil.PASSWORD_COLUMN_NAME),
		  tp.createDbElementName(SecurityUtil.PASSWORD_COLUMN_NAME));
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
    Log.e(TAG, "shouldn't be adding table from json!");
    TableProperties tp = addTable(dbh, dbTableName, dbTableName, displayName,
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
  public static TableProperties addTable(DbHelper dbh, String tableKey,
      String dbTableName, String displayName, TableType tableType, String id,
      KeyValueStore.Type typeOfStore) {
    // First we will add the entry in TableDefinitions.
    //  TODO: this should check for duplicate names.
    SQLiteDatabase db = dbh.getWritableDatabase();
    Map<String, String> mapProps = new HashMap<String, String>();
    mapProps.put(KEY_DISPLAY_NAME, displayName);
    mapProps.put(KEY_COLUMN_ORDER, DEFAULT_KEY_COLUMN_ORDER);
    mapProps.put(KEY_PRIME_COLUMNS, DEFAULT_KEY_PRIME_COLUMNS);
    mapProps.put(KEY_SORT_COLUMN, DEFAULT_KEY_SORT_COLUMN);
    mapProps.put(KEY_INDEX_COLUMN, DEFAULT_KEY_INDEX_COLUMN);
    mapProps.put(KEY_CURRENT_VIEW_TYPE, DEFAULT_KEY_CURRENT_VIEW_TYPE);
    mapProps.put(KEY_SUM_DISPLAY_FORMAT, DEFAULT_KEY_SUM_DISPLAY_FORMAT);
    TableProperties tp = null;
    KeyValueStoreManager kvms = KeyValueStoreManager.getKVSManager(dbh);
    try {
	    db.beginTransaction();
	    try {
	      Map<String, String> tableDefProps =
	          TableDefinitions.addTable(db, id, tableKey, dbTableName,
	              tableType);
	      mapProps.putAll(tableDefProps);
	        tp = constructPropertiesFromMap(dbh, mapProps, typeOfStore);
	        tp.refreshColumns();
	        KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(KVS_PARTITION);
	        kvsh.setString(KEY_DISPLAY_NAME, displayName);
	        kvsh.setString(KEY_COLUMN_ORDER, DEFAULT_KEY_COLUMN_ORDER);
	        kvsh.setString(KEY_PRIME_COLUMNS, DEFAULT_KEY_PRIME_COLUMNS);
	        kvsh.setString(KEY_SORT_COLUMN, DEFAULT_KEY_SORT_COLUMN);
	        kvsh.setString(KEY_INDEX_COLUMN, DEFAULT_KEY_INDEX_COLUMN);
	        kvsh.setString(KEY_CURRENT_VIEW_TYPE,
	            DEFAULT_KEY_CURRENT_VIEW_TYPE);
	        kvsh.setString(KEY_SUM_DISPLAY_FORMAT,
	            DEFAULT_KEY_SUM_DISPLAY_FORMAT);
	        kvsh.setString(KEY_CURRENT_QUERY, "");
	      Log.d(TAG, "adding table: " + dbTableName);
         DbTable.createDbTable(db, tp);
         // And now set the default color rules.
         ColorRuleGroup ruleGroup =
             ColorRuleGroup.getStatusColumnRuleGroup(tp);
         ruleGroup.replaceColorRuleList(
             ColorRuleUtil.getDefaultSyncStateColorRules());
         ruleGroup.saveRuleList();
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

  public void deleteTable() {
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVSM = kvsm.getSyncStoreForTable(tableId);
    boolean isSetToSync = syncKVSM.isSetToSync();
    // hilary's original
    //if (isSynched && (syncState == SyncUtil.State.REST
    //    || syncState == SyncUtil.State.UPDATING))
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
    Map<String, ColumnProperties> columns = getColumns();
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      db.execSQL("DROP TABLE " + dbTableName);
	      for (ColumnProperties cp : columns.values()) {
	        cp.deleteColumn(db);
	      }
	      TableDefinitions.deleteTableFromTableDefinitions(tableId, db);
	      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
	      kvsm.getStoreForTable(tableId, KeyValueStore.Type.ACTIVE)
	        .clearKeyValuePairs(db);
         kvsm.getStoreForTable(tableId, KeyValueStore.Type.DEFAULT)
           .clearKeyValuePairs(db);
         kvsm.getStoreForTable(tableId, KeyValueStore.Type.SERVER)
           .clearKeyValuePairs(db);
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
    tableKVSH.setString(KEY_DISPLAY_NAME, displayName);
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
    SQLiteDatabase db = dbh.getWritableDatabase();
    TableDefinitions.setValue(tableId, TableDefinitionsColumns.TYPE,
        tableType.name(), db);
    this.tableType = tableType;
    // TODO: handle closing of the database
  }

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
    tableKVSH.setString(TableProperties.KEY_CURRENT_VIEW_TYPE,
        viewType.name());
    this.currentViewType = viewType;
  }

  /**
   * Return a map of elementKey to columns as represented by their
   * {@link ColumnProperties}. If something has happened
   * to a column that did not go through TableProperties, update row also needs
   * to be called.
   * <p>
   * If used repeatedly, this value should be cached by the caller.
   * @return a map of the table's columns as represented by their
   * {@link ColumnProperties}.
   */
  public Map<String, ColumnProperties> getColumns() {
    if (mElementKeyToColumnProperties == null) {
      refreshColumns();
    }
    Map<String, ColumnProperties> defensiveCopy =
        new HashMap<String, ColumnProperties>();
    defensiveCopy.putAll(this.mElementKeyToColumnProperties);
    return defensiveCopy;
 }

  /**
   * Pulls the columns from the database into this TableProperties. Also
   * updates the maps of display name and sms label.
   */
  public void refreshColumns() {
    this.mElementKeyToColumnProperties =
        ColumnProperties.getColumnPropertiesForTable(dbh, tableId,
            backingStore);
  }

  /**
   * Return the index of the elementKey in the columnOrder, or -1 if it is not
   * present.
   * @param elementKey
   * @return
   */
  public int getColumnIndex(String elementKey) {
    return columnOrder.indexOf(elementKey);
  }

  public int getNumberOfDisplayColumns() {
	  return columnOrder.size();
  }

  public ColumnProperties getColumnByIndex(int idx) {
	return getColumnsInOrder().get(idx);
  }

  public ColumnProperties getColumnByElementKey(String elementKey) {
    if (this.mElementKeyToColumnProperties == null) {
      refreshColumns();
    }
    return mElementKeyToColumnProperties.get(elementKey);
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
  public ColumnProperties getColumnByDisplayName(String displayName) {
    if (this.mElementKeyToColumnProperties == null) {
      refreshColumns();
    }
    for ( ColumnProperties cp : this.mElementKeyToColumnProperties.values() ) {
 	  if ( cp.getDisplayName().equals(displayName) ) {
      	return cp;
      }
    }
    return null;
  }

  /**
   * Return the element key based upon the abbreviation/sms label.
   * <p>
   * NB: This is currently not fully conceptualized, and should be used with
   * caution.
   * @param abbreviation
   * @return
   */
  public ColumnProperties getColumnByAbbreviation(String abbreviation) {
    if (this.mElementKeyToColumnProperties == null) {
      refreshColumns();
    }
    for ( ColumnProperties cp : this.mElementKeyToColumnProperties.values() ) {
    	if ( cp.getSmsLabel().equals(abbreviation) ) {
    		return cp;
    	}
    }
    return null;
  }

  public ColumnProperties getColumnByElementName(String elementName) {
    if (this.mElementKeyToColumnProperties == null) {
      refreshColumns();
    }
    for ( ColumnProperties cp : this.mElementKeyToColumnProperties.values() ) {
    	if ( cp.getElementName().equals(elementName) ) {
    		return cp;
    	}
    }
    return null;
  }

  /**
   * Get the {@link ColumnProperties} for the column as specified by either
   * the case-insensitive display name or the case insensitive sms label.
   * @param name
   * @return
   */
  /*
   * Allowing the weird ignorecase thing because this is used by query, which
   * at the moment is all legacy code.
   */
  public ColumnProperties getColumnByUserLabel(String name) {
    Collection<ColumnProperties> cps = getColumns().values();
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
   * Create an element key based on the proposedKey parameter. The first
   * attempt will be the proposedKey prepended with an underscore and with
   * non-word characters (as defined by java's "\\W") replaced by an
   * underscore.
   * If that elementKey is already used for this table, an integer suffix,
   * beginning with 1, is tried to be added to key until a conflict no longer
   * exists.
   * @param tableId
   * @param proposedKey
   * @param db
   * @return
   */
  private String createDbElementKey(String proposedKey) {
    String baseName = "_" + proposedKey.replaceAll("\\W", "_");
    if (!keyConflict(baseName)) {
      return baseName;
    }
    // otherwise we need to create a non-conflicting key.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!keyConflict(nextName)) {
        return nextName;
      }
      suffix++;
    }
  }

  private boolean keyConflict(String elementKey) {
	return (this.getColumnByElementKey(elementKey) != null);
  }

  /**
   * Create an element name based on the proposedName parameter. The first
   * attempt will be the proposedName prepended with an underscore with
   * whitespace (as defined by java's "\\W") replaced by an underscore.
   * If that elementName is already used for this table, an integer suffix,
   * beginning with 1, is tried to be added to key until a conflict no longer
   * exists.
   * @param tableId
   * @param proposedName
   * @param db
   * @return
   */
  private String createDbElementName(String proposedName) {
    String baseName = "_" + proposedName.replaceAll("\\W", "_");
    if (!nameConflict(baseName)) {
      return baseName;
    }
    // otherwise we need to create a non-conflicting key.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!nameConflict(nextName)) {
        return nextName;
      }
      suffix++;
    }
  }

  private boolean nameConflict(String elementName) {
	return (this.getColumnByElementName(elementName) != null);
  }

  /**
   * Take the proposed display name and return a display name that has no
   * conflicts with other display names in the table. If there is a conflict,
   * integers are appended to the proposed name until there are no conflicts.
   * @param proposedDisplayName
   * @return
   */
  public String createDisplayName(String proposedDisplayName) {
    if (getColumnByDisplayName(proposedDisplayName) == null) {
      return proposedDisplayName;
    }
    // otherwise we need to create a non-conflicting name.
    int suffix = 1;
    while (true) {
      String nextName = proposedDisplayName + suffix;
      if (getColumnByDisplayName(nextName) == null) {
        return nextName;
      }
      suffix++;
    }
  }

  /**
   * Adds a column to the table.
   * <p>
   * The column is set to the default visibility. The column is added to the
   * backing store.
   * <p>
   * The elementKey and elementName must be unique to a given table. If you
   * are not ensuring this yourself, you should pass in null values and it will
   * generate names based on the displayName via
   * {@link ColumnProperties#createDbElementKey} and
   * {@link ColumnProperties#createDbElementName}.
   */
  public ColumnProperties addColumn(String displayName, String elementKey,
      String elementName) {
    return addColumn(displayName,elementKey, elementName,
        ColumnDefinitions.DEFAULT_DB_ELEMENT_TYPE,
        // TODO: this should be a default list of its own
        ColumnDefinitions.DEFAULT_LIST_CHILD_ELEMENT_KEYS,
        ColumnDefinitions.DEFAULT_DB_IS_PERSISTED,
        ColumnDefinitions.DEFAULT_DB_JOINS);
  }

  /**
   * Adds a column to the table.
   * <p>
   * The column is set to the default visibility. The column is added to the
   * backing store.
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
      String elementName, ColumnType columnType, String listChildElementKeys,
      boolean isPersisted, String joins) {
	// ensuring columns is initialized
	// refreshColumns();
    // adding column
	boolean failure = false;
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
      if (elementKey == null) {
        elementKey = createDbElementKey(displayName);
      }
      if (elementName == null) {
        elementName =createDbElementName(displayName);
      }
	    ColumnProperties cp = null;
	    db.beginTransaction();
	    try {
	      cp = ColumnProperties.addColumn(dbh, db, tableId,
	          displayName, elementKey, elementName, columnType,
	          listChildElementKeys, isPersisted, joins,
	          ColumnProperties.DEFAULT_KEY_VISIBLE,
	          this.getBackingStoreType());
	      db.execSQL("ALTER TABLE \"" + dbTableName + "\" ADD COLUMN \""
	          + cp.getElementKey() + "\"");
	      List<String> newColumnOrder = columnOrder;
	      newColumnOrder.add(cp.getElementKey());
	      setColumnOrder(db, newColumnOrder);
	      mElementKeyToColumnProperties.put(cp.getElementKey(), cp);
	      Log.d("TP", "here we are");
	      db.setTransactionSuccessful();
	    } catch (Exception e) {
	      failure = true;
	      e.printStackTrace();
	      Log.e(TAG, "error adding column: " + displayName);
	    } finally {
	      db.endTransaction();
	    }
//	    newColumns[columns.length] = cp;
//	    columns = newColumns;
	    // returning new ColumnProperties
	    return cp;
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
	    // update this object.
	    if ( failure ) {
	    	refreshColumns();
	    }
    }
  }

  /**
   * Deletes a column from the table.
   *
   * @param elementKey
   *          the elementKey of the column to delete
   */
  public void deleteColumn(String elementKey) {
    // forming a comma-separated list of columns to keep
    ColumnProperties colToDelete = this.getColumnByElementKey(elementKey);
    if (colToDelete == null) {
      Log.e(TAG, "could not find column to delete with element key: "
          + elementKey);
      return;
    }

    // Update the column order.
    List<String> newColumnOrder = this.getColumnOrder();
    newColumnOrder.remove(elementKey);
    // deleting the column
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
	    db.beginTransaction();
	    try {
	      colToDelete.deleteColumn(db);
	      this.setColumnOrder(db, newColumnOrder);
	      reformTable(db);
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
  public void reformTable(SQLiteDatabase db) {
    StringBuilder csvBuilder = new StringBuilder(DbTable.DB_CSV_COLUMN_LIST);
    List<String> columnOrder2 = getColumnOrder();
    for (String col : columnOrder2) {
      csvBuilder.append(", " + col);
    }
    String csv = csvBuilder.toString();
    db.execSQL("CREATE TEMPORARY TABLE backup_(" + csv + ")");
    db.execSQL("INSERT INTO backup_(" + csv + ") SELECT " + csv + " FROM " + dbTableName);
    db.execSQL("DROP TABLE " + dbTableName);
    DbTable.createDbTable(db, this);
    db.execSQL("INSERT INTO " + dbTableName + "(" + csv + ") SELECT " + csv +
        " FROM backup_");
    db.execSQL("DROP TABLE backup_");
  }

  /**
   * The column order is specified by an ordered list of element keys.
   * @return a copy of the columnOrder. Since it is a copy, should cache when
   * possible.
   */
  public List<String> getColumnOrder() {
    List<String> defensiveCopy = new ArrayList<String>();
    defensiveCopy.addAll(columnOrder);
    return defensiveCopy;
  }

  /**
   * Returns an unmodifiable list of the ColumnProperties in columnOrder.
   * @return
   */
  public List<ColumnProperties> getColumnsInOrder() {
	  if ( staleColumnsInOrder ) {
		  ArrayList<ColumnProperties> cio = new ArrayList<ColumnProperties>();
		  for ( String elementKey : columnOrder ) {
			  cio.add( getColumnByElementKey(elementKey) );
		  }
		  columnsInOrder = Collections.unmodifiableList(cio);
		  staleColumnsInOrder = false;
	  }
	  return columnsInOrder;
  }

  /**
   * Sets the column order. Must be element keys.
   *
   * @param colOrder
   *          an ordered array of the database names of the table's columns
   */
  public void setColumnOrder(List<String> colOrder) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
    	setColumnOrder(db, colOrder);
    } finally {
      // TODO: fix the when to close problem
//    	db.close();
    }
  }

  private void setColumnOrder(SQLiteDatabase db, List<String> columnOrder) {
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
    tableKVSH.setString(db, KEY_COLUMN_ORDER, colOrderList);
    this.columnOrder = columnOrder;
    this.staleColumnsInOrder = true;
  }

  /**
   * @return a copy of the element names of the prime columns. Since
   * is a copy, should cache when possible.
   */
  public List<String> getPrimeColumns() {
    List<String> defensiveCopy = new ArrayList<String>();
    defensiveCopy.addAll(this.primeColumns);
    return defensiveCopy;
  }

  public boolean isColumnPrime(String colDbName) {
    for (String prime : getPrimeColumns()) {
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
  public void setPrimeColumns(List<String> primes) {
    String primesStr;
    try {
      primesStr = mapper.writeValueAsString(primes);
      tableKVSH.setString(KEY_PRIME_COLUMNS, primesStr);
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
   * This is the user-friendly-ish string that for the short term (May 6)
   * is the
   * display name on the server.
   * @return
   */
  public String getTableKey() {
    return this.tableKey;
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
    tableKVSH.setString(KEY_SORT_COLUMN, sortColumn);
    this.sortColumn = sortColumn;
  }

  /**
   * Return the element key of the indexed (frozen) column.
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
    tableKVSH.setString(KEY_INDEX_COLUMN, indexColumnElementKey);
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
    SQLiteDatabase db = dbh.getWritableDatabase();
    TableDefinitions.setValue(tableId,
        TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS, accessControls, db);
    this.accessControls = accessControls;
    // TODO: figure out how to handle closing the db
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
   * @return the sync tag. Unsynched tables return the empty string.
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
    SQLiteDatabase db = dbh.getWritableDatabase();
    TableDefinitions.setValue(tableId, TableDefinitionsColumns.SYNC_TAG,
        syncTag, db);
    this.syncTag = syncTag;
    // TODO: figure out how to handle closing the database
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
    SQLiteDatabase db = dbh.getWritableDatabase();
    TableDefinitions.setValue(tableId, TableDefinitionsColumns.LAST_SYNC_TIME,
        time, db);
    this.lastSyncTime = time;
    // TODO: figure out how to handle closing the db
  }

  public KeyValueStore.Type getBackingStoreType() {
    return this.backingStore;
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
    tableKVSH.setString(KEY_SUM_DISPLAY_FORMAT, format);
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
      SQLiteDatabase db = dbh.getWritableDatabase();
      TableDefinitions.setValue(tableId, TableDefinitionsColumns.SYNC_STATE,
          state.name(), db);
      this.syncState = state;
      // TODO: figure out how to handle closing the db
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
    tableKVSH.setInteger(TableDefinitionsColumns.TRANSACTIONING,
        SyncUtil.boolToInt(transactioning));
    this.transactioning = transactioning;
  }

  public String toJson() {
    Map<String, ColumnProperties> columnsMap = getColumns(); // ensuring columns is initialized
    // I think this removes exceptions from not having getters/setters...
    ArrayList<String> colOrder = new ArrayList<String>();
    ArrayList<Object> cols = new ArrayList<Object>();
    for (ColumnProperties cp : columnsMap.values()) {
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
	  jo.put(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS, accessControls);
	  jo.put(JSON_KEY_COLUMN_ORDER, colOrder);
	  jo.put(JSON_KEY_COLUMNS, cols);
	  jo.put(JSON_KEY_PRIME_COLUMNS, primes);
	  jo.put(JSON_KEY_SORT_COLUMN, sortColumn);
	  jo.put(JSON_KEY_INDEX_COLUMN, indexColumn);
	  jo.put(JSON_KEY_CURRENT_VIEW_TYPE, currentViewType.name());
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
	Map<String, ColumnProperties> columnsMap = getColumns(); // ensuring columns is initialized
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
	      (String) jo.get(TableDefinitionsColumns.TABLE_ID_ACCESS_CONTROLS));
	  setCurrentViewType(TableViewType.valueOf(
	      (String)jo.get(JSON_KEY_CURRENT_VIEW_TYPE)));
	  setSummaryDisplayFormat((String) jo.get(JSON_KEY_SUM_DISPLAY_FORMAT));
	  Set<String> columnsToDelete = new HashSet<String>();
	  for (String cdn : columnOrder) {
	    columnsToDelete.add(cdn);
	  }
	  ArrayList<Object> colJArr = (ArrayList<Object>) jo.get(JSON_KEY_COLUMNS);
	  for (int i = 0; i < colOrder.size(); i++) {
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
		  ColumnProperties cp = getColumnByElementKey(colOrder.get(i));
          if (cp == null) {
            // then we need to create the bolumn.
            String coDispName =
                (String) colJo.get(ColumnProperties.KEY_DISPLAY_NAME);
            String coElKey =
                (String) colJo.get(ColumnProperties.JSON_KEY_ELEMENT_KEY);
            String coElName =
                (String) colJo.get(ColumnProperties.JSON_KEY_ELEMENT_NAME);
            cp = addColumn(coDispName, coElKey, coElName);
            cp.setFromJson((String) colJArr.get(i));
          }
          columnsToDelete.remove(colOrder.get(i));
      }
      for (String columnToDelete : columnsToDelete) {
        deleteColumn(columnToDelete);
      }
      setColumnOrder(colOrder);
//      orderColumns();
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
    Map<String, ColumnProperties> columnProperties = this.getColumns();
    for (ColumnProperties cp : columnProperties.values()) {
      if (cp.getColumnType() == ColumnType.NUMBER || cp.getColumnType()
          == ColumnType.INTEGER) {
        numericColCount++;
      } else if (cp.getColumnType() == ColumnType.GEOPOINT) {
        locationColCount += 2;// latitude and longitude
      } else if (cp.getColumnType() == ColumnType.DATE || cp.getColumnType()
          == ColumnType.DATETIME
          || cp.getColumnType() == ColumnType.TIME) {
        dateColCount++;
      } else if(isLatitudeColumn(cp) || isLongitudeColumn(cp)) {
    	  locationColCount++;
      }
    }
    List<TableViewType> list = new ArrayList<TableViewType>();
    list.add(TableViewType.Spreadsheet);
    list.add(TableViewType.List);
    list.add(TableViewType.Graph);
    if (locationColCount >= 1) {
      list.add(TableViewType.Map);
    }
    TableViewType[] arr = new TableViewType[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i);
    }
    return arr;
  }

  public static boolean isLatitudeColumn(ColumnProperties cp) {
    return (cp.getElementType() == ColumnType.GEOPOINT) ||
        endsWithIgnoreCase(cp.getDisplayName(), "latitude");
  }

  public static boolean isLongitudeColumn(ColumnProperties cp) {
    return (cp.getElementType() == ColumnType.GEOPOINT) ||
        endsWithIgnoreCase(cp.getDisplayName(), "longitude");
  }

  private static boolean endsWithIgnoreCase(String text, String ending) {
    if ( text.equalsIgnoreCase(ending) ) {
      return true;
    }
    int spidx = text.lastIndexOf(' ');
    int usidx = text.lastIndexOf('_');
    int idx = Math.max(spidx,  usidx);
    if ( idx == -1 ) {
      return false;
    }
    return text.substring(idx+1).equalsIgnoreCase(ending);
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
