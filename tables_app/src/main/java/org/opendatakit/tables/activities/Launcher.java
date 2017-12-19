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
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import org.opendatakit.activities.BaseLauncherActivity;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.provider.TablesProviderAPI;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.utilities.ODKFileUtils;

import java.util.List;

/**
 * This is the activity that gets called when another app tries to launch tables. It gets the app
 * name and then opens the table manager
 */
public class Launcher extends BaseLauncherActivity {

  /**
   * Used for logging
   */
  @SuppressWarnings("unused")
  private static final String TAG = Launcher.class.getName();

  // the app name
  private String mAppName;

  @Override
  public String getAppName() {
    return mAppName;
  }

  @Override
  protected void setAppSpecificPerms() {
    return;
  }

  /**
   * Restores saved state if possible. Then it looks at the table name that it was launched for
   * in the intent, and verifies that it matches the URI that tables was launched with. If they
   * match, it sets the app name based on the URI. It then makes sure that the device has the
   * right folders created, has the right dependencies installed (services and io file manager),
   * and if everything is good, it makes an intent to launch the TableManager to the requested table
   *
   * @param savedInstanceState the bundle packed by onSaveInstanceState
   */
  @Override
  public void onCreateWithPermission(Bundle savedInstanceState) {
    Intent intent = this.getIntent();
    Bundle extras = intent.getExtras();

    this.mAppName = IntentUtil.retrieveAppNameFromBundle(extras);
    if (this.mAppName == null) {
      this.mAppName = TableFileUtils.getDefaultAppName();
    }

    Uri uri = intent.getData();
    if (uri != null) {
      final Uri uriTablesProvider = TablesProviderAPI.CONTENT_URI;
      if (uri.getScheme().equalsIgnoreCase(uriTablesProvider.getScheme()) && uri.getAuthority()
          .equalsIgnoreCase(uriTablesProvider.getAuthority())) {
        List<String> segments = uri.getPathSegments();
        if (segments != null && segments.size() >= 1) {
          if (this.mAppName != null && !segments.get(0).equals(this.mAppName)) {
            Toast.makeText(this, "AppName in Intent does not match AppName in Tables URI",
                Toast.LENGTH_LONG).show();
            finish();
            return;
          }
          this.mAppName = segments.get(0);
        }
      }
    }

    // ensuring directories exist
    ODKFileUtils.verifyExternalStorageAvailability();
    ODKFileUtils.assertDirectoryStructure(this.mAppName);

    // Launch the TableManager.

    Intent i = new Intent(this, MainActivity.class);
    if (uri != null) {
      i.setData(uri);
    }
    if (extras != null) {
      i.putExtras(extras);
    }
    i.putExtra(IntentConsts.INTENT_KEY_APP_NAME, this.mAppName);
    startActivity(i);
    finish();
  }

  /**
   * We have to have this method because we implement DatabaseConnectionListener
   */
  @Override
  public void databaseAvailable() {
  }

  /**
   * We have to have this method because we implement DatabaseConnectionListener
   */
  @Override
  public void databaseUnavailable() {
  }

}
