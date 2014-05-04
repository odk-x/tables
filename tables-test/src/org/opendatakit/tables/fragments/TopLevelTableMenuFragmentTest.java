package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivityStub;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment.ITopLevelTableMenuActivity;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class TopLevelTableMenuFragmentTest {
  
  TopLevelTableMenuFragmentStub fragment;
  Activity activity;
  Menu menu;
  
  private void setupStateWithDefaults() {
    this.setupState(new TopLevelTableMenuFragmentStub());
  }
  
  /**
   * Set up the fragment with the interface that returns fragmentType from
   * {@link ITopLevelTableMenuActivity#getCurrentFragmentType()}.
   * @param fragmentType
   */
  private void setupStateWithCurrentFragment(ViewFragmentType fragmentType) {
    ITopLevelTableMenuActivity implMock = 
        mock(ITopLevelTableMenuActivity.class);
    doReturn(fragmentType).when(implMock).getCurrentFragmentType();
    TopLevelTableMenuFragmentStub.MENU_ACTIVITY_IMPL = implMock;
    this.setupState(new TopLevelTableMenuFragmentStub());
  }
  
  private void setupStateWithCustomPossibleViewTypes(
      PossibleTableViewTypes viewTypes) {
    TopLevelTableMenuFragmentStub stub = this.getStubWithViewTypes(viewTypes);
    this.setupState(stub);
  }
  
  private void setupState(TopLevelTableMenuFragmentStub fragmentStub) {
    this.fragment = fragmentStub;
    ODKFragmentTestUtil.startFragmentForTableActivity(
        AbsTableActivityStub.class,
        this.fragment,
        null);
    this.activity = this.fragment.getActivity();
    this.menu = shadowOf(this.activity).getOptionsMenu();
  }
  
  @Before
  public void setup() {
    ShadowLog.stream = System.out;
  }
  
  /**
   * Get a stub for testing. 
   * {@link TopLevelTableMenuFragmentStub#getPossibleViewTypes()} will return
   * the viewTypes passed in, unless it is null, in which case the default
   * value will be used.
   * @param viewTypes
   * @return
   */
  private TopLevelTableMenuFragmentStub getStubWithViewTypes(
      PossibleTableViewTypes viewTypes) {
    if (viewTypes != null) {
      TopLevelTableMenuFragmentStub.POSSIBLE_VIEW_TYPES = viewTypes;
    }
    return new TopLevelTableMenuFragmentStub();
  }
  
  @After
  public void after() {
    TableDisplayActivityStub.resetState();
    TopLevelTableMenuFragmentStub.resetState();
  }
  
  @Test
  public void assertNotNull() {
    this.setupStateWithDefaults();
    assertThat(this.fragment).isNotNull();
  }
  
  @Test
  public void menuHasAdd() {
    this.setupStateWithDefaults();
    MenuItem addItem = this.menu.findItem(R.id.top_level_table_menu_add);
    assertThat(addItem).isNotNull();
  }
  
  @Test
  public void menuHasSelectView() {
    this.setupStateWithDefaults();
    MenuItem selectView = this.getSelectViewItem();
    assertThat(selectView).isNotNull();
  }
  
  @Test
  public void selectViewIsSubMenu() {
    this.setupStateWithDefaults();
    MenuItem selectView = this.getSelectViewItem();
    // The submenu shouldn't return null if it is a submenu, as it should be.
    assertThat(selectView.getSubMenu()).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasSpreadsheet() {
    this.setupStateWithDefaults();
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem spreadsheetItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_spreadsheet_view);
    assertThat(spreadsheetItem).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasList() {
    this.setupStateWithDefaults();
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem listItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_list_view);
    assertThat(listItem).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasGraph() {
    this.setupStateWithDefaults();
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem graphItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_graph_view);
    assertThat(graphItem).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasMap() {
    this.setupStateWithDefaults();
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem mapItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_map_view);
    assertThat(mapItem).isNotNull();
  }
  
  @Test
  public void menuHasPreferences() {
    this.setupStateWithDefaults();
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    assertThat(preferenceItem).isNotNull();
  }
  
  @Test
  public void preferencesHasSubMenu() {
    this.setupStateWithDefaults();
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    SubMenu subMenu = preferenceItem.getSubMenu();
    assertThat(subMenu).isNotNull();
  }
  
  @Test
  public void preferencesSubMenuHasTableProperties() {
    this.setupStateWithDefaults();
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    SubMenu subMenu = preferenceItem.getSubMenu();
    MenuItem tablePropertiesItem = 
        subMenu.findItem(R.id.top_level_table_menu_table_properties);
    assertThat(tablePropertiesItem).isNotNull();
  }
  
  @Test
  public void preferencesSubMenuHasPropertySets() {
    this.setupStateWithDefaults();
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    SubMenu subMenu = preferenceItem.getSubMenu();
    MenuItem propertiesSets = 
        subMenu.findItem(R.id.top_level_table_menu_manage_property_sets);
    assertThat(propertiesSets).isNotNull();
  }
  
  @Test
  public void listViewDisablesCorrectly() {
    PossibleTableViewTypes viewTypes = new PossibleTableViewTypes(
        true,
        false,
        true,
        true);
    this.setupStateWithCustomPossibleViewTypes(viewTypes);
    this.assertEnabledOnItems(true, false, true, true);
  }
  
  @Test
  public void spreadsheetViewDisablesCorrectly() {
    PossibleTableViewTypes viewTypes = new PossibleTableViewTypes(
        false,
        true,
        true,
        true);
    this.setupStateWithCustomPossibleViewTypes(viewTypes);
    this.assertEnabledOnItems(false, true, true, true);
  }
  
  @Test
  public void graphViewDisablesCorrectly() {
    PossibleTableViewTypes viewTypes = new PossibleTableViewTypes(
        true,
        true,
        true,
        false);
    this.setupStateWithCustomPossibleViewTypes(viewTypes);
    this.assertEnabledOnItems(true, true, true, false);
  }
  
  @Test
  public void mapViewDisablesCorrectly() {
    PossibleTableViewTypes viewTypes = new PossibleTableViewTypes(
        true,
        true,
        false,
        true);
    this.setupStateWithCustomPossibleViewTypes(viewTypes);
    this.assertEnabledOnItems(true, true, false, true);
  }
  
  @Test
  public void multipleItemsDisableCorrectly() {
    PossibleTableViewTypes viewTypes = new PossibleTableViewTypes(
        true,
        false,
        false,
        false);
    this.setupStateWithCustomPossibleViewTypes(viewTypes);
    this.assertEnabledOnItems(true, false, false, false);
  }
  
  @Test
  public void spreadsheetChecked() {
    this.setupStateWithCurrentFragment(ViewFragmentType.SPREADSHEET);
    this.assertItemIsTheOnlyOneChecked(ViewFragmentType.SPREADSHEET);
  }
  
  @Test
  public void listChecked() {
    this.setupStateWithCurrentFragment(ViewFragmentType.LIST);
    this.assertItemIsTheOnlyOneChecked(ViewFragmentType.LIST);
  }
  
  @Test
  public void graphChecked() {
    this.setupStateWithCurrentFragment(ViewFragmentType.GRAPH);
    this.assertItemIsTheOnlyOneChecked(ViewFragmentType.GRAPH);
  }
  
  @Test
  public void mapChecked() {
    this.setupStateWithCurrentFragment(ViewFragmentType.MAP);
    this.assertItemIsTheOnlyOneChecked(ViewFragmentType.MAP);
  }
  
  private void assertItemIsTheOnlyOneChecked(ViewFragmentType fragmentType) {
    MenuItem spreadsheetItem = this.getSpreadsheetViewMenuItem();
    MenuItem listItem = this.getListViewMenuItem();
    MenuItem graphItem = this.getGraphViewMenuItem();
    MenuItem mapItem = this.getMapViewMenuItem();
    if (fragmentType == ViewFragmentType.SPREADSHEET) {
      assertThat(spreadsheetItem).isChecked();
    } else {
      assertThat(spreadsheetItem).isNotChecked();
    }
    if (fragmentType == ViewFragmentType.LIST) {
      assertThat(listItem).isChecked();
    } else {
      assertThat(listItem).isNotChecked();
    }
    if (fragmentType == ViewFragmentType.MAP) {
      assertThat(mapItem).isChecked();
    } else {
      assertThat(mapItem).isNotChecked();
    }
    if (fragmentType == ViewFragmentType.GRAPH) {
      assertThat(graphItem).isChecked();
    } else {
      assertThat(graphItem).isNotCheckable();
    }
  }
  
  private void assertEnabledOnItems(
      boolean spreadsheetEnabled,
      boolean listEnabled,
      boolean mapEnabled,
      boolean graphEnabled) {
    MenuItem spreadsheetItem = this.getSpreadsheetViewMenuItem();
    MenuItem listItem = this.getListViewMenuItem();
    MenuItem graphItem = this.getGraphViewMenuItem();
    MenuItem mapItem = this.getMapViewMenuItem();
    if (spreadsheetEnabled) {
      assertThat(spreadsheetItem).isEnabled();
    } else {
      assertThat(spreadsheetItem).isDisabled();
    }
    if (listEnabled) {
      assertThat(listItem).isEnabled();
    } else {
      assertThat(listItem).isDisabled();
    }
    if (graphEnabled) {
      assertThat(graphItem).isEnabled();
    } else {
      assertThat(graphItem).isDisabled();
    }
    if (mapEnabled) {
      assertThat(mapItem).isEnabled();
    } else {
      assertThat(mapItem).isDisabled();
    }
  }
  
  /**
   * Get the spreadsheet view menu item.
   * @return
   */
  MenuItem getSpreadsheetViewMenuItem() {
    MenuItem result = 
        this.menu.findItem(R.id.top_level_table_menu_view_spreadsheet_view);
    return result;
  }
  
  /**
   * Get the list view menu item.
   * @return
   */
  MenuItem getListViewMenuItem() {
    MenuItem result = 
        this.menu.findItem(R.id.top_level_table_menu_view_list_view);
    return result;
  }
  
  /**
   * Get the graph view menu item.
   * @return
   */
  MenuItem getGraphViewMenuItem() {
    MenuItem result = 
        this.menu.findItem(R.id.top_level_table_menu_view_graph_view);
    return result;
  }
  
  /**
   * Get the map view menu item.
   * @return
   */
  MenuItem getMapViewMenuItem() {
    MenuItem result = 
        this.menu.findItem(R.id.top_level_table_menu_view_map_view);
    return result;
  }
  
  /**
   * Get the {@link MenuItem} for selecting a view.
   * @return
   */
  MenuItem getSelectViewItem() {
    MenuItem selectView =
        this.menu.findItem(R.id.top_level_table_menu_select_view);
    return selectView;
  }
  
  /**
   * Convenience method for calling {@link Menu#findItem(int)} on
   * {@link #menu}.
   * @param itemId
   * @return
   */
  MenuItem getMenuItemWithId(int itemId) {
    return this.menu.findItem(itemId);
  }

}
