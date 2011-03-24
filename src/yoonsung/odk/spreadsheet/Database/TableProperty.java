package yoonsung.odk.spreadsheet.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TableProperty {

	// Columns in 'tableProperty' table in the database.
	public static final String TABLE_PROPERTY = "tableProperty";
	public static final String TABLE_PROPERTY_TABLE_ID = "tableID";
	public static final String TABLE_PROPERTY_PRIME = "prime";
	public static final String TABLE_PROPERTY_SORT_BY = "sortBy";
	public static final String TABLE_PROPERTY_COLUMN_ORDER = "colOrder";
	public static final String TABLE_PROPERTY_DEFOUTMSG = "defoutmsg";
	public static final String TPDOM_ID = "id";
	public static final String TPDOM_TABLEID = "tableID";
	public static final String TPDOM_FRMT = "format";
	
	// Database connection
	private DBIO db;
	private String tableID;
	
	// Constructor
	public TableProperty(String tableID) {
		this.db = new DBIO();
		this.tableID = tableID;
	}
	
	/**
	public String getPrime() {
		return getProperty(TABLE_PROPERTY_PRIME);
	}
	
	public void setPrime(String newVal) {
		setProperty(TABLE_PROPERTY_PRIME, newVal);
	}
	**/
	
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
	
	public Map<Integer, String> getDefOutMsg() {
		Map<Integer, String> res = new HashMap<Integer, String>();
		SQLiteDatabase con = db.getConn();
		String[] cols = new String[] {TPDOM_ID, TPDOM_FRMT};
		String selection = TPDOM_TABLEID + " = ?";
		String[] selectionArgs = new String[] {tableID};
		Cursor cs = con.query(TABLE_PROPERTY_DEFOUTMSG, cols, selection,
				selectionArgs, null, null, TPDOM_ID);
		boolean going = cs.moveToFirst();
		int idIndex = cs.getColumnIndexOrThrow(TPDOM_ID);
		int frmtIndex = cs.getColumnIndexOrThrow(TPDOM_FRMT);
		while(going) {
			int id = cs.getInt(idIndex);
			String frmt = cs.getString(frmtIndex);
			res.put(id, frmt);
			going = cs.moveToNext();
		}
		cs.close();
		con.close();
		return res;
	}
	
	public void addDefOutMsg(String newVal) {
		ContentValues vals = new ContentValues();
		vals.put(TPDOM_TABLEID, tableID);
		vals.put(TPDOM_FRMT, newVal);
		SQLiteDatabase con = db.getConn();
		con.insertOrThrow(TABLE_PROPERTY_DEFOUTMSG, null, vals);
		con.close();
	}
	
	public void changeDefOutMsg(int id, String newVal) {
		ContentValues vals = new ContentValues();
		vals.put(TPDOM_FRMT, newVal);
		String whereClause = TPDOM_ID + " = ?";
		String[] whereArgs = new String[] {Integer.toString(id)};
		SQLiteDatabase con = db.getConn();
		con.update(TABLE_PROPERTY_DEFOUTMSG, vals, whereClause, whereArgs);
		con.close();
	}
	
	public void removeDefOutMsg(int id) {
		String whereClause = TPDOM_ID + " = ?";
		String[] whereArgs = new String[] {(new Integer(id)).toString()};
		SQLiteDatabase con = db.getConn();
		con.delete(TABLE_PROPERTY_DEFOUTMSG, whereClause, whereArgs);
		con.close();
	}
	
	public void removeAll() {
		SQLiteDatabase con = db.getConn();
		con.delete(TABLE_PROPERTY, TABLE_PROPERTY_TABLE_ID+"="+tableID, null);
	}
	
	private String getProperty(String colName) {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM `" + TABLE_PROPERTY 
					+ "` WHERE `" + TABLE_PROPERTY_TABLE_ID + "` = " + tableID, null);
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
		if (stringOrder != null && stringOrder.length() != 0) {
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
		SQLiteDatabase con = db.getConn();
		
		// Pack the new entry
		ContentValues values = new ContentValues();
		values.put(TABLE_PROPERTY_TABLE_ID, tableID);
		values.put(propertyType, newVal);

		// Update or Insert
		if (isInsert()) {
			con.insert(TABLE_PROPERTY, null, values);
			Log.d("TableProperyt", "Insert Sucess.");
		} else {
			con.update(TABLE_PROPERTY, values, TABLE_PROPERTY_TABLE_ID+" = "+tableID, null);
			Log.d("TableProperty", "Update Sucess.");
		}
		
		con.close();	
	}
		
	private boolean isInsert() {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM `" + TABLE_PROPERTY
								 + "` WHERE `" + TABLE_PROPERTY_TABLE_ID + "` = " + tableID, null);
		int count = cs.getCount();
		cs.close();
		con.close();
		return (count == 0);
	}
	
}
