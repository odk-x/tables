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
package org.opendatakit.tables.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.data.Query.SqlData;
import org.opendatakit.tables.sync.SyncUtil;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and modifying a user table.
 *
 * @author hkworden@gmail.com (Hilary Worden)
 * @author sudar.sam@gmail.com
 */
public class DbTable {

  private final static String TAG = "DbTable";

    /********************************************************
     * Default values for those columns which require them.
     ********************************************************/
    // some of these are unfortunately littered in various places throughout
    // the code. I don't have time to track them down at the moment, but they
    // default values should probably be centralized here.
    // TODO: see above




    /*
     * These are the columns that are present in any row in the database.
     * Each row should have these in addition to the user-defined columns.
     * If you add a column here you have to be sure to also add it in the
     * create table statement, which can't be programmatically created easily.
     */
    private static final List<String> ADMIN_COLUMNS;

    /*
     * These are the columns that we want to include in sync rows to sync up
     * to the server. This is a work in progress that is being added later, so
     * I can't promise that there isn't some magic happening elsewhere that I
     * am missing. Hopefully this will be exhaustive, however. It is a map of
     * column name to column type that we will be putting into a row for
     * SyncProcessor. (At least that is the obvious place I'm making this for).
     */
    private static final Map<String, ColumnType> COLUMNS_TO_SYNC;

    static {
      ADMIN_COLUMNS = new ArrayList<String>();
      ADMIN_COLUMNS.add(DataTableColumns.ROW_ID);
      ADMIN_COLUMNS.add(DataTableColumns.URI_USER);
      ADMIN_COLUMNS.add(DataTableColumns.SYNC_TAG);
      ADMIN_COLUMNS.add(DataTableColumns.SYNC_STATE);
      ADMIN_COLUMNS.add(DataTableColumns.TRANSACTIONING);
      ADMIN_COLUMNS.add(DataTableColumns.TIMESTAMP);
      ADMIN_COLUMNS.add(DataTableColumns.SAVED);
      ADMIN_COLUMNS.add(DataTableColumns.FORM_ID);
      ADMIN_COLUMNS.add(DataTableColumns.INSTANCE_NAME);
      ADMIN_COLUMNS.add(DataTableColumns.LOCALE);
      // put the columns in to the to-sync map.
      COLUMNS_TO_SYNC = new HashMap<String, ColumnType>();
      COLUMNS_TO_SYNC.put(DataTableColumns.URI_USER, ColumnType.PHONE_NUMBER);
      COLUMNS_TO_SYNC.put(DataTableColumns.TIMESTAMP, ColumnType.DATETIME);
      COLUMNS_TO_SYNC.put(DataTableColumns.INSTANCE_NAME, ColumnType.TEXT);
    }

    /**
     * Return an unmodifiable list of the admin columns that must be present
     * in every database table.
     * @return
     */
    public static List<String> getAdminColumns() {
      return Collections.unmodifiableList(ADMIN_COLUMNS);
    }

    public enum SavedStatus {
    	COMPLETE,
    	INCOMPLETE
    };

    public static final String DB_CSV_COLUMN_LIST =
        DataTableColumns.ROW_ID + ", " + DataTableColumns.URI_USER +
        ", " + DataTableColumns.SYNC_TAG + ", " + DataTableColumns.SYNC_STATE + ", " + DataTableColumns.TRANSACTIONING +
        ", " + DataTableColumns.TIMESTAMP + ", " + DataTableColumns.SAVED + ", " + DataTableColumns.FORM_ID +
        ", " + DataTableColumns.INSTANCE_NAME + ", " + DataTableColumns.LOCALE;

    private final DataUtil du;
    private final DbHelper dbh;
    private final TableProperties tp;

    public static DbTable getDbTable(DbHelper dbh, String tableId) {
        return new DbTable(dbh, tableId);
    }

    public static Map<String, ColumnType> getColumnsToSync() {
      return Collections.unmodifiableMap(COLUMNS_TO_SYNC);
    }

