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
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.MainActivity.ScreenType;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestContextMenu;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;
import org.robolectric.shadows.ShadowLog;

import android.app.FragmentManager;
import android.content.ComponentName;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.RelativeLayout;

@RunWith(RobolectricTestRunner.class)
public class TableManagerFragmentTest {

  String mockTableName1 = "alpha";
  String mockTableName2 = "beta";
  String mockTableId1 = "firstTableId";
  String mockTableId2 = "secondTableId";
  String mockTableName3 = "gamma";
  String mockTableId3 = "thirdTableId";

  private TableManagerFragment fragment;
  private MainActivity parentActivity;
  
  @Before
  public void before() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();
  }

  @After
  public void after() {
    AbsBaseActivityStub.resetState();
    TestCaseUtils.resetExternalStorageState();
  }

  public void setupFragmentWithNoItems() {
    TestCaseUtils.setNoTableDataset();
    doGlobalSetup();
  }

  public void setupFragmentWithTwoItems() {
    TestCaseUtils.setTwoTableDataset();
    doGlobalSetup();
  }

  /**
   * Does the setup required regardless of what the fragment is returning.
   */
  public void doGlobalSetup() {
    ShadowLog.stream = System.out;
    // We need external storage available for accessing the database.
    TestCaseUtils.setExternalStorageMounted();
    this.parentActivity = ODKFragmentTestUtil.startMainActivity(null);
    parentActivity.swapScreens(ScreenType.TABLE_MANAGER_SCREEN);
    FragmentManager mgr = this.parentActivity.getFragmentManager();
    this.fragment = (TableManagerFragment) mgr.findFragmentByTag(ScreenType.TABLE_MANAGER_SCREEN.name());
  }

  @Test
  public void emptyViewIsVisibleWithoutContent() {
    setupFragmentWithNoItems();
    // We aren't retrieving any TableProperties, so it is empty.
    // Weirdly, the List is also visible. Perhaps this is because the list view
    // is always visible, just not taking up any screen real estate if there
    // are no elements? Should investigate this when we have known elements.
    View emptyView = this.fragment.getView().findViewById(android.R.id.empty);
    assertThat(emptyView).isVisible();
  }

  @Test
  public void listViewIsGoneWithoutContent() {
    setupFragmentWithNoItems();
    View listView = this.fragment.getView().findViewById(android.R.id.list);
    assertThat(listView).isGone();
  }

  @Test
  public void emptyViewIsGoneWithContent() {
    setupFragmentWithTwoItems();
    View emptyView = this.fragment.getView().findViewById(android.R.id.empty);
    assertThat(emptyView).isGone();
  }

  @Test
  public void listViewIsVisibleWithContent() {
    setupFragmentWithTwoItems();
    View listView = this.fragment.getView().findViewById(android.R.id.list);
    assertThat(listView).isVisible();
  }

  @Test
  public void hasCorrectMenuItems() {
    setupFragmentWithNoItems();
    ShadowActivity shadowActivity = shadowOf(parentActivity);
    Menu menu = shadowActivity.getOptionsMenu();
    assertThat(menu)
      .hasSize(5)
      .hasItem(R.id.menu_table_manager_export)
      .hasItem(R.id.menu_table_manager_import)
      .hasItem(R.id.menu_table_manager_sync)
      .hasItem(R.id.menu_table_manager_preferences)
      .hasItem(R.id.menu_table_about);
  }

  @Test
  public void contextMenuHasCorrectItems() {
    setupFragmentWithTwoItems();
    ContextMenu contextMenu = new TestContextMenu();
    this.fragment.onCreateContextMenu(contextMenu, null, null);
    assertThat(contextMenu)
        .hasSize(2)
        .hasItem(R.id.table_manager_delete_table)
        .hasItem(R.id.table_manager_edit_table_properties);
  }

  @Test
  public void onItemClickLaunchesTableDisplayActivityWithCorrectIntent() {
    int position = 0;
    this.setupFragmentWithTwoItems();
    View firstItemView = this.fragment.getListAdapter().getView(
        position,
        null,
        new RelativeLayout(this.parentActivity));
    assertThat(firstItemView).isNotNull();
    this.fragment.getListView().performItemClick(
        firstItemView,
        position,
        this.fragment.getListAdapter().getItemId(position));
    ShadowActivity shadowActivity = shadowOf(this.parentActivity);
    IntentForResult intent = shadowActivity.peekNextStartedActivityForResult();
    ComponentName target = new ComponentName(
        this.parentActivity,
        TableDisplayActivity.class);
    ComponentName intentComponent = intent.intent.getComponent();
    org.fest.assertions.api.Assertions.assertThat(intentComponent)
        .isNotNull()
        .isEqualTo(target);
  }

  @Test
  public void listResetsCorrectlyAfterUpdating() {
    this.setupFragmentWithTwoItems();
    // make sure it's ready immediately.
    org.fest.assertions.api.Assertions.assertThat(
        this.fragment.getListAdapter().getCount())
        .isEqualTo(2);
    org.fest.assertions.api.Assertions.assertThat(
        this.fragment.getListView().getAdapter().getCount())
        .isEqualTo(2);
    TestCaseUtils.setThreeTableDataset(true);
    this.fragment.databaseAvailable();
    // this.fragment.setList(threeItemList);
    org.fest.assertions.api.Assertions.assertThat(
        this.fragment.getListAdapter().getCount())
        .isEqualTo(3);
    org.fest.assertions.api.Assertions.assertThat(
        this.fragment.getListView().getAdapter().getCount())
        .isEqualTo(3);
  }

  @Test
  public void tableListIsRefreshedOnReturnFromSync() {
    this.setupFragmentWithTwoItems();
//    org.fest.assertions.api.Assertions.assertThat(
//        this.fragment.getNumberOfCallsToUpdatePropertiesList())
//        .isEqualTo(1);
//    this.fragment.onActivityResult(
//        Constants.RequestCodes.LAUNCH_SYNC,
//        Activity.RESULT_OK,
//        null);
//    org.fest.assertions.api.Assertions.assertThat(
//        this.fragment.getNumberOfCallsToUpdatePropertiesList())
//        .isEqualTo(2);
  }

  // TODO: Should probably also test that the context menu creates a dialog,
  // but it's not clear how to gain access to it to test.

  // TODO: there should really be a "long-pressing creates a context menu"
  // test, but I'm not sure how to do it.


}
