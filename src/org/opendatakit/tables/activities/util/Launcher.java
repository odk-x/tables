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
package org.opendatakit.tables.activities.util;

import java.io.File;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.CustomHomeScreenActivity;
import org.opendatakit.tables.activities.TableManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.webkits.CustomView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class Launcher extends Activity {

  private static final String TAG = Launcher.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ensuring directories exist
        String dir = ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        // this should happen in another thread if possible
        CustomView.initCommonWebView(this);
        // First determine if we're supposed to use a custom home screen.
        // Do a check also to make sure the file actually exists.
        Preferences preferences = new Preferences(this);
        if (preferences.getUseHomeScreen() &&
            TableFileUtils.tablesHomeScreenFileExists()) {
          // launch it.
          Log.d(TAG, "homescreen file exists and is set to be used.");
          Intent i = new Intent(this, CustomHomeScreenActivity.class);
          startActivity(i);
        } else {
          Log.d(TAG, "no homescreen file found, launching TableManager");
          // First set the prefs to false. This is useful in the case where
          // someone has configured an app to use a home screen and then
          // deleted that file out from under it.
          preferences.setUseHomeScreen(false);
          // Launch the TableManager.
          String tableId = (new Preferences(this)).getDefaultTableId();
          if (tableId == null) {
              Intent i = new Intent(this, TableManager.class);
              startActivity(i);
          } else {
              TableProperties tp = TableProperties.getTablePropertiesForTable(
                      DbHelper.getDbHelper(this, TableFileUtils.ODK_TABLES_APP_NAME), tableId,
                      KeyValueStore.Type.ACTIVE);
              Controller.launchTableActivity(this, tp, true);
          }
        }
        finish();
    }
}
