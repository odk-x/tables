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

import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.common.android.provider.ConflictType;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.SyncState;
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



   private static final String SQL_FOR_SYNC_STATE_AND_CONFLICT_STATE =
       DataTableColumns.SYNC_STATE + " = ? AND "
       + DataTableColumns.CONFLICT_TYPE + " IN ( ?, ? )";

   /**
    * The sql where clause to select a single row.
    */
   private static final String SQL_WHERE_FOR_SINGLE_ROW = "WHERE " +
       DataTableColumns.ID + " = ?";


    /*
     * These are the columns that are present in any row in the database.
     * Each row should have these in addition to the user-defined columns.
     * If you add a column here you have to be sure to also add it in the
     * create table statement, which can't be programmatically created easily.
     */
    private static final List<String> ADMIN_COLUMNS;

    /**
     * An unmodifiable list of the admin columns. Lazily cached in
     * {@link #getAdminColumns()}.
     */
    private static List<String> mCachedAdminColumns = null;

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
      ADMIN_COLUMNS.add(DataTableColumns.ID);
      ADMIN_COLUMNS.add(DataTableColumns.ROW_ETAG);
      ADMIN_COLUMNS.add(DataTableColumns.SYNC_STATE);
      ADMIN_COLUMNS.add(DataTableColumns.CONFLICT_TYPE);
      ADMIN_COLUMNS.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
      ADMIN_COLUMNS.add(DataTableColumns.SAVEPOINT_CREATOR);
      ADMIN_COLUMNS.add(DataTableColumns.SAVEPOINT_TYPE);
      ADMIN_COLUMNS.add(DataTableColumns.FORM_ID);
      ADMIN_COLUMNS.add(DataTableColumns.LOCALE);
      // put the columns in to the to-sync map.
      COLUMNS_TO_SYNC = new HashMap<String, ColumnType>();
      COLUMNS_TO_SYNC.put(DataTableColumns.SAVEPOINT_TIMESTAMP, ColumnType.STRING);
      COLUMNS_TO_SYNC.put(DataTableColumns.SAVEPOINT_CREATOR, ColumnType.STRING);
      COLUMNS_TO_SYNC.put(DataTableColumns.FORM_ID, ColumnType.STRING);
      COLUMNS_TO_SYNC.put(DataTableColumns.LOCALE, ColumnType.STRING);
    }

    /**
     * Return an unmodifiable list of the admin columns that must be present
     * in every database table.
     * @return
     */
    public static List<String> getAdminColumns() {
      if (mCachedAdminColumns == null) {
        mCachedAdminColumns = Collections.unmodifiableList(ADMIN_COLUMNS);
      }
      return mCachedAdminColumns;
    }

    public enum SavedStatus {
    	COMPLETE,
    	INCOMPLETE
    };

    public static final String DB_CSV_COLUMN_LIST =
        DataTableColumns.ID
        + ", " + DataTableColumns.ROW_ETAG
        + ", " + DataTableColumns.SYNC_STATE
        + ", " + DataTableColumns.CONFLICT_TYPE
        + ", " + DataTableColumns.FORM_ID
        + ", " + DataTableColumns.LOCALE
        + ", " + DataTableColumns.SAVEPOINT_TYPE
        + ", " + DataTableColumns.SAVEPOINT_TIMESTAMP
        + ", " + DataTableColumns.SAVEPOINT_CREATOR
        ;

    private final TableProperties tp;

    public static DbTable getDbTable(TableProperties tp) {
        return new DbTable(tp);
    }

    public static Map<String, ColumnType> getColumnsToSync() {
      return Collections.unmodifiableMap(COLUMNS_TO_SYNC);
    }

    private DbTable(TableProperties tp) {
        this.tp = tp;
    }

    /**
     * PreCondition: the TableProperties.mElementKeyToColumnProperties is non-null
     * or, if null, when fetched from the database, the correct values
     * will be reported.
     *
     * @param db
     * @param tp
     */
    static void createDbTable(SQLiteDatabase db, TableProperties tp) {
        StringBuilder colListBuilder = new StringBuilder();
        for (ColumnProperties cp : tp.getDatabaseColumns().values()) {
            colListBuilder.append(", " + cp.getElementKey());
            if (cp.getColumnType() == ColumnType.NUMBER) {
                colListBuilder.append(" REAL");
            } else if (cp.getColumnType() == ColumnType.INTEGER) {
                colListBuilder.append(" INTEGER");
            } else {
                colListBuilder.append(" TEXT");
            }
        }
        String toExecute = "CREATE TABLE " + tp.getDbTableName() + "(" +
            DataTableColumns.ID + " TEXT NOT NULL" +
     ", " + DataTableColumns.ROW_ETAG + " TEXT NULL" +
     ", " + DataTableColumns.SYNC_STATE + " TEXT NOT NULL" +
     ", " + DataTableColumns.CONFLICT_TYPE + " INTEGER NULL" +
     ", " + DataTableColumns.FORM_ID + " TEXT NULL" +
     ", " + DataTableColumns.LOCALE + " TEXT NULL" +
     ", " + DataTableColumns.SAVEPOINT_TYPE + " TEXT NULL" +
     ", " + DataTableColumns.SAVEPOINT_TIMESTAMP + " TEXT NOT NULL" +
     ", " + DataTableColumns.SAVEPOINT_CREATOR + " TEXT NULL" +
     colListBuilder.toString() +
     ")";
        db.execSQL(toExecute);
    }

    /**
     * See the doc at {@link DbTable#getRaw(List, String[], String[], String)}.
     * All restrictions apply there. The difference with this method is that
     * you can pass in more complicated queries (supporting things like sql
     * IN operators).
     * @param projection
     * @param sqlQuery
     * @param selectionArgs
     * @param orderBy
     * @return
     */
    public UserTable getRawWithSpecificQuery(List<String> projection,
        String sqlQuery, String[] selectionArgs, String orderBy) {
      return getRawHelper(projection, sqlQuery, null, selectionArgs, orderBy);
    }

    /**
     * Helper method for various of the other get raw methods. Handles null
     * arguments appropriately so that calls to the simpler getRaw* methods
     * can contain fewer parameters and be less confusing.
     * <p>
     * Either sqlQuery or selectionKeys can be null.
     * @param projection
     * @param sqlQuery
     * @param selectionKeys
     * @param selectionArgs
     * @param orderBy
     * @return
     */
    private UserTable getRawHelper(List<String> projection, String sqlQuery,
        String[] selectionKeys, String[] selectionArgs, String orderBy) {
      // The columns we will pass to the db to select. Must include the
      // columns parameter as well as all the metadata columns.
      List<String> columnsToSelect;
        if (projection == null) {
          columnsToSelect = tp.getColumnOrder();
          columnsToSelect.addAll(ADMIN_COLUMNS);
        } else {
          // The caller wants just their specified columns, but they'll also
          // have to get the admin columns.
          columnsToSelect = new ArrayList<String>();
          columnsToSelect.addAll(projection);
          columnsToSelect.addAll(ADMIN_COLUMNS);
        }
        String[] colArr = new String[columnsToSelect.size() + 1];
        colArr[0] = DataTableColumns.ID;
        for (int i = 0; i < columnsToSelect.size(); i++) {
            colArr[i + 1] = columnsToSelect.get(i);
        }
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
           db = tp.getReadableDatabase();
           // here's where we actually have to be smart. If the user has
           // provided a sqlQuery, we use that. Otherwise we build the
           // selection up for them.
           if (sqlQuery == null) {
             sqlQuery = buildSelectionSql(selectionKeys);
           } // else we just use the provided one.
           c = db.query(tp.getDbTableName(), colArr,
                   sqlQuery,
                   selectionArgs, null, null, orderBy);
           UserTable table = buildTable(c, tp, projection);
           return table;
       } finally {
         if ( c != null && !c.isClosed() ) {
            c.close();
         }
       }
    }

    /**
     * Gets an {@link UserTable} restricted by the query as necessary. The
     * list of columns should be the element keys to select, and should not
     * include any metadata columns, which will all be returned in the
     * {@link UserTable}.
     * @param the element keys of the user-defined columns to select (if null,
     * all columns will be selected)
     * @param selectionKeys the column names for the WHERE clause (can be null)
     * @param selectionArgs the selection arguments (can be null)
     * @param orderBy the column to order by (can be null)
     * @return a Table of the requested data
     */
    public UserTable getRaw(List<String> columns, String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
      return getRawHelper(columns, null, selectionKeys, selectionArgs,
          orderBy);
    }

    /**
     * Get a {@link UserTable} for this table based on the given where clause.
     * All columns from the table are returned.
     * <p>
     * It performs SELECT * FROM table whereClause.
     * <p>
     * Footers are all empty strings.
     * @param whereClause the whereClause for the selection, beginning with
     * "WHERE". Must include "?" instead of actual values, which are instead
     * passed in the selectionArgs.
     * @param selectionArgs the selection arguments for the where clause.
     * @return
     */
    public UserTable rawSqlQuery(String whereClause, String[] selectionArgs) {
      SQLiteDatabase db = null;
      Cursor c = null;
      try {
        String sqlQuery = "SELECT * FROM " + this.tp.getDbTableName() + " " +
            whereClause;
        db = tp.getReadableDatabase();
        c = db.rawQuery(sqlQuery, selectionArgs);
        UserTable table = buildTable(c, tp, tp.getColumnOrder());
        String[] emptyFooter = getEmptyFooter();
        table.setFooter(emptyFooter);
        return table;
      } finally {
        if ( c != null && !c.isClosed() ) {
          c.close();
        }
      }
    }

    /**
     * Return an {@link UserTable} that will contain a single row.
     * @param rowId
     * @return
     */
    public UserTable getTableForSingleRow(String rowId) {
      String[] sqlSelectionArgs = {rowId};
      return rawSqlQuery(SQL_WHERE_FOR_SINGLE_ROW, sqlSelectionArgs);
    }

    public UserTable getRaw(Query query, String[] columns) {
      List<String> desiredColumns = tp.getColumnOrder();
      desiredColumns.addAll(getAdminColumns());
        UserTable table = dataQuery(query.toSql(desiredColumns));
        table.setFooter(footerQuery(query));
        return table;
    }

    public UserTable getUserTable(Query query) {
      List<String> desiredColumns = tp.getColumnOrder();
      desiredColumns.addAll(getAdminColumns());
        UserTable table = dataQuery(query.toSql(desiredColumns));
        table.setFooter(footerQuery(query));
        return table;
    }

    public UserTable getUserOverviewTable(Query query) {
      // The element keys of the columns we want. We want to select both the
      // user-defined and the admin columns--both the user-defined and
      // ODKTables-specified information, in other words.
      List<String> desiredColumns = tp.getColumnOrder();
      desiredColumns.addAll(getAdminColumns());
        UserTable table = dataQuery(query.toOverviewSql(desiredColumns));
        table.setFooter(footerQuery(query));
        return table;
    }

    public ConflictTable getConflictTable() {
      List<String> userColumns = tp.getColumnOrder();
      // The new protocol for syncing is as follows:
      // local rows and server rows both have SYNC_STATE=CONFLICT.
      // The server version will have their _conflict_type column set to either
      // SERVER_DELETED_OLD_VALUES or SERVER_UPDATED_UPDATED_VALUES. The local
      // version will have its _conflict_type column set to either
      // LOCAL_DELETED_OLD_VALUES or LOCAL_UPDATED_UPDATED_VALUES. See the
      // lengthy discussion of these states and their implications at
      // SyncUtil.ConflictType.
      String[] selectionKeys = new String[2];
      selectionKeys[0] = DataTableColumns.SYNC_STATE;
      selectionKeys[1] = DataTableColumns.CONFLICT_TYPE;
      String syncStateConflictStr = SyncState.conflicting.name();
      String conflictTypeLocalDeletedStr =
          Integer.toString(ConflictType.LOCAL_DELETED_OLD_VALUES);
      String conflictTypeLocalUpdatedStr =
          Integer.toString(ConflictType.LOCAL_UPDATED_UPDATED_VALUES);
      String conflictTypeServerDeletedStr =
          Integer.toString(ConflictType.SERVER_DELETED_OLD_VALUES);
      String conflictTypeServerUpdatedStr = Integer.toString(
          ConflictType.SERVER_UPDATED_UPDATED_VALUES);
      UserTable localTable = getRawWithSpecificQuery(userColumns,
          SQL_FOR_SYNC_STATE_AND_CONFLICT_STATE,
          new String[] {syncStateConflictStr, conflictTypeLocalDeletedStr,
            conflictTypeLocalUpdatedStr},
          DataTableColumns.ID);
      UserTable serverTable = getRawWithSpecificQuery(userColumns,
          SQL_FOR_SYNC_STATE_AND_CONFLICT_STATE,
          new String[] {syncStateConflictStr, conflictTypeServerDeletedStr,
            conflictTypeServerUpdatedStr},
          DataTableColumns.ID);
      return new ConflictTable(localTable, serverTable);
    }

    private UserTable dataQuery(SqlData sd) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
        	db = tp.getReadableDatabase();
        	String sqlStr = sd.getSql();
        	String[] selArgs = sd.getArgs();
        	c = db.rawQuery(sd.getSql(), sd.getArgs());
        	UserTable table = buildTable(c, tp, tp.getColumnOrder());
         return table;
        } catch (Exception e) {
          Log.e(TAG, "error in dataQuery");
          e.printStackTrace();
          return null;
        } finally {
    		if ( c != null && !c.isClosed() ) {
    			c.close();
    		}
        }
    }

    /**
     * Builds a UserTable with the data from the given cursor.
     * The cursor, but not the columns array, must include the row ID column.
     * <p>
     * The cursor must have queried for both the user-defined columns and the
     * metadata columns.
     * @param c Cursor meeting the requirements above
     * @param userColumnOrder the user-specified column order
     */
    private UserTable buildTable(Cursor c, TableProperties tp,
        List<String> userColumnOrder) {
      return new UserTable(c, tp, userColumnOrder);
    }

    /**
     * Returns an empty footer. Useful for things like {@link rawSqlQuery},
     * which do not pass in the {@link Query} parameter that a footer requires.
     * @return
     */
    private String[] getEmptyFooter() {
      int numDisplayColumns = tp.getNumberOfDisplayColumns();
      String[] footer = new String[numDisplayColumns];
      for (int i = 0; i < footer.length; i++) {
        footer[i] = "";
      }
      return footer;
    }

    private String[] footerQuery(Query query) {
    	int numberOfDisplayColumns = tp.getNumberOfDisplayColumns();
        String[] footer = new String[numberOfDisplayColumns];
        for (int i = 0; i < numberOfDisplayColumns; i++) {
          ColumnProperties cp = tp.getColumnByIndex(i);
            switch (cp.getFooterMode()) {
            case count:
                footer[i] = getFooterItem(query, cp,
                        Query.GroupQueryType.COUNT);
                break;
            case maximum:
                footer[i] = getFooterItem(query, cp,
                        Query.GroupQueryType.MAXIMUM);
                break;
            case minimum:
                footer[i] = getFooterItem(query, cp,
                        Query.GroupQueryType.MINIMUM);
                break;
            case sum:
                footer[i] = getFooterItem(query, cp,
                        Query.GroupQueryType.SUM);
                break;
            case mean:
                footer[i] = getFooterItem(query, cp,
                        Query.GroupQueryType.AVERAGE);
                break;
            case none:
                // we'll just do nothing?
              break;
            default:
              Log.e(TAG, "unrecognized footer mode: " +
                  cp.getFooterMode().name());
            }
        }
        return footer;
    }

    private String getFooterItem(Query query, ColumnProperties cp,
            Query.GroupQueryType type) {
    	SQLiteDatabase db = null;
    	Cursor c = null;
    	try {
    		db = tp.getReadableDatabase();
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
    		if ( c != null && !c.isClosed() ) {
				c.close();
    		}
    	}
    }

    /**
     * Adds a row to the table with an inserting synchronization state.
     * <p>
     * If the rowId is null it is not added.
     * <p>
     * I don't think this is called when downloading table data from the
     * server. I think it is only called when creating on the phone...
     */
    public void addRow(String rowId, String formId, String locale,
          Long timestamp, String savepointCreator, Map<String, String> values ) {

        if (timestamp == null) {
        	timestamp = System.currentTimeMillis();
        }
        ContentValues cv = new ContentValues();
        if (rowId != null) {
          cv.put(DataTableColumns.ID, rowId);
        }
        for (String column : values.keySet()) {
        	if ( column != null ) {
        		cv.put(column, values.get(column));
        	}
        }
        // The admin columns get added here and also in actualAddRow
        cv.put(DataTableColumns.SYNC_STATE, SyncState.inserting.name());
        cv.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(timestamp));
        cv.put(DataTableColumns.SAVEPOINT_CREATOR, savepointCreator);
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
        if (!values.containsKey(DataTableColumns.ID)) {
          String id = UUID.randomUUID().toString();
          values.put(DataTableColumns.ID, id);
        }
        if (!values.containsKey(DataTableColumns.SAVEPOINT_TIMESTAMP)) {
        	values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()));
        }
        // There is the possibility here that for whatever reason some of the
        // values from the server will be null or non-existent. This will cause
        // problems if there are NON NULL constraints on the tables. Check and
        // add default values as appropriate.
        if (!values.containsKey(DataTableColumns.LOCALE) ||
            values.get(DataTableColumns.LOCALE) == null) {
          values.put(DataTableColumns.LOCALE,
              DataTableColumns.DEFAULT_LOCALE);
        }
        if (!values.containsKey(DataTableColumns.SAVEPOINT_CREATOR) ||
            values.get(DataTableColumns.SAVEPOINT_CREATOR) == null) {
          values.put(DataTableColumns.SAVEPOINT_CREATOR,
              DataTableColumns.DEFAULT_SAVEPOINT_CREATOR);
        }
        if (!values.containsKey(DataTableColumns.ROW_ETAG) ||
            values.get(DataTableColumns.ROW_ETAG) == null) {
          values.put(DataTableColumns.ROW_ETAG,
              DataTableColumns.DEFAULT_ROW_ETAG);
        }
        SQLiteDatabase db = tp.getWritableDatabase();
        try {
	        values.put(DataTableColumns.SAVEPOINT_TYPE, SavedStatus.COMPLETE.name());
	        long result = db.insertOrThrow(tp.getDbTableName(), null, values);
        } finally {
          // TODO: fix the when to close problem
//        	db.close();
        }
    }

    /**
     * Called when the schema on the server has changed w.r.t. the schema on
     * the device. In this case, we do not know whether the rows on the device
     * match those on the server. Reset all rows to 'insert' to ensure they
     * are sync'd to the server.
     */
    public void changeRestRowsToInserting() {
      SQLiteDatabase db = tp.getWritableDatabase();

      StringBuilder b = new StringBuilder();
      b.append("UPDATE ").append(tp.getDbTableName()).append(" SET ").append(DataTableColumns.SYNC_STATE)
      .append(" =? WHERE ").append(DataTableColumns.SYNC_STATE).append(" =?");
      String sql = b.toString();
      String args[] = {
          SyncState.inserting.name(),
          SyncState.rest.name()
      };
      db.execSQL(sql, args);
    }

    /**
     * Updates a row in the table and marks its synchronization state as
     * updating.
     * @param rowId the ID of the row to update
     * @param values the values to update the row with
     * @param savepointCreator the user saving this change of this row
     * @param timestamp the last modification time to put in the row
     */
    public void updateRow(String rowId,
            String formId, String locale, Long timestamp, String savepointCreator, Map<String, String> values) {
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
        boolean isSetToSync = tp.isSetToSync();
        // hilary's original
        //if (tp.isSynchronized() && getSyncState(rowId) == SyncUtil.State.REST)
        if (isSetToSync && getSyncState(rowId) == SyncState.rest)
          cv.put(DataTableColumns.SYNC_STATE, SyncState.updating.name());
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        if ( savepointCreator != null ) {
        	cv.put(DataTableColumns.SAVEPOINT_CREATOR, savepointCreator);
        }
        if ( timestamp != null ) {
        	cv.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(timestamp));
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
        actualUpdateRow(values, DataTableColumns.ID + " = ?", whereArgs);
    }

    private void actualUpdateRow(ContentValues values, String where,
            String[] whereArgs) {
        SQLiteDatabase db = tp.getWritableDatabase();
        if ( !values.containsKey(DataTableColumns.SAVEPOINT_TIMESTAMP) ) {
	        values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()));
        }
        try {
	        values.put(DataTableColumns.SAVEPOINT_TYPE, DbTable.SavedStatus.COMPLETE.name());
	        db.update(tp.getDbTableName(), values, where, whereArgs);
        } finally {
          // TODO: fix the when to close problem
//        	db.close();
        }
    }

    public void resolveConflict(String rowId, String serverRowETag,
            Map<String, String> values) {
        // We're going to delete the column with the matching row id that has
        // conflict_type SERVER_UPDATED or SERVER_DELETED.
      String[] deleteWhereArgs = { rowId };
        String deleteSql = DataTableColumns.ID + " = ? AND " +
            DataTableColumns.CONFLICT_TYPE + " IN ( " +
            ConflictType.SERVER_DELETED_OLD_VALUES + ", " +
            ConflictType.SERVER_UPDATED_UPDATED_VALUES + ")";
        ContentValues updateValues = new ContentValues();
        updateValues.put(DataTableColumns.SYNC_STATE, SyncState.updating.name());
        updateValues.put(DataTableColumns.ROW_ETAG, serverRowETag);
        updateValues.putNull(DataTableColumns.CONFLICT_TYPE);
        for (String key : values.keySet()) {
            updateValues.put(key, values.get(key));
        }
        String[] updateWhereArgs = { rowId };
        String updateWhereSql = DataTableColumns.ID + " = ?";
        SQLiteDatabase db = tp.getWritableDatabase();
        try {
	        db.delete(tp.getDbTableName(), deleteSql, deleteWhereArgs);
	        updateValues.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()));
	        updateValues.put(DataTableColumns.SAVEPOINT_TYPE, DbTable.SavedStatus.COMPLETE.name());
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
      boolean isSetToSync = tp.isSetToSync();
      // hilary's original
      //if (!tp.isSynchronized()) {
      if (!isSetToSync) {
        deleteRowActual(rowId);
      } else {
        SyncState syncState = getSyncState(rowId);
        if (syncState == SyncState.inserting) {
          deleteRowActual(rowId);
        } else if (syncState == SyncState.rest || syncState == SyncState.updating) {
          String[] whereArgs = { rowId };
          ContentValues values = new ContentValues();
          values.put(DataTableColumns.SYNC_STATE, SyncState.deleting.name());
          SQLiteDatabase db = tp.getWritableDatabase();
          try {
	          values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()));
	          values.put(DataTableColumns.SAVEPOINT_TYPE, DbTable.SavedStatus.COMPLETE.name());
	          db.update(tp.getDbTableName(), values, DataTableColumns.ID + " = ?", whereArgs);
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
        String whereClause = DataTableColumns.ID + " = ?";
        deleteRowActual(whereClause, whereArgs);
    }

    public void deleteRowActual(String whereClause, String[] whereArgs) {
        SQLiteDatabase db = tp.getWritableDatabase();
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
    private SyncState getSyncState(String rowId) {
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
	      db = tp.getReadableDatabase();
	      c = db.query(tp.getDbTableName(), new String[] { DataTableColumns.SYNC_STATE }, DataTableColumns.ID + " = ?",
	          new String[] { rowId }, null, null, null);
	      if (c.moveToFirst()) {
	        int syncStateIndex = c.getColumnIndex(DataTableColumns.SYNC_STATE);
	        if ( !c.isNull(syncStateIndex) ) {
	          String val = c.getString(syncStateIndex);
	          return SyncState.valueOf(val);
	        }
	      }
	      return null;
	    } finally {
    		if ( c != null && !c.isClosed() ) {
    			c.close();
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

//    public class ConflictTable {
//
//        private final String[] header;
//        private final String[] rowIds;
//        private final String[][] syncTags;
//        private final String[][][] values;
//
//        private ConflictTable(String[] header, String[] rowIds,
//                String[][] syncTags, String[][][] values) {
//            this.header = header;
//            this.rowIds = rowIds;
//            this.syncTags = syncTags;
//            this.values = values;
//        }
//
//        public int getCount() {
//            return rowIds.length;
//        }
//
//        public int getWidth() {
//            return header.length;
//        }
//
//        public String getHeader(int colNum) {
//            return header[colNum];
//        }
//
//        public String getRowId(int index) {
//            return rowIds[index];
//        }
//
//        public String getSyncTag(int index, int rowNum) {
//            return syncTags[index][rowNum];
//        }
//
//        public String getValue(int index, int rowNum, int colNum) {
//            return values[index][rowNum][colNum];
//        }
//    }
}
