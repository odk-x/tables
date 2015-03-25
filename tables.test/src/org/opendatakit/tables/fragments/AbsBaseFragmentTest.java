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

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;

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
