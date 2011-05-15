package yoonsung.odk.spreadsheet.csvie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import yoonsung.odk.spreadsheet.Database.DBIO;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.DefaultsManager;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;

/**
 * A class for importing tables from CSV files.
 */
public class CSVImporter {
	
	/**
	 * Imports to a new table from a CSV file.
	 * @param tableName the name for the new table
	 * @param file the file to import from
	 * @throws CSVException
	 */
	public void buildTable(String tableName, File file) throws CSVException {
		importFile(tableName, file, true);
	}
	
	/**
	 * Imports to an existing table from a CSV file.
	 * @param tableName the name of the table to import to
	 * @param file the file to import from
	 * @throws CSVException
	 */
	public void importTable(String tableName, File file) throws CSVException {
		importFile(tableName, file, false);
	}
	
	/**
	 * Imports to an existing table from a CSV file.
	 * @param tableid the name of the table to import to
	 * @param file the file to import from
	 * @param createTable whether the table needs to be created
	 * @throws CSVException
	 */
	private void importFile(String tableName, File file, boolean createTable)
			throws CSVException {
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new CSVException("File not found", e);
		}
		try {
			String[] header = reader.readNext();
			if(header == null) {
				throw new CSVException("File is empty.");
			}
			DataTable data;
			if(createTable) {
				data = new DataTable(createTable(tableName, header));
			} else {
				Integer tID = (new TableList()).getTableID(tableName);
				data = new DataTable(tID.toString());
			}
			int phoneIn = -1;
			int timestamp = -1;
			for(int i=0; i<header.length; i++) {
				String head = header[i];
				if(head.equals("_phoneNumberIn")) {
					phoneIn = i;
				} else if(head.equals("_timestamp")) {
					timestamp = i;
				}
			}
			String[] next = reader.readNext();
			while(next != null) {
				ContentValues cv = getValues(header, next);
				String pn = "";
				if(phoneIn >= 0) {
					pn = next[phoneIn];
				}
				String ts = "";
				if(timestamp >= 0) {
					ts = next[timestamp];
				}
				try {
				    data.addRow(cv, pn, ts);
				} catch(IllegalArgumentException e) {
				    // TODO: something to handle invalid values
				}
				next = reader.readNext();
			}
		} catch (IOException e) {
			throw new CSVException("Could not read file", e);
		}
	}
	
	/**
	 * Gets content values for a row.
	 * @param header the array of headers from the CSV file
	 * @param row the array of values for the row from the CSV file
	 * @return the content values
	 */
	private ContentValues getValues(String[] header, String[] row) {
		ContentValues vals = new ContentValues();
		for(int i=0; i<header.length; i++) {
			String head = "`" + header[i] + "`";
			if(!head.equals("_phoneNumberIn") && !head.equals("_timestamp")) {
				vals.put(head, row[i]);
			}
		}
		return vals;
	}
	
	/**
	 * Creates a table.
	 * @param tableName the name for the new table
	 * @param header the array of column names to add
	 * @param dt the data
	 * @return the ID of the newly-created table
	 */
	private String createTable(String tableName, String[] header) {
		TableList tl = new TableList();
		String res = tl.registerNewTable(tableName);
		if(res != null) {
			throw new IllegalArgumentException(res);
		}
		String stat = "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
				+ DataTable.DATA_ROWID + " INTEGER PRIMARY KEY,"
				+ DataTable.DATA_PHONE_NUMBER_IN + " TEXT,"
				+ DataTable.DATA_TIMESTAMP + " TEXT";
		for(String col : header) {
		    if(!col.equals("_phoneNumberIn") && !col.equals("_timestamp")) {
	            stat += ", `" + col + "` TEXT";
		    }
		}
		stat += ");";
		DBIO db = new DBIO();
		SQLiteDatabase con = db.getConn();
		con.execSQL(stat);
		con.close();
		Integer tID = tl.getTableID(tableName);
		String tableID = tID.toString();
		DefaultsManager dm = new DefaultsManager(tableID);
		TableProperty tp = new TableProperty(tableID);
		ArrayList<String> colOrder = tp.getColOrderArrayList();
		for(String col : header) {
			Log.d("csvi", "starting col add:" + col);
			dm.prepForNewCol(col);
			Log.d("csvi", "just called dm.prepForNewCol");
			colOrder.add(col);
		}
		tp.setColOrder(colOrder);
		return tableID;
	}
	
}
