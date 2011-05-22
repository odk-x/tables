package yoonsung.odk.spreadsheet.DataStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.ColumnProperty.ColumnType;

/**
 * Display preferences for a table.
 */
public class DisplayPrefs {
    
    private final DisplayPrefsDBManager dbm;
    private final String tableId;
    
    public DisplayPrefs(Context context, String tableId) {
        this.tableId = tableId;
        dbm = new DisplayPrefsDBManager(context);
    }
    
    public void addRule(String colName, int compType, String val, int color) {
        SQLiteDatabase db = dbm.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tableid", tableId);
        values.put("colname", colName);
        values.put("comp", compType);
        values.put("val", val);
        values.put("color", color);
        db.insertOrThrow("colors", null, values);
        db.close();
    }
    
    public ColumnColorRuler getColColorRuler(String colName) {
        // querying the database
        String[] cols = {"comp", "val", "color"};
        String selection = "tableid = ? AND colname = ?";
        String[] selectionArgs = {tableId, colName};
        SQLiteDatabase db = dbm.getReadableDatabase();
        Cursor cs = db.query("colors", cols, selection, selectionArgs, null,
                null, null, null);
        int compIndex = cs.getColumnIndexOrThrow("comp");
        int valIndex = cs.getColumnIndex("val");
        int colorIndex = cs.getColumnIndex("color");
        // initializing the ccr
        ColumnProperty cp = new ColumnProperty(tableId);
        ColumnColorRuler ccr = new ColumnColorRuler(cp.getColumnType(colName));
        // adding rules
        boolean done = !cs.moveToFirst();
        while(!done) {
            int compType = cs.getInt(compIndex);
            String val = cs.getString(valIndex);
            int color = cs.getInt(colorIndex);
            ccr.addRule(compType, val, color);
            done = !cs.moveToNext();
        }
        // closing cursor and database and returning
        cs.close();
        db.close();
        return ccr;
    }
    
    public class ColumnColorRuler {
        
        private ColumnType colType;
        private final List<Integer> ruleComps;
        private final List<String> ruleVals;
        private final List<Integer> ruleColors;
        
        private ColumnColorRuler(ColumnType colType) {
            this.colType = colType;
            ruleComps = new ArrayList<Integer>();
            ruleVals = new ArrayList<String>();
            ruleColors = new ArrayList<Integer>();
        }
        
        private void addRule(int compType, String val, int color) {
            ruleComps.add(compType);
            ruleVals.add(val);
            ruleColors.add(color);
        }
        
        public int getColor(String val, int defVal) {
            for(int i=0; i<ruleComps.size(); i++) {
                if(checkMatch(val, i)) {
                    return ruleColors.get(i);
                }
            }
            return defVal;
        }
        
        private boolean checkMatch(String val, int index) {
            int compVal;
            String ruleVal = ruleVals.get(index);
            if(colType == ColumnType.NUMERIC_VALUE) {
                compVal = (new Double(val)).compareTo(new Double(ruleVal));
            } else {
                compVal = val.compareTo(ruleVal);
            }
            switch(ruleComps.get(index)) {
            case 0:
                return (compVal == 0);
            case 1:
                return (compVal < 0);
            case 2:
                return (compVal > 0);
            case 3:
                return (compVal <= 0);
            case 4:
                return (compVal >= 0);
            default:
                return false;
            }
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
                    "  tableid TEXT" +
                    ", colname TEXT" +
                    ", comp TEXT" +
                    ", val TEXT" +
                    ", color TEXT" +
                    ");";
            db.execSQL(colorSql);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int v1, int v2) {
            // TODO Auto-generated method stub
        }
        
    }
    
}
