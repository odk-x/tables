package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class TableDisplayActivityTest {

  TableDisplayActivity activity;

  @Before
  public void before() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();
    
    TestCaseUtils.setThreeTableDataset(true);

    ShadowLog.stream = System.out;
    TestCaseUtils.setExternalStorageMounted();
  }

  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    TableDisplayActivityStub.resetState();
  }

  /**
   * Set up the {@link TableDisplayActivityStub} to use the display fragment
   * specified by fragmentType. If buildDisplayFragment is true, it actually
   * builds the fragment. Supplies default mocks.
   * @param fragmentType
   * @param buildDisplayFragment
   */
  private void setupActivityWithViewType(
      ViewFragmentType fragmentType) {
    activity = ODKFragmentTestUtil.startFragmentForTableDisplayActivity(
        fragmentType, TestConstants.DEFAULT_TABLE_ID);
  }

  @Test
  public void activityIsCreatedSuccessfully() {
    setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    assertThat(this.activity).isNotNull();
  }

  /**
   * After adding a row with collect, the table backing the object should be
   * refreshed to ensure that any potential rows are added.
   */
  @Test
  public void backingTableIsRefereshedOnSuccessfulCollectAddReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.ADD_ROW_COLLECT,
        Activity.RESULT_OK,
        true);
  }

  @Test
  public void backingTableIsRefreshedOnSuccessfulSurveyAddReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.ADD_ROW_SURVEY,
        Activity.RESULT_OK,
        true);
  }

  @Test
  public void backingTableIsRefreshedOnSuccessfulCollectEditReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.EDIT_ROW_COLLECT,
        Activity.RESULT_OK,
        true);
  }

  @Test
  public void backingTableIsRefreshedOnSuccessfulSurveyEditReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.EDIT_ROW_SURVEY,
        Activity.RESULT_OK,
        true);
  }

  @Test
  public void backingTableIsNotRefreshedOnCanceledCollectAddReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.ADD_ROW_COLLECT,
        Activity.RESULT_CANCELED,
        false);
  }

  @Test
  public void backingTableIsRefreshedOnCanceledSurveyAddReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.ADD_ROW_SURVEY,
        Activity.RESULT_CANCELED,
        true);
  }

  @Test
  public void backingTableIsNotRefreshedOnCanceledCollectEditReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.EDIT_ROW_COLLECT,
        Activity.RESULT_CANCELED,
        false);
  }

  @Test
  public void backingTableIsRefreshedOnCanceledSurveyEditReturn() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.helperAssertRefreshedTableForReturn(
        Constants.RequestCodes.EDIT_ROW_SURVEY,
        Activity.RESULT_CANCELED,
        true);
  }

  /**
   * Performs assertions for the cases where you have returned launched an
   * activity and are checking if the {@link UserTable} backing the fragment
   * was refreshed. The activity it launches is just a meaningless stub
   * activity to try and keep things decoupled.
   * @param requestCode the request code you launched the intent with
   * @param resultCode the response the activity gives you
   * @param didRefresh true if the table should be different, false if it
   * should be the same
   */
  private void helperAssertRefreshedTableForReturn(
      int requestCode,
      int resultCode,
      boolean didRefresh) {
    UserTable startingUserTable = activity.getUserTable();
    Intent intent = new Intent(activity, AbsBaseActivityStub.class);
    activity.startActivityForResult(
        intent,
        requestCode);
    // Swap in a new user table that will be called when "retrieveUserTable"
    // is invoked.
    TestCaseUtils.setThreeTableDatasetReviseRow1();
    shadowOf(activity).receiveResult(
        intent,
        resultCode,
        new Intent());
    if (didRefresh) {
      org.fest.assertions.api.Assertions.assertThat(startingUserTable)
        .isNotSameAs(activity.getUserTable());
    } else {
      org.fest.assertions.api.Assertions.assertThat(startingUserTable)
        .isSameAs(activity.getUserTable());
    }
  }

  @Test
  public void childrenVisibilityCorrectForSpreadsheet() {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    this.assertOnePaneViewItemsCorrectVisibility();
  }

  @Test
  public void childrenVisibilityCorrectForList() {
    this.setupActivityWithViewType(ViewFragmentType.LIST);
    this.assertOnePaneViewItemsCorrectVisibility();
  }

  @Test
  public void childrenVisibilityCorrectForDetail() {
    activity = ODKFragmentTestUtil.startDetailWebFragmentForTableDisplayActivity(TestConstants.DEFAULT_TABLE_ID, 
        TestConstants.DEFAULT_FILE_NAME, TestConstants.ROWID_1);
    this.assertOnePaneViewItemsCorrectVisibility();
  }

  @Test
  public void childrenVisibilityCorrectForGraphManager() {
    this.setupActivityWithViewType(ViewFragmentType.GRAPH_MANAGER);
    this.assertOnePaneViewItemsCorrectVisibility();
  }

  @Test
  public void childrenVisibilityCorrectForGraphView() {
    this.setupActivityWithViewType(ViewFragmentType.GRAPH_VIEW);
    this.assertOnePaneViewItemsCorrectVisibility();
  }

  @Test
  public void childrenVisibilityCorrectForMap() {
    this.setupActivityWithViewType(ViewFragmentType.MAP);
    this.assertMapPaneItemsCorrectVisibility();
  }

  @Test
  public void menuItemsCorrectForSpreadsheet() {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.SPREADSHEET);
  }

  @Test
  public void menuItemsCorrectForSpreadsheetAfterRecreation() {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    this.activity.recreate();
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.SPREADSHEET);
  }

  @Test
  public void menuItemsCorrectForList() {
    this.setupActivityWithViewType(ViewFragmentType.LIST);
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.LIST);
  }

  @Test
  public void menuItemsCorrectForListAfterRecreation() {
    this.setupActivityWithViewType(ViewFragmentType.LIST);
    this.activity.recreate();
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.LIST);
  }

  @Test
  public void menuItemsCorrectForGraphManager() {
    this.setupActivityWithViewType(ViewFragmentType.GRAPH_MANAGER);
    this.assertOptionsMenuCorrectForTopLevelView(
        ViewFragmentType.GRAPH_MANAGER);
  }

  @Test
  public void menuItemsCorrectForGraphManagerAfterRecreation() {
    this.setupActivityWithViewType(ViewFragmentType.GRAPH_MANAGER);
    this.activity.recreate();
    this.assertOptionsMenuCorrectForTopLevelView(
        ViewFragmentType.GRAPH_MANAGER);
  }

  @Test
  public void menuItemsCorrectForMap() {
    this.setupActivityWithViewType(ViewFragmentType.MAP);
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.MAP);
  }

  @Test
  public void menutItemsCorrectForMapAfterRecreation() {
    this.setupActivityWithViewType(ViewFragmentType.MAP);
    this.activity.recreate();
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.MAP);
  }

  @Test
  public void checkedMenuItemChangesToListFromSpreadsheet() throws RemoteException {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    // make sure it's correct to begin with.
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.SPREADSHEET);
    this.activity.showListFragment();
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.LIST);
  }

  @Test
  public void checkedMenuItemChangesToMapFromSpreadsheet() throws RemoteException {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    // make sure it's correct to begin with.
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.SPREADSHEET);
    this.activity.showMapFragment();
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.MAP);
  }

  @Test
  public void checkedMenuItemChangesToGraphFromSpreadsheet() {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    // make sure it's correct to begin with.
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.SPREADSHEET);
    this.activity.showGraphFragment();
    this.assertOptionsMenuCorrectForTopLevelView(
        ViewFragmentType.GRAPH_MANAGER);
  }

  @Test
  public void checkedMenuItemChangesToSpreadsheetFromList() {
    this.setupActivityWithViewType(ViewFragmentType.LIST);
    // make sure it's correct to begin with.
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.LIST);
    this.activity.showSpreadsheetFragment();
    this.assertOptionsMenuCorrectForTopLevelView(ViewFragmentType.SPREADSHEET);
  }



  /**
   * Asserts that the options menu is correct for a top level view like list or
   * spreadsheet. I.e. asserts that you have the new content, select view, and
   * preferences menu options only.
   * @param checkedItem the view type that should be checked
   */
  private void assertOptionsMenuCorrectForTopLevelView(
      ViewFragmentType checkedItem) {
    ShadowActivity shadow = shadowOf(this.activity);
    Menu optionsMenu = shadow.getOptionsMenu();
    assertThat(optionsMenu).hasSize(3);
    MenuItem addItem = optionsMenu.findItem(R.id.top_level_table_menu_add);
    MenuItem selectViewItem =
        optionsMenu.findItem(R.id.top_level_table_menu_select_view);
    MenuItem tablePropertiesItem =
        optionsMenu.findItem(R.id.top_level_table_menu_table_properties);
    assertThat(addItem)
      .isNotNull()
      .isVisible();
    assertThat(selectViewItem)
      .isNotNull()
      .isVisible();
    assertThat(tablePropertiesItem)
      .isNotNull()
      .isVisible();
    switch (checkedItem) {
    case SPREADSHEET:
      this.assertCorrectItemsChecked(true, false, false, false);
      break;
    case LIST:
      this.assertCorrectItemsChecked(false, true, false, false);
      break;
    case GRAPH_MANAGER:
      this.assertCorrectItemsChecked(false, false, true, false);
      break;
    case MAP:
      this.assertCorrectItemsChecked(false, false, false, true);
      break;
    case DETAIL:
    case GRAPH_VIEW:
      // Do nothing, shouldn't be present at all.
      break;
    default:
      throw new IllegalArgumentException(
          "Unrecognized view type: " + checkedItem);
    }
  }

  @Test
  public void menuHasAdd() {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    ShadowActivity shadowActivity = shadowOf(activity);
    Menu menu = shadowActivity.getOptionsMenu();
    MenuItem addItem = menu.findItem(R.id.top_level_table_menu_add);
    assertThat(addItem).isNotNull();
  }

  @Test
  public void menuHasSelectView() {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    MenuItem selectView = this.getSelectViewItem();
    assertThat(selectView).isNotNull();
  }

  @Test
  public void selectViewIsSubMenu() {
    this.setupActivityWithViewType(ViewFragmentType.SPREADSHEET);
    MenuItem selectView = this.getSelectViewItem();
    // The submenu shouldn't return null if it is a submenu, as it should be.
    assertThat(selectView.getSubMenu()).isNotNull();
  }

  /**
   * Get the {@link MenuItem} for selecting a view.
   * @return
   */
  MenuItem getSelectViewItem() {
    ShadowActivity shadowActivity = shadowOf(activity);
    Menu menu = shadowActivity.getOptionsMenu();
    MenuItem selectView =
        menu.findItem(R.id.top_level_table_menu_select_view);
    return selectView;
  }

  /**
   * Convenience method for calling {@link Menu#findItem(int)} on
   * {@link #menu}.
   * @param itemId
   * @return
   */
  MenuItem getMenuItemWithId(int itemId) {
    ShadowActivity shadowActivity = shadowOf(activity);
    Menu menu = shadowActivity.getOptionsMenu();
    return menu.findItem(itemId);
  }


  private void assertCorrectItemsChecked(
      boolean spreadsheetChecked,
      boolean listChecked,
      boolean graphChecked,
      boolean mapChecked) {
    ShadowActivity shadow = shadowOf(this.activity);
    Menu optionsMenu = shadow.getOptionsMenu();
    MenuItem spreadsheet =
        optionsMenu.findItem(R.id.top_level_table_menu_view_spreadsheet_view);
    MenuItem list =
        optionsMenu.findItem(R.id.top_level_table_menu_view_list_view);
    MenuItem graph =
        optionsMenu.findItem(R.id.top_level_table_menu_view_graph_view);
    MenuItem map =
        optionsMenu.findItem(R.id.top_level_table_menu_view_map_view);
    assertThat(spreadsheet).isNotNull();
    assertThat(list).isNotNull();
    assertThat(graph).isNotNull();
    assertThat(map).isNotNull();
    if (spreadsheetChecked) {
      assertThat(spreadsheet).isChecked();
    } else {
      assertThat(spreadsheet).isNotChecked();
    }
    if (listChecked) {
      assertThat(list).isChecked();
    } else {
      assertThat(list).isNotChecked();
    }
    if (graphChecked) {
      assertThat(graph).isChecked();
    } else {
      assertThat(graph).isNotChecked();
    }
    if (mapChecked) {
      assertThat(map).isChecked();
    } else {
      assertThat(map).isNotChecked();
    }
  }

  private void assertMapPaneItemsCorrectVisibility() {
    this.assertViewVisibility(false, true);
  }

  private void assertOnePaneViewItemsCorrectVisibility() {
    this.assertViewVisibility(true, false);
  }

  private void assertViewVisibility(
      boolean onePaneVisible,
      boolean mapContainerVisible) {
    ShadowActivity shadowActivity = shadowOf(this.activity);
    View contentView = shadowActivity.getContentView();
    View onePaneView = contentView.findViewById(
        R.id.activity_table_display_activity_one_pane_content);
    View mapHolder = contentView.findViewById(
        R.id.activity_table_display_activity_map_content);
    View mapListView = contentView.findViewById(R.id.map_view_list);
    View mapInnerMap = contentView.findViewById(R.id.map_view_inner_map);
    assertThat(onePaneView).isNotNull();
    assertThat(mapHolder).isNotNull();
    assertThat(mapListView).isNotNull();
    assertThat(mapInnerMap).isNotNull();
    if (onePaneVisible) {
      assertThat(onePaneView).isVisible();
    } else {
      assertThat(onePaneView).isGone();
    }
    if (mapContainerVisible) {
      assertThat(mapHolder).isVisible();
    } else {
      assertThat(mapHolder).isGone();
    }
  }

}
