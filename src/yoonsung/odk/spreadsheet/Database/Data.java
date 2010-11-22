package yoonsung.odk.spreadsheet.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import yoonsung.odk.spreadsheet.DataStructure.Table;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/*
 * Database object that allows to set and get the 
 * information from the database. To this work, there 
 * must be a table that saves data. 
 * 
 * @Author : YoonSung Hong (hys235@cs.washingon.edu)
 */
public class Data {

	// Table name
	public static final String DATA = "data";
	
	// Column Names
	public static final String DATA_ROWID = "RowID";
	public static final String DATA_PHONE_NUMBER_IN = "_phoneNumberIn";
	public static final String DATA_TIMESTAMP = "_timestamp";
	
	private DBIO db; // Database connection
	private TableProperty tp;
	
	
	// Constructor
	public Data() {
		this.db = new DBIO();
		this.tp = new TableProperty();
	}
	
	// Create a new column with this name. If there is a column
	// with this name, do-nothing.
	public void addNewColumn(String colName) {
		if (!isColumnExist(colName)) {
			// Add new column 'data' table
			SQLiteDatabase con = db.getConn();
			con.execSQL("ALTER TABLE " + db.toSafeSqlColumn(DATA, false, null) 
						+ " ADD " + db.toSafeSqlString(colName) + " TEXT");
			con.close();
			
			// Update colOrder in TableProperty
			ArrayList<String> colOrder = tp.getColOrderArrayList();
			colOrder.add(colName);
			tp.setColOrder(colOrder);
		} else {
			// column already exist;
		}
	}	
	
	// Drop a column with this name. If no such a column exsit,
	// do-nothing.
	public void dropColumn(String colName) {
		if (isColumnExist(colName)) {
			// Drop from 'data' table
			String originalTable = DATA;
			String backupTable = "baktable";
			String SelColumns = "rowID, " + dropAColumnHelper(colName);
			String InsColumns = "rowID INTEGER PRIMARY KEY ASC, " + dropAColumnHelper(colName);
			
			SQLiteDatabase con = db.getConn();
			try {
				con.beginTransaction();
				con.execSQL("CREATE TEMPORARY TABLE " + backupTable + "(" + InsColumns + ")");
				con.execSQL("INSERT INTO " + backupTable + " SELECT " + SelColumns + " FROM " + originalTable);
				con.execSQL("DROP TABLE " + originalTable);
				con.execSQL("CREATE TABLE " + originalTable + "(" + InsColumns + ")");
				con.execSQL("INSERT INTO " + originalTable + " SELECT " + SelColumns + " FROM " + backupTable);
				con.execSQL("DROP TABLE " + backupTable);
				con.setTransactionSuccessful();
			} catch (Exception e) {
				Log.d("Data", "Drop Column Failed");
			} finally {
				con.endTransaction();
				con.close();
			}
			
			// Drop from colOrder in Table Property
			ArrayList<String> colOrder = tp.getColOrderArrayList();
			colOrder.remove(colName);
			tp.setColOrder(colOrder);
			
		} else {
			// Column does not exist.
		}
	}
	
	// format to "x, y, z ..."
	private String dropAColumnHelper(String colName) {
		ArrayList<String> tempColOrder = tp.getColOrderArrayList();
		tempColOrder.remove(colName);
		
		return db.listColumns(tempColOrder, false, null, null);
	}
	
	// Check if such a column exist?
	public boolean isColumnExist(String colName) {
		// Get database
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM " + db.toSafeSqlColumn(DATA, false, null), null);
		
		// Check if such a column exist?
		for (int i = 0; i < cs.getColumnCount(); i++) {
			if (cs.getColumnName(i).equals(colName))  {
				// Existing
				cs.close();
				con.close();
				return true;
			}
		}
		
		// Not Existing
		cs.close();
		con.close();
		return false;
	}
	
	// Add new row with the specified information.
	public void addRow(ContentValues values, String phoneNumberIn, String timeStamp) {
		SQLiteDatabase con = db.getConn();
		values.put(DATA_PHONE_NUMBER_IN, phoneNumberIn);
		values.put(DATA_TIMESTAMP, timeStamp);
		try {
			con.insertOrThrow(DATA, null, values);
		} catch (Exception e) {
			Log.d("Data", "Add Row failed.");
		}
		con.close();
	}
	
	public void updateRow(ContentValues values, int rowID) {
		SQLiteDatabase con = db.getConn();
		con.update(DATA, values, DATA_ROWID + "=" + rowID, null);
		con.close();
	}
	
	public void removeRow(String rowID) {
		SQLiteDatabase con = db.getConn();
		con.delete(DATA, db.toSafeSqlColumn(DATA_ROWID, false, null) + " = " + rowID, null);
		con.close();
	}
	 
	public Table getTable() {
		// Main Frame
		return loadTable(true, tp.getColOrderArrayList(), tp.getPrime(), tp.getSortBy(), null);
	}
	
	public Table getTable(String whereCol, String whereArg) {
		// Into-History
		String whereClause = db.toSafeSqlColumn(whereCol, false, null) + " = " + db.toSafeSqlString(whereArg);
		return loadTable(false, tp.getColOrderArrayList(), null, tp.getSortBy(), whereClause);
	}
	
