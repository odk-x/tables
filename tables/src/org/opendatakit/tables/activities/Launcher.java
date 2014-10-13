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
import java.util.List;

import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.provider.TablesProviderAPI;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class Launcher extends Activity {

  private static final String TAG = Launcher.class.getName();

  private String mAppName;
  private String mFileName;

  protected void retrieveValuesFromIntent() {
    Intent intent = this.getIntent();
    Bundle extras = intent.getExtras();
    this.mAppName = IntentUtil.retrieveAppNameFromBundle(extras);
    if (this.mAppName == null) {
      this.mAppName = TableFileUtils.getDefaultAppName();
    }
    this.mFileName = IntentUtil.retrieveFileNameFromBundle(extras);
  }
  
  protected Preferences createPreferences() {
    Preferences preferences = new Preferences(this, this.mAppName);
    return preferences;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    this.retrieveValuesFromIntent();

    Uri uri = getIntent().getData();
    if (uri != null) {
      final Uri uriTablesProvider = TablesProviderAPI.CONTENT_URI;
      if (uri.getScheme().equalsIgnoreCase(uriTablesProvider.getScheme())
          && uri.getAuthority().equalsIgnoreCase(uriTablesProvider.getAuthority())) {
        List<String> segments = uri.getPathSegments();
        if (segments != null && segments.size() >= 1) {
          this.mAppName = segments.get(0);
        }
      }
    }

    // ensuring directories exist
    ODKFileUtils.verifyExternalStorageAvailability();
    ODKFileUtils.assertDirectoryStructure(this.mAppName);

    Preferences preferences = this.createPreferences();
    
    // First determine if we're supposed to use a custom home screen.
    // Do a check also to make sure the file actually exists.
    if (useDefaultHomeScreen() || this.customFileSpecified()) {
      
      String relativePathToFile = null;
      if (this.customFileSpecified()) {
        File customFile = ODKFileUtils.asAppFile(mAppName, mFileName);
        if (!customFile.exists()) {
          Toast.makeText(
              this,
              "File does not exist: " + customFile.getAbsolutePath(),
              Toast.LENGTH_SHORT)
            .show();
          this.finish();
          return;
        }
        relativePathToFile = ODKFileUtils.asRelativePath(
            mAppName,
            customFile);
      } else {
        File defaultHomeScreen =
            new File(ODKFileUtils.getTablesHomeScreenFile(this.mAppName));
        relativePathToFile = ODKFileUtils.asRelativePath(
            mAppName,
            defaultHomeScreen);
        WebLogger.getLogger(mAppName).d(TAG, "homescreen file exists and is set to be used.");
      }

      Uri data = getIntent().getData();
      Bundle extras = getIntent().getExtras();

      Intent i = new Intent(this, WebViewActivity.class);
      if (data != null) {
        i.setData(data);
      }
      if (extras != null) {
        i.putExtras(extras);
      }
      i.putExtra(Constants.IntentKeys.APP_NAME, this.mAppName);
      i.putExtra(Constants.IntentKeys.FILE_NAME, relativePathToFile);
      startActivity(i);
    } else {
      WebLogger.getLogger(mAppName).d(TAG, "no homescreen file found, launching TableManager");
      // First set the prefs to false. This is useful in the case where
      // someone has configured an app to use a home screen and then
      // deleted that file out from under it.
      preferences.setUseHomeScreen(false);
      // Launch the TableManager.
      
      Uri data = getIntent().getData();
      Bundle extras = getIntent().getExtras();

      Intent i = new Intent(this, MainActivity.class);
      if (data != null) {
        i.setData(data);
      }
      if (extras != null) {
        i.putExtras(extras);
      }
      i.putExtra(Constants.IntentKeys.APP_NAME, this.mAppName);
      startActivity(i);
    }
    finish();
  }
  
  protected boolean customFileSpecified() {
    return this.mFileName != null;
  }
  
  /**
   * Sets if the default home screen should be launched.
   * @return
   */
  protected boolean useDefaultHomeScreen() {
    Preferences preferences = this.createPreferences();
    File userHomeScreen =
        new File(ODKFileUtils.getTablesHomeScreenFile(this.mAppName));
    return preferences.getUseHomeScreen() && userHomeScreen.exists();
  }

  
}
