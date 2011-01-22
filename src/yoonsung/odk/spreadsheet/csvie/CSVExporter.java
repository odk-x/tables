package yoonsung.odk.spreadsheet.csvie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * A class for exporting tables to CSV files.
 * TODO: have it notify the user when the data has been exported
 */
public class CSVExporter {
	
	/**
	 * Exports a table to a CSV file.
	 * @param table the the table to export
	 * @param file the file to export to
	 * @param incPN whether to include the source phone numbers
	 * @param incTS whether to include the timestamps
	 * @throws IOException
	 */
	public void exportTable(Table table, File file, boolean incPN,
			boolean incTS) throws CSVException {
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(file));
		} catch (IOException e) {
			throw new CSVException("Could not open file", e);
		}
		List<String> header = table.getHeader();
		int[] exclude;
		int pnIndex = header.indexOf("_phoneNumberIn");
		int tsIndex = header.indexOf("_timestamp");
		if(!incPN && !incTS) {
			exclude = new int[2];
			exclude[0] = tsIndex;
			exclude[1] = pnIndex;
		} else if(!incPN) {
			exclude = new int[1];
			exclude[0] = pnIndex;
		} else if(!incTS) {
			exclude = new int[1];
			exclude[0] = tsIndex;
		} else {
			exclude = new int[0];
		}
		writeRow(header, writer, exclude);
		for(int i=0; i<table.getHeight(); i++) {
			List<String> row = table.getRow(i);
			writeRow(row, writer, exclude);
		}
		try {
			writer.close();
		} catch (IOException e) {
			throw new CSVException("Could not close file", e);
		}
	}
	
	/**
	 * Writes a row to the CSV file.
	 * @param row the values of the row to write
	 * @param writer the CSV writer
	 * @param exclude indices of columns to exclude from the output file
	 */
	private void writeRow(List<String> row, CSVWriter writer, int[] exclude) {
		Arrays.sort(exclude);
		for(int i=exclude.length - 1; i>=0; i--) {
			row.remove(exclude[i]);
		}
		writer.writeNext(row.toArray(new String[0]));
	}
	
}