    private DbTable(DbHelper dbh, String tableId) {
        this.du = DataUtil.getDefaultDataUtil();
        this.dbh = dbh;
        this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
            KeyValueStore.Type.ACTIVE);
        // so this looks like the problem, needs to somehow know if it should
        // be drawing the props from the server table (if you'd dl'ing a table)
        // or if you're creating a table and therefore want the active.
    }

    static void createDbTable(SQLiteDatabase db, TableProperties tp) {
      boolean testOpen = db.isOpen();
        StringBuilder colListBuilder = new StringBuilder();
        for (ColumnProperties cp : tp.getColumns()) {
            colListBuilder.append(", " + cp.getElementKey());
            if (cp.getColumnType() == ColumnType.NUMBER) {
                colListBuilder.append(" REAL");
            } else if (cp.getColumnType() == ColumnType.INTEGER) {
                colListBuilder.append(" INTEGER");
            } else {
                colListBuilder.append(" TEXT");
            }
        }
        testOpen = db.isOpen();
        String toExecute = "CREATE TABLE " + tp.getDbTableName() + "(" +
            DataTableColumns.ROW_ID + " TEXT NOT NULL" +
     ", " + DataTableColumns.URI_USER + " TEXT NULL" +
     ", " + DataTableColumns.SYNC_TAG + " TEXT NULL" +
     ", " + DataTableColumns.SYNC_STATE + " INTEGER NOT NULL" +
     ", " + DataTableColumns.TRANSACTIONING + " INTEGER NOT NULL" +
     ", " + DataTableColumns.TIMESTAMP + " INTEGER NOT NULL" +
     ", " + DataTableColumns.SAVED + " TEXT NULL" +
     ", " + DataTableColumns.FORM_ID + " TEXT NULL" +
     ", " + DataTableColumns.INSTANCE_NAME + " TEXT NOT NULL" +
     ", " + DataTableColumns.LOCALE + " TEXT NULL" +
     colListBuilder.toString() +
     ")";
        db.execSQL(toExecute);
    }

    /**
     * @return a raw table of all the data in the table
     */
    public Table getRaw() {
        return getRaw(null, null, null, null);
    }

    /**
     * Gets a table of raw data.
     * @param columns the columns to select (if null, all columns will be
     * selected)
     * @param selectionKeys the column names for the WHERE clause (can be null)
     * @param selectionArgs the selection arguments (can be null)
     * @param orderBy the column to order by (can be null)
     * @return a Table of the requested data
     */
    public Table getRaw(ArrayList<String> columns, String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
        if (columns == null) {
            ColumnProperties[] cps = tp.getColumns();
             columns = new ArrayList<String>();
             columns.addAll(ADMIN_COLUMNS);
//            columns = new ArrayList<String>();
//            columns.add(DB_URI_USER);
//            columns.add(DB_LAST_MODIFIED_TIME);
//            columns.add(DB_SYNC_TAG);
//            columns.add(DB_SYNC_STATE);
//            columns.add(DB_TRANSACTIONING);
//            need to add this stuff now;
            for (int i = 0; i < cps.length; i++) {
            	columns.add(cps[i].getElementKey());
            }
        }
        String[] colArr = new String[columns.size() + 1];
        colArr[0] = DataTableColumns.ROW_ID;
        for (int i = 0; i < columns.size(); i++) {
            colArr[i + 1] = columns.get(i);
        }
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
	        db = dbh.getReadableDatabase();
	        c = db.query(tp.getDbTableName(), colArr,
	                buildSelectionSql(selectionKeys),
	                selectionArgs, null, null, orderBy);
	        Table table = buildTable(c, columns);
	        return table;
	    } finally {
	    	try {
	    		if ( c != null && !c.isClosed() ) {
	    			c.close();
	    		}
	    	} finally {
	        // TODO: fix the when to close problem

//	    		if ( db != null ) {
//	    			db.close();
//	    		}
	    	}
	    }
    }

    public Table getRaw(Query query, String[] columns) {
        return dataQuery(query.toSql(columns));
    }

    public UserTable getUserTable(Query query) {
        Table table = dataQuery(query.toSql(tp.getColumnOrder()));
        return new UserTable(table.getRowIds(), getUserHeader(),
                table.getData(), footerQuery(query));
    }

    public UserTable getUserOverviewTable(Query query) {
        Table table = dataQuery(query.toOverviewSql(tp.getColumnOrder()));
        return new UserTable(table.getRowIds(), getUserHeader(),
                table.getData(), footerQuery(query));
    }

    public GroupTable getGroupTable(Query query, ColumnProperties groupColumn,
            Query.GroupQueryType type) {
    	SQLiteDatabase db = null;
    	Cursor c = null;
    	try {
	        SqlData sd = query.toGroupSql(groupColumn.getElementKey(), type);
	        db = dbh.getReadableDatabase();
	        c = db.rawQuery(sd.getSql(), sd.getArgs());
	        int gcColIndex = c.getColumnIndexOrThrow(
	                groupColumn.getElementKey());
	        int countColIndex = c.getColumnIndexOrThrow("g");
	        int rowCount = c.getCount();
	        String[] keys = new String[rowCount];
	        double[] values = new double[rowCount];
	        c.moveToFirst();
	        for (int i = 0; i < rowCount; i++) {
	            keys[i] = c.getString(gcColIndex);
	            values[i] = c.getDouble(countColIndex);
	            c.moveToNext();
	        }
	        return new GroupTable(keys, values);
	    } finally {
	    	try {
	    		if ( c != null && !c.isClosed() ) {
	    			c.close();
	    		}
	    	} finally {
	        // TODO: fix the when to close problem

//	    		if ( db != null ) {
//	    			db.close();
//	    		}
	    	}
	    }
    }

    public ConflictTable getConflictTable(Query query) {
    	SQLiteDatabase db = null;
    	Cursor c = null;
    	try {
	        db = dbh.getReadableDatabase();
	        SqlData sd = query.toConflictSql();
	        c = db.rawQuery(sd.getSql(), sd.getArgs());
	        Log.d(TAG, sd.getSql());
	        int count = c.getCount() / 2;
	        Log.d(TAG, "cursor count: " + c.getCount());
	        String[] header = new String[tp.getColumns().length];
	        String[] rowIds = new String[count];
	        String[][] syncTags = new String[count][2];
	        String[][][] values = new String[count][2][tp.getColumns().length];
	        if (count == 0) {
	            return new ConflictTable(header, rowIds, syncTags, values);
	        }
	        int idColIndex = c.getColumnIndexOrThrow(DataTableColumns.ROW_ID);
	        int stColIndex = c.getColumnIndexOrThrow(DataTableColumns.SYNC_TAG);
	        int[] colIndices = new int[tp.getColumns().length];
	        for (int i = 0; i < tp.getColumns().length; i++) {
	            colIndices[i] = c.getColumnIndexOrThrow(
	                    tp.getColumns()[i].getElementKey());
	            header[i] = tp.getColumns()[i].getDisplayName();
	        }
	        c.moveToFirst();
	        for (int i = 0; i < count; i++) {
	            rowIds[i] = c.getString(idColIndex);
	            syncTags[i][0] = c.getString(stColIndex);
	            for (int j = 0; j < tp.getColumns().length; j++) {
	                values[i][0][j] = c.getString(colIndices[j]);
	            }
	            c.moveToNext();
	            syncTags[i][1] = c.getString(stColIndex);
	            for (int j = 0; j < tp.getColumns().length; j++) {
	                values[i][1][j] = c.getString(colIndices[j]);
	            }
	            c.moveToNext();
	        }
	        return new ConflictTable(header, rowIds, syncTags, values);
	    } finally {
	    	try {
	    		if ( c != null && !c.isClosed() ) {
	    			c.close();
	    		}
	    	} finally {
	        // TODO: fix the when to close problem
//	    		if ( db != null ) {
//	    			db.close();
//	    		}
	    	}
	    }
    }

    private Table dataQuery(SqlData sd) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
        	db = dbh.getReadableDatabase();
        	c = db.rawQuery(sd.getSql(), sd.getArgs());
        	Table table = buildTable(c, tp.getColumnOrder());
            return table;
        } finally {
        	try {
        		if ( c != null && !c.isClosed() ) {
        			c.close();
        		}
        	} finally {
           // TODO: fix the when to close problem
//        		if ( db != null ) {
//        			db.close();
//        		}
        	}
        }
    }

    /**
     * Builds a Table with the data from the given cursor.
     * The cursor, but not the columns array, must include the row ID column.
     */
    private Table buildTable(Cursor c, ArrayList<String> arrayList) {
      //Log.i(TAG, "entered dbTable buildTable");
        int[] colIndices = new int[arrayList.size()];
        int rowCount = c.getCount();
        String[] rowIds = new String[rowCount];
        String[][] data = new String[rowCount][arrayList.size()];
        int rowIdIndex = c.getColumnIndexOrThrow(DataTableColumns.ROW_ID);
        for (int i = 0; i < arrayList.size(); i++) {
            colIndices[i] = c.getColumnIndexOrThrow(arrayList.get(i));
        }

        DataUtil du = DataUtil.getDefaultDataUtil();

        c.moveToFirst();
        for (int i = 0; i < rowCount; i++) {
//          Log.i(TAG, "i (row): " + i);
            rowIds[i] = c.getString(rowIdIndex);
            for (int j = 0; j < arrayList.size(); j++) {
//              Log.i(TAG, " j (column): " + j);
              String value;
              try {
                value = c.getString(colIndices[j]);
              } catch (Exception e) {
                try {
                  value = String.valueOf(c.getLong(colIndices[j]));
                } catch (Exception f) {
                  try {
                    value = String.valueOf(c.getInt(colIndices[j]));
                  } catch (Exception g) {
	                value = String.valueOf(c.getDouble(colIndices[j]));
	              }
                }
              }
        	  data[i][j] = value;
            }
            c.moveToNext();
        }
        //Log.i(TAG, "leaving Table buildTable");
        return new Table(rowIds, arrayList, data);
    }

    private String[] getUserHeader() {
        ColumnProperties[] cps = tp.getColumns();
        String[] header = new String[cps.length];
        for (int i = 0; i < header.length; i++) {
            header[i] = cps[i].getDisplayName();
        }
        return header;
    }

    private String[] footerQuery(Query query) {
        ColumnProperties[] cps = tp.getColumns();
        String[] footer = new String[cps.length];
        for (int i = 0; i < cps.length; i++) {
            switch (cps[i].getFooterMode()) {
            case count:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.COUNT);
                break;
            case maximum:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.MAXIMUM);
                break;
            case minimum:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.MINIMUM);
                break;
            case sum:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.SUM);
                break;
            case mean:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.AVERAGE);
                break;
            case none:
                // we'll just do nothing?
              break;
            default:
              Log.e(TAG, "unrecognized footer mode: " +
                  cps[i].getFooterMode().name());
            }
        }
        return footer;
    }

    private String getFooterItem(Query query, ColumnProperties cp,
            Query.GroupQueryType type) {
    	SQLiteDatabase db = null;
    	Cursor c = null;
    	try {
    		db = dbh.getReadableDatabase();
	        SqlData sd = query.toFooterSql(cp.getElementKey(), type);
	        c = db.rawQuery(sd.getSql(), sd.getArgs());
	        if ( c.getCount() == 1 ) {
		        int gColIndex = c.getColumnIndexOrThrow("g");
		        c.moveToFirst();
		        String value = c.getString(gColIndex);
		        return value;
	        } else {
	        	return ""; // TODO: should this return null ???
	        }
    	} finally {
			try {
	    		if ( c != null && !c.isClosed() ) {
    				c.close();
	    		}
			} finally {
		      // TODO: fix the when to close problem
//				if ( db != null ) {
//					db.close();
//				}
			}
    	}
    }

    /**
     * Adds a row to the table with an inserting synchronization state and the
     * transactioning status set to false.
     * <p>
     * If the rowId is null it is not added.
     * <p>
     * I don't think this is called when downloading table data from the
     * server. I think it is only called when creating on the phone...
     */
    public void addRow(Map<String, String> values, String rowId, 
          Long timestamp, String uriUser, String instanceName, String formId, 
          String locale ) {
        Log.d(TAG, values.toString());
        if (timestamp == null) {
        	timestamp = System.currentTimeMillis();
        }
        if (instanceName == null) {
        	instanceName = Long.toString(System.currentTimeMillis());
        }
        ContentValues cv = new ContentValues();
        if (rowId != null) {
          cv.put(DataTableColumns.ROW_ID, rowId);
        }
        for (String column : values.keySet()) {
        	if ( column != null ) {
        		cv.put(column, values.get(column));
        	}
        }
        // The admin columns get added here and also in actualAddRow
        cv.put(DataTableColumns.TIMESTAMP, timestamp);
        cv.put(DataTableColumns.URI_USER, uriUser);
        cv.put(DataTableColumns.SYNC_STATE, SyncUtil.State.INSERTING);
        cv.put(DataTableColumns.TRANSACTIONING, SyncUtil.boolToInt(false));
        cv.put(DataTableColumns.INSTANCE_NAME, instanceName);
        cv.put(DataTableColumns.FORM_ID, formId);
        cv.put(DataTableColumns.LOCALE, locale);
        actualAddRow(cv);
    }

    /**
     * Actually adds a row.
     * <p>
     * I think this gets called when you download a table from the server,
     * whereas I don't think that addRow() does.
     * <p>
     * Checks to ensure that all of the columns in {@link DataTableColumns} 
     * that have non-null constraints are present. If not, it adds their 
     * default value. This is NOT true of {@link DataTableColumns#SYNC_STATE},
     * which varies depending on who is calling this method. It is up to the 
     * caller to set it appropriately. 
     * @param values the values to put in the row
     */
    public void actualAddRow(ContentValues values) {
        if (!values.containsKey(DataTableColumns.ROW_ID)) {
          String id = UUID.randomUUID().toString();
          values.put(DataTableColumns.ROW_ID, id);
        }
        if (!values.containsKey(DataTableColumns.TIMESTAMP)) {
        	values.put(DataTableColumns.TIMESTAMP, System.currentTimeMillis());
        }
        // There is the possibility here that for whatever reason some of the
        // values from the server will be null or non-existent. This will cause
        // problems if there are NON NULL constraints on the tables. Check and
        // add default values as appropriate.
        if (!values.containsKey(DataTableColumns.INSTANCE_NAME) ||
            values.get(DataTableColumns.INSTANCE_NAME) == null) {
          values.put(DataTableColumns.INSTANCE_NAME, 
              DataTableColumns.DEFAULT_INSTANCE_NAME);
        }
        if (!values.containsKey(DataTableColumns.LOCALE) ||
            values.get(DataTableColumns.LOCALE) == null) {
          values.put(DataTableColumns.LOCALE, 
              DataTableColumns.DEFAULT_LOCALE);
        }
        if (!values.containsKey(DataTableColumns.URI_USER) ||
            values.get(DataTableColumns.DEFAULT_URI_USER) == null) {
          values.put(DataTableColumns.URI_USER, 
              DataTableColumns.DEFAULT_URI_USER);
        }
        if (!values.containsKey(DataTableColumns.SYNC_TAG) ||
            values.get(DataTableColumns.SYNC_TAG) == null) {
          values.put(DataTableColumns.SYNC_TAG, 
              DataTableColumns.DEFAULT_SYNC_TAG);
        }
        SQLiteDatabase db = dbh.getWritableDatabase();
        try {
	        values.put(DataTableColumns.SAVED, SavedStatus.COMPLETE.name());
	        long result = db.insertOrThrow(tp.getDbTableName(), null, values);
	        Log.d(TAG, "insert, id=" + result);
        } finally {
          // TODO: fix the when to close problem
//        	db.close();
        }
    }

    /**
     * Updates a row in the table and marks its synchronization state as
     * updating.
     * @param rowId the ID of the row to update
     * @param values the values to update the row with
     * @param uriUser the source phone number to put in the row
     * @param timestamp the last modification time to put in the row
     */
    public void updateRow(String rowId, Map<String, String> values,
            String uriUser, Long timestamp, String instanceName, String formId, String locale) {
        ContentValues cv = new ContentValues();
        // TODO is this a race condition of sorts? isSynchronized(), which
        // formerly returned isSynched, may kind of be doing double duty,
        // saving if it the table is selected TO sync with the server, and also
        // whether the information is up  to date? If so, and you are somehow
        // able to uncheck the box before the server starts syncing, this could
        // cause a problem. This should probably be resolved.
        // UPDATE: I have used the KeyValueStoreSync to return the same value
        // as hilary was originally using. However, this might have to be
        // updated.
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStoreSync syncKVSM =
            kvsm.getSyncStoreForTable(tp.getTableId());
        boolean isSetToSync = syncKVSM.isSetToSync();
        // hilary's original
        //if (tp.isSynchronized() && getSyncState(rowId) == SyncUtil.State.REST)
        if (isSetToSync && getSyncState(rowId) == SyncUtil.State.REST)
          cv.put(DataTableColumns.SYNC_STATE, SyncUtil.State.UPDATING);
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        if ( uriUser != null ) {
        	cv.put(DataTableColumns.URI_USER, uriUser);
        }
        if ( timestamp != null ) {
        	cv.put(DataTableColumns.TIMESTAMP, timestamp);
        }
        if ( instanceName != null ) {
        	cv.put(DataTableColumns.INSTANCE_NAME, instanceName);
        }
        if ( formId != null ) {
        	cv.put(DataTableColumns.FORM_ID, formId);
        }
        if ( locale != null ) {
        	cv.put(DataTableColumns.LOCALE, locale);
        }
        actualUpdateRowByRowId(rowId, cv);
    }

    /**
     * Actually updates a row.
     * @param rowId the ID of the row to update
     * @param values the values to update the row with
     */
    public void actualUpdateRowByRowId(String rowId, ContentValues values) {
        String[] whereArgs = { rowId };
        actualUpdateRow(values, DataTableColumns.ROW_ID + " = ?", whereArgs);
    }

    private void actualUpdateRow(ContentValues values, String where,
            String[] whereArgs) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        if ( !values.containsKey(DataTableColumns.TIMESTAMP) ) {
	        values.put(DataTableColumns.TIMESTAMP, System.currentTimeMillis());
        }
        try {
	        values.put(DataTableColumns.SAVED, DbTable.SavedStatus.COMPLETE.name());
	        db.update(tp.getDbTableName(), values, where, whereArgs);
        } finally {
          // TODO: fix the when to close problem
//        	db.close();
        }
    }

    public void resolveConflict(String rowId, String syncTag,
            Map<String, String> values) {
        String[] deleteWhereArgs = { rowId,
                Integer.valueOf(SyncUtil.State.DELETING).toString() };
        String deleteSql = DataTableColumns.ROW_ID + " = ? AND " + DataTableColumns.SYNC_STATE + " = ?";
        ContentValues updateValues = new ContentValues();
        updateValues.put(DataTableColumns.SYNC_STATE, SyncUtil.State.UPDATING);
        updateValues.put(DataTableColumns.SYNC_TAG, syncTag);
        for (String key : values.keySet()) {
            updateValues.put(key, values.get(key));
        }
        String[] updateWhereArgs = { rowId };
        String updateWhereSql = DataTableColumns.ROW_ID + " = ?";
        SQLiteDatabase db = dbh.getWritableDatabase();
        try {
	        db.delete(tp.getDbTableName(), deleteSql, deleteWhereArgs);
	        updateValues.put(DataTableColumns.TIMESTAMP, System.currentTimeMillis());
	        updateValues.put(DataTableColumns.SAVED, DbTable.SavedStatus.COMPLETE.name());
	        db.update(tp.getDbTableName(), updateValues, updateWhereSql,
	                updateWhereArgs);
        } finally {
          // TODO: fix the when to close problem
//        	db.close();
        }
    }

    /**
     * If table is synchronized and not in an INSERTING state, marks row as
     * deleted. Otherwise, actually deletes the row.
     */
    public void markDeleted(String rowId) {
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStoreSync syncKVSM =
          kvsm.getSyncStoreForTable(tp.getTableId());
      boolean isSetToSync = syncKVSM.isSetToSync();
      // hilary's original
      //if (!tp.isSynchronized()) {
      if (!isSetToSync) {
        deleteRowActual(rowId);
      } else {
        int syncState = getSyncState(rowId);
        if (syncState == SyncUtil.State.INSERTING) {
          deleteRowActual(rowId);
        } else if (syncState == SyncUtil.State.REST || syncState == SyncUtil.State.UPDATING) {
          String[] whereArgs = { rowId };
          ContentValues values = new ContentValues();
          values.put(DataTableColumns.SYNC_STATE, SyncUtil.State.DELETING);
          SQLiteDatabase db = dbh.getWritableDatabase();
          try {
	          values.put(DataTableColumns.TIMESTAMP, System.currentTimeMillis());
	          values.put(DataTableColumns.SAVED, DbTable.SavedStatus.COMPLETE.name());
	          db.update(tp.getDbTableName(), values, DataTableColumns.ROW_ID + " = ?", whereArgs);
          } finally {
            // TODO: fix the when to close problem
//        	  db.close();
          }
        }
      }
    }

        /**
         * Actually deletes a row from the table.
       * @param rowId the ID of the row to delete
     */
    public void deleteRowActual(String rowId) {
        String[] whereArgs = { rowId };
        String whereClause = DataTableColumns.ROW_ID + " = ?";
        deleteRowActual(whereClause, whereArgs);
    }

    public void deleteRowActual(String whereClause, String[] whereArgs) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        try {
        	db.delete(tp.getDbTableName(), whereClause, whereArgs);
        } finally {
          // TODO: fix the when to close problem
//        	db.close();
        }
    }

    /**
     * @param rowId
     * @return the sync state of the row (see {@link SyncUtil.State}), or -1 if
     *         the row does not exist.
     */
    private int getSyncState(String rowId) {
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
	      db = dbh.getReadableDatabase();
	      c = db.query(tp.getDbTableName(), new String[] { DataTableColumns.SYNC_STATE }, DataTableColumns.ROW_ID + " = ?",
	          new String[] { rowId }, null, null, null);
	      int syncState = -1;
	      if (c.moveToFirst()) {
	        int syncStateIndex = c.getColumnIndex(DataTableColumns.SYNC_STATE);
	        syncState = c.getInt(syncStateIndex);
	      }
	      return syncState;
	    } finally {
	    	try {
	    		if ( c != null && !c.isClosed() ) {
	    			c.close();
	    		}
	    	} finally {
	    		if ( db != null ) {
	    	      // TODO: fix the when to close problem
//	    			db.close();
	    		}
	    	}
	    }
    }

    /**
     * Builds a string of SQL for selection with the given column names.
     */
    private String buildSelectionSql(String[] selectionKeys) {
        if ((selectionKeys == null) || (selectionKeys.length == 0)) {
            return null;
        }
        StringBuilder selBuilder = new StringBuilder();
        for (String key : selectionKeys) {
            selBuilder.append(" AND " + key + " = ?");
        }
        selBuilder.delete(0, 5);
        return selBuilder.toString();
    }

    public class GroupTable {

        private String[] keys;
        private double[] values;

        GroupTable(String[] keys, double[] values) {
            this.keys = keys;
            this.values = values;
        }

        public int getSize() {
            return values.length;
        }

        public String getKey(int index) {
            return keys[index];
        }

        public double getValue(int index) {
            return values[index];
        }
    }

    public class ConflictTable {

        private final String[] header;
        private final String[] rowIds;
        private final String[][] syncTags;
        private final String[][][] values;

        private ConflictTable(String[] header, String[] rowIds,
                String[][] syncTags, String[][][] values) {
            this.header = header;
            this.rowIds = rowIds;
            this.syncTags = syncTags;
            this.values = values;
        }

        public int getCount() {
            return rowIds.length;
        }

        public int getWidth() {
            return header.length;
        }

        public String getHeader(int colNum) {
            return header[colNum];
        }

        public String getRowId(int index) {
            return rowIds[index];
        }

        public String getSyncTag(int index, int rowNum) {
            return syncTags[index][rowNum];
        }

        public String getValue(int index, int rowNum, int colNum) {
            return values[index][rowNum][colNum];
        }
    }
}
