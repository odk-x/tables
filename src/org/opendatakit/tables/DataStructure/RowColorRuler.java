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
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.util.Constants;

import android.util.Log;

/**
 * A ruler that looks at the rules set on a row and indicates what the correct
 * color of the row should be.
 * <p>
 * The abstraction that the ruler is composed of a list of {@link RowColorRule}
 * objects that together to form the ruler.
 * @author sudar.sam@gmail.com
 *
 */
public class RowColorRuler {
  
  private static final String TAG = RowColorRuler.class.getName();

  
  /*****************************
   * Things needed for the key value store.
   *****************************/
  public static final String KVS_PARTITION = "RowColorRuler";
  public static final String KEY_COLOR_RULES = 
      "RowColorRuler.ruleList";
  public static final String DEFAULT_KEY_COLOR_RULES = "[]";
  
//  private final TableProperties mTp;
//  private final ObjectMapper mMapper;
//  private final TypeFactory mTypeFactory;
//  private final KeyValueStoreHelper mKvsh;
//  // The list of rules that actually makes up the ruler.
//  private List<RowColorRule> mRuleList;
//  
//  public RowColorRuler(TableProperties tp) {
//    this.mTp = tp;
//    this.mKvsh = tp.getKeyValueStoreHelper(KVS_PARTITION);
//    this.mMapper = new ObjectMapper();
//    this.mTypeFactory = mMapper.getTypeFactory();
//    mMapper.setVisibilityChecker(
//        mMapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
//    mMapper.setVisibilityChecker(
//        mMapper.getVisibilityChecker().withCreatorVisibility(Visibility.ANY));
//    this.mRuleList = loadSavedColorRules();
//  }
//  
//  /**
//   * Get the RowColorRuler for the table given by the TableProperties 
//   * parameter.
//   * @param tp
//   */
//  public static RowColorRuler getRowColorRuler(TableProperties tp) {
//    return new RowColorRuler(tp);
//  }
//  
//  /**
//   * Just go ahead and reclaim any existing rules from the database.
//   * @return
//   */
//  private List<RowColorRule> loadSavedColorRules() {
//    String jsonRuleString = mKvsh.getObject(KEY_COLOR_RULES);
//    if (jsonRuleString == null) {
//      return new ArrayList<RowColorRule>();
//    }
//    List<RowColorRule> reclaimedRules = new ArrayList<RowColorRule>();
//    try {
//      reclaimedRules = 
//          mMapper.readValue(jsonRuleString, 
//              mTypeFactory.constructCollectionType(ArrayList.class, 
//                  RowColorRule.class));
//    } catch (JsonParseException e) {
//      Log.e(TAG, "problem parsing json to RowColorRule");
//      e.printStackTrace();
//    } catch (JsonMappingException e) {
//      Log.e(TAG, "problem mapping json to RowColorRule");
//      e.printStackTrace();     
//    } catch (IOException e) {
//      Log.e(TAG, "i/o problem with json to RowColorRule");
//      e.printStackTrace();     
//    }
//    return reclaimedRules;
//  }
//  
//  /**
//   * Get the list of {@link RowColorRule} objects that makes up this ruler.
//   * @return
//   */
//  public List<RowColorRule> getColorRules() {
//    return mRuleList;
//  }
//  
//  /**
//   * Replace the list of {@link RowColorRule} objecst that makes up this ruler.
//   * Doesn't persist it.
//   * @param newRules
//   */
//  public void replaceColorRuleList(List<RowColorRule> newRules) {
//    this.mRuleList = newRules;
//  }
//  
//  /**
//   * Persist the rule list into the key value store. Does nothing if there are
//   * no rules, and removes the old key if there are no rules in the current
//   * list.
//   */
//  public void saveRuleList() {
//    // First we'll remove the key if there are no longer any rules.
//    if (mRuleList.size() == 0) {
//      mKvsh.removeKey(KEY_COLOR_RULES);
//      return;
//    }
//    // Otherwise we have to parse it.
//    String ruleListJson = DEFAULT_KEY_COLOR_RULES;
//    try {
//      ruleListJson = mMapper.writeValueAsString(mRuleList);
//      mKvsh.setObject(KEY_COLOR_RULES, ruleListJson);
//    } catch (JsonParseException e) {
//      Log.e(TAG, "problem parsing list of RowColorRule");
//      e.printStackTrace();
//    } catch (JsonMappingException e) {
//      Log.e(TAG, "problem mapping list of RowColorRule");
//      e.printStackTrace();     
//    } catch (IOException e) {
//      Log.e(TAG, "i/o problem with list of RowColorRule");
//      e.printStackTrace();     
//    } 
//  }
//  
//  public int getRuleCount() {
//    return mRuleList.size();
//  }
//  
//  /**
//   * Use this rulelist to get the {@link ColorResult} for the row.
//   * @param values
//   * @return
//   */
//  public ColorGuide getForegroundColor(Map<String, String> values) {
//    for (int i = 0; i < mRuleList.size(); i++) {
//      // To know which the relevant value is, we have to get the data from the
//      // map.
//      String elementKey = mRuleList.get(i).getColumnElementKey();
//      if (checkMatch(values.get(elementKey), i)) {
//        int background = mRuleList.get(i).getBackground();
//        int foreground = mRuleList.get(i).getForeground();
//        ColorGuide result = new ColorGuide(background, foreground);
//        return result;
//      }
//    }
//    return new ColorGuide(Constants.DEFAULT_BACKGROUND_COLOR, 
//        Constants.DEFAULT_TEXT_COLOR);
//  }
//    
//  /**
//   * Check if a value matches the rule at position index in the rule list.
//   * @param val
//   * @param index
//   * @return
//   */
//  private boolean checkMatch(String val, int index) {
//    RowColorRule rule = mRuleList.get(index);
//    ColumnProperties cp = 
//        mTp.getColumnByElementKey(rule.getColumnElementKey());
//    try {
//      int compVal;
//      String ruleVal = rule.getVal();
//      if((cp.getColumnType() == ColumnType.NUMBER ||
//         cp.getColumnType() == ColumnType.INTEGER)){
//        if (val.equals("")) {
//          return false;
//        }
//        double doubleValue = Double.parseDouble(val);
//        double doubleRule = Double.parseDouble(ruleVal);
//        Log.d(TAG, "doubleValue:" + doubleValue);
//        Log.d(TAG, "doubleRule:" + doubleRule);
//        compVal = (Double.valueOf(val)).compareTo(Double.valueOf(ruleVal));
//      } else {
//        compVal = val.compareTo(ruleVal);
//      }
//      Log.d(TAG, "ruleVal:" + ruleVal);
//      Log.d(TAG, "val:" + val);
//      Log.d(TAG, "compVal:" + compVal);
//      switch(rule.getOperator()) {
//      case LESS_THAN:
//        return (compVal < 0);
//      case LESS_THAN_OR_EQUAL:
//        return (compVal <= 0);
//      case EQUAL:
//          return (compVal == 0);
//      case GREATER_THAN_OR_EQUAL:
//          return (compVal >= 0);
//      case GREATER_THAN:
//          return (compVal > 0);
//      default:
//          Log.e(TAG, "unrecongized op passed to checkMatch: " + 
//              rule.getOperator());
//          throw new IllegalArgumentException("unrecognized op passed " +
//              "to checkMatch: " + rule.getOperator());
//      }
//    } catch (NumberFormatException e) {
//      mRuleList.remove(index);
//      saveRuleList();
//      e.printStackTrace();
//      Log.e(TAG, "was an error parsing value to a number, removing the " +
//          "offending rule");
//    }
//    return false;      
//  }
//  
//  /**
//   * Basic struct for getting back color information when checking a rule.
//   * @author sudar.sam@gmail.com
//   *
//   */
//  public class ColorGuide {
//    int mForegroundColor;
//    int mBackGroundColor;
//    
//    public ColorGuide(int background, int foreground) {
//      this.mForegroundColor = foreground;
//      this.mBackGroundColor = background;
//    }
//    
//    public int getBackgroundColor() {
//      return this.mBackGroundColor;
//    }
//    
//    public int getForegroundColor() {
//      return this.mForegroundColor;
//    }
//  }
  
}
