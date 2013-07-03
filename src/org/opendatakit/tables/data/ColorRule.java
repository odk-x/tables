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

import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.annotate.JsonIgnore;

import android.util.Log;

/**
 * This is a single rule specifying a color for a given datum. 
 * @author sudar.sam@gmail.com
 *
 */
public class ColorRule {
  
  public static final String TAG = "ColColorRule";
  
  // The UUID of the rule.
  private String mId;
  /**
   * Element key of the column this rule queries on.
   */
  private String mElementKey;
  private RuleType mOperator;
  private String mValue;
  private int mForeground;
  private int mBackground;
  
  // ONLY FOR SERIALIZATION
  private ColorRule() {
    // not implemented, used only for serialization
  }
  
  /**
   * Construct a new color rule to dictate the coloring of cells. Constructs
   * a UUID for the column id.
   * @param colElementKey the element key of the column against which this rule
   * will be checking values
   * @param compType the comparison type of the rule
   * @param value the target value of the rule
   * @param foreground the foreground color of the rule
   * @param background the background color of the rule
   */
  public ColorRule(String colElementKey, RuleType compType, String value, 
      int foreground, int background) {
    // generate a UUID for the color rule. We can't let it autoincrement ints
    // as was happening before, as this would become corrupted when rules were
    // imported from other dbs.
    this(UUID.randomUUID().toString(), colElementKey, compType, value, 
        foreground, background);
  }
  
  /**
   * Construct a new color rule.
   * @param id
   * @param colName
   * @param compType
   * @param value
   * @param foreground
   * @param background
   */
  public ColorRule(String id, String colName, RuleType compType, 
      String value, int foreground, int background) {
    this.mId = id;
    this.mElementKey = colName;
    this.mOperator = compType;
    this.mValue = value;
    this.mForeground = foreground;
    this.mBackground = background;   
  }
  
  /**
   * Get the UUID of the rule.
   * @return
   */
  @JsonIgnore
  public String getRuleId() {
    return mId;
  }
  
  /**
   * Get the element key of the column to which this rule applies.
   * @return
   */
  @JsonIgnore
  public String getColumnElementKey() {
    return mElementKey;
  }
  
  /**
   * Get the target value to which the rule is being compared.
   * @return
   */
  @JsonIgnore
  public String getVal() {
    return mValue;
  }
  
  public void setVal(String newVal) {
    this.mValue = newVal;
  }
  
  /**
   * Get the foreground color of this rule.
   * @return
   */
  @JsonIgnore
  public int getForeground() {
    return mForeground;
  }
  
  /**
   * Return symbol space value.
   */
  @Override
  public String toString() {
    String symbol = mOperator.getSymbol();
    String value = mValue;
    return symbol + " " + value;
  }
  
  public void setForeground(int newForeground) {
    this.mForeground = newForeground;
  }
  
  /**
   * Get the background color of this rule.
   * @return
   */
  @JsonIgnore
  public int getBackground() {
    return mBackground;
  }
  
  public void setBackground(int newBackground) {
    this.mBackground = newBackground;
  }
  
  @JsonIgnore
  public RuleType getOperator() {
    return mOperator;
  }
  
  public void setOperator(RuleType newOperator) {
    this.mOperator = newOperator;
  }
  
  /**
   * Set the element key of the column to which this rule will apply.
   * @param elementKey
   */
  public void setColumnElementKey(String elementKey) {
    this.mElementKey = elementKey;
  }
  
  public boolean checkMatch(String[] rowData, 
      Map<String, Integer> indexMapping, 
      Map<String, ColumnProperties> propertiesMapping) {
    try {
      // First get the data abou the column.
      ColumnProperties cp = propertiesMapping.get(mElementKey);
      // Get the value we're testing against.
      String testValue = rowData[indexMapping.get(mElementKey)];
      if (testValue == null) {
        testValue = "";
      }
      int compVal;
        if((cp.getColumnType() == ColumnType.NUMBER ||
           cp.getColumnType() == ColumnType.INTEGER)){
          if (testValue.equals("")) {
            return false;
          }
            double doubleValue = Double.parseDouble(testValue);
            double doubleRule = Double.parseDouble(mValue);
            compVal = (Double.valueOf(doubleValue))
                .compareTo(Double.valueOf(doubleRule));
        } else {
            compVal = testValue.compareTo(mValue);
        }
        switch(mOperator) {
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
            Log.e(TAG, "unrecongized op passed to checkMatch: " + mOperator);
            throw new IllegalArgumentException("unrecognized op passed " +
                "to checkMatch: " + mOperator);
        }
    } catch (NumberFormatException e) {
      // Here we should maybe throw an exception that a rule is offending,
      // and then catch and delete it in the decider.
      e.printStackTrace();
      Log.e(TAG, "was an error parsing value to a number, removing the " +
          "offending rule");
    }
    return false;
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
