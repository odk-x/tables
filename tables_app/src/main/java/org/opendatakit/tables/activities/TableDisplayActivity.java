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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.UserTable;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.data.PossibleTableViewTypes;
import org.opendatakit.tables.fragments.*;
import org.opendatakit.tables.fragments.TableMapInnerFragment.TableMapInnerFragmentListener;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.views.ODKWebView;
import org.opendatakit.views.ViewDataQueryParams;
import org.opendatakit.webkitserver.utilities.UrlUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this activity.
 * <p>
 * The initially requested optional view, filename and instance id are specified on the
 * intent with these keys:
 * Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE
 * Constants.IntentKeys.FILE_NAME
 * IntentConsts.INTENT_KEY_INSTANCE_ID
 * <p>
 * If none are specified, the default view type for this table is retrieved from the database.
 * <p>
 * The user is able to switch to an alternate view from this. When they switch back to
 * this view type, they should see the view as described above.
 * <p>
 * Editing and other activities spawn a new view.
 * <p>
 * The current view type is persisted in:
 * INTENT_KEY_CURRENT_VIEW_TYPE
 * INTENT_KEY_CURRENT_FILE_NAME
 *
 * @author sudar.sam@gmail.com
 */
public class TableDisplayActivity extends AbsBaseWebActivity
    implements TableMapInnerFragmentListener, IOdkTablesActivity, DatabaseConnectionListener {

  // Some keys for things that get saved to the instance state
  public static final String INTENT_KEY_CURRENT_VIEW_TYPE = "currentViewType";
  public static final String INTENT_KEY_CURRENT_FILE_NAME = "currentFileName";
  public static final String INTENT_KEY_CURRENT_SUB_FILE_NAME = "currentSubFileName";
  public static final String INTENT_KEY_QUERIES = "queries";
  // Used for logging
  private static final String TAG = TableDisplayActivity.class.getSimpleName();
  /**
   * Keep references to all queries used to populate all fragments. Use the array index as the
   * viewID.
   */
  ViewDataQueryParams[] mQueries;
  /**
   * The type of fragment that is currently being displayed.
   */
  private ViewFragmentType mCurrentFragmentType;
  private String mCurrentFileName;
  private String mCurrentSubFileName;
  /**
   * The type of fragment that was originally requested.
   */
  private ViewFragmentType mOriginalFragmentType;
  private String mOriginalFileName;
  /**
   * Cached data from database
   */
  private PossibleTableViewTypes mPossibleTableViewTypes = null;
  /**
   * The {@link UserTable} that is being displayed in this activity.
   */
  private UserTable mUserTable = null;

  /**
   * Casts an array of objects from Parcelable to a given class that extends Parcelable..
   *
   * @param clazz           the class to cast things in the array to
   * @param parcelableArray an array of objects that all extend Parcelable and can be safely
   *                        casted to clazz
   * @param <T>             clazz
   * @return parcelableArray except all the objects are of the type clazz
   */
  @SuppressWarnings("unchecked")
  private static <T extends Parcelable> T[] castParcelableArray(Class<T> clazz,
      Parcelable[] parcelableArray) {
    if (parcelableArray == null) {
      return null;
    }
    final int length = parcelableArray.length;
    final T[] array = (T[]) Array.newInstance(clazz, length);
    for (int i = 0; i < length; i++) {
      array[i] = (T) parcelableArray[i];
    }
    return array;
  }

  /**
   * If we're being created for the first time, pull the display type (list, spreadsheet, map,
   * etc..) and the original filename from the intent. Otherwise pull it from the saved instance
   * state
   *
   * @param savedInstanceState the state we saved in onSaveInstanceState
   */
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
    if (savedInstanceState != null) {
      mCurrentFragmentType = savedInstanceState.containsKey(INTENT_KEY_CURRENT_VIEW_TYPE) ?
          ViewFragmentType.valueOf(savedInstanceState.getString(INTENT_KEY_CURRENT_VIEW_TYPE)) :
          null;
      mCurrentFileName = savedInstanceState.containsKey(INTENT_KEY_CURRENT_FILE_NAME) ?
          savedInstanceState.getString(INTENT_KEY_CURRENT_FILE_NAME) :
          null;
      mCurrentSubFileName = savedInstanceState.containsKey(INTENT_KEY_CURRENT_SUB_FILE_NAME) ?
          savedInstanceState.getString(INTENT_KEY_CURRENT_SUB_FILE_NAME) :
          null;

      mOriginalFragmentType = savedInstanceState
          .containsKey(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) ?
          ViewFragmentType
              .valueOf(savedInstanceState.getString(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE)) :
          null;
      mOriginalFileName = savedInstanceState.containsKey(Constants.IntentKeys.FILE_NAME) ?
          savedInstanceState.getString(Constants.IntentKeys.FILE_NAME) :
          null;

      Parcelable[] parcArr = savedInstanceState.containsKey(INTENT_KEY_QUERIES) ?
          savedInstanceState.getParcelableArray(INTENT_KEY_QUERIES) :
          null;
      mQueries = castParcelableArray(ViewDataQueryParams.class, parcArr);
    }

    if (mOriginalFragmentType == null) {
      // get the information from the Intent
      String viewType = getIntent().hasExtra(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) ?
          getIntent().getStringExtra(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) :
          null;

      if (viewType != null) {
        mOriginalFragmentType = ViewFragmentType.valueOf(viewType);
      }
    }
    if (mOriginalFileName == null) {
      // get the information from the Intent
      mOriginalFileName = getIntent().hasExtra(Constants.IntentKeys.FILE_NAME) ?
          getIntent().getStringExtra(Constants.IntentKeys.FILE_NAME) :
          null;
    }

    readQueryFromIntent(getIntent());

    this.setContentView(R.layout.activity_table_display_activity);
  }

  /**
   * Helps restore the saved instance state, along with onCreate. It tries to pull the current
   * view type and display type, the file and sub-file names and their originals, and the queries
   *
   * @param savedInstanceState the instance state that we saved out when we were being
   *                           paused/destroyed
   */
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    if (savedInstanceState != null) {
      mCurrentFragmentType = savedInstanceState.containsKey(INTENT_KEY_CURRENT_VIEW_TYPE) ?
          ViewFragmentType.valueOf(savedInstanceState.getString(INTENT_KEY_CURRENT_VIEW_TYPE)) :
          null;
      mCurrentFileName = savedInstanceState.containsKey(INTENT_KEY_CURRENT_FILE_NAME) ?
          savedInstanceState.getString(INTENT_KEY_CURRENT_FILE_NAME) :
          null;
      mCurrentSubFileName = savedInstanceState.containsKey(INTENT_KEY_CURRENT_SUB_FILE_NAME) ?
          savedInstanceState.getString(INTENT_KEY_CURRENT_SUB_FILE_NAME) :
          null;

      mOriginalFragmentType = savedInstanceState
          .containsKey(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE) ?
          ViewFragmentType
              .valueOf(savedInstanceState.getString(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE)) :
          null;
      mOriginalFileName = savedInstanceState.containsKey(Constants.IntentKeys.FILE_NAME) ?
          savedInstanceState.getString(Constants.IntentKeys.FILE_NAME) :
          null;

      Parcelable[] parcArr = savedInstanceState.containsKey(INTENT_KEY_QUERIES) ?
          savedInstanceState.getParcelableArray(INTENT_KEY_QUERIES) :
          null;
      mQueries = castParcelableArray(ViewDataQueryParams.class, parcArr);
    }
  }

  /**
   * Pulls all of the query parameters from the intent and puts them in mQueries[0
   *
   * @param in the intent that was used to launch this TableDisplayActivity
   */
  private void readQueryFromIntent(Intent in) {
    if (mQueries == null) {
      mQueries = new ViewDataQueryParams[2]; // We currently can have a maximum of two fragments
    }
    String tableId = in.hasExtra(IntentConsts.INTENT_KEY_TABLE_ID) ?
        in.getStringExtra(IntentConsts.INTENT_KEY_TABLE_ID) :
        null;
    String rowId = in.hasExtra(IntentConsts.INTENT_KEY_INSTANCE_ID) ?
        in.getStringExtra(IntentConsts.INTENT_KEY_INSTANCE_ID) :
        null;
    String whereClause = in.hasExtra(Constants.IntentKeys.SQL_WHERE) ?
        in.getStringExtra(Constants.IntentKeys.SQL_WHERE) :
        null;
    String[] selArgs = in.hasExtra(Constants.IntentKeys.SQL_SELECTION_ARGS) ?
        in.getStringArrayExtra(Constants.IntentKeys.SQL_SELECTION_ARGS) :
        null;
    String[] groupBy = in.hasExtra(Constants.IntentKeys.SQL_GROUP_BY_ARGS) ?
        in.getStringArrayExtra(Constants.IntentKeys.SQL_GROUP_BY_ARGS) :
        null;
    String having = in.hasExtra(Constants.IntentKeys.SQL_HAVING) ?
        in.getStringExtra(Constants.IntentKeys.SQL_HAVING) :
        null;
    String orderByElemKey = in.hasExtra(Constants.IntentKeys.SQL_ORDER_BY_ELEMENT_KEY) ?
        in.getStringExtra(Constants.IntentKeys.SQL_ORDER_BY_ELEMENT_KEY) :
        null;
    String orderByDir = in.hasExtra(Constants.IntentKeys.SQL_ORDER_BY_DIRECTION) ?
        in.getStringExtra(Constants.IntentKeys.SQL_ORDER_BY_DIRECTION) :
        null;

    ViewDataQueryParams queryParams = new ViewDataQueryParams(tableId, rowId, whereClause, selArgs,
        groupBy, having, orderByElemKey, orderByDir);
    mQueries[0] = queryParams;
  }

  /**
   * Saves the queries, open fragment type, file and sub-file name and original filenames and
   * fragment types to the state bundle so they can be restored later
   *
   * @param outState the state to be saved
   */
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mCurrentFragmentType != null) {
      outState.putString(INTENT_KEY_CURRENT_VIEW_TYPE, mCurrentFragmentType.name());
    }
    if (mCurrentFileName != null) {
      outState.putString(INTENT_KEY_CURRENT_FILE_NAME, mCurrentFileName);
    }
    if (mCurrentSubFileName != null) {
      outState.putString(INTENT_KEY_CURRENT_SUB_FILE_NAME, mCurrentSubFileName);
    }
    if (mOriginalFragmentType != null) {
      outState
          .putString(Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE, mOriginalFragmentType.name());
    }
    if (mOriginalFileName != null) {
      outState.putString(Constants.IntentKeys.FILE_NAME, mOriginalFileName);
    }

    if (mQueries != null) {
      outState.putParcelableArray(INTENT_KEY_QUERIES, mQueries);
    }
  }

  @Override public void databaseUnavailable() {}

  /**
   * Display the current fragment (which should already be around) when we resume
   */
  @Override public void databaseAvailable() {
    WebLogger.getLogger(getAppName()).i(TAG, "databaseAvailable called");
    showCurrentDisplayFragment(false);

    /*
     * super.databaseAvailable() should find the top fragment on the back stack and call
     * databaseAvailable on it, but sometimes the back stack takes a while to populate with the
     * new fragment, so instead of calling super.databaseAvailable() here we just get the
     * fragment by its ID and call it ourselves
     */
    this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Fragment f = getFragmentManager().findFragmentByTag(mCurrentFragmentType.name());
        if (f instanceof DatabaseConnectionListener) {
          ((DatabaseConnectionListener) f).databaseAvailable();
        }
      }
    });
  }

  /**
   * Gets the default filename based on the passed fragment type
   *
   * @param fragmentType the type of fragment to be used to display the table
   * @return the default filename to be used for that fragment type
   */
  private String getDefaultFileNameForViewFragmentType(ViewFragmentType fragmentType) {
    if (mPossibleTableViewTypes == null || fragmentType == null) {
      return null;
    }
    switch (fragmentType) {
    case SPREADSHEET:
      return null;
    case LIST:
      return mPossibleTableViewTypes.getDefaultListViewFileName();
    case MAP:
      return mPossibleTableViewTypes.getDefaultMapListViewFileName();
    case DETAIL:
      return mPossibleTableViewTypes.getDefaultDetailFileName();
    case DETAIL_WITH_LIST:
    case SUB_LIST:
      return null; // TODO: For now there is no default detail with list. Do we need one?
    default:
      return null;
    }
  }

  /**
   * Get the {@link UserTable} that is being held by this activity. AND CHANGES mUserTable! to
   * be that table
   *
   * @return the UserTable pulled from tables
   */
  public UserTable getUserTable() {
    if (mUserTable == null) {
      DbHandle db = null;
      try {
        db = Tables.getInstance().getDatabase().openDatabase(getAppName());
        SQLQueryStruct sqlQueryStruct = IntentUtil
            .getSQLQueryStructFromBundle(this.getIntent().getExtras());
        String[] emptyArray = {};

        ArrayList<String> dbGroupBy = TableUtil.get().getGroupByColumns(Tables.getInstance()
            .getDatabase(), getAppName(), db, getTableId());
        String[] groupBy = dbGroupBy.toArray(new String[dbGroupBy.size()]);
        if (groupBy.length == 0 || getIntentExtras().containsKey("intentOverridesDatabase")) {
          groupBy = sqlQueryStruct.groupBy;
        }

        String sort = TableUtil.get().getSortColumn(Tables.getInstance()
            .getDatabase(), getAppName(), db, getTableId());
        if (sort == null || sort.length() == 0 || getIntentExtras().containsKey("intentOverridesDatabase")) {
          sort = sqlQueryStruct.orderByElementKey;
        }

        mUserTable = Tables.getInstance().getDatabase()
            .simpleQuery(this.getAppName(), db, this.getTableId(), getColumnDefinitions(),
                sqlQueryStruct.whereClause,
                (sqlQueryStruct.selectionArgs == null) ? emptyArray : sqlQueryStruct.selectionArgs,
                (groupBy == null) ? emptyArray : groupBy,
                sqlQueryStruct.having, (sort == null) ? emptyArray : new String[] { sort },
                (sqlQueryStruct.orderByDirection == null) ?
                    emptyArray :
                    new String[] { sqlQueryStruct.orderByDirection }, null, null);
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
      } finally {
        if (db != null) {
          try {
            Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
          } catch (ServicesAvailabilityException e) {
            // ignore
          }
        }
      }
    }
    return mUserTable;
  }

  /**
   * If we're on a list view, pull the filename that the list view is using, otherwise return the
   * filename if possible, or null if neither of those are set
   *
   * @param ifChanged  unused
   * @param fragmentID tells us whether we're looking at a list view or not
   * @return a URI to the currently open file, or null
   */
  @Override
  public String getUrlBaseLocation(boolean ifChanged, String fragmentID) {
    // TODO: do we need to track the ifChanged status?

    String filename;
    if (fragmentID != null && fragmentID.equals(Constants.FragmentTags.DETAIL_WITH_LIST_LIST)) {
      filename = mCurrentSubFileName;
    } else {
      filename = mCurrentFileName;
    }
    if (filename != null) {
      return UrlUtils.getAsWebViewUri(this, getAppName(), filename);
    }
    return null;
  }

  /**
   * Forwards the request to the map list found via the fragment manager
   *
   * @return the index of the currently selected item in the map list, or null if no item was
   * selected
   */
  @Override
  public Integer getIndexOfSelectedItem() {
    MapListViewFragment mlvFragment = (MapListViewFragment) this.getFragmentManager()
        .findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    if (mlvFragment != null && mlvFragment.isVisible()) {
      return mlvFragment.getIndexOfSelectedItem();
    }
    // no item selected
    return null;
  }

  /**
   * Gets the instance id from the intent's extras bundle if we're in a detail or detail with
   * list view, otherwise returns null because we can't get an instance id from a map view
   *
   * @return the instance id
   */
  @Override
  public String getInstanceId() {
    if (mCurrentFragmentType == ViewFragmentType.DETAIL
        || mCurrentFragmentType == ViewFragmentType.DETAIL_WITH_LIST) {
      return IntentUtil.retrieveRowIdFromBundle(this.getIntent().getExtras());
    }
    // map views are not considered to have a specific instanceId.
    // While one of the items happens to be distinguished, the view
    // is still a list of items.
    return null;
  }

  /**
   * Log the destroy event
   */
  @Override
  protected void onDestroy() {
    super.onDestroy();
    WebLogger.getLogger(getAppName()).d(TAG, "[onDestroy]");
  }

  /**
   * Gets the webkit object out of the fragment if we're in a list, map, detail or detail with
   * list/sublist fragment, but not if we're in a spreadsheet fragment
   *
   * @param viewID used to determine which webkit object to get if we're in a list with
   *               detail/sublist view
   * @return the webkit object if we have one open, the specifically requested webkit object if
   * we're in a view that has multiple webkit objects, or null if we have no webkit object open
   */
  @Override
  public ODKWebView getWebKitView(String viewID) {

    FragmentManager fragmentManager = this.getFragmentManager();
    switch (mCurrentFragmentType) {
    case SPREADSHEET:
      // this isn't a webkit
      return null;
    case LIST:
      ListViewFragment listViewFragment = (ListViewFragment) fragmentManager
          .findFragmentByTag(ViewFragmentType.LIST.name());
      if (listViewFragment != null) {
        return listViewFragment.getWebKit();
      }
      break;
    case MAP:
      MapListViewFragment mapListViewFragment = (MapListViewFragment) fragmentManager
          .findFragmentByTag(Constants.FragmentTags.MAP_LIST);
      if (mapListViewFragment != null) {
        return mapListViewFragment.getWebKit();
      }
      break;
    case DETAIL:
      DetailViewFragment detailViewFragment = (DetailViewFragment) fragmentManager
          .findFragmentByTag(ViewFragmentType.DETAIL.name());
      if (detailViewFragment != null) {
        return detailViewFragment.getWebKit();
      }
      break;
    case DETAIL_WITH_LIST:
    case SUB_LIST:
      if (viewID == null || viewID.equals(Constants.FragmentTags.DETAIL_WITH_LIST_DETAIL)) {
        DetailWithListDetailViewFragment detailWithListDetailViewFragment = (DetailWithListDetailViewFragment) fragmentManager
            .findFragmentByTag(Constants.FragmentTags.DETAIL_WITH_LIST_DETAIL);
        if (detailWithListDetailViewFragment != null) {
          return detailWithListDetailViewFragment.getWebKit();
        }
      } else if (viewID.equals(Constants.FragmentTags.DETAIL_WITH_LIST_LIST)) {
        // webkit to get
        DetailWithListListViewFragment subListViewFragment = (DetailWithListListViewFragment) fragmentManager
            .findFragmentByTag(Constants.FragmentTags.DETAIL_WITH_LIST_LIST);
        if (subListViewFragment != null) {
          return subListViewFragment.getWebKit();
        }
      }
      break;

    }
    return null;
  }

  /**
   * Called when the user clicks the icon with the four horizontal lines in the bar on the top.
   * Populates the list of available view types (Spreadsheet, List and Map for most tables) and
   * sets some options to disabled if no files are set up to be used to view like that
   *
   * @param menu The menu to be opened
   * @return {@link AbsBaseActivity::onCreateOptionsMenu}
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // clear the menu so that we don't double inflate
    menu.clear();
    MenuInflater menuInflater = this.getMenuInflater();
    switch (mCurrentFragmentType) {
    case SPREADSHEET:
    case LIST:
    case MAP:
      /*
       * Disable or enable those menu items corresponding to view types that are
       * currently invalid or valid, respectively.
       */
      menuInflater.inflate(R.menu.top_level_table_menu, menu);
      MenuItem spreadsheetItem = menu.findItem(R.id.top_level_table_menu_view_spreadsheet_view);
      MenuItem listItem = menu.findItem(R.id.top_level_table_menu_view_list_view);
      MenuItem mapItem = menu.findItem(R.id.top_level_table_menu_view_map_view);
      spreadsheetItem.setEnabled(true); // always possible
      listItem.setEnabled(
          (mPossibleTableViewTypes != null) && mPossibleTableViewTypes.listViewIsPossible());
      mapItem.setEnabled(
          (mPossibleTableViewTypes != null) && mPossibleTableViewTypes.mapViewIsPossible());
      // Set the checkbox highlight to the view type being displayed.
      switch (mCurrentFragmentType) {
      case SPREADSHEET:
        spreadsheetItem.setChecked(true);
        break;
      case LIST:
        listItem.setChecked(true);
        break;
      case MAP:
        mapItem.setChecked(true);
        break;
      }
      break;
    case DETAIL:
      menuInflater.inflate(R.menu.detail_view_menu, menu);
      break;
    case SUB_LIST: // This should never happen...
    case DETAIL_WITH_LIST:
      menuInflater.inflate(R.menu.detail_view_menu, menu);
      break;
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    String filename = null;
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, getAppName());
    switch (item.getItemId()) {
    case R.id.top_level_table_menu_view_spreadsheet_view:
      setCurrentFragmentType(ViewFragmentType.SPREADSHEET, null, null);
      return true;
    case R.id.top_level_table_menu_view_list_view:
      if (mOriginalFragmentType != null && mOriginalFragmentType == ViewFragmentType.LIST) {
        filename = mOriginalFileName;
      }
      if (filename == null) {
        filename = (mPossibleTableViewTypes != null) ?
            mPossibleTableViewTypes.getDefaultListViewFileName() :
            null;
      }
      setCurrentFragmentType(ViewFragmentType.LIST, filename, null);
      return true;
    case R.id.top_level_table_menu_view_map_view:
      if (mOriginalFragmentType != null && mOriginalFragmentType == ViewFragmentType.MAP) {
        filename = mOriginalFileName;
      }
      if (filename == null) {
        filename = (mPossibleTableViewTypes != null) ?
            mPossibleTableViewTypes.getDefaultMapListViewFileName() :
            null;
      }
      setCurrentFragmentType(ViewFragmentType.MAP, filename, null);
      return true;
    case R.id.top_level_table_menu_add:
      WebLogger.getLogger(getAppName()).d(TAG, "[onOptionsItemSelected] add selected");
      try {
        ActivityUtil
            .addRow(this, this.getAppName(), this.getTableId(), null);
      } catch (ServicesAvailabilityException e) {
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
        WebLogger.getLogger(getAppName())
            .e(TAG, "[onOptionsItemSelected trying to edit row, but row id is null");
        Toast
            .makeText(this, getString(R.string.cannot_edit_row_please_try_again), Toast.LENGTH_LONG)
            .show();
        return true;
      }
      try {
        ActivityUtil
            .editRow(this, this.getAppName(), this.getTableId(), rowId);
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
      }
      return true;
    case R.id.menu_table_manager_sync:
      try {
        Intent syncIntent = new Intent();
        syncIntent.setComponent(
            new ComponentName(IntentConsts.Sync.APPLICATION_NAME, IntentConsts.Sync.ACTIVITY_NAME));
        syncIntent.setAction(Intent.ACTION_DEFAULT);
        syncIntent.putExtras(bundle);
        this.startActivityForResult(syncIntent, Constants.RequestCodes.LAUNCH_SYNC);
      } catch (ActivityNotFoundException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        Toast.makeText(this, R.string.sync_not_found, Toast.LENGTH_LONG).show();
      }
      return true;
    case R.id.menu_table_manager_preferences:
      Intent preferenceIntent = new Intent();
      preferenceIntent.setComponent(new ComponentName(IntentConsts.AppProperties.APPLICATION_NAME,
          IntentConsts.AppProperties.ACTIVITY_NAME));
      preferenceIntent.setAction(Intent.ACTION_DEFAULT);
      preferenceIntent.putExtras(bundle);
      this.startActivityForResult(preferenceIntent, Constants.RequestCodes.LAUNCH_DISPLAY_PREFS);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Called when an activity returns to this activity. Handles add and edit row actions
   *
   * @param requestCode Which activity is being returned from
   * @param resultCode  whether the activity was successful or not
   * @param data        The intent we used to open it
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // for most returns, we just refresh the data set and redraw the page
    // for others, we need to take more intensive action
    switch (requestCode) {
    case Constants.RequestCodes.ADD_ROW_SURVEY:
    case Constants.RequestCodes.EDIT_ROW_SURVEY:
      if (resultCode == Activity.RESULT_OK) {
        WebLogger.getLogger(getAppName())
            .d(TAG, "[onActivityResult] result ok, refreshing backing table");
      } else {
        WebLogger.getLogger(getAppName())
            .d(TAG, "[onActivityResult] result canceled, refreshing backing table");
      }
      break;
    }

    super.onActivityResult(requestCode, resultCode, data);

    try {
      // verify that the data table doesn't contain checkpoints...
      // always refresh, as table properties may have done something
      refreshDataAndDisplayFragment();
    } catch (IllegalStateException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }
  }

  /**
   * Refreshes the data in the fragment
   */
  public void refreshDataAndDisplayFragment() {
    WebLogger.getLogger(getAppName()).d(TAG, "refreshDataAndDisplayFragment called");
    // drop cached table, if any...
    mUserTable = null;
    // drop default filenames...
    mPossibleTableViewTypes = null;
    // destroy and recreate fragment
    showCurrentDisplayFragment(true);
  }

  /**
   * Set the current type of fragment that is being displayed.
   * Called when mocking interface.
   *
   * @param requestedType The type of fragment, a SpreadsheetFragment or something
   * @param fileName      Used for list/map/detail/etc.. views, just stored for later
   * @param subFileName   Used for list/map/detail/etc.. views, just stored for later
   */
  public void setCurrentFragmentType(ViewFragmentType requestedType, String fileName,
      String subFileName) {
    if (requestedType != ViewFragmentType.SPREADSHEET && fileName == null
        && mPossibleTableViewTypes != null) {
      fileName = getDefaultFileNameForViewFragmentType(requestedType);
    }
    mCurrentFragmentType = requestedType;
    mCurrentFileName = fileName;
    mCurrentSubFileName = subFileName;
    showCurrentDisplayFragment(false);
  }

  /**
   * Used in showCurrentDisplayFragment, tries to pull the possible table view types from the
   * database and set up the default filename and sub-filename if possible
   */
  private void possiblySupplyDefaults() {

    if (mPossibleTableViewTypes == null && Tables.getInstance().getDatabase() != null) {
      DbHandle db = null;
      try {
        db = Tables.getInstance().getDatabase().openDatabase(getAppName());
        mPossibleTableViewTypes = new PossibleTableViewTypes(getAppName(), db, getTableId(),
            getColumnDefinitions());
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        WebLogger.getLogger(getAppName()).e(TAG, "[databaseAvailable] unable to access database");
        Toast.makeText(this, getString(R.string.error_accessing_database), Toast.LENGTH_LONG)
            .show();
      } finally {
        if (db != null) {
          try {
            Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(getAppName()).printStackTrace(e);
            WebLogger.getLogger(getAppName())
                .e(TAG, "[databaseAvailable] unable to access database");
            Toast.makeText(this, getString(R.string.error_accessing_database), Toast.LENGTH_LONG)
                .show();
          }
        }
      }
    }

    if (mOriginalFragmentType == null && mPossibleTableViewTypes != null) {
      // recover the default view for this table from the database...
      mOriginalFragmentType = mPossibleTableViewTypes.getDefaultViewType();
    }

    ViewFragmentType original = mOriginalFragmentType;
    if (mOriginalFragmentType == null) {
      // and if that isn't set, use spreadsheet
      WebLogger.getLogger(getAppName())
          .i(TAG, "[retrieveFragmentTypeToDisplay] no view type found, defaulting to spreadsheet");
      original = ViewFragmentType.SPREADSHEET;
    }

    if (mOriginalFileName == null) {
      mOriginalFileName = getDefaultFileNameForViewFragmentType(mOriginalFragmentType);
    }

    if (mCurrentFragmentType == null) {
      mCurrentFragmentType = original;
      mCurrentFileName = mOriginalFileName;
    }

    if (mCurrentFileName == null) {
      mCurrentFileName = getDefaultFileNameForViewFragmentType(mCurrentFragmentType);
    }

    if ((mCurrentFragmentType == ViewFragmentType.DETAIL_WITH_LIST
        || mCurrentFragmentType == ViewFragmentType.SUB_LIST) && mCurrentSubFileName == null) {
      mCurrentSubFileName = getDefaultFileNameForViewFragmentType(ViewFragmentType.SUB_LIST);
    }
  }

  /**
   * Initialize the correct display fragment based on the result of
   * {@link #retrieveTableIdFromIntent()}. Initializes Spreadsheet if none is
   * present in Intent.
   */
  private void showCurrentDisplayFragment(boolean createNew) {
    possiblySupplyDefaults();
    updateChildViewVisibility(mCurrentFragmentType);
    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    // First acquire all the possible fragments.
    AbsBaseFragment spreadsheetFragment = (AbsBaseFragment) fragmentManager
        .findFragmentByTag(ViewFragmentType.SPREADSHEET.name());
    AbsBaseFragment listViewFragment = (AbsBaseFragment) fragmentManager
        .findFragmentByTag(ViewFragmentType.LIST.name());
    AbsBaseFragment mapListViewFragment = (AbsBaseFragment) fragmentManager
        .findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    Fragment innerMapFragment = fragmentManager
        .findFragmentByTag(Constants.FragmentTags.MAP_INNER_MAP);
    AbsBaseFragment detailViewFragment = (AbsBaseFragment) fragmentManager
        .findFragmentByTag(ViewFragmentType.DETAIL.name());
    AbsWebTableFragment detailWithListViewDetailFragment = (AbsWebTableFragment) fragmentManager
        .findFragmentByTag(Constants.FragmentTags.DETAIL_WITH_LIST_DETAIL);
    AbsWebTableFragment detailWithListViewListFragment = (AbsWebTableFragment) fragmentManager
        .findFragmentByTag(Constants.FragmentTags.DETAIL_WITH_LIST_LIST);

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
    if (mCurrentFragmentType != ViewFragmentType.DETAIL_WITH_LIST) {
      if (detailWithListViewDetailFragment != null) {
        fragmentTransaction.hide(detailWithListViewDetailFragment);
      }
      if (detailWithListViewListFragment != null) {
        fragmentTransaction.hide(detailWithListViewListFragment);
      }
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
          WebLogger.getLogger(getAppName())
              .i(TAG, "[showSpreadsheetFragment] removing existing fragment");
          // Get rid of the existing fragment
          fragmentTransaction.remove(spreadsheetFragment);
        }
        WebLogger.getLogger(getAppName()).i(TAG, "Creating NEW spreadsheet fragment");
        spreadsheetFragment = new SpreadsheetFragment();
        fragmentTransaction
            .add(R.id.activity_table_display_activity_one_pane_content, spreadsheetFragment,
                mCurrentFragmentType.name());
      } else {
        WebLogger.getLogger(getAppName()).i(TAG, "Showing existing spreadsheet fragment");
        fragmentTransaction.show(spreadsheetFragment);
      }
      break;
    case DETAIL:
      if (detailViewFragment == null || createNew) {
        if (detailViewFragment != null) {
          WebLogger.getLogger(getAppName())
              .d(TAG, "[showDetailViewFragment] removing existing fragment");
          // Get rid of the existing fragment
          fragmentTransaction.remove(detailViewFragment);
        }
        detailViewFragment = new DetailViewFragment();
        fragmentTransaction
            .add(R.id.activity_table_display_activity_one_pane_content, detailViewFragment,
                mCurrentFragmentType.name());
      } else {
        fragmentTransaction.show(detailViewFragment);
      }
      break;
    case SUB_LIST:
    case DETAIL_WITH_LIST:
      if (detailWithListViewDetailFragment == null || createNew) {
        if (detailWithListViewDetailFragment != null) {
          // remove the old fragment
          WebLogger.getLogger(getAppName())
              .d(TAG, "[showDetailWithListFragment] removing old detail fragment");
          fragmentTransaction.remove(detailWithListViewDetailFragment);
        }
        detailWithListViewDetailFragment = new DetailWithListDetailViewFragment();
        fragmentTransaction.add(R.id.top_pane, detailWithListViewDetailFragment,
            Constants.FragmentTags.DETAIL_WITH_LIST_DETAIL);
      } else {
        fragmentTransaction.show(detailWithListViewDetailFragment);
      }
      if (detailWithListViewListFragment == null || createNew) {
        if (detailWithListViewListFragment != null) {
          // remove the old fragment
          WebLogger.getLogger(getAppName())
              .d(TAG, "[showDetailWithListFragment] removing old list fragment");
          fragmentTransaction.remove(detailWithListViewListFragment);
        }
        detailWithListViewListFragment = new DetailWithListListViewFragment();
        fragmentTransaction.add(R.id.bottom_pane, detailWithListViewListFragment,
            Constants.FragmentTags.DETAIL_WITH_LIST_LIST);
      } else {
        fragmentTransaction.show(detailWithListViewListFragment);
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
        fragmentTransaction
            .add(R.id.activity_table_display_activity_one_pane_content, listViewFragment,
                mCurrentFragmentType.name());
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
        fragmentTransaction
            .add(R.id.map_view_list, mapListViewFragment, Constants.FragmentTags.MAP_LIST);
      } else {
        fragmentTransaction.show(mapListViewFragment);
      }
      if (innerMapFragment == null || createNew) {
        if (innerMapFragment != null) {
          // remove the old fragment
          WebLogger.getLogger(getAppName())
              .d(TAG, "[showMapFragment] removing old inner map fragment");
          fragmentTransaction.remove(innerMapFragment);
        }
        innerMapFragment = new TableMapInnerFragment();
        fragmentTransaction
            .add(R.id.map_view_inner_map, innerMapFragment, Constants.FragmentTags.MAP_INNER_MAP);
        ((TableMapInnerFragment) innerMapFragment).listener = this;
      } else {
        ((TableMapInnerFragment) innerMapFragment).listener = this;
        fragmentTransaction.show(innerMapFragment);
      }
      break;
    default:
      WebLogger.getLogger(getAppName())
          .e(TAG, "ViewFragmentType not recognized: " + this.mCurrentFragmentType);
      break;
    }
    fragmentTransaction.commit();

    invalidateOptionsMenu();
  }

  /**
   * Gives new arguments to an existing fragment
   *
   * @param fragmentID The id of the fragment to search the fragment manager for
   * @param args       the arguments to give to the fragment
   */
  public void updateFragment(String fragmentID, Bundle args) {
    if (!fragmentID.equals(Constants.FragmentTags.DETAIL_WITH_LIST_LIST)) {
      WebLogger.getLogger(getAppName())
          .e(TAG, "[updateFragment] Attempted to update an unsupported fragment id: " + fragmentID);
      return;
    }

    String fragmentType = IntentUtil.retrieveFragmentViewTypeFromBundle(args);
    if (fragmentType == null || !fragmentType.equals(ViewFragmentType.SUB_LIST.name())) {
      WebLogger.getLogger(getAppName())
          .e(TAG, "[updateFragment] Cannot update a fragment of type " + fragmentType);
      return;
    }

    String tableId = IntentUtil.retrieveTableIdFromBundle(args);
    String rowId = IntentUtil.retrieveRowIdFromBundle(args);
    mCurrentSubFileName = IntentUtil.retrieveFileNameFromBundle(args);
    SQLQueryStruct query = IntentUtil.getSQLQueryStructFromBundle(args);
    ViewDataQueryParams queryParams = new ViewDataQueryParams(tableId, rowId, query.whereClause,
        query.selectionArgs, query.groupBy, query.having, query.orderByElementKey,
        query.orderByElementKey);
    mQueries[1] = queryParams;

    FragmentManager fragmentManager = this.getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

    AbsWebTableFragment detailWithListViewListFragment = (AbsWebTableFragment) fragmentManager
        .findFragmentByTag(Constants.FragmentTags.DETAIL_WITH_LIST_LIST);

    if (detailWithListViewListFragment != null) {
      // remove the old fragment
      WebLogger.getLogger(getAppName())
          .d(TAG, "[showDetailWithListFragment] removing old list " + "fragment");
      fragmentTransaction.remove(detailWithListViewListFragment);
    }
    detailWithListViewListFragment = new DetailWithListListViewFragment();
    fragmentTransaction.add(R.id.bottom_pane, detailWithListViewListFragment,
        Constants.FragmentTags.DETAIL_WITH_LIST_LIST);
    fragmentTransaction.commit();
  }

  /**
   * Gets the view parameters being used with that fragment
   *
   * @param fragmentID which fragment to get the query parameters from
   * @return the query parameters for that fragment
   * @throws IllegalArgumentException if the database is down
   */
  @Override
  public ViewDataQueryParams getViewQueryParams(String fragmentID) throws IllegalArgumentException {

    int queryIndex = 0;

    if (fragmentID != null && fragmentID.equals(Constants.FragmentTags.DETAIL_WITH_LIST_LIST)) {
      queryIndex = 1;
    }

    if (mQueries == null) {
      return null;
    }

    return mQueries[queryIndex];
  }

  /**
   * Update the content view's children visibility for viewFragmentType. This is
   * required due to the fact that not all the fragments make use of the same
   * children views within the activity.
   *
   * @param viewFragmentType the type of fragment to update
   */
  void updateChildViewVisibility(ViewFragmentType viewFragmentType) {
    // The map fragments occupy a different view than the single pane
    // content. This is because the map is two views--the list and the map
    // itself. So, we need to hide and show the others as appropriate.
    View onePaneContent = this.findViewById(R.id.activity_table_display_activity_one_pane_content);
    View splitContent = this.findViewById(R.id.activity_table_display_activity_split_content);
    View mapContent = this.findViewById(R.id.activity_table_display_activity_map_content);
    switch (viewFragmentType) {
    case DETAIL:
    case LIST:
    case SPREADSHEET:
      onePaneContent.setVisibility(View.VISIBLE);
      splitContent.setVisibility(View.GONE);
      mapContent.setVisibility(View.GONE);
      return;
    case DETAIL_WITH_LIST:
    case SUB_LIST:
      onePaneContent.setVisibility(View.GONE);
      splitContent.setVisibility(View.VISIBLE);
      mapContent.setVisibility(View.GONE);
      return;
    case MAP:
      onePaneContent.setVisibility(View.GONE);
      splitContent.setVisibility(View.GONE);
      mapContent.setVisibility(View.VISIBLE);
      return;
    default:
      WebLogger.getLogger(getAppName())
          .e(TAG, "[updateChildViewVisibility] unrecognized type: " + viewFragmentType);
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
      WebLogger.getLogger(getAppName())
          .e(TAG, "[onSetIndex] mapListViewFragment is null! Returning");
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
      WebLogger.getLogger(getAppName())
          .e(TAG, "[setNoItemSelected] mapListViewFragment is null! Returning");
    } else {
      mapListViewFragment.setNoItemSelected();
    }
  }

  /**
   * Do nothing when initialization is completed
   */
  @Override
  public void initializationCompleted() {
  }

  /**
   * The fragment types this activity could be displaying.
   *
   * @author sudar.sam@gmail.com
   */
  public enum ViewFragmentType {
    SPREADSHEET, LIST, MAP, DETAIL, DETAIL_WITH_LIST, SUB_LIST
  }

}
