/*
 * Copyright (C) 2012 University of Washington
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

import org.opendatakit.tables.Activity.TableManager;
import org.opendatakit.tables.views.webkits.CustomAppView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * The Activity that will house the {@link CustomAppView} view for displaying
 * a custom html homescreen.
 * @author sudar.sam@gmail.com
 *
 */
public class CustomHomeScreenActivity extends SherlockActivity implements
    DisplayActivity {

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

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "in onCreate()");
    setTitle("");
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
      mFilename = CustomAppView.CUSTOM_HOMESCREEN_FILE_NAME;
    }
    //mController = new Controller(this, this, extras);
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
    mView = new CustomAppView(this, mFilename);
    mContainerView.addView(mView);
    mView.display();
    //mController.setDisplayView(mView);
    //setContentView(mController.getContainerView());
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
     item = menu.add(0, MENU_ITEM_TABLE_MANAGER, 0, "Launch Table Manager");
     item.setIcon(android.R.drawable.ic_menu_sort_by_size);
     item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

     return true;
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

}
