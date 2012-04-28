package org.opendatakit.tables.Database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * An object for managing default query and addition settings.
 */
public class DefaultsManager {
	
	/** the table name in the database */
	public static final String DEFOPTS_TABLE = "defaultopts";
	/** the name of the table ID column */
	public static final String DB_TABL = "tableID";
	/** the name of the operation column */
	public static final String DB_OPER = "operation";
	/** the name of the type column */
	public static final String DB_TYPE = "type";
	/** the name of the key column */
	public static final String DB_KEY = "itemkey";
	/** the name of the value column */
	public static final String DB_VAL = "itemval";
	/** the value for the operation column that indicates addition */
	private static final int ADDITION = 1;
	/** the value for the operation column that indicates querying */
	private static final int QUERYING = 2;
	/** the value for the type column that indicates default column value */
	private static final int COL_VALUE = 1;
	/** the value for the type column that indicates default column return */
	private static final int COL_RETURN = 2;
	/** the value for the type column that indicates a special default */
	private static final int OTHER = 3;
	/** the value for the key column that indicates "get available" */
	private static final String OAVAIL = "avail";
	
	/** the database */
	private DBIO db;
	/** the table ID */
	private String tableID;
	
	/**
	 * Constructs a new DefaultsManager
	 * @param tableID the table ID
	 */
	public DefaultsManager(String tableID) {
		db = new DBIO();
		this.tableID = tableID;
	}
	
	/**
	 * @return a map of column names to their default addition values
	 */
	public Map<String, String> getAddColVals() {
		return getColVals(ADDITION);
	}
	
	/**
	 * @return a map of column names to their default querying values
	 */
	public Map<String, String> getQueryColVals() {
		return getColVals(QUERYING);
	}
	
	/**
	 * @param oper the operation (addition or querying)
	 * @return a map of column names to their default values for the given
	 *         operation
	 */
	private Map<String, String> getColVals(int oper) {
		Map<String, String> map = new HashMap<String, String>();
		SQLiteDatabase con = db.getConn();
		String[] cols = {DB_KEY, DB_VAL};
		String sel = DB_TABL + "=" + tableID + " and " + DB_OPER + "=" + oper +
				" and " + DB_TYPE + "=" + COL_VALUE;
		Cursor c = con.query(DEFOPTS_TABLE, cols, sel, null, null, null, null);
		int keyIndex = c.getColumnIndexOrThrow(DB_KEY);
		int valIndex = c.getColumnIndexOrThrow(DB_VAL);
		boolean empty = !c.moveToFirst();
		while(!empty) {
			map.put(c.getString(keyIndex), c.getString(valIndex));
			empty = !c.moveToNext();
		}
		c.close();
		con.close();
		return map;
	}
	
	/**
	 * @return a set of the names of columns to always include in query
	 *         responses
	 */
	public Set<String> getQueryIncCols() {
		Set<String> set = new HashSet<String>();
		SQLiteDatabase con = db.getConn();
		String[] cols = {DB_KEY};
		String sel = DB_TABL + "=" + tableID + " and " + DB_OPER + "=" +
				QUERYING + " and " + DB_TYPE + "=" + COL_RETURN + " and " +
				DB_VAL + "=1";
		Cursor c = con.query(DEFOPTS_TABLE, cols, sel, null, null, null, null);
		int keyIndex = c.getColumnIndexOrThrow(DB_KEY);
		boolean empty = !c.moveToFirst();
		while(!empty) {
			set.add(c.getString(keyIndex));
			empty = !c.moveToNext();
		}
		c.close();
		con.close();
		return set;
	}
	
	/**
	 * @return the name of the column to check for availability when adding
	 */
	public String getAddAvailCol() {
		return getAvailCol(ADDITION);
	}
	
	/**
	 * @return the name of the column to check for availability when querying
	 */
	public String getQueryAvailCol() {
		return getAvailCol(QUERYING);
	}

	
	/**
	 * @return the name of the column to check for availability for the given
	 *         operation
	 */
	private String getAvailCol(int oper) {
		SQLiteDatabase con = db.getConn();
		String[] cols = {DB_VAL};
		String sel = db.toSafeSqlColumn(DB_TABL, false, null) + "=" + tableID +
				" and " + db.toSafeSqlColumn(DB_OPER, false, null) + "=" +
				oper + " and " + db.toSafeSqlColumn(DB_TYPE, false, null) +
				"=" + OTHER + " and " +
				db.toSafeSqlColumn(DB_KEY, false, null) + "='" + OAVAIL + "'";
		Cursor c = con.query(DEFOPTS_TABLE, cols, sel, null, null, null, null,
				"1");
		String r = null;
		if(c.moveToFirst()) {
			r = c.getString(c.getColumnIndexOrThrow(DB_VAL));
		}
		c.close();
		con.close();
		if((r == null) || (r.length() == 0)) {return null;}
		return r;
	}
	
