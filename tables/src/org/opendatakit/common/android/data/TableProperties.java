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
package org.opendatakit.common.android.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.KeyValueStoreColumns;
import org.opendatakit.common.android.provider.TableDefinitionsColumns;
import org.opendatakit.common.android.utilities.ColorRuleUtil;
import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.common.android.utilities.ODKDataUtils;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

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
 * places in the datastore: the immutable (ish) properties that are part of the
 * table's definition (things like db backing name, type of the table, etc), are
 * stored in the table_definitions table. The ODK Tables-specific properties
 * exist in the key value stores as described above.
 *
 * @author hkworden@gmail.com (Hilary Worden)
 * @author sudar.sam@gmail.com
 */
public class TableProperties {

  private static final String t = "TableProperties";

  /***********************************
   * The partition and aspect of table properties in the key value store.
   * KeyValueStoreConstants.PARTITION_TABLE
   ***********************************/

  /***********************************
   * The names of keys that are defaulted to exist in the key value store.
   *
   * KeyValueStoreConstants.TABLE_DISPLAY_NAME
   * KeyValueStoreConstants.TABLE_COL_ORDER
   * KeyValueStoreConstants.TABLE_GROUP_BY_COLS
   * KeyValueStoreConstants.TABLE_SORT_COL
   * KeyValueStoreConstants.TABLE_SORT_ORDER
   * KeyValueStoreConstants.TABLE_INDEX_COL
   ***********************************/

  // INDEX_COL is held fixed during left/right pan

  // this is not known by the server...
  private static final String KEY_DEFAULT_VIEW_TYPE = "defaultViewType";

  /** The file name for the list view that has been set on the table. */
  public static final String KEY_LIST_VIEW_FILE_NAME = "listViewFileName";

  /** The file name for the detail view that has been set on the table. */
  public static final String KEY_DETAIL_VIEW_FILE_NAME = "detailViewFileName";

  /** The file name for the list view that is displayed in the map. */
  public static final String KEY_MAP_LIST_VIEW_FILE_NAME = "mapListViewFileName";

  /***********************************
   * Default values for those keys which require them. TODO When the keys in the
   * KVS are moved to the respective classes that use them, these should go
   * there most likely.
   ***********************************/
  public static final String DEFAULT_KEY_GROUP_BY_COLUMNS = "";
  public static final String DEFAULT_KEY_SORT_COLUMN = "";
  public static final String DEFAULT_KEY_SORT_ORDER = "ASC";
  public static final String DEFAULT_KEY_INDEX_COLUMN = "";
  public static final String DEFAULT_KEY_CURRENT_VIEW_TYPE = TableViewType.SPREADSHEET.name();
  public static final String DEFAULT_KEY_COLUMN_ORDER = "";

  /*
   * These are the keys that exist in the key value store after the creation of
   * a table. In other words they should always exist in the key value store.
   */
  private static final String[] INIT_KEYS = { KeyValueStoreConstants.TABLE_DISPLAY_NAME,
      KeyValueStoreConstants.TABLE_COL_ORDER, KeyValueStoreConstants.TABLE_GROUP_BY_COLS,
      KeyValueStoreConstants.TABLE_SORT_COL, KeyValueStoreConstants.TABLE_SORT_ORDER,
      KEY_DEFAULT_VIEW_TYPE, KeyValueStoreConstants.TABLE_INDEX_COL };

  // these are now safe for multi-appName hosting...
  // we probably want to maintain only 'N' table properties in memory (in a
  // fixed-length array).

  private static Map<String, List<String>> activeAppNameTableIds = new HashMap<String, List<String>>();
  private static Map<String, Map<String, TableProperties>> activeAppNameTableIdMap = new HashMap<String, Map<String, TableProperties>>();

  /**
   * Return the TableProperties for all the tables in the specified KVS. store.
   *
   * @param dbh
   * @param typeOfStore
   *          the KVS from which to get the store
   * @return
   */
  private static synchronized void refreshActiveCache(Context context, String appName,
      SQLiteDatabase db) {
    try {
      List<String> activeTableIds = ODKDatabaseUtils.getAllTableIds(db);

      HashMap<String, TableProperties> activeTableIdMap = new HashMap<String, TableProperties>();
      for (String tableId : activeTableIds) {
        Map<String, String> propPairs = getMapOfPropertiesForTable(db, tableId);
        TableProperties tp = constructPropertiesFromMap(context, appName, db, propPairs);
        if (tp == null) {
          throw new IllegalStateException("Unexpectedly missing " + tableId);
        }
        activeTableIdMap.put(tp.getTableId(), tp);
      }
      activeAppNameTableIdMap.put(appName, activeTableIdMap);
      activeAppNameTableIds.put(appName, activeTableIds);
    } finally {
    }
  }

  private static synchronized void ensureActiveTableIdMapLoaded(Context context, String appName,
      SQLiteDatabase db) {
    List<String> allIds = ODKDatabaseUtils.getAllTableIds(db);
    List<String> knownIds = activeAppNameTableIds.get(appName);
    if (knownIds == null || knownIds.size() != allIds.size()) {
      refreshActiveCache(context, appName, db);
    }
  }

  /**
   *
   * @param typeOfStore
   *          - null if everything
   */
  public static synchronized void markStaleCache(String appName) {
    activeAppNameTableIds.remove(appName);
    activeAppNameTableIdMap.remove(appName);
  }

  /**
   * Return the TableProperties for the given table id.
   *
   * @param dbh
   * @param tableId
   * @param typeOfStore
   *          the store from which to get the properties
   * @param forceRefresh
   *          do not use the cache; update it with a fresh pull
   * @return
   */
  public static TableProperties getTablePropertiesForTable(Context context, String appName,
      String tableId) {

    DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
    SQLiteDatabase db = dh.getReadableDatabase();
    try {
      ensureActiveTableIdMapLoaded(context, appName, db);
      // just use the cached value...
      return activeAppNameTableIdMap.get(appName).get(tableId);
    } finally {
      db.close();
    }
  }

