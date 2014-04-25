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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utils.DataUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * This class represents a table. This can be conceptualized as a list of rows.
 * Each row comprises the user-defined columns, or data, as well as the
 * ODKTables-specified metadata.
 * <p>
 * This should be considered an immutable class.
 *
 * @author unknown
 * @author sudar.sam@gmail.com
 *
 */
public class UserTable {

  private static final String TAG = UserTable.class.getSimpleName();

  private final String[] header;
  private final ArrayList<Row> mRows;
  /**
   * The {@link TableProperties} associated with this table. Included so that
   * more intelligent things can be done with regards to interpretation of type.
   */
  private final TableProperties mTp;
  private final String mSqlWhereClause;
  private final String[] mSqlSelectionArgs;
  private final String[] mSqlGroupByArgs;
  private final String mSqlHavingClause;
  private final String mSqlOrderByElementKey;
  private final String mSqlOrderByDirection;

  private final String[] mElementKeyForIndex;
  // array of ColumnProperties for these element keys
  // this can go stale when ColumnProperties are changed,
  // so it must be explicitly recomputed before being used
  // e.g., reloadCacheOfColumnProperties()
  private final ArrayList<ColumnProperties> mColumnProperties = new ArrayList<ColumnProperties>();

  /**
   * Maps the element key of user-defined columns to the corresponding index in
   * the Row objects.
   */
  private final Map<String, Integer> mDataKeyToIndex;
  /**
   * Maps the element key of ODKTables-specified metadata columns to the
   * corresponding indices in the Row objects.
   */
  private final Map<String, Integer> mMetadataKeyToIndex;

  private Map<String, Integer> mUnmodifiableCachedDataKeyToIndex = null;
  private Map<String, Integer> mUnmodifiableCachedMetadataKeyToIndex = null;

  /**
   * A simple cache of color rules so they're not recreated unnecessarily
   * each time. Maps the column display name to {@link ColorRuleGroup} for
   * that column.
   */
  private Map<String, ColorRuleGroup> mElementKeyToColorRuleGroup =
      new HashMap<String, ColorRuleGroup>();

  private final DataUtil du;
  private DateTimeZone tz;
  private DateTimeFormatter dateFormatter;
  private DateTimeFormatter dateTimeFormatter;
  private DateTimeFormatter timeFormatter;

