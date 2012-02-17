package yoonsung.odk.spreadsheet.data;

import java.util.Map;
import java.util.UUID;
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
    public static final String DB_SYNC_ID = "syncId";
    public static final String DB_SYNC_TAG = "syncTag";
    public static final String DB_SYNC_STATE = "syncState";
    public static final String DB_TRANSACTIONING = "transactioning";
    
    private final DbHelper dbh;
    private final TableProperties tp;
    
    public static DbTable getDbTable(DbHelper dbh, String tableId) {
        return new DbTable(dbh, tableId);
    }
    
    private DbTable(DbHelper dbh, String tableId) {
        this.dbh = dbh;
        this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
    }
    
    static void createDbTable(SQLiteDatabase db, TableProperties tp) {
        db.execSQL("CREATE TABLE " + tp.getDbTableName() + "(" +
                       DB_ROW_ID + " TEXT UNIQUE NOT NULL" +
                ", " + DB_SRC_PHONE_NUMBER + " TEXT" +
                ", " + DB_LAST_MODIFIED_TIME + " TEXT NOT NULL" +
                ", " + DB_SYNC_ID + " TEXT" +
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
            columns = new String[cps.length + 6];
            columns[0] = DB_SRC_PHONE_NUMBER;
            columns[1] = DB_LAST_MODIFIED_TIME;
            columns[2] = DB_SYNC_ID;
            columns[3] = DB_SYNC_TAG;
            columns[4] = DB_SYNC_STATE;
            columns[5] = DB_TRANSACTIONING;
            for (int i = 0; i < cps.length; i++) {
                columns[i + 4] = cps[i].getColumnDbName();
            }
        }
        return dataQuery(columns, buildSelectionSql(selectionKeys),
                selectionArgs, orderBy);
    }
    
    /**
     * Gets a user table. Rows marked as deleted will not be included.
     */
    public UserTable getUserTable(String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
        String selection = buildUserSelectionSql(selectionKeys);
        Table table = dataQuery(tp.getColumnOrder(), selection, selectionArgs,
                orderBy);
        String[] footer = footerQuery(tp.getColumnOrder(), selection,
                selectionArgs);
        return new UserTable(table.getRowIds(), getUserHeader(),
                table.getData(), footer);
    }
    
    /**
     * Gets a collecton overview user table. Rows marked as deleted will not be
     * included.
     */
    public UserTable getUserOverview(String[] primes, String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
        if (primes.length == 0) {
            return getUserTable(selectionKeys, selectionArgs, orderBy);
        }
        String selection = buildUserSelectionSql(selectionKeys);
        
        StringBuilder allSelectList = new StringBuilder("y." + DB_ROW_ID +
                " AS " + DB_ROW_ID);
        for (String col : tp.getColumnOrder()) {
            allSelectList.append(", y." + col + " AS " + col);
        }
        
        StringBuilder xList = new StringBuilder();
        StringBuilder yList = new StringBuilder();
        for (String prime : primes) {
            xList.append(prime + ", ");
            yList.append(", " + prime);
        }
        if (orderBy == null) {
            xList.delete(xList.length() - 2, xList.length());
        } else {
            xList.append(orderBy);
        }
        yList.delete(0, 2);
        StringBuilder xSelect = new StringBuilder();
        xSelect.append("SELECT MAX(" + DB_ROW_ID + ") as id");
        if (orderBy != null) {
            xSelect.append(", " + xList.toString());
        }
        xSelect.append(" FROM " + tp.getDbTableName());
        if (selection != null) {
            xSelect.append(" WHERE " + selection);
        }
        xSelect.append(" GROUP BY " + xList.toString());
        
        StringBuilder idSelect;
        if (orderBy == null) {
            idSelect = xSelect;
        } else {
            StringBuilder joinBuilder = new StringBuilder();
            joinBuilder.append("y.s" + orderBy + " = x." + orderBy);
            for (String prime : primes) {
                joinBuilder.append(" AND x." + prime + " = y." + prime);
            }
            idSelect = new StringBuilder();
            idSelect.append("SELECT x." + DB_ROW_ID + " FROM (");
            idSelect.append(xSelect.toString());
            idSelect.append(") x JOIN (");
            idSelect.append("SELECT MAX(" + orderBy + ") AS s" + orderBy);
            idSelect.append(", " + yList.toString());
            idSelect.append(" FROM " + tp.getDbTableName());
            if (selection != null) {
                idSelect.append(" WHERE " + selection);
            }
            idSelect.append(" GROUP BY " + yList.toString());
            idSelect.append(") y ON " + joinBuilder.toString());
        }
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT " + allSelectList.toString() + " FROM (");
        sqlBuilder.append(idSelect.toString());
        sqlBuilder.append(") x JOIN " + tp.getDbTableName() + " y");
        sqlBuilder.append(" ON x." + DB_ROW_ID + " = y." + DB_ROW_ID);
        
        String[] dSelectionArgs = null;
        if (selectionArgs != null) {
            dSelectionArgs = new String[selectionArgs.length * 2];
            for (int i = 0; i < selectionArgs.length; i++) {
                dSelectionArgs[i] = selectionArgs[i];
                dSelectionArgs[selectionArgs.length + i] = selectionArgs[i];
            }
        }
        
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(sqlBuilder.toString(), dSelectionArgs);
        Table table = buildTable(c, tp.getColumnOrder());
        c.close();
        db.close();
        String[] footer = footerQuery(tp.getColumnOrder(), selection,
                selectionArgs);
        return new UserTable(table.getRowIds(), getUserHeader(),
                table.getData(), footer);
    }
    
    /**
     * Queries the table with the given options and returns a Table.
     */
    private Table dataQuery(String[] columns, String selection,
            String[] selectionArgs, String orderBy) {
        String[] colArr = new String[columns.length + 1];
        colArr[0] = DB_ROW_ID;
        for (int i = 0; i < columns.length; i++) {
            colArr[i + 1] = columns[i];
        }
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.query(tp.getDbTableName(), colArr, selection,
                selectionArgs, null, null, orderBy);
        Table table = buildTable(c, columns);
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
                data[i][j] = c.getString(colIndices[j]);
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
    
    private String[] footerQuery(String[] columns, String selection,
            String[] selectionArgs) {
        ColumnProperties[] cps = new ColumnProperties[columns.length];
        StringBuilder sqlBuilder = new StringBuilder("SELECT");
        for (int i = 0; i < columns.length; i++) {
            String colDbName = columns[i];
            cps[i] = tp.getColumnByDbName(colDbName);
            int mode = cps[i].getFooterMode();
            switch (mode) {
            case ColumnProperties.FooterMode.COUNT:
                sqlBuilder.append(", COUNT(" + colDbName + ") AS " +
                        colDbName);
                break;
            case ColumnProperties.FooterMode.MAXIMUM:
                sqlBuilder.append(", MAX(" + colDbName + ") AS " + colDbName);
                break;
            case ColumnProperties.FooterMode.MEAN:
                sqlBuilder.append(", COUNT(" + colDbName + ") AS count" +
                        colDbName);
                sqlBuilder.append(", SUM(" + colDbName + ") AS sum" +
                        colDbName);
                break;
            case ColumnProperties.FooterMode.MINIMUM:
                sqlBuilder.append(", MIN(" + colDbName + ") AS " + colDbName);
                break;
            case ColumnProperties.FooterMode.SUM:
                sqlBuilder.append(", SUM(" + colDbName + ") AS " + colDbName);
                break;
            }
        }
        if (sqlBuilder.length() == 6) {
            return new String[columns.length];
        }
        sqlBuilder.delete(6, 7);
        sqlBuilder.append(" FROM " + tp.getDbTableName());
        if ((selection != null) && (selection.length() != 0)) {
            sqlBuilder.append(" WHERE " + selection);
        }
        String[] footer = new String[columns.length];
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(sqlBuilder.toString(), selectionArgs);
        c.moveToFirst();
        for (int i = 0; i < columns.length; i++) {
            if (cps[i].getFooterMode() == ColumnProperties.FooterMode.MEAN) {
                int sIndex = c.getColumnIndexOrThrow("sum" + columns[i]);
                int cIndex = c.getColumnIndexOrThrow("count" + columns[i]);
                double sum = c.getInt(sIndex);
                int count = c.getInt(cIndex);
                footer[i] = String.valueOf(sum / count);
            } else if (cps[i].getFooterMode() !=
                    ColumnProperties.FooterMode.NONE) {
                int index = c.getColumnIndexOrThrow(columns[i]);
                footer[i] = c.getString(index);
            }
        }
        c.close();
        db.close();
        return footer;
    }
    
    /**
     * Adds a row to the table with the given values, no source phone number,
     * the current time as the last modification time, and an inserting
     * synchronization state.
     */
    public void addRow(Map<String, String> values) {
        addRow(values, DataUtil.getNowInDbFormat(), null);
    }
    
    /**
     * Adds a row to the table with an inserting synchronization state and the
     * transactioning status set to false.
     */
    public void addRow(Map<String, String> values, String lastModTime,
            String srcPhone) {
        if (lastModTime == null) {
            lastModTime = DataUtil.getNowInDbFormat();
        }
        ContentValues cv = new ContentValues();
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        cv.put(DB_LAST_MODIFIED_TIME, lastModTime);
        cv.put(DB_SRC_PHONE_NUMBER, srcPhone);
        cv.put(DB_SYNC_STATE, SyncUtil.State.INSERTING);
        cv.put(DB_TRANSACTIONING, SyncUtil.Transactioning.FALSE);
        actualAddRow(cv);
    }
    
    /**
     * Actually adds a row.
     * @param values the values to put in the row
     */
    public void actualAddRow(ContentValues values) {
        String id = UUID.randomUUID().toString();
        values.put(DB_ROW_ID, id);
        SQLiteDatabase db = dbh.getWritableDatabase();
        long result = db.insert(tp.getDbTableName(), null, values);
        db.close();
        Log.d("DBT", "insert, id=" + result);
        tp.setSyncState(SyncUtil.State.UPDATING);
    }
    
    /**
     * Updates a row in the table with the given values, no source phone
     * number, and the current time as the last modification time.
     */
    public void updateRow(String rowId, Map<String, String> values) {
        updateRow(rowId, values, null, DataUtil.getNowInDbFormat());
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
    
    /**
     * Actually updates a row.
     * @param syncId the synchronization ID of the row to update
     * @param values the values to update the row with
     */
    public void actualUpdateRowBySyncId(String syncId, ContentValues values) {
        String[] whereArgs = { syncId };
        actualUpdateRow(values, DB_SYNC_ID + " = ?", whereArgs);
    }
    
    private void actualUpdateRow(ContentValues values, String where,
            String[] whereArgs) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(tp.getDbTableName(), values, where, whereArgs);
        db.close();
        tp.setSyncState(SyncUtil.State.UPDATING);
    }
    
    /**
     * Marks the given row as deleted.
     */
    public void markDeleted(String rowId) {
        String[] whereArgs = { rowId };
        ContentValues values = new ContentValues();
        values.put(DB_SYNC_STATE, SyncUtil.State.DELETING);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(tp.getDbTableName(), values, DB_ROW_ID + " = ?", whereArgs);
        db.close();
        tp.setSyncState(SyncUtil.State.UPDATING);
    }
    
    /**
     * Actually deletes a row from the table.
     * @param rowId the ID of the row to delete
     */
    public void deleteRowActual(String rowId) {
        String[] whereArgs = { rowId };
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.delete(tp.getDbTableName(), DB_ROW_ID + " + ?", whereArgs);
        db.close();
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
    
    /**
     * Builds a SQL selection string that will exclude rows marked as deleted.
     */
    private String buildUserSelectionSql(String[] selectionKeys) {
        String sql = buildSelectionSql(selectionKeys);
        if (sql == null) {
            return DbTable.DB_SYNC_STATE + " != " + SyncUtil.State.DELETING;
        } else {
            return sql + " AND " + DbTable.DB_SYNC_STATE + " != " +
                    SyncUtil.State.DELETING;
        }
    }
}
