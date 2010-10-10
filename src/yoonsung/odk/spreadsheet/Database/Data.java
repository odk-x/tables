package yoonsung.odk.spreadsheet.Database;

import java.util.ArrayList;

import yoonsung.odk.spreadsheet.DataStructure.Table;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Data {

	// Table name
	public static final String DATA = "data";
	// Default essential columns
	public static final String DATA_ROWID = "RowID";
	public static final String DATA_PHONE_NUMBER_IN = "_phoneNumberIn";
	public static final String DATA_TIMESTAMP = "_timestamp";
	
	private DBIO db;
	private TableProperty tp;
	
	public Data() {
		this.db = new DBIO();
		this.tp = new TableProperty();
	}
	
	public void addNewColumn(String colName) {
		if (!isColumnExist(colName)) {
			// Add new column 'data' table
			SQLiteDatabase con = db.getConn();
			con.execSQL("ALTER TABLE " + db.toSafeSqlColumn(DATA, false) 
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
		
		return db.listColumns(tempColOrder, false);
	}
	
	
	public boolean isColumnExist(String colName) {
		// Get database
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM " + db.toSafeSqlColumn(DATA, false), null);
		
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
	
	public void removeRow(String rowID) {
		SQLiteDatabase con = db.getConn();
		con.delete(DATA, db.toSafeSqlColumn(DATA_ROWID, false) + " = " + rowID, null);
		con.close();
	}
	 
	public Table getTable() {
		// Main Frame
		return loadTable(true, tp.getColOrderArrayList(), tp.getPrime(), tp.getSortBy(), null);
	}
	
	public Table getTable(String whereCol, String whereArg) {
		// Into-History
		String whereClause = db.toSafeSqlColumn(whereCol, false) + " = " + db.toSafeSqlString(whereArg);
		return loadTable(false, tp.getColOrderArrayList(), null, tp.getSortBy(), whereClause);
	}
	
	private Table loadTable(boolean isMainTable, 
							ArrayList<String> colOrder, 
							String prime, 
							String sortBy, 
							String whereClause) 
	{
		// Connect to database
		SQLiteDatabase db = new DBIO().getConn();
		
		// Select Data Table
		Cursor cs = db.rawQuery(prepareQueryForTable(isMainTable, colOrder, prime, sortBy, whereClause), null);
		
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
			db.close();
			return new Table(width, height, rowID, header, data, null);
		} else {
			db.close();
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
			return "SELECT " + db.toSafeSqlColumn(DATA_ROWID, true)
					+ " FROM " + db.toSafeSqlColumn(DATA, false) + " ";
		}
		
		// Select statement
		String result = "SELECT " + db.toSafeSqlColumn(DATA_ROWID, true) + ", ";
		
		// Columns & table names
		result += db.listColumns(colOrder, true);
		result += " FROM " + db.toSafeSqlColumn(DATA, false) + " ";
		
		// Type of Table (Main vs Into-History)
		if (isMainTable) {
			// Main Table
			if (prime != null && prime.trim().length() != 0) 
				result += " GROUP BY " + db.toSafeSqlColumn(prime, false) 
					    + " ORDER BY " + db.toSafeSqlColumn(prime, false) + " ASC";
		} else {
			// Into-History
			result += " WHERE " + whereClause;
			result += " ORDER BY " + db.toSafeSqlColumn(sortBy, false) + " DESC";
		}
		
		Log.e("Table Query", result);
		
		return result;
	}	
	
}
