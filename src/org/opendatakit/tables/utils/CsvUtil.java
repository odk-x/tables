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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import org.opendatakit.tables.data.Table;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
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

    private static final char DELIMITING_CHAR = ',';
    private static final char QUOTE_CHAR = '\"';
    private static final char ESCAPE_CHAR = '\\';

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
     * store. Doing it another way would give users a workaround to add tables
     * to the server database.
     * @param importTask the ImportTask calling this method. It is used to
     * pass messages to the user in case of error. Null safe.
     * @param file
     * @param tableName
     * @return
     * @throws TableAlreadyExistsException if settings are included and a
     * table already exists with the matching tableId or dbTableName
     */
    public boolean importNewTable(Context c, ImportTask importTask, File file,
        String tableName) throws
      TableAlreadyExistsException {

        String dbTableName = TableProperties.createDbTableName(dbh, tableName);
        TableProperties tp;
        try {
          boolean includesProperties = false;
          // columns contains all columns in the csv file...
          List<String> columns = new ArrayList<String>();
          int idxTimestamp = -1;
          int idxUriUser = -1;
          int idxInstanceName = -1;
          int idxFormId = -1;
          int idxLocale = -1;
            CSVReader reader = new CSVReader(new FileReader(file),
                DELIMITING_CHAR, QUOTE_CHAR, ESCAPE_CHAR);
            String[] row = reader.readNext();
            if (row.length == 0) {
                reader.close();
                return true;
            }
            // adding columns
            if (row[0].startsWith(OPEN_CURLY_BRACKET)) {
              // has been exported with properties.
              includesProperties = true;
              String jsonProperties = row[0];
              // now we need the tableId. It is tempting to just scan the
              // string until we find it, but if there are other occurrences
              // of the tableId json key we will get into trouble. So, we must
              // deserialize it.
              tp = TableProperties.addTableFromJson(dbh, jsonProperties,
                  KeyValueStore.Type.ACTIVE);
              // we need to check if we need to import all the key value store
              // things as well.
              if (row.length > 1) {
                // This is, by convention, the key value store entries in list
                // form.
                try {
                  ObjectMapper mapper = new ObjectMapper();
                  mapper.setVisibilityChecker(mapper.getVisibilityChecker()
                      .withFieldVisibility(Visibility.ANY));
                  List<OdkTablesKeyValueStoreEntry> recoveredEntries =
                      mapper.readValue(row[1],
                          new
                          TypeReference<List<OdkTablesKeyValueStoreEntry>>(){});
                  KeyValueStoreManager kvsm =
                      KeyValueStoreManager.getKVSManager(dbh);
                  KeyValueStore kvs = kvsm.getStoreForTable(tp.getTableId(),
                      tp.getBackingStoreType());
                  kvs.addEntriesToStore(dbh.getWritableDatabase(),
                      recoveredEntries);
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
              row = reader.readNext();
              // now collect all the headings.
              for ( int i = 0 ; i < row.length ; ++i ) {
               	String colName = row[i];
            	if ( colName.equals(LAST_MOD_TIME_LABEL) ) {
              		idxTimestamp = i;
              	}
              	if ( colName.equals(URI_USER_LABEL)) {
              		idxUriUser = i;
              	}
              	if ( colName.equals(INSTANCE_NAME_LABEL)) {
              		idxInstanceName = i;
              	}
              	if ( colName.equals(FORM_ID_LABEL)) {
              		idxFormId = i;
              	}
              	if ( colName.equals(LOCALE_LABEL)) {
              		idxLocale = i;
              	}
	            String dbName = tp.getColumnByDisplayName(colName);
	            columns.add(dbName);
              }
            } else {
              tp = TableProperties.addTable(dbh, dbTableName, dbTableName,
                  tableName, TableType.data, 
                  KeyValueStore.Type.ACTIVE);

              columns = new ArrayList<String>();
              for ( int i = 0 ; i < row.length ; ++i ) {
              	String colName = row[i];
              	String dbName = null;
              	// detect and process all the metadata columns
              	if ( colName.equals(LAST_MOD_TIME_LABEL) ) {
              		idxTimestamp = i;
              		dbName = DataTableColumns.TIMESTAMP;
              	}
              	else if ( colName.equals(URI_USER_LABEL)) {
              		idxUriUser = i;
              		dbName = DataTableColumns.URI_USER;
              	}
              	else if ( colName.equals(INSTANCE_NAME_LABEL)) {
              		idxInstanceName = i;
              		dbName = DataTableColumns.INSTANCE_NAME;
              	}
              	else if ( colName.equals(FORM_ID_LABEL)) {
              		idxFormId = i;
              		dbName = DataTableColumns.FORM_ID;
              	}
              	else if ( colName.equals(LOCALE_LABEL)) {
              		idxLocale = i;
              		dbName = DataTableColumns.LOCALE;
              	}
              	else {
	                dbName = tp.getColumnByDisplayName(colName);
	                if ( dbName == null ) {
	                	tp.addColumn(colName, null, null);
	                	dbName = tp.getColumnByDisplayName(colName);
	                }
              	}
                columns.add(dbName);
              }
            }
            return importTable(c, reader, tp.getTableId(), columns,
            		idxTimestamp, idxUriUser, idxInstanceName, idxFormId, idxLocale,
            		includesProperties);
        } catch(FileNotFoundException e) {
            return false;
        } catch(IOException e) {
            return false;
        }
    }

    public boolean importAddToTable(Context c, File file, String tableId) {
      //TODO is this the correct KVS to get the properties from?
        TableProperties tp = TableProperties.getTablePropertiesForTable(dbh,
                tableId, KeyValueStore.Type.ACTIVE);
        try {
          // This flag indicates if the table was exported with the properties.
          // If so, then we know that it includes the admin/metadata columns.
          boolean includesProperties = false;
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] row = reader.readNext();
            if (row.length == 0) {
                reader.close();
                return true;
            }
            if ((row.length == 1) && row[0].startsWith(OPEN_CURLY_BRACKET)) {
              includesProperties = true;
              //TODO: it might be that we do NOT want to overwrite an existing
              // table's properties, in which case we shouldn't set from json.
                tp.setFromJson(row[0]);
                row = reader.readNext();
            }

            ArrayList<String> columns = new ArrayList<String>();

            int idxTimestamp = -1;
            int idxUriUser = -1;
            int idxInstanceName = -1;
            int idxFormId = -1;
            int idxLocale = -1;
            for ( int i = 0 ; i < row.length ; ++i ) {
            	String colName = row[i];
            	String dbName = null;
            	if ( colName.equals(LAST_MOD_TIME_LABEL) ) {
            		idxTimestamp = i;
            		dbName = DataTableColumns.TIMESTAMP;
            	}
            	else if ( colName.equals(URI_USER_LABEL)) {
            		idxUriUser = i;
            		dbName = DataTableColumns.URI_USER;
            	}
            	else if ( colName.equals(INSTANCE_NAME_LABEL)) {
            		idxInstanceName = i;
            		dbName = DataTableColumns.INSTANCE_NAME;
            	}
            	else if ( colName.equals(FORM_ID_LABEL)) {
            		idxFormId = i;
            		dbName = DataTableColumns.FORM_ID;
            	}
            	else if ( colName.equals(LOCALE_LABEL)) {
            		idxLocale = i;
            		dbName = DataTableColumns.LOCALE;
            	}
            	else {
            		dbName = tp.getColumnByDisplayName(colName);
            		// CHANGE: support adding columns via the import feature...
	                if ( dbName == null ) {
	                	tp.addColumn(colName, null, null);
	                	dbName = tp.getColumnByDisplayName(colName);
	                }
            	}
                columns.add(dbName);
            }

            return importTable(c, reader, tp.getTableId(), columns,
            		idxTimestamp, idxUriUser, idxInstanceName, idxFormId, idxLocale,
                    includesProperties);
        } catch(FileNotFoundException e) {
            return false;
        } catch(IOException e) {
            return false;
        }
    }

    /**
     * Used by InitializeTask
     * @param InitializeTask calling this method
     * @param File .csv file
     * @param String tablename
     * @return boolean true if successful
     */
    public boolean importConfigTables(Context c, InitializeTask it, File file,
    		String filename, String tablename) {

    	this.it = it;

    	// split on ".csv" to get the filename without extension
    	if (filename.endsWith(CSV_FILE_EXTENSION)) {

    		// create the file name/path of a .properties.csv file
    		// and check if it exits
    		String[] tokens = filename.split(CSV_FILE_EXTENSION);
    		StringBuffer s = new StringBuffer();
        	for (int i = 0; i < tokens.length; i++) {
        		s.append(tokens[i]);
        	}
        	String propFilename = s.append(PROPERTIES_CSV_FILE_EXTENSION).toString();

    		File csvProp = new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME), propFilename);

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

    	BufferedReader brp = new BufferedReader(new FileReader(prop));
    	BufferedReader brd = new BufferedReader(new FileReader(data));
    	FileWriter output = new FileWriter(temp);

    	// read in each line and add to the temp file
    	String line;
    	while ((line = brp.readLine()) != null) {
    		output.write(line);
    		output.write(NEW_LINE);
    	}
    	while ((line = brd.readLine()) != null) {
    		output.write(line);
    		output.write(NEW_LINE);
    	}

    	brp.close();
    	brd.close();
    	output.close();

    	Log.i(t, "Temp file made");
    	return temp;
    }

    private boolean importTable(Context c, CSVReader reader, String tableId,
    		List<String> columns,
    		int idxTimestamp, int idxUriUser, int idxInstanceName, int idxFormId, int idxLocale,
            boolean exportedWithProperties) {

    	DbTable dbt = DbTable.getDbTable(dbh, tableId);

    	try {
    		Set<Integer> idxMetadata = new HashSet<Integer>();
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
					if ( !idxMetadata.contains(i) ) {
    					values.put(columns.get(i), row[i]);
					}
				}
				String lastModTime = idxTimestamp == -1 ?
						du.formatNowForDb() : row[idxTimestamp];
				DateTime t = du.parseDateTimeFromDb(lastModTime);
				String uriUser = idxUriUser == -1 ? null : row[idxUriUser];
				String instanceName = idxInstanceName == -1 ? null : row[idxInstanceName];
				String formId = idxFormId == -1 ? null : row[idxFormId];
				String locale = idxLocale == -1 ? null : row[idxLocale];
				dbt.addRow(values, t.getMillis(), uriUser, instanceName, formId, locale);

				if (rowCount % 30 == 0 && it != null) {
					it.updateLineCount(c.getString(R.string.import_thru_row, 1+rowCount));
				}
				values.clear();
    			rowCount++;
    			row = reader.readNext();
    		}
    		reader.close();
    		return true;
    	} catch(IOException e) {
    		e.printStackTrace();
    		return false;
    	}
    }


