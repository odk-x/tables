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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.TypeReference;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
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
  
  /*****************************
   * Things needed for the key value store.
   *****************************/
  public static final String KEY_COLOR_RULES = 
      "DisplayPrefs.coloRules";
  public static final String DEFAULT_KEY_COLOR_RULES = "[]";
    
    private final Context context;
//    private final DisplayPrefsDBManager dbm;
//    private final String tableId;
    private TableProperties tp;
    private ObjectMapper mapper;
    private TypeFactory typeFactory;
    // the list of rules
    private List<ColColorRule> ruleList;
    // this is the elementKey of the column to which these display prefs apply
    private String elementKey;
    
    public DisplayPrefs(Context context, TableProperties tp, 
        String elementKey) {
        this.context = context;
//        this.tableId = tableId;
        this.tp = tp;
        this.mapper = new ObjectMapper();
        this.typeFactory = mapper.getTypeFactory();
        this.elementKey = elementKey;
        this.ruleList = loadSavedColorRules(elementKey);
        mapper.setVisibilityChecker(
            mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
        mapper.setVisibilityChecker(
            mapper.getVisibilityChecker()
            .withCreatorVisibility(Visibility.ANY));
//        dbm = DisplayPrefsDBManager.getManager(context);
    }
    
    public List<ColColorRule> getColorRules() {
      return ruleList;
    }
    
    private List<ColColorRule> loadSavedColorRules(String colElementKey) {
      // do this here b/c indexed columns being null passes around null values.
      if (colElementKey == null) {
        return new ArrayList<ColColorRule>();
      }
//        List<ColColorRule> ruleList = new ArrayList<ColColorRule>();
      String jsonRulesString = 
          tp.getObjectEntry(ColumnProperties.KVS_PARTITION,
          colElementKey, KEY_COLOR_RULES);
      if (jsonRulesString == null) { // no values in the kvs
        return new ArrayList<ColColorRule>();
      }
      List<ColColorRule> reclaimedRules = new ArrayList<ColColorRule>();
      try {
        List<ColColorRule> jsonRulesList = 
            mapper.readValue(jsonRulesString, 
                typeFactory.constructCollectionType(ArrayList.class, 
                    ColColorRule.class));
        reclaimedRules = jsonRulesList;
//        for (int i = 0; i < jsonRulesList.size(); i++) {
////            ColColorRule nextRule = 
////                mapper.readValue(jsonRulesList.get(i), 
////                ColColorRule.class);
//          ColColorRule nextRule = reclaimedRules.get(i);
//            reclaimedRules.add(nextRule);
//        }
      } catch (JsonParseException e) {
        Log.e(TAG, "problem parsing json to colocolorrule");
        e.printStackTrace();
      } catch (JsonMappingException e) {
        Log.e(TAG, "problem mapping json to colocolorrule");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e(TAG, "i/o problem with json to colocolorrule");
        e.printStackTrace();
      }
        // querying the database
        //String[] cols = {"id", "comp", "val", "foreground", "background"};
//        String[] cols = DisplayPrefsDBManager.getColumns();
//        String selection = DisplayPrefsDBManager.TABLE_ID_COL + " = ? AND " +
//            DisplayPrefsDBManager.COL_NAME_COL + " = ?";
//        String[] selectionArgs = {String.valueOf(tp.getTableId()), colName};
//        SQLiteDatabase db = null;
//        Cursor cs = null;
//        try {
//	        db = dbm.getReadableDatabase();
//	        cs = db.query(DisplayPrefsDBManager.DB_NAME, cols, selection, 
//	            selectionArgs, null, null, null, null);
//	        int idIndex = cs.getColumnIndexOrThrow(DisplayPrefsDBManager.ID_COL);
//	        int compIndex = cs.getColumnIndexOrThrow(
//	            DisplayPrefsDBManager.COMP_COL);
//	        int valIndex = cs.getColumnIndexOrThrow(DisplayPrefsDBManager.VAL_COL);
//	        int foregroundColorIndex = cs.getColumnIndexOrThrow(
//	            DisplayPrefsDBManager.FOREGROUND_COL);
//	        int backgroundColorIndex = cs.getColumnIndexOrThrow(
//	            DisplayPrefsDBManager.BACKGROUND_COL);
//	        // adding rules
//	        boolean done = !cs.moveToFirst();
//	        while(!done) {
//	            String id = cs.getString(idIndex);
//	            ColColorRule.RuleType compType = ColColorRule.RuleType.
//	                  getEnumFromString(cs.getString(compIndex));
//	            String val = cs.getString(valIndex);
//	            int foregroundColor = cs.getInt(foregroundColorIndex);
//	            int backgroundColor = cs.getInt(backgroundColorIndex);
//	            ruleList.add(new ColColorRule(id, colName, 
//	                compType, val, foregroundColor, backgroundColor));
//	            done = !cs.moveToNext();
//	        }
	        // closing cursor and database and returning
	        return reclaimedRules;
//        } finally {
//        	try {
//        		if ( cs != null && !cs.isClosed()) {
//        			cs.close();
//        		}
//        	} finally {
//        		if ( db != null ) {
//        			db.close();
//        		}
//        	}
//        }
        
    }
    
//    public void addRule(String ruleId, String colName, String compType, 
//          String val, int foregroundColor, int backgroundColor) {
//        SQLiteDatabase db = dbm.getWritableDatabase();
//        try {
//	        ContentValues values = new ContentValues();
//	        values.put(DisplayPrefsDBManager.ID_COL, ruleId);
//	        values.put(DisplayPrefsDBManager.TABLE_ID_COL, tp.getTableId());
//	        values.put(DisplayPrefsDBManager.COL_NAME_COL, colName);
//	        //values.put("comp", compType + "");
//	        values.put(DisplayPrefsDBManager.COMP_COL, compType);
//	        values.put(DisplayPrefsDBManager.VAL_COL, val);
//	        values.put(DisplayPrefsDBManager.FOREGROUND_COL, foregroundColor);
//	        values.put(DisplayPrefsDBManager.BACKGROUND_COL, backgroundColor);
//	        db.insertOrThrow(DisplayPrefsDBManager.DB_NAME, null, values);
//        } finally {
//        	db.close();
//        }
//    }
    
    public void addRule(ColColorRule newRule) {
//      addRule(newRule.id, newRule.colName, newRule.compType.getSymbol(), 
//          newRule.val, newRule.foreground, newRule.background);
      ruleList.add(newRule);
    }
    
    public String getTableId() {
      return tp.getTableId();
    }
    
    public void updateRule(ColColorRule rule) {
      for (int i = 0; i < ruleList.size(); i++) {
        if (ruleList.get(i).id.equals(rule.id)) {
          ruleList.set(i, rule);
          return;
        }
      }
      Log.e(TAG, "tried to update a rule that matched no saved ids");
//        SQLiteDatabase db = dbm.getWritableDatabase();
//        try {
//	        ContentValues values = new ContentValues();
//	        //values.put("comp", rule.compType + "");
//	        values.put(DisplayPrefsDBManager.ID_COL, rule.id);
//	        values.put(DisplayPrefsDBManager.COMP_COL, rule.compType.getSymbol());
//	        values.put(DisplayPrefsDBManager.VAL_COL, rule.val);
//	        values.put(DisplayPrefsDBManager.FOREGROUND_COL, rule.foreground);
//	        values.put(DisplayPrefsDBManager.BACKGROUND_COL, rule.background);
//	        String whereClause = DisplayPrefsDBManager.ID_COL + " = ?";
//	        String[] whereArgs = {rule.id + ""};
//	        db.update(DisplayPrefsDBManager.DB_NAME, values, whereClause, 
//	            whereArgs);
//        } finally {
//        	db.close();
//        }
    }
    
    /**
     * Put the cached rules into the key value store. Does nothing if there are
     * no rules, so will not pollute the key value store unless something has
     * been added.
     */
    public void saveRuleList() {
      // set it to this default just in case something goes wrong and it is 
      // somehow set. this way if you manage to set the object you will have
      // something that doesn't throw an error when you expect to get back 
      // an array list. it will just be of length 0. not sure if this is a good
      // idea or not.
      if (ruleList.size() == 0) {
        return;
      }
      String ruleListJson = DEFAULT_KEY_COLOR_RULES;
      try {
        ruleListJson = mapper.writeValueAsString(ruleList);
        tp.setObjectEntry(ColumnProperties.KVS_PARTITION, elementKey, 
            KEY_COLOR_RULES, ruleListJson);
      } catch (JsonGenerationException e) {
        Log.e(TAG, "problem parsing list of color rules");
        e.printStackTrace();
      } catch (JsonMappingException e) {
        Log.e(TAG, "problem mapping list of color rules");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e(TAG, "i/o problem with json list of color rules");
        e.printStackTrace();
      }
    }
    
    public void deleteRule(ColColorRule rule) {
      
//        SQLiteDatabase db = dbm.getWritableDatabase();
//        try {
//	        String whereClause = DisplayPrefsDBManager.ID_COL + " = ?";
//	        String[] whereArgs = {rule.id + ""};
//	        db.delete(DisplayPrefsDBManager.DB_NAME, whereClause, whereArgs);
//        } finally {
//        	db.close();
//        }
    }
    
    public ColumnColorRuler getColColorRuler(TableProperties tp, 
        String colName) {
      ColumnColorRuler columnRuler = 
          new ColumnColorRuler(
              tp.getColumnByDbName(tp.getColumnByDisplayName(colName))
              .getColumnType());
      for (ColColorRule rule : ruleList) {
        columnRuler.addRule(rule);
      }
      return columnRuler;
        // querying the database
        //String[] cols = {"comp", "val", "foreground", "background"};
//        String[] cols = DisplayPrefsDBManager.getColumns();
//        String selection = DisplayPrefsDBManager.TABLE_ID_COL + 
//            " = ? AND " + DisplayPrefsDBManager.COL_NAME_COL + " = ?";
//        // we don't want the display name here, b/c in the colors db we have it
//        // as an underscore. so prepend an underscore.
//        String[] selectionArgs = {String.valueOf(tp.getTableId()), "_" + colName};
//        SQLiteDatabase db = null;
//        Cursor cs = null;
//        try {
//        	db = dbm.getReadableDatabase();
//	        cs = db.query(DisplayPrefsDBManager.DB_NAME, cols, selection, 
//	            selectionArgs, null, null, null, null);
//	        int compIndex = cs.getColumnIndexOrThrow(
//	            DisplayPrefsDBManager.COMP_COL);
//	        int valIndex = cs.getColumnIndex(DisplayPrefsDBManager.VAL_COL);
//	        int foregroundIndex = cs.getColumnIndexOrThrow(
//	            DisplayPrefsDBManager.FOREGROUND_COL);
//	        int backgroundIndex = cs.getColumnIndexOrThrow(
//	            DisplayPrefsDBManager.BACKGROUND_COL);
//	        // initializing the ccr
//	        ColumnProperties cp = tp.getColumnByDbName(
//	            tp.getColumnByDisplayName(colName));
//	        ColumnColorRuler ccr = new ColumnColorRuler(cp.getColumnType());
//	        // adding rules
//	        boolean done = !cs.moveToFirst();
//	        while(!done) {
//	            String compStr = cs.getString(compIndex).trim();
//	            if(compStr.equals("")) {
//	                done = !cs.moveToNext();
//	                continue;
//	            }
//	            ColColorRule.RuleType compType = 
//	                ColColorRule.RuleType.getEnumFromString(compStr);
//	            String val = cs.getString(valIndex).trim();
//	            int foreground = cs.getInt(foregroundIndex);
//	            int background = cs.getInt(backgroundIndex);
//	            ccr.addRule(compType, val, foreground, background);
//	            done = !cs.moveToNext();
//	        }
//	        // closing cursor and database and returning
//	        return ccr;
//	    } finally {
//	    	try {
//	    		if ( cs != null && !cs.isClosed() ) {
//	    			cs.close();
//	    		}
//	    	} finally {
//	    		if ( db != null ) {
//	    			db.close();
//	    		}
//	    	}
//	    }
    }
    
    public class ColumnColorRuler {
        
        private ColumnType colType;
        private final List<ColColorRule.RuleType> ruleComps;
        private final List<String> ruleVals;
        private final List<Integer> foregroundColors;
        private final List<Integer> backgroundColors;
        
        private ColumnColorRuler(ColumnType colType) {
            this.colType = colType;
            ruleComps = new ArrayList<ColColorRule.RuleType>();
            ruleVals = new ArrayList<String>();
            foregroundColors = new ArrayList<Integer>();
            backgroundColors = new ArrayList<Integer>();
        }
//        
//        private void addRule(ColColorRule.RuleType compType, String val, 
//            int foreground,
//                int background) {
//            ruleComps.add(compType);
//            ruleVals.add(val);
//            foregroundColors.add(foreground);
//            backgroundColors.add(background);
//        }
        
        private void addRule(ColColorRule rule) {
          ruleComps.add(rule.compType);
          ruleVals.add(rule.val);
          foregroundColors.add(rule.foreground);
          backgroundColors.add(rule.background);
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
            if(colType == ColumnType.NUMBER ||
               colType == ColumnType.INTEGER) {
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
