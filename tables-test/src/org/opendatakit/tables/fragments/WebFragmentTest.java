package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.os.Bundle;

@RunWith(RobolectricTestRunner.class)
public class WebFragmentTest {
  
  WebFragmentStub fragment;
  Activity activity;
  
  @Before
  public void before() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();
    
    TestCaseUtils.setThreeTableDataset(true);
    ShadowLog.stream = System.out;
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
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
    this.activity = ODKFragmentTestUtil.startActivityAttachFragment(
        AbsBaseActivityStub.class,
        stub,
        null, null);
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

}
