package yoonsung.odk.spreadsheet.Database;

import java.util.ArrayList;

import yoonsung.odk.spreadsheet.Database.DBIO.DatabaseHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TableProperty {

	// Columns in 'tableProperty' table in the database.
	public static final String TABLE_PROPERTY = "tableProperty";
	public static final String TABLE_PROPERTY_PRIME = "prime";
	public static final String TABLE_PROPERTY_SORT_BY = "sortBy";
	public static final String TABLE_PROPERTY_COLUMN_ORDER = "colOrder";
	
	// Database connection
	private DBIO db;
	//private DatabaseHelper dh;
	
	// Constructor
	public TableProperty(Context context) {
		db = new DBIO(context);
		//dh = db.getConn();
	}
		
	public String getPrime() {
		return getProperty(TABLE_PROPERTY_PRIME);
	}
	
	public void setPrime(String newVal) {
		setProperty(TABLE_PROPERTY_PRIME, newVal);
	}
	
	public String getSortBy() {
		return getProperty(TABLE_PROPERTY_SORT_BY);
	}
	
	public void setSortBy(String newVal) {
		setProperty(TABLE_PROPERTY_SORT_BY, newVal);
	}
	
	public String getColOrderCSV() {
		return getProperty(TABLE_PROPERTY_COLUMN_ORDER);
	}
	
	public ArrayList<String> getColOrderArrayList() {
		return csvToArrayList(getProperty(TABLE_PROPERTY_COLUMN_ORDER));
	}
	
	public void setColOrder(ArrayList<String> order) {
		setProperty(TABLE_PROPERTY_COLUMN_ORDER, arrayListToCSV(order));
	}	
	
	protected String getProperty(String colName) {
		DatabaseHelper dh = db.getConn();
		SQLiteDatabase con = dh.getReadableDatabase();
		Cursor cs = con.rawQuery("SELECT * FROM " + db.toSafeSqlColumn(TABLE_PROPERTY, false, null), null);
		String result = null;
		if (cs != null) {
			if (cs.moveToFirst()){
				result = cs.getString(cs.getColumnIndex(colName));
			}
		}
		cs.close();
		con.close();
		return result;
	}
	
	public ArrayList<String> csvToArrayList(String stringOrder) {
		ArrayList<String> colOrder = new ArrayList<String>();
		if (stringOrder != null) {
			String[] tokens = stringOrder.split(";");
			for(String token : tokens) {
				colOrder.add(token);
			}
		}
		return colOrder;
	}
	
	public String arrayListToCSV(ArrayList<String> order) {
		String result = "";
		for (int i = 0; i < order.size(); i++) {
			result += order.get(i) + ";";
		}
		return result;
	}
	
	private void setProperty(String propertyType, String newVal) {
		// Connect to the database
		DatabaseHelper dh = db.getConn();
		SQLiteDatabase con = dh.getWritableDatabase();
		
		// Pack the new entry
		ContentValues values = new ContentValues();
		values.put(propertyType, newVal);

		// Update or Insert
		if (isInsert()) {
			con.insert(TABLE_PROPERTY, null, values);
			Log.d("TableProperyt", "Insert Sucess.");
		} else {
			con.update(TABLE_PROPERTY, values, "rowid = 1", null);
			Log.d("TableProperty", "Update Sucess.");
		}
		
		con.close();	
	}
		
	private boolean isInsert() {
		DatabaseHelper dh = db.getConn();
		SQLiteDatabase con = dh.getReadableDatabase();
		Cursor cs = con.rawQuery("SELECT * FROM " + db.toSafeSqlColumn(TABLE_PROPERTY, false, null) 
								 + " WHERE rowid = 1", null);
		int count = cs.getCount();
		cs.close();
		con.close();
		return (count == 0);
	}
	
}
