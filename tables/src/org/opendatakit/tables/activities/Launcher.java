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

import java.io.File;

import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.utilities.ODKFileUtils;
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
        String appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
        if ( appName == null ) {
          appName = TableFileUtils.getDefaultAppName();
        }
        // ensuring directories exist
        String dir = ODKFileUtils.getAppFolder(appName);
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        // this should happen in another thread if possible
        CustomView.initCommonWebView(this);
        // First determine if we're supposed to use a custom home screen.
        // Do a check also to make sure the file actually exists.
        Preferences preferences = new Preferences(this, appName);
        File homeScreen = new File(ODKFileUtils.getTablesHomeScreenFile(appName));
        if (preferences.getUseHomeScreen() && homeScreen.exists()) {
          // launch it.
          Log.d(TAG, "homescreen file exists and is set to be used.");
          Intent i = new Intent(this, CustomHomeScreenActivity.class);
          i.putExtra(Controller.INTENT_KEY_APP_NAME, appName);
          startActivity(i);
        } else {
          Log.d(TAG, "no homescreen file found, launching TableManager");
          // First set the prefs to false. This is useful in the case where
          // someone has configured an app to use a home screen and then
          // deleted that file out from under it.
          preferences.setUseHomeScreen(false);
          // Launch the TableManager.
          String tableId = (new Preferences(this, appName)).getDefaultTableId();
          if (tableId == null) {
              Intent i = new Intent(this, TableManager.class);
              i.putExtra(Controller.INTENT_KEY_APP_NAME, appName);
              startActivity(i);
          } else {
              TableProperties tp = TableProperties.getTablePropertiesForTable(
                      this, appName, tableId);
              Controller.launchTableActivity(this, tp, tp.getDefaultViewType());
          }
        }
        finish();
    }
}
