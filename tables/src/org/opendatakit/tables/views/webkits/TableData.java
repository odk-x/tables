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
package org.opendatakit.tables.views.webkits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.ElementType;
import org.opendatakit.common.android.data.ColorGuide;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.ElementTypeManipulator;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulatorFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * The model for the object that is handed to the javascript.
 *
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
   * The index of the marker that has been selected.
   */
  protected int mSelectedMapMarkerIndex;

  protected static final int INVALID_INDEX = -1;

  /**
   * A simple cache of color rules so they're not recreated unnecessarily each
   * time. Maps the column display name to {@link ColorRuleGroup} for that
   * column.
   */
  private Map<String, ColorRuleGroup> mElementKeyToColorRuleGroup = new HashMap<String, ColorRuleGroup>();
  private ColorRuleGroup mStatusColumnColorRuleGroup = null;
  private ColorRuleGroup mRowColorRuleGroup = null;

  public TableData(UserTable table) {
    this.mTable = table;
    this.mSelectedMapMarkerIndex = INVALID_INDEX;
    WebLogger.getLogger(mTable.getAppName()).d(TAG, "calling TableData constructor with UserTable");
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

  private void initMaps() {
  }

  private ArrayList<ColumnDefinition> getColumnDefinitions() {
    return mTable.getColumnDefinitions();
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
   * Return a strinfigied JSON array of the data in the columns. Returns null
   * and logs an error if the column is not found.
   *
   * @param elementPath
   * @param requestedRows
   * @return returns a String in JSONArray format containing all the row data
   *         for the given column name format: [row1, row2, row3, row4]
   */
  public String getColumnData(String elementPath, int requestedRows) {
    String elementKey = ColumnUtil.get().getElementKeyFromElementPath(elementPath);
    if (elementKey == null) {
      WebLogger.getLogger(mTable.getAppName()).e(TAG,
          "column not found with element path: " + elementPath);
      return null;
    }
    ArrayList<String> rowValues = new ArrayList<String>();
    for (int i = 0; i < requestedRows; i++) {
      int correctedIndex = getIndexIntoDataTable(i);
      Row row = this.mTable.getRowAtIndex(correctedIndex);
      rowValues.add(row.getRawDataOrMetadataByElementKey(elementKey));
    }
    return new JSONArray(rowValues).toString();
  }

  public String getColumnDataForElementKey(String elementKey, int requestedRows) {
    ArrayList<String> rowValues = new ArrayList<String>();
    for (int i = 0; i < requestedRows; i++) {
      int correctedIndex = getIndexIntoDataTable(i);
      Row row = this.mTable.getRowAtIndex(correctedIndex);
      rowValues.add(row.getRawDataOrMetadataByElementKey(elementKey));
    }
    return new JSONArray(rowValues).toString();
  }

  /**
   * Return a map of element key to the {@link ColumnType#label()}.
   */
  public String getColumns() {
    Map<String, String> colInfo = new HashMap<String, String>();
    ArrayList<ColumnDefinition> orderedDefn = getColumnDefinitions();
    for (ColumnDefinition cd : orderedDefn) {
      String label = getColumnTypeLabelForElementKey(cd.getElementKey());
      colInfo.put(cd.getElementKey(), label);
    }
    return new JSONObject(colInfo).toString();
  }

  /**
   * Get the element {@link ColumnType#label()} for the column with the given
   * elementKey.
   *
   * @param elementKey
   * @return
   */
  private String getColumnTypeLabelForElementKey(String elementKey) {
    ColumnDefinition cd = ColumnDefinition.find(getColumnDefinitions(), elementKey);
    ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance(this.mTable.getAppName());
    ITypeManipulatorFragment r = m.getDefaultRenderer(cd.getType());
    String label = r.getElementTypeDisplayLabel();
    return label;
  }

  public String getColumnForegroundColor(int rowNumber, String elementPath) {
    int correctedIndex = getIndexIntoDataTable(rowNumber);
    int foregroundColor = -16777216;

    String elementKey = ColumnUtil.get().getElementKeyFromElementPath(elementPath);
    if (elementKey == null) {
      return String.format("#%06X", (0xFFFFFF & foregroundColor));
    }
    ColorRuleGroup colRul = this.mElementKeyToColorRuleGroup.get(elementKey);
    if (colRul == null) {
      // If it's not already there, cache it for future use.
      colRul = ColorRuleGroup.getColumnColorRuleGroup(Tables.getInstance().getApplicationContext(),
          mTable.getAppName(), mTable.getTableId(), elementKey);
      this.mElementKeyToColorRuleGroup.put(elementKey, colRul);
    }

    Row row = mTable.getRowAtIndex(correctedIndex);
    ColorGuide guide = colRul.getColorGuide(getColumnDefinitions(), row);
    if (guide != null) {
      foregroundColor = guide.getForeground();
    }
    // I think this formatting needs to take place for javascript
    return String.format("#%06X", (0xFFFFFF & foregroundColor));
  }

  public String getStatusForegroundColor(int rowNumber) {
    int correctedIndex = getIndexIntoDataTable(rowNumber);
    int foregroundColor = -16777216;

    if (mStatusColumnColorRuleGroup == null) {
      mStatusColumnColorRuleGroup = ColorRuleGroup.getStatusColumnRuleGroup(Tables.getInstance()
          .getApplicationContext(), mTable.getAppName(), mTable.getTableId());
    }

    Row row = mTable.getRowAtIndex(correctedIndex);
    ColorGuide guide = mStatusColumnColorRuleGroup.getColorGuide(getColumnDefinitions(), row);
    if (guide != null) {
      foregroundColor = guide.getForeground();
    }
    // I think this formatting needs to take place for javascript
    return String.format("#%06X", (0xFFFFFF & foregroundColor));
  }

  public String getRowForegroundColor(int rowNumber) {
    int correctedIndex = getIndexIntoDataTable(rowNumber);
    int foregroundColor = -16777216;

    if (mRowColorRuleGroup == null) {
      mRowColorRuleGroup = ColorRuleGroup.getTableColorRuleGroup(Tables.getInstance()
          .getApplicationContext(), mTable.getAppName(), mTable.getTableId());
    }

    Row row = mTable.getRowAtIndex(correctedIndex);
    ColorGuide guide = mRowColorRuleGroup.getColorGuide(getColumnDefinitions(), row);
    if (guide != null) {
      foregroundColor = guide.getForeground();
    }
    // I think this formatting needs to take place for javascript
    return String.format("#%06X", (0xFFFFFF & foregroundColor));
  }

  /**
   * @see {@link TableDataIf#getData(int, String)}.
   */
  public String getData(int rowNum, String elementPath) {
    int dataIndex = this.getIndexIntoDataTable(rowNum);
    Row row = mTable.getRowAtIndex(dataIndex);
    if (row == null) {
      WebLogger.getLogger(mTable.getAppName()).e(TAG,
          "row " + rowNum + " does not exist! Returning null");
      return null;
    }

    String elementKey = ColumnUtil.get().getElementKeyFromElementPath(elementPath);
    if (elementKey == null) {
      WebLogger.getLogger(mTable.getAppName()).e(TAG,
          "column with elementPath: " + elementPath + " does not" + " exist.");
      return null;
    }
    ArrayList<ColumnDefinition> orderedDefns = getColumnDefinitions();
    ColumnDefinition cd = ColumnDefinition.find(orderedDefns, elementKey);
    ElementDataType type = cd.getType().getDataType();
    if (type == ElementDataType.array) {
      String result = row.getRawDataOrMetadataByElementKey(elementKey);
      return result;
    }

    if (cd.getChildren().isEmpty()) {
      String result = row.getRawDataOrMetadataByElementKey(elementKey);
      return result;
    }

    try {
      Map<String, Object> resultSet = new HashMap<String, Object>();
      assembleNonNullParts(row, resultSet, cd.getChildren());
      if (resultSet.isEmpty()) {
        return null;
      }
      String result = ODKFileUtils.mapper.writeValueAsString(resultSet);
      return result;
    } catch (JsonParseException e) {
      WebLogger.getLogger(mTable.getAppName()).printStackTrace(e);
      throw new IllegalStateException("unable to parse JSON expression");
    } catch (JsonMappingException e) {
      WebLogger.getLogger(mTable.getAppName()).printStackTrace(e);
      throw new IllegalStateException("unable to parse JSON expression");
    } catch (IOException e) {
      WebLogger.getLogger(mTable.getAppName()).printStackTrace(e);
      throw new IllegalStateException("unable to parse JSON expression");
    }
  }

  private void assembleNonNullParts(Row row, Map<String, Object> resultSet,
      List<ColumnDefinition> colDefns) throws JsonParseException, JsonMappingException, IOException {
    for (ColumnDefinition colDefn : colDefns) {
      ElementType type = colDefn.getType();
      ElementDataType dataType = type.getDataType();
      if (dataType == ElementDataType.array) {
        String result = row.getRawDataOrMetadataByElementKey(colDefn.getElementKey());
        resultSet.put(colDefn.getElementName(),
            ODKFileUtils.mapper.readValue(result, ArrayList.class));
      } else {
        List<ColumnDefinition> children = colDefn.getChildren();
        if (children.isEmpty()) {
          Class<?> clazz = ColumnUtil.get().getDataType(dataType);
          Object value = row.getRawDataType(colDefn.getElementKey(), clazz);
          if (value != null) {
            resultSet.put(colDefn.getElementName(), value);
          }
        } else {
          Map<String, Object> subValues = new HashMap<String, Object>();
          assembleNonNullParts(row, subValues, children);
          if (!subValues.isEmpty()) {
            resultSet.put(colDefn.getElementName(), subValues);
          }
        }
      }
    }
  }

  /**
   * Calculate the index into the data table given the display index. The caller
   * expects to iterate over the data rows in a particular order. This method
   * maps the display index into a data index. This data index can then be
   * requested to the backing data table.
   * 
   * @param displayIndex
   * @return
   */
  protected int getIndexIntoDataTable(int displayIndex) {
    if (!this.displayIndexMustBeCalculated()) {
      // Then we can just return it directly.
      return displayIndex;
    }
    // At the moment the only thing we have to account for is that a particular
    // row has been moved to the top of the list. The math is thus pretty neat.
    // Say that the selected index at the top is 5. The resultant values to be
    // returned are thus:
    // displayIndex: 0 1 2 3 4 5 6 7 8
    // returnValue : 5 0 1 2 3 4 6 7 8
    int result;
    if (displayIndex == 0) {
      result = this.mSelectedMapMarkerIndex;
    } else if (displayIndex <= this.mSelectedMapMarkerIndex) {
      result = displayIndex - 1;
    } else {
      // displayindex > selected marker index
      result = displayIndex;
    }
    return result;
  }

  /**
   * Return true if the display index does not map directly to the data index.
   * For example, this returns true if a map marker has been selected.
   * 
   * @return
   */
  boolean displayIndexMustBeCalculated() {
    return this.mSelectedMapMarkerIndex != INVALID_INDEX;
  }

  /**
   * Set the index of the map marker that has been selected.
   * 
   * @param mapIndex
   */
  public void setSelectedMapIndex(int mapIndex) {
    this.mSelectedMapMarkerIndex = mapIndex;
  }

  /**
   * Remove any map index from being selected.
   */
  public void setNoItemSelected() {
    this.mSelectedMapMarkerIndex = INVALID_INDEX;
  }

  public String getTableId() {
    return mTable.getTableId();
  }

  public String getRowId(int index) {
    int correctedIndex = getIndexIntoDataTable(index);
    return mTable.getRowAtIndex(correctedIndex).getRowId();
  }

}
