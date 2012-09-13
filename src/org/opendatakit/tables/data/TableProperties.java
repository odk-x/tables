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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendatakit.tables.Activity.util.SecurityUtil;
import org.opendatakit.tables.Activity.util.ShortcutUtil;
import org.opendatakit.tables.sync.SyncUtil;

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
    private static final String DB_SYNC_TAG = "syncTag";
    private static final String DB_LAST_SYNC_TIME = "lastSyncTime";
    private static final String DB_OV_VIEW_SETTINGS = "ovViewSettings";
    private static final String DB_CO_VIEW_SETTINGS = "coViewSettings";
    private static final String DB_DETAIL_VIEW_FILE = "detailViewFile";
    private static final String DB_SUM_DISPLAY_FORMAT = "summaryDisplayFormat";
    private static final String DB_SYNC_STATE = "syncState";
    private static final String DB_TRANSACTIONING = "transactioning";
    private static final String DB_IS_SYNCHED = "isSynched";
    // keys for JSON
    private static final String JSON_KEY_VERSION = "jVersion";
    private static final String JSON_KEY_TABLE_ID = "tableId";
    private static final String JSON_KEY_DB_TABLE_NAME = "dbTableName";
    private static final String JSON_KEY_DISPLAY_NAME = "displayName";
    private static final String JSON_KEY_TABLE_TYPE = "type";
    private static final String JSON_KEY_COLUMN_ORDER = "colOrder";
    private static final String JSON_KEY_COLUMNS = "columns";
    private static final String JSON_KEY_PRIME_COLUMNS = "primeCols";
    private static final String JSON_KEY_SORT_COLUMN = "sortCol";
    private static final String JSON_KEY_READ_SECURITY_TABLE_ID =
        "readAccessTid";
    private static final String JSON_KEY_WRITE_SECURITY_TABLE_ID =
        "writeAccessTid";
    private static final String JSON_KEY_OV_VIEW_SETTINGS = "ovViewSettings";
    private static final String JSON_KEY_CO_VIEW_SETTINGS = "coViewSettings";
    private static final String JSON_KEY_DETAIL_VIEW_FILE = "detailViewFile";
    private static final String JSON_KEY_SUM_DISPLAY_FORMAT =
        "summaryDisplayFormat";
    
    // the SQL where clause to use for selecting, updating, or deleting the row
    // for a given table
    private static final String ID_WHERE_SQL = DB_TABLE_ID + " = ?";
    // the SQL where clause to use for selecting by table type
    private static final String TYPE_WHERE_SQL = DB_TABLE_TYPE + " = ?";
    // the SQL where clause to use for selecting by sync state
    private static final String IS_SYNCHED_WHERE_SQL = DB_IS_SYNCHED + " = ?";
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
        DB_SYNC_TAG,
        DB_LAST_SYNC_TIME,
        DB_OV_VIEW_SETTINGS,
        DB_CO_VIEW_SETTINGS,
        DB_DETAIL_VIEW_FILE,
        DB_SUM_DISPLAY_FORMAT,
        DB_SYNC_STATE,
        DB_TRANSACTIONING, 
        DB_IS_SYNCHED,
    };
    // columns included in json properties
    private static final List<String> JSON_COLUMNS = Arrays.asList(new String[]{
      DB_TABLE_ID,
      DB_DB_TABLE_NAME,
      DB_DISPLAY_NAME,
      DB_TABLE_TYPE,
      DB_COLUMN_ORDER,
      DB_PRIME_COLUMNS,
      DB_SORT_COLUMN,
      DB_READ_SECURITY_TABLE_ID,
      DB_WRITE_SECURITY_TABLE_ID,
      DB_OV_VIEW_SETTINGS,
      DB_CO_VIEW_SETTINGS,
      DB_DETAIL_VIEW_FILE,
      DB_SUM_DISPLAY_FORMAT,
    });
    
    public class TableType {
        public static final int DATA = 0;
        public static final int SECURITY = 1;
        public static final int SHORTCUT = 2;
        private TableType() {}
    }
    
    public class ViewType {
        public static final int TABLE = 0;
        public static final int LIST = 1;
        public static final int LINE_GRAPH = 2;
        public static final int COUNT = 3;
        private ViewType() {}
    }
    
    private final DbHelper dbh;
    private final String[] whereArgs;
    
    private final String tableId;
    private String dbTableName;
    private String displayName;
    private int tableType;
    private ColumnProperties[] columns;
    private String[] columnOrder;
    private String[] primeColumns;
    private String sortColumn;
    private String readSecurityTableId;
    private String writeSecurityTableId;
    private String syncTag;
    private String lastSyncTime;
    private TableViewSettings overviewViewSettings;
    private TableViewSettings collectionViewSettings;
    private String detailViewFilename;
    private String sumDisplayFormat;
    private int syncState;
    private boolean transactioning;
    private boolean isSynched;
    
    private TableProperties(DbHelper dbh, String tableId, String dbTableName,
            String displayName, int tableType, String[] columnOrder,
            String[] primeColumns, String sortColumn,
            String readSecurityTableId, String writeSecurityTableId,
            String syncTag, String lastSyncTime, String ovViewSettingsDbString,
            String coViewSettingsDbString, String detailViewFilename,
            String sumDisplayFormat, int syncState, boolean transactioning, boolean isSynched) {
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
        this.syncTag = syncTag;
        this.lastSyncTime = lastSyncTime;
        this.overviewViewSettings = TableViewSettings.newOverviewTVS(this,
                ovViewSettingsDbString);
        this.collectionViewSettings = TableViewSettings.newCollectionTVS(this,
                coViewSettingsDbString);
        this.detailViewFilename = detailViewFilename;
        this.sumDisplayFormat = sumDisplayFormat;
        this.syncState = syncState;
        this.transactioning = transactioning;
        this.isSynched = isSynched;
    }
    
    public static TableProperties getTablePropertiesForTable(DbHelper dbh,
            String tableId) {
        TableProperties[] res = queryForTableProperties(dbh, ID_WHERE_SQL,
                new String[] {tableId}, true);
        return res[0];
    }
    
    public static TableProperties[] getTablePropertiesForAll(DbHelper dbh) {
        return queryForTableProperties(dbh, null, null);
    }
    
    public static TableProperties[] getTablePropertiesForSynchronizedTables(DbHelper dbh) {
        return queryForTableProperties(dbh, IS_SYNCHED_WHERE_SQL,
            new String[] { String.valueOf(SyncUtil.boolToInt(true)) }, true);
    }
    
    public static TableProperties[] getTablePropertiesForDataTables(
            DbHelper dbh) {
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
        return queryForTableProperties(dbh, where, whereArgs, false);
    }
    
    private static TableProperties[] queryForTableProperties(DbHelper dbh,
            String where, String[] whereArgs, boolean includeDeleting) {
        if (!includeDeleting) {
            where = (where == null) ?
                        (DB_SYNC_STATE + " != " + SyncUtil.State.DELETING) :
                        (where + " AND " + DB_SYNC_STATE + " != " +
                                SyncUtil.State.DELETING);
        }
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
        int syncTagIndex = c.getColumnIndexOrThrow(DB_SYNC_TAG);
        int lastSyncTimeIndex = c.getColumnIndexOrThrow(DB_LAST_SYNC_TIME);
        int ovViewSettingsIndex = c.getColumnIndexOrThrow(DB_OV_VIEW_SETTINGS);
        int coViewSettingsIndex = c.getColumnIndexOrThrow(DB_CO_VIEW_SETTINGS);
        int detailViewFileIndex = c.getColumnIndexOrThrow(DB_DETAIL_VIEW_FILE);
        int sumDisplayFormatIndex = c.getColumnIndexOrThrow(
                DB_SUM_DISPLAY_FORMAT);
        int syncStateIndex = c.getColumnIndexOrThrow(DB_SYNC_STATE);
        int transactioningIndex = c.getColumnIndexOrThrow(DB_TRANSACTIONING);
        int isSynchedIndex = c.getColumnIndexOrThrow(DB_IS_SYNCHED);
        
        int i = 0;
        c.moveToFirst();
        while (i < tps.length) {
            String columnOrderValue = c.getString(columnOrderIndex);  // wtf, in db has value, here returned empty
            String[] columnOrder = (columnOrderValue.length() == 0) ?
                new String[] {} : columnOrderValue.split("/");
            String primeOrderValue = c.getString(primeColumnsIndex);
            String[] primeList = (primeOrderValue.length() == 0) ?
                new String[] {} : primeOrderValue.split("/");
            tps[i] = new TableProperties(dbh, c.getString(tableIdIndex),
                    c.getString(dbtnIndex), c.getString(displayNameIndex),
                    c.getInt(tableTypeIndex), columnOrder, primeList,
                    c.getString(sortColumnIndex), c.getString(rsTableId),
                    c.getString(wsTableId), c.getString(syncTagIndex),
                    c.getString(lastSyncTimeIndex),
                    c.getString(ovViewSettingsIndex),
                    c.getString(coViewSettingsIndex),
                    c.getString(detailViewFileIndex),
                    c.getString(sumDisplayFormatIndex),
                    c.getInt(syncStateIndex),
                    SyncUtil.intToBool(c.getInt(transactioningIndex)),
                    SyncUtil.intToBool(c.getInt(isSynchedIndex)));
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
        String id = UUID.randomUUID().toString();
        TableProperties tp = addTable(dbh, dbTableName, displayName, tableType,
                id);
        if (tableType == TableType.SHORTCUT) {
            tp.addColumn("label", ShortcutUtil.LABEL_COLUMN_NAME);
            tp.addColumn("input", ShortcutUtil.INPUT_COLUMN_NAME);
            tp.addColumn("output", ShortcutUtil.OUTPUT_COLUMN_NAME);
        } else if (tableType == TableType.SECURITY) {
            tp.addColumn("user", SecurityUtil.USER_COLUMN_NAME);
            tp.addColumn("phone_number", SecurityUtil.PHONENUM_COLUMN_NAME);
            tp.addColumn("password", SecurityUtil.PASSWORD_COLUMN_NAME);
        }
        return tp;
    }
    
    public static TableProperties addTable(DbHelper dbh, String dbTableName,
            String displayName, int tableType, String id) {
        ContentValues values = new ContentValues();
        values.put(DB_TABLE_ID, id);
        values.put(DB_DB_TABLE_NAME, dbTableName);
        values.put(DB_DISPLAY_NAME, displayName);
        values.put(DB_TABLE_TYPE, tableType);
        values.put(DB_COLUMN_ORDER, "");
        values.put(DB_PRIME_COLUMNS, "");
        values.putNull(DB_SORT_COLUMN);
        values.putNull(DB_READ_SECURITY_TABLE_ID);
        values.putNull(DB_WRITE_SECURITY_TABLE_ID);
        values.putNull(DB_SYNC_TAG);
        values.put(DB_LAST_SYNC_TIME, -1);
        values.putNull(DB_OV_VIEW_SETTINGS);
        values.putNull(DB_CO_VIEW_SETTINGS);
        values.putNull(DB_DETAIL_VIEW_FILE);
        values.putNull(DB_SUM_DISPLAY_FORMAT);
        values.put(DB_SYNC_STATE, SyncUtil.State.INSERTING);
        values.put(DB_TRANSACTIONING, SyncUtil.boolToInt(false));
        values.put(DB_IS_SYNCHED, SyncUtil.boolToInt(false));
        TableProperties tp = new TableProperties(dbh, id, dbTableName,
                displayName, tableType, new String[0], new String[0], null,
                null, null, null, null, null, null, null, null,
                SyncUtil.State.INSERTING, false, false);
        tp.getColumns(); // ensuring columns are already initialized
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        long result = db.insert(DB_TABLENAME, null, values);
        Log.d("TP", "row id=" + result);
        if (result < 0) {
            throw new RuntimeException(
                    "Failed to insert table properties row.");
        }
        DbTable.createDbTable(db, tp);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        return tp;
    }
    
    public void deleteTable() {
      if (isSynched && (syncState == SyncUtil.State.REST || syncState == SyncUtil.State.UPDATING))
        setSyncState(SyncUtil.State.DELETING);
      else if (!isSynched || syncState == SyncUtil.State.INSERTING)
        deleteTableActual();
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
    
    public String getTableId() {
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
            columns = ColumnProperties.getColumnPropertiesForTable(dbh,
                    tableId);
            orderColumns();
        }
        return columns;
    }
    
    private void orderColumns() {
        ColumnProperties[] newColumns = new ColumnProperties[columns.length];
        for (int i = 0; i < columnOrder.length; i++) {
            for (int j = 0; j < columns.length; j++) {
                if (columns[j].getColumnDbName().equals(columnOrder[i])) {
                    newColumns[i] = columns[j];
                    break;
                }
            }
        }
        columns = newColumns;
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
            if ((cdn != null) && (cdn.equalsIgnoreCase(displayName))) {
                return cp.getColumnDbName();
            }
        }
        return null;
    }
    
    public String getColumnByAbbreviation(String abbreviation) {
        ColumnProperties[] cps = getColumns();
        for (ColumnProperties cp : cps) {
            String ca = cp.getAbbreviation();
            if ((ca != null) && (ca.equalsIgnoreCase(abbreviation))) {
                return cp.getColumnDbName();
            }
        }
        return null;
    }
    
    public ColumnProperties getColumnByUserLabel(String name) {
        ColumnProperties[] cps = getColumns();
        for (ColumnProperties cp : cps) {
            String cdn = cp.getDisplayName();
            if (cdn.equalsIgnoreCase(name)) {
                return cp;
            }
        }
        for (ColumnProperties cp : cps) {
            String ca = cp.getAbbreviation();
            if ((ca != null) && ca.equalsIgnoreCase(name)) {
                return cp;
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
        String csv = DbTable.DB_CSV_COLUMN_LIST;
        for (int i = 0; i < columns.length; i++) {
            if (i == colIndex) {
                continue;
            }
            csv += ", " + columns[i].getColumnDbName();
        }
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
        ColumnProperties colToDelete = columns[colIndex];
        columns = newColumns;
        String[] newColumnOrder = new String[columns.length];
        index = 0;
        for (String col : columnOrder) {
            if (col.equals(columnDbName)) {
                continue;
            }
            newColumnOrder[index] = col;
            index++;
        }
        setColumnOrder(newColumnOrder);
        // deleting the column
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        colToDelete.deleteColumn(db);
        reformTable(db, columnOrder);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }
    
    /**
     * Reforms the table.
     */
    public void reformTable(SQLiteDatabase db, String[] existingColumns) {
        StringBuilder csvBuilder =
            new StringBuilder(DbTable.DB_CSV_COLUMN_LIST);
        for (String col : existingColumns) {
            csvBuilder.append(", " + col);
        }
        String csv = csvBuilder.toString();
        db.execSQL("CREATE TEMPORARY TABLE backup_(" + csv + ")");
        db.execSQL("INSERT INTO backup_ SELECT " + csv + " FROM " +
                dbTableName);
        db.execSQL("DROP TABLE " + dbTableName);
        DbTable.createDbTable(db, this);
        db.execSQL("INSERT INTO " + dbTableName + " SELECT " + csv +
                " FROM backup_");
        db.execSQL("DROP TABLE backup_");
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
        if (str.length() > 0) {
            str = str.substring(0, str.length() - 1);
        }
        setStringProperty(DB_PRIME_COLUMNS, str);
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
        if ((sortColumn != null) && (sortColumn.length() == 0)) {
            sortColumn = null;
        }
        setStringProperty(DB_SORT_COLUMN, sortColumn);
        this.sortColumn = sortColumn;
    }
    
    /**
     * @return the ID of the read security table, or null if there is none
     */
    public String getReadSecurityTableId() {
        return readSecurityTableId;
    }
    
    /**
     * Sets the table's read security table.
     * @param tableId the ID of the new read security table (or null to set no
     * read security table)
     */
    public void setReadSecurityTableId(String tableId) {
        setStringProperty(DB_READ_SECURITY_TABLE_ID, tableId);
        this.readSecurityTableId = tableId;
    }
    
    /**
     * @return the ID of the write security table, or null if there is none
     */
    public String getWriteSecurityTableId() {
        return writeSecurityTableId;
    }
    
    /**
     * Sets the table's write security table.
     * @param tableId the ID of the new write security table (or null to set no
     * write security table)
     */
    public void setWriteSecurityTableId(String tableId) {
        setStringProperty(DB_WRITE_SECURITY_TABLE_ID, tableId);
        this.writeSecurityTableId = tableId;
    }
    
    /**
     * @return the sync tag (or null if the table has never been synchronized)
     */
    public String getSyncTag() {
        return syncTag;
    }
    
    /**
     * Sets the table's sync tag.
     * @param syncTag the new sync tag
     */
    public void setSyncTag(String syncTag) {
        setStringProperty(DB_SYNC_TAG, syncTag);
        this.syncTag = syncTag;
    }
    
    /**
     * @return the last synchronization time (in the format of
     * {@link DataUtil#getNowInDbFormat()}.
     */
    public String getLastSyncTime() {
        return lastSyncTime;
    }
    
    /**
     * Sets the table's last synchronization time.
     * @param time the new synchronization time (in the format of
     * {@link DataUtil#getNowInDbFormat()}).
     */
    public void setLastSyncTime(String time) {
        setStringProperty(DB_LAST_SYNC_TIME, time);
        this.lastSyncTime = time;
    }
    
    /**
     * @return the overview view settings
     */
    public TableViewSettings getOverviewViewSettings() {
        return overviewViewSettings;
    }
    
    /**
     * Sets the overview view settings.
     * @param dbString the string to put in the database
     */
    void setOverviewViewSettings(String dbString) {
        setStringProperty(DB_OV_VIEW_SETTINGS, dbString);
    }
    
    /**
     * @return the collection view settings
     */
    public TableViewSettings getCollectionViewSettings() {
        return collectionViewSettings;
    }
    
    /**
     * Sets the collection view settings.
     * @param dbString the string to put in the database
     */
    void setCollectionViewSettings(String dbString) {
        setStringProperty(DB_CO_VIEW_SETTINGS, dbString);
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
        }
    }
    
    /**
     * @return the transactioning status
     */
    public boolean isTransactioning() {
        return transactioning;
    }
    
    /**
     * Sets the transactioning status.
     * @param transactioning the new transactioning status
     */
    public void setTransactioning(boolean transactioning) {
        setIntProperty(DB_TRANSACTIONING, SyncUtil.boolToInt(transactioning));
        this.transactioning = transactioning;
    }
    
    public boolean isSynchronized() {
      return isSynched;
    }
    
    public void setSynchronized(boolean isSynchronized) {
      setIntProperty(DB_IS_SYNCHED, SyncUtil.boolToInt(isSynchronized));
      this.isSynched = isSynchronized;
    }
    
    public String toJson() {
        getColumns(); // ensuring columns is initialized
        JSONArray colOrder = new JSONArray();
        JSONArray cols = new JSONArray();
        for (ColumnProperties cp : columns) {
            colOrder.put(cp.getColumnDbName());
            cols.put(cp.toJsonObject());
        }
        JSONArray primes = new JSONArray();
        for (String prime : primeColumns) {
            primes.put(prime);
        }
        JSONObject jo = new JSONObject();
        try {
            jo.put(JSON_KEY_VERSION, 1);
            jo.put(JSON_KEY_TABLE_ID, tableId);
            jo.put(JSON_KEY_DB_TABLE_NAME, dbTableName);
            jo.put(JSON_KEY_DISPLAY_NAME, displayName);
            jo.put(JSON_KEY_TABLE_TYPE, tableType);
            jo.put(JSON_KEY_COLUMN_ORDER, colOrder);
            jo.put(JSON_KEY_COLUMNS, cols);
            jo.put(JSON_KEY_PRIME_COLUMNS, primes);
            jo.put(JSON_KEY_SORT_COLUMN, sortColumn);
            jo.put(JSON_KEY_READ_SECURITY_TABLE_ID, readSecurityTableId);
            jo.put(JSON_KEY_WRITE_SECURITY_TABLE_ID, writeSecurityTableId);
            // TODO
            jo.put(JSON_KEY_OV_VIEW_SETTINGS,
                    overviewViewSettings.toJsonObject().toString());
            // TODO
            jo.put(JSON_KEY_CO_VIEW_SETTINGS,
                    collectionViewSettings.toJsonObject().toString());
            jo.put(JSON_KEY_DETAIL_VIEW_FILE, detailViewFilename);
            jo.put(JSON_KEY_SUM_DISPLAY_FORMAT, sumDisplayFormat);
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
        Log.d("TP", "json: " + jo.toString());
        return jo.toString();
    }
    
    public void setFromJson(String json) {
        getColumns();
        try {
            JSONObject jo = new JSONObject(json);
            JSONArray colOrderJo = jo.getJSONArray(JSON_KEY_COLUMN_ORDER);
            String[] colOrder = new String[colOrderJo.length()];
            for (int i = 0; i < colOrderJo.length(); i++) {
                colOrder[i] = colOrderJo.getString(i);
            }
            JSONArray primesJo = jo.getJSONArray(JSON_KEY_PRIME_COLUMNS);
            String[] primes = new String[primesJo.length()];
            for (int i = 0; i < primesJo.length(); i++) {
                primes[i] = primesJo.getString(i);
            }
            setDisplayName(jo.getString(JSON_KEY_DISPLAY_NAME));
            setTableType(jo.getInt(JSON_KEY_TABLE_TYPE));
            setPrimeColumns(primes);
            setSortColumn(jo.optString(JSON_KEY_SORT_COLUMN));
            setReadSecurityTableId(jo.optString(
                    JSON_KEY_READ_SECURITY_TABLE_ID));
            setWriteSecurityTableId(jo.optString(
                    JSON_KEY_WRITE_SECURITY_TABLE_ID));
            if (jo.has(JSON_KEY_OV_VIEW_SETTINGS)) {
                // TODO
            }
            if (jo.has(JSON_KEY_CO_VIEW_SETTINGS)) {
                // TODO
            }
            setDetailViewFilename(jo.optString(JSON_KEY_DETAIL_VIEW_FILE));
            setSummaryDisplayFormat(jo.optString(JSON_KEY_SUM_DISPLAY_FORMAT));
            Set<String> columnsToDelete = new HashSet<String>();
            for (String cdn : columnOrder) {
                columnsToDelete.add(cdn);
            }
            JSONArray colJArr = jo.getJSONArray(JSON_KEY_COLUMNS);
            for (int i = 0; i < colOrder.length; i++) {
                JSONObject colJo = colJArr.getJSONObject(i);
                ColumnProperties cp = getColumnByDbName(colOrder[i]);
                if (cp == null) {
                    cp = addColumn(colOrder[i], colOrder[i]);
                }
                cp.setFromJsonObject(colJo);
                columnsToDelete.remove(colOrder[i]);
            }
            for (String columnToDelete : columnsToDelete) {
                deleteColumn(columnToDelete);
            }
            setColumnOrder(colOrder);
            orderColumns();
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void setIntProperty(String property, int value) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(DB_TABLENAME, values, ID_WHERE_SQL, whereArgs);
        db.close();
        if (isSynched && syncState == SyncUtil.State.REST && JSON_COLUMNS.contains(property))
          setSyncState(SyncUtil.State.UPDATING);
    }
    
    private void setStringProperty(String property, String value) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        setStringProperty(property, value, db);
        db.close();
        if (isSynched && syncState == SyncUtil.State.REST && JSON_COLUMNS.contains(property))
          setSyncState(SyncUtil.State.UPDATING);
    }
     
    private void setStringProperty(String property, String value,
            SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        int ra = db.update(DB_TABLENAME, values, ID_WHERE_SQL, whereArgs);
        Log.d("TP", "rows updated:" + ra);
        // bug thrown right here, got message "values:colOrder=" and that is
        // what cleared the colOrder field in the db. happened when hitting
        // back arrow from columProperties activity
        Log.d("TP", "values:" + values.toString());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TableProperties)) {
            return false;
        }
        TableProperties other = (TableProperties) obj;
        return tableId.equals(other.tableId);
    }
    
    @Override
    public int hashCode() {
        return tableId.hashCode();
    }
    
    @Override
    public String toString() {
      return displayName;
    }
    
    static String getTableCreateSql() {
        return "CREATE TABLE " + DB_TABLENAME + "(" +
                       DB_TABLE_ID + " TEXT UNIQUE NOT NULL" +
                ", " + DB_DB_TABLE_NAME + " TEXT UNIQUE" +
                ", " + DB_DISPLAY_NAME + " TEXT NOT NULL" +
                ", " + DB_TABLE_TYPE + " INTEGER NOT NULL" +
                ", " + DB_COLUMN_ORDER + " TEXT NOT NULL" +
                ", " + DB_PRIME_COLUMNS + " TEXT NOT NULL" +
                ", " + DB_SORT_COLUMN + " TEXT" +
                ", " + DB_READ_SECURITY_TABLE_ID + " TEXT" +
                ", " + DB_WRITE_SECURITY_TABLE_ID + " TEXT" +
                ", " + DB_SYNC_TAG + " TEXT" +
                ", " + DB_LAST_SYNC_TIME + " TEXT NOT NULL" +
                ", " + DB_OV_VIEW_SETTINGS + " TEXT" +
                ", " + DB_CO_VIEW_SETTINGS + " TEXT" +
                ", " + DB_DETAIL_VIEW_FILE + " TEXT" +
                ", " + DB_SUM_DISPLAY_FORMAT + " TEXT" +
                ", " + DB_SYNC_STATE + " INTEGER NOT NULL" +
                ", " + DB_TRANSACTIONING + " INTEGER NOT NULL" +
                ", " + DB_IS_SYNCHED + " INTEGER NOT NULL" +
                ")";
    }
}
