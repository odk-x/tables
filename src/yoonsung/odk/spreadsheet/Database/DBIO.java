package yoonsung.odk.spreadsheet.Database;

import java.util.ArrayList;

import android.database.sqlite.SQLiteDatabase;

public class DBIO {
	//public static final String DB_PATH = "/data/data/com.yoonsung.spreadsheetsms/databases/";
	public static final String DB_PATH = "/sdcard/";
	public static final String DB_NAME = "my_tables";
	public static final String DB_DATA_TABLE_NAME = "data";
	public static final String DB_TABLE_PROPERTY_TABLE_NAME = "tableProperty";
	public static final String DB_COL_PROPERTY_TABLE_NAME = "colProperty";
	
	public DBIO() {
		
	}
	
	public SQLiteDatabase getConn() {
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + DB_NAME, null);
		return db;
	}
	
	public String toSafeSqlString(String input) {
		if (isInteger(input) || isDouble(input))
			return input;
		else
			return "'" + input + "'";
	}
	
	private boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
		} catch(NumberFormatException e) {
			return false;
		} 
		return true;
	}
	
	private boolean isDouble(String str) {
		try {
			Double.parseDouble(str);
		} catch(NumberFormatException e) {
			return false;
		} 
		return true;
	}
	
	public String toSafeSqlColumn(String input, boolean as) {
		if (as)
			return "`" + input + "` AS `" + input + "`";
		else 
			return "`" + input + "`";
	}
	
	public String listColumns(ArrayList<String> columns, boolean as) {
		String result = "";
		for (int i = 0; i < columns.size(); i++) {
			if (i == 0)
				result += " " + toSafeSqlColumn(columns.get(i), as) + " ";
			else 
				result += "," + toSafeSqlColumn(columns.get(i), as) + " ";
		}
		return result;
	}
	
	// changeColName(original, new)
	// changeColType(colName, type)
	// changeColPrime(colName, prime)
	// changeColSortBy(colName, sortBy)
	// changeColFooterMode(colName, footerMode)
	
	// updateRow(rowID, array of stuffs)
	// createRow(rowID, array of stuffs)
	// deleteRow(rowID)
	// updateCol(colName, properties)
	// createCol(colName, properties)
	// deleteCol(colName)
	
}
