package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.RobolectricTestRunner;

import android.app.Activity;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class AbsBaseFragmentTest {
  
  AbsBaseFragmentStub fragment;
  Activity activity;
  
  @Before
  public void setup() {
    TestCaseUtils.setExternalStorageMounted();
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        AbsBaseActivityStub.class,
        stub,
        null);
    this.fragment = stub;
    this.activity = this.fragment.getActivity();
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    AbsBaseActivityStub.resetState();
  }
  
  @Test
  public void fragmentInitializesNonNull() {
    assertThat(this.fragment).isNotNull();
  }
  
  @Test(expected=IllegalStateException.class)
  public void activityOtherThanAbsBaseActivityThrowsIllegalState() {
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        Activity.class,
        stub,
        null);
  }
  
  @Test
  public void fragmentGetsAppNameFromParentActivity() {
    String targetAppName = "myFancyAppName";
    AbsBaseActivityStub.APP_NAME = targetAppName;
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        AbsBaseActivityStub.class,
        stub,
        null);
    String retrievedAppName = stub.getAppName();
    org.fest.assertions.api.Assertions.assertThat(retrievedAppName)
        .isNotNull()
        .isEqualTo(targetAppName);
  }

}
