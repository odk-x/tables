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
package org.opendatakit.tables.Activity.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Table;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;

import android.content.ContentValues;
import android.content.Context;
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

    private static final String LAST_MOD_TIME_LABEL = 
        DbTable.DB_LAST_MODIFIED_TIME;
    private static final String SRC_PHONE_LABEL = DbTable.DB_URI_USER;
    
    private static final char DELIMITING_CHAR = ",".charAt(0);
    private static final char QUOTE_CHAR = "\"".charAt(0);
    private static final char ESCAPE_CHAR = "\\".charAt(0);
    
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
     * @param file
     * @param tableName
     * @return
     */
    public boolean importNewTable(File file, String tableName) {
        String dbTableName = TableProperties.createDbTableName(dbh, tableName);
        TableProperties tp = TableProperties.addTable(dbh, dbTableName,
                tableName, TableType.data, 
                KeyValueStore.Type.ACTIVE);
        try {
          boolean includesProperties = false;
          // these columns will either be just those present in TablePropeties,
          // if it was not exported with properties, or it will be all the 
          // columns in heading row, which will be user and admin columns.
          List<String> columns;
            CSVReader reader = new CSVReader(new FileReader(file),
                DELIMITING_CHAR, QUOTE_CHAR, ESCAPE_CHAR);
            String[] row = reader.readNext();
            if (row.length == 0) {
                reader.close();
                return true;
            }
            // adding columns
            if (row[0].startsWith("{")) {
              // then it has been exported with properties.
              includesProperties = true;
              // It is possible that the read CSV has broken up what was output
              // as a single cell into a longer cell, due a missed case of 
              // exporting all the commas in a json string. Therefore we 
              // recombine it to be a single string.
              StringBuilder builder = new StringBuilder();
              for (int i = 0; i < row.length; i++) {
                builder.append(row[i]);
              }
              String jsonProperties = builder.toString();
              tp.setFromJson(jsonProperties);
              row = reader.readNext();
              // now collect all the headings.
              columns = new ArrayList<String>();
              for (String columnHeading : row) {
                columns.add(columnHeading);
              }
            } else {
                int startIndex = 0;
                if (row[startIndex].equals(LAST_MOD_TIME_LABEL)) {
                    startIndex++;
                }
                if ((row.length > startIndex) &&
                        row[startIndex].equals(SRC_PHONE_LABEL)) {
                    startIndex++;
                }
                for (int i = startIndex; i < row.length; i++) {
                    tp.addColumn(row[i], null, null);
                }
                columns = tp.getColumnOrder();
            }
            boolean includeTs = row[0].equals(LAST_MOD_TIME_LABEL);
            boolean includePn = (!includeTs || (row.length > 1)) &&
                    row[includeTs ? 1 : 0].equals(SRC_PHONE_LABEL);
            return importTable(reader, tp.getTableId(), columns,
                    includeTs, includePn, includesProperties);
        } catch(FileNotFoundException e) {
            return false;
        } catch(IOException e) {
            return false;
        }
    }
    
    public boolean importAddToTable(File file, String tableId) {
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
            if ((row.length == 1) && row[0].startsWith("{")) {
              includesProperties = true;
              //TODO: it might be that we do NOT want to overwrite an existing
              // table's properties, in which case we shouldn't set from json.
                tp.setFromJson(row[0]);
                row = reader.readNext();
            }
            boolean includeTs = row[0].equals(LAST_MOD_TIME_LABEL);
            boolean includePn = (row.length > (includeTs ? 1 : 0)) &&
                    row[includeTs ? 1 : 0].equals(SRC_PHONE_LABEL);
            int startIndex = (includeTs ? 1 : 0) + (includePn ? 1 : 0);
            ArrayList<String> columns = new ArrayList<String>();
            for (int i = 0; i < columns.size(); i++) {
                String displayName = row[startIndex + i];
                String dbName = tp.getColumnByDisplayName(displayName);
                columns.add(dbName);
            }
            return importTable(reader, tableId, columns, includeTs, includePn,
                includesProperties);
        } catch(FileNotFoundException e) {
            return false;
        } catch(IOException e) {
            return false;
        }
    }
    
    private boolean importTable(CSVReader reader, String tableId,
            List<String> columns, boolean includeTs, boolean includePn,
            boolean exportedWithProperties) {
        int tsIndex = includeTs ? 0 : -1;
        int pnIndex = includePn ? (includeTs ? 1 : 0) : -1;
        int startIndex = (includeTs ? 1 : 0) + (includePn ? 1 : 0);
        DbTable dbt = DbTable.getDbTable(dbh, tableId);
        try {
            String[] row = reader.readNext();
            while (row != null) {
              if (!exportedWithProperties) {
                Map<String, String> values = new HashMap<String, String>();
                for (int i = 0; i < columns.size(); i++) {
                    values.put(columns.get(i), row[startIndex + i]);
                }
                String lastModTime = tsIndex == -1 ?
                        du.formatNowForDb() : row[tsIndex];
                String srcPhone = pnIndex == -1 ? null : row[pnIndex];
                 dbt.addRow(values, lastModTime, srcPhone);
                values.clear();
              } else {
                // it was exported with properties, and we need to include ALL
                // the columns.
                ContentValues values = new ContentValues();
                for (int i = 0; i < columns.size(); i++) {
                  values.put(columns.get(i), row[i]);
                }
                dbt.actualAddRow(values);
                values.clear();
              }
              row = reader.readNext();
            }
            reader.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean export(File file, String tableId, boolean includeTs,
            boolean includePn) {
        return export(file, tableId, includeTs, includePn, true);
    }
    
    public boolean exportWithProperties(File file, String tableId,
            boolean includeTs, boolean includePn) {
        return export(file, tableId, includeTs, includePn, false);
    }
    
    private boolean export(File file, String tableId, boolean includeTs,
            boolean includePn, boolean raw) {
      //TODO test that this is the correct KVS to get the export from.
        TableProperties tp = TableProperties.getTablePropertiesForTable(dbh,
                tableId, KeyValueStore.Type.ACTIVE);
        // building array of columns to select and header row for output file
        int columnCount = tp.getColumns().length;
        if (!raw) {
          // then we are including all the metadata columns.
          columnCount += DbTable.getAdminColumns().size();
        } else {
          // we're only including the user columns and the optional phone 
          // number and time stamp.
          if (includeTs) columnCount++;
          if (includePn) columnCount++;
        }
        ArrayList<String> columns = new ArrayList<String>();
        ArrayList<String> headerRow = new ArrayList<String>();
        int index = 0;
        // TODO: here we'll want to actually include instance name as well...
        // I think we'll be trying to include every column that also goes to 
        // the server. I'm not sure how this works, so I am leaving it for 
        // now.
        if (includeTs) {
            columns.add(DbTable.DB_LAST_MODIFIED_TIME);
            headerRow.add(LAST_MOD_TIME_LABEL);
            index++;
        }
        if (includePn) {
            columns.add(DbTable.DB_URI_USER);
            headerRow.add(SRC_PHONE_LABEL);
            index++;
        }
        if (raw) {
            for (ColumnProperties cp : tp.getColumns()) {
                columns.add(cp.getElementKey());
                headerRow.add(cp.getDisplayName());
                index++;
            }
        } else {
          // Here there are two sets of things we want to export: 
          // 1. The elementKeys of the user columns.
          // 2. ALL the metadata columns.
          // confusingly, raw == false means use the elementKey.
            for (ColumnProperties cp : tp.getColumns()) {
                columns.add(cp.getElementKey());
                headerRow.add(cp.getElementKey());
                index++;
            }
            // And now add all the metadata columns EXCEPT the two we've added
            // up above iff we've already added them.
            for (String metadataHeading : DbTable.getAdminColumns()) {
              if (includeTs && metadataHeading.equals(LAST_MOD_TIME_LABEL)) {
                // we've already added it.
                continue;
              }
              if (includePn && metadataHeading.equals(SRC_PHONE_LABEL)) {
                continue;
              }
              columns.add(metadataHeading);
              headerRow.add(metadataHeading);
              index++;
            }
        }
        // getting data
        DbTable dbt = DbTable.getDbTable(dbh, tableId);
        String[] selectionKeys = { DbTable.DB_SAVED };
        String[] selectionArgs = { DbTable.SavedStatus.COMPLETE.name() };
        Table table = dbt.getRaw(columns, selectionKeys, selectionArgs, null);
        // writing data
        try {
            CSVWriter cw = new CSVWriter(new FileWriter(file), DELIMITING_CHAR,
                QUOTE_CHAR, ESCAPE_CHAR);
            if (!raw) {
                cw.writeNext(new String[] {tp.toJson()});
            }
            cw.writeNext(headerRow.toArray(new String[headerRow.size()]));
            String[] row = new String[columnCount];
            for (int i = 0; i < table.getHeight(); i++) {
                for (int j = 0; j < table.getWidth(); j++) {
                    row[j] = table.getData(i, j);
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
