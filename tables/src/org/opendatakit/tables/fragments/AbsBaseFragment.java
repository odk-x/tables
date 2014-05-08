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
