package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Robolectric.shadowOf;
import static org.robolectric.util.FragmentTestUtil.startFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import android.app.Activity;
import android.view.Menu;
import android.view.View;

@RunWith(RobolectricTestRunner.class)
public class TableManagerFragmentTest {
  
  private TableManagerFragment fragment;
  private Activity parentActivity;
  
  @Before
  public void setup() {
    ShadowLog.stream = System.out;
    // We need external storage available for accessing the database.
    TestCaseUtils.setExternalStorageMounted();
    TableManagerFragment spy = spy(new TableManagerFragment());
    doReturn(new TableProperties[0])
        .when(spy)
        .retrieveContentsToDisplay();
    this.fragment = spy;
    startFragment(this.fragment);
    this.parentActivity = this.fragment.getActivity();
    // Have to call visible to get the fragment to think its been attached to
    // a window.
    ActivityController.of(this.parentActivity).visible();
  }
  
  @Test
  public void emptyViewIsVisible() {
    // We aren't retrieving any TableProperties, so it is empty.
    // Weirdly, the List is also visible. Perhaps this is because the list view
    // is always visible, just not taking up any screen real estate if there
    // are no elements? Should investigate this when we have known elements.
    View emptyView = this.fragment.getView().findViewById(android.R.id.empty);
    assertThat(emptyView).isVisible();
  }
  
  @Test
  public void listViewIsGone() {
    View listView = this.fragment.getView().findViewById(android.R.id.list);
    assertThat(listView).isGone();
  }
  
  @Test
  public void hasCorrectMenuItems() {
    ShadowActivity shadowActivity = shadowOf(parentActivity);
    Menu menu = shadowActivity.getOptionsMenu();
    assertThat(menu)
      .hasSize(4)
      .hasItem(R.id.menu_table_manager_export)
      .hasItem(R.id.menu_table_manager_import)
      .hasItem(R.id.menu_table_manager_sync)
      .hasItem(R.id.menu_table_manager_preferences);
  }


}
