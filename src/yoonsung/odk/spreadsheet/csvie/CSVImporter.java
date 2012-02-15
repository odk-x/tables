package yoonsung.odk.spreadsheet.csvie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.content.Context;
import au.com.bytecode.opencsv.CSVReader;

/**
 * A class for importing tables from CSV files.
 */
public class CSVImporter {
    
    private final Context context;
    
    public CSVImporter(Context context) {
        this.context = context;
    }
	
	/**
	 * Imports to a new table from a CSV file.
	 * @param tableName the name for the new table
	 * @param file the file to import from
	 * @throws CSVException
	 */
	public void buildTable(String tableName, File file) throws CSVException {
	    CSVReader reader = getReader(file);
	    ImportInfo ii = getImportInfo(reader);
	    TableProperties tp = addTable(tableName, ii);
	    importTable(tp, ii, reader);
	    closeReader(reader);
	}
	
	/**
	 * Imports to an existing table from a CSV file.
	 * @param tp the table properties of the table to import to
	 * @param file the file to import from
	 * @throws CSVException
	 */
	public void importTable(TableProperties tp, File file)
	        throws CSVException {
        CSVReader reader = getReader(file);
        ImportInfo ii = getImportInfo(reader);
        importTable(tp, ii, reader);
        closeReader(reader);
	}
	
	private CSVReader getReader(File file) {
	    try {
	        return new CSVReader(new FileReader(file));
	    } catch (FileNotFoundException e) {
	        return null;
	    }
	}
	
	private ImportInfo getImportInfo(CSVReader reader) {
	    String[] row = getRow(reader);
	    int tsIndex = -1;
	    int pnIndex = -1;
	    int colCount = 0;
	    for (int i = 0; i < row.length; i++) {
	        if (row[i].equals(DbTable.DB_LAST_MODIFIED_TIME)) {
	            tsIndex = i;
	        } else if (row[i].equals(DbTable.DB_SRC_PHONE_NUMBER)) {
	            pnIndex = i;
	        } else {
	            colCount++;
	        }
	    }
	    String[] header = new String[colCount];
	    int index = 0;
	    for (int i = 0; i < row.length; i++) {
	        if ((i != tsIndex) && (i != pnIndex)) {
	            header[index] = row[i];
	            index++;
	        }
	    }
	    return new ImportInfo(header, tsIndex, pnIndex);
	}
	
	private TableProperties addTable(String tableName, ImportInfo ii) {
	    DbHelper dbh = DbHelper.getDbHelper(context);
	    String dbTableName = TableProperties.createDbTableName(dbh, tableName);
	    TableProperties tp = TableProperties.addTable(dbh, dbTableName,
	            tableName, TableProperties.TableType.DATA);
	    Set<String> colNames = new HashSet<String>();
	    for (int i = 0; i < ii.header.length; i++) {
	        String colName = ii.header[i];
	        if (colNames.contains(colName)) {
	            tp.addColumn(colName);
	        } else {
	            tp.addColumn(colName, colName);
	            colNames.add(colName);
	        }
	    }
	    return tp;
	}
	
	private void importTable(TableProperties tp, ImportInfo ii,
	        CSVReader reader) {
	    DbTable dbt = DbTable.getDbTable(DbHelper.getDbHelper(context),
	            tp.getTableId());
	    String[] row = getRow(reader);
	    String tsValue = null;
	    String pnValue = null;
	    Map<String, String> values = new HashMap<String, String>();
	    int index = 0;
	    while (row != null) {
	        for (int i = 0; i < row.length; i++) {
	            if (i == ii.tsIndex) {
	                tsValue = row[i];
	            } else if (i == ii.pnIndex) {
	                pnValue = row[i];
	            } else {
	                values.put(ii.header[index], row[i]);
	                index++;
	            }
	        }
	        dbt.addRow(values, tsValue, pnValue);
	        tsValue = null;
	        pnValue = null;
	        values.clear();
	        index = 0;
	        row = getRow(reader);
	    }
	}
	
	private String[] getRow(CSVReader reader) {
	    try {
            return reader.readNext();
        } catch(IOException e) {
            return null;
        }
	}
	
	private void closeReader(CSVReader reader) {
	    try {
            reader.close();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	private class ImportInfo {
	    public String[] header;
	    public int tsIndex;
	    public int pnIndex;
	    public ImportInfo(String[] header, int tsIndex, int pnIndex) {
	        this.header = header;
	        this.tsIndex = tsIndex;
	        this.pnIndex = pnIndex;
	    }
	}
}
