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

import org.opendatakit.common.android.logic.IntentConsts;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.database.data.OrderedColumns;
import org.opendatakit.common.android.exception.ServicesAvailabilityException;
import org.opendatakit.common.android.logging.WebLogger;
import org.opendatakit.common.android.database.service.DbHandle;

import android.os.Bundle;

/**
 * This class is the base for any Activity that will display information about
 * a particular table. Callers must pass in a table id in the bundle with the
 * key {@link IntentConsts#INTENT_KEY_TABLE_ID}.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsTableActivity extends AbsBaseActivity {
  
  private static final String TAG = 
      AbsTableActivity.class.getSimpleName();
  
  private String mTableId;
  private OrderedColumns mColumnDefinitions;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mTableId = retrieveTableIdFromIntent();
    if (mTableId == null) {
      WebLogger.getLogger(getAppName()).e(TAG, "[onCreate] table id was not present in Intent.");
      throw new IllegalStateException(
          "A table id was not passed to a table activity");
    }  
  }
  
  /**
   * Retrieve the table id from the intent. Returns null if not present.
   * @return
   */
  String retrieveTableIdFromIntent() {
    return this.getIntent().getStringExtra(IntentConsts.INTENT_KEY_TABLE_ID);
  }

  public String getTableId() {
    return this.mTableId;
  }
  
  public synchronized OrderedColumns getColumnDefinitions() {
    if ( this.mColumnDefinitions == null ) {
      WebLogger.getLogger(getAppName()).e(TAG, "[onCreate] building mColumnDefinitions.");
      CommonApplication app = (CommonApplication) getApplication();
      if ( app.getDatabase() != null ) {
        DbHandle db = null;
        try {
          db = app.getDatabase().openDatabase(getAppName());
          mColumnDefinitions = app.getDatabase().getUserDefinedColumns(getAppName(), db, getTableId());
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(getAppName()).e(TAG, "[onCreate] unable to access database.");
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          throw new IllegalStateException("database went down -- handle this! " + e.toString());
        } finally {
          if (db != null) {
            try {
              app.getDatabase().closeDatabase(getAppName(), db);
            } catch (ServicesAvailabilityException e) {
              WebLogger.getLogger(getAppName()).e(TAG, "[onCreate] unable to close database.");
              WebLogger.getLogger(getAppName()).printStackTrace(e);
              throw new IllegalStateException("database went down -- handle this! " + e.toString());
            }
          }
        }
      }
    }
    return this.mColumnDefinitions;
  }
}
