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
package org.opendatakit.common.android.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.opendatakit.aggregate.odktables.rest.ConflictType;
import org.opendatakit.aggregate.odktables.rest.SavepointTypeManipulator;
import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

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
   private static final String SQL_WHERE_FOR_SINGLE_ROW =
       DataTableColumns.ID + " = ?";


    public static final String DB_CSV_COLUMN_LIST =
        DataTableColumns.ID
        + ", " + DataTableColumns.ROW_ETAG
        + ", " + DataTableColumns.SYNC_STATE
        + ", " + DataTableColumns.CONFLICT_TYPE
        + ", " + DataTableColumns.FILTER_TYPE
        + ", " + DataTableColumns.FILTER_VALUE
        + ", " + DataTableColumns.FORM_ID
        + ", " + DataTableColumns.LOCALE
        + ", " + DataTableColumns.SAVEPOINT_TYPE
        + ", " + DataTableColumns.SAVEPOINT_TIMESTAMP
        + ", " + DataTableColumns.SAVEPOINT_CREATOR
        ;

    private final TableProperties tp;

    public DbTable(TableProperties tp) {
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
        for (String elementKey : tp.getPersistedColumns()) {
            ColumnProperties cp = tp.getColumnByElementKey(elementKey);
            colListBuilder.append(", " + elementKey);
            ElementDataType type = cp.getColumnType().getDataType();
            if (type == ElementDataType.number) {
                colListBuilder.append(" REAL");
            } else if (type == ElementDataType.integer) {
                colListBuilder.append(" INTEGER");
            } else {
                colListBuilder.append(" TEXT");
            }
        }
        String toExecute = "CREATE TABLE " + tp.getTableId() + "("
            + DataTableColumns.ID + " TEXT NOT NULL, "
            + DataTableColumns.ROW_ETAG + " TEXT NULL, "
            + DataTableColumns.SYNC_STATE + " TEXT NOT NULL, "
            + DataTableColumns.CONFLICT_TYPE + " INTEGER NULL,"
            + DataTableColumns.FILTER_TYPE + " TEXT NULL,"
            + DataTableColumns.FILTER_VALUE + " TEXT NULL,"
            + DataTableColumns.FORM_ID + " TEXT NULL,"
            + DataTableColumns.LOCALE + " TEXT NULL,"
            + DataTableColumns.SAVEPOINT_TYPE + " TEXT NULL,"
            + DataTableColumns.SAVEPOINT_TIMESTAMP + " TEXT NOT NULL,"
            + DataTableColumns.SAVEPOINT_CREATOR + " TEXT NULL"
            + colListBuilder.toString() + ")";
        db.execSQL(toExecute);
    }

    /**
     * Helper method for various of the other get raw methods. Handles null
     * arguments appropriately so that calls to the simpler getRaw* methods
     * can contain fewer parameters and be less confusing.
     * <p>
     * Either sqlQuery or selectionKeys can be null.
     * @param sqlQuery
     * @param selectionKeys
     * @param selectionArgs
     * @param orderBy
     * @return
     */
    public UserTable getRawHelper(String sqlQuery,
        String[] selectionKeys, String[] selectionArgs,
        String[] groupByArgs, String havingClause, String orderByElementKey, String orderByDirection) {

        // build the group-by
        String groupByClause = null;
        if ( groupByArgs != null && groupByArgs.length != 0 ) {
          StringBuilder b = new StringBuilder();
          boolean first = true;
          for ( String g : groupByArgs) {
            if (!first) {
              b.append(", ");
            }
            first = false;
            b.append(g);
          }
          groupByClause = b.toString();
        }
        String orderByClause = null;
        if ( orderByElementKey != null && orderByElementKey.length() != 0 ) {
          if ( orderByDirection != null && orderByDirection.length() != 0 ) {
            orderByClause = orderByElementKey + " " + orderByDirection;
          } else {
            orderByClause = orderByElementKey + " ASC";
          }
        }
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
           db = tp.getReadableDatabase();
           c = db.query(tp.getTableId(), null,
                   sqlQuery,
                   selectionArgs, groupByClause, havingClause, orderByClause);
           UserTable table = buildTable(c, tp.getAppName(), tp.getTableId(), tp.getPersistedColumns(),
               sqlQuery, selectionArgs,
               groupByArgs, havingClause, orderByElementKey, orderByDirection);
           return table;
        } finally {
          if ( c != null && !c.isClosed() ) {
             c.close();
          }
          db.close();
        }
    }

    /**
     * Get a {@link UserTable} for this table based on the given where clause.
     * All columns from the table are returned.
     * <p>
     * It performs SELECT * FROM table whereClause.
     * <p>
     * @param whereClause the whereClause for the selection, beginning with
     * "WHERE". Must include "?" instead of actual values, which are instead
     * passed in the selectionArgs.
     * @param selectionArgs the selection arguments for the where clause.
     * @return
     */
    public UserTable rawSqlQuery(String whereClause, String[] selectionArgs,
        String[] groupBy, String having, String orderByElementKey, String orderByDirection) {
      SQLiteDatabase db = null;
      Cursor c = null;
      try {
        StringBuilder s = new StringBuilder();
        s.append("SELECT * FROM ").append(this.tp.getTableId());
        if ( whereClause != null && whereClause.length() != 0 ) {
          s.append(" WHERE ").append(whereClause);
        }
        if ( groupBy != null && groupBy.length != 0 ) {
          s.append(" GROUP BY ");
          boolean first = true;
          for ( String elementKey : groupBy ) {
            if (!first) {
              s.append(", ");
            }
            first = false;
            s.append(elementKey);
          }
          if ( having != null && having.length() != 0 ) {
            s.append(" HAVING ").append(having);
          }
        }
        if ( orderByElementKey != null && orderByElementKey.length() != 0 ) {
          s.append(" ORDER BY ").append(orderByElementKey);
          if ( orderByDirection != null && orderByDirection.length() != 0 ) {
            s.append(" ").append(orderByDirection);
          } else {
            s.append(" ASC");
          }
        }
        String sqlQuery = s.toString();
        db = tp.getReadableDatabase();
        c = db.rawQuery(sqlQuery, selectionArgs);
        UserTable table = buildTable(c, tp.getAppName(), tp.getTableId(), tp.getPersistedColumns(),
            whereClause, selectionArgs, groupBy, having, orderByElementKey, orderByDirection);
        return table;
      } finally {
        if ( c != null && !c.isClosed() ) {
          c.close();
        }
        if ( db != null ) {
          db.close();
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
      return rawSqlQuery(SQL_WHERE_FOR_SINGLE_ROW, sqlSelectionArgs, null, null, null, null);
    }

    public SyncState getRowSyncState(String rowId) {
      String[] sqlSelectionArgs = {rowId};
      SQLiteDatabase db = null;
      Cursor c = null;
      try {
        StringBuilder s = new StringBuilder();
        s.append("SELECT ").append(DataTableColumns.SYNC_STATE)
         .append(" FROM ").append(this.tp.getTableId())
         .append(" WHERE ").append(SQL_WHERE_FOR_SINGLE_ROW);
        String sqlQuery = s.toString();
        db = tp.getReadableDatabase();
        c = db.rawQuery(sqlQuery, sqlSelectionArgs);
        if ( c.getCount() == 0 ) {
          return null;
        }
        if ( c.getCount() > 1 ) {
          // UGH -- bad state!
          Log.e(TAG, "Multiple records for this row!");
          return null;
        }
        c.moveToFirst();
        int idxSyncState = c.getColumnIndexOrThrow(DataTableColumns.SYNC_STATE);
        String value = ODKDatabaseUtils.getIndexAsString(c, idxSyncState);
        if ( value == null ) {
          Log.e(TAG, "Unexpected null value for sync state");
          return null;
        }
        return SyncState.valueOf(value);
      } finally {
        if ( c != null && !c.isClosed() ) {
          c.close();
        }
        if ( db != null ) {
          db.close();
        }
      }

    }

    public ConflictTable getConflictTable() {
      // The new protocol for syncing is as follows:
      // local rows and server rows both have SYNC_STATE=CONFLICT.
      // The server version will have their _conflict_type column set to either
      // SERVER_DELETED_OLD_VALUES or SERVER_UPDATED_UPDATED_VALUES. The local
      // version will have its _conflict_type column set to either
      // LOCAL_DELETED_OLD_VALUES or LOCAL_UPDATED_UPDATED_VALUES. See the
      // lengthy discussion of these states and their implications at
      // ConflictType.
      String[] selectionKeys = new String[2];
      selectionKeys[0] = DataTableColumns.SYNC_STATE;
      selectionKeys[1] = DataTableColumns.CONFLICT_TYPE;
      String syncStateConflictStr = SyncState.in_conflict.name();
      String conflictTypeLocalDeletedStr =
          Integer.toString(ConflictType.LOCAL_DELETED_OLD_VALUES);
      String conflictTypeLocalUpdatedStr =
          Integer.toString(ConflictType.LOCAL_UPDATED_UPDATED_VALUES);
      String conflictTypeServerDeletedStr =
          Integer.toString(ConflictType.SERVER_DELETED_OLD_VALUES);
      String conflictTypeServerUpdatedStr = Integer.toString(
          ConflictType.SERVER_UPDATED_UPDATED_VALUES);
      UserTable localTable = getRawHelper(
          SQL_FOR_SYNC_STATE_AND_CONFLICT_STATE, null,
          new String[] {syncStateConflictStr, conflictTypeLocalDeletedStr,
            conflictTypeLocalUpdatedStr}, null, null,
          DataTableColumns.ID, "ASC");
      UserTable serverTable = getRawHelper(
          SQL_FOR_SYNC_STATE_AND_CONFLICT_STATE, null,
          new String[] {syncStateConflictStr, conflictTypeServerDeletedStr,
            conflictTypeServerUpdatedStr}, null, null,
          DataTableColumns.ID, "ASC");
      return new ConflictTable(localTable, serverTable);
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
    private UserTable buildTable(Cursor c, String appName, String tableId, List<String> persistedColumns,
        String whereClause, String[] selectionArgs, String[] groupByArgs, String havingClause,
        String orderByElementKey, String orderByDirection) {
      return new UserTable(c, appName, tableId, 
          persistedColumns, whereClause, selectionArgs,
          groupByArgs, havingClause, orderByElementKey, orderByDirection);
    }

    /**
     * Adds a row to the table with a new_row synchronization state.
     * <p>
     * If the rowId is null it is not added.
     * <p>
     * I don't think this is called when downloading table data from the
     * server. I think it is only called when creating on the phone...
     */
    public void addRow(String rowId, String formId, String locale,
        String savepointType, String savepointTimestamp, String savepointCreator,
        String rowETag, String filterType, String filterValue, Map<String, String> values ) {


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
        cv.put(DataTableColumns.SYNC_STATE, SyncState.new_row.name());
        cv.put(DataTableColumns.FORM_ID, formId);
        cv.put(DataTableColumns.LOCALE, locale);
        cv.put(DataTableColumns.SAVEPOINT_TYPE, savepointType);
        cv.put(DataTableColumns.SAVEPOINT_TIMESTAMP, savepointTimestamp);
        cv.put(DataTableColumns.SAVEPOINT_CREATOR, savepointCreator);
        cv.put(DataTableColumns.ROW_ETAG, rowETag);
        cv.put(DataTableColumns.FILTER_TYPE, filterType);
        cv.put(DataTableColumns.FILTER_VALUE, filterValue);
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
        if (!values.containsKey(DataTableColumns.SAVEPOINT_TIMESTAMP) ||
            values.get(DataTableColumns.SAVEPOINT_TIMESTAMP) == null) {
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

        cleanUpValuesMap(values);

        SQLiteDatabase db = tp.getWritableDatabase();
        try {
          db.beginTransaction();
          // TODO: This is WRONG
	       values.put(DataTableColumns.SAVEPOINT_TYPE, SavepointTypeManipulator.complete());
	       long result = db.insertOrThrow(tp.getTableId(), null, values);
	       if ( result != -1 ) {
	         db.setTransactionSuccessful();
	       }
        } finally {
          db.endTransaction();
          db.close();
        }
    }

    /**
     * Called when the schema on the server has changed w.r.t. the schema on
     * the device. In this case, we do not know whether the rows on the device
     * match those on the server.
     *
     * Reset all 'in_conflict' rows to their original local state (changed or deleted).
     * Leave all 'deleted' rows in 'deleted' state.
     * Leave all 'changed' rows in 'changed' state.
     * Reset all 'synced' rows to 'new_row' to ensure they are sync'd to the server.
     * Reset all 'synced_pending_files' rows to 'new_row' to ensure they are sync'd to the server.
     */
    public void changeDataRowsToNewRowState() {

      StringBuilder b = new StringBuilder();

      // remove server conflicting rows
      b.setLength(0);
      b.append("DELETE FROM ").append(tp.getTableId()).append(" WHERE ").append(DataTableColumns.SYNC_STATE)
      .append(" =? AND ").append(DataTableColumns.CONFLICT_TYPE).append(" IN (?, ?)");

      String sqlConflictingServer = b.toString();
      String argsConflictingServer[] = {
          SyncState.in_conflict.name(),
          Integer.toString(ConflictType.SERVER_DELETED_OLD_VALUES),
          Integer.toString(ConflictType.SERVER_UPDATED_UPDATED_VALUES)
      };

      // update local delete conflicts to deletes
      b.setLength(0);
      b.append("UPDATE ").append(tp.getTableId()).append(" SET ").append(DataTableColumns.SYNC_STATE)
      .append(" =?, ").append(DataTableColumns.CONFLICT_TYPE).append(" = null WHERE ")
      .append(DataTableColumns.CONFLICT_TYPE).append(" = ?");

      String sqlConflictingLocalDeleting = b.toString();
      String argsConflictingLocalDeleting[] = {
          SyncState.deleted.name(),
          Integer.toString(ConflictType.LOCAL_DELETED_OLD_VALUES)
      };

      // update local update conflicts to updates
      String sqlConflictingLocalUpdating = sqlConflictingLocalDeleting;
      String argsConflictingLocalUpdating[] = {
          SyncState.changed.name(),
          Integer.toString(ConflictType.LOCAL_UPDATED_UPDATED_VALUES)
      };

      // reset all 'rest' rows to 'insert'
      b.setLength(0);
      b.append("UPDATE ").append(tp.getTableId()).append(" SET ").append(DataTableColumns.SYNC_STATE)
      .append(" =? WHERE ").append(DataTableColumns.SYNC_STATE).append(" =?");

      String sqlRest = b.toString();
      String argsRest[] = {
          SyncState.new_row.name(),
          SyncState.synced.name()
      };

      String sqlRestPendingFiles = sqlRest;
      String argsRestPendingFiles[] = {
          SyncState.new_row.name(),
          SyncState.synced_pending_files.name()
      };

      SQLiteDatabase db = tp.getWritableDatabase();
      try {
        db.beginTransaction();
        db.execSQL(sqlConflictingServer, argsConflictingServer);
        db.execSQL(sqlConflictingLocalDeleting, argsConflictingLocalDeleting);
        db.execSQL(sqlConflictingLocalUpdating, argsConflictingLocalUpdating);
        db.execSQL(sqlRest, argsRest);
        db.execSQL(sqlRestPendingFiles, argsRestPendingFiles);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
        db.close();
      }
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
        if (getSyncState(rowId) == SyncState.synced) {
          cv.put(DataTableColumns.SYNC_STATE, SyncState.changed.name());
        }
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

    private void cleanUpValuesMap(ContentValues values) {

      Map<String, String> toBeResolved = new HashMap<String,String>();

      for ( String key : values.keySet() ) {
        if ( DataTableColumns.CONFLICT_TYPE.equals(key) ) {
          continue;
        } else if ( DataTableColumns.FILTER_TYPE.equals(key) ) {
          continue;
        } else if ( DataTableColumns.FILTER_TYPE.equals(key) ) {
          continue;
        } else if ( DataTableColumns.FILTER_VALUE.equals(key) ) {
          continue;
        } else if ( DataTableColumns.FORM_ID.equals(key) ) {
          continue;
        } else if ( DataTableColumns.ID.equals(key) ) {
          continue;
        } else if ( DataTableColumns.LOCALE.equals(key) ) {
          continue;
        } else if ( DataTableColumns.ROW_ETAG.equals(key) ) {
          continue;
        } else if ( DataTableColumns.SAVEPOINT_CREATOR.equals(key) ) {
          continue;
        } else if ( DataTableColumns.SAVEPOINT_TIMESTAMP.equals(key) ) {
          continue;
        } else if ( DataTableColumns.SAVEPOINT_TYPE.equals(key) ) {
          continue;
        } else if ( DataTableColumns.SYNC_STATE.equals(key) ) {
          continue;
        } else if ( DataTableColumns._ID.equals(key) ) {
          continue;
        }
        // OK it is one of the data columns
        ColumnProperties cp = tp.getColumnByElementKey(key);
        if ( !cp.isUnitOfRetention() ) {
          toBeResolved.put(key, values.getAsString(key));
        }
      }

      // remove these non-retained values from the values set...
      for ( String key : toBeResolved.keySet() ) {
        values.remove(key);
      }

      for ( ; !toBeResolved.isEmpty(); ) {

        Map<String, String> moreToResolve = new HashMap<String, String>();

        for ( Map.Entry<String,String> entry : toBeResolved.entrySet() ) {
          String key = entry.getKey();
          String json = entry.getValue();
          if ( json == null ) {
            // don't need to do anything
            // since the value is null
            continue;
          }
          ColumnProperties cp = tp.getColumnByElementKey(key);
          try {
            Map<String,Object> struct = ODKFileUtils.mapper.readValue(json, Map.class);
            for ( ColumnDefinition child : cp.getChildren() ) {
              String subkey = child.getElementKey();
              ColumnProperties subcp = tp.getColumnByElementKey(subkey);
              if ( subcp.isUnitOfRetention() ) {
                ElementDataType type = subcp.getColumnType().getDataType();
                if ( type == ElementDataType.integer ) {
                  values.put(subkey, (Integer) struct.get(subcp.getElementName()));
                } else if ( type == ElementDataType.number ) {
                  values.put(subkey, (Double) struct.get(subcp.getElementName()));
                } else if ( type == ElementDataType.bool ) {
                  values.put(subkey, ((Boolean) struct.get(subcp.getElementName())) ? 1 : 0);
                } else {
                  values.put(subkey, (String) struct.get(subcp.getElementName()));
                }
              } else {
                // this must be a javascript structure... re-JSON it and save (for next round).
                moreToResolve.put(subkey, ODKFileUtils.mapper.writeValueAsString(struct.get(subcp.getElementName())));
              }
            }
          } catch (JsonParseException e) {
            e.printStackTrace();
            throw new IllegalStateException("should not be happening");
          } catch (JsonMappingException e) {
            e.printStackTrace();
            throw new IllegalStateException("should not be happening");
          } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("should not be happening");
          }
        }

        toBeResolved = moreToResolve;
      }
    }

    private void actualUpdateRow(ContentValues values, String where,
            String[] whereArgs) {
        if ( !values.containsKey(DataTableColumns.SAVEPOINT_TIMESTAMP) ) {
	        values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()));
        }

        cleanUpValuesMap(values);

        SQLiteDatabase db = tp.getWritableDatabase();
        try {
          db.beginTransaction();
	       values.put(DataTableColumns.SAVEPOINT_TYPE, SavepointTypeManipulator.complete());
	       db.update(tp.getTableId(), values, where, whereArgs);
	       db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
          db.close();
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
        updateValues.put(DataTableColumns.SYNC_STATE, SyncState.changed.name());
        updateValues.put(DataTableColumns.ROW_ETAG, serverRowETag);
        updateValues.putNull(DataTableColumns.CONFLICT_TYPE);
        for (String key : values.keySet()) {
            updateValues.put(key, values.get(key));
        }
        String[] updateWhereArgs = { rowId };
        String updateWhereSql = DataTableColumns.ID + " = ?";
        SQLiteDatabase db = tp.getWritableDatabase();
        try {
          db.beginTransaction();
	        db.delete(tp.getTableId(), deleteSql, deleteWhereArgs);
	        updateValues.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()));
	        updateValues.put(DataTableColumns.SAVEPOINT_TYPE, SavepointTypeManipulator.complete());
	        db.update(tp.getTableId(), updateValues, updateWhereSql,
	                updateWhereArgs);
	        db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
          db.close();
        }
    }

    /**
     * If row is not in an new_row state, marks row as
     * deleted. Otherwise, actually deletes the row.
     */
    public void markDeleted(String rowId) {
      SyncState syncState = getSyncState(rowId);
      if (syncState == SyncState.new_row) {
        deleteRowActual(rowId);
      } else if (syncState == SyncState.synced || syncState == SyncState.changed) {
        String[] whereArgs = { rowId };
        ContentValues values = new ContentValues();
        values.put(DataTableColumns.SYNC_STATE, SyncState.deleted.name());
        SQLiteDatabase db = tp.getWritableDatabase();
        try {
          db.beginTransaction();
          values.put(DataTableColumns.SAVEPOINT_TIMESTAMP, TableConstants.nanoSecondsFromMillis(System.currentTimeMillis()));
          values.put(DataTableColumns.SAVEPOINT_TYPE, SavepointTypeManipulator.complete());
	       db.update(tp.getTableId(), values, DataTableColumns.ID + " = ?", whereArgs);
	       db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
          db.close();
        }
      }
    }

    /**
     * Actually deletes a row from the table.
     * This will delete any server conflict rows too,
     * so we don't have to worry about cleaning those
     * up separately.
     *
     * @param rowId the ID of the row to delete
     */
    public void deleteRowActual(String rowId) {
        String[] whereArgs = { rowId };
        String whereClause = DataTableColumns.ID + " = ?";
        deleteRowActual(whereClause, whereArgs);
        File instanceFolder = new File(ODKFileUtils.getInstanceFolder(tp.getAppName(), tp.getTableId(), rowId));
        try {
          FileUtils.deleteDirectory(instanceFolder);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          Log.e(TAG, "Unable to delete this directory: " + instanceFolder.getAbsolutePath());
        }
    }

    public void deleteRowActual(String whereClause, String[] whereArgs) {
      SQLiteDatabase db = tp.getWritableDatabase();
      try {
         db.beginTransaction();
      	db.delete(tp.getTableId(), whereClause, whereArgs);
      	db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
        db.close();
      }
    }

    /**
     * @param rowId
     * @return the sync state of the row (see {@link SyncState}), or null if
     *         the row does not exist.
     */
    private SyncState getSyncState(String rowId) {
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
	      db = tp.getReadableDatabase();
	      c = db.query(tp.getTableId(), new String[] { DataTableColumns.SYNC_STATE }, DataTableColumns.ID + " = ?",
	          new String[] { rowId }, null, null, null);
	      if (c.moveToFirst()) {
	        int syncStateIndex = c.getColumnIndex(DataTableColumns.SYNC_STATE);
	        if ( !c.isNull(syncStateIndex) ) {
	          String val = ODKDatabaseUtils.getIndexAsString(c, syncStateIndex);
	          return SyncState.valueOf(val);
	        }
	      }
	      return null;
	    } finally {
    		if ( c != null && !c.isClosed() ) {
    			c.close();
    		}
    		db.close();
	    }
    }
}
