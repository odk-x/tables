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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Toast;

public class FileSelectorPreference extends EditTextPreference {

    /** Indicates which preference we are using the selector for. */
    private int mRequestCode;
    private Activity mActivity;
    private Fragment mFragment;
    private String mAppName;
    
    public FileSelectorPreference(Context context, AttributeSet attrs) {
      super(context, attrs);
    }
    
    /**
     * Set the fields this preference uses. An activity or fragment can be set
     * to call startActivityForResult. If both are set, activity takes
     * precedence.
     * @param activity
     * @param requestCode
     * @param appName
     */
    public void setFields(Activity activity, int requestCode, String appName) {
      this.mActivity = activity;
      this.helperSetFields(requestCode, appName);
    }
    
    /**
     * Set the fields this preference uses. An activity or fragment can be set
     * to call startActivityForResult. If both are set, activity takes
     * precedence.
     * @param fragment
     * @param requestCode
     * @param appName
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
     * @param intent
     */
    private void helperStartActivityForResult(Intent intent) {
      if (this.mActivity != null) {
        this.mActivity.startActivityForResult(intent, this.mRequestCode);
      } else {
        this.mFragment.startActivityForResult(intent, this.mRequestCode);
      }
    }

    @Override
    protected void onClick() {
      if (hasFilePicker()) {
        Intent intent = new Intent("org.openintents.action.PICK_FILE");
        if (getText() != null) {
          File fullFile = ODKFileUtils.asAppFile(this.mAppName, getText());
          try {
            intent.setData(Uri.parse("file://" + fullFile.getCanonicalPath()));
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(
                this.mActivity,
                this.mActivity.getString(
                    R.string.file_not_found,
                    fullFile.getAbsolutePath()),
                Toast.LENGTH_LONG).show();
          }
        }
        try {
          this.helperStartActivityForResult(intent);
        } catch (ActivityNotFoundException e) {
          e.printStackTrace();
          Toast.makeText(
              this.mActivity,
              this.mActivity.getString(R.string.file_picker_not_found),
              Toast.LENGTH_LONG).show();
        }
      } else {
        super.onClick();
        Toast.makeText(
            this.mActivity,
            this.mActivity.getString(
                R.string.file_picker_not_found),
                Toast.LENGTH_LONG).show();
      }
    }

    /**
     * @return True if the phone has a file picker installed, false otherwise.
     */
    private boolean hasFilePicker() {
      Activity activity = this.mActivity;
      if (activity == null) {
        activity = this.mFragment.getActivity();
      }
      PackageManager packageManager = activity.getPackageManager();
      Intent intent = new Intent("org.openintents.action.PICK_FILE");
      List<ResolveInfo> list = packageManager.queryIntentActivities(
          intent,
          PackageManager.MATCH_DEFAULT_ONLY);
      return (list.size() > 0);
    }
    
}
