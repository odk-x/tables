package yoonsung.odk.spreadsheet.DataStructure;

import java.util.ArrayList;
import java.util.List;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Display preferences for a table.
 */
public class DisplayPrefs {
    
    private final Context context;
    private final DisplayPrefsDBManager dbm;
    private final String tableId;
    
    public DisplayPrefs(Context context, String tableId) {
        this.context = context;
        this.tableId = tableId;
        dbm = new DisplayPrefsDBManager(context);
    }
    
    public List<ColColorRule> getColorRulesForCol(String colName) {
        List<ColColorRule> ruleList = new ArrayList<ColColorRule>();
        // querying the database
        String[] cols = {"id", "comp", "val", "foreground", "background"};
        String selection = "tableid = ? AND colname = ?";
        String[] selectionArgs = {String.valueOf(tableId), colName};
        SQLiteDatabase db = dbm.getReadableDatabase();
        Cursor cs = db.query("colors", cols, selection, selectionArgs, null,
                null, null, null);
        int idIndex = cs.getColumnIndexOrThrow("id");
        int compIndex = cs.getColumnIndexOrThrow("comp");
        int valIndex = cs.getColumnIndex("val");
        int foregroundColorIndex = cs.getColumnIndex("foreground");
        int backgroundColorIndex = cs.getColumnIndex("background");
        // adding rules
        boolean done = !cs.moveToFirst();
        while(!done) {
            int id = cs.getInt(idIndex);
            char compType = cs.getString(compIndex).charAt(0);
            String val = cs.getString(valIndex);
            int foregroundColor = cs.getInt(foregroundColorIndex);
            int backgroundColor = cs.getInt(backgroundColorIndex);
            ruleList.add(new ColColorRule(id, compType, val, foregroundColor,
                    backgroundColor));
            done = !cs.moveToNext();
        }
        // closing cursor and database and returning
        cs.close();
        db.close();
        return ruleList;
        
    }
    
    public void addRule(String colName, char compType, String val,
            int foregroundColor, int backgroundColor) {
        SQLiteDatabase db = dbm.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tableid", tableId);
        values.put("colname", colName);
        values.put("comp", compType + "");
        values.put("val", val);
        values.put("foreground", foregroundColor);
        values.put("background", backgroundColor);
        db.insertOrThrow("colors", null, values);
        db.close();
    }
    
    public void updateRule(ColColorRule rule) {
        SQLiteDatabase db = dbm.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("comp", rule.compType + "");
        values.put("val", rule.val);
        values.put("foreground", rule.foreground);
        values.put("background", rule.background);
        String whereClause = "id = ?";
        String[] whereArgs = {rule.id + ""};
        db.update("colors", values, whereClause, whereArgs);
        db.close();
    }
    
    public void deleteRule(ColColorRule rule) {
        SQLiteDatabase db = dbm.getWritableDatabase();
        String whereClause = "id = ?";
        String[] whereArgs = {rule.id + ""};
        db.delete("colors", whereClause, whereArgs);
        db.close();
    }
    
    public ColumnColorRuler getColColorRuler(String colName) {
        // querying the database
        String[] cols = {"comp", "val", "foreground", "background"};
        String selection = "tableid = ? AND colname = ?";
        String[] selectionArgs = {String.valueOf(tableId), colName};
        SQLiteDatabase db = dbm.getReadableDatabase();
        Cursor cs = db.query("colors", cols, selection, selectionArgs, null,
                null, null, null);
        int compIndex = cs.getColumnIndexOrThrow("comp");
        int valIndex = cs.getColumnIndex("val");
        int foregroundIndex = cs.getColumnIndex("foreground");
        int backgroundIndex = cs.getColumnIndex("background");
        // initializing the ccr
        TableProperties tp = TableProperties.getTablePropertiesForTable(DbHelper.getDbHelper(context), tableId);
        ColumnProperties cp = tp.getColumnByDbName(tp.getColumnByDisplayName(colName));
        ColumnColorRuler ccr = new ColumnColorRuler(cp.getColumnType());
        // adding rules
        boolean done = !cs.moveToFirst();
        while(!done) {
            String compStr = cs.getString(compIndex).trim();
            if(compStr.equals("")) {
                done = !cs.moveToNext();
                continue;
            }
            char compType = compStr.charAt(0);
            String val = cs.getString(valIndex).trim();
            int foreground = cs.getInt(foregroundIndex);
            int background = cs.getInt(backgroundIndex);
            ccr.addRule(compType, val, foreground, background);
            done = !cs.moveToNext();
        }
        // closing cursor and database and returning
        cs.close();
        db.close();
        return ccr;
    }
    
