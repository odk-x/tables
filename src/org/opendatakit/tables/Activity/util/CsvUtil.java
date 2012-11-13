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
import java.util.Map;

import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Table;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;

import android.content.Context;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;


public class CsvUtil {

    private static final String LAST_MOD_TIME_LABEL = "_ts";
    private static final String SRC_PHONE_LABEL = "_pn";
    
    private final DataUtil du;
    private final DbHelper dbh;
    
    public CsvUtil(Context context) {
        du = DataUtil.getDefaultDataUtil();
        dbh = DbHelper.getDbHelper(context);
    }
    
    /**
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
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] row = reader.readNext();
            if (row.length == 0) {
                reader.close();
                return true;
            }
            // adding columns
            if ((row.length == 1) && (row[0].startsWith("{"))) {
                tp.setFromJson(row[0]);
                row = reader.readNext();
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
            }
            boolean includeTs = row[0].equals(LAST_MOD_TIME_LABEL);
            boolean includePn = (!includeTs || (row.length > 1)) &&
                    row[includeTs ? 1 : 0].equals(SRC_PHONE_LABEL);
            return importTable(reader, tp.getTableId(), tp.getColumnOrder(),
                    includeTs, includePn);
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
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] row = reader.readNext();
            if (row.length == 0) {
                reader.close();
                return true;
            }
            if ((row.length == 1) && row[0].startsWith("{")) {
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
            return importTable(reader, tableId, columns, includeTs, includePn);
        } catch(FileNotFoundException e) {
            return false;
        } catch(IOException e) {
            return false;
        }
    }
    
    private boolean importTable(CSVReader reader, String tableId,
            ArrayList<String> columns, boolean includeTs, boolean includePn) {
        int tsIndex = includeTs ? 0 : -1;
        int pnIndex = includePn ? (includeTs ? 1 : 0) : -1;
        int startIndex = (includeTs ? 1 : 0) + (includePn ? 1 : 0);
        DbTable dbt = DbTable.getDbTable(dbh, tableId);
        try {
            Map<String, String> values = new HashMap<String, String>();
            String[] row = reader.readNext();
            while (row != null) {
                for (int i = 0; i < columns.size(); i++) {
                    values.put(columns.get(i), row[startIndex + i]);
                }
                String lastModTime = tsIndex == -1 ?
                        du.formatNowForDb() : row[tsIndex];
                String srcPhone = pnIndex == -1 ? null : row[pnIndex];
                dbt.addRow(values, lastModTime, srcPhone);
                values.clear();
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
        int columnCount = tp.getColumns().length + (includeTs ? 1 : 0) +
                (includePn ? 1 : 0);
        ArrayList<String> columns = new ArrayList<String>();
        ArrayList<String> headerRow = new ArrayList<String>();
        int index = 0;
        if (includeTs) {
            columns.add(DbTable.DB_LAST_MODIFIED_TIME);
            headerRow.add(LAST_MOD_TIME_LABEL);
            index++;
        }
        if (includePn) {
            columns.add(DbTable.DB_SRC_PHONE_NUMBER);
            headerRow.add(SRC_PHONE_LABEL);
            index++;
        }
        if (raw) {
            for (ColumnProperties cp : tp.getColumns()) {
                columns.add(cp.getColumnDbName());
                headerRow.add(cp.getDisplayName());
                index++;
            }
        } else {
            for (ColumnProperties cp : tp.getColumns()) {
                columns.add(cp.getColumnDbName());
                headerRow.add(cp.getColumnDbName());
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
            CSVWriter cw = new CSVWriter(new FileWriter(file));
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
