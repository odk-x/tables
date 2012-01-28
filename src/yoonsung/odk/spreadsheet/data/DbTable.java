package yoonsung.odk.spreadsheet.data;

import java.util.Arrays;
import java.util.Map;
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
    
    private static final String DB_ROW_ID = "id";
    private static final String DB_SRC_PHONE_NUMBER = "srcPhoneNum";
    private static final String DB_LAST_MODIFIED_TIME = "lastModTime";
    private static final String DB_SYNC_ID = "syncId";
    private static final String DB_SYNC_TAG = "syncTag";
    
    private final DbHelper dbh;
    private final TableProperties tp;
    
    public static DbTable getDbTable(DbHelper dbh, long tableId) {
        return new DbTable(dbh, tableId);
    }
    
    private DbTable(DbHelper dbh, long tableId) {
        this.dbh = dbh;
        this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
    }
    
    static void createDbTable(SQLiteDatabase db, TableProperties tp) {
        db.execSQL("CREATE TABLE " + tp.getDbTableName() + "(" +
                       DB_ROW_ID + " INTEGER PRIMARY KEY" +
                ", " + DB_SRC_PHONE_NUMBER + " TEXT" +
                ", " + DB_LAST_MODIFIED_TIME + " TEXT NOT NULL" +
                ", " + DB_SYNC_ID + " TEXT" +
                ", " + DB_SYNC_TAG + " TEXT" +
                ")");
    }
    
    /**
     * Gets a table of raw data, including metadata for rows.
     */
    public Table getRawComplete() {
        ColumnProperties[] cps = tp.getColumns();
        String[] cols = new String[cps.length + 4];
        cols[0] = DB_SRC_PHONE_NUMBER;
        cols[1] = DB_LAST_MODIFIED_TIME;
        cols[2] = DB_SYNC_ID;
        cols[3] = DB_SYNC_TAG;
        for (int i = 0; i < cps.length; i++) {
            cols[i + 4] = cps[i].getColumnDbName();
        }
        return dataQuery(cols, null, null, null);
    }
    
    public UserTable getUserTable(String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
        String selection = buildSelectionSql(selectionKeys);
        Table table = dataQuery(tp.getColumnOrder(), selection, selectionArgs,
                orderBy);
        String[] footer = footerQuery(tp.getColumnOrder(), selection,
                selectionArgs, orderBy);
        return new UserTable(table.getRowIds(), tp.getColumnOrder(),
                table.getData(), footer);
    }
    
    public UserTable getUserOverview(String[] primes, String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
        return getUserTable(selectionKeys, selectionArgs, orderBy);
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
        int[] rowIds = new int[rowCount];
        String[][] data = new String[rowCount][columns.length];
        int rowIdIndex = c.getColumnIndexOrThrow(DB_ROW_ID);
        for (int i = 0; i < columns.length; i++) {
            colIndices[i] = c.getColumnIndexOrThrow(columns[i]);
        }
        c.moveToFirst();
        for (int i = 0; i < rowCount; i++) {
            rowIds[i] = c.getInt(rowIdIndex);
            for (int j = 0; j < columns.length; j++) {
                data[i][j] = c.getString(colIndices[j]);
            }
            c.moveToNext();
        }
        return new Table(rowIds, data);
    }
    
    private String[] footerQuery(String[] columns, String selection,
            String[] selectionArgs, String orderBy) {
        return new String[columns.length];
    }
    
    /**
     * Adds a row to the table with the given values, no source phone number,
     * and the current time as the last modification time.
     */
    public void addRow(Map<String, String> values) {
        addRow(values, null, DataUtil.getNowInDbFormat());
    }
    
    /**
     * Adds a row to the table.
     */
    public void addRow(Map<String, String> values, String srcPhone,
            String lastModTime) {
        ContentValues cv = new ContentValues();
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        cv.put(DB_SRC_PHONE_NUMBER, srcPhone);
        cv.put(DB_LAST_MODIFIED_TIME, lastModTime);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.close();
    }
    
    /**
     * Updates a row in the table with the given values, no source phone
     * number, and the current time as the last modification time.
     */
    public void updateRow(int rowId, Map<String, String> values) {
        updateRow(rowId, values, null, DataUtil.getNowInDbFormat());
    }
    
    /**
     * Updates a row in the table.
     */
    public void updateRow(int rowId, Map<String, String> values,
            String srcPhone, String lastModTime) {
        ContentValues cv = new ContentValues();
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        String[] whereArgs = { String.valueOf(rowId) };
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(tp.getDbTableName(), cv, DB_ROW_ID + " = ?", whereArgs);
        db.close();
    }
    
    /**
     * Deletes the given row from the table.
     */
    public void deleteRow(int rowId) {
        String[] whereArgs = { String.valueOf(rowId) };
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.delete(tp.getDbTableName(), DB_ROW_ID + " = ?", whereArgs);
        db.close();
    }
    
    private String buildSelectionSql(String[] selectionKeys) {
        if ((selectionKeys == null) || (selectionKeys.length == 0)) {
            return null;
        }
        StringBuilder selBuilder = new StringBuilder();
        for (String key : selectionKeys) {
            selBuilder.append(" AND " + key);
        }
        if (selBuilder.length() > 0) {
            selBuilder.delete(0, 5);
        }
        return selBuilder.toString();
    }
}
