package org.opendatakit.tables.fragments;

import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.views.webkits.CustomTableView;
import org.opendatakit.tables.views.webkits.CustomView;

import android.app.Fragment;

/**
 * {@link Fragment} for displaying a List view.
 * @author sudar.sam@gmail.com
 *
 */
public class ListViewFragment extends AbsWebTableFragment {

  @Override
  public CustomView buildView() {
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
