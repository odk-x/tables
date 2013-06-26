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
package org.opendatakit.tables.utils;

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
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.exceptions.TableAlreadyExistsException;
import org.opendatakit.tables.tasks.ExportTask;
import org.opendatakit.tables.tasks.ImportTask;
import org.opendatakit.tables.tasks.InitializeTask;

import android.content.Context;
import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Various utilities for importing/exporting tables from/to CSV.
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 *
 */
public class CsvUtil {

  private static final String UTF_8 = "UTF-8";

  private static final String TAG = CsvUtil.class.getSimpleName();

  private static final String NEW_LINE = "\n";

  private static final String OPEN_CURLY_BRACKET = "{";

  private static final String PROPERTIES_CSV_FILE_EXTENSION = ".properties.csv";

  private static final String CSV_FILE_EXTENSION = ".csv";

  private static final String t = "CsvUtil";

  private static final String LAST_MOD_TIME_LABEL = "last_mod_time";
  private static final String URI_USER_LABEL = DataTableColumns.URI_USER;
  private static final String INSTANCE_NAME_LABEL = DataTableColumns.INSTANCE_NAME;
  private static final String FORM_ID_LABEL = DataTableColumns.FORM_ID;
  private static final String LOCALE_LABEL = DataTableColumns.LOCALE;
  private static final String ROW_ID_LABEL = DataTableColumns.ROW_ID;

  private static final char DELIMITING_CHAR = ',';
  private static final char QUOTE_CHAR = '\"';
  private static final char ESCAPE_CHAR = '\\';

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.setVisibilityChecker(mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
  }

  // reference to the InitializeTask that called CsvUtil
  private InitializeTask it = null;

  private final DataUtil du;
  private final DbHelper dbh;

