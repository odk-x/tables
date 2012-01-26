package yoonsung.odk.spreadsheet.data;

import java.util.Map;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    
    DbTable(DbHelper dbh, TableProperties tp) {
        this.dbh = dbh;
        this.tp = tp;
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
     * Queries the table with the given options and returns a Table.
     */
    private Table dataQuery(String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having,
            String orderBy) {
        String[] colArr = new String[columns.length + 1];
        colArr[0] = DB_ROW_ID;
        for (int i = 0; i < columns.length; i++) {
            colArr[i + 1] = columns[i];
        }
        int[] colIndices = new int[columns.length];
        SQLiteDatabase db = dbh.getWritableDatabase();
        Cursor c = db.query(tp.getDbTableName(), colArr, selection,
                selectionArgs, groupBy, having, orderBy);
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
        }
        c.close();
        db.close();
        return new Table(rowIds, data);
    }
    
    /**
     * @return an array of the column database names
     */
    private String[] getColumnArray() {
        ColumnProperties[] cps = tp.getColumns();
        String[] arr = new String[cps.length];
        for (int i = 0; i < cps.length; i++) {
            arr[i] = cps[i].getColumnDbName();
        }
        return arr;
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
        db.insert(tp.getDbTableName(), null, cv);
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
}
