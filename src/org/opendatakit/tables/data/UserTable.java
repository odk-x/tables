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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the data in a table. This can be conceptualized as a 
 * list of rows. Each row comprises the user-defined columns, or data, as well
 * as the ODKTables-specified metadata. 
 * <p>
 * This should be considered an immutable class, with the exception of the 
 * footer. The footer is only important to the user when viewing a table in 
 * certain conditions, and many other uses where the contents of a table need
 * to be accessed do not require the footer. For this reason it alone is
 * mutable.
 * 
 * @author unknown
 * @author sudar.sam@gmail.com
 *
 */
public class UserTable {
    
    //private final String[] rowIds;
    private final String[] header;
    //private final String[][] data;
    //private final String[][] userData;
    private String[] footer;
    private final ArrayList<Row> mRows;
    /**
     * Maps the element key of user-defined columns to the corresponding index 
     * in the Row objects.
     */
    private final Map<String, Integer> mDataKeyToIndex;
    /**
     * Maps the element key of ODKTables-specified metadata columns to the
     * corresponding indices in the Row objects.
     */
    private final Map<String, Integer> mMetadataKeyToIndex;
    
    public UserTable(String[] rowIds, String[] header, 
        String[][] userDefinedData, Map<String, Integer> dataElementKeyToIndex,
        String[][] odkTablesMetadata, 
        Map<String, Integer> metadataElementKeyToIndex, String[] footer) {
        //this.rowIds = rowIds;
        this.header = header;
        //this.data = data;
        int columnCount = userDefinedData.length > 0 ? userDefinedData[0].length : 0;
        String[][] userData = new String[userDefinedData.length][columnCount];
        mRows = new ArrayList<Row>(userDefinedData.length);
        for (int i = 0; i < userDefinedData.length; i++) {
            for (int j = 0; j < columnCount; j++) {
                userData[i][j] = userDefinedData[i][j];
            }
            Row nextRow = new Row(rowIds[i], userData[i], null);
            mRows.add(nextRow);
        }
        this.footer = footer;
        // Initialize the column maps.
        mDataKeyToIndex = new HashMap<String, Integer>();
        mMetadataKeyToIndex = new HashMap<String, Integer>();
    }
    
    public String getRowId(int rowNum) {
//        return rowIds[rowNum];
      return this.mRows.get(rowNum).mRowId;
    }
    
    public String getHeader(int colNum) {
        return header[colNum];
    }
    
    public String getData(int rowNum, int colNum) {
      return mRows.get(rowNum).getDataAtIndex(colNum);
//      return data[rowNum][colNum];
    }
    
    public String[] getRowData(int rowNum) {
      return mRows.get(rowNum).getAllData();
//      return data[rowNum];
    }
    
    public String getData(int cellNum) {
        int rowNum = cellNum / getWidth();
        int colNum = cellNum % getWidth();
        return mRows.get(rowNum).getDataAtIndex(colNum);
//        return getData(rowNum, colNum);
    }
    
    public String getUserData(int rowNum, int colNum) {
      return mRows.get(rowNum).getDataAtIndex(colNum);
//        return userData[rowNum][colNum];
    }
    
    public String getFooter(int colNum) {
        return footer[colNum];
    }
    
    public void setFooter(String[] footer) {
      this.footer = footer;
    }
    
    /**
     * Return a map containing the mapping of the element keys for the user-
     * defined columns to their index in array returned by 
     * {@link Row#getAllData()}.
     * @return
     */
    public Map<String, Integer> getMapOfUserDataToIndex() {
      Map<String, Integer> copy = new HashMap<String, Integer>();
      for (Map.Entry<String, Integer> entry : mDataKeyToIndex.entrySet()) {
        copy.put(entry.getKey(), entry.getValue());
      }
      return copy;
    }
    
    /**
     * Return a map containing the mapping of the element keys for the 
     * ODKTables-specified metadata columns to their index in the array
     * returned by {@link Row#getAllMetadata()}.
     * @return
     */
    public Map<String, Integer> getMapOfMetadataToIndex() {
      Map<String, Integer> copy = new HashMap<String, Integer>();
      for (Map.Entry<String, Integer> entry : mMetadataKeyToIndex.entrySet()) {
        copy.put(entry.getKey(), entry.getValue());
      }
      return copy;
    }
    
    public int getWidth() {
        return header.length;
    }
    
    /**
     * Get the number of metadata columns.
     * @return
     */
    public int getNumberOfMetadataColumns() {
      return mMetadataKeyToIndex.size();
    }
    
    public String[] getAllMetadataForRow(int rowNum) {
      return mRows.get(rowNum).getAllMetadata();
    }
    
    public int getHeight() {
//        return data.length;
      return this.mRows.size();
    }
    
//    public void setData(int rowNum, int colNum, String value) {
//        data[rowNum][colNum] = value;
//    }
//    
//    public void setData(int cellNum, String value) {
//        int rowNum = cellNum / getWidth();
//        int colNum = cellNum % getWidth();
//        setData(rowNum, colNum, value);
//    }
    
    /**
     * Scan the rowIds to get the row number. As the rowIds are not sorted,
     * this is a potentially expensive operation, scanning the entire array,
     * as well as the cost of checking String equality. Should be used only
     * when necessary.
     * <p>
     * Return -1 if the row Id is not found.
     * @param rowId
     * @return
     */
    public int getRowNumFromId(String rowId) {
//      for (int i = 0; i < rowIds.length; i++) {
//        if (rowIds[i].equals(rowId)) {
//          return i;
//        }
//      }
      for (int i = 0; i < this.mRows.size(); i++) {
        if (this.mRows.get(i).mRowId.equals(rowId)) {
          return i;
        }
      }
      return -1;
    }
    
    /**
     * This represents a single row of data in a table. 
     * @author sudar.sam@gmail.com
     *
     */
    /*
     * This class is final to try and reduce overhead. As final there is no
     * extended-class pointer. Not positive this is really a thing, need to 
     * investigate. Nothing harmed by finalizing, though.
     */
    public final class Row {
      
      /**
       * The id of the row.
       */
      private final String mRowId;
      
      /**
       * Holds the actual data in the row. To index into the array correctly,
       * must use the information contained in UserTable.
       */
      private final String[] mData;
      
      /**
       * Holds the metadata for the row. to index into the array correctly,
       * must use the information contained in UserTable.
       */
      private final String[] mMetadata;
      
      /**
       * Construct the row.
       * @param rowId
       * @param data the user-defined data of the row
       * @param metadata the ODKTables-specified metadata for the row.
       */
      public Row(String rowId, String[] data, String[] metadata) {
        this.mRowId = rowId;
        this.mData = data;
        this.mMetadata = metadata;
      }
      
      /**
       * Return the value of the row at the given index.
       * @param index
       * @return
       */
      public String getDataAtIndex(int index) {
        return mData[index];
      }
      
      /**
       * Get the array backing the entire row.
       * @return
       */
      public String[] getAllData() {
        return mData;
      }
      
      public String[] getAllMetadata() {
        return mMetadata;
      }
      
    }
}