	private Table loadTable(boolean isMainTable, 
							ArrayList<String> colOrder, 
							String prime, 
							String sortBy, 
							String whereClause) 
	{
		// Connect to database
		SQLiteDatabase con = db.getConn();
		
		// Select Data Table
		Cursor cs = con.rawQuery(prepareQueryForTable(isMainTable, colOrder, prime, sortBy, whereClause), null);
		
		// Table's fields
		ArrayList<Integer> rowID = new ArrayList<Integer>();
		ArrayList<String> data = new ArrayList<String>();
		ArrayList<String> header = colOrder;
		
		// Retrieve data from the database
		if (cs != null) {
			// Dimension
			int width = cs.getColumnCount() - 1; // rowID doesn't count.
			int height = cs.getCount(); 		 // header/footer not included.

			// Each row
			if (cs.moveToFirst()) {
				do {
					// Add a row id
					int thisRowID = cs.getInt(cs.getColumnIndex(DATA_ROWID));
					rowID.add(thisRowID);
					
					// Add a cell data
					for (int i = 0; i < header.size(); i++) {
						int index = cs.getColumnIndex(header.get(i));
						data.add(cs.getString(index));
					}
				} while (cs.moveToNext());
			}
			cs.close();
			con.close();
			return new Table(width, height, rowID, header, data, null);
		} else {
			con.close();
			return new Table();
		}
	}
	
	private String prepareQueryForTable(boolean isMainTable, 
										ArrayList<String> colOrder, 
										String prime, 
										String sortBy,
										String whereClause) 
	{	
		// No columns to visualize
		if (colOrder.size() == 0) {
			return "SELECT `" + DATA_ROWID + "` AS '" + DATA_ROWID + "'"
					+ " FROM " + db.toSafeSqlColumn(DATA, false, null) + " ";
		}
		
		// Select statement with ROWID
		String result = "SELECT " + db.toSafeSqlColumn(DATA_ROWID, true, null) + ", ";
	
		if (isMainTable) {
			// Main Table
			if (prime != null && prime.trim().length() != 0) 
				result += db.listColumns(colOrder, true, "MAX", sortBy)
						+ " FROM " + db.toSafeSqlColumn(DATA, false, null)
				        + " GROUP BY " + db.toSafeSqlColumn(prime, false, null)
						+ " ORDER BY " + db.toSafeSqlColumn(prime, false, null) + " ASC";
			else 
				result += db.listColumns(colOrder, true, null, null) 
						+ " FROM " + db.toSafeSqlColumn(DATA, false, null);
		} else {
			// Into-History
			result += db.listColumns(colOrder, true, null, null)
					+ " FROM " + db.toSafeSqlColumn(DATA, false, null)
					+ " WHERE " + whereClause;
			if (sortBy != null && sortBy.trim().length() != 0)
					result += " ORDER BY " + db.toSafeSqlColumn(sortBy, false, null) + " DESC";
		}
		
		Log.e("Table Query", result);
		
		return result;
	}
	
	/**
	 * Gets data from a spreadsheet
	 * @param consKeys the constraint keys ([key] [comp] [val])
	 * @param consComp the contraint comparators
	 * @param consVals the contrains values
	 * @param cols the columns to return values from
	 * @param orderby the column to order by (or null)
	 * @param asc whether to order from the top (1), bottom (2), or not at all (0)
	 * @param maxRows the maximum number of rows to return
	 * @return a set of mappings (column->value)
	 */
	public Set<Map<String, String>> querySheet(List<String> consKeys, List<String> consComp,
			List<String> consVals, String[] cols, String orderby, int asc, int maxRows) {
		DBIO db = new DBIO();
		String reqList = "";
		int i = 0;
		while(i < consKeys.size()) {
			reqList += " and " + db.toSafeSqlColumn(consKeys.get(i), false, null) +
					consComp.get(i) + db.toSafeSqlString(consVals.get(i));
			i++;
		}
		if(i != 0) {
			reqList = reqList.substring(5);
		}
		String limit;
		if(maxRows == -1) {
			limit = "5";
		} else {
			limit = new Integer(maxRows).toString();
		}
		if(orderby == null) {
			orderby = DATA_TIMESTAMP;
		} else {
			orderby = db.toSafeSqlColumn(orderby, false, null);
			if(asc == 1) {
				orderby += " DESC";
			} else if(asc == 2) {
				orderby += " ASC";
			}
		}
		SQLiteDatabase con = db.getConn();
		Cursor c = con.query(DATA, cols, reqList, null, null, null,
				orderby, limit);
		Set<Map<String, String>> result = new HashSet<Map<String, String>>();
		boolean empty = !c.moveToFirst();
		i = 0;
		while(!empty && (i < maxRows)) {
			Map<String, String> nextMap = new HashMap<String, String>();
			for(String req : cols) {
				String val = c.getString(c.getColumnIndexOrThrow(req));
				nextMap.put(req, val);
			}
			result.add(nextMap);
			empty = !c.moveToNext();
			i++;
		}
		c.close();
		con.close();
		return result;
	}
	
}
