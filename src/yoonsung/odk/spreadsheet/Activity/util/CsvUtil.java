package yoonsung.odk.spreadsheet.Activity.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DataUtil;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Table;
import yoonsung.odk.spreadsheet.data.TableProperties;


public class CsvUtil {

    private static final String LAST_MOD_TIME_LABEL = "_ts";
    private static final String SRC_PHONE_LABEL = "_pn";
    
    private final DataUtil du;
    private final DbHelper dbh;
    
    public CsvUtil(Context context) {
        du = DataUtil.getDefaultDataUtil();
        dbh = DbHelper.getDbHelper(context);
    }
    
    public boolean importNewTable(File file, String tableName) {
        String dbTableName = TableProperties.createDbTableName(dbh, tableName);
        TableProperties tp = TableProperties.addTable(dbh, dbTableName,
                tableName, TableProperties.TableType.DATA);
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
                    tp.addColumn(row[i]);
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
        TableProperties tp = TableProperties.getTablePropertiesForTable(dbh,
                tableId);
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
            String[] columns = new String[tp.getColumns().length];
            for (int i = 0; i < columns.length; i++) {
                String displayName = row[startIndex + i];
                String dbName = tp.getColumnByDisplayName(displayName);
                columns[i] = dbName;
            }
            return importTable(reader, tableId, columns, includeTs, includePn);
        } catch(FileNotFoundException e) {
            return false;
        } catch(IOException e) {
            return false;
        }
    }
    
    private boolean importTable(CSVReader reader, String tableId,
            String[] columns, boolean includeTs, boolean includePn) {
        int tsIndex = includeTs ? 0 : -1;
        int pnIndex = includePn ? (includeTs ? 1 : 0) : -1;
        int startIndex = (includeTs ? 1 : 0) + (includePn ? 1 : 0);
        DbTable dbt = DbTable.getDbTable(dbh, tableId);
        try {
            Map<String, String> values = new HashMap<String, String>();
            String[] row = reader.readNext();
            while (row != null) {
                for (int i = 0; i < columns.length; i++) {
                    values.put(columns[i], row[startIndex + i]);
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
        TableProperties tp = TableProperties.getTablePropertiesForTable(dbh,
                tableId);
        // building array of columns to select and header row for output file
        int columnCount = tp.getColumns().length + (includeTs ? 1 : 0) +
                (includePn ? 1 : 0);
        String[] columns = new String[columnCount];
        String[] headerRow = new String[columnCount];
        int index = 0;
        if (includeTs) {
            columns[index] = DbTable.DB_LAST_MODIFIED_TIME;
            headerRow[index] = LAST_MOD_TIME_LABEL;
            index++;
        }
        if (includePn) {
            columns[index] = DbTable.DB_SRC_PHONE_NUMBER;
            headerRow[index] = SRC_PHONE_LABEL;
            index++;
        }
        if (raw) {
            for (ColumnProperties cp : tp.getColumns()) {
                columns[index] = cp.getColumnDbName();
                headerRow[index] = cp.getDisplayName();
                index++;
            }
        } else {
            for (ColumnProperties cp : tp.getColumns()) {
                columns[index] = cp.getColumnDbName();
                headerRow[index] = cp.getColumnDbName();
                index++;
            }
        }
        // getting data
        DbTable dbt = DbTable.getDbTable(dbh, tableId);
        Table table = dbt.getRaw(columns, null, null, null);
        // writing data
        try {
            CSVWriter cw = new CSVWriter(new FileWriter(file));
            if (!raw) {
                cw.writeNext(new String[] {tp.toJson()});
            }
            cw.writeNext(headerRow);
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
