/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.utils;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.ElementDataType;
import org.opendatakit.common.android.data.KeyValueStoreEntry;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.utilities.NameUtil;
import org.opendatakit.common.android.utilities.ODKDataUtils;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.utils.LocalKeyValueStoreConstants.Tables;

import android.database.sqlite.SQLiteDatabase;

import com.fasterxml.jackson.core.JsonProcessingException;

public class TableUtil {

  private static final String TABLE_DEFAULT_VIEW_TYPE = "defaultViewType";

  // INDEX_COL is held fixed during left/right pan

  /***********************************
   * Default values for those keys which require them. TODO When the keys in the
   * KVS are moved to the respective classes that use them, these should go
   * there most likely.
   ***********************************/
  public static final String DEFAULT_KEY_SORT_ORDER = "ASC";
  public static final TableViewType DEFAULT_KEY_CURRENT_VIEW_TYPE = TableViewType.SPREADSHEET;

  private static TableUtil tableUtil = new TableUtil();
  
  public static TableUtil get() {
    return tableUtil;
  }
 
  /**
   * For mocking -- supply a mocked object.
   * 
   * @param util
   */
  public static void set(TableUtil util) {
    tableUtil = util;
  }
  
  protected TableUtil() {}
  
  public ArrayList<ColumnDefinition> getColumnDefinitions(SQLiteDatabase db, String tableId) {
    List<Column> columns = ODKDatabaseUtils.get().getUserDefinedColumns(db, tableId);
    return ColumnDefinition.buildColumnDefinitions(columns);
  }
  
  public String getLocalizedDisplayName(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String displayName = null;
    String jsonDisplayName = ah.getObject(KeyValueStoreConstants.TABLE_DISPLAY_NAME);
    if ( jsonDisplayName != null ) {
      displayName = ODKDataUtils.getLocalizedDisplayName(jsonDisplayName);
    }
    if ( displayName == null ) {
      displayName = NameUtil.constructSimpleDisplayName(tableId);
    }
    return displayName;
  }

  public String getRawDisplayName(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String jsonDisplayName = ah.getObject(KeyValueStoreConstants.TABLE_DISPLAY_NAME);
    if ( jsonDisplayName == null ) {
      jsonDisplayName = NameUtil.normalizeDisplayName(NameUtil.constructSimpleDisplayName(tableId));
    }
    return jsonDisplayName;
  }

  /**
   * Sets the table's raw displayName.
   * 
   * @param db
   * @param tableId
   * @param elementKey
   */
  public void setRawDisplayName( SQLiteDatabase db, String tableId, String rawDisplayName) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_DISPLAY_NAME;
    e.type = ElementDataType.object.name();
    e.value = rawDisplayName;
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Get the default view type for this table.
   * 
   * @param ctxt
   * @param appName
   * @param tableId
   * @return the specified default view type or SPREADSHEET_VIEW if none defined.
   */
  public TableViewType getDefaultViewType(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String rawViewType = ah.getString(TABLE_DEFAULT_VIEW_TYPE);
    if ( rawViewType == null ) {
      return DEFAULT_KEY_CURRENT_VIEW_TYPE;
    }
    try {
      TableViewType tvt = TableViewType.valueOf(rawViewType);
      return tvt;
    } catch (Exception e) {
      return DEFAULT_KEY_CURRENT_VIEW_TYPE;
    }
  }

  /**
   * Sets the table's raw displayName.
   * 
   * @param db
   * @param tableId
   * @param elementKey
   */
  public void setDefaultViewType( SQLiteDatabase db, String tableId, TableViewType viewType) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = TABLE_DEFAULT_VIEW_TYPE;
    e.type = ElementDataType.string.name();
    e.value = viewType.name();
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Get the filename for the detail view of this table.
   * 
   * @param ctxt
   * @param appName
   * @param tableId
   * @return null if none defined.
   */
  public String getDetailViewFilename(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String detailViewFilename = ah.getString(Tables.KEY_DETAIL_VIEW_FILE_NAME);
    return detailViewFilename;
  }

  /**
   * Sets the filename for the detail view of this table.
   * 
   * @param db
   * @param tableId
   * @param detailViewFilename
   */
  public void setDetailViewFilename( SQLiteDatabase db, String tableId, String detailViewFilename) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = Tables.KEY_DETAIL_VIEW_FILE_NAME;
    e.type = ElementDataType.string.name();
    e.value = detailViewFilename;
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Get the filename for the detail view of this table.
   * 
   * @param ctxt
   * @param appName
   * @param tableId
   * @return null if none defined.
   */
  public String getListViewFilename(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String listViewFilename = ah.getString(Tables.KEY_LIST_VIEW_FILE_NAME);
    return listViewFilename;
  }

