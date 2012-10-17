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
package org.opendatakit.tables.DataStructure;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Display preferences for a table.
 */
public class DisplayPrefs {
  
  public static final String TAG = "DisplayPrefs";
    
    private final Context context;
    private final DisplayPrefsDBManager dbm;
    private final String tableId;
    
    public DisplayPrefs(Context context, String tableId) {
        this.context = context;
        this.tableId = tableId;
        dbm = DisplayPrefsDBManager.getManager(context);
    }
    
    public List<ColColorRule> getColorRulesForCol(String colName) {
        List<ColColorRule> ruleList = new ArrayList<ColColorRule>();
        // querying the database
        //String[] cols = {"id", "comp", "val", "foreground", "background"};
        String[] cols = DisplayPrefsDBManager.getColumns();
        String selection = DisplayPrefsDBManager.TABLE_ID_COL + " = ? AND " +
            DisplayPrefsDBManager.COL_NAME_COL + " = ?";
        String[] selectionArgs = {String.valueOf(tableId), colName};
        SQLiteDatabase db = dbm.getReadableDatabase();
        Cursor cs = db.query(DisplayPrefsDBManager.DB_NAME, cols, selection, 
            selectionArgs, null, null, null, null);
        int idIndex = cs.getColumnIndexOrThrow(DisplayPrefsDBManager.ID_COL);
        int compIndex = cs.getColumnIndexOrThrow(
            DisplayPrefsDBManager.COMP_COL);
        int valIndex = cs.getColumnIndexOrThrow(DisplayPrefsDBManager.VAL_COL);
        int foregroundColorIndex = cs.getColumnIndexOrThrow(
            DisplayPrefsDBManager.FOREGROUND_COL);
        int backgroundColorIndex = cs.getColumnIndexOrThrow(
            DisplayPrefsDBManager.BACKGROUND_COL);
        // adding rules
        boolean done = !cs.moveToFirst();
        while(!done) {
            String id = cs.getString(idIndex);
            ColColorRule.RuleType compType = ColColorRule.RuleType.
                  getEnumFromString(cs.getString(compIndex));
            String val = cs.getString(valIndex);
            int foregroundColor = cs.getInt(foregroundColorIndex);
            int backgroundColor = cs.getInt(backgroundColorIndex);
            ruleList.add(new ColColorRule(id, colName, 
                compType, val, foregroundColor, backgroundColor));
            done = !cs.moveToNext();
        }
        // closing cursor and database and returning
        cs.close();
        db.close();
        return ruleList;
        
    }
    
