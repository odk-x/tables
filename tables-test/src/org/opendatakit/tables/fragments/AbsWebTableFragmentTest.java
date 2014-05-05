package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.views.webkits.CustomView;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

@RunWith(RobolectricTestRunner.class)
public class AbsWebTableFragmentTest {
  
  AbsWebTableFragmentStub fragment;
  Activity activity;
  
  @Before
  public void setup() {
    ShadowLog.stream = System.out;
    TableDisplayActivityStub.BUILD_MENU_FRAGMENT = false;
  }
  
  @After
  public void after() {
    TableDisplayActivityStub.resetState();
  }
  
  private void setupFragmentWithFileName(String fileName) {
    AbsWebTableFragmentStub stub = new AbsWebTableFragmentStub();
    Bundle bundle = new Bundle();
    bundle.putString(Constants.IntentKeys.FILE_NAME, fileName);
    stub.setArguments(bundle);
    this.doGlobalSetup(stub);
  }
  
  private void doGlobalSetup(AbsWebTableFragmentStub stub) {
    this.fragment = stub;
    ODKFragmentTestUtil.startFragmentForTableActivity(
        TableDisplayActivityStub.class,
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
  public void fragmentStoresCorrectFileName() {
    String target = "test/path/to/file";
    this.setupFragmentWithFileName(target);
    org.fest.assertions.api.Assertions.assertThat(this.fragment.getFileName())
        .isEqualTo(target);
  }
  
  @Test
  public void setsTheViewReturnedByBuildCustomView() {
    CustomView mockCustomView = mock(CustomView.class);
    AbsWebTableFragmentStub.CUSTOM_VIEW = mockCustomView;
    this.setupFragmentWithFileName("testFileName");
    View fragmentView = this.fragment.getView();
    assertThat(fragmentView).isSameAs(mockCustomView);
  }

}
