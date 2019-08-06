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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import org.opendatakit.activities.BaseActivity;
import org.opendatakit.application.CommonApplication;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.consts.RequestCodeConsts;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.TableHealthInfo;
import org.opendatakit.database.service.TableHealthStatus;
import org.opendatakit.dependencies.DependencyChecker;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;

import java.util.Iterator;
import java.util.List;

/**
 * The base Activity for all ODK Tables activities. Performs basic
 * functionality like retrieving the app name from an intent that all classes
 * should be doing.
 *
 * @author sudar.sam@gmail.com
 */
public abstract class AbsBaseActivity extends BaseActivity {

  /**
   * Used for logging
   */
  private static String TAG = AbsBaseActivity.class.getSimpleName();
  /**
   * The app name
   */
  protected String mAppName;
  /**
   * Holds the account name and password for AbsBaseWebActivity, and the users locale
   */
  protected PropertiesSingleton mProps;
  /**
   * The id of the table that will be passed along to survey when it is started to add or edit a
   * row
   */
  private String mActionTableId = null;

  private Bundle mCheckpointTables = new Bundle();
  private Bundle mConflictTables = new Bundle();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    this.mAppName = retrieveAppNameFromIntent();
    this.mProps = CommonToolProperties.get(this, mAppName);
    if (savedInstanceState != null) {
      if (savedInstanceState.containsKey(Constants.IntentKeys.ACTION_TABLE_ID)) {
        mActionTableId = savedInstanceState.getString(Constants.IntentKeys.ACTION_TABLE_ID);
        if (mActionTableId != null && mActionTableId.isEmpty()) {
          mActionTableId = null;
        }
      }

      if (savedInstanceState.containsKey(Constants.IntentKeys.CHECKPOINT_TABLES)) {
        mCheckpointTables = savedInstanceState.getBundle(Constants.IntentKeys.CHECKPOINT_TABLES);
      }

      if (savedInstanceState.containsKey(Constants.IntentKeys.CONFLICT_TABLES)) {
        mConflictTables = savedInstanceState.getBundle(Constants.IntentKeys.CONFLICT_TABLES);
      }
    }

    super.onCreate(savedInstanceState);

