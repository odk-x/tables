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
	
	public String toSafeSqlColumn(String input, boolean as, String func) {
		if (as && func != null)
			return func + "(" + input + ")" + " AS `" + input + "`"; 
		else if (as)
			return "`" + input + "` AS `" + input + "`";
		else 
			return "`" + input + "`";
	}
	
	public String listColumns(ArrayList<String> columns, boolean as, String func, String funcCol) {
		String result = "";
		for (int i = 0; i < columns.size(); i++) {
			String col = columns.get(i);
			// First & End
			if (i == 0) 
				result += " " + listColumnWithFunc(col, as, func, funcCol) + " ";
			else 
				result += ", " + listColumnWithFunc(col, as, func, funcCol) + " ";
		}
		return result;
	}
	
	// Add Function
	private String listColumnWithFunc(String col, boolean as, String func, String funCol) {
		if (col.equals(funCol)) 
			return toSafeSqlColumn(col, as, func);
		else
			return toSafeSqlColumn(col, as, null);
	}
}
