package yoonsung.odk.spreadsheet.data;

import yoonsung.odk.spreadsheet.sync.SyncUtil;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and managing table properties.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 */
public class TableProperties {
    
    // the name of the table properties table in the database
    private static final String DB_TABLENAME = "tableProps";
    // names of columns in the table properties table
    private static final String DB_TABLE_ID = "tableId";
    private static final String DB_DB_TABLE_NAME = "dbTableName";
    private static final String DB_DISPLAY_NAME = "displayName";
    private static final String DB_TABLE_TYPE = "type";
    private static final String DB_COLUMN_ORDER = "colOrder";
    private static final String DB_PRIME_COLUMNS = "primeCols";
    private static final String DB_SORT_COLUMN = "sortCol";
    private static final String DB_READ_SECURITY_TABLE_ID = "readAccessTid";
    private static final String DB_WRITE_SECURITY_TABLE_ID = "writeAccessTid";
    private static final String DB_SYNC_MODIFICATION_NUMBER = "syncModNum";
    private static final String DB_LAST_SYNC_TIME = "lastSyncTime";
    private static final String DB_DETAIL_VIEW_FILE = "detailViewFile";
    private static final String DB_SUM_DISPLAY_FORMAT = "summaryDisplayFormat";
    private static final String DB_SYNC_STATE = "syncState";
    private static final String DB_TRANSACTIONING = "transactioning";
    
    // the SQL where clause to use for selecting, updating, or deleting the row
    // for a given table
    private static final String ID_WHERE_SQL = DB_TABLE_ID + " = ?";
    // the SQL where clause to use for selecting by table type
    private static final String TYPE_WHERE_SQL = DB_TABLE_TYPE + " = ?";
    // the columns to be selected when initializing TableProperties
    private static final String[] INIT_COLUMNS = {
        DB_TABLE_ID,
        DB_DB_TABLE_NAME,
        DB_DISPLAY_NAME,
        DB_TABLE_TYPE,
        DB_COLUMN_ORDER,
        DB_PRIME_COLUMNS,
        DB_SORT_COLUMN,
        DB_READ_SECURITY_TABLE_ID,
        DB_WRITE_SECURITY_TABLE_ID,
        DB_SYNC_MODIFICATION_NUMBER,
        DB_LAST_SYNC_TIME,
        DB_DETAIL_VIEW_FILE,
        DB_SUM_DISPLAY_FORMAT,
        DB_SYNC_STATE,
        DB_TRANSACTIONING, 
    };
    
    public class TableType {
        public static final int DATA = 0;
        public static final int SECURITY = 1;
        public static final int SHORTCUT = 2;
        private TableType() {}
    }
    
    private final DbHelper dbh;
    private final String[] whereArgs;
    
    private final long tableId;
    private String dbTableName;
    private String displayName;
    private int tableType;
    private ColumnProperties[] columns;
    private String[] columnOrder;
    private String[] primeColumns;
    private String sortColumn;
    private long readSecurityTableId;
    private long writeSecurityTableId;
    private int syncModificationNumber;
    private String lastSyncTime;
    private String detailViewFilename;
    private String sumDisplayFormat;
    private int syncState;
    private int transactioning;
    
    private TableProperties(DbHelper dbh, long tableId, String dbTableName,
            String displayName, int tableType, String[] columnOrder,
            String[] primeColumns, String sortColumn, long readSecurityTableId,
            long writeSecurityTableId, int syncModificationNumber,
            String lastSyncTime, String detailViewFilename,
            String sumDisplayFormat, int syncState, int transactioning) {
        this.dbh = dbh;
        whereArgs = new String[] { String.valueOf(tableId) };
        this.tableId = tableId;
        this.dbTableName = dbTableName;
        this.displayName = displayName;
        this.tableType = tableType;
        columns = null;
        this.columnOrder = columnOrder;
        this.primeColumns = primeColumns;
        this.sortColumn = sortColumn;
        this.readSecurityTableId = readSecurityTableId;
        this.writeSecurityTableId = writeSecurityTableId;
        this.syncModificationNumber = syncModificationNumber;
        this.lastSyncTime = lastSyncTime;
        this.detailViewFilename = detailViewFilename;
        this.sumDisplayFormat = sumDisplayFormat;
        this.syncState = syncState;
        this.transactioning = transactioning;
    }
    
