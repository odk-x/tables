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
package org.opendatakit.hope.data;

/**
 * Represents the type of an entry in the key value store. This essentially
 * confines the string values that may be set to the type column in the key
 * value store. 
 * @author sudar.sam@gmail.com
 *
 */
public enum KeyValueStoreEntryType {
  TEXT("text"),
  INTEGER("integer"),
  NUMBER("number"),
  BOOLEAN("boolean"),
  ARRAYLIST("arraylist"),
  OBJECT("object");
  
  private String label;
  
  private KeyValueStoreEntryType(String label) {
    this.label = label;
  }
  
  public String getLabel() {
    return label;
  }
}
