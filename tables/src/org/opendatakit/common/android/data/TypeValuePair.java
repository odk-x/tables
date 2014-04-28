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
package org.opendatakit.common.android.data;

/**
 * Just a struct to hold the type and value of an entry from the key value 
 * store. It should be used in a Map<String, TypeValuePair>. An Entry of this
 * map would then form a (Key, Type, Value) tuple. It exists to give allow
 * correct parsing of the value without requiring prior knowledge about what
 * the type should be, and to allow easy throwing of exceptions if something
 * is not right.
 * @author sudar.sam@gmail.com
 *
 */
public class TypeValuePair {
  
  public String type;
  // The value is a String representation of the value. The type this value
  // should be interpreted as is given by the type field.
  public String value;
  
  public TypeValuePair(String type, String value) {
    this.type = type;
    this.value = value;
  }

}
