package yoonsung.odk.spreadsheet.csvie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import yoonsung.odk.spreadsheet.Database.DataTable;
import android.content.ContentValues;
import au.com.bytecode.opencsv.CSVReader;

/**
 * A class for importing tables from CSV files.
 */
public class CSVImporter {
	
	/**
	 * Imports to an existing table from a CSV file.
	 * @param tablename the name of the table to import to
	 * @param file the file to import from
	 * @throws Exception
	 */
	public void importTable(String tablename, File file) throws CSVException {
		DataTable data = new DataTable(tablename);
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
				data.addRow(cv, pn, ts);
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
			String head = header[i];
			if(!head.equals("_phoneNumberIn") && !head.equals("_timestamp")) {
				vals.put(head, row[i]);
			}
		}
		return vals;
	}
	
}