    // Will finish() and close the app if we're missing dependencies
    DependencyChecker.checkDependencies(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (mActionTableId != null && !mActionTableId.isEmpty()) {
      outState.putString(Constants.IntentKeys.ACTION_TABLE_ID, mActionTableId);
    }
    if (mCheckpointTables != null && !mCheckpointTables.isEmpty()) {
      outState.putBundle(Constants.IntentKeys.CHECKPOINT_TABLES, mCheckpointTables);
    }
    if (mConflictTables != null && !mConflictTables.isEmpty()) {
      outState.putBundle(Constants.IntentKeys.CONFLICT_TABLES, mConflictTables);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    ((CommonApplication) getApplication()).establishDoNotFireDatabaseConnectionListener(this);
  }

  @Override
  public void onPostResume() {
    super.onPostResume();
    ((CommonApplication) getApplication()).fireDatabaseConnectionListener();
  }

  public String getActionTableId() {
    return mActionTableId;
  }

  public void setActionTableId(String tableId) {
    mActionTableId = tableId;
  }

  /**
   * Checks all tables for checkpoints and conflicts, adding them to mConflictTables and
   * mCheckpointTables
   */
  public void scanAllTables() {
    long start = System.currentTimeMillis();
    WebLogger.getLogger(getAppName())
        .i(TAG, "scanAllTables -- searching for conflicts and checkpoints ");

    CommonApplication app = (CommonApplication) getApplication();
    DbHandle db = null;

    if (app.getDatabase() == null) {
      return;
    }

    try {
      db = app.getDatabase().openDatabase(mAppName);
      List<TableHealthInfo> tableHealthList = app.getDatabase()
          .getTableHealthStatuses(mAppName, db);

      Bundle checkpointTables = new Bundle();
      Bundle conflictTables = new Bundle();

      for (TableHealthInfo tableHealth : tableHealthList) {
        String tableId = tableHealth.getTableId();
        TableHealthStatus status = tableHealth.getHealthStatus();

        if (status == TableHealthStatus.TABLE_HEALTH_HAS_CHECKPOINTS
            || status == TableHealthStatus.TABLE_HEALTH_HAS_CHECKPOINTS_AND_CONFLICTS) {
          checkpointTables.putString(tableId, tableId);
        }
        if (status == TableHealthStatus.TABLE_HEALTH_HAS_CONFLICTS
            || status == TableHealthStatus.TABLE_HEALTH_HAS_CHECKPOINTS_AND_CONFLICTS) {
          conflictTables.putString(tableId, tableId);
        }
      }
      mCheckpointTables = checkpointTables;
      mConflictTables = conflictTables;
    } catch (ServicesAvailabilityException e) {
      handleError(e);
    } finally {
      if (db != null) {
        try {
          app.getDatabase().closeDatabase(mAppName, db);
        } catch (ServicesAvailabilityException e) {
          handleError(e);
        }
      }
    }

    long elapsed = System.currentTimeMillis() - start;
    WebLogger.getLogger(getAppName())
        .i(TAG, "scanAllTables -- full table scan completed: " + Long.toString(elapsed) + " ms");
  }

  /**
   * Hijack the app here, after all screens have been resumed, to ensure that all checkpoints and
   * conflicts have been resolved. If they haven't, we branch to the resolution activity.
   */
  protected void resolveAnyConflicts() {

    if ((mCheckpointTables == null || mCheckpointTables.isEmpty()) && (mConflictTables == null
        || mConflictTables.isEmpty())) {
      scanAllTables();
    }
    if (mCheckpointTables != null && !mCheckpointTables.isEmpty()) {
      Iterator<String> iterator = mCheckpointTables.keySet().iterator();
      String tableId = iterator.next();
      mCheckpointTables.remove(tableId);

      Intent i;
      i = new Intent();
      i.setComponent(new ComponentName(IntentConsts.ResolveCheckpoint.APPLICATION_NAME,
          IntentConsts.ResolveCheckpoint.ACTIVITY_NAME));
      i.setAction(Intent.ACTION_EDIT);
      i.putExtra(IntentConsts.INTENT_KEY_APP_NAME, getAppName());
      i.putExtra(IntentConsts.INTENT_KEY_TABLE_ID, tableId);
      try {
        this.startActivityForResult(i, RequestCodeConsts.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER);
      } catch (ActivityNotFoundException e) {
        handleError(e);
      }
    }
    if (mConflictTables != null && !mConflictTables.isEmpty()) {
      Iterator<String> iterator = mConflictTables.keySet().iterator();
      String tableId = iterator.next();
      mConflictTables.remove(tableId);

      Intent i;
      i = new Intent();
      i.setComponent(new ComponentName(IntentConsts.ResolveConflict.APPLICATION_NAME,
          IntentConsts.ResolveConflict.ACTIVITY_NAME));
      i.setAction(Intent.ACTION_EDIT);
      i.putExtra(IntentConsts.INTENT_KEY_APP_NAME, getAppName());
      i.putExtra(IntentConsts.INTENT_KEY_TABLE_ID, tableId);
      try {
        this.startActivityForResult(i, RequestCodeConsts.RequestCodes.LAUNCH_CONFLICT_RESOLVER);
      } catch (ActivityNotFoundException e) {
        handleError(e);
      }
    }
  }

  /**
   * Gets the app name from the Intent. If it is not present it returns a default app name.
   *
   * @return the app name the intent was for
   */
  String retrieveAppNameFromIntent() {
    String result = this.getIntent().getStringExtra(IntentConsts.INTENT_KEY_APP_NAME);
    if (result == null) {
      result = TableFileUtils.getDefaultAppName();
    }
    return result;
  }

  /**
   * Get the app name that has been set for this activity.
   *
   * @return the activity's app name
   */
  public String getAppName() {
    return this.mAppName;
  }

  /**
   * All Intents in the app expect an app name. This method returns an Intent
   * that can be expected to play nice with other activities. The class is not
   * set.
   *
   * @return an intent object that has the app name from the activity
   */
  public Intent createNewIntentWithAppName() {
    Intent intent = new Intent();
    intent.putExtra(IntentConsts.INTENT_KEY_APP_NAME, getAppName());
    return intent;
  }

  /**
   * Called when the database becomes available. It tries to get a DatabaseConnectionListener out
   * of the fragment manager and notify it that the database is available
   */
  @Override
  public void databaseAvailable() {
    if (getAppName() != null) {
      resolveAnyConflicts();
    }
    FragmentManager mgr = this.getSupportFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 1;
    if (idxLast >= 0) {
      FragmentManager.BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      Fragment newFragment = mgr.findFragmentByTag(entry.getName());
      if (newFragment instanceof DatabaseConnectionListener) {
        ((DatabaseConnectionListener) newFragment).databaseAvailable();
      }
    }
  }

  /**
   * Called when the database goes away. It tries to get the DatabaseConnectionListener from the
   * fragment manager and notify it that the database has gone away
   */
  @Override
  public void databaseUnavailable() {
    FragmentManager mgr = this.getSupportFragmentManager();
    int idxLast = mgr.getBackStackEntryCount() - 1;
    if (idxLast >= 0) {
      FragmentManager.BackStackEntry entry = mgr.getBackStackEntryAt(idxLast);
      Fragment newFragment = mgr.findFragmentByTag(entry.getName());
      if (newFragment instanceof DatabaseConnectionListener) {
        ((DatabaseConnectionListener) newFragment).databaseUnavailable();
      }
    }
  }

  /**
   * Attempts to handle an error upon not being able to access services
   *
   * @param e an exception to log
   */
  private void handleError(Throwable e) {
    String toast_text;
    if (e instanceof ActivityNotFoundException) {
      WebLogger.getLogger(mAppName).e(TAG, "Services not installed?");
      toast_text = getString(R.string.services_missing);
    } else {
      WebLogger.getLogger(mAppName).e(TAG, "Could not connect to the database");
      toast_text = getString(R.string.database_unavailable);
    }
    final String toast_text_copy = toast_text;
    WebLogger.getLogger(mAppName).printStackTrace(e);
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      public void run() {
        AbsBaseActivity.this.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(AbsBaseActivity.this, toast_text_copy, Toast.LENGTH_LONG).show();
          }
        });
      }
    }, 100);

  }

}
