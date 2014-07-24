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
package org.opendatakit.common.android.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.CharEncoding;
import org.opendatakit.aggregate.odktables.rest.ConflictType;
import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.aggregate.odktables.rest.SavepointTypeManipulator;
import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.KeyValueStoreEntryType;
import org.opendatakit.common.android.data.OdkTablesKeyValueStoreEntry;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.ColumnDefinitionsColumns;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.KeyValueStoreColumns;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

/**
 * Various utilities for importing/exporting tables from/to CSV.
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 *
 */
public class CsvUtil {


  public interface ExportListener {

    public void exportComplete(boolean outcome);
  };

  public interface ImportListener {
    public void updateProgressDetail(String progressDetailString);

    public void importComplete(boolean outcome);
  }

  private static final String TAG = CsvUtil.class.getSimpleName();

  private final Context context;
  private final String appName;

  public CsvUtil(Context context, String appName) {
	this.context = context;
    this.appName = appName;
  }

  // ===========================================================================================
  // EXPORT
  // ===========================================================================================

  /**
   * Export the given tableId.
   * Exports three csv files to the output/csv directory under the appName:
   * <ul>
   * <li>tableid.fileQualifier.csv - data table</li>
   * <li>tableid.fileQualifier.definition.csv - data table column definition</li>
   * <li>tableid.fileQualifier.properties.csv - key-value store of this table</li>
   * </ul>
   * If fileQualifier is null or an empty string, then it emits to
   * <ul>
   * <li>tableid.csv - data table</li>
   * <li>tableid.definition.csv - data table column definition</li>
   * <li>tableid.properties.csv - key-value store of this table</li>
   * </ul>
   *
   * @param exportListener
   * @param tp
   * @param fileQualifier
   * @return
   */
  public boolean exportSeparable(ExportListener exportListener, TableProperties tp, String fileQualifier) {
    // building array of columns to select and header row for output file
    // then we are including all the metadata columns.
    ArrayList<String> columns = new ArrayList<String>();

    Log.i(TAG, "exportSeparable: tableId: " + tp.getTableId() + " fileQualifier: " + ((fileQualifier == null) ? "<null>" : fileQualifier) );

    // put the user-relevant metadata columns in leftmost columns
    columns.add(DataTableColumns.ID);
    columns.add(DataTableColumns.FORM_ID);
    columns.add(DataTableColumns.LOCALE);
    columns.add(DataTableColumns.SAVEPOINT_TYPE);
    columns.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
    columns.add(DataTableColumns.SAVEPOINT_CREATOR);

    columns.addAll(tp.getPersistedColumns());

    // And now add all remaining export columns
    for (String colName : DbTable.getExportColumns()) {
      if (columns.contains(colName)) {
        continue;
      }
      columns.add(colName);
    }

    OutputStreamWriter output = null;
    try {
      // both files go under the output/csv directory...
      File outputCsv = new File(ODKFileUtils.getOutputTableCsvFile(appName, tp.getTableId(), fileQualifier));
      outputCsv.mkdirs();

      // emit properties files
      File definitionCsv = new File(ODKFileUtils.getOutputTableDefinitionCsvFile(appName, tp.getTableId(), fileQualifier));
      File propertiesCsv = new File(ODKFileUtils.getOutputTablePropertiesCsvFile(appName, tp.getTableId(), fileQualifier));

      if ( !writePropertiesCsv(tp, definitionCsv, propertiesCsv) ) {
        return false;
      }

      // getting data
      DbTable dbt = DbTable.getDbTable(tp);
      String whereString = DataTableColumns.SAVEPOINT_TYPE + " IS NOT NULL AND (" +
            DataTableColumns.CONFLICT_TYPE + " IS NULL OR " +
            DataTableColumns.CONFLICT_TYPE + " = " +
            Integer.toString(ConflictType.LOCAL_UPDATED_UPDATED_VALUES) + ")";
      UserTable table = dbt.rawSqlQuery(whereString, null, null, null, null, null);

      // emit data table...
      File file = new File( outputCsv, tp.getTableId() +
          ((fileQualifier != null && fileQualifier.length() != 0) ? ("." + fileQualifier) : "") + ".csv");
      FileOutputStream out = new FileOutputStream(file);
      output = new OutputStreamWriter(out, CharEncoding.UTF_8);
      RFC4180CsvWriter cw = new RFC4180CsvWriter(output);
      // don't have to worry about quotes in elementKeys...
      cw.writeNext(columns.toArray(new String[columns.size()]));
      String[] row = new String[columns.size()];
      for (int i = 0; i < table.getNumberOfRows(); i++) {
        Row dataRow = table.getRowAtIndex(i);
        for (int j = 0; j < columns.size(); ++j) {
          row[j] = dataRow.getRawDataOrMetadataByElementKey(columns.get(j));;
        }
        cw.writeNext(row);
      }
      cw.flush();
      cw.close();

      return true;
    } catch (IOException e) {
      return false;
    } finally {
      try {
        output.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * Writes the definition and properties files for the given tableId.
   * This is written to:
   * <ul>
   * <li>tables/tableId/definition.csv - data table column definition</li>
   * <li>tables/tableId/properties.csv - key-value store of this table</li>
   * </ul>
   * The definition.csv file contains the schema definition.
   * md5hash of it corresponds to the former schemaETag.
   *
   * The properties.csv file contains the table-level metadata
   * (key-value store). The md5hash of it corresponds to the
   * propertiesETag.
   *
   * For use by the sync mechanism.
   *
   * @param tp
   * @return
   */
  public boolean writePropertiesCsv(TableProperties tp) {
    File definitionCsv = new File(ODKFileUtils.getTableDefinitionCsvFile(appName, tp.getTableId()));
    File propertiesCsv = new File(ODKFileUtils.getTablePropertiesCsvFile(appName, tp.getTableId()));
    return writePropertiesCsv(tp, definitionCsv, propertiesCsv);
  }

  /**
   * Common routine to write the definition and properties files.
   *
   * @param tp
   * @param definitionCsv
   * @param propertiesCsv
   * @return
   */
  public boolean writePropertiesCsv(TableProperties tp, File definitionCsv, File propertiesCsv) {
    Log.i(TAG, "writePropertiesCsv: tableId: " + tp.getTableId());

    // writing metadata
    FileOutputStream out;
    RFC4180CsvWriter cw;
    OutputStreamWriter output = null;
    try {
      // emit definition.csv table...
      out = new FileOutputStream(definitionCsv);
      output = new OutputStreamWriter(out, CharEncoding.UTF_8);
      cw = new RFC4180CsvWriter(output);

      // Emit ColumnDefinitions

      ArrayList<String> colDefHeaders = new ArrayList<String>();
      colDefHeaders.add(ColumnDefinitionsColumns.ELEMENT_KEY);
      colDefHeaders.add(ColumnDefinitionsColumns.ELEMENT_NAME);
      colDefHeaders.add(ColumnDefinitionsColumns.ELEMENT_TYPE);
      colDefHeaders.add(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS);

      cw.writeNext(colDefHeaders.toArray(new String[colDefHeaders.size()]));
      String[] colDefRow = new String[colDefHeaders.size()];

      /**
       * Since the md5Hash of the file identifies identical schemas,
       * ensure that the list of columns is in alphabetical order.
       */
      ArrayList<ColumnProperties> orderedList = new ArrayList<ColumnProperties>();
      orderedList.addAll(tp.getAllColumns().values());
      Collections.sort(orderedList, new Comparator<ColumnProperties>(){
        @Override
        public int compare(ColumnProperties lhs, ColumnProperties rhs) {
          return lhs.getElementKey().compareTo(rhs.getElementKey());
        }});

      for ( ColumnProperties cp : orderedList ) {
        colDefRow[0] = cp.getElementKey();
        colDefRow[1] = cp.getElementName();
        colDefRow[2] = cp.getColumnType().toString();
        colDefRow[3] = ODKFileUtils.mapper.writeValueAsString(cp.getListChildElementKeys());
        cw.writeNext(colDefRow);
      }

      cw.flush();
      cw.close();

      // emit properties.csv...
      out = new FileOutputStream(propertiesCsv);
      output = new OutputStreamWriter(out, CharEncoding.UTF_8);
      cw = new RFC4180CsvWriter(output);

      // Emit KeyValueStore

      ArrayList<String> kvsHeaders = new ArrayList<String>();
      kvsHeaders.add(KeyValueStoreColumns.PARTITION);
      kvsHeaders.add(KeyValueStoreColumns.ASPECT);
      kvsHeaders.add(KeyValueStoreColumns.KEY);
      kvsHeaders.add(KeyValueStoreColumns.VALUE_TYPE);
      kvsHeaders.add(KeyValueStoreColumns.VALUE);

      /**
       * Since the md5Hash of the file identifies identical properties,
       * ensure that the list of KVS entries is in alphabetical order.
       */
      List<OdkTablesKeyValueStoreEntry> kvsEntries = tp.getMetaDataEntries();
      Collections.sort(kvsEntries, new Comparator<OdkTablesKeyValueStoreEntry>(){

        @Override
        public int compare(OdkTablesKeyValueStoreEntry lhs, OdkTablesKeyValueStoreEntry rhs) {
          int outcome;
          if ( lhs.partition == null && rhs.partition == null ) {
            outcome = 0;
          } else if ( lhs.partition == null ) {
            return -1;
          } else if ( rhs.partition == null ) {
            return 1;
          } else {
            outcome = lhs.partition.compareTo(rhs.partition);
          }
          if ( outcome != 0 ) return outcome;
          if ( lhs.aspect == null && rhs.aspect == null ) {
            outcome = 0;
          } else if ( lhs.aspect == null ) {
            return -1;
          } else if ( rhs.aspect == null ) {
            return 1;
          } else {
            outcome = lhs.aspect.compareTo(rhs.aspect);
          }
          if ( outcome != 0 ) return outcome;
          if ( lhs.key == null && rhs.key == null ) {
            outcome = 0;
          } else if ( lhs.key == null ) {
            return -1;
          } else if ( rhs.key == null ) {
            return 1;
          } else {
            outcome = lhs.key.compareTo(rhs.key);
          }
          return outcome;
        }});

      cw.writeNext(kvsHeaders.toArray(new String[kvsHeaders.size()]));
      String[] kvsRow = new String[kvsHeaders.size()];
      for (int i = 0; i < kvsEntries.size(); i++) {
        OdkTablesKeyValueStoreEntry entry = kvsEntries.get(i);
        kvsRow[0] = entry.partition;
        kvsRow[1] = entry.aspect;
        kvsRow[2] = entry.key;
        kvsRow[3] = entry.type;
        kvsRow[4] = entry.value;
        cw.writeNext(kvsRow);
      }
      cw.flush();
      cw.close();

      return true;
    } catch (IOException e) {
      return false;
    } finally {
      try {
        output.close();
      } catch (IOException e) {
      }
    }
  }

  private static class ColumnInfo {
    String elementKey;
    String displayName;
    String elementName;
    ColumnType elementType;
    List<String> listOfStringElementKeys = new ArrayList<String>();
    List<OdkTablesKeyValueStoreEntry> kvsEntries = new ArrayList<OdkTablesKeyValueStoreEntry>();
  };

  public int countUpToLastNonNullElement(String[] row) {
    for ( int i = row.length-1 ; i >= 0 ; --i ) {
      if ( row[i] != null ) {
        return (i+1);
      }
    }
    return 0;
  }

  /**
   * Update tableId from
   * <ul>
   * <li>tables/tableId/properties.csv</li>
   * <li>tables/tableId/definition.csv</li>
   * </ul>
   *
   * This will either create a table, or verify that the table structure
   * matches that defined in the csv. It will then override all the KVS
   * entries with those present in the file.
   *
   * @param importListener
   * @param tableId
   * @return either the updated TableProperties or null if an error occurs
   */
  public TableProperties updateTablePropertiesFromCsv(ImportListener importListener, String tableId) {

    Log.i(TAG, "updateTablePropertiesFromCsv: tableId: " + tableId );


    Map<String, ColumnInfo> columns = new HashMap<String, ColumnInfo>();

    // reading data
    File file = null;
    FileInputStream in = null;
    InputStreamReader input = null;
    RFC4180CsvReader cr = null;
    try {
      file = new File(ODKFileUtils.getTableDefinitionCsvFile(appName, tableId));
      in = new FileInputStream(file);
      input = new InputStreamReader(in, CharEncoding.UTF_8);
      cr = new RFC4180CsvReader(input);

      String[] row;

      // Read ColumnDefinitions
      // get the column headers
      String[] colHeaders = cr.readNext();
      int colHeadersLength = countUpToLastNonNullElement(colHeaders);
      // get the first row
      row = cr.readNext();
      while ( row != null && countUpToLastNonNullElement(row) != 0 ) {
        ColumnInfo ci = new ColumnInfo();

        int rowLength = countUpToLastNonNullElement(row);
        for ( int i = 0 ; i < rowLength ; ++i ) {
          if ( i >= colHeadersLength ) {
            throw new IllegalStateException("data beyond header row of ColumnDefinitions table");
          }
          if ( ColumnDefinitionsColumns.ELEMENT_KEY.equals(colHeaders[i]) ) {
            ci.elementKey = row[i];
          }
          if ( ColumnDefinitionsColumns.ELEMENT_NAME.equals(colHeaders[i]) ) {
            ci.elementName = row[i];
          }
          if ( ColumnDefinitionsColumns.ELEMENT_TYPE.equals(colHeaders[i]) ) {
            ci.elementType = ColumnType.valueOf(row[i]);
          }
          if ( ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS.equals(colHeaders[i]) ) {
            ci.listOfStringElementKeys = ODKFileUtils.mapper.readValue(row[i], ArrayList.class );
          }
        }

        if ( ci.elementKey == null || ci.elementType == null ) {
          throw new IllegalStateException("ElementKey and ElementType must be specified");
        }

        columns.put(ci.elementKey, ci);

        // get next row or blank to end...
        row = cr.readNext();
      }

      cr.close();
      try {
        input.close();
      } catch (IOException e) {
      }
      try {
        in.close();
      } catch (IOException e) {
      }

      file = new File(ODKFileUtils.getTablePropertiesCsvFile(appName, tableId));
      in = new FileInputStream(file);
      input = new InputStreamReader(in, CharEncoding.UTF_8);
      cr = new RFC4180CsvReader(input);
      // Read KeyValueStore
      // read the column headers
      String[] kvsHeaders = cr.readNext();
      int kvsHeadersLength = countUpToLastNonNullElement(kvsHeaders);
      String displayName = null;
      List<OdkTablesKeyValueStoreEntry> kvsEntries = new ArrayList<OdkTablesKeyValueStoreEntry>();
      // read the first row
      row = cr.readNext();
      while ( row != null && countUpToLastNonNullElement(row)  != 0 ) {
        OdkTablesKeyValueStoreEntry kvsEntry = new OdkTablesKeyValueStoreEntry();
        kvsEntry.tableId = tableId;
        int rowLength = countUpToLastNonNullElement(row);
        for ( int i = 0 ; i < rowLength ; ++i ) {
          if ( KeyValueStoreColumns.PARTITION.equals(kvsHeaders[i]) ) {
            kvsEntry.partition = row[i];
          }
          if ( KeyValueStoreColumns.ASPECT.equals(kvsHeaders[i]) ) {
            kvsEntry.aspect = row[i];
          }
          if ( KeyValueStoreColumns.KEY.equals(kvsHeaders[i]) ) {
            kvsEntry.key = row[i];
          }
          if ( KeyValueStoreColumns.VALUE_TYPE.equals(kvsHeaders[i]) ) {
            kvsEntry.type = row[i];
          }
          if ( KeyValueStoreColumns.VALUE.equals(kvsHeaders[i]) ) {
            kvsEntry.value = row[i];
          }
        }
        if ( ColumnProperties.KVS_PARTITION.equals(kvsEntry.partition) ) {
          // column-specific
          String column = kvsEntry.aspect;
          ColumnInfo ci = columns.get(column);
          if ( ci == null ) {
            throw new IllegalStateException("Reference to non-existent column: " + column + " of tableId: " + tableId);
          }
          // look for the displayName
          if ( ColumnProperties.KEY_DISPLAY_NAME.equals(kvsEntry.key) ) {
            ci.displayName = kvsEntry.value;
          } else {
            ci.kvsEntries.add(kvsEntry);
          }
        } else {
          // not column-specific
          // see if we can find the displayName
          if ( KeyValueStoreConstants.PARTITION_TABLE.equals(kvsEntry.partition) &&
               KeyValueStoreConstants.ASPECT_DEFAULT.equals(kvsEntry.aspect) &&
               KeyValueStoreConstants.TABLE_DISPLAY_NAME.equals(kvsEntry.key) ) {
            displayName = kvsEntry.value;
          }
          // still put it in the kvsEntries -- displayName is not stored???
          kvsEntries.add(kvsEntry);
        }
        // get next row or blank to end...
        row = cr.readNext();
      }
      cr.close();
      try {
        input.close();
      } catch (IOException e) {
      }
      try {
        in.close();
      } catch (IOException e) {
      }

      TableProperties tp = TableProperties.refreshTablePropertiesForTable(context, appName, tableId);
      if ( tp.getDbTableName() == null ) {
        tp = null;
      }

      if ( tp != null ) {
        // confirm that the column definitions are unchanged...
        if ( tp.getAllColumns().size() != columns.size() ) {
          throw new IllegalStateException("Unexpectedly found tableId with different column definitions that already exists!");
        }
        for ( ColumnInfo ci : columns.values() ) {
          ColumnProperties cp = tp.getColumnByElementKey(ci.elementKey);
          if ( cp == null ) {
            throw new IllegalStateException("Unexpectedly failed to match elementKey: " + ci.elementKey);
          }
          if ( !cp.getElementName().equals(ci.elementName) ) {
            throw new IllegalStateException("Unexpected mis-match of elementName for elementKey: " + ci.elementKey);
          }
          List<String> refList = cp.getListChildElementKeys();
          if ( refList == null ) {
            refList = new ArrayList<String>();
          }
          List<String> ciList = ci.listOfStringElementKeys;
          if ( ciList == null ) {
            ciList = new ArrayList<String>();
          }
          if ( refList.size() != ciList.size() ) {
            throw new IllegalStateException("Unexpected mis-match of listOfStringElementKeys for elementKey: " + ci.elementKey);
          }
          for ( int i = 0 ; i < refList.size() ; ++i ) {
            if ( !refList.get(i).equals(ciList.get(i)) ) {
              throw new IllegalStateException("Unexpected mis-match of listOfStringElementKeys[" + i + "] for elementKey: " + ci.elementKey);
            }
          }
          if ( cp.getColumnType() != ci.elementType ) {
            throw new IllegalStateException("Unexpected mis-match of elementType for elementKey: " + ci.elementKey);
          }
        }
        // OK -- we have matching table definition
        // now just clear and update the properties...
        tp.addMetaDataEntries(kvsEntries, true);
        for ( ColumnInfo ci : columns.values() ) {
          ColumnProperties cp = tp.getColumnByElementKey(ci.elementKey);
          // put the displayName into the KVS
          OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
          entry.tableId = tableId;
          entry.partition = ColumnProperties.KVS_PARTITION;
          entry.aspect = ci.elementKey;
          entry.key = ColumnProperties.KEY_DISPLAY_NAME;
          entry.type = KeyValueStoreEntryType.OBJECT.name();
          entry.value = ci.displayName;
          ci.kvsEntries.add(entry);
          cp.addMetaDataEntries(ci.kvsEntries);
        }
      } else {
        tp = TableProperties.addTable(context, appName,
            tableId, (displayName == null ? tableId : displayName), tableId);

        for ( ColumnInfo ci : columns.values() ) {
          ColumnProperties cp = tp.addColumn(ci.displayName, ci.elementKey, ci.elementName,
              ci.elementType, ci.listOfStringElementKeys);
          cp.addMetaDataEntries(ci.kvsEntries);
        }
        tp.addMetaDataEntries(kvsEntries, false);
      }
      tp = TableProperties.refreshTablePropertiesForTable(context, appName, tableId);
      if ( tp.getDbTableName() == null ) {
        tp = null;
      }
      return tp;
    } catch (IOException e) {
      return null;
    } finally {
      try {
        input.close();
      } catch (IOException e) {
      }
    }
  }


  /**
   * Imports data from a csv file with elementKey headings.
   * This csv file is assumed to be under:
   * <ul>
   * <li>assets/csv/tableId.fileQualifier.csv</li>
   * </ul>
   * If the table does not exist, it attempts to create it
   * using the schema and metadata located here:
   * <ul>
   * <li>tables/tableId/definition.csv - data table definition</li>
   * <li>tables/tableId/properties.csv - key-value store</li>
   * </ul>
   *
   * @param importListener
   * @param tableId
   * @param fileQualifier
   * @param createIfNotPresent -- true if we should try to create the table.
   * @return
   */
  public boolean importSeparable(ImportListener importListener, String tableId, String fileQualifier, boolean createIfNotPresent) {

    TableProperties tp = TableProperties.refreshTablePropertiesForTable(context, appName, tableId);
    if ( tp.getDbTableName() == null ) {
      tp = null;
    }

    if ( tp == null ) {
      if ( createIfNotPresent ) {
        tp = updateTablePropertiesFromCsv(importListener, tableId);
        if ( tp == null ) {
          return false;
        }
      } else {
        return false;
      }
    }

    Log.i(TAG, "importSeparable: tableId: " + tableId + " fileQualifier: " + ((fileQualifier == null) ? "<null>" : fileQualifier) );

    // reading data
    InputStreamReader input = null;
    try {
      // both files are read from assets/csv directory...
      File assetsCsv = new File(new File(ODKFileUtils.getAssetsFolder(appName)), "csv");

      // read data table...
      File file = new File( assetsCsv, tp.getTableId() +
          ((fileQualifier != null && fileQualifier.length() != 0) ? ("." + fileQualifier) : "") + ".csv");
      FileInputStream in = new FileInputStream(file);
      input = new InputStreamReader(in, CharEncoding.UTF_8);
      RFC4180CsvReader cr = new RFC4180CsvReader(input);
      // don't have to worry about quotes in elementKeys...
      String[] columnsInFile = cr.readNext();
      int columnsInFileLength = countUpToLastNonNullElement(columnsInFile);
      DbTable dbTable = DbTable.getDbTable(tp);

      String v_id;
      String v_form_id;
      String v_locale;
      String v_savepoint_type;
      String v_savepoint_creator;
      String v_savepoint_timestamp;
      String v_row_etag;
      String v_filter_type;
      String v_filter_value;

      Map<String,String> valueMap = new HashMap<String,String>();

      int rowCount = 0;
      String[] row;
      for (;;) {
        row = cr.readNext();
        rowCount++;
        if ( rowCount % 5 == 0 ) {
          importListener.updateProgressDetail("Row " + rowCount);
        }
        if ( row == null || countUpToLastNonNullElement(row) == 0 ) {
          break;
        }
        int rowLength = countUpToLastNonNullElement(row);

        // default values for metadata columns if not provided
        v_id = UUID.randomUUID().toString();
        v_form_id = null;
        v_locale = ODKDatabaseUtils.DEFAULT_LOCALE;
        v_savepoint_type = SavepointTypeManipulator.complete();
        v_savepoint_creator = ODKDatabaseUtils.DEFAULT_CREATOR;
        v_savepoint_timestamp = TableConstants.nanoSecondsFromMillis(System.currentTimeMillis());
        v_row_etag = null;
        v_filter_type = null;
        v_filter_value = null;
        // clear value map
        valueMap.clear();

        boolean foundId = false;
        for ( int i = 0 ; i < columnsInFileLength ; ++i ) {
          if ( i > rowLength ) break;
          String column = columnsInFile[i];
          String tmp = row[i];
          if ( DataTableColumns.ID.equals(column) && (tmp != null && tmp.length() != 0) ) {
            foundId = true;
            v_id = tmp;
          }
          if ( DataTableColumns.FORM_ID.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_form_id = tmp;
          }
          if ( DataTableColumns.LOCALE.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_locale = tmp;
          }
          if ( DataTableColumns.SAVEPOINT_TYPE.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_savepoint_type = tmp;
          }
          if ( DataTableColumns.SAVEPOINT_CREATOR.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_savepoint_creator = tmp;
          }
          if ( DataTableColumns.SAVEPOINT_TIMESTAMP.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_savepoint_timestamp = tmp;
          }
          if ( DataTableColumns.ROW_ETAG.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_row_etag = tmp;
          }
          if ( DataTableColumns.FILTER_TYPE.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_filter_type = tmp;
          }
          if ( DataTableColumns.FILTER_VALUE.equals(column) && (tmp != null && tmp.length() != 0) ) {
            v_filter_value = tmp;
          }
          if ( tp.getColumnByElementKey(column) != null ) {
            valueMap.put(column, tmp);
          }
        }

        // TODO: should resolve this properly when we have conflict rows and
        // uncommitted edits. For now, we just add our csv import to those, rather
        // than resolve the problems.
        SyncState syncState = null;
        if ( foundId ) {
          syncState = dbTable.getRowSyncState(v_id);
        }
        /**
         * Insertion will set the SYNC_STATE to inserting.
         *
         * If the table is sync'd to the server, this will cause one
         * sync interaction with the server to confirm that the server
         * also has this record.
         *
         * If a record with this same rowId already exists, if it is
         * in an inserting sync state, we update it here. Otherwise,
         * if there were any local changes, we leave the row unchanged.
         */
        if ( syncState != null ) {

          ContentValues cv = new ContentValues();
          if (v_id != null) {
            cv.put(DataTableColumns.ID, v_id);
          }
          for (String column : valueMap.keySet()) {
           if ( column != null ) {
              cv.put(column, valueMap.get(column));
           }
          }

          // The admin columns get added here
          cv.put(DataTableColumns.FORM_ID, v_form_id);
          cv.put(DataTableColumns.LOCALE, v_locale);
          cv.put(DataTableColumns.SAVEPOINT_TYPE, v_savepoint_type);
          cv.put(DataTableColumns.SAVEPOINT_TIMESTAMP, v_savepoint_timestamp);
          cv.put(DataTableColumns.SAVEPOINT_CREATOR, v_savepoint_creator);
          cv.put(DataTableColumns.ROW_ETAG, v_row_etag);
          cv.put(DataTableColumns.FILTER_TYPE, v_filter_type);
          cv.put(DataTableColumns.FILTER_VALUE, v_filter_value);


          cv.put(DataTableColumns.SYNC_STATE, SyncState.inserting.name());

          if ( syncState == SyncState.inserting ) {
            // we do the actual update here
            dbTable.actualUpdateRowByRowId(v_id, cv);
          }
          // otherwise, do NOT update the row.

        } else {

          dbTable.addRow(v_id, v_form_id, v_locale,
              v_savepoint_type, v_savepoint_timestamp, v_savepoint_creator,
              v_row_etag, v_filter_type, v_filter_value, valueMap);

        }
      }
      cr.close();
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      try {
        input.close();
      } catch (IOException e) {
      }
    }
  }

}
