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
import android.app.Fragment;
import android.content.Context;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.application.Tables;

/**
 * Base class that all fragments should extend.
 *
 * @author sudar.sam@gmail.com
 */
public abstract class AbsBaseFragment extends Fragment implements DatabaseConnectionListener {

  private static final String LOGTAG = "AbsBaseFragment";

  protected String mAppName;

  public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof AbsBaseActivity)) {
      throw new IllegalStateException(
          AbsBaseFragment.class.getSimpleName() + " must be attached to an " + AbsBaseActivity.class
              .getSimpleName());
    }
    mAppName = ((AbsBaseActivity) context).getAppName();
  }

  @Override
  public void onResume() {
    super.onResume();
    Tables.getInstance().possiblyFireDatabaseCallback(getActivity(), this);
  }

  /**
   * Get the name of the app this fragment is operating under.
   *
   * @return
   */
  public String getAppName() {
    // we do NOT know this will succeed because of the check in onAttach
    // Actually we found fragments still alive after their corresponding
    // activities are null so we need to always check that the activity is
    // valid in a fragment
    if (mAppName == null) {
      Activity activity = getActivity();
      if (activity != null) {
        if (!(activity instanceof AbsBaseActivity)) {
          throw new IllegalStateException(
              AbsBaseFragment.class.getSimpleName() + " must be attached to an "
                  + AbsBaseActivity.class.getSimpleName());
        }
        mAppName = ((AbsBaseActivity) activity).getAppName();
      }
      WebLogger.getLogger(mAppName)
          .d(LOGTAG, "mAppName was null and has been set using the " + "activity");
    }
    return mAppName;
  }

}
