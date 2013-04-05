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

import com.actionbarsherlock.app.SherlockActivity;

/**
 * The Activity that will house the {@link CustomAppView} view for displaying
 * a custom html homescreen. 
 * @author sudar.sam@gmail.com
 *
 */
public class CustomHomeScreenActivity extends SherlockActivity implements
    DisplayActivity {
  
  private Controller mController;
  /** 
   * This is the main view that is responsible for showing the custom app html 
   * page. It is the core of this Activity.
   */
  private CustomAppView mView;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("");
    mController = new Controller(this, this, getIntent().getExtras());
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    init();
  }

  @Override
  public void init() {
    // TODO Auto-generated method stub
  }

  @Override
  public void onSearch() {
    // TODO Auto-generated method stub
    
  }

}
