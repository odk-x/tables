package com.yoonsung.spreadsheetsms;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBIO {
	//public static final String DB_PATH = "/data/data/com.yoonsung.spreadsheetsms/databases/";
	public static final String DB_PATH = "/sdcard/";
	public static final String DB_NAME = "my_tables";
	public static final String DB_DATA_TABLE_NAME = "data";
	public static final String DB_TABLE_PROPERTY_TABLE_NAME = "tableProperty";
	public static final String DB_COL_PROPERTY_TABLE_NAME = "colProperty";
	
	public DBIO() {
		
	}
	
	public SQLiteDatabase getConn() {
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + DB_NAME, null);
		return db;
	}
	
	
	
	// changeColName(original, new)
	// changeColType(colName, type)
	// changeColPrime(colName, prime)
	// changeColSortBy(colName, sortBy)
	// changeColFooterMode(colName, footerMode)
	
	// updateRow(rowID, array of stuffs)
	// createRow(rowID, array of stuffs)
	// deleteRow(rowID)
	// updateCol(colName, properties)
	// createCol(colName, properties)
	// deleteCol(colName)
	
}
