package org.opendatakit.tables.views.webkits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.data.ColorRuleGroup.ColorGuide;

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

     // Initializes the colMap and primeColumns that provide methods quick
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
       ArrayList<String> rowValues = new ArrayList<String>();
       Integer columnIndex = mTable.getColumnIndexOfElementKey(elementKey);
       if (columnIndex == null) {
         Log.e(TAG, "column not found with element path: " + elementPath +
             " and key: " + elementKey);
         return null;
       }
       for (int i = 0; i < requestedRows; i++) {
          rowValues.add(this.mTable.getData(i, columnIndex));
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
        TableProperties tp = mTable.getTableProperties();
        String elementKey = 
            tp.getElementKeyFromElementPath(elementPath);
        ColorRuleGroup colRul = this.mElementKeyToColorRuleGroup
              .get(elementPath);
        if (colRul == null) {
           // If it's not already there, cache it for future use.
           colRul = ColorRuleGroup.getColumnColorRuleGroup(tp, elementKey);
           this.mElementKeyToColorRuleGroup.put(elementPath, colRul);
        }
        // Rather than hand off the whole row data, we'll just dummy up the
        // info requested, as this will be easier for the html programmer
        // to use than to have to give in the whole row.
        Map<String, Integer> indexOfDataMap = new HashMap<String, Integer>();
        indexOfDataMap.put(elementKey, 0);
        String[] elementKeyForIndex = new String[] { elementKey };
        Map<String, Integer> indexOfMetadataMap = 
            new HashMap<String, Integer>();
        indexOfMetadataMap.put(elementKey, 0);
        // We need to construct a dummy UserTable for the ColorRule to
        // interpret.
        String[] header = new String[] { elementPath };
        String[] rowId = new String[] { "dummyRowId" };
        String[][] data = new String[1][1];
        String[][] metadata = new String[1][1];
        data[0][0] = value;
        metadata[0][0] = "dummyMetadata";
        UserTable table = new UserTable(tp, rowId, header, data,
              elementKeyForIndex, indexOfDataMap, metadata,
              indexOfMetadataMap, null);
        ColorGuide guide = colRul.getColorGuide(table.getRowAtIndex(0));
        int foregroundColor;
        if (guide.didMatch()) {
           foregroundColor = guide.getForeground();
        } else {
           foregroundColor = -16777216; // this crazy value was found here
        }
        // I think this formatting needs to take place for javascript
        return String.format("#%06X", (0xFFFFFF & foregroundColor));
     }

     /**
      * @see {@link TableDataIf#getData(int, String)}.
      */
     public String getData(int rowNum, String elementPath) {
       String elementKey =
           mTable.getTableProperties().getElementKeyFromElementPath(
               elementPath);
       Integer columnIndex = mTable.getColumnIndexOfElementKey(elementKey);
       if (columnIndex == null) {
         Log.e(TAG, "column with elementKey: " + elementKey + " does not" +
              " exist.");
         return null;
       }
       String result = mTable.getDataByElementKey(rowNum, elementKey);
       return result;
     }
     
     public String getTableId() {
       return mTable.getTableProperties().getTableId();
     }
     
     public String getRowId(int index) {
       return mTable.getRowAtIndex(index).getRowId();
     }

}
