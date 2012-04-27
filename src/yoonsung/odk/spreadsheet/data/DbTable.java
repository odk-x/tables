package yoonsung.odk.spreadsheet.data;

import java.util.Map;
import java.util.UUID;
import yoonsung.odk.spreadsheet.data.Query.SqlData;
import yoonsung.odk.spreadsheet.sync.SyncUtil;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and modifying a user table.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 */
public class DbTable {
    
    public static final String DB_ROW_ID = "id";
    public static final String DB_SRC_PHONE_NUMBER = "srcPhoneNum";
    public static final String DB_LAST_MODIFIED_TIME = "lastModTime";
    public static final String DB_SYNC_TAG = "syncTag";
    public static final String DB_SYNC_STATE = "syncState";
    public static final String DB_TRANSACTIONING = "transactioning";
    
    private final DataUtil du;
    private final DbHelper dbh;
    private final TableProperties tp;
    
    public static DbTable getDbTable(DbHelper dbh, String tableId) {
        return new DbTable(dbh, tableId);
    }
    
    private DbTable(DbHelper dbh, String tableId) {
        this.du = DataUtil.getDefaultDataUtil();
        this.dbh = dbh;
        this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
    }
    
    static void createDbTable(SQLiteDatabase db, TableProperties tp) {
        db.execSQL("CREATE TABLE " + tp.getDbTableName() + "(" +
                       DB_ROW_ID + " TEXT UNIQUE NOT NULL" +
                ", " + DB_SRC_PHONE_NUMBER + " TEXT" +
                ", " + DB_LAST_MODIFIED_TIME + " TEXT NOT NULL" +
                ", " + DB_SYNC_TAG + " TEXT" +
                ", " + DB_SYNC_STATE + " INTEGER NOT NULL" +
                ", " + DB_TRANSACTIONING + " INTEGER NOT NULL" +
                ")");
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
    public Table getRaw(String[] columns, String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
        if (columns == null) {
            ColumnProperties[] cps = tp.getColumns();
            columns = new String[cps.length + 5];
            columns[0] = DB_SRC_PHONE_NUMBER;
            columns[1] = DB_LAST_MODIFIED_TIME;
            columns[2] = DB_SYNC_TAG;
            columns[3] = DB_SYNC_STATE;
            columns[4] = DB_TRANSACTIONING;
            for (int i = 0; i < cps.length; i++) {
                columns[i + 5] = cps[i].getColumnDbName();
            }
        }
        String[] colArr = new String[columns.length + 1];
        colArr[0] = DB_ROW_ID;
        for (int i = 0; i < columns.length; i++) {
            colArr[i + 1] = columns[i];
        }
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.query(tp.getDbTableName(), colArr,
                buildSelectionSql(selectionKeys),
                selectionArgs, null, null, orderBy);
        Table table = buildTable(c, columns);
        c.close();
        db.close();
        return table;
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
        SqlData sd = query.toGroupSql(groupColumn.getColumnDbName(), type);
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(sd.getSql(), sd.getArgs());
        int gcColIndex = c.getColumnIndexOrThrow(
                groupColumn.getColumnDbName());
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
        c.close();
        db.close();
        return new GroupTable(keys, values);
    }
    
    private Table dataQuery(SqlData sd) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(sd.getSql(), sd.getArgs());
        Table table = buildTable(c, tp.getColumnOrder());
        c.close();
        db.close();
        return table;
    }
    
