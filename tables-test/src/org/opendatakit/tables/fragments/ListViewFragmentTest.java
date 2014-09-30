package org.opendatakit.tables.fragments;

import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowWebView;

import android.app.Activity;
import android.webkit.WebView;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class ListViewFragmentTest {
  
  ListViewFragmentStub fragment;
  Activity activity;
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    ListViewFragmentStub.resetState();
  }
  
  @Before
  public void before() {
    TestCaseUtils.setThreeTableDataset();
    TestCaseUtils.setExternalStorageMounted();
    ShadowLog.stream = System.out;
    TableDisplayActivityStub.BUILD_MENU_FRAGMENT = false;
    ListViewFragmentStub stub = new ListViewFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        TableDisplayActivityStub.class,
        stub,
        null);
    this.fragment = stub;
    this.activity = this.fragment.getActivity();
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
    WebView webView = (WebView) this.fragment.getView();
    ShadowWebView result = shadowOf(webView);
    return result;
  }
  
  

}
