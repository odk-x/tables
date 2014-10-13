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

import org.opendatakit.tables.activities.AbsBaseActivity;

import android.app.Activity;
import android.app.Fragment;

/**
 * Base class that all fragments should extend.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsBaseFragment extends Fragment {
  
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof AbsBaseActivity)) {
      throw new IllegalStateException(
          AbsBaseFragment.class.getSimpleName() +
            " must be attached to an " +
              AbsBaseActivity.class.getSimpleName());
    }
  }
  
  /**
   * Get the name of the app this fragment is operating under.
   * @return
   */
  protected String getAppName() {
    // we know this will succeed because of the check in onAttach
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    return activity.getAppName();
  }

}
