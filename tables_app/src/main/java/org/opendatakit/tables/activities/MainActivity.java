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

import android.app.*;
import android.app.FragmentManager.BackStackEntry;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import org.opendatakit.activities.IInitResumeActivity;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.fragment.AboutMenuFragment;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.InitializationFragment;
import org.opendatakit.tables.fragments.TableManagerFragment;
import org.opendatakit.tables.fragments.WebFragment;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.utilities.ODKFileUtils;
import org.opendatakit.views.ODKWebView;
import org.opendatakit.views.ViewDataQueryParams;
import org.opendatakit.webkitserver.utilities.UrlUtils;

import java.io.File;
import java.util.Collections;

/**
 * The main activity for ODK Tables. It serves primarily as a holder for fragments.
 *
 * @author sudar.sam@gmail.com
 */
public class MainActivity extends AbsBaseWebActivity
    implements DatabaseConnectionListener, IInitResumeActivity {

  // Used for logging
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final String CURRENT_FRAGMENT = "currentFragment";
  private static final String QUERY_START_PARAM = "?";
  /**
   * The active screen -- retained state
   */
  ScreenType activeScreenType = ScreenType.TABLE_MANAGER_SCREEN;
  File webFileToDisplay = null;
  /**
   * used to determine whether we need to change the menu (action bar) because of a change in the
   * active fragment.
   */
  private ScreenType lastMenuType = null;

  /**
   * Finds the webkit object in the fragment manager and returns it, if we're on a web view screen
   *
   * @param viewID unused
   * @return the webkit view or null if there is no web view
   */
  @Override
  public ODKWebView getWebKitView(String viewID) {
    // Don't use viewID as there is only one webkit to return

    if (activeScreenType == ScreenType.WEBVIEW_SCREEN) {
      FragmentManager mgr = this.getFragmentManager();
      Fragment newFragment = mgr.findFragmentByTag(activeScreenType.name());
      if (newFragment != null) {
        return ((WebFragment) newFragment).getWebKit();
      }
    }
    return null;
  }

  /**
   * Gets the web view out of the fragment manager, gets its uri and parses it into a filename
   * and its query.
   *
   * @param ifChanged  unused
   * @param fragmentID unused
   * @return A URI that represents the current location of the web view, with query strings if
   * needed
   */
  @Override
  public String getUrlBaseLocation(boolean ifChanged, String fragmentID) {
    // TODO: do we need to track the ifChanged status?
    if (activeScreenType == ScreenType.WEBVIEW_SCREEN) {
      FragmentManager mgr = this.getFragmentManager();
      Fragment newFragment = mgr.findFragmentByTag(activeScreenType.name());
      if (newFragment != null && webFileToDisplay != null) {
        // Split off query parameter if it exists
        String[] webFileStrs = checkForQueryParameter(webFileToDisplay);
        String filename = null;
        if (webFileStrs.length > 1) {
          File webFile = new File(webFileStrs[0]);
          filename = ODKFileUtils.asRelativePath(mAppName, webFile);
        } else {
          filename = ODKFileUtils.asRelativePath(mAppName, webFileToDisplay);
        }

        if (filename != null) {
          if (webFileStrs.length > 1) {
            return UrlUtils.getAsWebViewUri(this, getAppName(),
                filename.concat(QUERY_START_PARAM).concat(webFileStrs[1]));
          } else {
            return UrlUtils.getAsWebViewUri(this, getAppName(), filename);
          }
        }
      }
    }
    return null;
  }

  @Override
  public Integer getIndexOfSelectedItem() {
    // never a map view -- no item selected
    return null;
  }

  /**
   * If the app is configured to use a home screen, then load that home screen in a webview.
   * Otherwise, if we're restoring from a saved state, set the active screen type to the screen
   * type we were using when we got saved.
   *
   * @param savedInstanceState the state we saved in onSaveInstanceState
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.activity_main_activity);

    webFileToDisplay = getHomeScreen(savedInstanceState);

    if (webFileToDisplay != null) {
      activeScreenType = ScreenType.WEBVIEW_SCREEN;
    }

    if (savedInstanceState != null) {
      // if we are restoring, assume that initialization has already occurred.
      activeScreenType = ScreenType.valueOf(savedInstanceState.containsKey(CURRENT_FRAGMENT) ?
          savedInstanceState.getString(CURRENT_FRAGMENT) :
          activeScreenType.name());
    }
  }

  @Override
  public String getTableId() {
    return null;
  }

  @Override
  public String getInstanceId() {
    return null;
  }

  /**
   * Saves the current screen type to the output state bundle, and if we're viewing a file in a
   * webview, save that too.
   *
   * @param outState the state to be saved
   */
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(CURRENT_FRAGMENT, activeScreenType.name());
    if (webFileToDisplay != null) {
      outState.putString(Constants.IntentKeys.FILE_NAME,
          ODKFileUtils.asRelativePath(mAppName, webFileToDisplay));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    swapScreens(activeScreenType);
  }

  /**
   * Retrieve the app-relative file name from either the
   * saved instance state or the {@link Intent} that was
   * used to create the activity.
   * <p>
   * If none supplied, then return the default home screen
   * if we are configured to show it. If we are configured
   * to show it and it is not present, clear that flag.
   *
   * @return
   */
  protected File getHomeScreen(Bundle savedInstanceState) {
    Boolean setting = mProps.getBooleanProperty(CommonToolProperties.KEY_USE_HOME_SCREEN);
    String relativeFileName = IntentUtil
        .retrieveFileNameFromSavedStateOrArguments(savedInstanceState,
            this.getIntent().getExtras());

    File userHomeScreen = null;
    if (relativeFileName != null) {
      userHomeScreen = ODKFileUtils.asAppFile(mAppName, relativeFileName);
    } else {
      userHomeScreen = new File(ODKFileUtils.getTablesHomeScreenFile(this.mAppName));
    }

    // Make sure that query parameters are still passed through
    String[] userHomeScreenUrlParts = checkForQueryParameter(userHomeScreen);
    File userHomeScreenFile = userHomeScreen;
    if (userHomeScreenUrlParts.length > 1) {
      userHomeScreenFile = new File(userHomeScreenUrlParts[0]);
    }

    if ((relativeFileName != null) || (setting == null ? false : setting) || (
        userHomeScreenFile.exists() && userHomeScreenFile.isFile())) {
      return userHomeScreen;
    } else {
      if ((setting == null || setting == Boolean.TRUE) && relativeFileName == null) {
        // the home screen doesn't exist but we are requesting to show it -- clear the setting
        mProps.setProperties(Collections
            .singletonMap(CommonToolProperties.KEY_USE_HOME_SCREEN, Boolean.toString(false)));
      }
      return null;
    }
  }

  private String[] checkForQueryParameter(File webFile) {
    String webFileToDisplayPath = webFile.getPath();
    String[] webFileStrs = webFileToDisplayPath.split("[" + QUERY_START_PARAM + "]", 2);
    return webFileStrs;
  }

  private void popBackStack() {
    FragmentManager mgr = getFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 2;
    if (idxLast < 0) {
      Intent result = new Intent();
      this.setResult(RESULT_OK, result);
      finish();
    } else {
      BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      swapScreens(ScreenType.valueOf(entry.getName()));
    }
  }

  @Override
  public void initializationCompleted() {
    popBackStack();
  }

  @Override
  public void onBackPressed() {
    popBackStack();
  }

  public ScreenType getCurrentScreenType() {
    return activeScreenType;
  }

  /**
   * Transitions the active screen to the passed argument. If the fragment manager already has a
   * fragment of the requested type, we reuse that. If there is a matching fragment in the
   * fragment manager's back stack, we switch to that using a fragment manager transaction.
   * Otherwise we create a new one. However, if we need to reinitialize, we actually transition
   * to the initialization screen.
   *
   * @param newScreenType what screen type to use
   */
  public void swapScreens(ScreenType newScreenType) {
    WebLogger.getLogger(getAppName()).i(TAG,
        "swapScreens: Transitioning from " + ((activeScreenType == null) ?
            "-none-" :
            activeScreenType.name()) + " to " + newScreenType.name());
    FragmentManager mgr = this.getFragmentManager();
    FragmentTransaction trans = null;
    Fragment newFragment = null;
    switch (newScreenType) {
    case TABLE_MANAGER_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new TableManagerFragment();
      }
      break;
    case WEBVIEW_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new WebFragment();
      }
      break;
    case ABOUT_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new AboutMenuFragment();
      }
      break;
    case INITIALIZATION_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if (newFragment == null) {
        newFragment = new InitializationFragment();
      }
      break;
    default:
      throw new IllegalStateException("Unexpected default case");
    }

    boolean matchingBackStackEntry = false;
    for (int i = 0; i < mgr.getBackStackEntryCount(); ++i) {
      BackStackEntry e = mgr.getBackStackEntryAt(i);
      WebLogger.getLogger(mAppName).i(TAG, "BackStackEntry[" + i + "] " + e.getName());
      if (e.getName().equals(newScreenType.name())) {
        matchingBackStackEntry = true;
      }
    }

    if (matchingBackStackEntry) {
      if (trans != null) {
        WebLogger.getLogger(mAppName).e(TAG, "Unexpected active transaction when popping state!");
        trans = null;
      }
      // flush backward, to the screen we want to go back to
      activeScreenType = newScreenType;
      mgr.popBackStackImmediate(activeScreenType.name(), 0);
    } else {
      // add transaction to show the screen we want
      if (trans == null) {
        trans = mgr.beginTransaction();
      }
      activeScreenType = newScreenType;
      trans.replace(R.id.activity_main_activity, newFragment, activeScreenType.name());
      trans.addToBackStack(activeScreenType.name());
    }

    // and see if we should re-initialize...
    if ((activeScreenType != ScreenType.INITIALIZATION_SCREEN) && Tables.getInstance()
        .shouldRunInitializationTask(getAppName())) {
      WebLogger.getLogger(getAppName())
          .i(TAG, "swapToFragmentView -- calling clearRunInitializationTask");
      // and immediately clear the should-run flag...
      Tables.getInstance().clearRunInitializationTask(getAppName());
      // OK we should swap to the InitializationFragment view
      // this will skip the transition to whatever screen we were trying to
      // go to and will instead show the InitializationFragment view. We
      // restore to the desired screen via the setFragmentToShowNext()
      //
      // NOTE: this discards the uncommitted transaction.
      // Robolectric complains about a recursive state transition.
      if (trans != null) {
        trans.commit();
      }
      swapScreens(ScreenType.INITIALIZATION_SCREEN);
    } else {
      if (trans != null) {
        trans.commit();
      }
      invalidateOptionsMenu();
    }
  }

  private void changeOptionsMenu(Menu menu) {
    MenuInflater menuInflater = this.getMenuInflater();
    if (activeScreenType == ScreenType.WEBVIEW_SCREEN) {
      menuInflater.inflate(R.menu.web_view_activity, menu);
    } else if (activeScreenType == ScreenType.TABLE_MANAGER_SCREEN) {
      menuInflater.inflate(R.menu.table_manager, menu);
    }
    lastMenuType = activeScreenType;

    ActionBar actionBar = getActionBar();
    actionBar.show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    changeOptionsMenu(menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    if (lastMenuType != activeScreenType) {
      changeOptionsMenu(menu);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  /**
   * Called when the user clicks on an option in the main menu, including the import/export
   * buttons at the top, the options menu with "Sync", "Preferences" and "About" in it and any of
   * those three options,
   *
   * @param item The id of the item that the user clicked on
   * @return
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    String appName = getAppName();
    WebLogger.getLogger(appName).d(TAG, "[onOptionsItemSelected] selecting an item");
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, appName);
    switch (item.getItemId()) {
    case R.id.menu_web_view_activity_table_manager:
      swapScreens(ScreenType.TABLE_MANAGER_SCREEN);
      return true;
    case R.id.menu_table_about:
      swapScreens(ScreenType.ABOUT_SCREEN);
      return true;
    case R.id.menu_table_manager_preferences:
      Intent preferenceIntent = new Intent();
      preferenceIntent.setComponent(new ComponentName(IntentConsts.AppProperties.APPLICATION_NAME,
          IntentConsts.AppProperties.ACTIVITY_NAME));
      preferenceIntent.setAction(Intent.ACTION_DEFAULT);
      preferenceIntent.putExtras(bundle);
      this.startActivityForResult(preferenceIntent, Constants.RequestCodes.LAUNCH_DISPLAY_PREFS);
      return true;
    case R.id.menu_table_manager_import:
      Intent importIntent = new Intent(this, ImportCSVActivity.class);
      importIntent.putExtras(bundle);
      this.startActivityForResult(importIntent, Constants.RequestCodes.LAUNCH_IMPORT);
      return true;
    case R.id.menu_table_manager_export:
      Intent exportIntent = new Intent(this, ExportCSVActivity.class);
      exportIntent.putExtras(bundle);
      this.startActivityForResult(exportIntent, Constants.RequestCodes.LAUNCH_EXPORT);
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
        WebLogger.getLogger(appName).printStackTrace(e);
        Toast.makeText(this, R.string.sync_not_found, Toast.LENGTH_LONG).show();
      }
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Callbacks for WebFragment activities
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    String tableId = this.getActionTableId();
    if (tableId != null) {
      switch (requestCode) {
      case Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER:
      case Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER:
        // don't let the user cancel out of these...
        break;
      // For now, we will just refresh the table if something could have
      // changed.
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
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public ViewDataQueryParams getViewQueryParams(String viewID) {
    // Ignore viewID, there is only one fragment

    Bundle bundle = this.getIntentExtras();

    String tableId = IntentUtil.retrieveTableIdFromBundle(bundle);
    String rowId = IntentUtil.retrieveRowIdFromBundle(bundle);
    if (tableId == null || tableId.isEmpty()) {
      throw new IllegalArgumentException("Tables view launched without tableId specified");
    }

    SQLQueryStruct sqlQueryStruct = IntentUtil.getSQLQueryStructFromBundle(bundle);

    ViewDataQueryParams params = new ViewDataQueryParams(tableId, rowId, sqlQueryStruct.whereClause,
        sqlQueryStruct.selectionArgs, sqlQueryStruct.groupBy, sqlQueryStruct.having,
        sqlQueryStruct.orderByElementKey, sqlQueryStruct.orderByDirection);

    return params;
  }

  public enum ScreenType {
    INITIALIZATION_SCREEN, TABLE_MANAGER_SCREEN, ABOUT_SCREEN, WEBVIEW_SCREEN
  }

}
