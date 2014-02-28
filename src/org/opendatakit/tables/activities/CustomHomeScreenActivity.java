/*
 * Copyright (C) 2013 University of Washington
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

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.fragments.InitializeTaskDialogFragment;
import org.opendatakit.tables.tasks.InitializeTask;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.ConfigurationUtil;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.webkits.CustomAppView;
import org.opendatakit.tables.views.webkits.CustomView.CustomViewCallbacks;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * The Activity that will house the {@link CustomAppView} view for displaying
 * a custom html homescreen.
 * @author sudar.sam@gmail.com
 *
 */
public class CustomHomeScreenActivity extends SherlockFragmentActivity
    implements DisplayActivity, CustomViewCallbacks, InitializeTask.Callbacks {

  private static final String TAG = CustomHomeScreenActivity.class.getName();

  public static final int MENU_ITEM_TABLE_MANAGER = 1;

  public static final String INTENT_KEY_FILENAME = "filename";

 // private Controller mController;
  /**
   * This is the main view that is responsible for showing the custom app html
   * page. It is the core of this Activity.
   */
  private CustomAppView mView;
  private LinearLayout mContainerView;
  private String mFilename;
  /**
   * The Preferences for dealing with importing based on a config file.
   */
  private Preferences mPrefs;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "in onCreate()");
    setTitle("");
    this.mPrefs = new Preferences(this);
    mContainerView = new LinearLayout(this);
    mContainerView.setLayoutParams(new ViewGroup.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT));
    mContainerView.setOrientation(LinearLayout.VERTICAL);
    setContentView(mContainerView);
    Bundle extras = getIntent().getExtras();
    if (extras != null && extras.getString(INTENT_KEY_FILENAME) != null) {
      mFilename = extras.getString(INTENT_KEY_FILENAME);
    } else {
      mFilename = TableFileUtils.getTablesHomeScreenFile();
    }
    // It's possible that we're coming back after a rotation. In this case, a
    // InitializeTaskDialogFragment will still exist and we'll have to hook up
    // our callbacks.
    InitializeTaskDialogFragment initalizeTaskDialogFragment =
        (InitializeTaskDialogFragment)
        getSupportFragmentManager().findFragmentByTag(
            InitializeTaskDialogFragment.TAG_FRAGMENT);
    if (initalizeTaskDialogFragment != null) {
      initalizeTaskDialogFragment.setCallbacks(this);
    } else {
      // We'll check to see if we need to begin an initialization task.
      if (ConfigurationUtil.isChanged(mPrefs)) {
        InitializeTask initializeTask = new InitializeTask(this, TableFileUtils.ODK_TABLES_APP_NAME);
        initalizeTaskDialogFragment = new InitializeTaskDialogFragment();
        initalizeTaskDialogFragment.setTask(initializeTask);
        initalizeTaskDialogFragment.setCallbacks(this);
        initalizeTaskDialogFragment.setCancelable(false);
        initializeTask.setDialogFragment(initalizeTaskDialogFragment);
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        initalizeTaskDialogFragment.show(fragmentManager,
            InitializeTaskDialogFragment.TAG_FRAGMENT);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "in onResume()");
    init();
  }

  @Override
  public void init() {
    Log.d(TAG, "in init()");
    // First we have to remove all the views--otherwise you end up with
    // multiple views and none seem to display.
    mContainerView.removeAllViews();
    mView = new CustomAppView(this, TableFileUtils.ODK_TABLES_APP_NAME, mFilename, this);
    mContainerView.addView(mView);
    mView.display();
  }

  @Override
  public void onSearch() {
    Log.e(TAG, "called onSearch, which is unimplemented");

  }

  // CREATE OPTION MENU
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
     super.onCreateOptionsMenu(menu);

     // We'll start with something to take us to the TableManager, which will
     // mean much greater flexibility.
     MenuItem item;
     item = menu.add(0, MENU_ITEM_TABLE_MANAGER, 0, getString(R.string.launch_table_manager));
     item.setIcon(android.R.drawable.ic_menu_sort_by_size);
     item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

     return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
        Intent data) {
    switch (requestCode) {
    // We are going to handle the add row the same for both cases--an add row
    // for the original table as well as an add row for another table. This is
    // because the List and Detail activities maintain a TableProperties object
    // for the table they were displaying. So when you return, if you were
    // adding to the same table that you were displaying you already have the
    // TableProperties object. In the case of this Activity, however, we never
    // have a TableProperties (since we aren't really displaying a specific
    // table), so you have to handle the return the same in both cases--first
    // retrieve the tableId for the table that launched Collect, then get the
    // TableProperties for that table, then add the row.
    case Controller.RCODE_ODK_COLLECT_ADD_ROW:
    case Controller.RCODE_ODK_COLLECT_ADD_ROW_SPECIFIED_TABLE: {
      String tableId = CollectUtil.retrieveAndRemoveTableIdForAddRow(this);
      if (tableId == null) {
        Log.e(TAG, "return from ODK Collect expected to find a tableId " +
            "specifying the target of the add row, but was null.");
        return;
      }
      TableProperties tpToReceiveAdd =
          TableProperties.getTablePropertiesForTable(
              DbHelper.getDbHelper(this, TableFileUtils.ODK_TABLES_APP_NAME), tableId,
              KeyValueStore.Type.ACTIVE);
      CollectUtil.handleOdkCollectAddReturn(this, TableFileUtils.ODK_TABLES_APP_NAME, tpToReceiveAdd,
          resultCode, data);
      break;
    }
    case Controller.RCODE_ODK_SURVEY_ADD_ROW:
    case Controller.RCODE_ODK_SURVEY_EDIT_ROW: {
      // no-op ???
      break;
    }
    default:
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public boolean onMenuItemSelected(int featureId,
      com.actionbarsherlock.view.MenuItem item) {
    switch (item.getItemId()) {
    case MENU_ITEM_TABLE_MANAGER:
      Intent i = new Intent(this, TableManager.class);
      startActivity(i);
      return true;
    default:
      Log.e(TAG, "unrecognized MenuItem id: " + item.getItemId());
      return false;
    }
  }

  @Override
  public String getSearchString() {
    // "search" makes no sense on the homescreen, so just return an empty
    // string.
    return "";
  }

  /*
   * (non-Javadoc)
   * @see org.opendatakit.tables.tasks.InitializeTask.Callbacks#getPrefs()
   */
  @Override
  public Preferences getPrefs() {
    return this.mPrefs;
  }

  /*
   * (non-Javadoc)
   * @see org.opendatakit.tables.tasks.InitializeTask.Callbacks#updateAfterImports()
   */
  @Override
  public void onImportsComplete() {
    // Here we essentially just need to reload the display for the user. This
    // is important as the javascript might be doing something like displaying
    // the current tables.
    init();
  }

}
