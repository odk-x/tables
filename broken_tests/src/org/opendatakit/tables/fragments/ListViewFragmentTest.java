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

import static org.robolectric.Robolectric.shadowOf;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowWebView;

import android.app.Activity;
import android.app.FragmentManager;
import android.view.View;
import android.webkit.WebView;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class ListViewFragmentTest {
  
  ListViewFragment fragment;
  Activity activity;
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
  }
  
  @Before
  public void before() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();
    
    TestCaseUtils.setThreeTableDataset(true);
    ShadowLog.stream = System.out;
    String tableIdFile = ODKFileUtils.getTablesFolder(TestConstants.TABLES_DEFAULT_APP_NAME, TestConstants.DEFAULT_TABLE_ID) +
        File.separator + "testlisting.html";
    File tableHtmlFile = new File(tableIdFile);
    TestCaseUtils.assertFile(tableHtmlFile,"<html><head></head><body><p>This is a test file</p></body></html>"); 
        
    activity = ODKFragmentTestUtil.startListWebFragmentForTableDisplayActivity(
        TestConstants.DEFAULT_TABLE_ID, ODKFileUtils.asRelativePath(TestConstants.TABLES_DEFAULT_APP_NAME, tableHtmlFile));
    FragmentManager mgr = activity.getFragmentManager();
    fragment = (ListViewFragment) mgr.findFragmentByTag(ViewFragmentType.LIST.name());
  }
  
  @Test
  public void webViewHasControlInterface() {
    ShadowWebView shadow = this.getShadowWebViewFromFragment();
    Object controlInterface =
        shadow.getJavascriptInterface(Constants.JavaScriptHandles.CONTROL);
    org.fest.assertions.api.Assertions.assertThat(controlInterface)
      .isNotNull();
  }
  
  @Test
  public void webViewHasDataInterface() {
    ShadowWebView shadow = this.getShadowWebViewFromFragment();
    Object controlInterface =
        shadow.getJavascriptInterface(Constants.JavaScriptHandles.DATA);
    org.fest.assertions.api.Assertions.assertThat(controlInterface)
      .isNotNull();
  }
  
  private ShadowWebView getShadowWebViewFromFragment() {
    View v = this.fragment.getView();
    WebView webView = (WebView) v.findViewById(R.id.webkit);
    ShadowWebView result = shadowOf(webView);
    return result;
  }
  
  

}
