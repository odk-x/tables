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

import java.util.UUID;

import android.util.Log;

/**
 * As far as I can tell this is a rule that is applied to color column cells.
 * @author sudar.sam@gmail.com
 *
 */
public class ColColorRule {
  
  public static final String TAG = "ColColorRule";
  
  // this is the primary key of the column, so basically an int UUID.
  // changing to be a uuid.
  private String id;
  private String columnElementKey;
  private RuleType operator;
  private String val;
  private int foreground;
  private int background;
  
  // ONLY FOR SERIALIZATION
  private ColColorRule() {
    // not implemented, used only for serialization
  }
  
  /**
   * Construct a new color rule to dictate the coloring of cells. Constructs
   * a UUID for the column id.
   * @param colName
   * @param compType
   * @param val
   * @param foreground
   * @param background
   */
  public ColColorRule(String colElementKey, RuleType compType, String val, 
      int foreground, int background) {
    // generate a UUID for the color rule. We can't let it autoincrement ints
    // as was happening before, as this would become corrupted when rules were
    // imported from other dbs.
    this.id = UUID.randomUUID().toString();
    this.columnElementKey = colElementKey;
    this.operator = compType;
    this.val = val;
    this.foreground = foreground;
    this.background = background;
  }
  
  /**
   * Construct a new color rule. Presumes that the passed in id is a UUID.
   * @param id
   * @param colName
   * @param compType
   * @param val
   * @param foreground
   * @param background
   */
  public ColColorRule(String id, String colName, RuleType compType, String val,
      int foreground, int background) {
    this.id = id;
    this.columnElementKey = colName;
    this.operator = compType;
    this.val = val;
    this.foreground = foreground;
    this.background = background;   
  }
  
  public String getId() {
    return id;
  }
  
  public String getColumnElementKey() {
    return columnElementKey;
  }
  
  public String getVal() {
    return val;
  }
  
  public void setVal(String newVal) {
    this.val = newVal;
  }
  
  public int getForeground() {
    return foreground;
  }
  
  /**
   * Return symbol space value.
   */
  @Override
  public String toString() {
    String symbol = operator.getSymbol();
    String value = val;
    return symbol + " " + value;
  }
  
  public void setForeground(int newForeground) {
    this.foreground = newForeground;
  }
  
  public int getBackground() {
    return background;
  }
  
  public void setBackground(int newBackground) {
    this.background = newBackground;
  }
  
  public RuleType getOperator() {
    return operator;
  }
  
  public void setOperator(RuleType newOperator) {
    this.operator = newOperator;
  }
  
  public static enum RuleType {
        
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    EQUAL("="),
    GREATER_THAN_OR_EQUAL(">="),
    GREATER_THAN(">"),
    NO_OP("operation value");
    
    private static final String STR_LESS_THAN = "<";
    private static final String STR_LESS_OR_EQUAL = "<=";
    private static final String STR_EQUAL = "=";
    private static final String STR_GREATER_OR_EQUAL = ">=";
    private static final String STR_GREATER_THAN = ">";
    private static final int NUM_VALUES_FOR_SPINNER = 5;

    
    // This is the string that represents this operation.
    private String symbol;
    
    private RuleType(String symbol) {
      this.symbol = symbol;
    }
    
    /**
     * Return the possible values. Intended for a preference screen.
     * @return
     */
    public static CharSequence[] getValues() {
      CharSequence[] result = new CharSequence[NUM_VALUES_FOR_SPINNER];
      result[0] = STR_LESS_THAN;
      result[1] = STR_LESS_OR_EQUAL;
      result[2] = STR_EQUAL;
      result[3] = STR_GREATER_OR_EQUAL;
      result[4] = STR_GREATER_THAN;
      return result;
    }
    
    public String getSymbol() {
      return String.valueOf(symbol);
    }
    
    public static RuleType getEnumFromString(String inputString) {
      if (inputString.equals(LESS_THAN.symbol)) {
        return LESS_THAN;
      } else if (inputString.equals(LESS_THAN_OR_EQUAL.symbol)) {
        return LESS_THAN_OR_EQUAL;
      } else if (inputString.equals(EQUAL.symbol)) {
        return EQUAL;
      } else if (inputString.equals(GREATER_THAN_OR_EQUAL.symbol)) {
        return GREATER_THAN_OR_EQUAL;
      } else if (inputString.equals(GREATER_THAN.symbol)) {
        return GREATER_THAN;
     // this case is just to handle original code's nonsense
      } else if (inputString.equals("") || inputString.equals(" ")) { 
        return NO_OP;
      } else {
        Log.e(TAG, "unrecognized rule operator: " + inputString);
        throw new IllegalArgumentException("unrecognized rule operator: " +
            inputString);
      }
    }
  }
}
