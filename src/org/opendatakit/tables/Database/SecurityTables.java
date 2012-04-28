package org.opendatakit.tables.Database;

import java.util.HashMap;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SecurityTables {

	public static final String COL_1_PHONE_NUM = "phone_number";
	public static final String COL_2_PASSWORD  = "password";
	public static final String COL_3_ID        = "id";
	
	private DBIO db;
	private TableProperty tp;
	private TableList tl;
	
	public SecurityTables(String tableID) {
		db = new DBIO();
		tp = new TableProperty(tableID);
		tl = new TableList();
	}
	
	// returns -1 if none
	public String getReadTableID() {
		return tp.getReadSecurityTableID();
	}
	
	// returns -1 if none
	public String getWriteTableID() {
		return tp.getWriteSecurityTableID();
	}
	
	public boolean isReadSecurityTableExist() {
		String tableID = getReadTableID();
		return isSecurityTableExist(tableID);
	}
	
	public boolean isWriteSecurityTableExist() {
		String tableID = getWriteTableID();
		return isSecurityTableExist(tableID);
	}
	
	private boolean isSecurityTableExist(String tableID) {	
		if (tableID != null && tableID.trim().length() != 0 && !tableID.equals("-1")) {
			return true;
		}
		return false;
	}
	
	public boolean isPhoneNumInReadTable(String phoneNum) {
		if (!isReadSecurityTableExist()) {
			return false;
		}
		
		String tableID = getReadTableID();
		if (tableID.equals("-1")) {
			return false;
		} else {
			HashMap<String, String> map = searchByPhoneNumber(tableID, phoneNum);
			return !map.isEmpty();
		}
	}
	
	public boolean isPhoneNumInWriteTable(String phoneNum) {
		if (!isWriteSecurityTableExist()) {
			return false;
		}
		
		String tableID = getWriteTableID();
		if (tableID.equals("-1")) {
			return false;
		} else {
			HashMap<String, String> map = searchByPhoneNumber(tableID, phoneNum);
			return !map.isEmpty();
		}
	}
	
	public boolean validatePasswordForPhoneNum(String secTableID, String phoneNum, String pass) {
		HashMap<String, String> map = searchByPhoneNumber(secTableID, phoneNum);
		String dbPhoneNum = map.get(COL_1_PHONE_NUM);
		if (dbPhoneNum != null && dbPhoneNum.equals(phoneNum))  {
			// Phone number validated
			if (map.containsKey(COL_2_PASSWORD)) {
				// There is password column in the security table
				String dbPass = map.get(COL_2_PASSWORD);
				if (dbPass == null || dbPass.equals("")) {
					// password column exists but empty value
					return true;
				} else {
					// password column exists and value exists
					return dbPass.equals(pass);
				}
			} else {
				// There is no password column
				return true; 
			}
		}
		return false;
	}
	
	private HashMap<String, String> searchByPhoneNumber(String tableID, String phoneNum) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		String tableName = tl.getTableName(tableID);
	
		Log.e("searchByPhoneNumber", "" + tableID + ", " + tableName);
		
		SQLiteDatabase con = db.getConn();
		String sql = "SELECT * FROM `" + tableName + "` WHERE `phone_number` = '" + phoneNum + "'";
		try {
			Cursor cs = con.rawQuery(sql, null);
			if (cs != null) {	
				if (cs.moveToFirst()) {
					for (int i = 0; i < cs.getColumnCount(); i++) {
						String colName = cs.getColumnName(i);
						String colVal  = cs.getString(i);
						result.put(colName, colVal);
					}
				}
			}
			cs.close();
		} catch (Exception e) {
			Log.e("SecurityTable", "Error at searchByPhoneNumber method.");
		}
		con.close();
		return result;
	}
	
}
