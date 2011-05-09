package yoonsung.odk.spreadsheet.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public static String TABLE_IS_SECURITY_TABLE = "isSecTable";
	public static final String DB_TABLE_TYPE = "tableType";
	
	public static final int TABLETYPE_DATA = 1;
	public static final int TABLETYPE_SECURITY = 2;
	public static final int TABLETYPE_SHORTCUT = 3;
	
	private DBIO db;
	
	public TableList() {
		this.db = new DBIO();
	}
	
	public HashMap<String, String> getDataTableList() {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.query(TABLE_LIST, null,
		        TABLE_IS_SECURITY_TABLE+"=0 and " + DB_TABLE_TYPE +
		        "=1", null, null, null, null);
		HashMap<String, String> result = getTableList(cs);
		con.close();
		return result;
	}
	
	public HashMap<String, String> getSecurityTableList() {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.query(TABLE_LIST, null, TABLE_IS_SECURITY_TABLE+"=1", null, null, null, null);
		HashMap<String, String> result = getTableList(cs);
		con.close();
		return result;
	}
	
	public HashMap<String, String> getAllTableList() {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.query(TABLE_LIST, null, null, null, null, null, null);
		HashMap<String, String> result = getTableList(cs);
		con.close();
		return result;
	}
	
	private HashMap<String, String> getTableList(Cursor cs) {
		HashMap<String, String> result = new HashMap<String, String>();
		
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

		return result;
	}
	
	public void setAsSecurityTable(String tableID) {
		SQLiteDatabase con = db.getConn();
		String sql = "UPDATE `"+TABLE_LIST+"` SET `"+TABLE_IS_SECURITY_TABLE+"` = 1 WHERE `"+TABLE_ID+"` = " + tableID;
		con.execSQL(sql);
		con.close();
	}
	
	public boolean isSecurityTable(String tableID) {
		boolean result = false;
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.query(TABLE_LIST, null, TABLE_ID+"="+tableID, null, null, null, null);
		if (cs != null) {
			if (cs.moveToFirst()) {
				int index = cs.getColumnIndex(TABLE_IS_SECURITY_TABLE);
				String tmp = cs.getString(index);
				if (tmp!= null && tmp.equals("1")) {
					result = true;
				}
			}
		}
		cs.close();
		con.close();
		return result;
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
			cv.put(DB_TABLE_TYPE, 1);
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
	
	public boolean isTableExistByTableID(String tableID) {
		return getTableName(tableID) != null;
	}
	
	public List<TableInfo> getTableList() {
	    List<TableInfo> res = new ArrayList<TableInfo>();
	    String[] cols = {TABLE_ID, TABLE_NAME, DB_TABLE_TYPE};
	    SQLiteDatabase con = db.getConn();
	    Cursor cs = con.query(TABLE_LIST, cols, null, null, null, null, null);
	    int idIndex = cs.getColumnIndexOrThrow(TABLE_ID);
	    int nameIndex = cs.getColumnIndexOrThrow(TABLE_NAME);
        int typeIndex = cs.getColumnIndexOrThrow(DB_TABLE_TYPE);
        if(!cs.moveToFirst()) {
            cs.close();
            con.close();
            return res;
        }
        do {
            String tID = cs.getString(idIndex);
            String tName = cs.getString(nameIndex);
            int tType = cs.getInt(typeIndex);
            res.add(new TableInfo(tID, tName, tType));
        } while(cs.moveToNext());
	    cs.close();
	    con.close();
	    return res;
	}
    
    public List<TableInfo> getShortcutTableList() {
        List<TableInfo> res = new ArrayList<TableInfo>();
        String[] cols = {TABLE_ID, TABLE_NAME, DB_TABLE_TYPE};
        SQLiteDatabase con = db.getConn();
        String selection = DB_TABLE_TYPE + " = ?";
        String[] selArgs = {Integer.toString(TABLETYPE_SHORTCUT)};
        Cursor cs = con.query(TABLE_LIST, cols, selection, selArgs, null, null,
                null);
        int idIndex = cs.getColumnIndexOrThrow(TABLE_ID);
        int nameIndex = cs.getColumnIndexOrThrow(TABLE_NAME);
        int typeIndex = cs.getColumnIndexOrThrow(DB_TABLE_TYPE);
        if(!cs.moveToFirst()) {
            cs.close();
            con.close();
            return res;
        }
        do {
            String tID = cs.getString(idIndex);
            String tName = cs.getString(nameIndex);
            int tType = cs.getInt(typeIndex);
            res.add(new TableInfo(tID, tName, tType));
        } while(cs.moveToNext());
        cs.close();
        con.close();
        return res;
    }
	
	public void registerNewTable(String tableName, int tableType)
	        throws Exception {
	    if(isTableExist(tableName)) {
	        throw new IllegalArgumentException();
	    }
	    ContentValues vals = new ContentValues();
	    vals.put(TABLE_NAME, tableName);
	    vals.put(DB_TABLE_TYPE, tableType);
	    SQLiteDatabase con = db.getConn();
	    con.insertOrThrow(TABLE_LIST, null, vals);
	    con.close();
	}
	
	public class TableInfo {
	    
	    private String tableID;
	    private String tableName;
	    private int tableType;
	    
	    TableInfo(String tableID, String tableName, int tableType) {
	        this.tableID = tableID;
	        this.tableName = tableName;
	        this.tableType = tableType;
	    }
	    
	    public String getTableID() { return tableID; }
	    
	    public String getTableName() { return tableName; }
	    
	    public int getTableType() { return tableType; }
	    
	}
	
}
