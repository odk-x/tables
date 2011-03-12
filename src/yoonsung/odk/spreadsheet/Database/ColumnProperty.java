package yoonsung.odk.spreadsheet.Database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/*
 * Database object that allows users to set and 
 * get column properties from the database.
 * To this to work, you must have a table for the
 * column property.
 * 
 * @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class ColumnProperty {

	// Table Name
	public static final String COLUMN_PROPERTY = "colProperty";
	
	// Column Names
	public static final String COLUMN_PROPERTY_TABLE_ID = "tableID";
	public static final String COLUMN_PROPERTY_NAME = "colName"; 
	public static final String COLUMN_PROPERTY_ABRV = "abreviation";
	public static final String COLUMN_PROPERTY_TYPE = "type";
	public static final String COLUMN_PROPERTY_SMSIN = "SMSIN";
	public static final String COLUMN_PROPERTY_SMSOUT = "SMSOUT";
	public static final String COLUMN_PROPERTY_FOOTER_MODE = "footerMode";
	public static final String COLUMN_PROPERTY_ISINDEX = "isIndex";
	
	// Database connection
	private DBIO db;
	private String tableID;
	
	// Constructor
	public ColumnProperty(String tableID) {
		this.db = new DBIO();
		this.tableID = tableID;
	}

	public String getName(String colName) {
		return getProperty(colName, COLUMN_PROPERTY_NAME);
	}
	
	public String getNameByAbrv(String abrv) {
    	SQLiteDatabase con = db.getConn();
    	
    	//String[] spec = {colName};
    	Cursor cs = con.rawQuery("SELECT * FROM " +  db.toSafeSqlColumn(COLUMN_PROPERTY, false, null) 
    							 + " WHERE " + db.toSafeSqlColumn(COLUMN_PROPERTY_ABRV, false, null) 
    							 +  " = " + db.toSafeSqlString(abrv)
    							 + " AND " + db.toSafeSqlColumn(COLUMN_PROPERTY_TABLE_ID, false, null)
    							 + " = " + tableID, null);
    	if (cs != null) {
    		int colIndex = cs.getColumnIndex(COLUMN_PROPERTY_NAME);
    		if (cs.moveToFirst() && !cs.isNull(colIndex)) {
    			String result = cs.getString(colIndex); 
    			cs.close();
    			con.close();
            	return result;
    		}
    	}
    	
    	cs.close();
    	con.close();
    	return null;
	}
	
	public void setName(String colName, String newVal) {
		setProperty(colName, COLUMN_PROPERTY_NAME, newVal);
	}
	
	public String getAbrev(String colName) {
    	return getProperty(colName, COLUMN_PROPERTY_ABRV);
    }
	
	public void setAbrev(String colName, String newVal) {
		setProperty(colName, COLUMN_PROPERTY_ABRV, newVal);
	}
    
    public String getType(String colName) {
    	return getProperty(colName, COLUMN_PROPERTY_TYPE);
    }
    
    public void setType(String colName, String newVal) {
		setProperty(colName, COLUMN_PROPERTY_TYPE, newVal);
	}
    
    public boolean getSMSIN(String colName) {
    	String result = getProperty( colName, COLUMN_PROPERTY_SMSIN);
    	if (result == null) {
    		return false;
    	} else if (result.equals("0")) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public void setSMSIN(String colName, boolean newVal) {
    	String stringVal;
    	if (newVal)
    		stringVal = "1";
    	else
    		stringVal = "0";
		setProperty(colName, COLUMN_PROPERTY_SMSIN, stringVal);
	}
    
    public boolean getSMSOUT(String colName) {
    	String result = getProperty( colName, COLUMN_PROPERTY_SMSOUT);
    	if (result == null) {
    		return false;
    	} else if (result.equals("0")) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public void setSMSOUT(String colName, boolean newVal) {
    	String stringVal;
    	if (newVal)
    		stringVal = "1";
    	else
    		stringVal = "0";
		setProperty(colName, COLUMN_PROPERTY_SMSOUT, stringVal);
	}
    
    public String getFooterMode(String colName) {
    	return getProperty(colName, COLUMN_PROPERTY_FOOTER_MODE);
    }
    
    public void setFooterMode(String colName, String newVal) {
		setProperty(colName, COLUMN_PROPERTY_FOOTER_MODE, newVal);
	}
    
    public boolean getIsIndex(String colName) {
    	String result = getProperty( colName, COLUMN_PROPERTY_ISINDEX);
    	if (result == null) {
    		return false;
    	} else if (result.equals("0")) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public void setIsIndex(String colName, boolean newVal) {
    	String stringVal;
    	if (newVal)
    		stringVal = "1";
    	else
    		stringVal = "0";
		setProperty(colName, COLUMN_PROPERTY_ISINDEX, stringVal);
	}
    
    public void removeAll() {
    	SQLiteDatabase con = db.getConn();
    	con.delete(COLUMN_PROPERTY, COLUMN_PROPERTY_TABLE_ID+"="+tableID, null);
    }
    
    // Returns null if nothing defined.
    private String getProperty(String colName, String propertyType) {
    	SQLiteDatabase con = db.getConn();;
    	
    	//String[] spec = {colName};
    	Cursor cs = con.rawQuery("SELECT * FROM " +  db.toSafeSqlColumn(COLUMN_PROPERTY, false, null) 
    							 + " WHERE " + db.toSafeSqlColumn(COLUMN_PROPERTY_NAME, false, null) 
    							 + " = " + db.toSafeSqlString(colName) 
    							 + " AND " + db.toSafeSqlColumn(COLUMN_PROPERTY_TABLE_ID, false, null)
    							 + " = " + tableID, null);
    	if (cs != null) {
    		int colIndex = cs.getColumnIndex(propertyType);
    		if (cs.moveToFirst() && !cs.isNull(colIndex)) {
    			String result = cs.getString(colIndex); 
    			cs.close();
    			con.close();
            	return result;
    		}
    	}
    	
    	cs.close();
    	con.close();
    	return null;
    }
	
    // Set a new value on this column property.
    private void setProperty(String colName, String propertyType, String propertyValue) {
    	SQLiteDatabase con = db.getConn();
    	
        if (isInsert(colName, propertyType)) {
        	// INSERT
        	try {
        		ContentValues values = new ContentValues();
        		values.put(COLUMN_PROPERTY_TABLE_ID, tableID);
        		values.put(COLUMN_PROPERTY_NAME, colName);
        		values.put(propertyType, propertyValue);
        		con.insertOrThrow(COLUMN_PROPERTY, null, values);
        	} catch(Exception e) {
        		Log.d("ColumnProperty", "Insert Failed: " + e.getMessage());
        	}
        } else {
        	// UPDATE
        	try {
        		con.execSQL("UPDATE " + db.toSafeSqlColumn(COLUMN_PROPERTY,false, null) 
        					+ " SET " +  db.toSafeSqlColumn(propertyType,false, null) 
        					+ " = " + db.toSafeSqlString(propertyValue) 
        					+ " WHERE " + db.toSafeSqlColumn(COLUMN_PROPERTY_NAME, false, null) 
        					+ " = " + db.toSafeSqlString(colName)
        					+ " AND " + db.toSafeSqlColumn(COLUMN_PROPERTY_TABLE_ID, false, null)
        					+ " = " + tableID);
        	} catch (Exception e) {
        		Log.d("ColumnProperty", "Update Failed: " + e.getMessage());
        	}
        }
        
    	con.close();
    }
    
    // Check with database if 'insert' is need for this column property.
    private boolean isInsert(String colName, String propertyType) {
    	SQLiteDatabase con = db.getConn();
    	
        int count = 0;
        try {
        	String query = "SELECT * FROM " + db.toSafeSqlColumn(COLUMN_PROPERTY, false, null) 
        				 + " WHERE " + db.toSafeSqlColumn(COLUMN_PROPERTY_NAME, false, null) 
        				 + " = " + db.toSafeSqlString(colName)
        				 + " AND " + db.toSafeSqlColumn(COLUMN_PROPERTY_TABLE_ID, false, null)
        				 + " = " + tableID;
        	Cursor cs = con.rawQuery(query, null);
        	count = cs.getCount();
        	cs.close();
        } catch (Exception e) {
        	Log.d("ColumnProperty", "isInsert() Failed");
        }
        
        con.close();
        return count == 0;
    }
    
}

