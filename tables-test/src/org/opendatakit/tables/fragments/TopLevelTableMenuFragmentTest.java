package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
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
  
  TopLevelTableMenuFragment fragment;
  Activity activity;
  Menu menu;
  
  @Before
  public void setup() {
    ShadowLog.stream = System.out;
    // We need a table activity because we're going to be disabling things on
    this.fragment = new TopLevelTableMenuFragment();
    ODKFragmentTestUtil.startFragmentForTableActivity(
        TableDisplayActivityStub.class,
        this.fragment,
        null);
    this.activity = this.fragment.getActivity();
    this.menu = shadowOf(this.activity).getOptionsMenu();
  }
  
  @After
  public void after() {
    TableDisplayActivityStub.resetState();
  }
  
  @Test
  public void assertNotNull() {
    assertThat(this.fragment).isNotNull();
  }
  
  @Test
  public void menuHasAdd() {
    MenuItem addItem = this.menu.findItem(R.id.top_level_table_menu_add);
    assertThat(addItem).isNotNull();
  }
  
  @Test
  public void menuHasSelectView() {
    MenuItem selectView = this.getSelectViewItem();
    assertThat(selectView).isNotNull();
  }
  
  @Test
  public void selectViewIsSubMenu() {
    MenuItem selectView = this.getSelectViewItem();
    // The submenu shouldn't return null if it is a submenu, as it should be.
    assertThat(selectView.getSubMenu()).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasSpreadsheet() {
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem spreadsheetItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_spreadsheet_view);
    assertThat(spreadsheetItem).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasList() {
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem listItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_list_view);
    assertThat(listItem).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasGraph() {
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem graphItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_graph_view);
    assertThat(graphItem).isNotNull();
  }
  
  @Test
  public void selectViewSubMenuHasMap() {
    MenuItem selectView = this.getSelectViewItem();
    SubMenu subMenu = selectView.getSubMenu();
    MenuItem mapItem = 
        subMenu.findItem(R.id.top_level_table_menu_view_map_view);
    assertThat(mapItem).isNotNull();
  }
  
  @Test
  public void menuHasPreferences() {
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    assertThat(preferenceItem).isNotNull();
  }
  
  @Test
  public void preferencesHasSubMenu() {
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    SubMenu subMenu = preferenceItem.getSubMenu();
    assertThat(subMenu).isNotNull();
  }
  
  @Test
  public void preferencesSubMenuHasTableProperties() {
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    SubMenu subMenu = preferenceItem.getSubMenu();
    MenuItem tablePropertiesItem = 
        subMenu.findItem(R.id.top_level_table_menu_table_properties);
    assertThat(tablePropertiesItem).isNotNull();
  }
  
  @Test
  public void preferencesSubMenuHasPropertySets() {
    MenuItem preferenceItem = 
        this.getMenuItemWithId(R.id.top_level_table_menu_select_preferences);
    SubMenu subMenu = preferenceItem.getSubMenu();
    MenuItem propertiesSets = 
        subMenu.findItem(R.id.top_level_table_menu_manage_property_sets);
    assertThat(propertiesSets).isNotNull();
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
