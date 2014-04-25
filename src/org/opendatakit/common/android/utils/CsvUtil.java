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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.CharEncoding;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.opendatakit.aggregate.odktables.rest.ConflictType;
import org.opendatakit.aggregate.odktables.rest.SavepointTypeManipulator;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.exception.TableAlreadyExistsException;
import org.opendatakit.common.android.provider.ColumnDefinitionsColumns;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.KeyValueStoreColumns;
import org.opendatakit.common.android.provider.TableDefinitionsColumns;
import org.opendatakit.common.android.sync.aggregate.SyncTag;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;

import android.content.Context;
import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;

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
    public void updateLineCount(String progressString);

    public void importComplete(boolean outcome);
  }

  private static final String TAG = CsvUtil.class.getSimpleName();

  private static final String NEW_LINE = "\n";

  private static final String OPEN_CURLY_BRACKET = "{";

  private static final String PROPERTIES_CSV_FILE_EXTENSION = ".properties.csv";

  private static final String CSV_FILE_EXTENSION = ".csv";

  /** TempFilename for a csv file used for joining files (?) */
  private static final String ODK_TABLES_JOINING_CSV_FILENAME = "temp.csv";


  private static final String t = "CsvUtil";

  private static final String LAST_MOD_TIME_LABEL = "_last_mod_time";
  private static final String ACCESS_CONTROL_LABEL = "_access_control";

  private static final char DELIMITING_CHAR = ',';
  private static final char QUOTE_CHAR = '\"';
  private static final char ESCAPE_CHAR = '\\';

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.setVisibilityChecker(mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
  }

  private final DataUtil du;
  private final Context context;
  private final String appName;

  public CsvUtil(Context context, String appName) {
	this.context = context;
    this.appName = appName;
    du = new DataUtil(Locale.ENGLISH, TimeZone.getDefault());;
  }

  /**
   * Import a table to the database.
   * <p>
   * Tables imported through this function are added to the active key value
   * store. Doing it another way would give users a workaround to add tables to
   * the server database.
   *
   * @param importTask
   *          the ImportTask calling this method. It is used to pass messages to
   *          the user in case of error. Null safe.
   * @param file
   * @param tableName
   * @return
   * @throws TableAlreadyExistsException
   */
  public boolean importNewTable(Context c, ImportListener importTask, File file, String tableName) throws TableAlreadyExistsException {

    String dbTableName = NameUtil.createUniqueDbTableName(c, appName, tableName);
    String tableId = NameUtil.createUniqueTableId(c, appName, dbTableName);
    TableProperties tp;
    try {
      boolean includesProperties = false;
      // columns contains all columns in the csv file...
      List<String> columns = new ArrayList<String>();
      int idxRowId = -1;
      int idxFormId = -1;
      int idxLocale = -1;
      int idxTimestamp = -1;
      int idxSavepointCreator = -1;

      InputStream is;
      try {
        is = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new IllegalStateException(e);
      }
      // Now get the reader.
      InputStreamReader isr;
      try {
        isr = new InputStreamReader(is, Charset.forName(CharEncoding.UTF_8));
      } catch (UnsupportedCharsetException e) {
        Log.w(TAG, "UTF-8 wasn't supported--trying with default charset");
        isr = new InputStreamReader(is);
      }

      CSVReader reader = new CSVReader(isr, DELIMITING_CHAR, QUOTE_CHAR, ESCAPE_CHAR);
      String[] row = reader.readNext();
      if (row.length == 0) {
        reader.close();
        return true;
      }
      // adding columns
      boolean discoverColumnNames = false;
      if (row[0].startsWith(OPEN_CURLY_BRACKET)) {
        // has been exported with properties.
        includesProperties = true;
        String jsonProperties = row[0];
        // now we need the tableId. It is tempting to just scan the
        // string until we find it, but if there are other occurrences
        // of the tableId json key we will get into trouble. So, we must
        // deserialize it.
        tp = TableProperties.addTableFromJson(c, appName, jsonProperties);
        // we need to check if we need to import all the key value store
        // things as well.
        if (row.length > 1) {
          // This is, by convention, the key value store entries in
          // list
          // form.
          try {
            List<OdkTablesKeyValueStoreEntry> recoveredEntries = mapper.readValue(row[1],
                new TypeReference<List<OdkTablesKeyValueStoreEntry>>() {
                });
            tp.addMetaDataEntries(recoveredEntries, false);
            // Since the KVS has all the display properties for a table, we must
            // re-read everything to get them.
            tp = TableProperties.refreshTablePropertiesForTable(context, appName, tp.getTableId());
            // TODO: sort out closing database appropriately.
          } catch (JsonGenerationException e) {
            e.printStackTrace();
            if (importTask != null) {
              importTask.importComplete(false);
            }
          } catch (JsonMappingException e) {
            e.printStackTrace();
            if (importTask != null) {
              importTask.importComplete(false);
            }
          } catch (IOException e) {
            e.printStackTrace();
            if (importTask != null) {
              importTask.importComplete(false);
            }
          }
        }
        discoverColumnNames = false;
        row = reader.readNext();
      } else {
        tp = TableProperties.addTable(c, appName, dbTableName, tableName, tableId);
        discoverColumnNames = true;
      }

      // now collect all the headings.
      for (int i = 0; i < row.length; ++i) {
        String colName = row[i];
        String dbName = null;

        if (colName.equals(DataTableColumns.ID)) {
          idxRowId = i;
          dbName = DataTableColumns.ID;
        } else if (colName.equals(DataTableColumns.FORM_ID)) {
          idxFormId = i;
          dbName = DataTableColumns.FORM_ID;
        } else if (colName.equals(DataTableColumns.LOCALE)) {
          idxLocale = i;
          dbName = DataTableColumns.LOCALE;
        } else if (colName.equals(LAST_MOD_TIME_LABEL) || colName.equals(DataTableColumns.SAVEPOINT_TIMESTAMP)) {
          idxTimestamp = i;
          dbName = DataTableColumns.SAVEPOINT_TIMESTAMP;
        } else if (colName.equals(ACCESS_CONTROL_LABEL) || colName.equals(DataTableColumns.SAVEPOINT_CREATOR)) {
          idxSavepointCreator = i;
          dbName = DataTableColumns.SAVEPOINT_CREATOR;
        } else {
          Log.d(TAG, "processing column: " + colName);

          ColumnProperties cp = tp.getColumnByDisplayName(colName);
          if (cp == null) {
            // And now add all remaining metadata columns
            if (DbTable.getAdminColumns().contains(colName)) {
              dbName = colName;
            } else if (discoverColumnNames) {
              cp = tp.addColumn(colName, null, null, ColumnType.STRING, null, true);
              dbName = cp.getElementKey();
            } else {
              reader.close();
              throw new IllegalStateException("column name " + colName
                  + " should have been defined in metadata");
            }
          } else {
            dbName = cp.getElementKey();
          }
        }
        columns.add(dbName);
      }
      return importTable(c, importTask, reader, tp, columns, idxRowId, idxFormId, idxLocale, idxTimestamp,
          idxSavepointCreator, includesProperties);
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean importAddToTable(Context c, ImportListener importTask, File file, String tableId) {
    try {
      // This flag indicates if the table was exported with the
      // properties.
      // If so, then we know that it includes the admin/metadata columns.
      boolean includesProperties = false;

      InputStream is;
      try {
        is = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new IllegalStateException(e);
      }
      // Now get the reader.
      InputStreamReader isr;
      try {
        isr = new InputStreamReader(is, Charset.forName(CharEncoding.UTF_8));
      } catch (UnsupportedCharsetException e) {
        Log.w(TAG, "UTF-8 wasn't supported--trying with default charset");
        isr = new InputStreamReader(is);
      }

      CSVReader reader = new CSVReader(isr, DELIMITING_CHAR, QUOTE_CHAR, ESCAPE_CHAR);
      String[] row = reader.readNext();
      if (row.length == 0) {
        reader.close();
        return true;
      }
      TableProperties tp;
      boolean discoverColumnNames = true;
      if (row[0].startsWith(OPEN_CURLY_BRACKET)) {
        includesProperties = true;
        // TODO: it might be that we do NOT want to overwrite an
        // existing
        // table's properties, in which case we shouldn't set from json.
        tp = TableProperties.getTablePropertiesForTable(c, appName, tableId);
        if (tp.setFromJson(row[0])) {
          // OK the metadata is for this tableId, so we can proceed...

          // we need to check if we need to import all the key value store
          // things as well.
          if (row.length > 1) {
            // This is, by convention, the key value store entries in
            // list
            // form.
            try {
              List<OdkTablesKeyValueStoreEntry> recoveredEntries = mapper.readValue(row[1],
                  new TypeReference<List<OdkTablesKeyValueStoreEntry>>() {
                  });
              tp.addMetaDataEntries(recoveredEntries, false);
              // Since the KVS has all the display properties for a table, we
              // must
              // re-read everything to get them.
              tp = TableProperties.refreshTablePropertiesForTable(c, appName, tp.getTableId());
              // TODO: sort out closing database appropriately.
            } catch (JsonGenerationException e) {
              e.printStackTrace();
              if (importTask != null) {
                importTask.importComplete(false);
              }
            } catch (JsonMappingException e) {
              e.printStackTrace();
              if (importTask != null) {
                importTask.importComplete(false);
              }
            } catch (IOException e) {
              e.printStackTrace();
              if (importTask != null) {
                importTask.importComplete(false);
              }
            }
          }
          discoverColumnNames = false;
        }
        row = reader.readNext();
      } else {
        tp = TableProperties.getTablePropertiesForTable(c, appName, tableId);
        discoverColumnNames = true;
      }

      ArrayList<String> columns = new ArrayList<String>();
      int idxRowId = -1;
      int idxTimestamp = -1;
      int idxSavepointCreator = -1;
      int idxFormId = -1;
      int idxLocale = -1;
      for (int i = 0; i < row.length; ++i) {
        String colName = row[i];
        String dbName = null;
        if (colName.equals(DataTableColumns.ID)) {
          idxRowId = i;
          dbName = DataTableColumns.ID;
        } else if (colName.equals(DataTableColumns.FORM_ID)) {
          idxFormId = i;
          dbName = DataTableColumns.FORM_ID;
        } else if (colName.equals(DataTableColumns.LOCALE)) {
          idxLocale = i;
          dbName = DataTableColumns.LOCALE;
        } else if (colName.equals(LAST_MOD_TIME_LABEL)) {
          idxTimestamp = i;
          dbName = DataTableColumns.SAVEPOINT_TIMESTAMP;
        } else if (colName.equals(ACCESS_CONTROL_LABEL)) {
          idxSavepointCreator = i;
          dbName = DataTableColumns.SAVEPOINT_CREATOR;
        } else {
          Log.d(TAG, "processing column: " + colName);

          ColumnProperties cp = tp.getColumnByDisplayName(colName);
          if (cp == null) {
            // And now add all remaining metadata columns
            if (DbTable.getAdminColumns().contains(colName)) {
              dbName = colName;
            } else if (discoverColumnNames) {
              cp = tp.addColumn(colName, null, null, ColumnType.STRING, null, true);
              dbName = cp.getElementKey();
            } else {
              throw new IllegalStateException("column name " + colName
                  + " should have been defined in metadata");
            }
          } else {
            dbName = cp.getElementKey();
          }
          // dbName = cp.getElementKey();
        }
        columns.add(dbName);
      }

      return importTable(c, importTask, reader, tp, columns, idxRowId, idxFormId, idxLocale, idxTimestamp,
          idxSavepointCreator, includesProperties);
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Used by InitializeTask
   *
   * @param InitializeTask
   *          calling this method
   * @param File
   *          .csv file
   * @param String
   *          tablename
   * @return boolean true if successful
   * @throws TableAlreadyExistsException
   */
  public boolean importConfigTables(Context c, ImportListener it, File file, String filename,
                                    String tablename) throws TableAlreadyExistsException {

    if (filename != null) {

      String baseName = null; // the filename without the .csv
      // split on ".csv" to get the filename without extension
      if (filename.endsWith(CSV_FILE_EXTENSION)) {
        // create the file name/path of a .properties.csv file
        // and check if it exits
        String[] tokens = filename.split(CSV_FILE_EXTENSION);
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < tokens.length; i++) {
          s.append(tokens[i]);
        }
        baseName = s.toString();
      }
      String propFilename = baseName += PROPERTIES_CSV_FILE_EXTENSION;

      File csvProp = new File(ODKFileUtils.getAppFolder(appName), propFilename);

      if (csvProp.exists()) {
        try {
          File temp = joinCSVs(csvProp, file);
          boolean success = this.importNewTable(c, null, temp, tablename);
          // delete temporary file
          temp.delete();
          return success;
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      } else {
        // try {
        return this.importNewTable(c, null, file, tablename);
      }
    } else {
      Log.e(t, "bad filename");
      return false;
    }
  }

  private File joinCSVs(File prop, File data) throws IOException {
    File temp = new File(ODKFileUtils.getAppFolder(appName),
                         ODK_TABLES_JOINING_CSV_FILENAME);

    InputStream isProp = null;
    InputStreamReader isrProp = null;
    BufferedReader brProp = null;
    try {
      isProp = new FileInputStream(prop);
      isrProp = new InputStreamReader(isProp, CharEncoding.UTF_8);
      brProp = new BufferedReader(isrProp);
    } catch (FileNotFoundException e1) {
      throw new IllegalStateException(e1);
    } catch (UnsupportedEncodingException uee) {
      Log.w(t, "UTF 8 encoding unavailable, trying default encoding");
      isrProp = new InputStreamReader(isProp);
      brProp = new BufferedReader(isrProp);
    }

    InputStream isData = null;
    InputStreamReader isrData = null;
    BufferedReader brData = null;
    try {
      isData = new FileInputStream(data);
      isrData = new InputStreamReader(isData, CharEncoding.UTF_8);
      brData = new BufferedReader(isrData);
    } catch (FileNotFoundException e1) {
      throw new IllegalStateException(e1);
    } catch (UnsupportedEncodingException uee) {
      Log.w(t, "UTF 8 encoding unavailable, trying default encoding");
      isrData = new InputStreamReader(isData);
      brData = new BufferedReader(isrData);
    }

    OutputStreamWriter output = null;
    try {
      FileOutputStream out = new FileOutputStream(temp);
      output = new OutputStreamWriter(out, CharEncoding.UTF_8);

      // read in each line and add to the temp file
      String line;
      while ((line = brProp.readLine()) != null) {
        output.write(line);
        output.write(NEW_LINE);
      }
      while ((line = brData.readLine()) != null) {
        output.write(line);
        output.write(NEW_LINE);
      }

      output.flush();
      output.close();
      Log.i(t, "Temp file made");
      return temp;
    } finally {
      try {
        brProp.close();
        isrProp.close();
      } catch (IOException e) {
      }
      try {
        brData.close();
        isrData.close();
      } catch (IOException e) {
      }
    }
  }

  private boolean importTable(Context c, ImportListener importListener, CSVReader reader, TableProperties tableProperties,
                              List<String> columns, int idxRowId, int idxFormId, int idxLocale,
                              int idxSavepointTimestamp, int idxSavepointCreator,
                              boolean exportedWithProperties) {

    DbTable dbt = DbTable.getDbTable(tableProperties);

    try {
      Set<Integer> idxMetadata = new HashSet<Integer>();
      idxMetadata.add(idxRowId);
      idxMetadata.add(idxFormId);
      idxMetadata.add(idxLocale);
      idxMetadata.add(idxSavepointTimestamp);
      idxMetadata.add(idxSavepointCreator);
      ColumnProperties[] cps = new ColumnProperties[columns.size()];
      for (int i = 0; i < columns.size(); ++i) {
        if (!idxMetadata.contains(i)) {
          cps[i] = tableProperties.getColumnByElementKey(columns.get(i));
        } else {
          cps[i] = null;
        }
      }

      String[] row = reader.readNext();
      int rowCount = 0;
      while (row != null) {
        String rowId = idxRowId == -1 ? null : row[idxRowId];
        if (rowId == null) {
          rowId = UUID.randomUUID().toString();
        }
        Map<String, String> values = new HashMap<String, String>();
        for (int i = 0; i < columns.size(); i++) {
          if (cps[i] != null) {
            String value = du.validifyValue(cps[i], row[i]);
            values.put(columns.get(i), value);
            ColumnType type = cps[i].getColumnType();
            if (type == ColumnType.IMAGEURI || type == ColumnType.AUDIOURI
                || type == ColumnType.VIDEOURI || type == ColumnType.MIMEURI) {
              value = du.serializeAsMimeUri(c, tableProperties, rowId, type.baseContentType(),
                  value);
            }
          }
        }
        String formId = idxFormId == -1 ? null : row[idxFormId];
        String locale = idxLocale == -1 ? null : row[idxLocale];
        String lastModTime = idxSavepointTimestamp == -1 ? du.formatNowForDb() : row[idxSavepointTimestamp];
        DateTime t = du.parseDateTimeFromDb(lastModTime);
        String savepointCreator = idxSavepointCreator == -1 ? null : row[idxSavepointCreator];
        dbt.addRow(rowId, formId, locale,
            SavepointTypeManipulator.complete(), TableConstants.nanoSecondsFromMillis(t.getMillis()),
            savepointCreator, null, null, null, values );
        if (rowCount % 30 == 0 && importListener != null) {
          importListener.updateLineCount(c.getString(R.string.import_thru_row, 1 + rowCount));
        }
        values.clear();
        rowCount++;
        row = reader.readNext();
      }
      reader.close();
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } catch (IllegalArgumentException e) { // validifyValue
      e.printStackTrace();
      return false;
    }
  }

  // ===========================================================================================
  // EXPORT
  // ===========================================================================================

  /**
   * Export the given tableId.
   * Exports two csv files to the output/csv directory under the appName:
   * <ul>
   * <li>tableid.fileQualifier.csv - data table</li>
   * <li>tableid.fileQualifier.properties.csv - metadata definition of this table</li>
   * </ul>
   * If fileQualifier is null or an empty string, then it emits to
   * <ul>
   * <li>tableid.csv - data table</li>
   * <li>tableid.properties.csv - metadata definition of this table</li>
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

    int numberOfDisplayColumns = tp.getNumberOfDisplayColumns();
    for (int i = 0; i < numberOfDisplayColumns; ++i) {
      ColumnProperties cp = tp.getColumnByIndex(i);
      columns.add(cp.getElementKey());
    }

    // And now add all remaining export columns
    for (String colName : DbTable.getExportColumns()) {
      if (columns.contains(colName)) {
        continue;
      }
      columns.add(colName);
    }

    // getting data
    DbTable dbt = DbTable.getDbTable(tp);
    String whereString = DataTableColumns.SAVEPOINT_TYPE + " IS NOT NULL AND (" +
          DataTableColumns.CONFLICT_TYPE + " IS NULL OR " +
          DataTableColumns.CONFLICT_TYPE + " = " +
          Integer.toString(ConflictType.LOCAL_UPDATED_UPDATED_VALUES) + ")";
    UserTable table = dbt.rawSqlQuery(whereString, null, null, null, null, null);
    // writing data
    OutputStreamWriter output = null;
    try {
      // both files go under the output/csv directory...
      File outputCsv = new File(new File(ODKFileUtils.getOutputFolder(appName)), "csv");
      outputCsv.mkdirs();

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
          row[j] = dataRow.getDataOrMetadataByElementKey(columns.get(j));;
        }
        cw.writeNext(row);
      }
      cw.flush();
      cw.close();

      // emit metadata table...
      file = new File( outputCsv, tp.getTableId() +
          ((fileQualifier != null && fileQualifier.length() != 0) ? ("." + fileQualifier) : "") + ".properties.csv");
      out = new FileOutputStream(file);
      output = new OutputStreamWriter(out, CharEncoding.UTF_8);
      cw = new RFC4180CsvWriter(output);

      String[] blank = {};

      // Emit TableDefinitions SyncTag
      // I don't think we need anything else in the TableDefinitions??
      ArrayList<String> tableDefHeaders = new ArrayList<String>();
      tableDefHeaders.add(TableDefinitionsColumns.SYNC_TAG);

      cw.writeNext(tableDefHeaders.toArray(new String[tableDefHeaders.size()]));
      String[] tableDefRow = new String[tableDefHeaders.size()];
      tableDefRow[0] = (tp.getSyncTag() == null) ? null : tp.getSyncTag().toString();
      cw.writeNext(tableDefRow);

      cw.writeNext(blank);

      // Emit ColumnDefinitions

      ArrayList<String> colDefHeaders = new ArrayList<String>();
      colDefHeaders.add(ColumnDefinitionsColumns.ELEMENT_KEY);
      colDefHeaders.add(ColumnDefinitionsColumns.ELEMENT_NAME);
      colDefHeaders.add(ColumnDefinitionsColumns.ELEMENT_TYPE);
      colDefHeaders.add(ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION);
      colDefHeaders.add(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS);

      cw.writeNext(colDefHeaders.toArray(new String[colDefHeaders.size()]));
      String[] colDefRow = new String[colDefHeaders.size()];
      for ( ColumnProperties cp : tp.getAllColumns().values() ) {
        colDefRow[0] = cp.getElementKey();
        colDefRow[1] = cp.getElementName();
        colDefRow[2] = cp.getColumnType().toString();
        colDefRow[3] = Boolean.toString(cp.isUnitOfRetention());
        colDefRow[4] = ODKFileUtils.mapper.writeValueAsString(cp.getListChildElementKeys());
        cw.writeNext(colDefRow);
      }

      cw.writeNext(blank);

      // Emit KeyValueStore

      ArrayList<String> kvsHeaders = new ArrayList<String>();
      kvsHeaders.add(KeyValueStoreColumns.PARTITION);
      kvsHeaders.add(KeyValueStoreColumns.ASPECT);
      kvsHeaders.add(KeyValueStoreColumns.KEY);
      kvsHeaders.add(KeyValueStoreColumns.VALUE_TYPE);
      kvsHeaders.add(KeyValueStoreColumns.VALUE);

      List<OdkTablesKeyValueStoreEntry> kvsEntries = tp.getMetaDataEntries();
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
    Boolean isUnitOfRetention;
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
   * Export the given tableId.
   * Exports two csv files to the output/csv directory under the appName:
   * <ul>
   * <li>tableid.fileQualifier.csv - data table</li>
   * <li>tableid.fileQualifier.properties.csv - metadata definition of this table</li>
   * </ul>
   * If fileQualifier is null or an empty string, then it emits to
   * <ul>
   * <li>tableid.csv - data table</li>
   * <li>tableid.properties.csv - metadata definition of this table</li>
   * </ul>
   *
   * @param exportListener
   * @param fileQualifier
   * @param tp
   * @return
   */
  public boolean createTableFromCsv(ImportListener importListener, String tableId, String fileQualifier) {

    {
      TableProperties tp = TableProperties.getTablePropertiesForTable(context, appName, tableId);
      if ( tp != null ) {
        throw new IllegalStateException("Unexpectedly found tableId already exists!");
      }
    }

    Log.i(TAG, "createTableFromCsv: tableId: " + tableId + " fileQualifier: " + ((fileQualifier == null) ? "<null>" : fileQualifier) );


    Map<String, ColumnInfo> columns = new HashMap<String, ColumnInfo>();

    // reading data
    InputStreamReader input = null;
    try {
      // both files are read from assets/csv directory...
      File assetsCsv = new File(new File(ODKFileUtils.getAssetsFolder(appName)), "csv");

      // read data table...
      File file = new File( assetsCsv, tableId +
          ((fileQualifier != null && fileQualifier.length() != 0) ? ("." + fileQualifier) : "") + ".properties.csv");
      FileInputStream in = new FileInputStream(file);
      input = new InputStreamReader(in, CharEncoding.UTF_8);
      RFC4180CsvReader cr = new RFC4180CsvReader(input);

      String[] row;

      // Read TableDefinitions SyncTag header
      row = cr.readNext();
      if ( row == null || countUpToLastNonNullElement(row) != 1  ) {
        throw new IllegalStateException("Unexpected row length -- expected one cell on first line");
      }

      if ( !TableDefinitionsColumns.SYNC_TAG.equals(row[0]) ) {
        throw new IllegalStateException("Expected " + TableDefinitionsColumns.SYNC_TAG);
      }

      row = cr.readNext();
      // Read TableDefinitions SyncTag
      SyncTag syncTag = SyncTag.valueOf(row[0]);

      // blank
      row = cr.readNext();

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
          if ( ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION.equals(colHeaders[i]) ) {
            ci.isUnitOfRetention = Boolean.valueOf(row[i]);
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

      // we already read the blank row to get here...

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
          if ( TableProperties.KVS_PARTITION.equals(kvsEntry.partition) &&
               KeyValueStoreHelper.DEFAULT_ASPECT.equals(kvsEntry.aspect) &&
               TableProperties.KEY_DISPLAY_NAME.equals(kvsEntry.key) ) {
            displayName = kvsEntry.value;
          } else {
            kvsEntries.add(kvsEntry);
          }
        }
        // get next row or blank to end...
        row = cr.readNext();
      }
      cr.close();

      TableProperties tp = TableProperties.addTable(context, appName,
          tableId, (displayName == null ? tableId : displayName), tableId);

      tp.addMetaDataEntries(kvsEntries, false);
      for ( ColumnInfo ci : columns.values() ) {
        ColumnProperties cp = tp.addColumn(ci.displayName, ci.elementKey, ci.elementName,
            ci.elementType, ci.listOfStringElementKeys, ci.isUnitOfRetention);
        cp.addMetaDataEntries(ci.kvsEntries);
      }
      tp.setSyncTag(syncTag);
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

  public boolean importSeparable(ImportListener importListener, String tableId, String fileQualifier, boolean createIfNotPresent) {

    TableProperties tp = TableProperties.getTablePropertiesForTable(context, appName, tableId);
    if ( tp == null ) {
      if ( createIfNotPresent ) {
        if ( !createTableFromCsv(importListener, tableId, fileQualifier) ) {
          return false;
        }
        // and now load the TP we just defined...
        tp = TableProperties.getTablePropertiesForTable(context, appName, tableId);
      } else {
        return false;
      }
    }

    Log.i(TAG, "createTableFromCsv: tableId: " + tableId + " fileQualifier: " + ((fileQualifier == null) ? "<null>" : fileQualifier) );

    // building array of columns to select and header row for output file
    // then we are including all the metadata columns.
    ArrayList<String> columns = new ArrayList<String>();

    // put the user-relevant metadata columns in leftmost columns
    columns.add(DataTableColumns.ID);
    columns.add(DataTableColumns.FORM_ID);
    columns.add(DataTableColumns.LOCALE);
    columns.add(DataTableColumns.SAVEPOINT_TYPE);
    columns.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
    columns.add(DataTableColumns.SAVEPOINT_CREATOR);

    int numberOfDisplayColumns = tp.getNumberOfDisplayColumns();
    for (int i = 0; i < numberOfDisplayColumns; ++i) {
      ColumnProperties cp = tp.getColumnByIndex(i);
      columns.add(cp.getElementKey());
    }

    // And now add all remaining metadata columns
    for (String colName : DbTable.getExportColumns()) {
      if (columns.contains(colName)) {
        continue;
      }
      columns.add(colName);
    }

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

      String[] row;
      for (;;) {
        row = cr.readNext();
        if ( row == null || countUpToLastNonNullElement(row) == 0 ) {
          break;
        }
        int rowLength = countUpToLastNonNullElement(row);

        // default values for metadata columns if not provided
        v_id = UUID.randomUUID().toString();
        v_form_id = null;
        v_locale = null;
        v_savepoint_type = SavepointTypeManipulator.complete();
        v_savepoint_creator = null;
        v_savepoint_timestamp = TableConstants.nanoSecondsFromMillis(System.currentTimeMillis());
        v_row_etag = null;
        v_filter_type = null;
        v_filter_value = null;
        // clear value map
        valueMap.clear();

        for ( int i = 0 ; i < columnsInFileLength ; ++i ) {
          if ( i > rowLength ) break;
          String column = columnsInFile[i];
          String tmp = row[i];
          if ( DataTableColumns.ID.equals(column) && (tmp != null && tmp.length() != 0) ) {
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

        /**
         * Insertion will set the SYNC_STATE to inserting.
         *
         * If the table is sync'd to the server, this will cause one
         * sync interaction with the server to confirm that the server
         * also has this record.
         */
        dbTable.addRow(v_id, v_form_id, v_locale,
            v_savepoint_type, v_savepoint_timestamp, v_savepoint_creator,
            v_row_etag, v_filter_type, v_filter_value, valueMap);
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
