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

import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.views.webkits.TableData;
import org.opendatakit.testutils.TestConstants;

import android.webkit.WebView;

public class AbsWebTableFragmentStub extends AbsWebTableFragment {
  
  public static final WebView DEFAULT_WEB_VIEW = 
      TestConstants.getWebViewMock();
  public static final TableData DEFAULT_TABLE_DATA = 
      TestConstants.getTableDataMock();
  public static final String DEFAULT_FILE_NAME =
      TestConstants.DEFAULT_FILE_NAME;
  public static final ViewFragmentType DEFAULT_FRAGMENT_TYPE =
      ViewFragmentType.SPREADSHEET;
  
  public static WebView WEB_VIEW = DEFAULT_WEB_VIEW;
  public static String FILE_NAME = DEFAULT_FILE_NAME;
  public static ViewFragmentType FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
  public static TableData TABLE_DATA = DEFAULT_TABLE_DATA;
  
  public static void resetState() {
    WEB_VIEW = DEFAULT_WEB_VIEW;
    FILE_NAME = DEFAULT_FILE_NAME;
    FRAGMENT_TYPE = DEFAULT_FRAGMENT_TYPE;
    TABLE_DATA = DEFAULT_TABLE_DATA;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return FRAGMENT_TYPE;
  }

  @Override
  protected TableData createDataObject() {
    // TODO Auto-generated method stub
    return null;
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
