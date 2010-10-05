package com.yoonsung.spreadsheetsms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TableProperty {

	public static final String PROPERTY_DBTABLE_PRIME = "prime";
	public static final String PROPERTY_DBTABLE_SORT_BY = "sortBy";
	public static final String PROPERTY_DBTABLE_ORDER_BY = "colOrder";
	
	private DBIO db;
	
	private String prime;
	private String sortBy;
	private ArrayList<String> colOrder;
	private HashMap<String, ColProperty> cols;
	
	public TableProperty() {
		reload();
	}
	
	public void reload() {
		initialize();
		loadTableProperty();
		validateTableProperty();
		loadColProperty();
	}
	
	private void initialize() {
		this.db = new DBIO();
		this.prime = null;
		this.sortBy = null;
		this.colOrder = new ArrayList<String>();
		this.cols = new HashMap<String, ColProperty>();
	}
	
	public void loadTableProperty() {
		SQLiteDatabase db = new DBIO().getConn();
		Cursor cs = db.rawQuery("SELECT * FROM tableProperty", null);
		if (cs != null) {
			if (cs.moveToFirst()) {
				do {
					// Set prime and sortBy
					for (int i = 0; i < cs.getColumnCount(); i++) {
						if (cs.getColumnName(i).equals("prime")) {
							this.prime = cs.getString(i);
						} else if (cs.getColumnName(i).equals("sortBy")) {
							this.sortBy = cs.getString(i);
						} else if (cs.getColumnName(i).equals("colOrder")) {
							//this.order = cs.getString(i);
							Log.e("ORDER", "got in order");
							stringToOrderArray(cs.getString(i));
						}
					}
				} while (cs.moveToNext());
			}
		}
		db.close();
	}
	// Check if colOrder and actual column names are synced
	private void validateTableProperty() {
		// Connect DB
		SQLiteDatabase db = new DBIO().getConn();
		// Select Data Table
		Cursor cs = db.rawQuery("SELECT * FROM data", null);
		// Column Names
		boolean colNamesVSOrder = true;
		
		List<String> list = Arrays.asList(cs.getColumnNames());
		ArrayList<String> colNames = new ArrayList<String>(list);
		for (int i = 0; i < colNames.size(); i++) {
			if (!colNames.get(i).equals(Table.ROWID) 
				&& !colOrder.contains(colNames.get(i)))
					colNamesVSOrder = false;
		}
		if (colNamesVSOrder == false) {
			Log.e("COLVALIDATION", "Column validation caught an error");
			colOrder = colNames;
		}
	}
	
	public void loadColProperty() {
		SQLiteDatabase db = new DBIO().getConn();
		Cursor cs = db.rawQuery("SELECT * FROM colProperty", null);
		if (cs != null) {
			if (cs.moveToFirst()) {
				do {
					// Set column properties
					HashMap<String, String> col = new HashMap<String, String>();
					for (int i = 0; i < cs.getColumnCount(); i++) {
						col.put(cs.getColumnName(i), cs.getString(i));
					}
					// Add to columns hash map
					cols.put(col.get(ColProperty.PROPERTY_DBCOL_NAME), 
							 new ColProperty(col.get(ColProperty.PROPERTY_DBCOL_NAME), 
									 		 col.get(ColProperty.PROPERTY_DBCOL_ABRV),
									 		 col.get(ColProperty.PROPERTY_DBCOL_TYPE),
									 		 col.get(ColProperty.PROPERTY_DBCOL_FOOTER_MODE)));
				} while (cs.moveToNext());
			}
		}
		db.close();
	}
	
	// setPrime
	public void setPrime(String value) {
		// DB
		setDB("prime", value);
		reload();
	}
	
	// getPrime
	public String getPrime() {
		return this.prime;
	}
	
	// setSortBy
	public void setSortBy(String value) {
		// DB
		setDB("sortBy", value);
		reload();
	}
	
	// getSortBy
	public String getSortBy() {
		return this.sortBy;
	}
	
	// setOrder
	public void setColOrder(ArrayList<String> order) {
		setDB("colOrder", orderArrayToString(order));
		reload();
	}
	
	// getOrder
	public ArrayList<String> getColOrder() {
		return this.colOrder;
	}
	
	
	private void setDB(String col, String val) {
		// DB
		SQLiteDatabase con = db.getConn();
		
		// Pack the new entry
		ContentValues values = new ContentValues();
		values.put(col, val);

		// Update or Insert
		if (isInsert()) {
			con.insert("tableProperty", null, values);
			Log.e("newcol", "insert");
			
		} else {
			con.update("tableProperty", values, "rowid = 1", null);
			Log.e("newcol", "update");
		}
		
		con.close();	
	}
		
	private boolean isInsert() {
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM tableProperty WHERE rowid = 1", null);
		int count = cs.getCount();
		cs.close();
		con.close();
		return (count == 0);
	}
	
	
	// getColProperty(String name)
	public ColProperty getColProperty(String name) {
		return cols.get(name);
	}
	
	// stringToOderLinkedList
	private void stringToOrderArray(String stringOrder) {
		// Convert string to a linked list.
		String[] tokens = stringOrder.split(";");
		for(String token : tokens) {
			this.colOrder.add(token);
		}
		Log.e("ArrayCheck", colOrder.toString());
	}
	
	private String orderArrayToString(ArrayList<String> order) {
		String result = "";
		for (int i = 0; i < order.size(); i++) {
			result += order.get(i) + ";";
		}
		return result;
	}
	
	// ColProperty
	public class ColProperty {
		
		public static final String PROPERTY_DBCOL_NAME = "name";
		public static final String PROPERTY_DBCOL_ABRV = "abbreviation";
		public static final String PROPERTY_DBCOL_TYPE = "type";
		public static final String PROPERTY_DBCOL_FOOTER_MODE = "footerMode";
		
		private String name;
		private String abrv;
		private String type;
		private String footerMode;
		
		public ColProperty(String name, String abrv, String type, String footerMode) {
			this.name = name;
			this.abrv = abrv;
			this.type = type;
			this.footerMode = footerMode;
		}
		
		public String getName() {
			return this.name;
		}
		
		public String getAbbreviation() {
			return this.abrv;
		}
		
		public String getType() {
			return this.type;
		}
		
		public String getFooterMode() {
			return this.footerMode;
		}
		
	}
	
}
