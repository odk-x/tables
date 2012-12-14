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
 * The type of a table. Data is user data. Security is access control stuff.
 * Shortcut is sms shortcuts.
 * <p>
 * The lower case names of constants are for javascript reasons and the ability
 * to simply call TableType.valueOf("data") when you read an object from the 
 * database.
 * @author sudar.sam@gmail.com
 *
 */
public enum TableType {
  data,
  security,
  shortcut;

}
