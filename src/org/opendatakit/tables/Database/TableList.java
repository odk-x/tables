/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.Database;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
	public static final String DB_SYNC_MOD_NUMBER = "syncModNum";
	public static final String DB_LAST_SYNC_TIME = "lastSyncTime";
	
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
		setTableType(tableID, TABLETYPE_SECURITY);
	}
	
	public void unsetAsSecurityTable(String tableId) {
        SQLiteDatabase con = db.getConn();
        String sql = "UPDATE `"+TABLE_LIST+"` SET `"+TABLE_IS_SECURITY_TABLE+"` = 0 WHERE `"+TABLE_ID+"` = " + tableId;
        con.execSQL(sql);
        con.close();
        setAsDataTable(tableId);
	}
	
	public void setAsShortcutTable(String tableId) {
	    setTableType(tableId, TABLETYPE_SHORTCUT);
	}
	
	public void setAsDataTable(String tableId) {
	    setTableType(tableId, TABLETYPE_DATA);
	}
	
	private void setTableType(String tableId, int type) {
	    SQLiteDatabase con = db.getConn();
	    String sql = "UPDATE `" + TABLE_LIST + "` SET `" + DB_TABLE_TYPE +
	            "` = " + type + " WHERE `" + TABLE_ID + "` = " + tableId;
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
			cv.put(DB_SYNC_MOD_NUMBER, 0);
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
	
	public int getSyncModNumber(String tableId) {
	    SQLiteDatabase con = db.getConn();
	    Cursor c = con.query(TABLE_LIST, new String[] {DB_SYNC_MOD_NUMBER},
	            TABLE_ID + " = ?", new String[] {tableId}, null, null, null);
	    int colIndex = c.getColumnIndexOrThrow(DB_SYNC_MOD_NUMBER);
	    if (!c.moveToFirst()) {
	        return -1;
	    }
	    int num = c.getInt(colIndex);
	    c.close();
	    return num;
	}
	
	public void updateSyncModNumber(String tableId, int num) {
	    ContentValues values = new ContentValues();
	    values.put(DB_SYNC_MOD_NUMBER, num);
	    SQLiteDatabase con = db.getConn();
	    con.update(TABLE_LIST, values, TABLE_ID + " = ?",
	            new String[] {tableId});
	    con.close();
	}
	
	public Date getLastSyncTime(String tableId) {
        SQLiteDatabase con = db.getConn();
        Cursor c = con.query(TABLE_LIST, new String[] {DB_LAST_SYNC_TIME},
                TABLE_ID + " = ?", new String[] {tableId}, null, null, null);
        int colIndex = c.getColumnIndexOrThrow(DB_LAST_SYNC_TIME);
        if (!c.moveToFirst()) {
            return null;
        }
        String dateString = c.getString(colIndex);
        c.close();
        if ((dateString == null) || (dateString.length() == 0)) {
            return null;
        } else {
            try {
                return DataUtils.getInstance().parseDateTimeFromDB(dateString);
            } catch(ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
	}
    
    public void updateLastSyncTime(String tableId, Date time) {
        ContentValues values = new ContentValues();
        String dateString;
        if (time == null) {
            dateString = "";
        } else {
            dateString = DataUtils.getInstance().formatDateTimeForDB(time);
        }
        values.put(DB_LAST_SYNC_TIME, dateString);
        SQLiteDatabase con = db.getConn();
        con.update(TABLE_LIST, values, TABLE_ID + " = ?",
                new String[] {tableId});
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