    public static TableProperties getTablePropertiesForTable(DbHelper dbh,
            long tableId) {
        TableProperties[] res = queryForTableProperties(dbh, ID_WHERE_SQL,
                new String[] {String.valueOf(tableId)});
        return res[0];
    }
    
    public static TableProperties[] getTablePropertiesForAll(DbHelper dbh) {
        return queryForTableProperties(dbh, null, null);
    }
    
    public static TableProperties[] getTablePropertiesForDataTables(DbHelper dbh) {
        return queryForTableProperties(dbh, TYPE_WHERE_SQL,
                new String[] { String.valueOf(TableType.DATA) });
    }
    
    public static TableProperties[] getTablePropertiesForSecurityTables(
            DbHelper dbh) {
        return queryForTableProperties(dbh, TYPE_WHERE_SQL,
                new String[] { String.valueOf(TableType.SECURITY) });
    }
    
    public static TableProperties[] getTablePropertiesForShortcutTables(
            DbHelper dbh) {
        return queryForTableProperties(dbh, TYPE_WHERE_SQL,
                new String[] { String.valueOf(TableType.SHORTCUT) });
    }
    
    private static TableProperties[] queryForTableProperties(DbHelper dbh,
            String where, String[] whereArgs) {
        where += " AND " + DB_SYNC_STATE + " != " + SyncUtil.State.DELETING;
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.query(DB_TABLENAME, INIT_COLUMNS, where, whereArgs, null,
                null, null);
        TableProperties[] tps = new TableProperties[c.getCount()];
        int tableIdIndex = c.getColumnIndex(DB_TABLE_ID);
        int dbtnIndex = c.getColumnIndexOrThrow(DB_DB_TABLE_NAME);
        int displayNameIndex = c.getColumnIndexOrThrow(DB_DISPLAY_NAME);
        int tableTypeIndex = c.getColumnIndexOrThrow(DB_TABLE_TYPE);
        int columnOrderIndex = c.getColumnIndexOrThrow(DB_COLUMN_ORDER);
        int primeColumnsIndex = c.getColumnIndexOrThrow(DB_PRIME_COLUMNS);
        int sortColumnIndex = c.getColumnIndexOrThrow(DB_SORT_COLUMN);
        int rsTableId = c.getColumnIndexOrThrow(DB_READ_SECURITY_TABLE_ID);
        int wsTableId = c.getColumnIndexOrThrow(DB_WRITE_SECURITY_TABLE_ID);
        int syncModNumIndex =
            c.getColumnIndexOrThrow(DB_SYNC_MODIFICATION_NUMBER);
        int lastSyncTimeIndex = c.getColumnIndexOrThrow(DB_LAST_SYNC_TIME);
        int detailViewFileIndex = c.getColumnIndexOrThrow(DB_DETAIL_VIEW_FILE);
        int sumDisplayFormatIndex =
            c.getColumnIndexOrThrow(DB_SUM_DISPLAY_FORMAT);
        int syncStateIndex = c.getColumnIndexOrThrow(DB_SYNC_STATE);
        int transactioningIndex = c.getColumnIndexOrThrow(DB_TRANSACTIONING);
        
        int i = 0;
        c.moveToFirst();
        while (i < tps.length) {
            String columnOrderValue = c.getString(columnOrderIndex);
            String[] columnOrder = (columnOrderValue.length() == 0) ?
                new String[] {} : columnOrderValue.split("/");
            String primeOrderValue = c.getString(primeColumnsIndex);
            String[] primeList = (primeOrderValue.length() == 0) ?
                new String[] {} : primeOrderValue.split("/");
            tps[i] = new TableProperties(dbh, c.getLong(tableIdIndex),
                    c.getString(dbtnIndex), c.getString(displayNameIndex),
                    c.getInt(tableTypeIndex), columnOrder, primeList,
                    c.getString(sortColumnIndex), c.getLong(rsTableId),
                    c.getLong(wsTableId), c.getInt(syncModNumIndex),
                    c.getString(lastSyncTimeIndex),
                    c.getString(detailViewFileIndex),
                    c.getString(sumDisplayFormatIndex),
                    c.getInt(syncStateIndex),
                    c.getInt(transactioningIndex));
            i++;
            c.moveToNext();
        }
        c.close();
        db.close();
        return tps;
    }
    
