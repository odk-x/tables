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
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.FragmentManager;

@RunWith(RobolectricTestRunner.class)
public class AbsWebTableFragmentTest {
  
  DetailViewFragment fragment;
  TableDisplayActivity activity;
  
  @Before
  public void before() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();
    
    TestCaseUtils.setThreeTableDataset(true);
    ShadowLog.stream = System.out;
    TableDisplayActivityStub.BUILD_MENU_FRAGMENT = false;
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    TableDisplayActivityStub.resetState();
  }
  
  private void setupFragmentWithDefaultFileName() {
    setupFragmentWithFileName(TestConstants.DEFAULT_FILE_NAME);
  }
  
  private void setupFragmentWithFileName(String fileName) {
    this.doGlobalSetup(fileName);
  }
  
  private void doGlobalSetup(String fileName) {
    this.activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(
        TestConstants.DEFAULT_TABLE_ID,
        fileName,
        TestConstants.ROWID_1
        );
    FragmentManager mgr = activity.getFragmentManager();
    String fragTag = TableDisplayActivity.getFragmentTag(ViewFragmentType.DETAIL);
    this.fragment = (DetailViewFragment) mgr.findFragmentByTag(fragTag);
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
  
}