  /**
   * Sets the filename for the detail view of this table.
   * 
   * @param db
   * @param tableId
   * @param listViewFilename
   */
  public void setListViewFilename( SQLiteDatabase db, String tableId, String listViewFilename) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = Tables.KEY_LIST_VIEW_FILE_NAME;
    e.type = ElementDataType.string.name();
    e.value = listViewFilename;
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Get the filename for the detail view of this table.
   * 
   * @param ctxt
   * @param appName
   * @param tableId
   * @return null if none defined.
   */
  public String getMapListViewFilename(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String listViewFilename = ah.getString(Tables.KEY_MAP_LIST_VIEW_FILE_NAME);
    return listViewFilename;
  }

  /**
   * Sets the filename for the detail view of this table.
   * 
   * @param db
   * @param tableId
   * @param listViewFilename
   */
  public void setMapListViewFilename( SQLiteDatabase db, String tableId, String listViewFilename) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = Tables.KEY_MAP_LIST_VIEW_FILE_NAME;
    e.type = ElementDataType.string.name();
    e.value = listViewFilename;
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Get the elementKey of the sort-by column
   * 
   * @param ctxt
   * @param appName
   * @param tableId
   * @return null if none defined.
   */
  public String getSortColumn(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String sortColumn = ah.getString(KeyValueStoreConstants.TABLE_SORT_COL);
    return sortColumn;
  }

  /**
   * Sets the table's sort column.
   * 
   * @param db
   * @param tableId
   * @param elementKey
   */
  public void setSortColumn( SQLiteDatabase db, String tableId, String elementKey) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_SORT_COL;
    e.type = ElementDataType.string.name();
    e.value = elementKey;
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Return the sort order of the display (ASC or DESC)
   *
   * @return ASC if none specified.
   */
  public String getSortOrder(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String sortOrder = ah.getString(KeyValueStoreConstants.TABLE_SORT_ORDER);
    if ( sortOrder == null ) {
      return DEFAULT_KEY_SORT_ORDER;
    }
    return sortOrder;
  }

  /**
   * Set the sort order of the display (ASC or DESC)
   * 
   * @param db
   * @param tableId
   * @param sortOrder
   */
  public void setSortOrder( SQLiteDatabase db, String tableId, String sortOrder) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_SORT_ORDER;
    e.type = ElementDataType.string.name();
    e.value = sortOrder;
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Return the element key of the indexed (frozen) column.
   *
   * @return null if none
   */
  public String getIndexColumn(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String indexColumn = ah.getString(KeyValueStoreConstants.TABLE_INDEX_COL);
    return indexColumn;
  }

  /**
   * Set the elementKey of the indexed (frozen) column.
   * 
   * @param db
   * @param tableId
   * @param elementKey
   */
  public void setIndexColumn( SQLiteDatabase db, String tableId, String elementKey) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_INDEX_COL;
    e.type = ElementDataType.string.name();
    e.value = elementKey;
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Get the group-by columns, in order
   * 
   * @param ctxt
   * @param appName
   * @param tableId
   * @return empty list if none
   */
  public ArrayList<String> getGroupByColumns(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    ArrayList<String> jsonGroupBys = ah.getArray(KeyValueStoreConstants.TABLE_GROUP_BY_COLS, String.class);
    if ( jsonGroupBys != null ) {
      return jsonGroupBys;
    }
    return new ArrayList<String>();
  }

  /**
   * Set the group-by columns.
   * 
   * @param db
   * @param tableId
   * @param elementKeys
   */
  public void setGroupByColumns( SQLiteDatabase db, String tableId, ArrayList<String> elementKeys) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_GROUP_BY_COLS;
    e.type = ElementDataType.array.name();
    try {
      e.value = ODKFileUtils.mapper.writeValueAsString(elementKeys);
    } catch (JsonProcessingException e1) {
      e1.printStackTrace();
      throw new IllegalArgumentException("Unexpected groupByCols conversion failure!");
    }
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }

  /**
   * Get the order of display of the columns in the spreadsheet view
   * 
   * @param ctxt
   * @param appName
   * @param tableId
   * @return empty list of none specified. Otherwise the elementKeys in the order of display.
   */
  public ArrayList<String> getColumnOrder(SQLiteDatabase db, String tableId) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    ArrayList<String> jsonDisplayChoices = ah.getArray(KeyValueStoreConstants.TABLE_COL_ORDER, String.class);
    if ( jsonDisplayChoices != null ) {
      return jsonDisplayChoices;
    }
    // TODO: construct list of persisted columns
    return new ArrayList<String>();
  }

  /**
   * Set the order of display of the columns in the spreadsheet view.
   * 
   * @param db
   * @param tableId
   * @param elementKeys
   */
  public void setColumnOrder( SQLiteDatabase db, String tableId, ArrayList<String> elementKeys) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_COL_ORDER;
    e.type = ElementDataType.array.name();
    try {
      e.value = ODKFileUtils.mapper.writeValueAsString(elementKeys);
    } catch (JsonProcessingException e1) {
      e1.printStackTrace();
      throw new IllegalArgumentException("Unexpected columnOrder conversion failure!");
    }
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }
}
