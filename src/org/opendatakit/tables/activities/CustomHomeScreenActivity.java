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

import org.opendatakit.tables.view.custom.CustomAppView;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * The Activity that will house the {@link CustomAppView} view for displaying
 * a custom html homescreen. 
 * @author sudar.sam@gmail.com
 *
 */
public class CustomHomeScreenActivity extends SherlockActivity implements
    DisplayActivity {
  
  private static final String TAG = CustomHomeScreenActivity.class.getName();
  
 // private Controller mController;
  /** 
   * This is the main view that is responsible for showing the custom app html 
   * page. It is the core of this Activity.
   */
  private CustomAppView mView;
  private LinearLayout mContainerView;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "in onCreate()");
    setTitle("");
    mContainerView = new LinearLayout(this);
    mContainerView.setLayoutParams(new ViewGroup.LayoutParams(
        LinearLayout.LayoutParams.FILL_PARENT,
        LinearLayout.LayoutParams.FILL_PARENT));
    mContainerView.setOrientation(LinearLayout.VERTICAL);
    setContentView(mContainerView);
    Bundle extras = getIntent().getExtras();
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
    mView = new CustomAppView(this);
    mContainerView.addView(mView);
    mView.display();
    //mController.setDisplayView(mView);
    //setContentView(mController.getContainerView());
  }

  @Override
  public void onSearch() {
    Log.e(TAG, "called onSearch, which is unimplemented");
    
  }
  
//  @Override
//  public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
//      mController.buildOptionsMenu(menu);
//    return true;
//  }
  
//  @Override
//  public boolean onMenuItemSelected(int featureId, 
//      com.actionbarsherlock.view.MenuItem item) {
//    return mController.handleMenuItemSelection(item);
//  }

}
