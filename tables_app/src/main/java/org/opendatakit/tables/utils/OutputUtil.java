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

import android.content.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.CharEncoding;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.exception.ServicesAvailabilityException;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.application.Tables;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Methods for dealing with things necessary for debugging in chrome. This
 * requires outputting json structures representing the state of the database.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class OutputUtil {

  private static final String TAG = OutputUtil.class.getSimpleName();

  /** The filename of the control object that will be written out. */
  public static final String CONTROL_FILE_NAME = "control.json";
  /** The suffix of the name for each data object that will be written. */
  public static final String DATA_FILE_SUFFIX = "_data.json";

  public static final String CTRL_KEY_TABLE_ID_TO_DISPLAY_NAME = "tableIdToDisplayName";
  public static final String CTRL_KEY_TABLE_INFO = "tables";
  /** The key for the default detail view file in the table. */
  public static final String CTRL_KEY_DEFAULT_DETAIL_FILE = "defaultDetailFile";
  /** The key for the default list view file in the table. */
  public static final String CTRL_KEY_DEFAULT_LIST_FILE = "defaultListFile";

  // These are keys we'll be outputting for use in debugging when we write
  // this object to a file.
  public static final String DATA_KEY_IN_COLLECTION_MODE = "inCollectionMode";
  public static final String DATA_KEY_COUNT = "count";
  public static final String DATA_KEY_COLUMNS = "columns";
  public static final String DATA_KEY_IS_GROUPED_BY = "isGroupedBy";
  public static final String DATA_KEY_DATA = "data";
  public static final String DATA_KEY_COLUMN_DATA = "columnData";
  public static final String DATA_KEY_TABLE_ID = "tableId";
  public static final String DATA_KEY_ROW_IDS = "rowIds";

  // Keys for the table object contained within control objects.
  public static final String CTRL_TABLE_KEY_ELEMENT_PATH_TO_KEY = "pathToKey";
  public static final String CTRL_TABLE_KEY_ELEMENT_KEY_TO_DISPLAY_NAME = "pathToName";

  public static final int NUM_ROWS_IN_DATA_OBJECT = 10;

  /**
   * Gets a string containing information necessary for the control object's
   * methods to be meaningful. E.g. there is a method to retrieve all the table
   * ids. This object will contain that information.
   * <p>
   * The object is as follows: <br>
   * { {@link #CTRL_TABLE_KEY_ELEMENT_KEY_TO_DISPLAY_NAME}: {tableIdOne:
   * displayName, ...}, {@link #CTRL_KEY_TABLE_INFO: tableIdOne: tableObjectOne,
   * ...}
   *
   * @return
   * @throws JsonProcessingException 
   * @throws ServicesAvailabilityException
   */
  private static String getStringForControlObject(Context context, String appName) throws JsonProcessingException, ServicesAvailabilityException {
    Map<String, Object> controlMap = new HashMap<String, Object>();
    Map<String, String> tableIdToDisplayName = new HashMap<String, String>();
    Map<String, Map<String, Object>> tableIdToControlTable = new HashMap<String, Map<String, Object>>();

    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName);
      List<String> tableIds = Tables.getInstance().getDatabase().getAllTableIds(appName, db);
      for (String tableId : tableIds) {

        String localizedDisplayName;
        localizedDisplayName = TableUtil.get().getLocalizedDisplayName(Tables.getInstance(), appName, db, tableId);
        tableIdToDisplayName.put(tableId, localizedDisplayName);
        Map<String, Object> controlTable = getMapForControlTable(appName, db, tableId);
        tableIdToControlTable.put(tableId, controlTable);
      }
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
    controlMap.put(CTRL_KEY_TABLE_ID_TO_DISPLAY_NAME, tableIdToDisplayName);
    controlMap.put(CTRL_KEY_TABLE_INFO, tableIdToControlTable);
    String result = ODKFileUtils.mapper.writeValueAsString(controlMap);
    return result;
  }

  /**
   * The control.js object serves as a hub for some information about a table.
   * E.g. the control object needs to be able to return the elementKey based on
   * an elementPath for the table, as well as get display names for a column.
   * <p>
   * The string returned by this object should likely not be useful outside of
   * the context of a control object. {
   * {@link #CTRL_TABLE_KEY_ELEMENT_PATH_TO_KEY}: {elementPath: elementKey,
   * ...}, {@link #CTRL_TABLE_KEY_ELEMENT_KEY_TO_DISPLAY_NAME}: {elementPath:
   * displayName, ...},
   *
   * }
   * 
   * @param db
   * @param appName
   * @param tableId
   * @return
   * @throws ServicesAvailabilityException
   */
  public static Map<String, Object> getMapForControlTable(String appName, OdkDbHandle db,
      String tableId) throws ServicesAvailabilityException {
    Map<String, Object> controlTable = new HashMap<String, Object>();
    Map<String, String> pathToKey = new HashMap<String, String>();
    OrderedColumns orderedDefns;

    String defaultDetailFileName = null;
    String defaultListFileName = null;
    Map<String, String> keyToDisplayName = new HashMap<String, String>();

    orderedDefns = Tables.getInstance().getDatabase().getUserDefinedColumns(appName, db, tableId);
    defaultDetailFileName = TableUtil.get().getDetailViewFilename(Tables.getInstance(), appName, db, tableId);
    defaultListFileName = TableUtil.get().getListViewFilename(Tables.getInstance(), appName, db, tableId);

    for (ColumnDefinition cd : orderedDefns.getColumnDefinitions()) {
      String elementName = cd.getElementName();
      if (elementName != null) {
        pathToKey.put(cd.getElementName(), cd.getElementKey());

        String localizedDisplayName;
        localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(Tables.getInstance(), appName,
            db, tableId,
            cd.getElementKey());

        keyToDisplayName.put(cd.getElementKey(), localizedDisplayName);
      }
    }

    controlTable.put(CTRL_TABLE_KEY_ELEMENT_PATH_TO_KEY, pathToKey);
    controlTable.put(CTRL_TABLE_KEY_ELEMENT_KEY_TO_DISPLAY_NAME, keyToDisplayName);
    controlTable.put(CTRL_KEY_DEFAULT_DETAIL_FILE, defaultDetailFileName);
    controlTable.put(CTRL_KEY_DEFAULT_LIST_FILE, defaultListFileName);
    return controlTable;
  }

  /**
   * Writes the control string to a json file in the debug folder.
   *
   * @param context
   * @param appName
   * @throws ServicesAvailabilityException
   * @throws JsonProcessingException 
   */
  public static void writeControlObject(Context context, String appName) throws JsonProcessingException, ServicesAvailabilityException {
    String controlString = getStringForControlObject(context, appName);
    String fileName = ODKFileUtils.getTablesDebugObjectFolder(appName) + File.separator
        + CONTROL_FILE_NAME;
    PrintWriter writer;
    try {
      writer = new PrintWriter(fileName, CharEncoding.UTF_8);
      WebLogger.getLogger(appName).d(TAG, "writing control to: " + fileName);
      writer.print(controlString);
      writer.flush();
      writer.close();
    } catch (FileNotFoundException e) {
      WebLogger.getLogger(appName).printStackTrace(e);
    } catch (UnsupportedEncodingException e) {
      WebLogger.getLogger(appName).printStackTrace(e);
    }
  }

}
