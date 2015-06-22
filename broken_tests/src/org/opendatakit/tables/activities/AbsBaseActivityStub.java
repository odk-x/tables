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

import org.opendatakit.testutils.TestConstants;

public class AbsBaseActivityStub extends AbsBaseActivity {
  
  public static final String DEFAULT_APP_NAME = 
      TestConstants.TABLES_DEFAULT_APP_NAME;
    
  public static String APP_NAME = DEFAULT_APP_NAME;
    
  @Override
  String retrieveAppNameFromIntent() {
    return APP_NAME;
  }
  
  public static void resetState() {
    APP_NAME = DEFAULT_APP_NAME;
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
