package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.ActivityUtil;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Displays the common menu shared across all top level views onto a table.
 * I.e. the menu that is shared between Spreadsheet, Map, List, and Graph views
 * is initialized here.
 * @author sudar.sam@gmail.com
 *
 */
public class TopLevelTableMenuFragment extends AbsBaseFragment {

  public interface ITopLevelTableMenuActivity {
    /**
     * Get the fragment type that is currently being displayed by the activity.
     * Informs rendering of the menu.
     * @return
     */
    public TableDisplayActivity.ViewFragmentType getCurrentFragmentType();
    public void showSpreadsheetFragment();
    public void showMapFragment();
    public void showListFragment();
    public void showGraphFragment();
  }

  private static final String TAG =
      TopLevelTableMenuFragment.class.getSimpleName();

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof AbsTableActivity)) {
      throw new IllegalStateException("This fragment must be attached to " +
      		"an " + AbsTableActivity.class.getSimpleName());
    }
    if (!(activity instanceof ITopLevelTableMenuActivity)) {
      throw new IllegalStateException("This fragment must be attached to " +
      		"an " + ITopLevelTableMenuActivity.class.getSimpleName());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    // The whole point of this class is to display the menus.
    this.setHasOptionsMenu(true);
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(
        R.menu.top_level_table_menu,
        menu);
    PossibleTableViewTypes viewTypes = this.getPossibleViewTypes();
    enableAndDisableViewTypes(
        viewTypes,
        menu);
    selectCorrectViewType(retrieveInterfaceImpl(), menu);
  }

  /**
   * Selects the correct view type that is being displayed by the
   * {@link ITopLevelTableMenuActivity}.
   * @param impl
   * @param inflatedMenu
   */
  private void selectCorrectViewType(
      ITopLevelTableMenuActivity impl,
      Menu inflatedMenu) {
    ViewFragmentType currentFragment = impl.getCurrentFragmentType();
    if (currentFragment == null) {
      Log.e(TAG, "did not find a current fragment type. Not selecting view.");
      return;
    }
    MenuItem menuItem = null;
    switch (currentFragment) {
    case SPREADSHEET:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_spreadsheet_view);
      menuItem.setChecked(true);
      break;
    case LIST:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_list_view);
      menuItem.setChecked(true);
      break;
    case GRAPH_MANAGER:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_graph_view);
      menuItem.setChecked(true);
      break;
    case MAP:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_map_view);
      menuItem.setChecked(true);
      break;
    default:
      Log.e(TAG, "view type not recognized: " + currentFragment);
    }
  }

  /**
   * Retrieve the implementation of {@link ITopLevelTableMenuActivity} this
   * object is plugged into.
   * @return
   */
  ITopLevelTableMenuActivity retrieveInterfaceImpl() {
    ITopLevelTableMenuActivity impl =
        (ITopLevelTableMenuActivity) getActivity();
    return impl;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ITopLevelTableMenuActivity interfaceImpl = retrieveInterfaceImpl();
    switch (item.getItemId()) {
    case R.id.top_level_table_menu_view_spreadsheet_view:
      interfaceImpl.showSpreadsheetFragment();
      return true;
    case R.id.top_level_table_menu_view_list_view:
      interfaceImpl.showListFragment();
      return true;
    case R.id.top_level_table_menu_view_graph_view:
      interfaceImpl.showGraphFragment();
      return true;
    case R.id.top_level_table_menu_view_map_view:
      interfaceImpl.showMapFragment();
      return true;
    case R.id.top_level_table_menu_add:
      Log.d(TAG, "[onOptionsItemSelected] add selected");
      ActivityUtil.addRow(
          (AbsBaseActivity) this.getActivity(),
          this.getTableProperties(),
          null);
      return true;
    case R.id.top_level_table_menu_table_properties:
      ActivityUtil.launchTableLevelPreferencesActivity(
          this.getActivity(),
          this.getAppName(),
          this.getTableProperties().getTableId());
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  };

  /**
   * Retrieve the {@link TableViewType}s that are valid for the table
   * associated with the {@link TableDisplayActivity}.
   * @return
   */
  PossibleTableViewTypes getPossibleViewTypes() {
    return this.getTableProperties().getPossibleViewTypes();
  }

  /**
   * Return the {@link TableProperties} associated with the Activity related
   * to this table.
   * @return
   */
  TableProperties getTableProperties() {
    TableDisplayActivity activity = (TableDisplayActivity) this.getActivity();
    return activity.getTableProperties();
  }

  /**
   * Disable or enable those menu items corresponding to view types that are
   * currently invalid or valid, respectively. The inflatedMenu must have
   * already been created from the resource.
   * @param validViewTypes
   * @param inflatedMenu
   */
  private void enableAndDisableViewTypes(
      PossibleTableViewTypes possibleViews,
      Menu inflatedMenu) {
    MenuItem spreadsheetItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_spreadsheet_view);
    MenuItem listItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_list_view);
    MenuItem mapItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_map_view);
    MenuItem graphItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_graph_view);
    spreadsheetItem.setEnabled(possibleViews.spreadsheetViewIsPossible());
    listItem.setEnabled(possibleViews.listViewIsPossible());
    mapItem.setEnabled(possibleViews.mapViewIsPossible());
    graphItem.setEnabled(possibleViews.graphViewIsPossible());
  }

}
