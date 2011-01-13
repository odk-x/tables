package yoonsung.odk.spreadsheet.csvie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.Data;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * A class for exporting tables to CSV files.
 */
public class CSVExporter {
	
	/**
	 * Exports a table to a CSV file.
	 * @param tablename the name of the table to export
	 * @param file the file to export to
	 * @throws IOException
	 */
	public void exportTable(String tablename, File file)
			throws CSVException {
		// TODO: make it care what table it is
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(file));
		} catch (IOException e) {
			throw new CSVException("Could not open file", e);
		}
		Table table = (new Data()).getTable();
		String[] sarr = new String[0];
		writer.writeNext(table.getHeader().toArray(sarr));
		for(int i=0; i<table.getHeight(); i++) {
			writer.writeNext(table.getRow(i).toArray(sarr));
		}
		try {
			writer.close();
		} catch (IOException e) {
			throw new CSVException("Could not close file", e);
		}
	}
	
}
