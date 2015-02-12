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

import java.util.ArrayList;
import java.util.Iterator;

import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

/**
 * The base Activity for all ODK Tables activities. Performs basic
 * functionality like retrieving the app name from an intent that all classes
 * should be doing.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsBaseActivity extends Activity {

  protected String mAppName;
  protected String mActionTableId = null;
  
  Bundle mCheckpointTables = new Bundle();
  Bundle mConflictTables = new Bundle();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mAppName = retrieveAppNameFromIntent();
    if ( savedInstanceState != null ) {
      if ( savedInstanceState.containsKey(Constants.IntentKeys.ACTION_TABLE_ID) ) {
        mActionTableId = savedInstanceState.getString(Constants.IntentKeys.ACTION_TABLE_ID);
        if ( mActionTableId != null && mActionTableId.length() == 0 ) {
          mActionTableId = null;
        }
      }
      
      if ( savedInstanceState.containsKey(Constants.IntentKeys.CHECKPOINT_TABLES) ) {
        mCheckpointTables = savedInstanceState.getBundle(Constants.IntentKeys.CHECKPOINT_TABLES);
      }

      if ( savedInstanceState.containsKey(Constants.IntentKeys.CONFLICT_TABLES) ) {
        mConflictTables = savedInstanceState.getBundle(Constants.IntentKeys.CONFLICT_TABLES);
      }
    }
  }
  
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    
    if ( mActionTableId != null && mActionTableId.length() != 0 ) {
      outState.putString(Constants.IntentKeys.ACTION_TABLE_ID, mActionTableId);
    }
    if ( mCheckpointTables != null && !mCheckpointTables.isEmpty() ) {
      outState.putBundle(Constants.IntentKeys.CHECKPOINT_TABLES, mCheckpointTables);
    }
    if ( mConflictTables != null && !mConflictTables.isEmpty() ) {
      outState.putBundle(Constants.IntentKeys.CONFLICT_TABLES, mConflictTables);
    }
  }

  public String getActionTableId() {
    return mActionTableId;
  }
  
  public void setActionTableId(String tableId) {
    mActionTableId = tableId;
  }
  
  public void scanAllTables() {
    long now = System.currentTimeMillis();
    WebLogger.getLogger(getAppName()).i(this.getClass().getSimpleName(), "scanAllTables -- searching for conflicts and checkpoints ");
    
    SQLiteDatabase db = null;

    try {
      db = DatabaseFactory.get().getDatabase(this, getAppName());
      ArrayList<String> tableIds = ODKDatabaseUtils.get().getAllTableIds(db);
      
      Bundle checkpointTables = new Bundle();
      Bundle conflictTables = new Bundle();
      
      for ( String tableId : tableIds ) {
        int health = ODKDatabaseUtils.get().getTableHealth(db, tableId);
        
        if ( (health & ODKDatabaseUtils.TABLE_HEALTH_HAS_CHECKPOINTS) != 0) {
            checkpointTables.putString(tableId, tableId);
        }
        if ( (health & ODKDatabaseUtils.TABLE_HEALTH_HAS_CONFLICTS) != 0) {
            conflictTables.putString(tableId, tableId);
        }
      }
      mCheckpointTables = checkpointTables;
      mConflictTables = conflictTables;
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    
    long elapsed = System.currentTimeMillis() - now;
    WebLogger.getLogger(getAppName()).i(this.getClass().getSimpleName(), "scanAllTables -- full table scan completed: " + Long.toString(elapsed) + " ms");
  }
  
  @Override
  protected void onPostResume() {
    super.onPostResume();
    // Hijack the app here, after all screens have been resumed,
    // to ensure that all checkpoints and conflicts have been
    // resolved. If they haven't, we branch to the resolution
    // activity.
    
    if ( ( mCheckpointTables == null || mCheckpointTables.isEmpty() ) &&
         ( mConflictTables == null || mConflictTables.isEmpty() ) ) {
      scanAllTables();
    }
    if ( (mCheckpointTables != null) && !mCheckpointTables.isEmpty() ) {
      Iterator<String> iterator = mCheckpointTables.keySet().iterator();
      String tableId = iterator.next();
      mCheckpointTables.remove(tableId);

      Intent i;
      i = new Intent();
      i.setComponent(new ComponentName(Constants.ExternalIntentStrings.SYNC_PACKAGE_NAME,
              Constants.ExternalIntentStrings.SYNC_CHECKPOINT_ACTIVITY_COMPONENT_NAME));
      i.setAction(Intent.ACTION_EDIT);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tableId);
      try {
        this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER);
      } catch ( ActivityNotFoundException e ) {
        Toast.makeText(this, getString(R.string.activity_not_found, 
            Constants.ExternalIntentStrings.SYNC_CHECKPOINT_ACTIVITY_COMPONENT_NAME), Toast.LENGTH_LONG).show();
      }
    }
    if ( (mConflictTables != null) && !mConflictTables.isEmpty() ) {
      Iterator<String> iterator = mConflictTables.keySet().iterator();
      String tableId = iterator.next();
      mConflictTables.remove(tableId);

      Intent i;
      i = new Intent();
      i.setComponent(new ComponentName(Constants.ExternalIntentStrings.SYNC_PACKAGE_NAME,
          Constants.ExternalIntentStrings.SYNC_CONFLICT_ACTIVITY_COMPONENT_NAME));
      i.setAction(Intent.ACTION_EDIT);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tableId);
      try {
        this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER);
      } catch ( ActivityNotFoundException e ) {
        Toast.makeText(this, getString(R.string.activity_not_found, 
            Constants.ExternalIntentStrings.SYNC_CONFLICT_ACTIVITY_COMPONENT_NAME), Toast.LENGTH_LONG).show();
      }
    }

  }
  
  /**
   * Gets the app name from the Intent. If it is not present it returns a
   * default app name.
   * @return
   */
  String retrieveAppNameFromIntent() {
    String result = 
        this.getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if (result == null) {
      result = TableFileUtils.getDefaultAppName();
    }
    return result;
  }
  
  /**
   * Get the app name that has been set for this activity.
   * @return
   */
  public String getAppName() {
    return this.mAppName;
  }
  
  /**
   * All Intents in the app expect an app name. This method returns an Intent
   * that can be expected to play nice with other activities. The class is not
   * set.
   * @return
   */
  public Intent createNewIntentWithAppName() {
    Intent intent = new Intent();
    intent.putExtra(
        Constants.IntentKeys.APP_NAME,
        getAppName());
    return intent;
  }

}
