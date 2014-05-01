package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;
import static org.robolectric.util.FragmentTestUtil.startFragment;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestContextMenu;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.tester.android.view.TestMenuItem;
import org.robolectric.util.ActivityController;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;

@RunWith(RobolectricTestRunner.class)
public class TableManagerFragmentTest {
  
  String mockTableName1 = "alpha";
  String mockTableName2 = "beta";
  
  private TableManagerFragment fragment;
  private Activity parentActivity;
  
  public void setupFragmentWithNoItems() {
    this.fragment = getSpy(new ArrayList<TableProperties>());
    doGlobalSetup();
  }
  
  public void setupFragmentWithTwoItems() {
    TableProperties tp1 = mock(TableProperties.class);
    TableProperties tp2 = mock(TableProperties.class);
    when(tp1.getDisplayName()).thenReturn(mockTableName1);
    when(tp2.getDisplayName()).thenReturn(mockTableName2);
    List<TableProperties> listOfMocks = new ArrayList<TableProperties>();
    listOfMocks.add(tp1);
    listOfMocks.add(tp2);
    this.fragment = getSpy(listOfMocks);
    doGlobalSetup();
  }
  
  /**
   * Does the setup required regardless of what the fragment is returning.
   */
  public void doGlobalSetup() {
    ShadowLog.stream = System.out;
    // We need external storage available for accessing the database.
    TestCaseUtils.setExternalStorageMounted();
    startFragment(this.fragment);
    this.parentActivity = this.fragment.getActivity();
    // Have to call visible to get the fragment to think its been attached to
    // a window.
    ActivityController.of(this.parentActivity).visible();
  }
    
  /**
   * Get a mocked TableManagerFragment that will return toDisplay when asked to
   * retrieve TableProperties.
   * @param toDisplay
   * @return
   */
  private TableManagerFragment getSpy(List<TableProperties> toDisplay) {
    TableManagerFragment stub = new TableManagerFragmentStub(toDisplay);
    return stub;
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
      .hasSize(4)
      .hasItem(R.id.menu_table_manager_export)
      .hasItem(R.id.menu_table_manager_import)
      .hasItem(R.id.menu_table_manager_sync)
      .hasItem(R.id.menu_table_manager_preferences);
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
  public void clickingDeleteTableCreatesDialog() {
    setupFragmentWithTwoItems();
    ContextMenu contextMenu = new TestContextMenu();
    ListView listView = this.fragment.getListView();
    View view1 = listView.getAdapter().getView(
        1,
        null,
        new LinearLayout(Robolectric.application));
//    int firstPosition = listView.getFirstVisiblePosition();
//    View view = listView.getChildAt(firstPosition);
//    listView.showContextMenuForChild(view);
//    shadowListView.
//    AdapterContextMenuInfo menuInfo = new AdapterContextMenuInfo(
//         view,
//         0,
//         1);
//    this.fragment.onCreateContextMenu(contextMenu, listView, menuInfo);
    view1.createContextMenu(contextMenu);
    assertThat(contextMenu).hasSize(2);
    MenuItem deleteItem = 
        contextMenu.findItem(R.id.table_manager_delete_table);
    TestMenuItem tmi = new TestMenuItem();
    assertThat(deleteItem).isNotNull();
    this.fragment.onContextItemSelected(deleteItem);
    // This should have created a dialog.
    AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
    assertThat(dialog).isNotNull();
  }
  
  // TODO: there should really be a "long-pressing creates a context menu"
  // test, but I'm not sure how to do it.


}
