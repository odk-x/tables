/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.activities;

import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.testutils.TestConstants;

/**
 * Basic implementation of a TableActivity for testing. More complicated
 * table activity stubs should extend this one.
 * <p>
 * Set the static objects here as you need them, but be sure to reset them to
 * their default values.
 * @author sudar.sam@gmail.com
 *
 */
public class AbsTableActivityStub extends AbsTableActivity {
  
  // If modified during tests, the APP_NAME and TABLE_PROPERTIES objects should
  // be reset to these default values so that tests begin in a known state.
  public static final String DEFAULT_APP_NAME = 
      TestConstants.TABLES_DEFAULT_APP_NAME;
  public static final String DEFAULT_TABLE_ID = TestConstants.DEFAULT_TABLE_ID;
  public static final ViewFragmentType DEFAULT_FRAGMENT_TYPE =
      ViewFragmentType.SPREADSHEET;
  
  public static String APP_NAME = DEFAULT_APP_NAME;
  public static String TABLE_ID = DEFAULT_TABLE_ID;
  public static ViewFragmentType FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
  
  @Override
  String retrieveAppNameFromIntent() {
    return APP_NAME;
  }
  
  @Override
  String retrieveTableIdFromIntent() {
    return TABLE_ID;
  }
  
  /**
   * Reset the stub's state to the default values. Should be called after each
   * test modifying the object.
   */
  public static void resetState() {
    APP_NAME = DEFAULT_APP_NAME;
    TABLE_ID = DEFAULT_TABLE_ID;
  }

  public ViewFragmentType getCurrentFragmentType() {
    return FRAGMENT_TYPE;
  }

  @Override
  public void databaseAvailable() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void databaseUnavailable() {
    // TODO Auto-generated method stub
    
  }

}
