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
import org.opendatakit.testutils.TestConstants;

import android.webkit.WebView;

public class WebFragmentStub extends WebFragment {
  
  public static final WebView DEFAULT_WEB_VIEW =
      TestConstants.getWebViewMock();
  public static final String DEFAULT_FILE_NAME =
      TestConstants.DEFAULT_FILE_NAME;
  public static final Control DEFAULT_CONTROL =
      TestConstants.getControlMock();
  
  public static WebView WEB_VIEW = DEFAULT_WEB_VIEW;
  public static String FILE_NAME = DEFAULT_FILE_NAME;
  public static Control CONTROL = DEFAULT_CONTROL;
  
  public static void resetState() {
    WEB_VIEW = DEFAULT_WEB_VIEW;
    FILE_NAME = DEFAULT_FILE_NAME;
  }
  
  
}