	/**
	 * Sets an addition default for a column
	 * @param col the column name
	 * @param val the new default value
	 * @return true if successful
	 */
	public boolean setAddColDefault(String col, String val) {
		return setColDefault(ADDITION, col, val);
	}
	
	/**
	 * Sets a query default for a column
	 * @param col the column name
	 * @param val the new default value
	 * @return true if successful
	 */
	public boolean setQueryColDefault(String col, String val) {
		return setColDefault(QUERYING, col, val);
	}
	
	/**
	 * Sets a default for a column for the given operation
	 * @param oper the operation (addition or querying)
	 * @param col the column name
	 * @param val the new default value
	 * @return true if successful
	 */
	private boolean setColDefault(int oper, String col, String val) {
		SQLiteDatabase con = db.getConn();
		ContentValues vals = new ContentValues();
		vals.put(DB_VAL, val);
		String where = DB_TABL + "=" + tableID + " and " + DB_OPER + "=" +
				oper + " and " + DB_TYPE + "=" + COL_VALUE + " and " + DB_KEY +
				"=" + db.toSafeSqlString(col);
		con.update(DEFOPTS_TABLE, vals, where, null);
		con.close();
		return true;
	}
	
	/**
	 * Sets whether a column should be included in all query responses
	 * @param col the column name
	 * @param inc whether the column should always be included
	 * @return true if successful
	 */
	public boolean setQueryIncCol(String col, boolean inc) {
		SQLiteDatabase con = db.getConn();
		ContentValues vals = new ContentValues();
		vals.put(DB_VAL, inc);
		String where = DB_TABL + "=" + tableID + " and " + DB_OPER + "=" +
				QUERYING + " and " + DB_TYPE + "=" + COL_RETURN + " and " +
				DB_KEY + "=" + db.toSafeSqlString(col);
		con.update(DEFOPTS_TABLE, vals, where, null);
		con.close();
		return true;
	}
	
	public boolean setAddAvailCol(String val) {
		return setAvailCol(ADDITION, val);
	}
	
	public boolean setQueryAvailCol(String val) {
		return setAvailCol(QUERYING, val);
	}
	
	private boolean setAvailCol(int oper, String val) {
		SQLiteDatabase con = db.getConn();
		String where = DB_TABL + "=" + tableID + " and " + DB_OPER + "=" +
				oper + " and " + DB_TYPE + "=" + OTHER + " and " + DB_KEY +
				"=" + db.toSafeSqlString(OAVAIL);
		Cursor cs = con.query(DEFOPTS_TABLE, null, where, null, null, null,
				null);
		ContentValues vals = new ContentValues();
		vals.put(DB_VAL, val);
		if(cs.getCount() > 0) {
			con.update(DEFOPTS_TABLE, vals, where, null);
		} else {
			vals.put(DB_TABL, tableID);
			vals.put(DB_OPER, oper);
			vals.put(DB_TYPE, OTHER);
			vals.put(DB_KEY, OAVAIL);
			con.insertOrThrow(DEFOPTS_TABLE, null, vals);
		}
		con.close();
		return true;
	}
	
	/**
	 * Adds fields to the default options table for a new column.
	 * @param colName the new column's name
	 */
	public void prepForNewCol(String colName) {
		ContentValues vals1 = new ContentValues();
		vals1.put(DB_TABL, tableID);
		vals1.put(DB_OPER, ADDITION);
		vals1.put(DB_TYPE, COL_VALUE);
		vals1.put(DB_KEY, colName);
		vals1.put(DB_VAL, "");
		ContentValues vals2 = new ContentValues();
		vals2.put(DB_TABL, tableID);
		vals2.put(DB_OPER, QUERYING);
		vals2.put(DB_TYPE, COL_VALUE);
		vals2.put(DB_KEY, colName);
		vals2.put(DB_VAL, "");
		ContentValues vals3 = new ContentValues();
		vals3.put(DB_TABL, tableID);
		vals3.put(DB_OPER, QUERYING);
		vals3.put(DB_TYPE, COL_RETURN);
		vals3.put(DB_KEY, colName);
		vals3.put(DB_VAL, "0");
		SQLiteDatabase con = db.getConn();
		con.insertOrThrow(DEFOPTS_TABLE, null, vals1);
		con.insertOrThrow(DEFOPTS_TABLE, null, vals2);
		con.insertOrThrow(DEFOPTS_TABLE, null, vals3);
		con.close();
	}
	
	/**
	 * Adds row to the default options table for a new table.
	 * @param tableID the new table's ID
	 */
	public void prepForNewTable(String tableID) {
		ContentValues vals1 = new ContentValues();
		vals1.put(DB_TABL, tableID);
		vals1.put(DB_OPER, ADDITION);
		vals1.put(DB_TYPE, OTHER);
		vals1.put(DB_KEY, OAVAIL);
		SQLiteDatabase con = db.getConn();
		con.insertOrThrow(DEFOPTS_TABLE, null, vals1);
		con.close();
	}
	
}
