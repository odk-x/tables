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
import java.util.HashMap;
import java.util.Map;

import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.utilities.KeyValueStoreHelper;
import org.opendatakit.common.android.utilities.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.LocalKeyValueStoreConstants;
import org.opendatakit.common.android.utilities.NameUtil;
import org.opendatakit.common.android.utilities.ODKDataUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.StaticStateManipulator;
import org.opendatakit.common.android.utilities.StaticStateManipulator.IStaticFieldManipulator;
import org.opendatakit.database.service.KeyValueStoreEntry;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.data.TableViewType;

import android.os.RemoteException;

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
  
  public class TableColumns {
    public final OrderedColumns orderedDefns;
    public final String[] adminColumns;
    public final Map<String,String> localizedDisplayNames;
    
    TableColumns(OrderedColumns orderedDefns, String[] adminColumns, Map<String,String> localizedDisplayNames) {
      this.orderedDefns = orderedDefns;
      this.adminColumns = adminColumns;
      this.localizedDisplayNames = localizedDisplayNames;
    }
  }

  private static TableUtil tableUtil = new TableUtil();
  
  static {
    // register a state-reset manipulator for 'tableUtil' field.
    StaticStateManipulator.get().register(50, new IStaticFieldManipulator() {

      @Override
      public void reset() {
        tableUtil = new TableUtil();
      }
      
    });
  }

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
  
  public String getLocalizedDisplayName(String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
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

  public String getRawDisplayName(String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
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
   * @param appName
   * @param db
   * @param tableId
   * @param elementKey
   * @throws RemoteException 
   */
  public void setRawDisplayName( String appName, OdkDbHandle db, String tableId, String rawDisplayName) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_DISPLAY_NAME;
    e.type = ElementDataType.object.name();
    e.value = rawDisplayName;
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Get the default view type for this table.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @return the specified default view type or SPREADSHEET_VIEW if none defined.
   * @throws RemoteException 
   */
  public TableViewType getDefaultViewType( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
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
   * @param appName
   * @param db
   * @param tableId
   * @param elementKey
   * @throws RemoteException 
   */
  public void setDefaultViewType( String appName, OdkDbHandle db, String tableId, TableViewType viewType) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = TABLE_DEFAULT_VIEW_TYPE;
    e.type = ElementDataType.string.name();
    e.value = viewType.name();
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Get the filename for the detail view of this table.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @return null if none defined.
   * @throws RemoteException 
   */
  public String getDetailViewFilename( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String detailViewFilename = ah.getString(LocalKeyValueStoreConstants.Tables.KEY_DETAIL_VIEW_FILE_NAME);
    return detailViewFilename;
  }

  /**
   * Sets the filename for the detail view of this table.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @param detailViewFilename
   * @throws RemoteException 
   */
  public void setDetailViewFilename( String appName, OdkDbHandle db, String tableId, String detailViewFilename) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = LocalKeyValueStoreConstants.Tables.KEY_DETAIL_VIEW_FILE_NAME;
    e.type = ElementDataType.string.name();
    e.value = detailViewFilename;
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Get the filename for the detail view of this table.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @return null if none defined.
   * @throws RemoteException 
   */
  public String getListViewFilename( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String listViewFilename = ah.getString(LocalKeyValueStoreConstants.Tables.KEY_LIST_VIEW_FILE_NAME);
    return listViewFilename;
  }

  /**
   * Sets the filename for the detail view of this table.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @param listViewFilename
   * @throws RemoteException 
   */
  public void setListViewFilename( String appName, OdkDbHandle db, String tableId, String listViewFilename) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = LocalKeyValueStoreConstants.Tables.KEY_LIST_VIEW_FILE_NAME;
    e.type = ElementDataType.string.name();
    e.value = listViewFilename;
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Get the filename for the detail view of this table.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @return null if none defined.
   * @throws RemoteException 
   */
  public String getMapListViewFilename( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String listViewFilename = ah.getString(LocalKeyValueStoreConstants.Tables.KEY_MAP_LIST_VIEW_FILE_NAME);
    return listViewFilename;
  }

  /**
   * Sets the filename for the detail view of this table.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @param listViewFilename
   * @throws RemoteException 
   */
  public void setMapListViewFilename( String appName, OdkDbHandle db, String tableId, String listViewFilename) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = LocalKeyValueStoreConstants.Tables.KEY_MAP_LIST_VIEW_FILE_NAME;
    e.type = ElementDataType.string.name();
    e.value = listViewFilename;
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Get the elementKey of the sort-by column
   * 
   * @param appName
   * @param db
   * @param tableId
   * @return null if none defined.
   * @throws RemoteException 
   */
  public String getSortColumn( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String sortColumn = ah.getString(KeyValueStoreConstants.TABLE_SORT_COL);
    return sortColumn;
  }

  /**
   * Sets the table's sort column.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @param elementKey
   * @throws RemoteException 
   */
  public void setSortColumn( String appName, OdkDbHandle db, String tableId, String elementKey) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_SORT_COL;
    e.type = ElementDataType.string.name();
    e.value = elementKey;
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Return the sort order of the display (ASC or DESC)
   *
   * @param appName
   * @param db
   * @param tableId
   * @return ASC if none specified.
   * @throws RemoteException 
   */
  public String getSortOrder( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
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
   * @param appName
   * @param db
   * @param tableId
   * @param sortOrder
   * @throws RemoteException 
   */
  public void setSortOrder( String appName, OdkDbHandle db, String tableId, String sortOrder) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_SORT_ORDER;
    e.type = ElementDataType.string.name();
    e.value = sortOrder;
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Return the element key of the indexed (frozen) column.
   *
   * @param appName
   * @param db
   * @param tableId
   * @return null if none
   * @throws RemoteException 
   */
  public String getIndexColumn( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
    AspectHelper ah = kvsh.getAspectHelper(KeyValueStoreConstants.ASPECT_DEFAULT);
    String indexColumn = ah.getString(KeyValueStoreConstants.TABLE_INDEX_COL);
    return indexColumn;
  }

  /**
   * Set the elementKey of the indexed (frozen) column.
   * 
   * @param appName
   * @param db
   * @param tableId
   * @param elementKey
   * @throws RemoteException 
   */
  public void setIndexColumn( String appName, OdkDbHandle db, String tableId, String elementKey) throws RemoteException {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_TABLE;
    e.aspect = KeyValueStoreConstants.ASPECT_DEFAULT;
    e.key = KeyValueStoreConstants.TABLE_INDEX_COL;
    e.type = ElementDataType.string.name();
    e.value = elementKey;
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Get the group-by columns, in order
   * 
   * @param appName
   * @param db
   * @param tableId
   * @return empty list if none
   * @throws RemoteException 
   */
  public ArrayList<String> getGroupByColumns( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
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
   * @param appName
   * @param db
   * @param tableId
   * @param elementKeys
   * @throws RemoteException 
   */
  public void setGroupByColumns( String appName, OdkDbHandle db, String tableId, ArrayList<String> elementKeys) throws RemoteException {
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
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }

  /**
   * Get the order of display of the columns in the spreadsheet view
   * 
   * @param appName
   * @param db
   * @param tableId
   * @return empty list of none specified. Otherwise the elementKeys in the order of display.
   * @throws RemoteException 
   */
  public ArrayList<String> getColumnOrder( String appName, OdkDbHandle db, String tableId) throws RemoteException {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, tableId, KeyValueStoreConstants.PARTITION_TABLE);
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
   * @param appName
   * @param db
   * @param tableId
   * @param elementKeys
   * @throws RemoteException 
   */
  public void setColumnOrder( String appName, OdkDbHandle db, String tableId, ArrayList<String> elementKeys) throws RemoteException {
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
    Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, e);
  }
    
  public TableColumns getTableColumns( String appName, OdkDbHandle db, String tableId ) throws RemoteException {
    String[] adminColumns = Tables.getInstance().getDatabase().getAdminColumns();
    HashMap<String,String> colDisplayNames = new HashMap<String,String>();
    OrderedColumns orderedDefns = Tables.getInstance().getDatabase()
        .getUserDefinedColumns(appName, db, tableId);
    for (ColumnDefinition cd : orderedDefns.getColumnDefinitions()) {
      if (cd.isUnitOfRetention()) {
        String localizedDisplayName;
        localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(Tables.getInstance(),
            appName, db, tableId, cd.getElementKey());

        colDisplayNames.put(cd.getElementKey(), localizedDisplayName);
      }
    }
    return new TableColumns(orderedDefns, adminColumns, colDisplayNames);
  }
}
