package org.opendatakit.tables.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opendatakit.tables.activities.DetailDisplayActivity;
import org.opendatakit.tables.activities.ListDisplayActivity;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.views.webkits.TableData;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
   * displayName, ...}, {@link #CTRL_KEY_TABLE_INFO: tableIdOne:
   * tableObjectOne, ...} }
   *
   * @return
   */
  public static String getStringForControlObject(Context context, String appName) {
    Map<String, Object> controlMap = new HashMap<String, Object>();
    DbHelper dbHelper = DbHelper.getDbHelper(context, appName);
    TableProperties[] allTableProperties = TableProperties.getTablePropertiesForDataTables(
        dbHelper, KeyValueStore.Type.ACTIVE);
    Map<String, String> tableIdToDisplayName = new HashMap<String, String>();
    Map<String, Map<String, Object>> tableIdToControlTable = new HashMap<String, Map<String, Object>>();
    for (TableProperties tableProperties : allTableProperties) {
      tableIdToDisplayName.put(tableProperties.getTableId(), tableProperties.getDisplayName());
      Map<String, Object> controlTable = getMapForControlTable(context, tableProperties);
      tableIdToControlTable.put(tableProperties.getTableId(), controlTable);
    }
    Gson gson = new Gson();
    controlMap.put(CTRL_KEY_TABLE_ID_TO_DISPLAY_NAME, tableIdToDisplayName);
    controlMap.put(CTRL_KEY_TABLE_INFO, tableIdToControlTable);
    String result = gson.toJson(controlMap);
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
   * @param context
   * @param tableProperties
   * @return
   */
  public static Map<String, Object> getMapForControlTable(Context context,
      TableProperties tableProperties) {
    Map<String, Object> controlTable = new HashMap<String, Object>();
    Map<String, String> pathToKey = new HashMap<String, String>();
    Map<String, String> keyToDisplayName = new HashMap<String, String>();
    for (ColumnProperties columnProperties : tableProperties.getAllColumns().values()) {
      pathToKey.put(columnProperties.getElementName(), columnProperties.getElementKey());
      keyToDisplayName.put(columnProperties.getElementKey(), columnProperties.getDisplayName());
    }
    String defaultDetailFileName = DetailDisplayActivity.getDefaultDetailFileName(tableProperties);
    String defaultListFileName = ListDisplayActivity.getDefaultListFileName(tableProperties);
    controlTable.put(CTRL_TABLE_KEY_ELEMENT_PATH_TO_KEY, pathToKey);
    controlTable.put(CTRL_TABLE_KEY_ELEMENT_KEY_TO_DISPLAY_NAME, keyToDisplayName);
    controlTable.put(CTRL_KEY_DEFAULT_DETAIL_FILE, defaultDetailFileName);
    controlTable.put(CTRL_KEY_DEFAULT_LIST_FILE, defaultListFileName);
    return controlTable;
  }

  /**
   * Gets a string containing information necessary for the data object for this
   * particular table. The object is something like the following:<br>
   * { {@link #DATA_KEY_IN_COLLECTION_MODE}: boolean, {@link #DATA_KEY_COUNT}:
   * int, {@link #DATA_KEY_COLLECTION_SIZE}: Array, (an array of ints)
   * {@link #DATA_KEY_IS_INDEXED}: boolean, {@link #DATA_KEY_DATA: Array, (2d,
   * array of rows) {@link #DATA_KEY_COLUMNS}: {elementKey: string, ...},
   * {@link #DATA_KEY_ELEMENT_KEY_TO_PATH: elementPath: elementPath, ...},
   *
   * @param context
   * @param appName
   * @param tableId
   * @return
   */
  public static String getStringForDataObject(Context context, String appName, String tableId,
      int numberOfRows) {
    DbHelper dbHelper = DbHelper.getDbHelper(context, appName);
    TableProperties tableProperties = TableProperties.getTablePropertiesForTable(dbHelper, tableId,
        KeyValueStore.Type.ACTIVE);
    DbTable dbTable = DbTable.getDbTable(dbHelper, tableProperties);
    UserTable userTable = dbTable.rawSqlQuery("", null);

    // TODO: This is broken w.r.t. elementKey != elementPath
    // TODO: HACKED HACKED HACKED HACKED
    // Because the code is so freaked up we don't have an easy way to get the
    // information out of the UserTable without going through the TableData
    // object. So we need to create a dummy CustomTableView to get it to work.
    TableData tableData = new TableData(userTable);
    // We need to also store the element key to the index of the data so that
    // we know how to access it out of the array representing each row.
    Map<String, Integer> elementKeyToIndex = new HashMap<String, Integer>();
    Set<String> elementKeys = tableProperties.getAllColumns().keySet();
    for (String elementKey : elementKeys) {
      elementKeyToIndex.put(elementKey, userTable.getColumnIndexOfElementKey(elementKey));
    }
    // We don't want to try and write more rows than we have.
    int numRowsToWrite = Math.min(numberOfRows, userTable.getNumberOfRows());
    // First write the array of row ids.
    String[] rowIds = new String[numRowsToWrite];
    for (int i = 0; i < rowIds.length; i++) {
      rowIds[i] = userTable.getRowAtIndex(i).getRowId();
    }
    // Here we're using this object b/c these appear to be the columns
    // available to the client--are metadata columns exposed to them? It's
    // not obvious to me here.
    Set<String> columnKeys = elementKeyToIndex.keySet();
    Map[] partialData = new Map[numRowsToWrite];
    // Now construct up the partial data object.
    for (int i = 0; i < numRowsToWrite; i++) {
      Map<String, Object> rowOut = new HashMap<String, Object>();
      for (String elementKey : columnKeys) {
        rowOut.put(elementKey, tableData.getData(i, elementKey));
      }
      partialData[i] = rowOut;
    }
    // And now construct the object storing the columns data.
    JsonParser jsonParser = new JsonParser();
    Map<String, Object> elementKeyToColumnData = new HashMap<String, Object>();
    for (String elementKey : columnKeys) {
      // The tableData object returns a string, so we'll have to parse it back
      // into json.
      String strColumnData = tableData.getColumnDataForElementKey(elementKey, numRowsToWrite);
      if ( strColumnData == null ) continue;
      JsonArray columnData = (JsonArray) jsonParser.parse(strColumnData);
      // Now that it's json, we want to convert it to an array. Otherwise it
      // serializes to an object with a single key "elements". Oh gson.
      String[] columnDataArray = new String[columnData.size()];
      for (int i = 0; i < columnDataArray.length; i++) {
        JsonElement e = columnData.get(i);
        if ( e == JsonNull.INSTANCE ) {
          columnDataArray[i] = null;
        } else {
          columnDataArray[i] = e.getAsString();
        }
      }
      elementKeyToColumnData.put(elementKey, columnDataArray);
    }
    Gson gson = new Gson();
    // We need to parse some of the String objects returned by TableData into
    // json so that they're output as objects rather than strings.
    String columnString = tableData.getColumns();
    JsonObject columnJson = (JsonObject) jsonParser.parse(columnString);
    // Here, as with JsonArray, we need to convert this to a map or else we'll
    // serialize as the object to a "members" key.
    Map<String, Object> columnJsonMap = new HashMap<String, Object>();
    for (Map.Entry<String, JsonElement> entry : columnJson.entrySet()) {
      JsonElement e = entry.getValue();
      if ( e == JsonNull.INSTANCE ) {
        columnJsonMap.put(entry.getKey(), null);
      } else {
        columnJsonMap.put(entry.getKey(), e.getAsString());
      }
    }
    Map<String, Object> outputObject = new HashMap<String, Object>();
    outputObject.put(DATA_KEY_IS_GROUPED_BY, tableData.isGroupedBy());
    // We don't want the real count, as that could interfere with for loops in
    // the code. We in fact want the number of rows that are written, as that
    // will be the number of rows available to the javascript.
    outputObject.put(DATA_KEY_TABLE_ID, userTable.getTableProperties().getTableId());
    outputObject.put(DATA_KEY_COUNT, numRowsToWrite);
    outputObject.put(DATA_KEY_COLUMNS, columnJsonMap);
    outputObject.put(DATA_KEY_COLUMN_DATA, elementKeyToColumnData);
    outputObject.put(DATA_KEY_DATA, partialData);
    outputObject.put(DATA_KEY_ROW_IDS, rowIds);
    String outputString = gson.toJson(outputObject);
    return outputString;
  }

  /**
   * Get the table data object with {@link #NUM_ROWS_IN_DATA_OBJECT} rows.
   *
   * @see #getStringForDataObject(Context, String, String, int)
   * @param context
   * @param appName
   * @param tableId
   * @return
   */
  public static String getStringForDataObject(Context context, String appName, String tableId) {
    return getStringForDataObject(context, appName, tableId, NUM_ROWS_IN_DATA_OBJECT);
  }

  /**
   * Writes the control string to a json file in the debug folder.
   *
   * @param context
   * @param appName
   */
  public static void writeControlObject(Context context, String appName) {
    String controlString = getStringForControlObject(context, appName);
    String fileName = TableFileUtils.getTablesDebugObjectFolder(appName) + File.separator
        + CONTROL_FILE_NAME;
    PrintWriter writer;
    try {
      writer = new PrintWriter(fileName, "UTF-8");
      Log.d(TAG, "writing control to: " + fileName);
      writer.print(controlString);
      writer.flush();
      writer.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Writes the data objects for all the data tables in the database.
   *
   * @param context
   * @param appName
   * @param numberOfRows
   */
  public static void writeAllDataObjects(Context context, String appName, int numberOfRows) {
    DbHelper dbHelper = DbHelper.getDbHelper(context, appName);
    TableProperties[] allDataTables = TableProperties.getTablePropertiesForDataTables(dbHelper,
        KeyValueStore.Type.ACTIVE);
    for (TableProperties tableProperties : allDataTables) {
      writeDataObject(context, appName, tableProperties.getTableId(), numberOfRows);
    }
  }

  /**
   * Convenience method. Calls
   * {@link #writeAllDataObjects(Context, String, int)} with
   * {@link #NUM_ROWS_IN_DATA_OBJECT}.
   *
   * @param context
   * @param appName
   */
  public static void writeAllDataObjects(Context context, String appName) {
    writeAllDataObjects(context, appName, NUM_ROWS_IN_DATA_OBJECT);
  }

  /**
   * Write the data object with the given number of rows to the debug folder.
   * The file is tableId_DATA_FILE_SUFFIX.
   *
   * @param context
   * @param appName
   * @param tableId
   * @param numberOfRows
   */
  public static void writeDataObject(Context context, String appName, String tableId,
      int numberOfRows) {
    String dataString = getStringForDataObject(context, appName, tableId, numberOfRows);
    if ( dataString == null ) return;
    String fileName = TableFileUtils.getTablesDebugObjectFolder(appName) + File.separator + tableId
        + DATA_FILE_SUFFIX;
    PrintWriter writer;
    try {
      writer = new PrintWriter(fileName, "UTF-8");
      Log.d(TAG, "writing data object to: " + fileName);
      writer.print(dataString);
      writer.flush();
      writer.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
