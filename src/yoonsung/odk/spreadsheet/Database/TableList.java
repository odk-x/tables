package yoonsung.odk.spreadsheet.Database;

import java.util.HashMap;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TableList {

	// Table Name
	public static String TABLE_LIST = "tableList";
	
	// Column Names
	public static String TABLE_ID   = "tableID";
	public static String TABLE_NAME = "tableName";
	
	private DBIO db;
	
	public TableList() {
		this.db = new DBIO();
	}
	
	public HashMap<String, String> getTableList() {
		HashMap<String, String> result = new HashMap<String, String>();
		
		SQLiteDatabase con = db.getConn();
		
		Cursor cs = con.query(TABLE_LIST, null, null, null, null, null, null);
		if (cs != null) {
			if (cs.moveToFirst()) {
				do {
					int tableIDIndex = cs.getColumnIndex(TABLE_ID);
					int tableNameIndex = cs.getColumnIndex(TABLE_NAME);
					String tableID = cs.getString(tableIDIndex);
					String tableName = cs.getString(tableNameIndex);
					result.put(tableID, tableName);
				} while (cs.moveToNext());
			}
		}
		
		cs.close();
		con.close();
		
		return result;
	}
	
	/**
	 * @return an array of table names, in alphabetical order
	 */
	public String[] getTableNames() {
		SQLiteDatabase con = db.getConn();
		String[] qCols = {TABLE_NAME};
		Cursor cs = con.query(TABLE_LIST, qCols, null, null, null, null,
				TABLE_NAME);
		String[] res = new String[cs.getCount()];
		int index = 0;
		while(cs.moveToNext()) {
			res[index] = cs.getString(1);
			index++;
		}
		cs.close();
		con.close();
		return res;
	}
	
	public String registerNewTable(String tableName) {
		SQLiteDatabase con = db.getConn();
		
		// the table name already exist?
		if (isTableExist(tableName)) {
			con.close();
			return "Table already exists.";
		} else {
			// Register/Insert
			ContentValues cv = new ContentValues();
			cv.put(TABLE_NAME, tableName);
			try {
				con.insertOrThrow(TABLE_LIST, null, cv);
			} catch (Exception e) {
				con.close();
				return "Database Fail.";
			}
		}
		
		con.close();
		return null;
	}
	
	public void unregisterTable(String tableID) {
		SQLiteDatabase con = db.getConn();		
		con.delete(TABLE_LIST, TABLE_ID+" = "+tableID, null);
		con.close();
	}
	
	public void editTableName(String tableID, String newTableName) {
		SQLiteDatabase con = db.getConn();
		ContentValues cv = new ContentValues();
		cv.put(TABLE_NAME, newTableName);
		con.update(TABLE_LIST, cv, TABLE_ID+" = "+tableID, null);
		con.close();
	}
	
	public int getTableID(String tableName) {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM `" + TABLE_LIST 
				+ "` WHERE `" + TABLE_NAME + "` = '" + tableName + "'", null);
		
		if (cs != null) {
			if(cs.moveToFirst()) {
				int index = cs.getColumnIndex(TABLE_ID);
				int tableID = cs.getInt(index);
				Log.e("getTableID", "" + index + " " + tableID);
				cs.close();
				con.close();
				return tableID;
			}
		}
		
		cs.close();
		con.close();
		
		return -1;
	}
	
	public String getTableName(String tableID) {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM `" + TABLE_LIST 
				+ "` WHERE `" + TABLE_ID + "` = " + tableID, null);
		
		if (cs != null) {
			if(cs.moveToFirst()) {
				int index = cs.getColumnIndex(TABLE_NAME);
				String tableName = cs.getString(index);
				cs.close();
				con.close();
				return tableName;
			}
		}
		
		cs.close();
		con.close();
		
		return null;
	}
	
	public boolean isTableExist(String tableName) {
		int tableID = getTableID(tableName);
		Log.e("check dup -table id", "" + tableID);
		return (tableID != -1);
	}
	
}
