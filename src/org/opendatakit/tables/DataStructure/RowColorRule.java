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

import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.util.Constants;

/**
 * This is a single rule that defines the coloring of a row. The actual 
 * coloration of that row depends on the result of a series of rules, and is
 * represented by {@link RowColorRuler}.
 * @author sudar.sam@gmail.com
 *
 */
/*
 * We are extending ColColorRule because really it is virtually the same thing.
 * The ColColorRule doesn't NEED the element key, since it is managed by the
 * system to only apply to a particular column. Since the elementKey is there,
 * we're going to coopt it for now and actually use the elementKey.
 */
public class RowColorRule extends ColColorRule {

  // For Serialization
  private RowColorRule() {
    super(null, null, null, Constants.DEFAULT_TEXT_COLOR, 
        Constants.DEFAULT_BACKGROUND_COLOR);
  }
  
  /**
   * Create a new rule.
   * @param colElementKey the element key of the column that will be of 
   * interest for this rule. E.g. you might want to color the whole row if the 
   * value in this column is greater than 5.
   * @param compType the type of comparison
   * @param val the value to which you are comparing
   * @param foreground the foreground color
   * @param background the background color
   */
  public RowColorRule(String colElementKey, RuleType compType, String val,
      int foreground, int background) {
    super (colElementKey, compType, val, foreground, background);
  }
  
  public RowColorRule(String uuid, String colElementKey, RuleType compType,
      String val, int foreground, int background) {
    super(uuid, colElementKey, compType, val, foreground, background);
  }
  
  public String toString() {
    // We want this to just say the column name first.
    // We should be saying the display name, not the element key!
    return this.getColumnElementKey() + " " + super.toString();
  }
}
