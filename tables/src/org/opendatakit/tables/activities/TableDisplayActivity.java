package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.fragments.DetailViewFragment;
import org.opendatakit.tables.fragments.ListViewFragment;
import org.opendatakit.tables.fragments.SpreadsheetFragment;
import org.opendatakit.tables.fragments.TableMapFragment;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment.ITopLevelTableMenuActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this  activity.
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivity extends AbsTableActivity
    implements ITopLevelTableMenuActivity {
  
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
    GRAPH,
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
    this.initializeMenuFragment();
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
    this.initializeDisplayFragment();
  }
  
  /**
   * Update the options menu for this activity to be appropriate for the given
   * fragment.
   * @param viewFragmentType
   */
  void handleMenuForViewFragmentType(ViewFragmentType viewFragmentType) {
    TopLevelTableMenuFragment menuFragment = this.retrieveMenuFragment();
    DetailViewFragment detailFragment = this.retrieveDetailFragment();
    switch (viewFragmentType) {
    case SPREADSHEET:
    case MAP:
    case LIST:
    case GRAPH:
      // Show the menu fragment but not the detail view's.
      if (menuFragment != null) {
        menuFragment.setMenuVisibility(true);
      }
      if (detailFragment != null) {
        menuFragment.setMenuVisibility(false);
      }
      break;
    case DETAIL:
      if (menuFragment != null) {
        menuFragment.setMenuVisibility(false);
      }
      if (detailFragment != null) {
        detailFragment.setMenuVisibility(true);
      }
      break;
    default:
      Log.e(
          TAG,
          "[handleMenuForViewFragmentType] unrecognized fragment type: "
              + viewFragmentType);
    }
  }
  
  @Override
  protected void onStart() {
     super.onStart();
     Log.i(TAG, "[onStart]");
  }
  
  /**
   * Initialize the correct display fragment based on the result of
   * {@link #retrieveTableIdFromIntent()}. Initializes Spreadsheet if none
   * is present in Intent.
   */
  protected void initializeDisplayFragment() {
    switch (this.mCurrentFragmentType) {
    case SPREADSHEET:
      this.showSpreadsheetFragment();
      break;
    case DETAIL:
      this.showDetailFragment();
      break;
    case GRAPH:
      this.showGraphFragment();
      break;
    case LIST:
      this.showListFragment();
      break;
    case MAP:
      this.showMapFragment();
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
  protected ViewFragmentType getViewFragmentTypeFromViewType(
      TableViewType viewType) {
    switch (viewType) {
    case SPREADSHEET:
      return ViewFragmentType.SPREADSHEET;
    case MAP:
      return ViewFragmentType.MAP;
    case GRAPH:
      return ViewFragmentType.GRAPH;
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
  
  protected void initializeMenuFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    TopLevelTableMenuFragment menuFragment = new TopLevelTableMenuFragment();
    fragmentManager.beginTransaction().add(
        menuFragment,
        Constants.FragmentTags.TABLE_MENU).commit();
  }
  
  /**
   * Show the spreadsheet fragment, creating a new one if it doesn't yet exist.
   */
  public void showSpreadsheetFragment() {
    this.setCurrentFragmentType(ViewFragmentType.SPREADSHEET);
    FragmentManager fragmentManager = this.getFragmentManager();
    // Try to retrieve one already there.
    SpreadsheetFragment spreadsheetFragment = (SpreadsheetFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.SPREADSHEET);
    if (spreadsheetFragment != null) {
      fragmentManager.beginTransaction().show(spreadsheetFragment).commit();
    }
    // Otherwise create a new one.
    spreadsheetFragment = new SpreadsheetFragment();
    fragmentManager.beginTransaction().replace(
        android.R.id.content,
        spreadsheetFragment,
        Constants.FragmentTags.SPREADSHEET).commit();
    this.invalidateOptionsMenu();
  }
  
  public void showMapFragment() {
    this.setCurrentFragmentType(ViewFragmentType.MAP);
    FragmentManager fragmentManager = this.getFragmentManager();
    TableMapFragment mapFragment = (TableMapFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP);
    if (mapFragment != null) {
      fragmentManager.beginTransaction().show(mapFragment).commit();
    }
    // Otherwise we need to create one.
    mapFragment = new TableMapFragment();
    fragmentManager.beginTransaction().replace(
        android.R.id.content,
        mapFragment,
        Constants.FragmentTags.MAP).commit();
    this.handleMenuForViewFragmentType(ViewFragmentType.SPREADSHEET);
    this.invalidateOptionsMenu();
  }
  

  @Override
  public void showListFragment() {
    this.setCurrentFragmentType(ViewFragmentType.LIST);
    // Try to use a passed file name. If one doesn't exist, try to use the
    // default.
    String fileName =
        IntentUtil.retrieveFileNameFromBundle(this.getIntent().getExtras());
    if (fileName == null) {
      fileName = getTableProperties().getListViewFileName();
    }
    Bundle bundle = new Bundle();
    bundle.putString(Constants.IntentKeys.FILE_NAME, fileName);
    FragmentManager fragmentManager = this.getFragmentManager();
    ListViewFragment listViewFragment = (ListViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.LIST);
    if (listViewFragment != null) {
      listViewFragment.getArguments().putAll(bundle);
    } else {
      listViewFragment = new ListViewFragment();
      listViewFragment.setArguments(bundle);
    }
    fragmentManager.beginTransaction().replace(
        android.R.id.content,
        listViewFragment,
        Constants.FragmentTags.LIST).commit();
    this.handleMenuForViewFragmentType(ViewFragmentType.LIST);
    this.invalidateOptionsMenu();
  }

  @Override
  public void showGraphFragment() {
    this.setCurrentFragmentType(ViewFragmentType.GRAPH);
    this.handleMenuForViewFragmentType(ViewFragmentType.GRAPH);
    this.invalidateOptionsMenu();
    // TODO Auto-generated method stub
    
  }
  
  
  public void showDetailFragment() {
    this.setCurrentFragmentType(ViewFragmentType.DETAIL);
    FragmentManager fragmentManager = this.getFragmentManager();
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
    if (detailViewFragment == null) {
      detailViewFragment = new DetailViewFragment();
      Bundle bundle = new Bundle();
      IntentUtil.addRowIdToBundle(bundle, rowId);
      IntentUtil.addFileNameToBundle(bundle, fileName);
      detailViewFragment.setArguments(bundle);
    }
    if (!detailViewFragment.isResumed()) {
      fragmentManager.beginTransaction().replace(
          android.R.id.content,
          detailViewFragment,
          Constants.FragmentTags.DETAIL_FRAGMENT).commit();
    }
    this.handleMenuForViewFragmentType(ViewFragmentType.DETAIL);
    this.invalidateOptionsMenu();
  }
  
  /**
   * Retrieve the {@link TopLevelTableMenuFragment} that is associated with
   * this activity.
   * @return the fragment, or null if it is not present
   */
  TopLevelTableMenuFragment retrieveMenuFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    TopLevelTableMenuFragment result = (TopLevelTableMenuFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.TABLE_MENU);
    return result;
  }
  
  /**
   * Retrieve the {@link DetailViewFragment} that is associated with this
   * activity.
   * @return the fragment, or null if it is not present
   */
  DetailViewFragment retrieveDetailFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    DetailViewFragment result = (DetailViewFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.DETAIL_FRAGMENT);
    return result;
  }


  /**
   * Return the {@link ViewFragmentType} that is currently being displayed.
   */
  @Override
  public ViewFragmentType getCurrentFragmentType() {
    return this.mCurrentFragmentType;
  }

}
