package yoonsung.odk.spreadsheet.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and managing column properties.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 */
public class ColumnProperties {
    
    // the name of the column properties table in the database
    private static final String DB_TABLENAME = "colProps";
    // names of columns in the column properties table
    private static final String DB_COLUMN_ID = "columnId";
    private static final String DB_TABLE_ID = "tableId";
    private static final String DB_DB_COLUMN_NAME = "dbColumnName";
    private static final String DB_DISPLAY_NAME = "displayName";
    private static final String DB_ABBREVIATION = "abrev";
    private static final String DB_COLUMN_TYPE = "colType";
    private static final String DB_FOOTER_MODE = "footerMode";
    private static final String DB_SMS_IN = "smsIn";
    private static final String DB_SMS_OUT = "smsOut";
    private static final String DB_MULTIPLE_CHOICE_OPTIONS = "mcOptions";
    
    // the SQL where clause to use for selecting, updating, or deleting the row
    // for a given column
    private static final String WHERE_SQL = DB_TABLE_ID + " = ? and " +
            DB_DB_COLUMN_NAME + " = ?";
    // the columns to be selected when initializing ColumnProperties
    private static final String[] INIT_COLUMNS = {
        DB_DB_COLUMN_NAME,
        DB_DISPLAY_NAME,
        DB_ABBREVIATION,
        DB_COLUMN_TYPE,
        DB_FOOTER_MODE,
        DB_SMS_IN,
        DB_SMS_OUT,
        DB_MULTIPLE_CHOICE_OPTIONS
    };
    
    public class ColumnType {
        public static final int NONE = 0;
        public static final int TEXT = 1;
        public static final int NUMBER = 2;
        public static final int DATE = 3;
        public static final int DATE_RANGE = 4;
        public static final int PHONE_NUMBER = 5;
        public static final int FILE = 6;
        public static final int COLLECT_FORM = 7;
        public static final int MC_OPTIONS = 8;
        private ColumnType() {}
    }
    
    public class FooterMode {
        public static final int NONE = 0;
        public static final int COUNT = 1;
        public static final int MINIMUM = 2;
        public static final int MAXIMUM = 3;
        public static final int MEAN = 4;
        private FooterMode() {}
    }
    
    private final DbHelper dbh;
    private final String[] whereArgs;
    
    private final long tableId;
    private final String columnDbName;
    private String displayName;
    private String abbreviation;
    private int columnType;
    private int footerMode;
    private boolean smsIn;
    private boolean smsOut;
    private String[] multipleChoiceOptions;
    
    private ColumnProperties(DbHelper dbh, long tableId, String columnDbName,
            String displayName, String abbreviation, int columnType,
            int footerMode, boolean smsIn, boolean smsOut,
            String[] multipleChoiceOptions) {
        this.dbh = dbh;
        whereArgs = new String[] {String.valueOf(tableId), columnDbName};
        this.tableId = tableId;
        this.columnDbName = columnDbName;
        this.displayName = displayName;
        this.abbreviation = abbreviation;
        this.columnType = columnType;
        this.footerMode = footerMode;
        this.smsIn = smsIn;
        this.smsOut = smsOut;
        this.multipleChoiceOptions = multipleChoiceOptions;
    }
    