// ===========================================================================================
//                                          EXPORT
// ===========================================================================================

    /**
     * Export the file.
     * <p>
     * If raw is false, it means that you DO export the settings. In this case
     * the first row is:
     * [json representation of table properties, json of list of kvs entries],
     * and all the column headings are the column element keys or the names of
     * the admin columns in the table.
     * @param exportTask the exportTask to which a message is sent if writing
     * key value store settings fails. null safe.
     * @param file
     * @param tableId
     * @param includeTimestamp
     * @param includeUriUser
     * @param includeInstanceName
     * @param includeFormId
     * @param includeLocale
     * @param exportProperties (automatically includes all the above fields)
     * @return
     */
    public boolean export(ExportTask exportTask, File file, String tableId,
    		boolean includeTimestamp,
            boolean includeUriUser,
            boolean includeInstanceName,
            boolean includeFormId,
            boolean includeLocale,
            boolean exportProperties) {
      //TODO test that this is the correct KVS to get the export from.
        TableProperties tp = TableProperties.getTablePropertiesForTable(dbh,
                tableId, KeyValueStore.Type.ACTIVE);
        // building array of columns to select and header row for output file
        int columnCount = tp.getColumns().length;
        if (exportProperties) {
          // then we are including all the metadata columns.
          columnCount += DbTable.getAdminColumns().size();
        } else {
          if (includeTimestamp) columnCount++;
          if (includeUriUser) columnCount++;
          if (includeInstanceName) columnCount++;
          if (includeFormId) columnCount++;
          if (includeLocale) columnCount++;
        }
        ArrayList<String> columns = new ArrayList<String>();
        ArrayList<String> headerRow = new ArrayList<String>();
        int idxTimestamp = -1;
        int idxUriUser = -1;
        int idxInstanceName = -1;
        int idxFormId = -1;
        int idxLocale = -1;

        int index = 0;
        if (exportProperties) {
        	// put the user-relevant metadata columns in leftmost columns
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

        	ColumnProperties[] colProps = tp.getColumns();
        	for ( int i = 0 ; i < colProps.length ; ++i ) {
        		ColumnProperties cp = colProps[i];
            	String displayName = cp.getDisplayName();
                columns.add(cp.getElementKey());
                headerRow.add(displayName);
                index++;
            }

            // And now add all remaining metadata columns
            for (String colName : DbTable.getAdminColumns()) {
            	if ( columns.contains(colName) ) {
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

            // export everything in the user columns
            for (ColumnProperties cp : tp.getColumns()) {
                columns.add(cp.getElementKey());
                headerRow.add(cp.getElementKey());
                index++;
            }
        }
        // getting data
        DbTable dbt = DbTable.getDbTable(dbh, tableId);
        String[] selectionKeys = { DataTableColumns.SAVED };
        String[] selectionArgs = { DbTable.SavedStatus.COMPLETE.name() };
        Table table = dbt.getRaw(columns, selectionKeys, selectionArgs, null);
        // writing data
        try {
            CSVWriter cw = new CSVWriter(new FileWriter(file), DELIMITING_CHAR,
                QUOTE_CHAR, ESCAPE_CHAR);
            if (exportProperties) {
              // The first row must be [tableProperties, secondaryKVSEntries]
              // The tableProperties json is easily had,
              // so first we must get the secondary entries.
              KeyValueStoreManager kvsm =
                  KeyValueStoreManager.getKVSManager(dbh);
              KeyValueStore kvs = kvsm.getStoreForTable(tp.getTableId(),
                  tp.getBackingStoreType());
              List<String> partitions =
                  kvs.getAllPartitions(dbh.getReadableDatabase());
              // TODO sort out and handle appropriate closing of database
              // We do NOT want to include the table or column partitions.
              partitions.remove(TableProperties.KVS_PARTITION);
              partitions.remove(ColumnProperties.KVS_PARTITION);
              List<OdkTablesKeyValueStoreEntry> kvsEntries =
                  kvs.getEntriesForPartitions(dbh.getReadableDatabase(),
                      partitions);
              // TODO sort out and handle appropriate closing of database
              ObjectMapper mapper = new ObjectMapper();
              mapper.setVisibilityChecker(mapper.getVisibilityChecker()
                  .withFieldVisibility(Visibility.ANY));
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
                settingsRow = new String[] {tp.toJson()};
              } else {
                // something was mapped
                settingsRow = new String[] {tp.toJson(), strKvsEntries};
              }
              cw.writeNext(settingsRow);
            }
            cw.writeNext(headerRow.toArray(new String[headerRow.size()]));
            String[] row = new String[columnCount];
            for (int i = 0; i < table.getHeight(); i++) {
                for (int j = 0; j < table.getWidth(); j++) {
                    row[j] = table.getData(i, j);
                }
                if ( idxTimestamp != -1 ) {
                	// reformat the timestamp to be a nice string
                	Long timestamp = Long.valueOf(row[idxTimestamp]);
                	DateTime dt = new DateTime(timestamp);
                	row[idxTimestamp] = du.formatDateTimeForDb(dt);
                }
                cw.writeNext(row);
            }
            cw.close();
            return true;
        } catch(IOException e) {
            return false;
        }
    }

}
