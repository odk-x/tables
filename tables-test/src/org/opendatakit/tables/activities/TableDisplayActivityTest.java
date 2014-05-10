package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.fragments.DetailViewFragment;
import org.opendatakit.tables.fragments.GraphManagerFragment;
import org.opendatakit.tables.fragments.GraphViewFragment;
import org.opendatakit.tables.fragments.ListViewFragment;
import org.opendatakit.tables.fragments.MapListViewFragment;
import org.opendatakit.tables.fragments.SpreadsheetFragment;
import org.opendatakit.tables.fragments.TableMapInnerFragment;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
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
    ShadowLog.stream = System.out;
    this.activity = Robolectric.buildActivity(TableDisplayActivityStub.class)
        .create()
        .start()
        .resume()
        .visible()
        .get();
  }
  
  @After
  public void after() {
    TableDisplayActivityStub.resetState();
  }
  
  /**
   * Set up the {@link TableDisplayActivityStub} to use the display fragment
   * specified by fragmentType. If buildDisplayFragment is true, it actually
   * builds the fragment. Supplies default mocks.
   * @param fragmentType
   * @param buildDisplayFragment
   */
  private void setupActivityWithViewTypeAndMock(
      ViewFragmentType fragmentType,
      boolean buildDisplayFragment) {
    Bundle extras = new Bundle();
    Intent intent =
        new Intent(
            Robolectric.application.getApplicationContext(),
            TableDisplayActivityStub.class);
    IntentUtil.addFragmentViewTypeToBundle(extras, fragmentType);
    intent.putExtras(extras);
    // Now handle the internal static state we're going to need.
    TableDisplayActivityStub.BUILD_DISPLAY_FRAGMENT = true;
    TableDisplayActivityStub.SPREADSHEET_FRAGMENT = 
        mock(SpreadsheetFragment.class);
    TableDisplayActivityStub.LIST_VIEW_FRAGMENT =
        mock(ListViewFragment.class);
    TableDisplayActivityStub.DETAIL_VIEW_FRAGMENT =
        mock(DetailViewFragment.class);
    TableDisplayActivityStub.GRAPH_MANAGER_FRAGMENT =
        mock(GraphManagerFragment.class);
    TableDisplayActivityStub.GRAPH_VIEW_FRAGMENT =
        mock(GraphViewFragment.class);
    TableDisplayActivityStub.MAP_INNER_FRAGMENT =
        mock(TableMapInnerFragment.class);
    TableDisplayActivityStub.MAP_LIST_VIEW_FRAGMENT =
        mock(MapListViewFragment.class);
    this.activity = Robolectric.buildActivity(TableDisplayActivityStub.class)
        .withIntent(intent)
        .create()
        .start()
        .resume()
        .visible()
        .get();
  }
  
  @Test
  public void activityIsCreatedSuccessfully() {
    assertThat(this.activity).isNotNull();
  }
  
  @Test
  public void menuFragmentIsNotNull() {
    FragmentManager fragmentManager = this.activity.getFragmentManager();
    TopLevelTableMenuFragment menuFragment = (TopLevelTableMenuFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.TABLE_MENU);
    assertThat(menuFragment).isNotNull();
  }
  
  @Test
  public void childrenVisibilityCorrectForSpreadsheet() {
    this.setupActivityWithViewTypeAndMock(ViewFragmentType.SPREADSHEET, true);
    this.assertOnePaneViewItemsCorrectVisibility();
  }
  
  @Test
  public void childrenVisibilityCorrectForList() {
    this.setupActivityWithViewTypeAndMock(ViewFragmentType.LIST, true);
    this.assertOnePaneViewItemsCorrectVisibility();
  }
  
  @Test
  public void childrenVisibilityCorrectForDetail() {
    this.setupActivityWithViewTypeAndMock(ViewFragmentType.DETAIL, true);
    this.assertOnePaneViewItemsCorrectVisibility();
  }
  
  @Test
  public void childrenVisibilityCorrectForGraphManager() {
    this.setupActivityWithViewTypeAndMock(
        ViewFragmentType.GRAPH_MANAGER,
        true);
    this.assertOnePaneViewItemsCorrectVisibility();
  }
  
  @Test
  public void childrenVisibilityCorrectForGraphView() {
    this.setupActivityWithViewTypeAndMock(ViewFragmentType.GRAPH_VIEW, true);
    this.assertOnePaneViewItemsCorrectVisibility();
  }
  
  @Test
  public void childrenVisibilityCorrectForMap() {
    this.setupActivityWithViewTypeAndMock(ViewFragmentType.MAP, true);
    this.assertMapPaneItemsCorrectVisibility();
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