    public void addRule(String ruleId, String colName, String compType, 
          String val, int foregroundColor, int backgroundColor) {
        SQLiteDatabase db = dbm.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DisplayPrefsDBManager.ID_COL, ruleId);
        values.put(DisplayPrefsDBManager.TABLE_ID_COL, tableId);
        values.put(DisplayPrefsDBManager.COL_NAME_COL, colName);
        //values.put("comp", compType + "");
        values.put(DisplayPrefsDBManager.COMP_COL, compType);
        values.put(DisplayPrefsDBManager.VAL_COL, val);
        values.put(DisplayPrefsDBManager.FOREGROUND_COL, foregroundColor);
        values.put(DisplayPrefsDBManager.BACKGROUND_COL, backgroundColor);
        db.insertOrThrow(DisplayPrefsDBManager.DB_NAME, null, values);
        db.close();
    }
    
    public void addRule(ColColorRule newRule) {
      addRule(newRule.id, newRule.colName, newRule.compType.getSymbol(), 
          newRule.val, newRule.foreground, newRule.background);
    }
    
    public String getTableId() {
      return tableId;
    }
    
    public void updateRule(ColColorRule rule) {
        SQLiteDatabase db = dbm.getWritableDatabase();
        ContentValues values = new ContentValues();
        //values.put("comp", rule.compType + "");
        values.put(DisplayPrefsDBManager.ID_COL, rule.id);
        values.put(DisplayPrefsDBManager.COMP_COL, rule.compType.getSymbol());
        values.put(DisplayPrefsDBManager.VAL_COL, rule.val);
        values.put(DisplayPrefsDBManager.FOREGROUND_COL, rule.foreground);
        values.put(DisplayPrefsDBManager.BACKGROUND_COL, rule.background);
        String whereClause = DisplayPrefsDBManager.ID_COL + " = ?";
        String[] whereArgs = {rule.id + ""};
        db.update(DisplayPrefsDBManager.DB_NAME, values, whereClause, 
            whereArgs);
        db.close();
    }
    
    public void deleteRule(ColColorRule rule) {
        SQLiteDatabase db = dbm.getWritableDatabase();
        String whereClause = DisplayPrefsDBManager.ID_COL + " = ?";
        String[] whereArgs = {rule.id + ""};
        db.delete(DisplayPrefsDBManager.DB_NAME, whereClause, whereArgs);
        db.close();
    }
    
    public ColumnColorRuler getColColorRuler(String colName) {
        // querying the database
        //String[] cols = {"comp", "val", "foreground", "background"};
        String[] cols = DisplayPrefsDBManager.getColumns();
        String selection = DisplayPrefsDBManager.TABLE_ID_COL + 
            " = ? AND " + DisplayPrefsDBManager.COL_NAME_COL + " = ?";
        // we don't want the display name here, b/c in the colors db we have it
        // as an underscore. so prepend an underscore.
        String[] selectionArgs = {String.valueOf(tableId), "_" + colName};
        SQLiteDatabase db = dbm.getReadableDatabase();
        Cursor cs = db.query(DisplayPrefsDBManager.DB_NAME, cols, selection, 
            selectionArgs, null, null, null, null);
        int compIndex = cs.getColumnIndexOrThrow(
            DisplayPrefsDBManager.COMP_COL);
        int valIndex = cs.getColumnIndex(DisplayPrefsDBManager.VAL_COL);
        int foregroundIndex = cs.getColumnIndexOrThrow(
            DisplayPrefsDBManager.FOREGROUND_COL);
        int backgroundIndex = cs.getColumnIndexOrThrow(
            DisplayPrefsDBManager.BACKGROUND_COL);
        // initializing the ccr
        TableProperties tp = TableProperties.getTablePropertiesForTable(
            DbHelper.getDbHelper(context), tableId,
            KeyValueStore.Type.ACTIVE);
        ColumnProperties cp = tp.getColumnByDbName(
            tp.getColumnByDisplayName(colName));
        ColumnColorRuler ccr = new ColumnColorRuler(cp.getColumnType());
        // adding rules
        boolean done = !cs.moveToFirst();
        while(!done) {
            String compStr = cs.getString(compIndex).trim();
            if(compStr.equals("")) {
                done = !cs.moveToNext();
                continue;
            }
            ColColorRule.RuleType compType = 
                ColColorRule.RuleType.getEnumFromString(compStr);
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
        private final List<ColColorRule.RuleType> ruleComps;
        private final List<String> ruleVals;
        private final List<Integer> foregroundColors;
        private final List<Integer> backgroundColors;
        
        private ColumnColorRuler(int colType) {
            this.colType = colType;
            ruleComps = new ArrayList<ColColorRule.RuleType>();
            ruleVals = new ArrayList<String>();
            foregroundColors = new ArrayList<Integer>();
            backgroundColors = new ArrayList<Integer>();
        }
        
        private void addRule(ColColorRule.RuleType compType, String val, 
            int foreground,
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
                compVal = (Double.valueOf(val)).compareTo(Double.valueOf(ruleVal));
            } else {
                compVal = val.compareTo(ruleVal);
            }
            Log.d("DP", "ruleVal:" + ruleVal);
            Log.d("DP", "val:" + val);
            Log.d("DP", "compVal:" + compVal);
            switch(ruleComps.get(index)) {
            case LESS_THAN:
              return (compVal < 0);
            case LESS_THAN_OR_EQUAL:
              return (compVal <= 0);
            case EQUAL:
                return (compVal == 0);
            case GREATER_THAN_OR_EQUAL:
                return (compVal >= 0);
            case GREATER_THAN:
                return (compVal > 0);
            default:
                Log.e(TAG, "unrecongized op passed to checkMatch: " + 
                    ruleComps.get(index));
                throw new IllegalArgumentException("unrecognized op passed " +
                    "to checkMatch: " + ruleComps.get(index));
            }
        }
        
    }
    
    
}
