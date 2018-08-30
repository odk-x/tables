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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.TablePropertiesFragment;

import java.util.List;

/**
 * An activity for managing a table's properties.
 *
 * @author hkworden@gmail.com
 * @author sudar.sam@gmail.com
 */
public class TablePropertiesActivity extends AbsTableActivity
    implements DatabaseConnectionListener, IAppAwareActivity {

  /**
   * Used for logging
   */
  @SuppressWarnings("unused")
  private static final String TAG = TablePropertiesActivity.class.getSimpleName();

  // these ints are used when selecting/changing the view files
  public static final int RC_DETAIL_VIEW_FILE = 0;
  public static final int RC_LIST_VIEW_FILE = 1;
  public static final int RC_MAP_LIST_VIEW_FILE = 2;

  private String appName;
  private String tableId;

  private TablePropertiesFragment tpFragment;

  public PropertiesSingleton getPropertisSingleton(){
    return mProps;
  }

  public String getTableId() {return tableId;}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FragmentManager mgr = this.getSupportFragmentManager();

    // try to find fragment if it exists already
    Fragment possibleFragment = mgr.findFragmentByTag(TablePropertiesFragment.TAG_NAME);
    if (possibleFragment instanceof TablePropertiesFragment) {
      tpFragment = (TablePropertiesFragment) possibleFragment;
    }

    // if no fragment exists create
    if (tpFragment == null) {
      tpFragment = new TablePropertiesFragment();
    }

    mgr.beginTransaction()
        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
        .show(tpFragment).commit();

  }

  @Override
  protected void onResume() {
    super.onResume();

    Tables.getInstance().establishDoNotFireDatabaseConnectionListener(this);
  }

  @Override
  public void onPostResume() {
    super.onPostResume();
    Tables.getInstance().fireDatabaseConnectionListener();
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      return;
    }

    boolean receivedResult = false;
    if(tpFragment != null) {
      receivedResult = tpFragment.processOnActivityResult(requestCode, resultCode, data);
    }
    if(!receivedResult) {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onBackPressed() {
    setResult(RESULT_OK);
    finish();
  }

  /**
   * @return True if the phone has a file picker installed, false otherwise.
   */
  public boolean hasFilePicker() {
    PackageManager packageManager = getPackageManager();
    Intent intent = new Intent("org.openintents.action.PICK_FILE");
    List<ResolveInfo> list = packageManager
        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return !list.isEmpty();
  }
}