  public static TableProperties refreshTablePropertiesForTable(Context context, String appName,
      String tableId) {

    DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
    SQLiteDatabase db = dh.getReadableDatabase();
    try {
      ensureActiveTableIdMapLoaded(context, appName, db);
      Map<String, String> mapProps = getMapOfPropertiesForTable(db, tableId);
      TableProperties tp = constructPropertiesFromMap(context, appName, db, mapProps);
      if (tp != null) {
        // update the cache...
        Map<String, TableProperties> activeTableIdMap = activeAppNameTableIdMap.get(appName);
        activeTableIdMap.put(tp.getTableId(), tp);
      }
      return tp;
    } finally {
      db.close();
    }
  }

  /**
   * Return the TableProperties for all the tables in the specified KVS. store.
   *
   * @param dbh
   * @param typeOfStore
   *          the KVS from which to get the store
   * @return
   */
  public static TableProperties[] getTablePropertiesForAll(Context context, String appName) {
    ODKFileUtils.assertDirectoryStructure(appName);
    SQLiteDatabase db = null;
    try {
      DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
      db = dh.getReadableDatabase();
      ensureActiveTableIdMapLoaded(context, appName, db);
      Map<String, TableProperties> activeTableIdMap = activeAppNameTableIdMap.get(appName);
      return activeTableIdMap.values().toArray(new TableProperties[activeTableIdMap.size()]);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public static List<String> getAllTableIds(Context context, String appName) {
    ODKFileUtils.assertDirectoryStructure(appName);
    SQLiteDatabase db = null;
    try {
      DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
      db = dh.getReadableDatabase();
      return ODKDatabaseUtils.getAllTableIds(db);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  /***********************************
   * The fields that make up a TableProperties object.
   ***********************************/
  /*
   * The fields that belong only to the object, and are not related to the
   * actual table itself.
   */

  private final Context context;
  private final String appName;
  /*
   * The fields that reside in TableDefintions
   */
  private final String tableId;
  private String schemaETag;
  private String lastDataETag;
  // TODO lastSyncTime should probably eventually be an int?
  // keeping as a string for now to minimize errors.
  private String lastSyncTime;
  /*
   * The fields that are in the key value store.
   */
  private String displayName;

  private ArrayList<ColumnDefinition> mDefinitions;
  
  private List<String> columnOrder;
  private List<String> groupByColumns;
  private String sortColumn;
  private String sortOrder;
  private String indexColumn;
  private TableViewType defaultViewType;
  private KeyValueStoreHelper tableKVSH;

  private TableProperties(Context context, String appName, SQLiteDatabase db, String tableId,
      String displayName, ArrayList<String> columnOrder,
      ArrayList<String> groupByColumns, String sortColumn, String sortOrder, String indexColumn,
      String tableSchemaETag, String tableDataETag,
      String lastSyncTime, TableViewType defaultViewType) {
    this.context = context;
    this.appName = appName;
    this.tableId = tableId;
    this.displayName = displayName;
    this.mDefinitions = null;
    if (groupByColumns == null) {
      groupByColumns = new ArrayList<String>();
    }
    this.groupByColumns = groupByColumns;
    if (sortColumn == null || sortColumn.length() == 0) {
      this.sortColumn = null;
    } else {
      this.sortColumn = sortColumn;
    }
    if (sortOrder == null || sortOrder.length() == 0) {
      this.sortOrder = null;
    } else {
      this.sortOrder = sortOrder;
    }
    if ((indexColumn == null)) {
      this.indexColumn = DEFAULT_KEY_INDEX_COLUMN;
    } else {
      this.indexColumn = indexColumn;
    }
    this.schemaETag = tableSchemaETag;
    this.lastDataETag = tableDataETag;
    this.lastSyncTime = lastSyncTime;
    this.defaultViewType = defaultViewType;
    this.tableKVSH = this.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_TABLE);

    // This should be OK even when we are creating a new database
    // because there should be no column entries defined yet.
    refreshColumns(db);
    if (columnOrder.size() == 0) {
      this.columnOrder = getPersistedColumns();
    } else {
      this.columnOrder = columnOrder;
    }
  }

  /**
   * Test whether the given database table has savepoint checkpoints
   *
   * @param db
   * @param dbTableName
   * @return
   */
  public boolean hasCheckpoints() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = null;
    try {
      c = db.rawQuery("SELECT COUNT(*) AS C FROM \"" + tableId + "\" WHERE "
          + DataTableColumns.SAVEPOINT_TYPE + " IS NULL", null);
      c.moveToFirst();
      int idxC = c.getColumnIndex("C");
      int value = c.getInt(idxC);
      c.close();
      return (value != 0);
    } finally {
      if (c != null && !c.isClosed()) {
        c.close();
      }
      if (db.isOpen()) {
        db.close();
      }
    }
  }

  /**
   * Test whether the given database table has conflict rows
   *
   * @param db
   * @param dbTableName
   * @return
   */
  public boolean hasConflicts() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = null;
    try {
      c = db.rawQuery("SELECT COUNT(*) AS C FROM \"" + tableId + "\" WHERE "
          + DataTableColumns.CONFLICT_TYPE + " IS NOT NULL", null);
      c.moveToFirst();
      int idxC = c.getColumnIndex("C");
      int value = c.getInt(idxC);
      c.close();
      return (value != 0);
    } finally {
      if (c != null && !c.isClosed()) {
        c.close();
      }
      if (db.isOpen()) {
        db.close();
      }
    }
  }

  public SQLiteDatabase getReadableDatabase() {
    DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
    return dh.getReadableDatabase();
  }

  public SQLiteDatabase getWritableDatabase() {
    DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
    return dh.getWritableDatabase();
  }
  /*
   * The SQL query for selecting the row specified by the table id.
   */
  private static final String WHERE_SQL_FOR_TABLE_DEFINITION = TableDefinitionsColumns.TABLE_ID + " = ?";

  /**
   * Return a map of columnName->Value for the row with the given table id.
   * TODO: perhaps this should become columnName->TypevValuePair like the rest
   * of these maps.
   * @param db
   * @param tableId
   * @return
   */
  private static TableDefinitionEntry getFields(SQLiteDatabase db, String tableId ) {
    Cursor c = null;
    TableDefinitionEntry entry = new TableDefinitionEntry(tableId);
    try {
      c = db.query(DataModelDatabaseHelper.TABLE_DEFS_TABLE_NAME, null, 
          WHERE_SQL_FOR_TABLE_DEFINITION,
          new String[] {tableId}, null, null, null);

      if (c.getCount() > 1) {
        Log.e(t, "query for tableId: " + tableId + " returned >1 row in" +
            "TableDefinitions");
        throw new IllegalStateException("Multiple TableDefinitions for " + tableId);
      }
      if (c.getCount() < 1) {
        Log.e(t, "query for tableId: " + tableId + " returned <1 row in" +
            "TableDefinitions");
        return null;
      }

      c.moveToFirst();
      int idxId = c.getColumnIndex(TableDefinitionsColumns.TABLE_ID);
      int idxLastTime = c.getColumnIndex(TableDefinitionsColumns.LAST_SYNC_TIME);
      int idxSchemaETag = c.getColumnIndex(TableDefinitionsColumns.SCHEMA_ETAG);
      int idxTableDataETag = c.getColumnIndex(TableDefinitionsColumns.LAST_DATA_ETAG);

      if ( c.isNull(idxId) ) {
        throw new IllegalStateException("unexpected null tableId!");
      }
      entry.tableId = c.getString(idxId);
      entry.lastSyncTime = c.isNull(idxLastTime) ? null : c.getString(idxLastTime);
      entry.schemaETag = c.isNull(idxSchemaETag) ? null : c.getString(idxSchemaETag);
      entry.lastDataETag = c.isNull(idxTableDataETag) ? null : c.getString(idxTableDataETag);

      return entry;
    } finally {
        if (c != null && !c.isClosed()) {
          c.close();
        }
    }
  }

  /*
   * The base wehre clause for getting only the key values that are contained
   * in the list of table properties. Its usage must be followed by appending
   * ")".
   */
  private static final String WHERE_SQL_FOR_PARTITION_ASPECT_KEYS =
      KeyValueStoreColumns.TABLE_ID + " = ? AND " +
      KeyValueStoreColumns.PARTITION + " = ? AND " +
      KeyValueStoreColumns.ASPECT + " = ? AND " +
      KeyValueStoreColumns.KEY + " in (";

  /**
   * Returns a string of question marks separated by commas for use in an
   * android sqlite query.
   * @param numArgs number of question marks
   * @return
   */
  private static String makePlaceHolders(int numArgs) {
    String holders = "";
    if (numArgs == 0)
      return holders;
    for (int i = 0; i < numArgs; i++) {
      holders = holders + "?,";
    }
    holders = holders.substring(0, holders.length()-1);
    return holders;
  }

  /*
   * Get the full entries from the table. These are the full entries, with
   * tableId and type information.
   */
  private static ArrayList<KeyValueStoreEntry> getEntriesFromCursor(Cursor c) {
    ArrayList<KeyValueStoreEntry> entries =
        new ArrayList<KeyValueStoreEntry>();
    int idIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.TABLE_ID);
    int partitionIndex =
        c.getColumnIndexOrThrow(KeyValueStoreColumns.PARTITION);
    int aspectIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.ASPECT);
    int keyIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.KEY);
    int valueIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.VALUE);
    int typeIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.VALUE_TYPE);
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      KeyValueStoreEntry entry = new KeyValueStoreEntry();
      entry.tableId = ODKDatabaseUtils.getIndexAsString(c, idIndex);
      entry.partition = ODKDatabaseUtils.getIndexAsString(c, partitionIndex);
      entry.aspect = ODKDatabaseUtils.getIndexAsString(c, aspectIndex);
      entry.key = ODKDatabaseUtils.getIndexAsString(c, keyIndex);
      entry.type = ODKDatabaseUtils.getIndexAsString(c, typeIndex);
      entry.value = ODKDatabaseUtils.getIndexAsString(c, valueIndex);
      entries.add(entry);
      i++;
      c.moveToNext();
    }
    c.close();
    return entries;
  }

  /*
   * Return a map of key to value from a cursor that has queried the database
   * backing the key value store.
   */
  private static Map<String, String> getKeyValuesFromCursor(Cursor c) {
    int keyIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.KEY);
    int valueIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.VALUE);
    Map<String, String> keyValues = new HashMap<String, String>();
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      String value = ODKDatabaseUtils.getIndexAsString(c, valueIndex);
      if (value == null || value.equals("")) {
        value = null;
      }
      keyValues.put(ODKDatabaseUtils.getIndexAsString(c, keyIndex), value);
      i++;
      c.moveToNext();
    }
    c.close();
    return keyValues;
  }

  /**
   * Return a map of only the properties from the store backing this
   * object. These
   * are the properties as defined as the {@link INIT_KEYS} in TableProperties.
   * Empty strings are returned as null.
   * @param db
   * @return
   */
  private static Map<String, String> getProperties(SQLiteDatabase db, String tableId) {
    String[] basicProps = getInitKeys();
    String[] desiredKeys = new String[basicProps.length + 3];
    // we want the first to be the tableId, b/c that is the table id we are
    // querying over in the database.
    desiredKeys[0] = tableId;
    desiredKeys[1] = KeyValueStoreConstants.PARTITION_TABLE;
    desiredKeys[2] = KeyValueStoreConstants.ASPECT_DEFAULT;
    for (int i = 0; i < basicProps.length; i++) {
      desiredKeys[i+3] = basicProps[i];
    }
    String whereClause = WHERE_SQL_FOR_PARTITION_ASPECT_KEYS +
        makePlaceHolders(TableProperties.getInitKeys().length) + ")";
    Cursor c = null;
    try {
       c = db.query(DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME,
           new String[] {KeyValueStoreColumns.KEY, KeyValueStoreColumns.VALUE},
           whereClause,
           desiredKeys, null, null, null);
       return getKeyValuesFromCursor(c);
    } finally {
      if ( c != null && !c.isClosed() ) {
         c.close();
      }
    }
  }

  /*
   * Return the map of all the properties for the given table. The properties
   * include both the values of this table's row in TableDefinition and the
   * values in the key value store pointed to by INIT_KEYS.
   * 
   * Atm this is just key->value. The caller must know the intended type of the
   * value and parse it correctly. This map should eventually become a
   * key->TypeValuePair or something like that. TODO: make it the above
   * 
   * This deserves its own method b/c to get the properties you are forced to go
   * through both the key value store and the TableDefinitions table.
   * 
   * NOTE: this routine now supplies valid default values should the database
   * not contain the appropriate default settings for a particular value. This
   * provides a degree of upgrade-ability.
   * 
   * @param dbh
   * 
   * @param tableId
   * 
   * @param typeOfStore
   * 
   * @return
   */
  private static Map<String, String> getMapOfPropertiesForTable(SQLiteDatabase db, String tableId) {
    try {
      TableDefinitionEntry tableDefn = getFields(db, tableId);
      Map<String, String> kvsMap = getProperties(db, tableId);
      Map<String, String> mapProps = new HashMap<String, String>();
      // table definitions wins -- apply it 2nd
      mapProps.putAll(kvsMap);
      mapProps.put(TableDefinitionsColumns.LAST_SYNC_TIME, tableDefn.lastSyncTime);
      mapProps.put(TableDefinitionsColumns.SCHEMA_ETAG, tableDefn.schemaETag);
      mapProps.put(TableDefinitionsColumns.LAST_DATA_ETAG, tableDefn.lastDataETag);

      if (mapProps.get(TableDefinitionsColumns.TABLE_ID) == null) {
        mapProps.put(TableDefinitionsColumns.TABLE_ID, tableId);
      }
      if (mapProps.get(KeyValueStoreConstants.TABLE_DISPLAY_NAME) == null) {
        mapProps.put(KeyValueStoreConstants.TABLE_DISPLAY_NAME, tableId);
      }
      if (mapProps.get(KeyValueStoreConstants.TABLE_COL_ORDER) == null) {
        mapProps.put(KeyValueStoreConstants.TABLE_COL_ORDER, DEFAULT_KEY_COLUMN_ORDER);
      }
      if (mapProps.get(KeyValueStoreConstants.TABLE_GROUP_BY_COLS) == null) {
        mapProps.put(KeyValueStoreConstants.TABLE_GROUP_BY_COLS, DEFAULT_KEY_GROUP_BY_COLUMNS);
      }
      if (mapProps.get(KeyValueStoreConstants.TABLE_SORT_COL) == null) {
        mapProps.put(KeyValueStoreConstants.TABLE_SORT_COL, DEFAULT_KEY_SORT_COLUMN);
      }
      if (mapProps.get(KeyValueStoreConstants.TABLE_SORT_ORDER) == null) {
        mapProps.put(KeyValueStoreConstants.TABLE_SORT_ORDER, DEFAULT_KEY_SORT_ORDER);
      }
      if (mapProps.get(KeyValueStoreConstants.TABLE_INDEX_COL) == null) {
        mapProps.put(KeyValueStoreConstants.TABLE_INDEX_COL, DEFAULT_KEY_INDEX_COLUMN);
      }
      if (mapProps.get(KEY_DEFAULT_VIEW_TYPE) == null) {
        mapProps.put(KEY_DEFAULT_VIEW_TYPE, DEFAULT_KEY_CURRENT_VIEW_TYPE);
      }
      return mapProps;
    } finally {
      // TODO: fix the when to close problem
      // if ( db != null ) {
      // db.close();
      // }
    }
  }

  /*
   * Constructs a table properties object based on a map of key values as would
   * be acquired from the key value store.
   */
  private static TableProperties constructPropertiesFromMap(Context context, String appName,
      SQLiteDatabase db, Map<String, String> props) {
    String columnOrderValue = props.get(KeyValueStoreConstants.TABLE_COL_ORDER);
    String defaultViewTypeStr = props.get(KEY_DEFAULT_VIEW_TYPE);
    TableViewType defaultViewType;
    if (defaultViewTypeStr == null) {
      defaultViewType = TableViewType.SPREADSHEET;
      props.put(KEY_DEFAULT_VIEW_TYPE, TableViewType.SPREADSHEET.name());
    } else {
      try {
        defaultViewType = TableViewType.valueOf(defaultViewTypeStr);
      } catch (Exception e) {
        defaultViewType = TableViewType.SPREADSHEET;
        props.put(KEY_DEFAULT_VIEW_TYPE, TableViewType.SPREADSHEET.name());
      }
    }
    // for legacy reasons, the code expects the DB_COLUMN_ORDER and
    // DB_PRIME_COLUMN values to be empty strings, not null. However, when
    // retrieving values from the key value store, empty strings are converted
    // to null, because many others expect null values. For that reason, first
    // check here to set null values for these columns to empty strings.
    if (columnOrderValue == null)
      columnOrderValue = "";
    ArrayList<String> columnOrder = new ArrayList<String>();
    if (columnOrderValue.length() != 0) {
      try {
        columnOrder = ODKFileUtils.mapper.readValue(columnOrderValue, ArrayList.class);
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

    String groupByColumnsJsonString = props.get(KeyValueStoreConstants.TABLE_GROUP_BY_COLS);
    ArrayList<String> groupByCols = new ArrayList<String>();
    if (groupByColumnsJsonString != null && groupByColumnsJsonString.length() != 0) {
      try {
        groupByCols = ODKFileUtils.mapper.readValue(groupByColumnsJsonString, ArrayList.class);
      } catch (JsonParseException e) {
        e.printStackTrace();
        Log.e(t, "ignore invalid json: " + groupByColumnsJsonString);
      } catch (JsonMappingException e) {
        e.printStackTrace();
        Log.e(t, "ignore invalid json: " + groupByColumnsJsonString);
      } catch (IOException e) {
        e.printStackTrace();
        Log.e(t, "ignore invalid json: " + groupByColumnsJsonString);
      }
    }

    return new TableProperties(context, appName, db, props.get(TableDefinitionsColumns.TABLE_ID),
        props.get(KeyValueStoreConstants.TABLE_DISPLAY_NAME), columnOrder, groupByCols,
        props.get(KeyValueStoreConstants.TABLE_SORT_COL),
        props.get(KeyValueStoreConstants.TABLE_SORT_ORDER),
        props.get(KeyValueStoreConstants.TABLE_INDEX_COL), 
        props.get(TableDefinitionsColumns.SCHEMA_ETAG),
        props.get(TableDefinitionsColumns.LAST_DATA_ETAG),
        props.get(TableDefinitionsColumns.LAST_SYNC_TIME), defaultViewType);
  }
  
  /***********************************
   *  Default values for those columns which require them.
   ***********************************/
  public static final int DEFAULT_DB_LAST_SYNC_TIME = -1;

  /**
   * Add a table to the database. The intendedStore type exists to force you to
   * be specific to which store you are adding the table to.
   * <p>
   * NB: Currently adds all the keys defined in this class to the key value
   * store as well. This should likely change.
   * <p>
   * NB: Sets the db_table_name in TableDefinitions to the dbTableName
   * parameter.
   *
   * @param context
   * @param appName
   * @param dbTableName
   * @param displayName
   * @param tableId
   * @param typeOfStore
   * @return
   */
  public static TableProperties addTable(Context context, String appName, String dbTableName,
      String displayName, String tableId) {
    Log.e(t, "adding table with id: " + tableId);
    // First we will add the entry in TableDefinitions.
    // TODO: this should check for duplicate names.
    TableProperties tp = null;
    SQLiteDatabase db;
    try {
      DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
      db = dh.getWritableDatabase();
      db.beginTransaction();
      try {
        ContentValues values = new ContentValues();
        values.put(TableDefinitionsColumns.TABLE_ID, tableId);
        values.putNull(TableDefinitionsColumns.SCHEMA_ETAG);
        values.putNull(TableDefinitionsColumns.LAST_DATA_ETAG);
        values.put(TableDefinitionsColumns.LAST_SYNC_TIME, DEFAULT_DB_LAST_SYNC_TIME);
        db.insert(DataModelDatabaseHelper.TABLE_DEFS_TABLE_NAME, null, values);

        Map<String, String> propPairs = getMapOfPropertiesForTable(db, tableId);
        if (!(displayName.startsWith("\"") || displayName.startsWith("{"))) {
          // wrap it so that it is a JSON string
          displayName = ODKFileUtils.mapper.writeValueAsString(displayName);
        }
        propPairs.put(KeyValueStoreConstants.TABLE_DISPLAY_NAME, displayName);
        tp = constructPropertiesFromMap(context, appName, db, propPairs);
        if (tp == null) {
          throw new IllegalStateException("Unexpectedly missing " + tableId);
        }
        Log.d(t, "adding table: " + dbTableName);
        
        // TODO: confirm that column definitions are available
        ODKDatabaseUtils.createOrOpenDBTableWithColumns(db, 
            tp.getTableId(), ColumnDefinition.getColumns(tp.getColumnDefinitions()));

        // And now set the default color rules.
        ColorRuleGroup ruleGroup = ColorRuleGroup.getStatusColumnRuleGroup(tp);
        ruleGroup.replaceColorRuleList(ColorRuleUtil.getDefaultSyncStateColorRules());
        ruleGroup.saveRuleList();
        db.setTransactionSuccessful();
      } catch (Exception e) {
        e.printStackTrace();
        if (e instanceof IllegalStateException) {
          throw (IllegalStateException) e;
        } else {
          throw new IllegalStateException("TableProperties could not be created", e);
        }
      } finally {
        db.endTransaction();
        db.close();
      }
      return tp;
    } finally {
      // TODO: fix the when to close problem
      // db.close();
    }
  }

  /**
   * Remove the table from the local database. This cannot be undone.
   */
  public void deleteTable() {
    // Two things must be done: delete all the key value pairs from the active
    // key value store and drop the table holding the data from the database.
    String tableDir = ODKFileUtils.getTablesFolder(appName, tableId);
    try {
      FileUtils.deleteDirectory(new File(tableDir));
    } catch (IOException e1) {
      e1.printStackTrace();
      throw new IllegalStateException("Unable to delete the " + tableDir + " directory", e1);
    }

    String assetsCsvDir = ODKFileUtils.getAssetsFolder(appName) + "/csv";
    try {
      Collection<File> files = FileUtils.listFiles(new File(assetsCsvDir), new IOFileFilter() {

        @Override
        public boolean accept(File file) {
          String[] parts = file.getName().split("\\.");
          return (parts[0].equals(tableId) && parts[parts.length - 1].equals("csv") && (parts.length == 2
              || parts.length == 3 || (parts.length == 4 && parts[parts.length - 2]
              .equals("properties"))));
        }

        @Override
        public boolean accept(File dir, String name) {
          String[] parts = name.split("\\.");
          return (parts[0].equals(tableId) && parts[parts.length - 1].equals("csv") && (parts.length == 2
              || parts.length == 3 || (parts.length == 4 && parts[parts.length - 2]
              .equals("properties"))));
        }
      }, new IOFileFilter() {

        // don't traverse into directories
        @Override
        public boolean accept(File arg0) {
          return false;
        }

        // don't traverse into directories
        @Override
        public boolean accept(File arg0, String arg1) {
          return false;
        }
      });

      FileUtils.deleteDirectory(new File(tableDir));
      for (File f : files) {
        FileUtils.deleteQuietly(f);
      }
    } catch (IOException e1) {
      e1.printStackTrace();
      throw new IllegalStateException("Unable to delete the " + tableDir + " directory", e1);
    }

    SQLiteDatabase db = null;
    try {
      DataModelDatabaseHelper dh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
      db = dh.getWritableDatabase();
      ODKDatabaseUtils.deleteTableAndData(db, tableId);
    } finally {
      if ( db != null && db.isOpen()) {
        db.close();
      }
      markStaleCache(appName);
    }
  }

  public String getAppName() {
    return appName;
  }

  /**
   * This is the user-defined string that is not translated and uniquely
   * identifies this data table. Begins as the cleaned up display name of the
   * table.
   *
   * @return
   */
  public String getTableId() {
    return tableId;
  }

  /**
   * @return the table's display name
   */
  public String getDisplayName() {
    return displayName;
  }

  public String getLocalizedDisplayName() {
    String localized = ODKDataUtils.getLocalizedDisplayName(getDisplayName());
    if (localized == null) {
      return getTableId();
    }
    return localized;
  }

  /**
   * Sets the table's display name.
   *
   * @param displayName
   *          the new display name
   */
  public void setDisplayName(SQLiteDatabase db, String displayName) {
    tableKVSH.setString(db, KeyValueStoreConstants.TABLE_DISPLAY_NAME, displayName);
    this.displayName = displayName;
  }

  /**
   * Get the current view type of the table.
   *
   * @return
   */
  public TableViewType getDefaultViewType() {
    return this.defaultViewType;
  }

  /**
   * Set the current view type of the table.
   *
   * @param viewType
   */
  public void setDefaultViewType(SQLiteDatabase db, TableViewType viewType) {
    tableKVSH.setString(db, TableProperties.KEY_DEFAULT_VIEW_TYPE, viewType.name());
    this.defaultViewType = viewType;
  }

  private void refreshColumnsFromDatabase() {
    SQLiteDatabase db = null;
    try {
      db = getReadableDatabase();
      refreshColumns(db);
    } finally {
      db.close();
    }
  }

  public void setColumnDefinitions(ArrayList<ColumnDefinition> defns) {
    this.mDefinitions = defns;
  }
  
  public ArrayList<ColumnDefinition> getColumnDefinitions() {
    return this.mDefinitions;
  }
  
  /**
   * Pulls the columns from the database into this TableProperties. Also updates
   * the maps of display name and sms label.
   */
  private void refreshColumns(SQLiteDatabase db) {
    List<Column> columns = ODKDatabaseUtils.getUserDefinedColumns(db, getTableId());
    ArrayList<ColumnDefinition> colDefns = ColumnDefinition.buildColumnDefinitions(columns);
    this.setColumnDefinitions(colDefns);
  }

  public ColumnDefinition getColumnDefinitionByElementKey(String elementKey) {
    return ColumnDefinition.find(mDefinitions, elementKey);
  }
  

  /**
   * Return the element key for the column based on the element path.
   * <p>
   * TODO: CURRENTLY A HACK!!!
   *
   * @param elementPath
   * @return
   */
  public String getElementKeyFromElementPath(String elementPath) {
    // TODO: do this correctly. This is just a hack that often works.
    String hackPath = elementPath.replace(".", "_");
    return hackPath;
  }

  /**
   * The column order is specified by an ordered list of element keys.
   *
   * @return a copy of the columnOrder. Since it is a copy, should cache when
   *         possible.
   */
  public List<String> getColumnOrder() {
    List<String> defensiveCopy = new ArrayList<String>();
    defensiveCopy.addAll(columnOrder);
    return defensiveCopy;
  }

  /**
   * Get the columns that are actually stored in the database
   *
   * @return
   */
  public List<String> getPersistedColumns() {
    List<String> persistedColumns = new ArrayList<String>();
    if (mDefinitions == null) {
      refreshColumnsFromDatabase();
    }

    ArrayList<ColumnDefinition> orderedDefns = this.getColumnDefinitions();
    for ( ColumnDefinition cd : orderedDefns ) {
      if ( cd.isUnitOfRetention() ) {
        persistedColumns.add(cd.getElementKey());
      }
    }
    return persistedColumns;
  }

  public void setColumnOrder(SQLiteDatabase db, List<String> columnOrder)
      throws JsonGenerationException, JsonMappingException, IOException {
    String colOrderList = null;
    colOrderList = ODKFileUtils.mapper.writeValueAsString(columnOrder);
    tableKVSH.setString(db, KeyValueStoreConstants.TABLE_COL_ORDER, colOrderList);
    this.columnOrder = columnOrder;
  }

  /**
   * @return a copy of the element names of the prime columns. Since is a copy,
   *         should cache when possible.
   */
  public List<String> getGroupByColumns() {
    List<String> defensiveCopy = new ArrayList<String>();
    defensiveCopy.addAll(this.groupByColumns);
    return defensiveCopy;
  }

  public boolean isGroupByColumn(String elementKey) {
    return groupByColumns.contains(elementKey);
  }

  public boolean hasGroupByColumns() {
    return !groupByColumns.isEmpty();
  }

  /**
   * Sets the table's prime columns.
   *
   * @param db
   * @param groupByCols
   *          an array of the database names of the table's prime columns
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonGenerationException
   */
  public void setGroupByColumns(SQLiteDatabase db, List<String> groupByCols)
      throws JsonGenerationException, JsonMappingException, IOException {
    String groupByJsonStr;
    if (groupByCols == null) {
      groupByCols = new ArrayList<String>();
    }
    groupByJsonStr = ODKFileUtils.mapper.writeValueAsString(groupByCols);
    tableKVSH.setString(db, KeyValueStoreConstants.TABLE_GROUP_BY_COLS, groupByJsonStr);
    this.groupByColumns = groupByCols;
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
  public void setSortColumn(SQLiteDatabase db, String sortColumn) {
    if ((sortColumn != null) && (sortColumn.length() == 0)) {
      sortColumn = null;
    }
    tableKVSH.setString(db, KeyValueStoreConstants.TABLE_SORT_COL, sortColumn);
    this.sortColumn = sortColumn;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(SQLiteDatabase db, String sortOrder) {
    if ((sortOrder != null) && (sortOrder.length() == 0)) {
      sortOrder = null;
    }

    tableKVSH.setString(db, KeyValueStoreConstants.TABLE_SORT_ORDER, sortOrder);
    this.sortOrder = sortOrder;
  }

  /**
   * Unimplemented.
   * <p>
   * Should set the table id. First should verify uniqueness, etc.
   *
   * @param newTableId
   */
  public void setTableId(String newTableId) {
    throw new UnsupportedOperationException("setTableId is not yet implemented.");
  }

  /**
   * Return the element key of the indexed (frozen) column.
   *
   * @return
   */
  public String getIndexColumn() {
    return this.indexColumn;
  }

  /**
   * Set the index column for the table. This should be set by the display name
   * of the column. A null value will set the index column back to the default
   * value. TODO: make this use the element key
   *
   * @param indexColumnElementKey
   */
  public void setIndexColumn(SQLiteDatabase db, String indexColumnElementKey) {
    if ((indexColumnElementKey == null)) {
      indexColumnElementKey = DEFAULT_KEY_INDEX_COLUMN;
    }
    tableKVSH.setString(db, KeyValueStoreConstants.TABLE_INDEX_COL, indexColumnElementKey);
    this.indexColumn = indexColumnElementKey;
  }

  public String getSchemaETag() {
    return this.schemaETag;
  }
  
  public String getDataETag() {
    return this.lastDataETag;
  }

  public void setSchemaETag(String schemaETag) {
    SQLiteDatabase db = getWritableDatabase();
    try {
      db.beginTransaction();
      ContentValues values = new ContentValues();
      boolean clear = false;
      values.put(TableDefinitionsColumns.SCHEMA_ETAG, schemaETag);
      if ( schemaETag == null ? (schemaETag != null) : !schemaETag.equals(schemaETag) ) {
        clear = true;
        values.putNull(TableDefinitionsColumns.LAST_DATA_ETAG); // reset
      }
      db.update(DataModelDatabaseHelper.TABLE_DEFS_TABLE_NAME, values, WHERE_SQL_FOR_TABLE_DEFINITION,
          new String[] {tableId});
      this.schemaETag = schemaETag;
      if ( clear ) {
        this.lastDataETag = null;
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }
  
  public void setDataETag(String dataETag) {
    SQLiteDatabase db = getWritableDatabase();
    try {
      db.beginTransaction();
      ContentValues values = new ContentValues();
      values.put(TableDefinitionsColumns.LAST_DATA_ETAG, dataETag); // reset
      db.update(DataModelDatabaseHelper.TABLE_DEFS_TABLE_NAME, values, WHERE_SQL_FOR_TABLE_DEFINITION,
          new String[] {tableId});
      this.lastDataETag = dataETag;
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
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
    SQLiteDatabase db = getWritableDatabase();
    try {
      db.beginTransaction();
      ContentValues values = new ContentValues();
      values.put(TableDefinitionsColumns.LAST_SYNC_TIME, time);
      db.update(DataModelDatabaseHelper.TABLE_DEFS_TABLE_NAME, values, WHERE_SQL_FOR_TABLE_DEFINITION,
          new String[] {tableId});
      this.lastSyncTime = time;
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  /**
   * Get the possible view types for this table.
   * 
   * @return a {@link Set} containing the possible view types in the table.
   */
  public PossibleTableViewTypes getPossibleViewTypes() {
    boolean spreadsheetValid = this.spreadsheetViewIsPossible();
    boolean listValid = this.listViewIsPossible();
    boolean graphValid = this.graphViewIsPossible();
    boolean mapValid = this.mapViewIsPossible();
    PossibleTableViewTypes result = new PossibleTableViewTypes(spreadsheetValid, listValid,
        mapValid, graphValid);
    return result;
  }

  private boolean spreadsheetViewIsPossible() {
    return true;
  }

  private boolean listViewIsPossible() {
    return (getListViewFileName() != null);
  }

  /**
   *
   * @return true if a map view can be displayed for the table.
   */
  private boolean mapViewIsPossible() {
    List<ColumnDefinition> geoPoints = getGeopointColumnDefinitions();
    if (geoPoints.size() != 0) {
      return true;
    }

    List<String> elementKeys = getPersistedColumns();
    for (String elementKey : elementKeys) {
      ColumnDefinition cd = this.getColumnDefinitionByElementKey(elementKey);
      if (isLatitudeColumnDefinition(geoPoints, cd) || isLongitudeColumnDefinition(geoPoints, cd)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Need at least one numeric column to do a graph
   *
   * @return true is a graph view can be displayed on the table.
   */
  private boolean graphViewIsPossible() {
    List<String> elementKeys = getPersistedColumns();
    for (String elementKey : elementKeys) {
      ColumnDefinition cd = this.getColumnDefinitionByElementKey(elementKey);
      ElementType elementType = cd.getType();
      ElementDataType type = elementType.getDataType();
      if (type == ElementDataType.number || type == ElementDataType.integer) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the filename for the list view that has been set on this table.
   * 
   * @return the filename of the list view, or null if none exists.
   */
  public String getListViewFileName() {
    KeyValueStoreHelper kvsh = this.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_TABLE);
    String listFileName = kvsh.getString(KEY_LIST_VIEW_FILE_NAME);
    return listFileName;
  }

  /**
   * Return the file name for the list view that has been set to be displayed in
   * the map view.
   * 
   * @return the file name of the list view, or null if none exists.
   */
  public String getMapListViewFileName() {
    KeyValueStoreHelper kvsh = this.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_TABLE);
    String fileName = kvsh.getString(KEY_MAP_LIST_VIEW_FILE_NAME);
    return fileName;
  }

  /**
   * Return the file name for the detail view that has been set on this table.
   * 
   * @return the filename for the detail view, or null if none exists.
   */
  public String getDetailViewFileName() {
    KeyValueStoreHelper kvsh = this.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_TABLE);
    String detailFileName = kvsh.getString(KEY_DETAIL_VIEW_FILE_NAME);
    return detailFileName;
  }

  /**
   * Return the element groups that define a uriFragment of type rowpath
   * and a contentType. Whatever these are named, they are media attachment groups.
   * 
   * @return
   */
  public List<ColumnDefinition> getUriColumnDefinitions() {
    ArrayList<ColumnDefinition> orderedDefns = this.getColumnDefinitions();
    
    Set<ColumnDefinition> uriFragmentList = new HashSet<ColumnDefinition>();
    Set<ColumnDefinition> contentTypeList = new HashSet<ColumnDefinition>();
    
    for (ColumnDefinition cd : orderedDefns ) {
      ColumnDefinition cdParent = cd.getParent();
      
      if ( cd.getElementName().equals("uriFragment") && 
          cd.getType().getDataType() == ElementDataType.rowpath &&
              cdParent != null ) {
        uriFragmentList.add(cdParent);
      }
      if ( cd.getElementName().equals("contentType") &&
          cdParent != null ) {
        contentTypeList.add(cdParent);
      }
    }
    uriFragmentList.retainAll(contentTypeList);
    
    List<ColumnDefinition> cdList = new ArrayList<ColumnDefinition>(uriFragmentList);
    Collections.sort(cdList);
    return cdList;
  }

  public List<ColumnDefinition> getGeopointColumnDefinitions() {
    List<ColumnDefinition> cdList = new ArrayList<ColumnDefinition>();

    ArrayList<ColumnDefinition> orderedDefns = getColumnDefinitions();
    for (ColumnDefinition cd : orderedDefns) {
      if (cd.getType().getElementType().equals(ElementType.GEOPOINT)) {
        cdList.add(cd);
      }
    }
    return cdList;
  }

  public boolean isLatitudeColumnDefinition(List<ColumnDefinition> geoPointList, ColumnDefinition cd) {
    if ( !cd.isUnitOfRetention() ) {
      return false;
    }
    
    ElementDataType type = cd.getType().getDataType();
    if (!(type == ElementDataType.number || type == ElementDataType.integer)) {
      return false;
    }
    
    ColumnDefinition cdParent = cd.getParent();

    if ( cdParent != null &&
        geoPointList.contains(cdParent) &&
        cd.getElementName().equals("latitude")) {
      return true;
    }

    if (endsWithIgnoreCase(cd.getElementName(), "latitude")) {
      return true;
    }
    
    return false;
  }

  public boolean isLongitudeColumnDefinition(List<ColumnDefinition> geoPointList, ColumnDefinition cd) {
    if ( !cd.isUnitOfRetention() ) {
      return false;
    }
    
    ElementDataType type = cd.getType().getDataType();
    if (!(type == ElementDataType.number || type == ElementDataType.integer))
      return false;
    
    ColumnDefinition cdParent = cd.getParent();

    if ( cdParent != null &&
        geoPointList.contains(cdParent) &&
        cd.getElementName().equals("longitude")) {
      return true;
    }

    if (endsWithIgnoreCase(cd.getElementName(), "longitude")) {
      return true;
    }

    return false;
  }

  private static boolean endsWithIgnoreCase(String text, String ending) {
    if (text.equalsIgnoreCase(ending)) {
      return true;
    }
    int spidx = text.lastIndexOf(' ');
    int usidx = text.lastIndexOf('_');
    int idx = Math.max(spidx, usidx);
    if (idx == -1) {
      return false;
    }
    return text.substring(idx + 1).equalsIgnoreCase(ending);
  }

  /**
   * Get the accessor object for persisted values in the key value store.
   *
   * @param partition
   * @return
   */
  public KeyValueStoreHelper getKeyValueStoreHelper(String partition) {
    return new KeyValueStoreHelper(context, this.getAppName(), this.getTableId(), partition);
  }

  /**
   * Returns an array of the initialized properties. These are the keys that
   * exist in the key value store for any table.
   *
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

  /**
   * This is used by the various ListViews that display tables
   */
  @Override
  public String toString() {
    return getLocalizedDisplayName();
  }

}
