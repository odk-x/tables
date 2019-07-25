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
package org.opendatakit.tables.fragments;

import android.app.Activity;
import android.app.Application;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.os.Bundle;

import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.application.CommonApplication;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.utilities.AppNameUtil;

/**
 * Base class that all fragments should extend.
 *
 * @author sudar.sam@gmail.com
 */
public abstract class AbsTablesFragment extends Fragment
    implements DatabaseConnectionListener {

  private static final String TAG = AbsTablesFragment.class.getSimpleName();

  /**
   * The app name
   */
  protected String mAppName;

  public void onAttach(Context context) {
    super.onAttach(context);
    Activity activity = getActivity();
    if (!(activity instanceof AbsBaseActivity)) {
      throw new IllegalStateException(
          AbsTablesFragment.class.getSimpleName() + " must be attached to an " + AbsBaseActivity.class
              .getSimpleName());
    }
    mAppName = AppNameUtil.getAppNameFromActivity(activity);
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (mAppName == null) {
      mAppName = AppNameUtil.getAppNameFromActivity(getActivity());
    }
  }

    @Override
  public void onResume() {
    super.onResume();
    getCommonApplication().possiblyFireDatabaseCallback(getActivity(), this);
  }

  public CommonApplication getCommonApplication() {
    Application app = getActivity().getApplication();
    if (app instanceof CommonApplication) {
      return (CommonApplication) app;
    }
    throw new IllegalStateException("Bad app");
  }

  /**
   * Get the name of the app this fragment is operating under.
   *
   * @return the app name
   */
  public String getAppName() {
    // we do NOT know this will succeed because of the check in onAttach
    // Actually we found fragments still alive after their corresponding
    // activities are null so we need to always check that the activity is
    // valid in a fragment
    if (mAppName == null) {
      Activity activity = getActivity();
      if (activity != null) {
        if (!(activity instanceof IAppAwareActivity)) {
          throw new IllegalStateException(
              AbsTablesFragment.class.getSimpleName() + " must be attached to an "
                  + IAppAwareActivity.class.getSimpleName());
        }
        mAppName = ((IAppAwareActivity) activity).getAppName();
      }
      WebLogger.getLogger(mAppName).d(TAG, "mAppName was null and has been set using the activity");
    }
    return mAppName;
  }

}