    public class ColumnColorRuler {
        
        private int colType;
        private final List<Character> ruleComps;
        private final List<String> ruleVals;
        private final List<Integer> foregroundColors;
        private final List<Integer> backgroundColors;
        
        private ColumnColorRuler(int colType) {
            this.colType = colType;
            ruleComps = new ArrayList<Character>();
            ruleVals = new ArrayList<String>();
            foregroundColors = new ArrayList<Integer>();
            backgroundColors = new ArrayList<Integer>();
        }
        
        private void addRule(char compType, String val, int foreground,
                int background) {
            ruleComps.add(compType);
            ruleVals.add(val);
            foregroundColors.add(foreground);
            backgroundColors.add(background);
        }
        
        public int getRuleCount() {
            return ruleComps.size();
        }
        
        public int getForegroundColor(String val, int defVal) {
            for(int i=0; i<ruleComps.size(); i++) {
                if(checkMatch(val, i)) {
                    return foregroundColors.get(i);
                }
            }
            return defVal;
        }
        
        public int getBackgroundColor(String val, int defVal) {
            for(int i=0; i<ruleComps.size(); i++) {
                if(checkMatch(val, i)) {
                    return backgroundColors.get(i);
                }
            }
            return defVal;
        }
        
        private boolean checkMatch(String val, int index) {
            int compVal;
            String ruleVal = ruleVals.get(index);
            if(colType == ColumnProperties.ColumnType.NUMBER) {
                double doubleValue = Double.parseDouble(val);
                double doubleRule = Double.parseDouble(ruleVal);
                Log.d("DP", "doubleValue:" + doubleValue);
                Log.d("DP", "doubleRule:" + doubleRule);
                compVal = (new Double(val)).compareTo(new Double(ruleVal));
            } else {
                compVal = val.compareTo(ruleVal);
            }
            Log.d("DP", "ruleVal:" + ruleVal);
            Log.d("DP", "val:" + val);
            Log.d("DP", "compVal:" + compVal);
            switch(ruleComps.get(index)) {
            case '=':
                return (compVal == 0);
            case '<':
                return (compVal < 0);
            case '>':
                return (compVal > 0);
            default:
                return false;
            }
        }
        
    }
    
    public class ColColorRule {
        public int id;
        public char compType;
        public String val;
        public int foreground;
        public int background;
        public ColColorRule(int id, char compType, String val, int foreground,
                int background) {
            this.id = id;
            this.compType = compType;
            this.val = val;
            this.foreground = foreground;
            this.background = background;
        }
    }
    
    private class DisplayPrefsDBManager extends SQLiteOpenHelper {
        
        private static final String DB_LOC = "display_prefs.sql";
        
        private DisplayPrefsDBManager(Context context) {
            super(context, DB_LOC, null, 1);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            String colorSql;
            colorSql = "CREATE TABLE colors (" +
                    "  id INTEGER PRIMARY KEY" +
                    ", tableid TEXT" +
                    ", colname TEXT" +
                    ", comp TEXT" +
                    ", val TEXT" +
                    ", foreground TEXT" +
                    ", background TEXT" +
                    ");";
            db.execSQL(colorSql);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int v1, int v2) {
            // TODO Auto-generated method stub
        }
        
    }
    
}
