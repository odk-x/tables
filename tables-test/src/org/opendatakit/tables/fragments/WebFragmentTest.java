package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

@RunWith(RobolectricTestRunner.class)
public class WebFragmentTest {
  
  WebFragmentStub fragment;
  Activity activity;
  
  @Before
  public void setup() {
    ShadowLog.stream = System.out;
  }
  
  @After
  public void after() {
    WebFragmentStub.resetState();
  }
  
  private void setupFragmentWithFileName(String fileName) {
    WebFragmentStub stub = new WebFragmentStub();
    Bundle bundle = new Bundle();
    IntentUtil.addFileNameToBundle(bundle, fileName);
    stub.setArguments(bundle);
    this.doGlobalSetup(stub);
  }
  
  private void doGlobalSetup(WebFragmentStub stub) {
    this.fragment = stub;
    ODKFragmentTestUtil.startFragmentForActivity(
        AbsBaseActivityStub.class,
        stub,
        null);
    this.activity = this.fragment.getActivity();    
  }
  
  @Test
  public void fragmentInitializesNonNull() {
    this.setupFragmentWithFileName("testFileName");
    assertThat(this.fragment).isNotNull();
  }
  
  @Test
  public void fragmentStoresCorrectFileNameInArguments() {
    String target = "test/path/to/file";
    this.setupFragmentWithFileName(target);
    org.fest.assertions.api.Assertions.assertThat(this.fragment.getFileName())
        .isEqualTo(target);
  }
  
  @Test
  public void setsTheViewReturnedByBuildView() {
    WebView mockWebView = TestConstants.getWebViewMock();
    WebFragmentStub.WEB_VIEW = mockWebView;
    this.setupFragmentWithFileName("testFileName");
    View fragmentView = this.fragment.getView();
    assertThat(fragmentView).isSameAs(mockWebView);
  }

}
