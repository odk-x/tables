package org.opendatakit.tables.activities;

import java.util.ArrayList;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.DetailViewFragment;
import org.opendatakit.tables.fragments.GraphManagerFragment;
import org.opendatakit.tables.fragments.GraphViewFragment;
import org.opendatakit.tables.fragments.ListViewFragment;
import org.opendatakit.tables.fragments.MapListViewFragment;
import org.opendatakit.tables.fragments.SpreadsheetFragment;
import org.opendatakit.tables.fragments.TableMapInnerFragment;
import org.opendatakit.tables.fragments.TableMapInnerFragment.TableMapInnerFragmentListener;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this  activity.
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivity extends AbsTableActivity
    implements TableMapInnerFragmentListener {

  private static final String TAG = TableDisplayActivity.class.getSimpleName();
  private static final String INTENT_KEY_CURRENT_FRAGMENT =
      "saveInstanceCurrentFragment";

  /**
   * The fragment types this activity could be displaying.
   * @author sudar.sam@gmail.com
   *
   */
  public enum ViewFragmentType {
    SPREADSHEET,
    LIST,
    MAP,
    GRAPH_MANAGER,
    GRAPH_VIEW,
    DETAIL;
  }

  /**
   * The {@link UserTable} that is being displayed in this activity.
   */
  private UserTable mUserTable;
  /**
   *  The type of fragment that is currently being displayed.
   */
  private ViewFragmentType mCurrentFragmentType;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // see if we saved the state
    this.initializeBackingTable();
    this.mCurrentFragmentType =
        this.retrieveFragmentTypeToDisplay(savedInstanceState);
    this.setContentView(R.layout.activity_table_display_activity);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    this.mCurrentFragmentType =
        this.retrieveFragmentTypeToDisplay(savedInstanceState);
    Log.i(TAG, "[onRestoreInstanceState] current fragment type: " +
        this.mCurrentFragmentType);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "[onDestroy]");
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (this.mCurrentFragmentType != null) {
      Log.i(TAG, "[onSaveInstanceState] saving current fragment type: "
          + this.mCurrentFragmentType.name());
      outState.putString(
          INTENT_KEY_CURRENT_FRAGMENT,
          this.mCurrentFragmentType.name());
    } else {
      Log.i(TAG, "[onSaveInstanceState] no current fragment type to save");
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.i(TAG, "[onResume]");
    TableProperties tableProperties = this.getTableProperties();
    if ( tableProperties.hasCheckpoints() ) {
      Intent i = new Intent(this,
          CheckpointResolutionListActivity.class);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tableProperties.getTableId());
      this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER);

    } else if ( tableProperties.hasConflicts() ) {
      Intent i = new Intent(this,
          ConflictResolutionListActivity.class);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tableProperties.getTableId());
      this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER);

    } else {
      this.initializeDisplayFragment();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // clear the menu so that we don't double inflate
    menu.clear();
    MenuInflater menuInflater = this.getMenuInflater();
    switch (this.getCurrentFragmentType()) {
    case SPREADSHEET:
    case LIST:
    case GRAPH_MANAGER:
    case MAP:
      menuInflater.inflate(
          R.menu.top_level_table_menu,
          menu);
      PossibleTableViewTypes viewTypes = this.retrievePossibleViewTypes();
      this.enableAndDisableViewTypes(viewTypes, menu);
      this.selectCorrectViewType(menu);
      break;
    case DETAIL:
      menuInflater.inflate(R.menu.detail_view_menu, menu);
      break;
    case GRAPH_VIEW:
      // for now, do nothing.
      break;
    }
    return super.onCreateOptionsMenu(menu);
  }

  /**
   * Retrieve the {@link PossibleTableViewTypes} representing the valid views
   * for this table.
   * @return
   */
  PossibleTableViewTypes retrievePossibleViewTypes() {
    return this.getTableProperties().getPossibleViewTypes();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.top_level_table_menu_view_spreadsheet_view:
      this.showSpreadsheetFragment();
      return true;
    case R.id.top_level_table_menu_view_list_view:
      this.showListFragment();
      return true;
    case R.id.top_level_table_menu_view_graph_view:
      this.showGraphFragment();
      return true;
    case R.id.top_level_table_menu_view_map_view:
      this.showMapFragment();
      return true;
    case R.id.top_level_table_menu_add:
      Log.d(TAG, "[onOptionsItemSelected] add selected");
      ActivityUtil.addRow(
          this,
          this.getTableProperties(),
          null);
      return true;
    case R.id.top_level_table_menu_table_properties:
      ActivityUtil.launchTableLevelPreferencesActivity(
          this,
          this.getAppName(),
          this.getTableProperties().getTableId(),
          TableLevelPreferencesActivity.FragmentType.TABLE_PREFERENCE);
      return true;
    case R.id.menu_edit_row:
      // We need to retrieve the row id.
      DetailViewFragment detailViewFragment = this.findDetailViewFragment();
      if (detailViewFragment == null) {
        Log.e(
            TAG,
            "[onOptionsItemSelected] trying to edit row, but detail view " +
              " fragment null");
        Toast.makeText(
            this,
            getString(R.string.cannot_edit_row_please_try_again),
            Toast.LENGTH_LONG)
          .show();
      }
      String rowId = detailViewFragment.getRowId();
      if (rowId == null) {
        Log.e(
            TAG,
            "[onOptionsItemSelected trying to edit row, but row id is null");
        Toast.makeText(
            this,
            getString(R.string.cannot_edit_row_please_try_again),
            Toast.LENGTH_LONG)
          .show();
      }
      ActivityUtil.editRow(
          this,
          this.getTableProperties(),
          rowId);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(
      int requestCode,
      int resultCode,
      Intent data) {
    switch (requestCode) {
    case Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER:
    case Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER:
      // these are no-ops on return, as the onResume() method will deal with
      // any fall-out from them.
      this.refreshDataTable();
      this.refreshDisplayFragment();
      break;
    // For now, we will just refresh the table if something could have changed.
    case Constants.RequestCodes.ADD_ROW_COLLECT:
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "[onActivityResult] result ok, refreshing backing table");
        TableProperties tableProperties = this.getTableProperties();
        CollectUtil.handleOdkCollectAddReturn(getBaseContext(), getAppName(), tableProperties, resultCode, data);
        
        this.refreshDataTable();
        // We also want to cause the fragments to redraw themselves, as their
        // data may have changed.
        this.refreshDisplayFragment();
      } else {
        Log.d(
            TAG,
            "[onActivityResult] result canceled, not refreshing backing " +
              "table");
      }
      break;
    case Constants.RequestCodes.EDIT_ROW_COLLECT:
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "[onActivityResult] result ok, refreshing backing table");
        TableProperties tableProperties = this.getTableProperties();
        CollectUtil.handleOdkCollectEditReturn(getBaseContext(), getAppName(), tableProperties, resultCode, data);
        
        this.refreshDataTable();
        // We also want to cause the fragments to redraw themselves, as their
        // data may have changed.
        this.refreshDisplayFragment();
      } else {
        Log.d(
            TAG,
            "[onActivityResult] result canceled, not refreshing backing " +
              "table");
      }
      break;
    case Constants.RequestCodes.ADD_ROW_SURVEY:
    case Constants.RequestCodes.EDIT_ROW_SURVEY:
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "[onActivityResult] result ok, refreshing backing table");
      } else {
        Log.d(
            TAG,
            "[onActivityResult] result canceled, refreshing backing table");
      }
      // always refresh, as survey may have done something
      this.refreshDataTable();
      this.refreshDisplayFragment();
      break;
    }
    super.onActivityResult(requestCode, resultCode, data);
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

  /**
   * Selects the correct view type that is being displayed by the
   * {@link ITopLevelTableMenuActivity}.
   * @param impl
   * @param inflatedMenu
   */
  private void selectCorrectViewType(Menu inflatedMenu) {
    ViewFragmentType currentFragment = this.getCurrentFragmentType();
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

  @Override
  protected void onStart() {
     super.onStart();
     Log.i(TAG, "[onStart]");
  }

  public void refreshDisplayFragment() {
    Log.d(TAG, "[refreshDisplayFragment]");
    this.helperInitializeDisplayFragment(true);
  }

  protected void initializeDisplayFragment() {
    this.helperInitializeDisplayFragment(false);
  }

  /**
   * Initialize the correct display fragment based on the result of
   * {@link #retrieveTableIdFromIntent()}. Initializes Spreadsheet if none
   * is present in Intent.
   */
  private void helperInitializeDisplayFragment(boolean createNew) {
    switch (this.mCurrentFragmentType) {
    case SPREADSHEET:
      this.showSpreadsheetFragment(createNew);
      break;
    case DETAIL:
      this.showDetailFragment(createNew);
      break;
    case GRAPH_MANAGER:
      this.showGraphFragment(createNew);
      break;
    case LIST:
      this.showListFragment(createNew);
      break;
    case MAP:
      this.showMapFragment(createNew);
      break;
    case GRAPH_VIEW:
      String graphName =
          this.getIntent().getStringExtra(Constants.IntentKeys.GRAPH_NAME);
      if (graphName == null) {
        Log.e(
            TAG,
            "[initializeDisplayFragment] graph name not present in bundle");
      }
      this.showGraphViewFragment(graphName, createNew);
      break;
    default:
      Log.e(TAG, "ViewFragmentType not recognized: " +
          this.mCurrentFragmentType);
      break;
    }
  }

  /**
   * Set the current type of fragment that is being displayed.
   * @param currentType
   */
  protected void setCurrentFragmentType(ViewFragmentType currentType) {
    this.mCurrentFragmentType = currentType;
    this.invalidateOptionsMenu();
  }

  /**
   * @return the {@link ViewFragmentType} that was passed in the intent,
   * or null if none exists.
   */
  protected ViewFragmentType retrieveViewFragmentTypeFromIntent() {
    if (this.getIntent().getExtras() == null) {
      return null;
    }
    String viewFragmentTypeStr = this.getIntent().getExtras().getString(
        Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE);
    if (viewFragmentTypeStr == null) {
      return null;
    } else {
      ViewFragmentType result = ViewFragmentType.valueOf(viewFragmentTypeStr);
      return result;
    }
  }

  /**
   * Get the {@link ViewFragmentType} that should be displayed. Any type in
   * the passed in bundle takes precedence, on the assumption that is was from
   * a saved instance state. Next is any type that
   * was passed in the Intent. If neither is present, the value
   * corresponding to {@link TableProperties#getDefaultViewType()} wins. If
   * none is present, returns {@link ViewFragmentType#SPREADSHEET}.
   * @return
   */
  protected ViewFragmentType retrieveFragmentTypeToDisplay(
      Bundle savedInstanceState) {
    // 1) First check the passed in bundle.
    if (savedInstanceState != null &&
        savedInstanceState.containsKey(INTENT_KEY_CURRENT_FRAGMENT)) {
      String instanceTypeStr =
          savedInstanceState.getString(INTENT_KEY_CURRENT_FRAGMENT);
      Log.i(TAG, "[retrieveFragmentTypeToDisplay] found type in saved instance" +
          " state: " + instanceTypeStr);
      return ViewFragmentType.valueOf(instanceTypeStr);
    }
    Log.i(TAG, "[retrieveFragmentTypeToDisplay] didn't find fragment type " +
    		"in saved instance state");
    // 2) then check the intent
    ViewFragmentType result = retrieveViewFragmentTypeFromIntent();
    if (result == null) {
      // 3) then use the default
      TableViewType viewType =
          this.getTableProperties().getDefaultViewType();
      result = this.getViewFragmentTypeFromViewType(viewType);
    }
    if (result == null) {
      // 4) last case, do spreadsheet
      Log.i(TAG, "[retrieveFragmentTypeToDisplay] no view type found, " +
      		"defaulting to spreadsheet");
      result = ViewFragmentType.SPREADSHEET;
    }
    return result;
  }

  /**
   * Get the {@link ViewFragmentType} that corresponds to
   * {@link TableViewType}. If no match is found, returns null.
   * @param viewType
   * @return
   */
  public ViewFragmentType getViewFragmentTypeFromViewType(
      TableViewType viewType) {
    switch (viewType) {
    case SPREADSHEET:
      return ViewFragmentType.SPREADSHEET;
    case MAP:
      return ViewFragmentType.MAP;
    case GRAPH:
      return ViewFragmentType.GRAPH_MANAGER;
    case LIST:
      return ViewFragmentType.LIST;
    default:
      Log.e(TAG, "viewType " + viewType + " not recognized.");
      return null;
    }
  }

  /**
   * Initialize {@link TableDisplayActivity#mUserTable}.
   */
  private void initializeBackingTable() {
    UserTable userTable = this.retrieveUserTable();
    this.mUserTable = userTable;
  }

  /**
   * Get the {@link UserTable} that is being held by this activity.
   * @return
   */
  public UserTable getUserTable() {
    return this.mUserTable;
  }

  /**
   * Refresh the data being displayed.
   */
  public void refreshDataTable() {
    this.initializeBackingTable();
  }

  /**
   * Get the {@link UserTable} from the database that should be displayed.
   * @return
   */
  UserTable retrieveUserTable() {
    TableProperties tableProperties = this.getTableProperties();
    SQLQueryStruct sqlQueryStruct =
        this.retrieveSQLQueryStatStructFromIntent();
    DbTable dbTable = DbTable.getDbTable(tableProperties);
    UserTable result = dbTable.rawSqlQuery(
        sqlQueryStruct.whereClause,
        sqlQueryStruct.selectionArgs,
        sqlQueryStruct.groupBy,
        sqlQueryStruct.having,
        sqlQueryStruct.orderByElementKey,
        sqlQueryStruct.orderByDirection);
    return result;
  }

  /**
   * Retrieve the {@link SQLQueryStruct} specified in the {@link Intent} that
   * restricts the current table.
   * @return
   */
  SQLQueryStruct retrieveSQLQueryStatStructFromIntent() {
    SQLQueryStruct result = IntentUtil.getSQLQueryStructFromBundle(
        this.getIntent().getExtras());
    return result;
  }

  /**
   * Show the spreadsheet fragment, creating a new one if it doesn't yet exist.
   */
  public void showSpreadsheetFragment() {
    this.showSpreadsheetFragment(false);
  }

  /**
   * Show the spreadsheet fragment.
   * @param createNew
   */
  public void showSpreadsheetFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.SPREADSHEET);
    this.updateChildViewVisibility(ViewFragmentType.SPREADSHEET);
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    // Hide all the other fragments.
    this.hideAllOtherViewFragments(
        ViewFragmentType.SPREADSHEET,
        fragmentTransaction);
    // Try to retrieve one already there.
    SpreadsheetFragment spreadsheetFragment = (SpreadsheetFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.SPREADSHEET);
    if (spreadsheetFragment == null) {
      Log.d(TAG, "[showSpreadsheetFragment] no existing spreadshseet " +
      		"fragment found");
    } else {
      Log.d(TAG, "[showSpreadsheetFragment] existing spreadsheet fragment " +
      		"found");
    }
    Log.d(TAG, "[showSpreadsheetFragment] createNew is: " + createNew);
    if (spreadsheetFragment == null || createNew) {
      if (spreadsheetFragment != null) {
        Log.d(TAG, "[showSpreadsheetFragment] removing existing fragment");
        // Get rid of the existing fragment
        fragmentTransaction.remove(spreadsheetFragment);
      }
      spreadsheetFragment = this.createSpreadsheetFragment();
      fragmentTransaction.add(
          R.id.activity_table_display_activity_one_pane_content,
          spreadsheetFragment,
          Constants.FragmentTags.SPREADSHEET);
    } else {
      fragmentTransaction.show(spreadsheetFragment);
    }
    fragmentTransaction.commit();
  }

  /**
   * Hide every fragment except that specified by fragmentToKeepVisible.
   * @param fragmentToKeepVisible
   * @param fragmentTransaction the transaction on which the calls to hide the
   * fragments is to be performed
   */
  private void hideAllOtherViewFragments(
      ViewFragmentType fragmentToKeepVisible,
      FragmentTransaction fragmentTransaction) {
    FragmentManager fragmentManager = this.getFragmentManager();
    // First acquire all the possible fragments.
    Fragment spreadsheet =
        fragmentManager.findFragmentByTag(Constants.FragmentTags.SPREADSHEET);
    Fragment list =
        fragmentManager.findFragmentByTag(Constants.FragmentTags.LIST);
    Fragment graphManager = fragmentManager.findFragmentByTag(
        Constants.FragmentTags.GRAPH_MANAGER);
    Fragment mapList =
        fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    Fragment mapInner = fragmentManager.findFragmentByTag(
        Constants.FragmentTags.MAP_INNER_MAP);
    Fragment detailFragment = fragmentManager.findFragmentByTag(
        Constants.FragmentTags.DETAIL_FRAGMENT);
    Fragment graphViewFragment = fragmentManager.findFragmentByTag(
        Constants.FragmentTags.GRAPH_VIEW);
    if (fragmentToKeepVisible != ViewFragmentType.SPREADSHEET &&
        spreadsheet != null) {
      fragmentTransaction.hide(spreadsheet);
    }
    if (fragmentToKeepVisible != ViewFragmentType.LIST &&
        list != null) {
      fragmentTransaction.hide(list);
    }
    if (fragmentToKeepVisible != ViewFragmentType.GRAPH_MANAGER &&
        graphManager != null) {
      fragmentTransaction.hide(graphManager);
    }
    if (fragmentToKeepVisible != ViewFragmentType.DETAIL &&
        detailFragment != null) {
      fragmentTransaction.hide(detailFragment); 
    }
    if (fragmentToKeepVisible != ViewFragmentType.GRAPH_VIEW &&
        graphViewFragment != null) {
      fragmentTransaction.hide(graphViewFragment);
    }
    if (fragmentToKeepVisible != ViewFragmentType.MAP) {
      if (mapList != null) {
        fragmentTransaction.hide(mapList);
      }
      if (mapInner != null) {
        fragmentTransaction.hide(mapInner);
      }
    }
  }

  /**
   * Create a {@link SpreadsheetFragment} to be displayed in the activity.
   * @return
   */
  SpreadsheetFragment createSpreadsheetFragment() {
    SpreadsheetFragment result = new SpreadsheetFragment();
    return result;
  }

  public void showMapFragment() {
    this.showMapFragment(false);
  }

  public void showMapFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.MAP);
    this.updateChildViewVisibility(ViewFragmentType.MAP);
    // Set the list view file name.
    String fileName =
        IntentUtil.retrieveFileNameFromBundle(this.getIntent().getExtras());
    if (fileName == null) {
      // use the default.
      fileName = this.getTableProperties().getMapListViewFileName();
    }
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    this.hideAllOtherViewFragments(
        ViewFragmentType.MAP,
        fragmentTransaction);
    MapListViewFragment mapListViewFragment = (MapListViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    TableMapInnerFragment innerMapFragment = (TableMapInnerFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.MAP_INNER_MAP);
    if (mapListViewFragment == null ||
        (fileName != null && !fileName.equals(mapListViewFragment.getFileName())) ||
        createNew) {
      if ( mapListViewFragment != null ) {
        // remove the old fragment
        Log.d(TAG, "[showMapFragment] removing old map list fragment");
        fragmentTransaction.remove(mapListViewFragment);
      }
      Log.d(TAG, "[showMapFragment] creating new map list fragment");
      mapListViewFragment = this.createMapListViewFragment(fileName);
      fragmentTransaction.add(
          R.id.map_view_list,
          mapListViewFragment,
          Constants.FragmentTags.MAP_LIST);
    } else {
      Log.d(TAG, "[showMapFragment] existing map list fragment found");
      fragmentTransaction.show(mapListViewFragment);
    }
    if (innerMapFragment == null || createNew) {
      if ( innerMapFragment != null ) {
        // remove the old fragment
        Log.d(TAG, "[showMapFragment] removing old inner map fragment");
        fragmentTransaction.remove(innerMapFragment);
      }
      Log.d(TAG, "[showMapFragment] creating new inner map fragment");
      innerMapFragment = this.createInnerMapFragment();
      fragmentTransaction.add(
          R.id.map_view_inner_map,
          innerMapFragment,
          Constants.FragmentTags.MAP_INNER_MAP);
      innerMapFragment.listener = this;
    } else {
      Log.d(TAG, "[showMapFragment] existing inner map fragment found");
      innerMapFragment.listener = this;
      fragmentTransaction.show(innerMapFragment);
    }
    fragmentTransaction.commit();
  }

  /**
   * Create the {@link TableMapInnerFragment} that will be displayed as the
   * map.
   * @return
   */
  TableMapInnerFragment createInnerMapFragment() {
    TableMapInnerFragment result = new TableMapInnerFragment();
    return result;
  }

  /**
   * Create the {@link MapListViewFragment} that will be displayed with the
   * map view.
   * @param listViewFileName the file name of the list view that will be
   * displayed
   * @return
   */
  MapListViewFragment createMapListViewFragment(String listViewFileName) {
    MapListViewFragment result = new MapListViewFragment();
    Bundle listArguments = new Bundle();
    IntentUtil.addFileNameToBundle(listArguments, listViewFileName);
    result.setArguments(listArguments);
    return result;
  }

  public void showListFragment() {
    this.showListFragment(false);
  }

  public void showListFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.LIST);
    this.updateChildViewVisibility(ViewFragmentType.LIST);
    // Try to use a passed file name. If one doesn't exist, try to use the
    // default.
    String fileName =
        IntentUtil.retrieveFileNameFromBundle(this.getIntent().getExtras());
    if (fileName == null) {
      fileName = getTableProperties().getListViewFileName();
    }
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    this.hideAllOtherViewFragments(
        ViewFragmentType.LIST,
        fragmentTransaction);
    ListViewFragment listViewFragment = (ListViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.LIST);
    if (listViewFragment == null || createNew) {
      if (listViewFragment == null) {
        Log.d(TAG, "[showListFragment] existing list fragment not found");
      } else {
        // remove the old fragment
        Log.d(TAG, "[showListFragment] removing old list fragment");
        fragmentTransaction.remove(listViewFragment);
      }
      listViewFragment = this.createListViewFragment(fileName);
      fragmentTransaction.add(
          R.id.activity_table_display_activity_one_pane_content,
          listViewFragment,
          Constants.FragmentTags.LIST);
    } else {
      Log.d(TAG, "[showListFragment] existing list fragment found");
      fragmentTransaction.show(listViewFragment);
    }
    fragmentTransaction.commit();
  }

  /**
   * Create a {@link ListViewFragment} to be used by the activity.
   * @param fileName the file name to be displayed
   */
  ListViewFragment createListViewFragment(String fileName) {
    ListViewFragment result = new ListViewFragment();
    Bundle arguments = new Bundle();
    IntentUtil.addFileNameToBundle(arguments, fileName);
    result.setArguments(arguments);
    return result;
  }

  public void showGraphFragment() {
    this.showGraphFragment(false);
  }

  public void showGraphFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.GRAPH_MANAGER);
    this.updateChildViewVisibility(ViewFragmentType.GRAPH_MANAGER);
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    this.hideAllOtherViewFragments(
        ViewFragmentType.GRAPH_MANAGER,
        fragmentTransaction);
    // Try to retrieve the fragment if it already exists.
    GraphManagerFragment graphManagerFragment = (GraphManagerFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.GRAPH_MANAGER);
    if (graphManagerFragment == null || createNew) {
      if (graphManagerFragment == null) {
        Log.d(TAG, "[showGraphFragment] existing graph fragment not found");
      } else {
        Log.d(TAG, "[showGraphFragment] removing old graph fragment");
        fragmentTransaction.remove(graphManagerFragment);
      }
      graphManagerFragment = this.createGraphManagerFragment();
      fragmentTransaction.add(
          R.id.activity_table_display_activity_one_pane_content,
          graphManagerFragment,
          Constants.FragmentTags.GRAPH_MANAGER);
    } else {
      Log.d(TAG, "[showGraphFragment] existing graph fragment found");
      fragmentTransaction.show(graphManagerFragment);
    }
    fragmentTransaction.commit();
  }

  /**
   * Create a {@link GraphManagerFragment} that will be used by the activity.
   * @return
   */
  GraphManagerFragment createGraphManagerFragment() {
    GraphManagerFragment result = new GraphManagerFragment();
    return result;
  }

  public void showGraphViewFragment(String graphName) {
    this.showGraphViewFragment(graphName, false);
  }

  public void showGraphViewFragment(String graphName, boolean createNew) {
    Log.d(TAG, "[showGraphViewFragment] graph name: " + graphName);
    this.setCurrentFragmentType(ViewFragmentType.GRAPH_VIEW);
    this.updateChildViewVisibility(ViewFragmentType.GRAPH_VIEW);
    // Try and use the default.
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    this.hideAllOtherViewFragments(
        ViewFragmentType.GRAPH_VIEW,
        fragmentTransaction);
    GraphViewFragment graphViewFragment = (GraphViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.GRAPH_VIEW);
    if (graphViewFragment == null || createNew) {
      if (graphViewFragment != null) {
        Log.d(TAG, "[showGraphViewFragment] removing old graphview fragment");
        fragmentTransaction.remove(graphViewFragment);
      } else {
        Log.d(TAG, "[showGraphViewFragment] no existing graphview fragment found");
      }
      graphViewFragment = this.createGraphViewFragment(graphName);
    } else {
      // Add the value to the existing fragment so it displays the correct
      // value.
      Bundle arguments = new Bundle();
      arguments.putString(Constants.IntentKeys.GRAPH_NAME, graphName);
      graphViewFragment.getArguments().putAll(arguments);
    }
    fragmentTransaction.add(
        R.id.activity_table_display_activity_one_pane_content,
        graphViewFragment,
        Constants.FragmentTags.GRAPH_VIEW);
    fragmentTransaction.commit();
  }

  /**
   * Create a {@link GraphViewFragment} to be added to the activity.
   * @param graphName
   * @return
   */
  GraphViewFragment createGraphViewFragment(String graphName) {
    GraphViewFragment result = new GraphViewFragment();
    Bundle arguments = new Bundle();
    arguments.putString(Constants.IntentKeys.GRAPH_NAME, graphName);
    result.setArguments(arguments);
    return result;
  }

  public void showDetailFragment() {
    this.showDetailFragment(false);
  }

  public void showDetailFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.DETAIL);
    this.updateChildViewVisibility(ViewFragmentType.DETAIL);
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    this.hideAllOtherViewFragments(
        ViewFragmentType.DETAIL,
        fragmentTransaction);
    String fileName = IntentUtil.retrieveFileNameFromBundle(
        this.getIntent().getExtras());
    // Try and use the default.
    if (fileName == null) {
      Log.d(TAG, "[showDetailFragment] fileName not found in Intent");
      fileName = this.getTableProperties().getDetailViewFileName();
    }
    String rowId = IntentUtil.retrieveRowIdFromBundle(
        this.getIntent().getExtras());
    // Try to retrieve one that already exists.
    DetailViewFragment detailViewFragment = (DetailViewFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.DETAIL_FRAGMENT);
    if (detailViewFragment == null || createNew) {
      if (detailViewFragment != null) {
        Log.d(TAG, "[showDetailViewFragment] removing old detail view fragment");
        fragmentTransaction.remove(detailViewFragment);
      } else {
        Log.d(TAG, "[showDetailViewFragment] no existing detail view fragment found");
      }
      detailViewFragment = this.createDetailViewFragment(fileName, rowId);
      
      fragmentTransaction.add(
          R.id.activity_table_display_activity_one_pane_content,
          detailViewFragment,
          Constants.FragmentTags.DETAIL_FRAGMENT);
    } else {
      Log.d(TAG, "[showDetailViewFragment] existing detail view fragment found");
      fragmentTransaction.show(detailViewFragment);
    }

    fragmentTransaction.commit();
  }
  

  /**
   * Create a {@link DetailViewFragment} to be used with the fragments.
   * @param fileName
   * @param rowId
   * @return
   */
  DetailViewFragment createDetailViewFragment(String fileName, String rowId) {
    DetailViewFragment result = new DetailViewFragment();
    Bundle bundle = new Bundle();
    IntentUtil.addRowIdToBundle(bundle, rowId);
    IntentUtil.addFileNameToBundle(bundle, fileName);
    result.setArguments(bundle);
    return result;
  }

  /**
   * Update the content view's children visibility for viewFragmentType. This
   * is required due to the fact that not all the fragments make use of the
   * same children views within the activity.
   * @param viewFragmentType
   */
  void updateChildViewVisibility(ViewFragmentType viewFragmentType) {
    // The map fragments occupy a different view than the single pane
    // content. This is because the map is two views--the list and the map
    // itself. So, we need to hide and show the others as appropriate.
    View onePaneContent = this.findViewById(
        R.id.activity_table_display_activity_one_pane_content);
    View mapContent =
        this.findViewById(R.id.activity_table_display_activity_map_content);
    switch (viewFragmentType) {
    case DETAIL:
    case GRAPH_MANAGER:
    case GRAPH_VIEW:
    case LIST:
    case SPREADSHEET:
      onePaneContent.setVisibility(View.VISIBLE);
      mapContent.setVisibility(View.GONE);
      return;
    case MAP:
      onePaneContent.setVisibility(View.GONE);
      mapContent.setVisibility(View.VISIBLE);
      return;
    default:
      Log.e(
          TAG,
          "[updateChildViewVisibility] unrecognized type: " +
              viewFragmentType);
    }
  }

  /**
   * Retrieve the {@link DetailViewFragment} that is associated with this
   * activity.
   * @return the fragment, or null if it is not present
   */
  DetailViewFragment findDetailViewFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    DetailViewFragment result = (DetailViewFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.DETAIL_FRAGMENT);
    return result;
  }


  /**
   * Return the {@link ViewFragmentType} that is currently being displayed.
   */
  public ViewFragmentType getCurrentFragmentType() {
    return this.mCurrentFragmentType;
  }

  /**
   * Invoked by TableMapInnerFragment when an item has been selected
   */
  @Override
  public void onSetSelectedItemIndex(int i) {
    MapListViewFragment mapListViewFragment = this.findMapListViewFragment();
    if (mapListViewFragment == null) {
      Log.e(TAG, "[onSetIndex] mapListViewFragment is null! Returning");
      return;
    } else {
      mapListViewFragment.setIndexOfSelectedItem(i);
    }
  }

  /**
   * Invoked by TableMapInnerFragment when an item has stopped being selected
   */
  public void setNoItemSelected() {
    MapListViewFragment mapListViewFragment = this.findMapListViewFragment();
    if (mapListViewFragment == null) {
      Log.e(TAG, "[setNoItemSelected] mapListViewFragment is null! Returning");
      return;
    } else {
      mapListViewFragment.setNoItemSelected();
    }
  }

  /**
   * Find a {@link MapListViewFragment} that is associated with this activity.
   * If not present, returns null.
   * @return
   */
  MapListViewFragment findMapListViewFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    MapListViewFragment result = (MapListViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    return result;
  }

}
