package yoonsung.odk.spreadsheet.Database;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public class DataTable {

	// Table name
	public static final String BASE_TABLE_NAME = "table";
	
	// Column Names
	public static final String DATA_ROWID = "RowID";
	public static final String DATA_PHONE_NUMBER_IN = "_phoneNumberIn";
	public static final String DATA_TIMESTAMP = "_timestamp";
	public static final String DATA_SYNC_ID = "_syncId";
	public static final String DATA_SYNC_TAG = "_syncTag";
	
	private String currentTableID;
	private String currentTableName;
	
	private DBIO db;
	private TableList tl;
	private TableProperty tp;
	private ColumnProperty cp;
	
	// Constructor
	public DataTable(String tableID) {
		
		this.db = new DBIO();
		this.tl = new TableList();
		this.tp = new TableProperty(tableID);
		this.cp = new ColumnProperty(tableID);
		
		this.currentTableID = tableID;
		if (tableID == null) {
			this.currentTableName = null;
		} else {
			this.currentTableName = tl.getTableName(tableID);
		}
		this.currentTableName = "tableName";
	}
	
	// Create a new column with this name. If there is a column
	// with this name, do-nothing.
	public void addNewColumn(String colName) {
		colName = colName.trim();
		if(isColumnExist(colName)) {
			return;
		}
		
		// Add new column 'data' table
		SQLiteDatabase con = db.getConn();
		con.execSQL("ALTER TABLE " + db.toSafeSqlColumn(currentTableName, false, null) 
					+ " ADD " + db.toSafeSqlString(colName) + " TEXT");
		con.close();
		// adding rows to the default options table
		(new DefaultsManager(currentTableID)).prepForNewCol(colName);
		
		// Update colOrder in TableProperty
		ArrayList<String> colOrder = tp.getColOrderArrayList();
		colOrder.add(colName);
		tp.setColOrder(colOrder);
	}	
	
	// Drop a column with this name. If no such a column exsit,
	// do-nothing.
	public void dropColumn(String colName) {
		
		if (isColumnExist(colName)) {
			// Drop from 'data' table
			String originalTable = currentTableName;
			String backupTable = "baktable";
			String SelColumns = "rowID, " + dropAColumnHelper(colName);
			String InsColumns = "rowID INTEGER PRIMARY KEY ASC, " + dropAColumnHelper(colName);
			
			SQLiteDatabase con = db.getConn();
			try {
				//con.beginTransaction();
				con.execSQL("CREATE TEMPORARY TABLE `" + backupTable + "`(" + InsColumns + ")");
				con.execSQL("INSERT INTO `" + backupTable + "` SELECT " + SelColumns + " FROM `" + originalTable + "`");
				con.execSQL("DROP TABLE `" + originalTable + "`");
				con.execSQL("CREATE TABLE `" + originalTable + "`(" + InsColumns + ")");
				con.execSQL("INSERT INTO `" + originalTable + "` SELECT " + SelColumns + " FROM `" + backupTable + "`");
				con.execSQL("DROP TABLE `" + backupTable + "`");
				//con.setTransactionSuccessful();
			} catch (Exception e) {
				Log.d("Data", "Drop Column Failed");
			} finally {
				//con.endTransaction();
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
		Cursor cs = con.rawQuery("SELECT * FROM " + db.toSafeSqlColumn(currentTableName, false, null), null);
		
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
		
	    for(Entry<String, Object> entry : values.valueSet()) {
	        DataUtils du = DataUtils.getInstance();
	        String key = entry.getKey();
            String val = entry.getValue().toString();
	        String colType = cp.getType(key);
	        if("Numeric Value".equals(colType)) {
	            try {
	                new Double(val);
	            } catch(NumberFormatException e) {
                    throw new IllegalArgumentException();
	            }
	        }
	    }
	    
	    values.put(DATA_PHONE_NUMBER_IN, phoneNumberIn);
	    values.put(DATA_TIMESTAMP, timeStamp);
	    
		SQLiteDatabase con = db.getConn();
		try {
			con.insertOrThrow(currentTableName, null, values);
		} catch (Exception e) {
			Log.d("Data", "Add Row failed." + e.getMessage());
		}
		con.close();
	}
	
	/*
	// Add new row with the specified information.
	public void addRow(ContentValues values, String phoneNumberIn, String timeStamp) {
		values.put(DATA_PHONE_NUMBER_IN, phoneNumberIn);
		values.put(DATA_TIMESTAMP, timeStamp);
		Log.e("DataCheck", values.toString());
		addRow(values);
	}
	*/
	
	public void updateRow(ContentValues values, int rowID) {
		
		SQLiteDatabase con = db.getConn();
		con.update(currentTableName, values, DATA_ROWID + "=" + rowID, null);
		con.close();
		
	}
	
	public void removeRow(String rowID) {
		
		SQLiteDatabase con = db.getConn();
		con.delete(currentTableName, db.toSafeSqlColumn(DATA_ROWID, false, null) + " = " + rowID, null);
		con.close();
		
	}
	
	public void updateTimestamp(int rowId, String timestamp) {
	    ContentValues values = new ContentValues();
	    values.put(DATA_TIMESTAMP, timestamp);
	    SQLiteDatabase con = db.getConn();
	    con.update(currentTableName, values, DATA_ROWID + " = ?",
	            new String[] { rowId + "" });
	    con.close();
	}
	 
	public Table getTable() {
        ArrayList<String> colOrder = new ArrayList<String>();
        colOrder.add("column1");
        colOrder.add("column2");
        colOrder.add("column3");
        colOrder.add("column4");
		
		// Main Frame
		return loadTable(true, colOrder, "column3", null);
		
	}
	
	/**
	 * @return a table that includes timestamps and source phone numbers at the end.
	 */
	public Table getCompleteTable() {
		ArrayList<String> colOrder = tp.getColOrderArrayList();
		colOrder.add("_timestamp");
		colOrder.add("_phoneNumberIn");
		return loadTable(true, colOrder, tp.getSortBy(), null);
	}
	
	public Table getTable(boolean isMain, Map<String, String> constraints) {
	    ArrayList<String> colOrder = tp.getColOrderArrayList();
	    String whereClause = "";
	    for (String key : constraints.keySet()) {
	        if (!colOrder.contains(key)) {
	            continue;
	        }
	        whereClause += " and " + db.toSafeSqlColumn(key, false, null) +
	                " = " + db.toSafeSqlString(constraints.get(key));
	    }
	    if (whereClause.length() == 0) {
	        whereClause = null;
	    } else {
	        whereClause = whereClause.substring(5);
	    }
	    Log.d("DT", "whereClause:" + whereClause);
        return loadTable(isMain, colOrder, tp.getSortBy(), whereClause);
	}
	
	public Table getTable(int matchRowId) {
		//ArrayList<String> colOrder = tp.getColOrderArrayList();
	    ArrayList<String> colOrder = new ArrayList<String>();
	    colOrder.add("column1");
	    colOrder.add("column2");
		String selection = DATA_ROWID + " = ?";
		String[] selectionArgs = {(new Integer(matchRowId)).toString()};
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.query(currentTableName, null, selection, selectionArgs,
				null, null, null, null);
		cs.moveToFirst();
		String whereClause = "";
		for(String colName : colOrder) {
			if(cp.getIsIndex(colName)) {
				int colIndex = cs.getColumnIndexOrThrow(colName);
				whereClause += " and " + db.toSafeSqlColumn(colName, false,
						null) + " = " + db.toSafeSqlString(cs.getString(
						colIndex));
			}
		}
		cs.close();
		con.close();
		if(whereClause.length() == 0) {
			whereClause = null;
		} else {
			whereClause = whereClause.substring(5);
		}
		// Into-History
		return loadTable(false, colOrder, "column2", whereClause);
		
	}
	
	public Table getTableByVal(String whereCol, String whereArg) {
        
        // Into-History
        String whereClause = db.toSafeSqlColumn(whereCol, false, null) + " = " + db.toSafeSqlString(whereArg);
        return loadTable(false, tp.getColOrderArrayList(), tp.getSortBy(), whereClause);
    }

	
	private Table loadTable(boolean isMainTable, 
							ArrayList<String> colOrder,
							String sortBy, 
							String whereClause) 
	{
		
		// Connect to database
		SQLiteDatabase con = db.getConn();
		
		// Select Data Table
		String tableSQL = prepareQueryForTable(isMainTable, colOrder, sortBy, whereClause);
		Log.d("dt", "tableSQL:" + tableSQL);
		Cursor cs = con.rawQuery(tableSQL, null);
		
		// Table's fields
		ArrayList<Integer> rowID = new ArrayList<Integer>();
		ArrayList<String> data = new ArrayList<String>();
		ArrayList<String> header = colOrder;
		ArrayList<String> footer = new ArrayList<String>();
		
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
			
			// Retrieve footer
			// Create a HashMap maps column name to function name
			ColumnProperty cp = new ColumnProperty(currentTableID);
			HashMap<String, String> colMapFunc = new HashMap<String, String>();
			for (int i = 0; i < colOrder.size(); i++) {
				String colName = colOrder.get(i);
				String func = cp.getFooterMode(colName);
				String sqlFunc = null;
				if (func == null) {
					sqlFunc = "";
				} else if (func.equals("None")) {
					sqlFunc = "";
				} else if (func.equals("Average")) {
					sqlFunc = "AVG";
				} else if (func.equals("Count")) {
					sqlFunc = "COUNT";
				} else if (func.equals("Max")) {
					sqlFunc = "MAX";
				} else if (func.equals("Min")) {
					sqlFunc = "MIN";
				}
				colMapFunc.put(colName, sqlFunc);
			}
			
			// Load footer
			String footerSQL = prepareQueryForFooter(colMapFunc, tableSQL);
			if (footerSQL != null) {
				cs = con.rawQuery(footerSQL, null);
				if (cs != null) {
					if (cs.moveToFirst()) {
						for (int i = 0; i < colOrder.size(); i++) {
							String colName = colOrder.get(i);
							String colFuncVal = colMapFunc.get(colName);
							String footerVal = cs.getString(cs.getColumnIndex(colName));
							if (colFuncVal == null || colFuncVal.equals("")) {
								footer.add("");
							} else {
								footer.add(footerVal);
							}
						}
					} else {
						for(int i=0; i<colOrder.size(); i++) {
							footer.add("");
						}
					}
				}
				cs.close();
			}
			con.close();
			
			return new Table(currentTableID, width, height, rowID, header, data, footer);
		} else {
			con.close();
			return new Table();
		}
		
	}
	
	private String prepareQueryForTable(boolean isMainTable, 
										ArrayList<String> colOrder,
										String sortBy,
										String whereClause) 
	{	
		Log.e("colOrder", colOrder.toString() + " " + colOrder.size());
		
		// No columns to visualize
		if (colOrder.size() == 0) {
			return "SELECT `" + DATA_ROWID + "` AS '" + DATA_ROWID + "'"
					+ " FROM " + db.toSafeSqlColumn(currentTableName, false, null) + " ";
		}
		
		// Select statement with ROWID
		String result = "SELECT " + db.toSafeSqlColumn(DATA_ROWID, true, null) + ", ";
	
		if (isMainTable) {
			// Main Table
			String indexList = "";
			for(String colName : colOrder) {
				if(colName.equals("column1") || colName.equals("column2")) {
					indexList += ", " + db.toSafeSqlColumn(colName, false, null);
				}
			}
			if (indexList.length() != 0) {
				indexList = indexList.substring(2);
				result += db.listColumns(colOrder, true, "MAX", sortBy)
						+ " FROM " + db.toSafeSqlColumn(currentTableName, false, null)
						+ (whereClause == null ? "" : " WHERE " + whereClause)
						+ " GROUP BY " + indexList
						+ " ORDER BY " + indexList + " ASC";
			} else {
				result += db.listColumns(colOrder, true, null, null) 
						+ " FROM " + db.toSafeSqlColumn(currentTableName, false, null);
                if (whereClause != null)
                    result += " WHERE " + whereClause;
				if (sortBy != null)
					result += " ORDER BY Cast(" + db.toSafeSqlColumn(sortBy, false, null) + " as integer)";
			}
		} else {
			// Into-History
			result += db.listColumns(colOrder, true, null, null)
					+ " FROM " + db.toSafeSqlColumn(currentTableName, false, null)
					+ (whereClause == null ? "" : (" WHERE " + whereClause));
			
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
		} else if(maxRows == -2) {
			limit = null;
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
		Cursor c = con.query(currentTableName, cols, reqList, null, null, null,
				orderby, limit);
		Set<Map<String, String>> result = new HashSet<Map<String, String>>();
		boolean empty = !c.moveToFirst();
		i = 0;
		while(!empty) {
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
	
	private String prepareQueryForFooter(HashMap<String, String> colMapFunc, String tableSQL) {
		
		if (colMapFunc.size() == 0)
			return null;
		
		String result = "SELECT " + db.listColumns(colMapFunc, true)
					  + " FROM (" + tableSQL + ")"; 
		Log.e("FooterMode", result);
		return result;
		
	}
	
	/**
	 * Gets counts of rows grouped by column value.
	 * @param col the column to group by
	 * @return a map from values to counts
	 */
	public Map<String, Integer> getCounts(String col) {
		Map<String, Integer> res = new HashMap<String, Integer>();
		SQLiteDatabase con = db.getConn();
		String[] columns = {col, "COUNT(*)"};
		Cursor cs = con.query(currentTableName, columns, null, null, col, null,
				null);
		int valIndex = cs.getColumnIndexOrThrow(col);
		int countIndex = cs.getColumnIndexOrThrow("COUNT(*)");
		while(cs.moveToNext()) {
			String val = cs.getString(valIndex);
			int count = cs.getInt(countIndex);
			res.put(val, count);
		}
		con.close();
		return res;
	}
	
	/**
	 * Deletes a row.
	 * @param rowId the ID of the row to delete
	 */
	public void deleteRow(int rowId) {
		SQLiteDatabase con = db.getConn();
		String whereClause = DATA_ROWID + " = ?";
		String[] whereArgs = {(new Integer(rowId)).toString()};
		con.delete(currentTableName, whereClause, whereArgs);
		con.close();
	}
	
	/**
	 * Checks whether the value is valid for the column's type.
	 * @param colName the name of the column
	 * @param value the value
	 * @return true if it is a valid value; false otherwise
	 */
	public boolean isValidValue(String colName, String value) {
	    Log.d("dt", "isValidValue called:" + colName + "/" + value);
	    String colType = cp.getType(colName);
	    if("None".equals(colType)) {
	        return true;
	    } else if("Text".equals(colType)) {
	        return true;
	    } else if("Numeric Value".equals(colType)) {
	        return value.matches("\\d+");
	    } else if("Date".equals(colType)) {
	        return (DataUtils.getInstance().parseDateTime(value) != null);
	    } else if("Date Range".equals(colType)) {
	        return (DataUtils.getInstance().parseDateRange(value) != null);
	    } else if("Phone Number".equals(colType)) {
	        return true;
	    } else {
	        return true;
	    }
	}
	
	public boolean isHiddenColumn(String colName) {
	    return (colName.equals(DATA_PHONE_NUMBER_IN) ||
	            colName.equals(DATA_TIMESTAMP));
	}
}
