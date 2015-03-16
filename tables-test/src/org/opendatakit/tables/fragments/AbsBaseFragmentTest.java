package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.RemoteException;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class AbsBaseFragmentTest {
  
  @Before
  public void setup() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();

    TestCaseUtils.setThreeTableDataset(true);
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    AbsBaseActivityStub.resetState();
  }
  
  @Test
  public void fragmentInitializesNonNull() {
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    Activity activity = ODKFragmentTestUtil.startActivityAttachFragment(
        AbsBaseActivityStub.class,
        stub, null,
        TestConstants.DEFAULT_FRAGMENT_TAG);
    FragmentManager mgr = activity.getFragmentManager();
    Fragment fragment = mgr.findFragmentByTag(TestConstants.DEFAULT_FRAGMENT_TAG);
    assertThat(fragment).isNotNull();
  }
  
  @Test(expected=IllegalStateException.class)
  public void activityOtherThanAbsBaseActivityThrowsIllegalState() {
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    Activity activity = ODKFragmentTestUtil.startActivityAttachFragment(
        Activity.class,
        stub, null,
        TestConstants.DEFAULT_FRAGMENT_TAG);
    FragmentManager mgr = activity.getFragmentManager();
    Fragment fragment = mgr.findFragmentByTag(TestConstants.DEFAULT_FRAGMENT_TAG);
  }
  
  @Test
  public void fragmentGetsAppNameFromParentActivity() {
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    Activity activity = ODKFragmentTestUtil.startActivityAttachFragment(
        AbsBaseActivityStub.class,
        stub, null,
        TestConstants.DEFAULT_FRAGMENT_TAG);
    FragmentManager mgr = activity.getFragmentManager();
    Fragment fragment = mgr.findFragmentByTag(TestConstants.DEFAULT_FRAGMENT_TAG);
    
    String retrievedAppName = stub.getAppName();
    org.fest.assertions.api.Assertions.assertThat(retrievedAppName)
        .isNotNull()
        .isEqualTo(TestConstants.TABLES_DEFAULT_APP_NAME);
  }

}
