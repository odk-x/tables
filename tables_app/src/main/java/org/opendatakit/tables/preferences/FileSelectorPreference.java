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
package org.opendatakit.tables.preferences;

import androidx.fragment.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import androidx.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import org.opendatakit.activities.utils.FilePickerUtil;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A class that can be used to launch a file picker and get the selected filename in the textbox
 */
public class FileSelectorPreference extends EditTextPreference {

  /**
   * Indicates which preference we are using the selector for.
   */
  private int mRequestCode = 0;
  private Fragment mFragment = null;
  private String mAppName = null;

  /**
   * Default constructor
   *
   * @param context unused
   * @param attrs   unused
   */
  public FileSelectorPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Set the fields this preference uses. An activity or fragment can be set
   * to call startActivityForResult. If both are set, activity takes
   * precedence.
   *
   * @param fragment    a fragment to save off, used for starting activities
   * @param requestCode the request code to use to start activities, fragment::onActivityResult
   *                    will get these
   * @param appName     the app name
   */
  public void setFields(Fragment fragment, int requestCode, String appName) {
    this.mFragment = fragment;
    this.helperSetFields(requestCode, appName);
  }

  private void helperSetFields(int requestCode, String appName) {
    this.mRequestCode = requestCode;
    this.mAppName = appName;
  }

  /**
   * A helper method that calls startActivityForResult on either the activity
   * or fragment, as appropriate.
   *
   * @param intent Used to start the activity
   */
  private void helperStartActivityForResult(Intent intent) {
    this.mFragment.startActivityForResult(intent, this.mRequestCode);
  }

  @Override
  protected void onClick() {
      String startingDirectory = null;
      if (getText() != null) {
        File fullFile = ODKFileUtils.asAppFile(this.mAppName, getText());
        startingDirectory = fullFile.getAbsolutePath();
      }
      Intent intent = FilePickerUtil.createFilePickerIntent(null, "*/*", startingDirectory);
      try {
        this.helperStartActivityForResult(intent);
      } catch (ActivityNotFoundException e) {
        WebLogger.getLogger(mAppName).printStackTrace(e);
        Toast.makeText(mFragment.getActivity(), mFragment.getString(R.string.file_picker_not_found),
            Toast.LENGTH_LONG).show();
      }

  }


}
