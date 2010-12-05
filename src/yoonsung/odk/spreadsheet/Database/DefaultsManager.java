package yoonsung.odk.spreadsheet.Database;

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
	private static final String DEFOPTS_TABLE = "defaultopts";
	/** the name of the operation column */
	private static final String DB_OPER = "operation";
	/** the name of the type column */
	private static final String DB_TYPE = "type";
	/** the name of the key column */
	private static final String DB_KEY = "itemkey";
	/** the name of the value column */
	private static final String DB_VAL = "itemval";
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
	
	/**
	 * Constructs a new DefaultsManager
	 */
	public DefaultsManager() {
		db = new DBIO();
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
		String sel = DB_OPER + "=" + oper + " and " + DB_TYPE + "=" +
				COL_VALUE;
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
		String sel = DB_OPER + "=" + QUERYING + " and " + DB_TYPE + "=" +
				COL_RETURN + " and " + DB_VAL + "=1";
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
		String sel = db.toSafeSqlColumn(DB_OPER, false, null) + "=" + oper +
				" and " + db.toSafeSqlColumn(DB_TYPE, false, null) + "=" +
				OTHER + " and " + db.toSafeSqlColumn(DB_KEY, false, null) +
				"='" + OAVAIL + "'";
		Cursor c = con.query(DEFOPTS_TABLE, cols, sel, null, null, null, null,
				"1");
		String r = null;
		if(c.moveToFirst()) {
			r = c.getString(c.getColumnIndexOrThrow(DB_VAL));
		}
		c.close();
		con.close();
		if(r.length() == 0) {return null;}
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
		String where = DB_OPER + "=" + oper + " and " + DB_TYPE + "=" +
				COL_VALUE + " and " + DB_KEY + "=" + db.toSafeSqlString(col);
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
		String where = DB_OPER + "=" + QUERYING + " and " + DB_TYPE + "=" +
				COL_RETURN + " and " + DB_KEY + "=" + db.toSafeSqlString(col);
		con.update(DEFOPTS_TABLE, vals, where, null);
		con.close();
		return true;
	}
	
	public boolean setQueryAvailCol(String val) {
		return setAvailCol(QUERYING, val);
	}
	
	private boolean setAvailCol(int oper, String val) {
		SQLiteDatabase con = db.getConn();
		ContentValues vals = new ContentValues();
		vals.put(DB_VAL, val);
		String where = DB_OPER + "=" + oper + " and " + DB_TYPE + "=" + OTHER +
				" and " + DB_KEY + "=" + db.toSafeSqlString(OAVAIL);
		con.update(DEFOPTS_TABLE, vals, where, null);
		con.close();
		return true;
	}
	
}
