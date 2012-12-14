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
  public String id;
  public String colName;
  public RuleType compType;
  public String val;
  public int foreground;
  public int background;
  
  /**
   * Construct a new color rule to dictate the coloring of cells. Constructs
   * a UUID for the column id.
   * @param colName
   * @param compType
   * @param val
   * @param foreground
   * @param background
   */
  public ColColorRule(String colName, RuleType compType, String val, 
      int foreground, int background) {
    // generate a UUID for the color rule. We can't let it autoincrement ints
    // as was happening before, as this would become corrupted when rules were
    // imported from other dbs.
    this.id = UUID.randomUUID().toString();
    this.colName = colName;
    this.compType = compType;
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
    this.colName = colName;
    this.compType = compType;
    this.val = val;
    this.foreground = foreground;
    this.background = background;   
  }
  
  public static enum RuleType {
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    EQUAL("="),
    GREATER_THAN_OR_EQUAL(">="),
    GREATER_THAN(">"),
    NO_OP("add: operation value");
    
    // This is the string that represents this operation.
    private String symbol;
    
    private RuleType(String symbol) {
      this.symbol = symbol;
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