    public static String createDbTableName(DbHelper dbh, String displayName) {
        TableProperties[] allProps = getTablePropertiesForAll(dbh);
        String baseName = displayName.replace(' ', '_');
        if (!nameConflict(baseName, allProps)) {
            return baseName;
        }
        int suffix = 1;
        while (true) {
            String nextName = baseName + suffix;
            if (!nameConflict(nextName, allProps)) {
                return nextName;
            }
            suffix++;
        }
    }
    
    private static boolean nameConflict(String dbTableName,
            TableProperties[] allProps) {
        for (TableProperties tp : allProps) {
            if (tp.getDbTableName().equals(dbTableName)) {
                return true;
            }
        }
        return false;
    }
    
    public static TableProperties addTable(DbHelper dbh, String dbTableName,
            String displayName, int tableType) {
        ContentValues values = new ContentValues();
        values.put(DB_DB_TABLE_NAME, dbTableName);
        values.put(DB_DISPLAY_NAME, displayName);
        values.put(DB_TABLE_TYPE, tableType);
        values.put(DB_COLUMN_ORDER, "");
        values.put(DB_PRIME_COLUMNS, "");
        values.putNull(DB_SORT_COLUMN);
        values.put(DB_READ_SECURITY_TABLE_ID, -1);
        values.put(DB_WRITE_SECURITY_TABLE_ID, -1);
        values.put(DB_SYNC_MODIFICATION_NUMBER, -1);
        values.put(DB_LAST_SYNC_TIME, -1);
        values.putNull(DB_DETAIL_VIEW_FILE);
        values.putNull(DB_SUM_DISPLAY_FORMAT);
        values.put(DB_SYNC_STATE, SyncUtil.State.INSERTING);
        values.put(DB_TRANSACTIONING, SyncUtil.Transactioning.FALSE);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        long id = db.insert(DB_TABLENAME, null, values);
        Log.d("TP", "new id=" + id);
        TableProperties tp = new TableProperties(dbh, id, dbTableName,
                displayName, tableType, new String[0], new String[0], null, -1,
                -1, -1, null, null, null, SyncUtil.State.INSERTING,
                SyncUtil.Transactioning.FALSE);
        DbTable.createDbTable(db, tp);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        return tp;
    }
    
    public void deleteTable() {
        ContentValues values = new ContentValues();
        values.put(DB_SYNC_STATE, SyncUtil.State.DELETING);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(DB_TABLENAME, values, ID_WHERE_SQL, whereArgs);
        db.close();
    }
    
    public void deleteTableActual() {
        ColumnProperties[] columns = getColumns();
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("DROP TABLE " + dbTableName);
        for (ColumnProperties cp : columns) {
            cp.deleteColumn(db);
        }
        db.delete(DB_TABLENAME, ID_WHERE_SQL, whereArgs);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }
    
    public long getTableId() {
        return tableId;
    }
    
    /**
     * @return the table's name in the database
     */
    public String getDbTableName() {
        return dbTableName;
    }
    
    /**
     * @return the table's display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Sets the table's display name.
     * @param displayName the new display name
     */
    public void setDisplayName(String displayName) {
        setStringProperty(DB_DISPLAY_NAME, displayName);
        this.displayName = displayName;
    }
    
    /**
     * @return the table's type
     */
    public int getTableType() {
        return tableType;
    }
    
    /**
     * Sets the table's type.
     * @param tableType the new table type
     */
    public void setTableType(int tableType) {
        setIntProperty(DB_TABLE_TYPE, tableType);
        this.tableType = tableType;
    }
    
    /**
     * @return an unordered array of the table's columns
     */
    public ColumnProperties[] getColumns() {
        if (columns == null) {
            ColumnProperties[] cps = ColumnProperties
                    .getColumnPropertiesForTable(dbh, tableId);
            columns = new ColumnProperties[cps.length];
            for (int i = 0; i < columnOrder.length; i++) {
                for (int j = 0; j < cps.length; j++) {
                    if (cps[j].getColumnDbName().equals(columnOrder[i])) {
                        columns[i] = cps[j];
                        break;
                    }
                }
            }
        }
        return columns;
    }
    