  public CsvUtil(Context context) {
    du = DataUtil.getDefaultDataUtil();
    dbh = DbHelper.getDbHelper(context);
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
   *           if settings are included and a table already exists with the
   *           matching tableId or dbTableName
   */
  public boolean importNewTable(Context c, ImportTask importTask, File file, String tableName)
      throws TableAlreadyExistsException {

    String dbTableName = TableProperties.createDbTableName(dbh, tableName);
    TableProperties tp;
    try {
      boolean includesProperties = false;
      // columns contains all columns in the csv file...
      List<String> columns = new ArrayList<String>();
      int idxRowId = -1;
      int idxTimestamp = -1;
      int idxUriUser = -1;
      int idxInstanceName = -1;
      int idxFormId = -1;
      int idxLocale = -1;

      InputStream is;
      try {
        is = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new IllegalStateException(e);
      }
      // Now get the reader.
      InputStreamReader isr;
      try {
        isr = new InputStreamReader(is, Charset.forName(FileUtils.UTF8));
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
        tp = TableProperties.addTableFromJson(dbh, jsonProperties, KeyValueStore.Type.ACTIVE);
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
            KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
            KeyValueStore kvs = kvsm.getStoreForTable(tp.getTableId(), tp.getBackingStoreType());
            kvs.addEntriesToStore(dbh.getWritableDatabase(), recoveredEntries);
            // Since the KVS has all the display properties for a table, we must
            // re-read everything to get them.
            tp = TableProperties.getTablePropertiesForTable(dbh, tp.getTableId(),
                tp.getBackingStoreType());
            // TODO: sort out closing database appropriately.
          } catch (JsonGenerationException e) {
            e.printStackTrace();
            if (importTask != null) {
              importTask.problemImportingKVSEntries = true;
            }
          } catch (JsonMappingException e) {
            e.printStackTrace();
            if (importTask != null) {
              importTask.problemImportingKVSEntries = true;
            }
          } catch (IOException e) {
            e.printStackTrace();
            if (importTask != null) {
              importTask.problemImportingKVSEntries = true;
            }
          }
        }
        discoverColumnNames = false;
        row = reader.readNext();
      } else {
        tp = TableProperties.addTable(dbh, dbTableName, dbTableName, tableName, TableType.data,
            KeyValueStore.Type.ACTIVE);
        discoverColumnNames = true;
      }

      // now collect all the headings.
      for (int i = 0; i < row.length; ++i) {
        String colName = row[i];
        String dbName = null;

        if (colName.equals(ROW_ID_LABEL)) {
          idxRowId = i;
          dbName = DataTableColumns.ROW_ID;
        } else if (colName.equals(LAST_MOD_TIME_LABEL)) {
          idxTimestamp = i;
          dbName = DataTableColumns.TIMESTAMP;
        } else if (colName.equals(URI_USER_LABEL)) {
          idxUriUser = i;
          dbName = DataTableColumns.URI_USER;
        } else if (colName.equals(INSTANCE_NAME_LABEL)) {
          idxInstanceName = i;
          dbName = DataTableColumns.INSTANCE_NAME;
        } else if (colName.equals(FORM_ID_LABEL)) {
          idxFormId = i;
          dbName = DataTableColumns.FORM_ID;
        } else if (colName.equals(LOCALE_LABEL)) {
          idxLocale = i;
          dbName = DataTableColumns.LOCALE;
        } else {
          Log.d(TAG, "processing column: " + colName);

          ColumnProperties cp = tp.getColumnByDisplayName(colName);
          if (cp == null) {
            // And now add all remaining metadata columns
            if ( DbTable.getAdminColumns().contains(colName) ) {
              dbName = colName;
            } else if (discoverColumnNames) {
              cp = tp.addColumn(colName, null, null);
              dbName = cp.getElementKey();
            } else {
              throw new IllegalStateException("column name " + colName
                  + " should have been defined in metadata");
            }
          } else {
            dbName = cp.getElementKey();
          }
        }
        columns.add(dbName);
      }
      return importTable(c, reader, tp, columns, idxRowId, idxTimestamp, idxUriUser,
          idxInstanceName, idxFormId, idxLocale, includesProperties);
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean importAddToTable(Context c, File file, TableProperties tp) {
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
        isr = new InputStreamReader(is, Charset.forName(FileUtils.UTF8));
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
      if ((row.length == 1) && row[0].startsWith(OPEN_CURLY_BRACKET)) {
        includesProperties = true;
        // TODO: it might be that we do NOT want to overwrite an
        // existing
        // table's properties, in which case we shouldn't set from json.
        tp.setFromJson(row[0]);
        row = reader.readNext();
      }

      ArrayList<String> columns = new ArrayList<String>();

      int idxRowId = -1;
      int idxTimestamp = -1;
      int idxUriUser = -1;
      int idxInstanceName = -1;
      int idxFormId = -1;
      int idxLocale = -1;
      for (int i = 0; i < row.length; ++i) {
        String colName = row[i];
        String dbName = null;
        if (colName.equals(LAST_MOD_TIME_LABEL)) {
          idxTimestamp = i;
          dbName = DataTableColumns.TIMESTAMP;
        } else if (colName.equals(ROW_ID_LABEL)) {
          idxRowId = i;
          dbName = DataTableColumns.ROW_ID;
        } else if (colName.equals(URI_USER_LABEL)) {
          idxUriUser = i;
          dbName = DataTableColumns.URI_USER;
        } else if (colName.equals(INSTANCE_NAME_LABEL)) {
          idxInstanceName = i;
          dbName = DataTableColumns.INSTANCE_NAME;
        } else if (colName.equals(FORM_ID_LABEL)) {
          idxFormId = i;
          dbName = DataTableColumns.FORM_ID;
        } else if (colName.equals(LOCALE_LABEL)) {
          idxLocale = i;
          dbName = DataTableColumns.LOCALE;
        } else {
          ColumnProperties cp = tp.getColumnByDisplayName(colName);
          // CHANGE: support adding columns via the import feature...
          if (cp == null) {
            tp.addColumn(colName, null, null);
            cp = tp.getColumnByDisplayName(colName);
          }
          dbName = cp.getElementKey();
        }
        columns.add(dbName);
      }

      return importTable(c, reader, tp, columns, idxRowId, idxTimestamp, idxUriUser,
          idxInstanceName, idxFormId, idxLocale, includesProperties);
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
   */
  public boolean importConfigTables(Context c, InitializeTask it, File file, String filename,
      String tablename) {

    this.it = it;

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

      File csvProp = new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME),
          propFilename);

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
        } catch (TableAlreadyExistsException e) {
          e.printStackTrace();

          return false;
        }
      } else {
        try {
          return this.importNewTable(c, null, file, tablename);
        } catch (TableAlreadyExistsException e) {
          e.printStackTrace();
          return false;
        }
      }
    } else {
      Log.e(t, "bad filename");
      return false;
    }
  }

  private File joinCSVs(File prop, File data) throws IOException {
    File temp = new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME),
        TableFileUtils.ODK_TABLES_JOINING_CSV_FILENAME);

    InputStream isProp = null;
    InputStreamReader isrProp = null;
    BufferedReader brProp = null;
    try {
      isProp = new FileInputStream(prop);
      isrProp = new InputStreamReader(isProp, UTF_8);
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
      isrData = new InputStreamReader(isData, UTF_8);
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
      output = new OutputStreamWriter(out, UTF_8);

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

  private boolean importTable(Context c, CSVReader reader, TableProperties tableProperties,
      List<String> columns, int idxRowId, int idxTimestamp, int idxUriUser, int idxInstanceName,
      int idxFormId, int idxLocale, boolean exportedWithProperties) {

    DbTable dbt = DbTable.getDbTable(dbh, tableProperties);

    try {
      Set<Integer> idxMetadata = new HashSet<Integer>();
      idxMetadata.add(idxRowId);
      idxMetadata.add(idxTimestamp);
      idxMetadata.add(idxUriUser);
      idxMetadata.add(idxInstanceName);
      idxMetadata.add(idxFormId);
      idxMetadata.add(idxLocale);
      String[] row = reader.readNext();
      int rowCount = 0;
      while (row != null) {
        Map<String, String> values = new HashMap<String, String>();
        for (int i = 0; i < columns.size(); i++) {
          if (!idxMetadata.contains(i)) {
            values.put(columns.get(i), row[i]);
          }
        }
        String lastModTime = idxTimestamp == -1 ? du.formatNowForDb() : row[idxTimestamp];
        DateTime t = du.parseDateTimeFromDb(lastModTime);
        String uriUser = idxUriUser == -1 ? null : row[idxUriUser];
        String instanceName = idxInstanceName == -1 ? null : row[idxInstanceName];
        String rowId = idxRowId == -1 ? null : row[idxRowId];
        String formId = idxFormId == -1 ? null : row[idxFormId];
        String locale = idxLocale == -1 ? null : row[idxLocale];
        dbt.addRow(values, rowId, t.getMillis(), uriUser, instanceName, formId, locale);

        if (rowCount % 30 == 0 && it != null) {
          it.updateLineCount(c.getString(R.string.import_thru_row, 1 + rowCount));
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
    }
  }

  // ===========================================================================================
  // EXPORT
  // ===========================================================================================

  /**
   * Export the file.
   * <p>
   * If raw is false, it means that you DO export the settings. In this case the
   * first row is: [json representation of table properties, json of list of kvs
   * entries], and all the column headings are the column element keys or the
   * names of the admin columns in the table.
   *
   * @param exportTask
   *          the exportTask to which a message is sent if writing key value
   *          store settings fails. null safe.
   * @param file
   * @param tableId
   * @param includeTimestamp
   * @param includeUriUser
   * @param includeInstanceName
   * @param includeFormId
   * @param includeLocale
   * @param exportProperties
   *          (automatically includes all the above fields)
   * @return
   */
  public boolean export(ExportTask exportTask, File file, TableProperties tp,
      boolean includeTimestamp, boolean includeUriUser, boolean includeInstanceName,
      boolean includeFormId, boolean includeLocale, boolean exportProperties) {
    // building array of columns to select and header row for output file
    int columnCount = tp.getColumns().size();
    if (exportProperties) {
      // then we are including all the metadata columns.
      columnCount += DbTable.getAdminColumns().size();
    } else {
      if (includeTimestamp)
        columnCount++;
      if (includeUriUser)
        columnCount++;
      if (includeInstanceName)
        columnCount++;
      if (includeFormId)
        columnCount++;
      if (includeLocale)
        columnCount++;
    }
    int idxFirstUserColumns;
    ArrayList<String> userColumns = new ArrayList<String>();
    ArrayList<String> columns = new ArrayList<String>();
    ArrayList<String> headerRow = new ArrayList<String>();
    int idxRowId = -1;
    int idxTimestamp = -1;
    int idxUriUser = -1;
    int idxInstanceName = -1;
    int idxFormId = -1;
    int idxLocale = -1;

    int index = 0;
    if (exportProperties) {
      // put the user-relevant metadata columns in leftmost columns
      {
        columns.add(DataTableColumns.ROW_ID);
        headerRow.add(ROW_ID_LABEL);
        idxRowId = index;
        index++;
      }

      {
        columns.add(DataTableColumns.TIMESTAMP);
        headerRow.add(LAST_MOD_TIME_LABEL);
        idxTimestamp = index;
        index++;
      }

      {
        columns.add(DataTableColumns.URI_USER);
        headerRow.add(URI_USER_LABEL);
        idxUriUser = index;
        index++;
      }

      {
        columns.add(DataTableColumns.INSTANCE_NAME);
        headerRow.add(INSTANCE_NAME_LABEL);
        idxInstanceName = index;
        index++;
      }

      {
        columns.add(DataTableColumns.FORM_ID);
        headerRow.add(FORM_ID_LABEL);
        idxFormId = index;
        index++;
      }

      {
        columns.add(DataTableColumns.LOCALE);
        headerRow.add(LOCALE_LABEL);
        idxLocale = index;
        index++;
      }

      idxFirstUserColumns = index;
      int numberOfDisplayColumns = tp.getNumberOfDisplayColumns();
      for (int i = 0; i < numberOfDisplayColumns; ++i) {
        ColumnProperties cp = tp.getColumnByIndex(i);
        String displayName = cp.getDisplayName();
        userColumns.add(cp.getElementKey());
        columns.add(cp.getElementKey());
        headerRow.add(displayName);
        index++;
      }

      // And now add all remaining metadata columns
      for (String colName : DbTable.getAdminColumns()) {
        if (columns.contains(colName)) {
          continue;
        }
        String displayName = colName;
        columns.add(colName);
        headerRow.add(displayName);
        index++;
      }
    } else {
      if (includeTimestamp) {
        columns.add(DataTableColumns.TIMESTAMP);
        headerRow.add(LAST_MOD_TIME_LABEL);
        idxTimestamp = index;
        index++;
      }
      if (includeUriUser) {
        columns.add(DataTableColumns.URI_USER);
        headerRow.add(URI_USER_LABEL);
        idxUriUser = index;
        index++;
      }
      if (includeInstanceName) {
        columns.add(DataTableColumns.INSTANCE_NAME);
        headerRow.add(INSTANCE_NAME_LABEL);
        idxInstanceName = index;
        index++;
      }
      if (includeFormId) {
        columns.add(DataTableColumns.FORM_ID);
        headerRow.add(FORM_ID_LABEL);
        idxFormId = index;
        index++;
      }
      if (includeLocale) {
        columns.add(DataTableColumns.LOCALE);
        headerRow.add(LOCALE_LABEL);
        idxLocale = index;
        index++;
      }

      idxFirstUserColumns = index;
      // export everything in the user columns
      for (ColumnProperties cp : tp.getColumns().values()) {
        userColumns.add(cp.getElementKey());
        columns.add(cp.getElementKey());
        headerRow.add(cp.getElementKey());
        index++;
      }
    }
    // getting data
    DbTable dbt = DbTable.getDbTable(dbh, tp);
    String[] selectionKeys = { DataTableColumns.SAVED };
    String[] selectionArgs = { DbTable.SavedStatus.COMPLETE.name() };
    UserTable table = dbt.getRaw(userColumns, selectionKeys, selectionArgs, null);
    // writing data
    OutputStreamWriter output = null;
    try {
      FileOutputStream out = new FileOutputStream(file);
      output = new OutputStreamWriter(out, UTF_8);
      CSVWriter cw = new CSVWriter(output, DELIMITING_CHAR, QUOTE_CHAR, ESCAPE_CHAR);
      if (exportProperties) {
        // TODO: INSTEAD USE A TABLE DEFINITION PROBABLY?
        // The first row must be [tableProperties, secondaryKVSEntries]
        // The tableProperties json is easily had,
        // so first we must get the secondary entries.
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStore kvs = kvsm.getStoreForTable(tp.getTableId(), tp.getBackingStoreType());
        List<String> partitions = kvs.getAllPartitions(dbh.getReadableDatabase());
        // TODO sort out and handle appropriate closing of database
        // We do NOT want to include the table or column partitions.
        // partitions.remove(TableProperties.KVS_PARTITION);
        // partitions.remove(ColumnProperties.KVS_PARTITION);
        List<OdkTablesKeyValueStoreEntry> kvsEntries = kvs.getEntriesForPartitions(
            dbh.getReadableDatabase(), partitions);
        // TODO sort out and handle appropriate closing of database
        String[] settingsRow;
        String strKvsEntries = null;
        try {
          strKvsEntries = mapper.writeValueAsString(kvsEntries);
        } catch (JsonGenerationException e) {
          e.printStackTrace();
          if (exportTask != null) {
            exportTask.keyValueStoreSuccessful = false;
          }
        } catch (JsonMappingException e) {
          e.printStackTrace();
          if (exportTask != null) {
            exportTask.keyValueStoreSuccessful = false;
          }
        } catch (IOException e) {
          e.printStackTrace();
          if (exportTask != null) {
            exportTask.keyValueStoreSuccessful = false;
          }
        }
        if (strKvsEntries == null) {
          // mapping failed
          settingsRow = new String[] { tp.toJson() };
        } else {
          // something was mapped
          settingsRow = new String[] { tp.toJson(), strKvsEntries };
        }
        cw.writeNext(settingsRow);
      }
      cw.writeNext(headerRow.toArray(new String[headerRow.size()]));
      String[] row = new String[columnCount];
      for (int i = 0; i < table.getHeight(); i++) {
        for (int j = 0; j < columns.size(); ++j) {
          if (j >= idxFirstUserColumns && j < idxFirstUserColumns + userColumns.size()) {
            row[j] = table.getData(i, j - idxFirstUserColumns);
          } else {
            row[j] = table.getMetadataByElementKey(i, columns.get(j));
          }
        }
        if (idxTimestamp != -1) {
          // reformat the timestamp to be a nice string
          Long timestamp = Long.valueOf(row[idxTimestamp]);
          DateTime dt = new DateTime(timestamp);
          row[idxTimestamp] = du.formatDateTimeForDb(dt);
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

}
