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

/**
 * This class represents a column that is joined to by another column, in a 
 * separate table, that is of type join. "Join" in this sense is not a strictly
 * database definition of the word, but is more like a link to another column
 * in another table.
 * <p>
 * For instance, say that you had a column that was facility ID. You might like
 * to link that column to a table of all equipment, and have that table 
 * restricted to only those rows that show the equipment for that facility. 
 * In the first table, the column with the facility code would be of type 
 * join. It would join to the "equipment table", and in particular the 
 * "facility code" column in the equipment table. When you click on the linked,
 * or joined, cell in the first table, it would push you into that table and
 * restrict the rows to display only the equipment for the facility with the
 * code on which you clicked.
 * <p>
 * @author sudar.sam@gmail.com
 *
 */
/*
 * We are going to keep this just as a basic struct of an object that is easy
 * to be serialized as JSON. This would possibly allow for a list of joins if
 * we ever wanted to do that. But for now, we just want to imagine one of these
 * objects existing as a JSON string in the joins column of the column
 * definitions table.
 */
public class JoinColumn {
  
  /**
   * This is the message that should be added to the join column upon creation.
   */
  public static final String DEFAULT_NOT_SET_VALUE = "";
  
  /*
   * The table id of the table to which you are joining.
   */
  private String tableId;
  
  /*
   * The element key (which with the table id forms the unique column 
   * identifier) for the column to which you are joining.
   */
  private String elementKey;
  
  /*
   * Just in case we need this for serialization.
   */
  private JoinColumn() {
  }
  
  public JoinColumn(String tableId, String elementKey) {
    this.tableId = tableId;
    this.elementKey = elementKey;
  }
  
  public String getTableId() {
    return this.tableId;
  }
  
  public String getElementKey() {
    return this.elementKey;
  }
  
  public void setTableId(String tableId) {
    this.tableId = tableId;
  }
  
  public void setElementKey(String elementKey) {
    this.elementKey = elementKey;
  }
  
  
}