    public ColumnProperties getColumnByDbName(String colDbName) {
        int colIndex = getColumnIndex(colDbName);
        if (colIndex < 0) {
            return null;
        }
        return getColumns()[colIndex];
    }
    
    public int getColumnIndex(String colDbName) {
        String[] colOrder = getColumnOrder();
        for (int i = 0; i < colOrder.length; i++) {
            if (colOrder[i].equals(colDbName)) {
                return i;
            }
        }
        return -1;
    }
    
    public String getColumnByDisplayName(String displayName) {
        ColumnProperties[] cps = getColumns();
        for (ColumnProperties cp : cps) {
            String cdn = cp.getDisplayName();
            if ((cdn != null) && (cdn.equals(displayName))) {
                return cp.getColumnDbName();
            }
        }
        return null;
    }
    
    public String getColumnByAbbreviation(String abbreviation) {
        ColumnProperties[] cps = getColumns();
        for (ColumnProperties cp : cps) {
            String ca = cp.getAbbreviation();
            if ((ca != null) && (ca.equals(abbreviation))) {
                return cp.getColumnDbName();
            }
        }
        return null;
    }
    
    /**
     * Adds a column to the table using a default database name.
     * @param displayName the column's display name
     * @return ColumnProperties for the new table
     */
    public ColumnProperties addColumn(String displayName) {
        // ensuring columns is initialized
        getColumns();
        // determining a database name for the column
        String baseName = "_" + displayName.toLowerCase().replace(' ', '_');
        if (!columnNameConflict(baseName)) {
            return addColumn(displayName, baseName);
        }
        int suffix = 1;
        while (true) {
            String name = baseName + suffix;
            if (!columnNameConflict(name)) {
                return addColumn(displayName, name);
            }
            suffix++;
        }
    }
    
