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

import java.io.File;

import org.opendatakit.common.android.activities.IInitResumeActivity;
import org.opendatakit.common.android.fragment.AboutMenuFragment;
import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.common.android.logic.PropertiesSingleton;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.InitializationFragment;
import org.opendatakit.tables.fragments.TableManagerFragment;
import org.opendatakit.tables.fragments.WebFragment;
import org.opendatakit.tables.logic.TablesToolProperties;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * The main activity for ODK Tables. It serves primarily as a holder for
 * fragments.
 * @author sudar.sam@gmail.com
 *
 */
public class MainActivity extends AbsBaseActivity implements
    DatabaseConnectionListener, IInitResumeActivity {

  private static final String TAG = "MainActivity";
  private static final String CURRENT_FRAGMENT = "currentFragment";
  
  public enum ScreenType {
    INITIALIZATION_SCREEN,
    TABLE_MANAGER_SCREEN,
    ABOUT_SCREEN,
    WEBVIEW_SCREEN
  };
  
  /**
   * The active screen -- retained state
   */
  ScreenType activeScreenType = ScreenType.TABLE_MANAGER_SCREEN;

  File webFileToDisplay = null;
  
  /** 
   * used to determine whether we need to change the menu (action bar)
   * because of a change in the active fragment.
   */
  private ScreenType lastMenuType = null;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.activity_main_activity);
    
    webFileToDisplay = getHomeScreen(savedInstanceState);
    
    if ( webFileToDisplay != null ) {
      activeScreenType = ScreenType.WEBVIEW_SCREEN;
    }

    if (savedInstanceState != null) {
      // if we are restoring, assume that initialization has already occurred.
      activeScreenType = ScreenType
          .valueOf(savedInstanceState.containsKey(CURRENT_FRAGMENT) ? savedInstanceState
              .getString(CURRENT_FRAGMENT) : activeScreenType.name());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(CURRENT_FRAGMENT, activeScreenType.name());
    if ( webFileToDisplay != null ) {
      outState.putString(Constants.IntentKeys.FILE_NAME, ODKFileUtils.asRelativePath(mAppName, webFileToDisplay));
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
   * 
   * If none supplied, then return the default home screen
   * if we are configured to show it. If we are configured
   * to show it and it is not present, clear that flag.
   * 
   * @return
   */
  protected File getHomeScreen(Bundle savedInstanceState) {
    PropertiesSingleton props = TablesToolProperties.get(this, mAppName);
    Boolean setting = props.getBooleanProperty(TablesToolProperties.KEY_USE_HOME_SCREEN);
    String relativeFileName = 
        IntentUtil.retrieveFileNameFromSavedStateOrArguments(savedInstanceState, this.getIntent().getExtras());
    
    File userHomeScreen = null;
    if ( relativeFileName != null ) {
      userHomeScreen = ODKFileUtils.asAppFile(mAppName, relativeFileName);
    } else {
      userHomeScreen = new File(ODKFileUtils.getTablesHomeScreenFile(this.mAppName));
    }
    if (((relativeFileName != null) || 
         (setting == null ? false : setting)) &&
         userHomeScreen.exists() && userHomeScreen.isFile()) {
      return userHomeScreen;
    } else {
      if ( setting == true && relativeFileName == null ) {
        // the home screen doesn't exist but we are requesting to show it -- clear the setting
        props.setBooleanProperty(TablesToolProperties.KEY_USE_HOME_SCREEN, false);
        props.writeProperties();
      }
      return null;
    }
  }

  @Override
  public void onPostResume() {
    super.onPostResume();
    Tables.getInstance().establishDatabaseConnectionListener(this);
  }

  @Override
  public void databaseAvailable() {
    FragmentManager mgr = this.getFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 1;
    if (idxLast >= 0) {
      BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      Fragment newFragment = null;
      newFragment = mgr.findFragmentByTag(entry.getName());
      if ( newFragment instanceof DatabaseConnectionListener ) {
        ((DatabaseConnectionListener) newFragment).databaseAvailable();
      }
    }
  }

  @Override
  public void databaseUnavailable() {
    FragmentManager mgr = this.getFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 1;
    if (idxLast >= 0) {
      BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      Fragment newFragment = null;
      newFragment = mgr.findFragmentByTag(entry.getName());
      if ( newFragment instanceof DatabaseConnectionListener ) {
        ((DatabaseConnectionListener) newFragment).databaseUnavailable();
      }
    }
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
  
  public void swapScreens(ScreenType newScreenType) {
    WebLogger.getLogger(getAppName()).i(TAG, "swapScreens: Transitioning from " + 
        ((activeScreenType == null) ? "-none-" : activeScreenType.name()) +
        " to " + newScreenType.name());
    FragmentManager mgr = this.getFragmentManager();
    FragmentTransaction trans = null;
    Fragment newFragment = null;
    switch ( newScreenType ) {
    case TABLE_MANAGER_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if ( newFragment == null ) {
        newFragment = new TableManagerFragment();
      }
      break;
    case WEBVIEW_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      
      if ( newFragment == null ) {
        newFragment = new WebFragment();

        if ( this.webFileToDisplay != null ) {
          Bundle args = new Bundle();
          args.putString(Constants.IntentKeys.FILE_NAME, ODKFileUtils.asRelativePath(mAppName, webFileToDisplay));
          newFragment.setArguments(args);
        }
      }
      break;
    case ABOUT_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if ( newFragment == null ) {
        newFragment = new AboutMenuFragment();
      }
      break;
    case INITIALIZATION_SCREEN:
      newFragment = mgr.findFragmentByTag(newScreenType.name());
      if ( newFragment == null ) {
        newFragment = new InitializationFragment();
      }
      break;
    default:
      throw new IllegalStateException("Unexpected default case");
    }

    boolean matchingBackStackEntry = false;
    for (int i = 0; i < mgr.getBackStackEntryCount(); ++i) {
      BackStackEntry e = mgr.getBackStackEntryAt(i);
      WebLogger.getLogger(mAppName).i(TAG, "BackStackEntry["+i+"] " + e.getName());
      if (e.getName().equals(newScreenType.name())) {
        matchingBackStackEntry = true;
      }
    }

    if (matchingBackStackEntry) {
      if ( trans != null ) {
        WebLogger.getLogger(mAppName).e(TAG,  "Unexpected active transaction when popping state!");
        trans = null;
      }
      // flush backward, to the screen we want to go back to
      activeScreenType = newScreenType;
      mgr.popBackStackImmediate(activeScreenType.name(), 0);
    } else {
      // add transaction to show the screen we want
      if ( trans == null ) {
        trans = mgr.beginTransaction();
      }
      activeScreenType = newScreenType;
      trans.replace(R.id.activity_main_activity, newFragment, activeScreenType.name());
      trans.addToBackStack(activeScreenType.name());
    }
    
    // and see if we should re-initialize...
    if ((activeScreenType != ScreenType.INITIALIZATION_SCREEN)
        && Tables.getInstance().shouldRunInitializationTask(getAppName())) {
      WebLogger.getLogger(getAppName()).i(TAG, "swapToFragmentView -- calling clearRunInitializationTask");
      // and immediately clear the should-run flag...
      Tables.getInstance().clearRunInitializationTask(getAppName());
      // OK we should swap to the InitializationFragment view
      // this will skip the transition to whatever screen we were trying to 
      // go to and will instead show the InitializationFragment view. We
      // restore to the desired screen via the setFragmentToShowNext()
      //
      // NOTE: this discards the uncommitted transaction.
      // Robolectric complains about a recursive state transition.
      if ( trans != null ) {
        trans.commit();
      }
      swapScreens(ScreenType.INITIALIZATION_SCREEN);
    } else {
      if ( trans != null ) {
        trans.commit();
      }
      invalidateOptionsMenu();
    }
  }

  private void changeOptionsMenu(Menu menu) {
    MenuInflater menuInflater = this.getMenuInflater();
    if ( activeScreenType == ScreenType.WEBVIEW_SCREEN ) {
      menuInflater.inflate(R.menu.web_view_activity, menu);
    } else if ( activeScreenType == ScreenType.TABLE_MANAGER_SCREEN ) {
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
    if ( lastMenuType != activeScreenType ) {
      changeOptionsMenu(menu);
    }
    return super.onPrepareOptionsMenu(menu);
  }

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
      Intent preferenceIntent = new Intent(this, DisplayPrefsActivity.class);
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
        syncIntent.setComponent(new ComponentName("org.opendatakit.sync",
            "org.opendatakit.sync.activities.SyncActivity"));
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
      try {
  
        switch (requestCode) {
        case Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER:
        case Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER:
          // don't let the user cancel out of these...
          break;
        // For now, we will just refresh the table if something could have
        // changed.
        case Constants.RequestCodes.ADD_ROW_COLLECT:
          if (resultCode == Activity.RESULT_OK) {
            WebLogger.getLogger(getAppName()).d(TAG,
                "[onActivityResult] result ok, refreshing backing table");
            CollectUtil.handleOdkCollectAddReturn(getBaseContext(), getAppName(), tableId,
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
            CollectUtil.handleOdkCollectEditReturn(getBaseContext(), getAppName(), tableId,
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
      } catch (RemoteException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

}
