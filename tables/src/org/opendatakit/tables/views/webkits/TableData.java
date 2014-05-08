package org.opendatakit.tables.views.webkits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;

import android.app.Activity;
import android.util.Log;

/**
 * The model for the object that is handed to the javascript.
 * @author sudar.sam@gmail.com
 *
 */
public class TableData {

     private static final String TAG = "TableData";

     public TableDataIf getJavascriptInterfaceWithWeakReference() {
        return new TableDataIf(this);
     }

     private final UserTable mTable;

     /**
      * A simple cache of color rules so they're not recreated unnecessarily
      * each time. Maps the column display name to {@link ColorRuleGroup} for
      * that column.
      */
     private Map<String, ColorRuleGroup> mElementKeyToColorRuleGroup;
     protected Activity mActivity;

     public TableData(Activity activity, UserTable table) {
        Log.d(TAG, "calling TableData constructor with Table");
        this.mActivity = activity;
        this.mTable = table;
        initMaps();
     }

     public TableData(UserTable table) {
        Log.d(TAG, "calling TableData constructor with UserTable");
        this.mTable = table;
        initMaps();
     }

     public boolean isGroupedBy() {
       return mTable.isGroupedBy();
     }

     public String getWhereClause() {
       return mTable.getWhereClause();
     }

     public String[] getSelectionArgs() {
       return mTable.getSelectionArgs();
     }

     public String[] getGroupByArgs() {
       return mTable.getGroupByArgs();
     }

     public String getHavingClause() {
       return mTable.getHavingClause();
     }

     public String getOrderByElementKey() {
       return mTable.getOrderByElementKey();
     }

     public String getOrderByDirection() {
       return mTable.getOrderByDirection();
     }

     // Initializes the colMap and groupByColumns that provide methods quick
     // access to the current table's state.
     private void initMaps() {
        mElementKeyToColorRuleGroup =
            new HashMap<String, ColorRuleGroup>();
     }

     // Returns the number of rows in the table being viewed.
     public int getCount() {
        return this.mTable.getNumberOfRows();
     }

     /**
      * @see {@link TableDataIf#getColumnData(String)}
      */
     public String getColumnData(String elementPath) {
       // Return all the rows.
       return getColumnData(elementPath, getCount());
     }

     /**
      * Return a strinfigied JSON array of the data in the columns. Returns
      * null and logs an error if the column is not found.
      * @param elementPath
      * @param requestedRows
      * @return returns a String in JSONArray format containing all the row
      * data for the given column name format: [row1, row2, row3, row4]
      */
     public String getColumnData(String elementPath, int requestedRows) {
       String elementKey =
           this.mTable.getTableProperties().getElementKeyFromElementPath(
               elementPath);
       if (elementKey == null) {
         Log.e(TAG, "column not found with element path: " + elementPath);
         return null;
       }
       ArrayList<String> rowValues = new ArrayList<String>();
       for (int i = 0; i < requestedRows; i++) {
         Row row = this.mTable.getRowAtIndex(i);
         rowValues.add(row.getDataOrMetadataByElementKey(elementKey));
       }
       return new JSONArray(rowValues).toString();
     }

     public String getColumnDataForElementKey(String elementKey, int requestedRows) {
       ArrayList<String> rowValues = new ArrayList<String>();
       for (int i = 0; i < requestedRows; i++) {
         Row row = this.mTable.getRowAtIndex(i);
         rowValues.add(row.getDataOrMetadataByElementKey(elementKey));
       }
       return new JSONArray(rowValues).toString();
     }

     /**
      * Return a map of element key to the {@link ColumnType#label()}.
      */
     public String getColumns() {
        Map<String, String> colInfo = new HashMap<String, String>();
        for (String elementKey :
             mTable.getTableProperties().getAllColumns().keySet()) {
           String label = getColumnTypeLabelForElementKey(elementKey);
           colInfo.put(elementKey, label);
        }
        return new JSONObject(colInfo).toString();
     }

     /**
      * Get the element {@link ColumnType#label()} for the column with the
      * given elementKey.
      * @param elementKey
      * @return
      */
     private String getColumnTypeLabelForElementKey(String elementKey) {
       TableProperties tp = mTable.getTableProperties();
       ColumnProperties cp = tp.getColumnByElementKey(elementKey);
       String label = cp.getColumnType().label();
       return label;
     }

     /**
      * @see {@link TableDataIf#getForegroundColor(String, String)}
      */
     public String getForegroundColor(String elementPath, String value) {
        Integer foregroundColor =
            mTable.getForegroundColorOfData(elementPath, value);
        if (foregroundColor == null) {
           foregroundColor = -16777216; // this crazy value was found here
        }
        // I think this formatting needs to take place for javascript
        return String.format("#%06X", (0xFFFFFF & foregroundColor));
     }

     /**
      * @see {@link TableDataIf#getData(int, String)}.
      */
     public String getData(int rowNum, String elementPath) {
       Row row = mTable.getRowAtIndex(rowNum);
       if ( row == null ) {
         Log.e(TAG, "row " + rowNum + " does not exist! Returning null");
         return null;
       }

       String elementKey =
           mTable.getTableProperties().getElementKeyFromElementPath(
               elementPath);
       if (elementKey == null) {
         Log.e(TAG, "column with elementPath: " + elementPath + " does not" +
              " exist.");
         return null;
       }

       String result = row.getDataOrMetadataByElementKey(elementKey);
       return result;
     }

     public String getTableId() {
       return mTable.getTableProperties().getTableId();
     }

     public String getRowId(int index) {
       return mTable.getRowAtIndex(index).getRowId();
     }

}
