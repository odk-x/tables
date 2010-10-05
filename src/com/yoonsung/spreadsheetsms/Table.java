package com.yoonsung.spreadsheetsms;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Table {
	
	public static final String ROWID = "rowID";
	
	private int width;
	private int height;
	private ArrayList<Integer> rowID;
	private ArrayList tableData;
	private ArrayList<String> colNames;
		
	public Table() {
		new Table(0, 0, null, null, null);
	}
	
	public Table(int width, int height, ArrayList<Integer> rowID,
				 ArrayList tableData, ArrayList<String> colNames) {
		this.width = width;
		this.height = height;
		this.rowID = rowID;
		this.tableData = tableData;
		this.colNames = colNames;
	}
	
	public void loadTable() {
		// Connect DB
		SQLiteDatabase db = new DBIO().getConn();
		// Select Data Table
		Cursor cs = db.rawQuery("SELECT * FROM data", null);
		// Save as Table
		this.rowID = new ArrayList<Integer>();
		this.tableData = new ArrayList<String>();
		this.colNames = new ArrayList<String>();
		if (cs != null) {
			// Dimesions
			this.width = cs.getColumnCount() - 1; // rowID doesn't count.
			this.height = cs.getCount(); // header not included.
			// Column Names / Header
			for (String colName : cs.getColumnNames()) {
				if (!colName.equals(ROWID))
					colNames.add(colName);
			}
			// Row ids in the entry and table data
			if (cs.moveToFirst()) {
				do {
					for (int i = 0; i < cs.getColumnCount(); i++) {
						if (cs.getColumnName(i).equals(ROWID)) {
							rowID.add(cs.getInt(i));
						} else {
							tableData.add(cs.getString(i));
						}
					}
				} while (cs.moveToNext());
			}
		} else {
			this.width = 0;
			this.height = 0;
		}
		db.close();
	}
	
	public void loadTable(ArrayList<String> colOrder, String prime, String sortBy) {
		Log.e("RECEIVED", colOrder.toString());
		// Connect DB
		SQLiteDatabase db = new DBIO().getConn();
		// Select Data Table
		Cursor cs = db.rawQuery(prepareSQL((ArrayList<String>)colOrder.clone(), prime, sortBy), null);
		// Save as Table
		this.rowID = new ArrayList<Integer>();
		this.tableData = new ArrayList<String>();
		this.colNames = new ArrayList<String>();
		if (cs != null) {
			// Dimesions
			this.width = cs.getColumnCount() - 1; // rowID doesn't count.
			this.height = cs.getCount(); // header not included.
			// Column Names / Header
			this.colNames = colOrder;
			// Each row
			if (cs.moveToFirst()) {
				do {
					// Row IDs Entry
					int thisRowID = cs.getInt(cs.getColumnIndex(ROWID));
					rowID.add(thisRowID);
					Log.e("ROWID", "" + thisRowID);	
					
					// Table Entry
					for (int i = 0; i < colOrder.size(); i++) {
						// SQL-Friendly "`" in both end
						int index = cs.getColumnIndex(colOrder.get(i));
						tableData.add(cs.getString(index));
					}
					
				} while (cs.moveToNext());
			}
		} else {
			this.width = 0;
			this.height = 0;
		}
		db.close();
	}
	
	private String prepareSQL(ArrayList<String> colOrder, String prime, String sortBy) {
		String result = "SELECT `rowID` AS `rowID` ";
		
		// Add other columns
		for (int i = 0; i < colOrder.size(); i++) {
			if (i == 0)
				result += "," + toSafeSqlString(colOrder.get(i), true) + " ";
			else 
				result += "," + toSafeSqlString(colOrder.get(i), true) + " ";
		}
		result += " FROM " + toSafeSqlString("data", false) + " ";
		
		if (prime != null && prime.trim().length() != 0) {
			result += " GROUP BY " + toSafeSqlString(prime, false) + " ORDER BY " + toSafeSqlString(prime, false) + " ASC"; 
		}
		
		Log.e("RESULT_TEST", result);
		
		return result;
	}
	
	private String toSafeSqlString(String input, boolean as) {
		if (as)
			return "`" + input + "` AS `" + input + "`";
		else 
			return "`" + input + "`";
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
		
	public ArrayList<Integer> getRowID() {
		return this.rowID;
	}
	
	public ArrayList getTableData() {
		return this.tableData;
	}
	
	public ArrayList<String> getColNames() {
		return this.colNames;
	}
	
	public ArrayList getRow(int rowNum) {
		if (height > rowNum) {
			ArrayList row = new ArrayList();
			for (int r = (rowNum * width); r < (rowNum * width) + width; r++)
				row.add(tableData.get(r));
			return row;
		} else {
			return null;
		}
	}
	
	public ArrayList getCol(int colNum) {
		if (width > colNum) {
			ArrayList col = new ArrayList();
			for (int c = colNum; c < (height * width + colNum); c = c + width)
				col.add(tableData.get(c));
			return col;
		} else {
			return null;
		}
	}
	
	public int getRowNum(int position) {
		return (position / width);
	}
	
	public int getColNum(int position) {
		return (position % width);
	}
	
	public int getTableRowID(int rowNum) {
		return rowID.get(rowNum);
	}
	
	public String getColName(int colNum) {
		return colNames.get(colNum);
	}
	
}