    private boolean columnNameConflict(String name) {
        for (ColumnProperties cp : columns) {
            if (cp.getColumnDbName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Adds a column to the table.
     * @param displayName the column's display name
     * @param dbName the database name for the new column
     * @return ColumnProperties for the new table
     */
    public ColumnProperties addColumn(String displayName, String dbName) {
        // ensuring columns is initialized
        getColumns();
        // preparing column order
        ColumnProperties[] newColumns =
            new ColumnProperties[columns.length + 1];
        String[] newColumnOrder = new String[columns.length + 1];
        for (int i = 0; i < columns.length; i++) {
            newColumns[i] = columns[i];
            newColumnOrder[i] = columnOrder[i];
        }
        newColumnOrder[columns.length] = dbName;
        // adding column
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        ColumnProperties cp = ColumnProperties.addColumn(dbh, db, tableId,
                dbName, displayName);
        db.execSQL("ALTER TABLE " + dbTableName + " ADD COLUMN " + dbName);
        setColumnOrder(newColumnOrder, db);
        Log.d("TP", "here we are");
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        // updating TableProperties
        newColumns[columns.length] = cp;
        columns = newColumns;
        // returning new ColumnProperties
        return cp;
    }
    
    /**
     * Deletes a column from the table.
     * @param columnDbName the database name of the column to delete
     */
    public void deleteColumn(String columnDbName) {
        // ensuring columns is initialized
        getColumns();
        // finding the index of the column in columns
        int colIndex = 0;
        for (ColumnProperties cp : columns) {
            if (cp.getColumnDbName().equals(columnDbName)) {
                break;
            } else {
                colIndex++;
            }
        }
        if (colIndex == columns.length) {
            Log.e(TableProperties.class.getName(),
                    "deleteColumn() did not find the column");
            return;
        }
        // forming a comma-separated list of columns to keep
        String csv = "";
        for (int i = 0; i < columns.length; i++) {
            if (i == colIndex) {
                continue;
            }
            csv += columns[i].getColumnDbName() + ",";
        }
        csv = csv.substring(0, csv.length() - 1);
        // deleting the column
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        columns[colIndex].deleteColumn(db);
        db.execSQL("CREATE TEMPORARY TABLE backup_(" + csv + ")");
        db.execSQL("INSERT INTO backup_ SELECT " + csv + " FROM " +
                dbTableName);
        db.execSQL("DROP TABLE " + dbTableName);
        db.execSQL("CREATE TABLE " + dbTableName + "(" + csv + ")");
        db.execSQL("INSERT INTO " + dbTableName + " SELECT " + csv +
                " FROM backup_");
        db.execSQL("DROP TABLE backup_");
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        // updating TableProperties
        ColumnProperties[] newColumns =
            new ColumnProperties[columns.length - 1];
        int index = 0;
        for (int i = 0; i < columns.length; i++) {
            if (i == colIndex) {
                continue;
            }
            newColumns[index] = columns[i];
            index++;
        }
        columns = newColumns;
        String[] newColumnOrder = new String[columns.length - 1];
        index = 0;
        for (String col : columnOrder) {
            if (col.equals(columnDbName)) {
                continue;
            }
            newColumnOrder[index] = col;
            index++;
        }
        setColumnOrder(newColumnOrder);
    }
    
    /**
     * @return an ordered array of the database names of the table's columns
     */
    public String[] getColumnOrder() {
        return columnOrder;
    }
    
    /**
     * Sets the column order.
     * @param columnOrder an ordered array of the database names of the table's
     * columns
     */
    public void setColumnOrder(String[] columnOrder) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        setColumnOrder(columnOrder, db);
        db.close();
    }
    
    private void setColumnOrder(String[] columnOrder, SQLiteDatabase db) {
        StringBuilder orderBuilder = new StringBuilder();
        for (String cdn : columnOrder) {
            orderBuilder.append("/" + cdn);
        }
        if (orderBuilder.length() > 0) {
            orderBuilder.delete(0, 1);
        }
        setStringProperty(DB_COLUMN_ORDER, orderBuilder.toString(), db);
        this.columnOrder = columnOrder;
    }
    
    /**
     * @return an array of the database names of the prime columns
     */
    public String[] getPrimeColumns() {
        return primeColumns;
    }
    
    public boolean isColumnPrime(String colDbName) {
        for (String prime : primeColumns) {
            if (prime.equals(colDbName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Sets the table's prime columns.
     * @param primeColumns an array of the database names of the table's prime
     * columns
     */
    public void setPrimeColumns(String[] primeColumns) {
        String str = "";
        for (String cdb : primeColumns) {
            str += cdb + "/";
        }
        setStringProperty(DB_PRIME_COLUMNS, str.substring(0,
                str.length() - 1));
        this.primeColumns = primeColumns;
    }
    
    /**
     * @return the database name of the sort column (or null for no sort
     * column)
     */
    public String getSortColumn() {
        return sortColumn;
    }
    
    /**
     * Sets the table's sort column.
     * @param sortColumn the database name of the new sort column (or null for
     * no sort column)
     */
    public void setSortColumn(String sortColumn) {
        setStringProperty(DB_SORT_COLUMN, sortColumn);
        this.sortColumn = sortColumn;
    }
    
    /**
     * @return the ID of the read security table, or -1 if there is none
     */
    public long getReadSecurityTableId() {
        return readSecurityTableId;
    }
    
    /**
     * Sets the table's read security table.
     * @param tableId the ID of the new read security table (or -1 to set no
     * read security table)
     */
    public void setReadSecurityTableId(long tableId) {
        setLongProperty(DB_READ_SECURITY_TABLE_ID, tableId);
        this.readSecurityTableId = tableId;
    }
    
    /**
     * @return the ID of the write security table, or -1 if there is none
     */
    public long getWriteSecurityTableId() {
        return writeSecurityTableId;
    }
    
    /**
     * Sets the table's write security table.
     * @param tableId the ID of the new write security table (or -1 to set no
     * write security table)
     */
    public void setWriteSecurityTableId(long tableId) {
        setLongProperty(DB_WRITE_SECURITY_TABLE_ID, tableId);
        this.writeSecurityTableId = tableId;
    }
    
    /**
     * @return the sync modification number (or -1 if the table has never been
     * synchronized)
     */
    public int getSyncModificationNumber() {
        return syncModificationNumber;
    }
    
    /**
     * Sets the table's sync modification number.
     * @param modNum the new modification number
     */
    public void setSyncModificationNumber(int modNum) {
        setIntProperty(DB_SYNC_MODIFICATION_NUMBER, modNum);
        this.syncModificationNumber = modNum;
    }
    
    /**
     * @return the last synchronization time (in the format of {@link DataUtil#getNowInDbFormat()}.
     */
    public String getLastSyncTime() {
        return lastSyncTime;
    }
    
    /**
     * Sets the table's last synchronization time.
     * @param time the new synchronization time (in the format of {@link DataUtil#getNowInDbFormat()}).
     */
    public void setLastSyncTime(String time) {
        setStringProperty(DB_LAST_SYNC_TIME, time);
        this.lastSyncTime = time;
    }
    
    /**
     * @return the detail view filename
     */
    public String getDetailViewFilename() {
        return detailViewFilename;
    }
    
    /**
     * Sets the table's detail view filename.
     * @param filename the new filename
     */
    public void setDetailViewFilename(String filename) {
        setStringProperty(DB_DETAIL_VIEW_FILE, filename);
        this.detailViewFilename = filename;
    }
    
    /**
     * @return the format for summary displays
     */
    public String getSummaryDisplayFormat() {
        return sumDisplayFormat;
    }
    
    /**
     * Sets the table's summary display format.
     * @param format the new summary display format
     */
    public void setSummaryDisplayFormat(String format) {
        setStringProperty(DB_SUM_DISPLAY_FORMAT, format);
        this.sumDisplayFormat = format;
    }
    
    /**
     * @return the synchronization state
     */
    public int getSyncState() {
        return syncState;
    }
    
    /**
     * Sets the table's synchronization state.
     * Can only move to or from the REST state (e.g., no skipping straight from
     * INSERTING to UPDATING).
     * @param state the new synchronization state
     */
    public void setSyncState(int state) {
        if (state == SyncUtil.State.REST ||
                this.syncState == SyncUtil.State.REST) {
            setIntProperty(DB_SYNC_STATE, state);
            this.syncState = state;
        } else {
            throw new RuntimeException("invalid call to setSyncState:");
        }
    }
    
    /**
     * @return the transactioning status
     */
    public int getTransactioning() {
        return transactioning;
    }
    
    /**
     * Sets the transactioning status.
     * @param transactioning the new transactioning status
     */
    public void setTransactioning(int transactioning) {
        setIntProperty(DB_TRANSACTIONING, transactioning);
        this.transactioning = transactioning;
    }
    
    private void setIntProperty(String property, int value) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(DB_TABLENAME, values, ID_WHERE_SQL, whereArgs);
        db.close();
    }
    
    private void setLongProperty(String property, long value) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(DB_TABLENAME, values, ID_WHERE_SQL, whereArgs);
        db.close();
    }
    
    private void setStringProperty(String property, String value) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        setStringProperty(property, value, db);
        db.close();
    }
    
    private void setStringProperty(String property, String value,
            SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        int ra = db.update(DB_TABLENAME, values, ID_WHERE_SQL, whereArgs);
        Log.d("TP", "rows updated:" + ra);
        Log.d("TP", "values:" + values.toString());
    }
    
    static String getTableCreateSql() {
        return "CREATE TABLE " + DB_TABLENAME + "(" +
                       DB_TABLE_ID + " INTEGER PRIMARY KEY" +
                ", " + DB_DB_TABLE_NAME + " TEXT UNIQUE" +
                ", " + DB_DISPLAY_NAME + " TEXT NOT NULL" +
                ", " + DB_TABLE_TYPE + " INTEGER NOT NULL" +
                ", " + DB_COLUMN_ORDER + " TEXT NOT NULL" +
                ", " + DB_PRIME_COLUMNS + " TEXT NOT NULL" +
                ", " + DB_SORT_COLUMN + " TEXT" +
                ", " + DB_READ_SECURITY_TABLE_ID + " INTEGER NOT NULL" +
                ", " + DB_WRITE_SECURITY_TABLE_ID + " INTEGER NOT NULL" +
                ", " + DB_SYNC_MODIFICATION_NUMBER + " INTEGER NOT NULL" +
                ", " + DB_LAST_SYNC_TIME + " INTEGER NOT NULL" +
                ", " + DB_DETAIL_VIEW_FILE + " TEXT" +
                ", " + DB_SUM_DISPLAY_FORMAT + " TEXT" +
                ", " + DB_SYNC_STATE + " INTEGER NOT NULL" +
                ", " + DB_TRANSACTIONING + " INTEGER NOT NULL" +
                ")";
    }
}
