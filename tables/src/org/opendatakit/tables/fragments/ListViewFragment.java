package org.opendatakit.tables.fragments;

import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.views.webkits.CustomTableView;
import org.opendatakit.tables.views.webkits.CustomView;

import android.app.Fragment;
import android.util.Log;

/**
 * {@link Fragment} for displaying a List view.
 * @author sudar.sam@gmail.com
 *
 */
public class ListViewFragment extends AbsWebTableFragment {
  
  private static final String TAG = ListViewFragment.class.getSimpleName();

  @Override
  public CustomView buildView() {
    Log.d(TAG, "[buildView]");
    CustomView result = CustomTableView.get(
        getActivity(),
        getAppName(),
        getUserTable(),
        getFileName());
    return result;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.LIST;
  }

}