    static ColumnProperties getColumnProperties(DbHelper dbh, long tableId,
            String dbColumnName) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.query(DB_TABLENAME, INIT_COLUMNS, WHERE_SQL,
                new String[] {String.valueOf(tableId), dbColumnName}, null,
                null, null);
        int dbcnIndex = c.getColumnIndexOrThrow(DB_DB_COLUMN_NAME);
        int displayNameIndex = c.getColumnIndexOrThrow(DB_DISPLAY_NAME);
        int abrvIndex = c.getColumnIndexOrThrow(DB_ABBREVIATION);
        int colTypeIndex = c.getColumnIndexOrThrow(DB_COLUMN_TYPE);
        int footerModeIndex = c.getColumnIndexOrThrow(DB_FOOTER_MODE);
        int smsInIndex = c.getColumnIndexOrThrow(DB_SMS_IN);
        int smsOutIndex = c.getColumnIndexOrThrow(DB_SMS_OUT);
        int mcOptionsIndex = c.getColumnIndexOrThrow(
                DB_MULTIPLE_CHOICE_OPTIONS);
        String mcOptionsValue = c.isNull(mcOptionsIndex) ?
                null : c.getString(mcOptionsIndex);
        String[] mcOptionsList = decodeMultipleChoiceOptions(
                mcOptionsValue);
        c.moveToFirst();
        ColumnProperties cp = new ColumnProperties(dbh, tableId,
                c.getString(dbcnIndex), c.getString(displayNameIndex),
                c.getString(abrvIndex), c.getInt(colTypeIndex),
                c.getInt(footerModeIndex), c.getInt(smsInIndex) == 1,
                c.getInt(smsOutIndex) == 1, mcOptionsList);
        c.close();
        db.close();
        return cp;
    }
    
    static ColumnProperties[] getColumnPropertiesForTable(DbHelper dbh,
            long tableId) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.query(DB_TABLENAME, INIT_COLUMNS, DB_TABLE_ID + " = ?",
                new String[] {String.valueOf(tableId)}, null, null, null);
        ColumnProperties[] cps = new ColumnProperties[c.getCount()];
        int dbcnIndex = c.getColumnIndexOrThrow(DB_DB_COLUMN_NAME);
        int displayNameIndex = c.getColumnIndexOrThrow(DB_DISPLAY_NAME);
        int abrvIndex = c.getColumnIndexOrThrow(DB_ABBREVIATION);
        int colTypeIndex = c.getColumnIndexOrThrow(DB_COLUMN_TYPE);
        int footerModeIndex = c.getColumnIndexOrThrow(DB_FOOTER_MODE);
        int smsInIndex = c.getColumnIndexOrThrow(DB_SMS_IN);
        int smsOutIndex = c.getColumnIndexOrThrow(DB_SMS_OUT);
        int mcOptionsIndex = c.getColumnIndexOrThrow(
                DB_MULTIPLE_CHOICE_OPTIONS);
        int i = 0;
        c.moveToFirst();
        while (i < cps.length) {
            String mcOptionsValue = c.isNull(mcOptionsIndex) ?
                    null : c.getString(mcOptionsIndex);
            String[] mcOptionsList = decodeMultipleChoiceOptions(
                    mcOptionsValue);
            cps[i] = new ColumnProperties(dbh, tableId, c.getString(dbcnIndex),
                    c.getString(displayNameIndex), c.getString(abrvIndex),
                    c.getInt(colTypeIndex), c.getInt(footerModeIndex),
                    c.getInt(smsInIndex) == 1, c.getInt(smsOutIndex) == 1,
                    mcOptionsList);
            i++;
            c.moveToNext();
        }
        c.close();
        db.close();
        return cps;
    }
    
    static ColumnProperties addColumn(DbHelper dbh, SQLiteDatabase db,
            long tableId, String columnDbName, String columnDisplayName) {
        ContentValues values = new ContentValues();
        values.put(DB_TABLE_ID, tableId);
        values.put(DB_DB_COLUMN_NAME, columnDbName);
        values.put(DB_DISPLAY_NAME, columnDisplayName);
        values.putNull(DB_ABBREVIATION);
        values.put(DB_COLUMN_TYPE, ColumnType.NONE);
        values.put(DB_FOOTER_MODE, FooterMode.NONE);
        values.put(DB_SMS_IN, 1);
        values.put(DB_SMS_OUT, 1);
        values.putNull(DB_MULTIPLE_CHOICE_OPTIONS);
        db.insert(DB_TABLENAME, null, values);
        return new ColumnProperties(dbh, tableId, columnDbName,
                columnDisplayName, null, ColumnType.NONE, FooterMode.NONE,
                true, true, new String[0]);
    }
    
    void deleteColumn(SQLiteDatabase db) {
        int count = db.delete(DB_TABLENAME, WHERE_SQL,
                new String[] {String.valueOf(tableId), columnDbName});
        if (count != 1) {
            Log.e(ColumnProperties.class.getName(),
                    "deleteColumn() deleted " + count + " rows");
        }
    }
    
    /**
     * @return the column's name in the database
     */
    public String getColumnDbName() {
        return columnDbName;
    }
    
    /**
     * @return the column's display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Sets the column's display name.
     * @param displayName the new display name
     */
    public void setDisplayName(String displayName) {
        setStringProperty(DB_DISPLAY_NAME, displayName);
        this.displayName = displayName;
    }
    
    /**
     * @return the column's abbreviation (or null for no abbreviation)
     */
    public String getAbbreviation() {
        return abbreviation;
    }
    
    /**
     * Sets the column's abbreviation.
     * @param abbreviation the new abbreviation (or null for no abbreviation)
     */
    public void setAbbreviation(String abbreviation) {
        setStringProperty(DB_ABBREVIATION, abbreviation);
        this.abbreviation = abbreviation;
    }
    
    /**
     * @return the column's type
     */
    public int getColumnType() {
        return columnType;
    }
    
    /**
     * Sets the column's type.
     * @param columnType the new type
     */
    public void setColumnType(int columnType) {
        setIntProperty(DB_COLUMN_TYPE, columnType);
        this.columnType = columnType;
    }
    
    /**
     * @return the column's footer mode
     */
    public int getFooterMode() {
        return footerMode;
    }
    
    /**
     * Sets the column's footer mode.
     * @param footerMode the new footer mode
     */
    public void setFooterMode(int footerMode) {
        setIntProperty(DB_FOOTER_MODE, footerMode);
        this.footerMode = footerMode;
    }
    
    /**
     * @return the SMS-in setting
     */
    public boolean getSmsIn() {
        return smsIn;
    }
    
    /**
     * Sets the SMS-in setting.
     * @param setting the new SMS-in setting
     */
    public void setSmsIn(boolean setting) {
        setIntProperty(DB_SMS_IN, setting ? 1 : 0);
        this.smsIn = setting;
    }
    
    /**
     * @return the SMS-out setting
     */
    public boolean getSmsOut() {
        return smsOut;
    }
    
    /**
     * Sets the SMS-out setting.
     * @param setting the new SMS-out setting
     */
    public void setSmsOut(boolean setting) {
        setIntProperty(DB_SMS_OUT, setting ? 1 : 0);
        this.smsOut = setting;
    }
    
    /**
     * @return an array of the multiple-choice options
     */
    public String[] getMultipleChoiceOptions() {
        return multipleChoiceOptions;
    }
    
    /**
     * Sets the multiple-choice options.
     * @param options the array of options
     */
    public void setMultipleChoiceOptions(String[] options) {
        String encoding = encodeMultipleChoiceOptions(options);
        setStringProperty(DB_MULTIPLE_CHOICE_OPTIONS, encoding);
        multipleChoiceOptions = options;
    }
    
    private static String encodeMultipleChoiceOptions(String[] options) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%02d", options.length));
        for (String option : options) {
            builder.append(String.format("%02d", option.length()));
        }
        for (String option : options) {
            builder.append(option);
        }
        return builder.toString();
    }
    
    private static String[] decodeMultipleChoiceOptions(String encoding) {
        if ((encoding == null) || (encoding.length() == 0)) {
            return new String[0];
        }
        String countString = encoding.substring(0, 2);
        int count = Integer.valueOf(countString);
        String[] options = new String[count];
        int index = (count + 1) * 2;
        for (int i = 0; i < count; i++) {
            int ls = (i + 1) * 2;
            String lengthString = encoding.substring(ls, ls + 2);
            int length = Integer.valueOf(lengthString);
            options[i] = encoding.substring(index, index + length);
            index += length;
        }
        return options;
    }
    
    private void setIntProperty(String property, int value) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        SQLiteDatabase db = dbh.getWritableDatabase();
        int count = db.update(DB_TABLENAME, values, WHERE_SQL, whereArgs);
        if (count != 1000) {
            Log.e(ColumnProperties.class.getName(),
                    "setting " + property + " updated " + count + " rows");
        }
        db.close();
    }
    
    private void setStringProperty(String property, String value) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        SQLiteDatabase db = dbh.getWritableDatabase();
        int count = db.update(DB_TABLENAME, values, WHERE_SQL, whereArgs);
        if (count != 1) {
            Log.e(ColumnProperties.class.getName(),
                    "setting " + property + " updated " + count + " rows");
        }
        db.close();
    }
    
    static String getTableCreateSql() {
        return "CREATE TABLE " + DB_TABLENAME + "(" +
                       DB_COLUMN_ID + " INTEGER PRIMARY KEY" +
                ", " + DB_TABLE_ID + " INTEGER NOT NULL" +
                ", " + DB_DB_COLUMN_NAME + " TEXT NOT NULL" +
                ", " + DB_DISPLAY_NAME + " TEXT NOT NULL" +
                ", " + DB_ABBREVIATION + " TEXT" +
                ", " + DB_COLUMN_TYPE + " TEXT NOT NULL" +
                ", " + DB_FOOTER_MODE + " TEXT NOT NULL" +
                ", " + DB_SMS_IN + " INTEGER NOT NULL" +
                ", " + DB_SMS_OUT + " INTEGER NOT NULL" +
                ", " + DB_MULTIPLE_CHOICE_OPTIONS + " TEXT" +
                ")";
    }
}