    /**
     * Builds a Table with the data from the given cursor.
     * The cursor, but not the columns array, must include the row ID column.
     */
    private Table buildTable(Cursor c, String[] columns) {
        int[] colIndices = new int[columns.length];
        int rowCount = c.getCount();
        String[] rowIds = new String[rowCount];
        String[][] data = new String[rowCount][columns.length];
        int rowIdIndex = c.getColumnIndexOrThrow(DB_ROW_ID);
        for (int i = 0; i < columns.length; i++) {
            colIndices[i] = c.getColumnIndexOrThrow(columns[i]);
        }
        c.moveToFirst();
        for (int i = 0; i < rowCount; i++) {
            rowIds[i] = c.getString(rowIdIndex);
            for (int j = 0; j < columns.length; j++) {
              String value;
              try {
                value = c.getString(colIndices[j]);
              } catch (Exception e) {
                try {
                  value = String.valueOf(c.getInt(colIndices[j]));
                } catch (Exception f) {
                  value = String.valueOf(c.getDouble(colIndices[j]));
                }
              }
              data[i][j] = value;
            }
            c.moveToNext();
        }
        return new Table(rowIds, columns, data);
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
            case ColumnProperties.FooterMode.COUNT:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.COUNT);
                break;
            case ColumnProperties.FooterMode.MAXIMUM:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.MAXIMUM);
                break;
            case ColumnProperties.FooterMode.MINIMUM:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.MINIMUM);
                break;
            case ColumnProperties.FooterMode.SUM:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.SUM);
                break;
            case ColumnProperties.FooterMode.MEAN:
                footer[i] = getFooterItem(query, cps[i],
                        Query.GroupQueryType.AVERAGE);
                break;
            }
        }
        return footer;
    }
    
    private String getFooterItem(Query query, ColumnProperties cp,
            Query.GroupQueryType type) {
        SqlData sd = query.toGroupSql(cp.getColumnDbName(), type);
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(sd.getSql(), sd.getArgs());
        int gColIndex = c.getColumnIndexOrThrow("g");
        c.moveToFirst();
        String value = c.getString(gColIndex);
        c.close();
        db.close();
        return value;
    }
    
    /**
     * Adds a row to the table with the given values, no source phone number,
     * the current time as the last modification time, and an inserting
     * synchronization state.
     */
    public void addRow(Map<String, String> values) {
        addRow(values, null, null);
    }
    
    /**
     * Adds a row to the table with an inserting synchronization state and the
     * transactioning status set to false.
     */
    public void addRow(Map<String, String> values, String lastModTime,
            String srcPhone) {
        if (lastModTime == null) {
            lastModTime = du.formatNowForDb();
        }
        ContentValues cv = new ContentValues();
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        cv.put(DB_LAST_MODIFIED_TIME, lastModTime);
        cv.put(DB_SRC_PHONE_NUMBER, srcPhone);
        cv.put(DB_SYNC_STATE, SyncUtil.State.INSERTING);
        cv.put(DB_TRANSACTIONING, SyncUtil.boolToInt(false));
        actualAddRow(cv);
    }
    
    /**
     * Actually adds a row.
     * @param values the values to put in the row
     */
    public void actualAddRow(ContentValues values) {
        if (!values.containsKey(DB_ROW_ID)) {
          String id = UUID.randomUUID().toString();
          values.put(DB_ROW_ID, id);
        }
        SQLiteDatabase db = dbh.getWritableDatabase();
        long result = db.insert(tp.getDbTableName(), null, values);
        db.close();
        Log.d("DBT", "insert, id=" + result);
    }
    
    /**
     * Updates a row in the table with the given values, no source phone
     * number, and the current time as the last modification time.
     */
    public void updateRow(String rowId, Map<String, String> values) {
        updateRow(rowId, values, null, du.formatNowForDb());
    }
    
    /**
     * Updates a row in the table and marks its synchronization state as
     * updating.
     * @param rowId the ID of the row to update
     * @param values the values to update the row with
     * @param srcPhone the source phone number to put in the row
     * @param lastModTime the last modification time to put in the row
     */
    public void updateRow(String rowId, Map<String, String> values,
            String srcPhone, String lastModTime) {
        ContentValues cv = new ContentValues();
        if (tp.isSynchronized() && getSyncState(rowId) == SyncUtil.State.REST)
          cv.put(DB_SYNC_STATE, SyncUtil.State.UPDATING);
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
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
        actualUpdateRow(values, DB_ROW_ID + " = ?", whereArgs);
    }
    
    private void actualUpdateRow(ContentValues values, String where,
            String[] whereArgs) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(tp.getDbTableName(), values, where, whereArgs);
        db.close();
    }
    
   /**
    * If table is synchronized and not in an INSERTING state, marks row as
    * deleted. Otherwise, actually deletes the row.
    */
    public void markDeleted(String rowId) {
      if (!tp.isSynchronized()) {
        deleteRowActual(rowId);
      } else {
        int syncState = getSyncState(rowId);
        if (syncState == SyncUtil.State.INSERTING) {
          deleteRowActual(rowId);
        } else if (syncState == SyncUtil.State.REST || syncState == SyncUtil.State.UPDATING) {
          String[] whereArgs = { rowId };
          ContentValues values = new ContentValues();
          values.put(DB_SYNC_STATE, SyncUtil.State.DELETING);
          SQLiteDatabase db = dbh.getWritableDatabase();
          db.update(tp.getDbTableName(), values, DB_ROW_ID + " = ?", whereArgs);
          db.close();
        }
      }
    }
      
        /**
         * Actually deletes a row from the table.
       * @param rowId the ID of the row to delete
     */
    public void deleteRowActual(String rowId) {
        String[] whereArgs = { rowId };
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.delete(tp.getDbTableName(), DB_ROW_ID + " = ?", whereArgs);
        db.close();
    }
    
    /**
     * @param rowId
     * @return the sync state of the row (see {@link SyncUtil.State}), or -1 if
     *         the row does not exist.
     */
    private int getSyncState(String rowId) {
      SQLiteDatabase db = dbh.getReadableDatabase();
      Cursor c = db.query(tp.getDbTableName(), new String[] { DB_SYNC_STATE }, DB_ROW_ID + " = ?",
          new String[] { rowId }, null, null, null);
      int syncState = -1;
      if (c.moveToFirst()) {
        int syncStateIndex = c.getColumnIndex(DB_SYNC_STATE);
        syncState = c.getInt(syncStateIndex);
      }
      c.close();
      db.close();
      return syncState;
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
}
