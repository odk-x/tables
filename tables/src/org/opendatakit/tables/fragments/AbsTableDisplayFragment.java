package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.application.Tables;

import android.app.Activity;
import android.app.Fragment;

/**
 * The base class for any {@link Fragment} that displays a table.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsTableDisplayFragment extends AbsBaseFragment {
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof TableDisplayActivity)) {
      throw new IllegalStateException("fragment must be attached to a " +
          TableDisplayActivity.class.getSimpleName());
    }
  }
  
  /**
   * Get the {@link TableProperties} backing the {@link UserTable}.
   * @return
   */
  TableProperties getTableProperties() {
    UserTable table = this.getUserTable();
    TableProperties result = 
        TableProperties.getTablePropertiesForTable(
            Tables.getInstance().getApplicationContext(), table.getAppName(), table.getTableId());
    return result;
  }
  
  /**
   * Get the {@link UserTable} being held by the {@link TableDisplayActivity}.
   * @return
   */
  UserTable getUserTable() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    UserTable result = activity.getUserTable();
    return result;
  }
  
  /** Return the type of this fragment. */
  public abstract TableDisplayActivity.ViewFragmentType getFragmentType();

}
