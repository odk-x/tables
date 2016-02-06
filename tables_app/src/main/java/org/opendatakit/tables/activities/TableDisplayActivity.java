/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.activities;

import android.view.*;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.common.android.utilities.UrlUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.views.ODKWebView;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.data.PossibleTableViewTypes;
import org.opendatakit.tables.fragments.*;
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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.widget.Toast;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this activity.
 *
 * The initially requested optional view, filename and instance id are specified on the
 * intent with these keys:
 *    Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE
 *    Constants.IntentKeys.FILE_NAME
 *    IntentConsts.INTENT_KEY_INSTANCE_ID
 *
 * If none are specified, the default view type for this table is retrieved from the database.
 *
 * The user is able to switch to an alternate view from this. When they switch back to
 * this view type, they should see the view as described above.
 *
 * Editing and other activities spawn a new view.
 *
 * The current view type is persisted in:
 *     INTENT_KEY_CURRENT_VIEW_TYPE
 *     INTENT_KEY_CURRENT_FILE_NAME
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivity extends AbsTableWebActivity implements
    TableMapInnerFragmentListener {

  private static final String TAG = "TableDisplayActivity";

  public static final String INTENT_KEY_CURRENT_VIEW_TYPE = "currentViewType";
  public static final String INTENT_KEY_CURRENT_FILE_NAME = "currentFileName";


  public static final String INTENT_KEY_CURRENT_FRAGMENT = "saveInstanceCurrentFragment";
  /**
   * The fragment types this activity could be displaying.
   * 
   * @author sudar.sam@gmail.com
   *
   */
  public enum ViewFragmentType {
    SPREADSHEET, LIST, MAP, DETAIL;
  }

  /**
   * The type of fragment that is currently being displayed.
   */
  private ViewFragmentType mCurrentFragmentType;
  private String mCurrentFileName;
  /**
   * The type of fragment that was originally requested.
   */
  private ViewFragmentType mOriginalFragmentType;
  private String mOriginalFileName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    /*
     * If we are restoring from a saved state, the fleshed-out original view type and filename
     * will be in the savedInstance bundle. Otherwise, we will need to extract it from the Intent.
     *
     * Once they are extracted, the original values may be fleshed out from the database and
     * configuration settings. The fleshed-out values will be stored in the savedInstanceState
     * so that recovery can proceed more quickly
     */
    if ( savedInstanceState != null ) {
      mCurrentFragmentType = savedInstanceState.containsKey(INTENT_KEY_CURRENT_VIEW_TYPE) ?
          ViewFragmentType.valueOf(savedInstanceState.getString(INTENT_KEY_CURRENT_VIEW_TYPE)) :
          null;
      mCurrentFileName = savedInstanceState.containsKey(INTENT_KEY_CURRENT_FILE_NAME) ?
          savedInstanceState.getString(INTENT_KEY_CURRENT_FILE_NAME) : null;

      mOriginalFragmentType = savedInstanceState.containsKey(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) ?
          ViewFragmentType.valueOf(savedInstanceState.getString(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE)) :
          null;
      mOriginalFileName = savedInstanceState.containsKey(Constants.IntentKeys.FILE_NAME) ?
          savedInstanceState.getString(Constants.IntentKeys.FILE_NAME) : null;
    }

    if ( mOriginalFragmentType == null ) {
      // get the information from the Intent
      String viewType = getIntent().hasExtra(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) ?
          getIntent().getStringExtra(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) :
          null;

      if (viewType != null) {
        mOriginalFragmentType = ViewFragmentType.valueOf(viewType);
      }
    }
    if ( mOriginalFileName == null ) {
      // get the information from the Intent
      mOriginalFileName = getIntent().hasExtra(Constants.IntentKeys.FILE_NAME) ?
          getIntent().getStringExtra(Constants.IntentKeys.FILE_NAME) : null;
    }

    this.setContentView(R.layout.activity_table_display_activity);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    if ( savedInstanceState != null ) {
      mCurrentFragmentType = savedInstanceState.containsKey(INTENT_KEY_CURRENT_VIEW_TYPE) ?
          ViewFragmentType.valueOf(savedInstanceState.getString(INTENT_KEY_CURRENT_VIEW_TYPE)) :
          null;
      mCurrentFileName = savedInstanceState.containsKey(INTENT_KEY_CURRENT_FILE_NAME) ?
          savedInstanceState.getString(INTENT_KEY_CURRENT_FILE_NAME) : null;

      mOriginalFragmentType = savedInstanceState.containsKey(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) ?
          ViewFragmentType.valueOf(savedInstanceState.getString(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE)) :
          null;
      mOriginalFileName = savedInstanceState.containsKey(Constants.IntentKeys.FILE_NAME) ?
          savedInstanceState.getString(Constants.IntentKeys.FILE_NAME) : null;
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mCurrentFragmentType != null) {
      outState.putString(INTENT_KEY_CURRENT_VIEW_TYPE, mCurrentFragmentType.name());
    }
    if (mCurrentFileName != null) {
      outState.putString(INTENT_KEY_CURRENT_FILE_NAME, mCurrentFileName);
    }
    if (mOriginalFragmentType != null) {
      outState.putString(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE, mOriginalFragmentType.name());
    }
    if (mOriginalFileName != null) {
      outState.putString(Constants.IntentKeys.FILE_NAME, mOriginalFileName);
    }
  }

  /** Cached data from database */
  private PossibleTableViewTypes mPossibleTableViewTypes = null;

  /**
   * The {@link UserTable} that is being displayed in this activity.
   */
  private UserTable mUserTable = null;

  @Override
  public void databaseAvailable() {

    // see if we saved the state
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName());
      if (mOriginalFragmentType == null) {
        // recover the default view for this table from the database...
        TableViewType type;
        type = TableUtil.get().getDefaultViewType(Tables.getInstance(), getAppName(), db, getTableId());
        mOriginalFragmentType = this.getViewFragmentTypeFromViewType(type);
        if (mOriginalFragmentType == null) {
          // and if that isn't set, use spreadsheet
          WebLogger.getLogger(getAppName()).i(TAG,
              "[retrieveFragmentTypeToDisplay] no view type found, defaulting to spreadsheet");
          mOriginalFragmentType = ViewFragmentType.SPREADSHEET;
        }
      }
      if (mOriginalFileName == null && mOriginalFragmentType != ViewFragmentType.SPREADSHEET ) {
        mOriginalFileName = getDefaultFileNameForViewFragmentType(db, mOriginalFragmentType);
      }

      if ( mCurrentFragmentType == null ) {
        mCurrentFragmentType = mOriginalFragmentType;
        mCurrentFileName = mOriginalFileName;
      }

      if ( mCurrentFileName == null ) {
        mCurrentFileName = getDefaultFileNameForViewFragmentType(db, mCurrentFragmentType);
      }

      if ( mPossibleTableViewTypes == null ) {
        this.mPossibleTableViewTypes = new PossibleTableViewTypes(getAppName(), db, getTableId(),
            getColumnDefinitions());
      }
    } catch (RemoteException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName()).e(TAG,
          "[databaseAvailable] unable to access database");
      Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
    } finally {
      if (db != null) {
        try {
          Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
        } catch (RemoteException e) {
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          WebLogger.getLogger(getAppName()).e(TAG,
              "[databaseAvailable] unable to access database");
          Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
        }
      }
    }

    // at this point, we have all the information necessary to render the fragment
    this.showCurrentDisplayFragment(false);

    // wait for the appropriate fragment to render, then call notify to change status
    Handler handler = new Handler() {};
    handler.postDelayed(new Runnable() {

      @Override
      public void run() {
        notifyCurrentFragment(true);
      }}, 100);
    super.databaseAvailable();
  }

  @Override
  public void databaseUnavailable() {
    super.databaseUnavailable();
    // TODO: is this necessary
    notifyCurrentFragment(false);
  }

  private String getDefaultFileNameForViewFragmentType(OdkDbHandle db,
      ViewFragmentType fragmentType) throws
      RemoteException {
    switch ( fragmentType ) {
    case SPREADSHEET:
      return null;
    case LIST:
      return TableUtil.get().getListViewFilename(Tables.getInstance(), getAppName(), db, getTableId());
    case MAP:
      return TableUtil.get().getMapListViewFilename(Tables.getInstance(), getAppName(), db, getTableId());
    case DETAIL:
      return TableUtil.get().getDetailViewFilename(Tables.getInstance(), getAppName(), db, getTableId());
    default:
      return null;
    }
  }

  /**
   * Get the {@link UserTable} from the database that should be displayed.
   *
   * @return
   * @throws RemoteException
   */
  void initializeBackingTable(OdkDbHandle db) throws RemoteException {
    SQLQueryStruct sqlQueryStruct = IntentUtil.getSQLQueryStructFromBundle(this.getIntent().getExtras());
    String[] emptyArray = {};
    UserTable result = Tables.getInstance().getDatabase().rawSqlQuery(this.getAppName(), db,
        this.getTableId(), getColumnDefinitions(), sqlQueryStruct.whereClause,
        (sqlQueryStruct.selectionArgs == null) ? emptyArray : sqlQueryStruct.selectionArgs,
        (sqlQueryStruct.groupBy == null) ? emptyArray : sqlQueryStruct.groupBy,
        sqlQueryStruct.having, sqlQueryStruct.orderByElementKey, sqlQueryStruct.orderByDirection);
    mUserTable = result;
  }

  /**
   * Get the {@link ViewFragmentType} that corresponds to {@link TableViewType}.
   * If no match is found, returns null.
   *
   * @param viewType
   * @return
   */
  public ViewFragmentType getViewFragmentTypeFromViewType(TableViewType viewType) {
    switch (viewType) {
    case SPREADSHEET:
      return ViewFragmentType.SPREADSHEET;
    case MAP:
      return ViewFragmentType.MAP;
    case LIST:
      return ViewFragmentType.LIST;
    default:
      WebLogger.getLogger(getAppName()).e(TAG, "viewType " + viewType + " not recognized.");
      return null;
    }
  }

  /**
   * Get the {@link UserTable} that is being held by this activity.
   *
   * @return
   */
  public UserTable getUserTable() {
    if ( mUserTable == null ) {
      OdkDbHandle db = null;
      try {
        db = Tables.getInstance().getDatabase().openDatabase(getAppName());
        initializeBackingTable(db);
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        if ( db != null ) {
          try {
            Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
          } catch (RemoteException e) {
            // ignore
            e.printStackTrace();
          }
        }
      }
    }
    return mUserTable;
  }

  @Override
  public String getUrlBaseLocation(boolean ifChanged) {
    // TODO: do we need to track the ifChanged status?
    String filename = mCurrentFileName;
    if ( filename != null ) {
      return UrlUtils.getAsWebViewUri(this, getAppName(), filename);
    }
    return null;
  }

  @Override public String getInstanceId() {
    if ( mCurrentFragmentType == ViewFragmentType.DETAIL ) {
      String rowId = IntentUtil.retrieveRowIdFromBundle(this.getIntent().getExtras());
      return rowId;
    }
    // map views are not considered to have a specific instanceId.
    // While one of the items happens to be distinguished, the view
    // is still a list of items.
    return null;
  }

  private void notifyCurrentFragment(boolean databaseAvailable) {
    FragmentManager fragmentManager = this.getFragmentManager();

    switch ( mCurrentFragmentType ) {
    case SPREADSHEET:
      SpreadsheetFragment spreadsheetFragment = (SpreadsheetFragment) fragmentManager
        .findFragmentByTag(mCurrentFragmentType.name());
      if ( spreadsheetFragment != null ) {
        if ( databaseAvailable ) {
          spreadsheetFragment.databaseAvailable();
        } else {
          spreadsheetFragment.databaseUnavailable();
        }
      }
      break;
    case LIST:
      ListViewFragment listViewFragment = (ListViewFragment) fragmentManager
      .findFragmentByTag(mCurrentFragmentType.name());
      if ( listViewFragment != null ) {
        if ( databaseAvailable ) {
          listViewFragment.databaseAvailable();
        } else {
          listViewFragment.databaseUnavailable();
        }
      }
      break;
    case MAP:
      MapListViewFragment mapListViewFragment = (MapListViewFragment) fragmentManager
        .findFragmentByTag(mCurrentFragmentType.name());
      if ( mapListViewFragment != null ) {
        if ( databaseAvailable ) {
          mapListViewFragment.databaseAvailable();
        } else {
          mapListViewFragment.databaseUnavailable();
        }
      }
      break;
    case DETAIL:
      DetailViewFragment detailViewFragment = (DetailViewFragment) fragmentManager
      .findFragmentByTag(mCurrentFragmentType.name());
      if ( detailViewFragment != null ) {
        if ( databaseAvailable ) {
          detailViewFragment.databaseAvailable();
        } else {
          detailViewFragment.databaseUnavailable();
        }
      }
      break;
    }

  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    WebLogger.getLogger(getAppName()).d(TAG, "[onDestroy]");
  }

  @Override public ODKWebView getWebKitView() {
    FragmentManager fragmentManager = this.getFragmentManager();
    switch (mCurrentFragmentType) {
    case SPREADSHEET:
      // this isn't a webkit
      return null;
    case LIST:
      ListViewFragment listViewFragment =
          (ListViewFragment) fragmentManager.findFragmentByTag(ViewFragmentType.LIST.name());
      if ( listViewFragment != null ) {
        return listViewFragment.getWebKit();
      }
      break;
    case MAP:
      MapListViewFragment mapListViewFragment = (MapListViewFragment) fragmentManager
          .findFragmentByTag(Constants.FragmentTags.MAP_LIST);
      if ( mapListViewFragment != null ) {
        return mapListViewFragment.getWebKit();
      }
      break;
    case DETAIL:
      DetailViewFragment detailViewFragment =
          (DetailViewFragment) fragmentManager.findFragmentByTag(ViewFragmentType.DETAIL.name());
      if ( detailViewFragment != null ) {
        return detailViewFragment.getWebKit();
      }
      break;
    }
    // TODO: use view extended from webkit inside fragments
    return null;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // clear the menu so that we don't double inflate
    menu.clear();
    MenuInflater menuInflater = this.getMenuInflater();
    switch (mCurrentFragmentType) {
    case SPREADSHEET:
    case LIST:
    case MAP:
      menuInflater.inflate(R.menu.top_level_table_menu, menu);
      enableAndDisableViewTypes(mPossibleTableViewTypes, menu);
      selectCorrectViewType(menu);
      break;
    case DETAIL:
      menuInflater.inflate(R.menu.detail_view_menu, menu);
      break;
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    String filename = null;
    switch (item.getItemId()) {
    case R.id.top_level_table_menu_view_spreadsheet_view:
      setCurrentFragmentType(ViewFragmentType.SPREADSHEET, null);
      return true;
    case R.id.top_level_table_menu_view_list_view:
      if ( mOriginalFragmentType == ViewFragmentType.LIST ) {
        filename = mOriginalFileName;
      }
      setCurrentFragmentType(ViewFragmentType.LIST, filename);
      return true;
    case R.id.top_level_table_menu_view_map_view:
      if ( mOriginalFragmentType == ViewFragmentType.MAP ) {
        filename = mOriginalFileName;
      }
      setCurrentFragmentType(ViewFragmentType.MAP, filename);
      return true;
    case R.id.top_level_table_menu_add:
      WebLogger.getLogger(getAppName()).d(TAG, "[onOptionsItemSelected] add selected");
      try {
        ActivityUtil.addRow(this, this.getAppName(), this.getTableId(), this.getColumnDefinitions(),
            null);
      } catch (RemoteException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
      }
      return true;
    case R.id.top_level_table_menu_table_properties:
      ActivityUtil.launchTableLevelPreferencesActivity(this, this.getAppName(), this.getTableId(),
          TableLevelPreferencesActivity.FragmentType.TABLE_PREFERENCE);
      return true;
    case R.id.menu_edit_row:
      // We need to retrieve the row id.
      String rowId = getInstanceId();
      if (rowId == null) {
        WebLogger.getLogger(getAppName()).e(TAG,
            "[onOptionsItemSelected trying to edit row, but row id is null");
        Toast.makeText(this, getString(R.string.cannot_edit_row_please_try_again),
            Toast.LENGTH_LONG).show();
        return true;
      }
      try {
        ActivityUtil.editRow(this, this.getAppName(), this.getTableId(), this.getColumnDefinitions(),
          rowId);
      } catch (RemoteException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
      }
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    try {
      // for most returns, we just refresh the data set and redraw the page
      // for others, we need to take more intensive action
      switch (requestCode) {
      case Constants.RequestCodes.ADD_ROW_COLLECT:
        if (resultCode == Activity.RESULT_OK) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result ok, refreshing backing table");
          CollectUtil.handleOdkCollectAddReturn(getBaseContext(), getAppName(), getTableId(),
              resultCode, data);
        } else {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result canceled, not refreshing backing " + "table");
        }
        break;
      case Constants.RequestCodes.EDIT_ROW_COLLECT:
        if (resultCode == Activity.RESULT_OK) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result ok, refreshing backing table");
          CollectUtil.handleOdkCollectEditReturn(getBaseContext(), getAppName(), getTableId(),
              resultCode, data);
        } else {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result canceled, not refreshing backing " + "table");
        }
        break;
      case Constants.RequestCodes.ADD_ROW_SURVEY:
      case Constants.RequestCodes.EDIT_ROW_SURVEY:
        if (resultCode == Activity.RESULT_OK) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result ok, refreshing backing table");
        } else {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result canceled, refreshing backing table");
        }
        break;
      }
      // verify that the data table doesn't contain checkpoints...
      // always refresh, as survey may have done something
      refreshDataAndDisplayFragment();
    } catch ( RemoteException e ) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  /**
   * Disable or enable those menu items corresponding to view types that are
   * currently invalid or valid, respectively. The inflatedMenu must have
   * already been created from the resource.
   * 
   * @param possibleViews
   * @param inflatedMenu
   */
  private void enableAndDisableViewTypes(PossibleTableViewTypes possibleViews, Menu inflatedMenu) {
    MenuItem spreadsheetItem = inflatedMenu
        .findItem(R.id.top_level_table_menu_view_spreadsheet_view);
    MenuItem listItem = inflatedMenu.findItem(R.id.top_level_table_menu_view_list_view);
    MenuItem mapItem = inflatedMenu.findItem(R.id.top_level_table_menu_view_map_view);
    spreadsheetItem.setEnabled((possibleViews != null) && possibleViews.spreadsheetViewIsPossible());
    listItem.setEnabled((possibleViews != null) && possibleViews.listViewIsPossible());
    mapItem.setEnabled((possibleViews != null) && possibleViews.mapViewIsPossible());
  }

  /**
   * Selects the correct view type that is being displayed by the
   * {@see ITopLevelTableMenuActivity}.
   * 
   * @param inflatedMenu
   */
  private void selectCorrectViewType(Menu inflatedMenu) {
    if (mCurrentFragmentType == null) {
      WebLogger.getLogger(getAppName()).e(TAG,
          "did not find a current fragment type. Not selecting view.");
      return;
    }
    MenuItem menuItem = null;
    switch (mCurrentFragmentType) {
    case SPREADSHEET:
      menuItem = inflatedMenu.findItem(R.id.top_level_table_menu_view_spreadsheet_view);
      menuItem.setChecked(true);
      break;
    case LIST:
      menuItem = inflatedMenu.findItem(R.id.top_level_table_menu_view_list_view);
      menuItem.setChecked(true);
      break;
    case MAP:
      menuItem = inflatedMenu.findItem(R.id.top_level_table_menu_view_map_view);
      menuItem.setChecked(true);
      break;
    default:
      WebLogger.getLogger(getAppName()).e(TAG, "view type not recognized: " + mCurrentFragmentType);
    }
  }

  public void refreshDataAndDisplayFragment() throws RemoteException {
    WebLogger.getLogger(getAppName()).d(TAG, "[refreshDataAndDisplayFragment]");
    // drop cached table, if any...
    mUserTable = null;
    showCurrentDisplayFragment(true);
  }

  /**
   * Set the current type of fragment that is being displayed.
   * Called when mocking interface.
   *
   * @param requestedType
   * @param fileName
   */
  public void setCurrentFragmentType(ViewFragmentType requestedType, String fileName) {
    if ( requestedType != ViewFragmentType.SPREADSHEET &&
        fileName == null &&
        Tables.getInstance().getDatabase() != null ) {
      try {
        OdkDbHandle db = null;
        try {
          db = Tables.getInstance().getDatabase().openDatabase(getAppName());
          fileName = getDefaultFileNameForViewFragmentType(db, requestedType);
        } finally {
          if ( db != null ) {
            Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
          }
        }
      } catch (RemoteException e1) {
        WebLogger.getLogger(getAppName()).printStackTrace(e1);
        Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
        return;
      }
    }
    mCurrentFragmentType = requestedType;
    mCurrentFileName = fileName;
    showCurrentDisplayFragment(false);
  }

  /**
   * Initialize the correct display fragment based on the result of
   * {@link #retrieveTableIdFromIntent()}. Initializes Spreadsheet if none is
   * present in Intent.
   */
  private void showCurrentDisplayFragment(boolean createNew) {
    this.updateChildViewVisibility(mCurrentFragmentType);
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    // First acquire all the possible fragments.
    Fragment spreadsheetFragment = fragmentManager.findFragmentByTag(ViewFragmentType.SPREADSHEET.name());
    Fragment listViewFragment = fragmentManager.findFragmentByTag(ViewFragmentType.LIST.name());
    Fragment mapListViewFragment = fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    Fragment innerMapFragment = fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP_INNER_MAP);
    Fragment detailViewFragment = fragmentManager.findFragmentByTag(ViewFragmentType.DETAIL.name());

    // Hide all fragments other than the current fragment type...
    if (mCurrentFragmentType != ViewFragmentType.SPREADSHEET && spreadsheetFragment != null) {
      fragmentTransaction.hide(spreadsheetFragment);
    }
    if (mCurrentFragmentType != ViewFragmentType.LIST && listViewFragment != null) {
      fragmentTransaction.hide(listViewFragment);
    }
    if (mCurrentFragmentType != ViewFragmentType.DETAIL && detailViewFragment != null) {
      fragmentTransaction.hide(detailViewFragment);
    }
    if (mCurrentFragmentType != ViewFragmentType.MAP) {
      if (mapListViewFragment != null) {
        fragmentTransaction.hide(mapListViewFragment);
      }
      if (innerMapFragment != null) {
        fragmentTransaction.hide(innerMapFragment);
      }
    }

    // and enable, or delete and re-create, the fragment that we want to display
    switch (mCurrentFragmentType) {
    case SPREADSHEET:
      if (spreadsheetFragment == null || createNew) {
        if (spreadsheetFragment != null) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[showSpreadsheetFragment] removing existing fragment");
          // Get rid of the existing fragment
          fragmentTransaction.remove(spreadsheetFragment);
        }
        spreadsheetFragment = new SpreadsheetFragment();
        fragmentTransaction.add(R.id.activity_table_display_activity_one_pane_content,
            spreadsheetFragment, mCurrentFragmentType.name());
      } else {
        fragmentTransaction.show(spreadsheetFragment);
      }
      break;
    case DETAIL:
      if (detailViewFragment == null || createNew) {
        if (detailViewFragment != null) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[showDetailViewFragment] removing existing fragment");
          // Get rid of the existing fragment
          fragmentTransaction.remove(detailViewFragment);
        }
        detailViewFragment = new DetailViewFragment();
        fragmentTransaction.add(R.id.activity_table_display_activity_one_pane_content,
            detailViewFragment, mCurrentFragmentType.name());
      } else {
        fragmentTransaction.show(detailViewFragment);
      }
      break;
    case LIST:
      if (listViewFragment == null || createNew) {
        if (listViewFragment != null) {
          // remove the old fragment
          WebLogger.getLogger(getAppName()).d(TAG, "[showListFragment] removing old list fragment");
          fragmentTransaction.remove(listViewFragment);
        }
        listViewFragment = new ListViewFragment();
        fragmentTransaction.add(R.id.activity_table_display_activity_one_pane_content,
            listViewFragment, mCurrentFragmentType.name());
      } else {
        fragmentTransaction.show(listViewFragment);
      }
      break;
    case MAP:
      if (mapListViewFragment == null || createNew) {
        if (mapListViewFragment != null) {
          // remove the old fragment
          WebLogger.getLogger(getAppName())
              .d(TAG, "[showMapFragment] removing old map list fragment");
          fragmentTransaction.remove(mapListViewFragment);
        }
        mapListViewFragment = new MapListViewFragment();
        fragmentTransaction.add(R.id.map_view_list, mapListViewFragment,
            Constants.FragmentTags.MAP_LIST);
      } else {
        fragmentTransaction.show(mapListViewFragment);
      }
      if (innerMapFragment == null || createNew) {
        if (innerMapFragment != null) {
          // remove the old fragment
          WebLogger.getLogger(getAppName()).d(TAG,
              "[showMapFragment] removing old inner map fragment");
          fragmentTransaction.remove(innerMapFragment);
        }
        innerMapFragment =  new TableMapInnerFragment();
        fragmentTransaction.add(R.id.map_view_inner_map, innerMapFragment,
            Constants.FragmentTags.MAP_INNER_MAP);
        ((TableMapInnerFragment) innerMapFragment).listener = this;
      } else {
        ((TableMapInnerFragment) innerMapFragment).listener = this;
        fragmentTransaction.show(innerMapFragment);
      }
      break;
    default:
      WebLogger.getLogger(getAppName()).e(TAG,
          "ViewFragmentType not recognized: " + this.mCurrentFragmentType);
      break;
    }
    fragmentTransaction.commit();

    invalidateOptionsMenu();
  }

  /**
   * Update the content view's children visibility for viewFragmentType. This is
   * required due to the fact that not all the fragments make use of the same
   * children views within the activity.
   * 
   * @param viewFragmentType
   */
  void updateChildViewVisibility(ViewFragmentType viewFragmentType) {
    // The map fragments occupy a different view than the single pane
    // content. This is because the map is two views--the list and the map
    // itself. So, we need to hide and show the others as appropriate.
    View onePaneContent = this.findViewById(R.id.activity_table_display_activity_one_pane_content);
    View mapContent = this.findViewById(R.id.activity_table_display_activity_map_content);
    switch (viewFragmentType) {
    case DETAIL:
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
      WebLogger.getLogger(getAppName()).e(TAG,
          "[updateChildViewVisibility] unrecognized type: " + viewFragmentType);
    }
  }

  /**
   * Invoked by TableMapInnerFragment when an item has been selected
   */
  @Override
  public void onSetSelectedItemIndex(int i) {
    FragmentManager fragmentManager = getFragmentManager();
    MapListViewFragment mapListViewFragment = (MapListViewFragment) fragmentManager
        .findFragmentByTag(Constants.FragmentTags.MAP_LIST);

    if (mapListViewFragment == null) {
      WebLogger.getLogger(getAppName()).e(TAG,
          "[onSetIndex] mapListViewFragment is null! Returning");
      return;
    } else {
      mapListViewFragment.setIndexOfSelectedItem(i);
    }
  }

  /**
   * Invoked by TableMapInnerFragment when an item has stopped being selected
   */
  public void setNoItemSelected() {
    FragmentManager fragmentManager = getFragmentManager();
    MapListViewFragment mapListViewFragment = (MapListViewFragment) fragmentManager
        .findFragmentByTag(Constants.FragmentTags.MAP_LIST);

    if (mapListViewFragment == null) {
      WebLogger.getLogger(getAppName()).e(TAG,
          "[setNoItemSelected] mapListViewFragment is null! Returning");
      return;
    } else {
      mapListViewFragment.setNoItemSelected();
    }
  }

  @Override public void initializationCompleted() {

  }


}
