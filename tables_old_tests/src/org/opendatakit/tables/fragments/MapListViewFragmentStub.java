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

package org.opendatakit.tables.fragments;

import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;
import org.opendatakit.testutils.TestConstants;

public class MapListViewFragmentStub extends MapListViewFragment {
  
  public static TableData TABLE_DATA = TestConstants.getTableDataMock();
  public static Control CONTROL = TestConstants.getControlMock();
  public static String FILE_NAME = TestConstants.DEFAULT_FILE_NAME;
  
  public static void resetState() {
    TABLE_DATA = TestConstants.getTableDataMock();
    CONTROL = TestConstants.getControlMock();
    FILE_NAME = TestConstants.DEFAULT_FILE_NAME;
  }
  
  @Override
  public Control createControlObject() {
    return CONTROL;
  }
  
  @Override
  protected TableData createDataObject() {
    return TABLE_DATA;
  }
  
  @Override
  public String getFileName() {
    return FILE_NAME;
  }

}
