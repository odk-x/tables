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

import java.util.List;

import org.opendatakit.common.android.activities.BaseActivity;
import org.opendatakit.common.android.provider.TablesProviderAPI;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.TableFileUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class Launcher extends BaseActivity {

  private static final String TAG = Launcher.class.getName();

  private String mAppName;
  
  @Override
  public String getAppName() {
    return mAppName;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = this.getIntent();
    Bundle extras = intent.getExtras();

    this.mAppName = IntentUtil.retrieveAppNameFromBundle(extras);
    if (this.mAppName == null) {
      this.mAppName = TableFileUtils.getDefaultAppName();
    }

    Uri uri = intent.getData();
    if (uri != null) {
      final Uri uriTablesProvider = TablesProviderAPI.CONTENT_URI;
      if (uri.getScheme().equalsIgnoreCase(uriTablesProvider.getScheme())
          && uri.getAuthority().equalsIgnoreCase(uriTablesProvider.getAuthority())) {
        List<String> segments = uri.getPathSegments();
        if (segments != null && segments.size() >= 1) {
          if ( this.mAppName != null && !segments.get(0).equals(this.mAppName) ) {
            Toast.makeText(this, "AppName in Intent does not match AppName in Tables URI", Toast.LENGTH_LONG).show();
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
    i.putExtra(Constants.IntentKeys.APP_NAME, this.mAppName);
    startActivity(i);
    finish();
  }

  @Override
  public void databaseAvailable() {
  }

  @Override
  public void databaseUnavailable() {
  }

}