  private void buildFormatters() {
    Locale l = Locale.getDefault();
    tz = DateTimeZone.forTimeZone(TimeZone.getDefault());
    dateFormatter = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("M-", l)).withZone(tz);
    dateTimeFormatter = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("ML", l)).withZone(tz);
    timeFormatter = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("-L", l)).withZone(tz);
  }

  public UserTable(UserTable table, List<Integer> indexes) {
    du = table.du;
    buildFormatters();
    this.header = table.header;
    mRows = new ArrayList<Row>(indexes.size());
    for (int i = 0 ; i < indexes.size(); ++i) {
      Row r = table.getRowAtIndex(i);
      mRows.add(r);
    }
    this.mTp = table.getTableProperties();
    this.mSqlWhereClause = table.mSqlWhereClause;
    this.mSqlSelectionArgs = table.mSqlSelectionArgs;
    this.mSqlGroupByArgs = table.mSqlGroupByArgs;
    this.mSqlHavingClause = table.mSqlHavingClause;
    this.mSqlOrderByElementKey = table.mSqlOrderByElementKey;
    this.mSqlOrderByDirection = table.mSqlOrderByDirection;
    this.mDataKeyToIndex = table.getMapOfUserDataToIndex();
    this.mMetadataKeyToIndex = table.getMapOfMetadataToIndex();
    this.mElementKeyForIndex = table.getElementKeysForIndex();
  }

  public UserTable(Cursor c, TableProperties tableProperties,
      List<String> userColumnOrder,
      String sqlWhereClause, String[] sqlSelectionArgs,
      String[] sqlGroupByArgs, String sqlHavingClause,
      String sqlOrderByElementKey, String sqlOrderByDirection) {
    du = new DataUtil(Locale.ENGLISH, TimeZone.getDefault());
    buildFormatters();
    mTp = tableProperties;
    this.mSqlWhereClause = sqlWhereClause;
    this.mSqlSelectionArgs = sqlSelectionArgs;
    this.mSqlGroupByArgs = sqlGroupByArgs;
    this.mSqlHavingClause = sqlHavingClause;
    this.mSqlOrderByElementKey = sqlOrderByElementKey;
    this.mSqlOrderByDirection = sqlOrderByDirection;
    List<String> adminColumnOrder = DbTable.getAdminColumns();
    int rowIdIndex = c.getColumnIndexOrThrow(DataTableColumns.ID);
    // These maps will map the element key to the corresponding index in
    // either data or metadata. If the user has defined a column with the
    // element key _my_data, and this column is at index 5 in the data
    // array, dataKeyToIndex would then have a mapping of _my_data:5.
    // The sync_state column, if present at index 7, would have a mapping
    // in metadataKeyToIndex of sync_state:7.
    mDataKeyToIndex = new HashMap<String, Integer>();
    mElementKeyForIndex = new String[userColumnOrder.size()];
    header = new String[userColumnOrder.size()];
    int[] userColumnCursorIndex = new int[userColumnOrder.size()];
    for (int i = 0; i < userColumnOrder.size(); i++) {
      String elementKey = userColumnOrder.get(i);
      mElementKeyForIndex[i] = elementKey;
      mDataKeyToIndex.put(elementKey, i);
      header[i] = mTp.getColumnByElementKey(elementKey).getDisplayName();
      userColumnCursorIndex[i] = c.getColumnIndex(elementKey);
    }
    mMetadataKeyToIndex = new HashMap<String, Integer>();
    int[] adminColumnCursorIndex = new int[adminColumnOrder.size()];
    for (int i = 0; i < adminColumnOrder.size(); i++) {
      // TODO: problem is here. unclear how to best get just the
      // metadata in here. hmm.
      String elementKey = adminColumnOrder.get(i);
      mMetadataKeyToIndex.put(elementKey, i);
      adminColumnCursorIndex[i] = c.getColumnIndex(elementKey);
    }

    c.moveToFirst();
    int rowCount = c.getCount();
    mRows = new ArrayList<Row>(rowCount);

    String[] rowData = new String[userColumnOrder.size()];
    String[] rowMetadata = new String[adminColumnOrder.size()];
    if (c.moveToFirst()) {
      do {
        String rowId = c.getString(rowIdIndex);
        // First get the user-defined data for this row.
        for (int i = 0; i < userColumnOrder.size(); i++) {
          String value = getIndexAsString(c, userColumnCursorIndex[i]);
          rowData[i] = value;
        }
        // Now get the metadata for this row.
        for (int i = 0; i < adminColumnOrder.size(); i++) {
          String value = getIndexAsString(c, adminColumnCursorIndex[i]);
          rowMetadata[i] = value;
        }
        Row nextRow = new Row(rowId, rowData.clone(), rowMetadata.clone());
        mRows.add(nextRow);
      } while (c.moveToNext());
    }
  }

  /**
   * Return the data stored in the cursor at the given index and given position
   * (ie the given row which the cursor is currently on) as a String.
   * <p>
   * NB: Currently only checks for Strings, long, int, and double.
   *
   * @param c
   * @param i
   * @return
   */
  @SuppressLint("NewApi")
  private static final String getIndexAsString(Cursor c, int i) {
    // If you add additional return types here be sure to modify the javadoc.
    if ( i == -1 ) return null;
    int version = android.os.Build.VERSION.SDK_INT;
    if (version < 11) {
      // getType() is not yet supported.
      String str = null;
      try {
        str = c.getString(i);
      } catch (Exception e1) {
        try {
          str = Long.toString(c.getLong(i));
        } catch (Exception e2) {
          try {
            str = Double.toString(c.getDouble(i));
          } catch (Exception e3) {
            throw new IllegalStateException("Unexpected data type in SQLite table");
          }
        }
      }
      return str;
    } else {
      switch (c.getType(i)) {
      case Cursor.FIELD_TYPE_STRING:
        return c.getString(i);
      case Cursor.FIELD_TYPE_FLOAT:
        return Double.toString(c.getDouble(i));
      case Cursor.FIELD_TYPE_INTEGER:
        return Long.toString(c.getLong(i));
      case Cursor.FIELD_TYPE_NULL:
        return c.getString(i);
      default:
      case Cursor.FIELD_TYPE_BLOB:
        throw new IllegalStateException("Unexpected data type in SQLite table");
      }
    }
  }

  public Long getTimestamp(int rowNum) {
    return TableConstants.milliSecondsFromNanos(getMetadataByElementKey(rowNum, DataTableColumns.SAVEPOINT_TIMESTAMP));
  }

  public Row getRowAtIndex(int index) {
    return this.mRows.get(index);
  }

  public String getHeader(int colNum) {
    return header[colNum];
  }

  public String getElementKey(int colNum) {
    return mElementKeyForIndex[colNum];
  }

  /**
   * Get the index of the element key for the user-defined columns.
   * @param elementKey
   * @return null if the column is not found
   */
  public Integer getColumnIndexOfElementKey(String elementKey) {
    return mDataKeyToIndex.get(elementKey);
  }

  public String getData(int cellNum) {
    int rowNum = cellNum / getWidth();
    int colNum = cellNum % getWidth();
    return getData(rowNum, colNum);
  }

  public String getData(int rowNum, int colNum) {
    return mRows.get(rowNum).getDataAtIndex(colNum);
  }

  public String getWhereClause() {
    return mSqlWhereClause;
  }

  public String[] getSelectionArgs() {
    return mSqlSelectionArgs.clone();
  }

  /**
   * True if the table has a group-by clause in its query
   *
   * @return
   */
  public boolean isGroupedBy() {
    return mSqlGroupByArgs != null && mSqlGroupByArgs.length != 0;
  }

  public String[] getGroupByArgs() {
    return mSqlGroupByArgs.clone();
  }

  public String getHavingClause() {
    return mSqlHavingClause;
  }

  public String getOrderByElementKey() {
    return mSqlOrderByElementKey;
  }

  public String getOrderByDirection() {
    return mSqlOrderByDirection;
  }

  public Integer getForegroundColorOfData(String elementPath, String value) {
    String elementKey = mTp.getElementKeyFromElementPath(elementPath);
    if (elementKey == null) {
      // Note that this currently cannot happen, because the implementation
      // of getElementKeyFromElementPath is not real. It just does a string
      // replace, which is incorrect. But we should have this case.
      return null;
    }
    ColorRuleGroup colRul = this.mElementKeyToColorRuleGroup
          .get(elementPath);
    if (colRul == null) {
       // If it's not already there, cache it for future use.
       colRul = ColorRuleGroup.getColumnColorRuleGroup(mTp, elementKey);
       this.mElementKeyToColorRuleGroup.put(elementPath, colRul);
    }

    // We need to construct a dummy Row for the ColorRule to interpret
    String[] rowData = new String[this.mDataKeyToIndex.size()];
    String[] rowMetadata = new String[this.mMetadataKeyToIndex.size()];
    rowData[this.mDataKeyToIndex.get(elementKey)] = value;
    Row row = new Row("dummyRowId", rowData, rowMetadata);
    ColorGuide guide = colRul.getColorGuide(row);
    if (guide != null) {
      return guide.getForeground();
    } else {
      return null;
    }
  }

  public String getDisplayTextOfData(Context context, int cellNum) {
    int rowNum = cellNum / getWidth();
    int colNum = cellNum % getWidth();
    return getDisplayTextOfData(context, rowNum, colNum, true);
  }

  public String getDisplayTextOfData(Context context, int rowNum, int colNum, boolean showErrorText) {
    // TODO: share processing with CollectUtil.writeRowDataToBeEdited(...)
    String raw = getData(rowNum,colNum);
    if ( raw == null ) {
      return null;
    }
    ColumnProperties cp = mColumnProperties.get(colNum);
    ColumnType type = cp.getColumnType();
    if ( type == ColumnType.AUDIOURI ||
         type == ColumnType.IMAGEURI ||
         type == ColumnType.MIMEURI ||
         type == ColumnType.VIDEOURI ) {
      try {
        if (raw.length() == 0 ) {
          return raw;
        }
        @SuppressWarnings("rawtypes")
        Map m = ODKFileUtils.mapper.readValue(raw, Map.class);
        String uriFragment = (String) m.get("uriFragment");
        File f = FileProvider.getAsFile(context, mTp.getAppName(), uriFragment);
        return f.getName();
      } catch (JsonParseException e) {
        e.printStackTrace();
      } catch (JsonMappingException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return raw;
    } else if ( type == ColumnType.DATE ) {
      DateTime d = du.parseDateTimeFromDb(raw);
      return dateFormatter.print(d);
    } else if ( type == ColumnType.DATETIME ) {
      DateTime d = du.parseDateTimeFromDb(raw);
      return dateTimeFormatter.print(d);
    } else if ( type == ColumnType.TIME ) {
      DateTime d = du.parseDateTimeFromDb(raw);
      return timeFormatter.print(d);
    } else if ( type == ColumnType.TABLE_JOIN ) {
      return raw;
    } else {
      return raw;
    }
  }

  /**
   * The cache should be reloaded before using getDisplayTextOfData
   * (above) because the column properties could change due to
   * changes in the property values for those columns.
   */
  public void reloadCacheOfColumnProperties() {
    mColumnProperties.clear();
    for ( int i = 0 ; i < mElementKeyForIndex.length ; ++i ) {
      String elementKey = mElementKeyForIndex[i];
      mColumnProperties.add(mTp.getColumnByElementKey(elementKey));
    }
  }

  /**
   * Retrieve the metadata datum in the column specified by elementKey at the
   * given row number.
   *
   * @param rowNum
   * @param elementKey
   * @return
   */
  public String getMetadataByElementKey(int rowNum, String elementKey) {
    Integer idx = mMetadataKeyToIndex.get(elementKey);
    if ( idx == null ) {
      return null;
    }
    return mRows.get(rowNum).getMetadataAtIndex(idx);
  }

  /**
   * Return the data or metadata value in the given row by element key.
   * @param rowNum
   * @param elementKey
   * @return
   */
  public String getDataByElementKey(int rowNum, String elementKey) {
    return mRows.get(rowNum).getDataOrMetadataByElementKey(elementKey);
  }

  /**
   * Return a map containing the mapping of the element keys for the user-
   * defined columns to their index in array returned by
   * {@link Row#getAllData()}.
   *
   * @return
   */
  public Map<String, Integer> getMapOfUserDataToIndex() {
    if (this.mUnmodifiableCachedDataKeyToIndex == null) {
      this.mUnmodifiableCachedDataKeyToIndex = Collections.unmodifiableMap(this.mDataKeyToIndex);
    }
    return this.mUnmodifiableCachedDataKeyToIndex;
  }

  public TableProperties getTableProperties() {
    return mTp;
  }

  /**
   * Return a map containing the mapping of the element keys for the
   * ODKTables-specified metadata columns to their index in the array returned
   * by {@link Row#getAllMetadata()}.
   *
   * @return
   */
  public Map<String, Integer> getMapOfMetadataToIndex() {
    if (this.mUnmodifiableCachedMetadataKeyToIndex == null) {
      this.mUnmodifiableCachedMetadataKeyToIndex = Collections
          .unmodifiableMap(this.mMetadataKeyToIndex);
    }
    return this.mUnmodifiableCachedMetadataKeyToIndex;
  }

  public String[] getElementKeysForIndex() {
    return this.mElementKeyForIndex.clone();
  }

  public int getWidth() {
    return header.length;
  }

  /**
   * Get the number of metadata columns.
   *
   * @return
   */
  public int getNumberOfMetadataColumns() {
    return mMetadataKeyToIndex.size();
  }

  public String[] getAllMetadataForRow(int rowNum) {
    return mRows.get(rowNum).getAllMetadata();
  }

  public int getNumberOfRows() {
    return this.mRows.size();
  }

  /**
   * Scan the rowIds to get the row number. As the rowIds are not sorted, this
   * is a potentially expensive operation, scanning the entire array, as well as
   * the cost of checking String equality. Should be used only when necessary.
   * <p>
   * Return -1 if the row Id is not found.
   *
   * @param rowId
   * @return
   */
  public int getRowNumFromId(String rowId) {
    for (int i = 0; i < this.mRows.size(); i++) {
      if (this.mRows.get(i).mRowId.equals(rowId)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * This represents a single row of data in a table.
   *
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
     * Holds the actual data in the row. To index into the array correctly, must
     * use the information contained in UserTable.
     */
    private final String[] mData;

    /**
     * Holds the metadata for the row. to index into the array correctly, must
     * use the information contained in UserTable.
     */
    private final String[] mMetadata;

    /**
     * Construct the row.
     *
     * @param rowId
     * @param data
     *          the user-defined data of the row
     * @param metadata
     *          the ODKTables-specified metadata for the row.
     */
    public Row(String rowId, String[] data, String[] metadata) {
      this.mRowId = rowId;
      this.mData = data;
      this.mMetadata = metadata;
    }

    /**
     * Return the value of the row at the given index.
     *
     * @param index
     * @return
     */
    public String getDataAtIndex(int index) {
      return mData[index];
    }

    /**
     * Return the metadata value at the given index.
     *
     * @param index
     * @return
     */
    public String getMetadataAtIndex(int index) {
      return mMetadata[index];
    }

    /**
     * Return the id of this row.
     * @return
     */
    public String getRowId() {
      return this.mRowId;
    }

    /**
     * Return the String representing the contents of the column represented by
     * the passed in elementKey. This can be either the element key of a
     * user-defined column or a ODKTabes-specified metadata column.
     * <p>
     * Null values are returned as an empty string. Null is returned if the
     * elementKey is not found in the table.
     *
     * @param elementKey
     *          elementKey of data or metadata column
     * @return String representation of contents of column. Null values are
     *         converted to an empty string. If the elementKey is not contained
     *         in the table, returns null.
     */
    public String getDataOrMetadataByElementKey(String elementKey) {
      String result;
      if (UserTable.this.mDataKeyToIndex.containsKey(elementKey)) {
        result = this.mData[UserTable.this.mDataKeyToIndex.get(elementKey)];
      } else if (UserTable.this.mMetadataKeyToIndex.containsKey(elementKey)) {
        result = this.mMetadata[UserTable.this.mMetadataKeyToIndex.get(elementKey)];
      } else {
        // The elementKey was not in the table. Probable error or misuse.
        Log.e(TAG, "elementKey [" + elementKey + "] was not found in table");
        return null;
      }
      if (result == null) {
        result = "";
      }
      return result;
    }

    /**
     * Get the array backing the entire row.
     *
     * @return
     */
    public String[] getAllData() {
      return mData;
    }

    public String[] getAllMetadata() {
      return mMetadata;
    }

    @Override
    public int hashCode() {
      final int PRIME = 31;
      int result = 1;
      result = result * PRIME + this.mRowId.hashCode();
      result = result * PRIME + this.mData.hashCode();
      result = result * PRIME + this.mMetadata.hashCode();
      return result;
    }

  }
}
